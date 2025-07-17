package cz.iocb.sparql.engine.translator.imcode;

import static java.util.stream.Collectors.joining;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import cz.iocb.sparql.engine.translator.imcode.expression.SqlNodeValue;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlVariable;



//TODO: add support for binding parameter and result variables
public class SqlProcedureCall extends SqlIntercode
{
    private static final String resultName = "@res";

    private final SqlIntercode child;
    private final ProcedureDefinition procedure;
    private final LinkedHashMap<ParameterDefinition, SqlNodeValue> parameters;
    private final LinkedHashMap<ResultDefinition, String> results;
    private final Map<Column, Column> columnMap;


    protected SqlProcedureCall(UsedVariables variables, ProcedureDefinition procedure,
            LinkedHashMap<ParameterDefinition, SqlNodeValue> parameters,
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
            LinkedHashMap<ParameterDefinition, SqlNodeValue> parameters,
            LinkedHashMap<ResultDefinition, String> results, SqlIntercode child)
    {
        return create(request, procedure, parameters, results, child, null);
    }


    protected static SqlIntercode create(Request request, ProcedureDefinition procedure,
            LinkedHashMap<ParameterDefinition, SqlNodeValue> parameters,
            LinkedHashMap<ResultDefinition, String> results, SqlIntercode child, Restrictions restrictions)
    {
        UsedVariables callVariables = new UsedVariables();

        for(Entry<ParameterDefinition, SqlNodeValue> entry : parameters.entrySet())
        {
            ParameterDefinition definition = entry.getKey();
            ResourceClass resClass = definition.getParameterClass();
            SqlNodeValue node = entry.getValue();

            if(node instanceof SqlVariable var)
            {
                String name = var.getName();
                UsedVariable other = callVariables.get(var.getName());

                if(other != null)
                {
                    Set<ResourceClass> otherClasses = other.getClasses();

                    if(!otherClasses.contains(resClass) && !otherClasses.contains(resClass.getGeneralClass()))
                    {
                        if(otherClasses.stream().anyMatch(r -> resClass == r.getGeneralClass()))
                        {
                            Map<ResourceClass, List<Column>> mappings = new HashMap<ResourceClass, List<Column>>();

                            for(Entry<ResourceClass, List<Column>> e : other.getMappings().entrySet())
                                if(e.getKey().getGeneralClass() != resClass)
                                    mappings.put(e.getKey(), e.getValue());

                            mappings.put(resClass, getColumns(child, name, resClass));

                            callVariables.add(new UsedVariable(name, mappings, false));
                        }
                        else
                        {
                            Map<ResourceClass, List<Column>> mappings = new HashMap<>(other.getMappings());
                            mappings.put(resClass, getColumns(child, name, resClass));

                            callVariables.add(new UsedVariable(name, mappings, false));
                        }
                    }
                }
                else
                {
                    callVariables.add(new UsedVariable(name, resClass, getColumns(child, name, resClass), false));
                }
            }
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

        for(Entry<ParameterDefinition, SqlNodeValue> entry : parameters.entrySet())
            childRestrictions.add(entry.getValue().getRequirements(Set.of(entry.getKey().getParameterClass())));

        //FIXME: is procedure deterministic?
        SqlIntercode optChild = child.optimize(request, childRestrictions, reduced, evalServices);


        if(optChild instanceof SqlUnion union)
        {
            List<SqlIntercode> childs = new ArrayList<SqlIntercode>();

            for(SqlIntercode child : union.getChilds())
                childs.add(create(request, procedure, parameters, optResults, child, restrictions));

            return SqlUnion.union(request, childs).optimize(request, restrictions, reduced, evalServices);
        }

        if(optChild != SqlEmptySolution.get() && parameters.values().stream()
                .noneMatch(n -> n instanceof SqlVariable v && !v.getUsedVariable().isConstant()))
        {
            SqlIntercode call = create(request, procedure, parameters, optResults, SqlEmptySolution.get());
            return SqlJoin.join(request, call, optChild).optimize(request, restrictions, reduced, evalServices);
        }


        UsedVariables callVariables = new UsedVariables();

        for(Entry<ParameterDefinition, SqlNodeValue> entry : parameters.entrySet())
        {
            ParameterDefinition definition = entry.getKey();
            ResourceClass resClass = definition.getParameterClass();
            SqlNodeValue node = entry.getValue();

            if(node instanceof SqlVariable var)
            {
                String name = var.getName();
                UsedVariable variable = callVariables.get(var.getName());

                if(variable != null)
                {
                    ResourceClass other = variable.getResourceClass();

                    if(resClass != other && resClass.getGeneralClass() != other && resClass != other.getGeneralClass())
                        return SqlNoSolution.get();

                    if(resClass == other.getGeneralClass())
                        resClass = other;
                }

                List<Column> columns = getColumns(optChild, name, resClass);

                if(columns == null)
                    return SqlNoSolution.get();

                callVariables.add(new UsedVariable(name, resClass, columns, false));
            }
            else if(node == null
                    || resClass != node.getResourceClass() && resClass.getGeneralClass() != node.getResourceClass()
                            && resClass != node.getResourceClass().getGeneralClass())
            {
                return SqlNoSolution.get();
            }
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


        if(optResults.equals(results) && optChild == child && restrictions.isOptimized(variables))
            return this;

        return create(request, procedure, parameters, optResults, optChild, restrictions);
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

        for(Entry<ParameterDefinition, SqlNodeValue> entry : parameters.entrySet())
        {
            appendComma(builder, hasParameter);
            hasParameter = true;

            ResourceClass resClass = entry.getKey().getParameterClass();
            SqlNodeValue node = entry.getValue();
            List<Column> columns = node.asResource(request, resClass);

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

        if(child != SqlEmptySolution.get())
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

        return variable.toResource(resClass);
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
}
