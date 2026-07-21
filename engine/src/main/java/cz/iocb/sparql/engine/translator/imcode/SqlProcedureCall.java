package cz.iocb.sparql.engine.translator.imcode;

import static cz.iocb.sparql.engine.mapping.classes.ResourceClass.getIntersectionClass;
import static java.util.stream.Collectors.joining;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.database.ExpressionColumn;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.mapping.extension.ParameterDefinition;
import cz.iocb.sparql.engine.mapping.extension.ProcedureDefinition;
import cz.iocb.sparql.engine.mapping.extension.ResultDefinition;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.UsedPairedVariable;
import cz.iocb.sparql.engine.translator.UsedVariable;
import cz.iocb.sparql.engine.translator.UsedVariables;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlExpressionIntercode;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlExpressionIntercode.Restriction;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlVariable;



//TODO: add support for binding parameter and result variables
public final class SqlProcedureCall extends SqlIntercode
{
    private static final String resultName = "@res";

    private final SqlIntercode child;
    private final ProcedureDefinition procedure;
    private final LinkedHashMap<ParameterDefinition, SqlExpressionIntercode> parameters;
    private final LinkedHashMap<ResultDefinition, String> results;
    private final Map<Column, Column> columnMap;


    protected SqlProcedureCall(UsedVariables variables, ProcedureDefinition procedure,
            LinkedHashMap<ParameterDefinition, SqlExpressionIntercode> parameters,
            LinkedHashMap<ResultDefinition, String> results, SqlIntercode child, Map<Column, Column> columnMap)
    {
        super(variables, child.isDeterministic()); //TODO: is procedure deterministic?

        this.child = child;
        this.procedure = procedure;
        this.parameters = parameters;
        this.results = results;
        this.columnMap = columnMap;
    }


    public static SqlIntercode create(Request request, ProcedureDefinition procedure,
            LinkedHashMap<ParameterDefinition, SqlExpressionIntercode> parameters,
            LinkedHashMap<ResultDefinition, String> results, SqlIntercode child)
    {
        return create(request, procedure, parameters, results, child, null);
    }


    protected static SqlIntercode create(Request request, ProcedureDefinition procedure,
            LinkedHashMap<ParameterDefinition, SqlExpressionIntercode> parameters,
            LinkedHashMap<ResultDefinition, String> results, SqlIntercode child, Restrictions restrictions)
    {
        Map<String, Set<ResourceClass>> resClasses = new HashMap<String, Set<ResourceClass>>();

        for(Entry<ParameterDefinition, SqlExpressionIntercode> entry : parameters.entrySet())
        {
            ParameterDefinition definition = entry.getKey();
            ResourceClass resClass = definition.getParameterClass();
            SqlExpressionIntercode node = entry.getValue();

            if(node instanceof SqlVariable var)
                resClasses.computeIfAbsent(var.getName(), _ -> new HashSet<ResourceClass>()).add(resClass);
        }


        UsedVariables callVariables = new UsedVariables();

        for(Entry<String, Set<ResourceClass>> e : resClasses.entrySet())
        {
            ResourceClass interClass = getIntersectionClass(e.getValue());
            String name = e.getKey();

            if(interClass == null)
                continue;

            callVariables.add(new UsedVariable(name, interClass, getColumns(child, name, interClass), false));
        }

        for(Entry<ResultDefinition, String> entry : results.entrySet())
        {
            ResultDefinition definition = entry.getKey();
            String node = entry.getValue();

            Map<ResourceClass, List<Column>> mappings = new HashMap<ResourceClass, List<Column>>();

            for(Entry<ResourceClass, List<Column>> e : definition.getMappings().entrySet())
                mappings.put(e.getKey(), getSqlResultColumns(e.getValue()));

            callVariables.add(new UsedVariable(node, mappings, false));
        }


        Map<Column, Column> columnMap = new HashMap<Column, Column>();
        UsedVariables variables = getJoinUsedVariables(request, callVariables, child.getVariables(), null, null, null,
                columnMap).restrict(restrictions);

        return new SqlProcedureCall(variables, procedure, parameters, results, child, columnMap);
    }


    @Override
    public SqlIntercode optimize(Request request, Restrictions restrictions, boolean reduced, boolean evalServices)
    {
        LinkedHashMap<ResultDefinition, String> optResults = new LinkedHashMap<>();

        for(Entry<ResultDefinition, String> result : results.entrySet())
            if(restrictions.contains(result.getValue(), result.getKey().getMappings().keySet()))
                optResults.put(result.getKey(), result.getValue());


        Restrictions childRestrictions = new Restrictions(restrictions);

        for(Entry<ParameterDefinition, SqlExpressionIntercode> entry : parameters.entrySet())
            childRestrictions.add(entry.getValue().getRequirements());

        //FIXME: is procedure deterministic?

        SqlIntercode optChild = child.optimize(request, childRestrictions, reduced, evalServices);
        LinkedHashMap<ParameterDefinition, SqlExpressionIntercode> optParameters = optimize(request, parameters,
                optChild);


        if(optChild instanceof SqlUnion union)
        {
            List<SqlIntercode> childs = new ArrayList<SqlIntercode>();

            for(SqlIntercode child : union.getChilds())
            {
                LinkedHashMap<ParameterDefinition, SqlExpressionIntercode> childParams = optimize(request,
                        optParameters, child);
                childs.add(create(request, procedure, childParams, optResults, child, restrictions));
            }

            return SqlUnion.union(request, childs).optimize(request, restrictions, reduced, evalServices);
        }


        Map<String, Set<ResourceClass>> resClasses = new HashMap<String, Set<ResourceClass>>();

        for(Entry<ParameterDefinition, SqlExpressionIntercode> entry : optParameters.entrySet())
        {
            ParameterDefinition definition = entry.getKey();
            ResourceClass resClass = definition.getParameterClass();
            SqlExpressionIntercode node = entry.getValue();

            if(node instanceof SqlVariable var)
                resClasses.computeIfAbsent(var.getName(), _ -> new HashSet<ResourceClass>()).add(resClass);
            else if(node == null || ResourceClass.areDisjunct(resClass, node.getResourceClasses()))
                return SqlNoSolution.get();
        }


        UsedVariables callVariables = new UsedVariables();

        for(Entry<String, Set<ResourceClass>> e : resClasses.entrySet())
        {
            ResourceClass interClass = getIntersectionClass(e.getValue());
            String name = e.getKey();

            if(interClass == null)
                return SqlNoSolution.get();

            callVariables.add(new UsedVariable(name, interClass, getColumns(optChild, name, interClass), false));
        }


        for(Entry<ResultDefinition, String> entry : optResults.entrySet())
        {
            ResultDefinition definition = entry.getKey();
            String node = entry.getValue();

            Map<ResourceClass, List<Column>> mappings = new HashMap<ResourceClass, List<Column>>();

            for(Entry<ResourceClass, List<Column>> e : definition.getMappings().entrySet())
                mappings.put(e.getKey(), getSqlResultColumns(e.getValue()));

            callVariables.add(new UsedVariable(node, mappings, false));
        }

        for(SqlIntercode r : getJoinList(optChild))
        {
            ArrayList<UsedPairedVariable> pairs = UsedPairedVariable.getPairs(callVariables, r.getVariables());

            if(pairs.stream().anyMatch(p -> !p.isJoinable()))
                return SqlNoSolution.get();
        }


        if(optResults.equals(results) && optParameters.equals(parameters) && optChild == child
                && restrictions.isOptimized(variables))
            return this;

        return create(request, procedure, optParameters, optResults, optChild, restrictions);
    }


    protected static LinkedHashMap<ParameterDefinition, SqlExpressionIntercode> optimize(Request request,
            LinkedHashMap<ParameterDefinition, SqlExpressionIntercode> parameters, SqlIntercode context)
    {
        LinkedHashMap<ParameterDefinition, SqlExpressionIntercode> result = new LinkedHashMap<ParameterDefinition, SqlExpressionIntercode>();

        for(Entry<ParameterDefinition, SqlExpressionIntercode> e : parameters.entrySet())
            result.put(e.getKey(), e.getValue().optimize(request, context.getVariables(),
                    new Restriction(e.getKey().getParameterClass()), false));

        return result;
    }


    @Override
    public String translate(Request request)
    {
        Set<Column> columns = variables.getNonConstantColumns();

        StringBuilder builder = new StringBuilder();

        builder.append("SELECT ");

        if(!columns.isEmpty())
            builder.append(columns.stream().map(c -> (columnMap.get(c) != null ? columnMap.get(c) + " AS " : "") + c)
                    .collect(joining(", ")));
        else
            builder.append("1");

        builder.append(" FROM (");
        generateInnerSelect(request, builder);
        builder.append(" ) AS tab ");

        return builder.toString();
    }


    private void generateInnerSelect(Request request, StringBuilder builder)
    {
        builder.append("SELECT ");

        builder.append(procedure.getSqlProcedure());

        boolean hasParameter = false;
        builder.append("(");

        for(Entry<ParameterDefinition, SqlExpressionIntercode> entry : parameters.entrySet())
        {
            appendComma(builder, hasParameter);
            hasParameter = true;

            ResourceClass resClass = entry.getKey().getParameterClass();
            SqlExpressionIntercode node = entry.getValue();
            List<Column> columns = node.get(resClass);

            for(int i = 0; i < resClass.getColumnCount(); i++)
            {
                appendComma(builder, i > 0);
                builder.append(columnMap.getOrDefault(columns.get(i), columns.get(i)));
            }
        }

        builder.append(")");

        builder.append(" AS \"");
        builder.append(resultName);
        builder.append('"');

        for(Column column : child.getVariables().restrict(new Restrictions(this.getVariables().getNames()))
                .getNonConstantColumns())
        {
            builder.append(", ");
            builder.append(column);
        }

        if(!child.equals(SqlEmptySolution.get()))
        {
            builder.append(" FROM (");
            builder.append(child.translate(request));
            builder.append(" ) AS tab");
        }
    }


    private static List<Column> getColumns(SqlIntercode child, String name, ResourceClass resClass)
    {
        UsedVariable variable = child.getVariables().get(name);

        if(variable == null)
            return resClass.getSqlTypes().stream().map(t -> (Column) new ConstantColumn(null, t)).toList();

        return variable.deriveMapping(resClass);
    }


    private static List<Column> getSqlResultColumns(List<Column> fields)
    {
        List<Column> result = new ArrayList<Column>();

        if(fields == null)
        {
            result.add(new ExpressionColumn("\"" + resultName + "\""));
        }
        else
        {
            for(Column field : fields)
            {
                if(field instanceof TableColumn)
                    result.add(new ExpressionColumn("(\"" + resultName + "\")." + field));
                else if(field instanceof ConstantColumn)
                    result.add(field);
                else
                    throw new IllegalArgumentException();
            }
        }

        return result;
    }


    @Override
    public boolean hasServiceSubpattern()
    {
        return child.hasServiceSubpattern();
    }


    @Override
    public void generateExplanation(StringBuilder builder, String indent)
    {
        builder.append("call ");
        builder.append(procedure.getProcedureName());

        for(Entry<ResultDefinition, String> e : results.entrySet())
        {
            indentInfo(builder, indent, true);

            if(e.getKey().getResultName() != null)
                builder.append(e.getKey().getResultName()).append(" ");
            else
                builder.append("#result");

            builder.append(" to ");
            builder.append(e.getValue());
        }

        for(Entry<ParameterDefinition, SqlExpressionIntercode> e : parameters.entrySet())
        {
            indentInfo(builder, indent, true);
            builder.append(e.getKey().getParamName());
            builder.append(" as ");
            e.getValue().generateExplanation(builder, null);
        }

        indentChild(builder, indent, true);
        child.generateExplanation(builder, getIndent(indent, true));
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(!(object instanceof SqlProcedureCall imcode))
            return false;

        if(!super.equals(imcode))
            return false;

        if(!Objects.equals(procedure, imcode.procedure))
            return false;

        if(!Objects.equals(parameters, imcode.parameters))
            return false;

        if(!Objects.equals(results, imcode.results))
            return false;

        if(!Objects.equals(child, imcode.child))
            return false;

        return true;
    }


    @Override
    protected int getHashCode()
    {
        return Objects.hash(procedure, parameters, results, child);
    }
}
