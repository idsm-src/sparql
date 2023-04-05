package cz.iocb.chemweb.server.sparql.translator.imcode;

import static java.util.stream.Collectors.joining;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import cz.iocb.chemweb.server.sparql.database.Column;
import cz.iocb.chemweb.server.sparql.database.ConstantColumn;
import cz.iocb.chemweb.server.sparql.database.ExpressionColumn;
import cz.iocb.chemweb.server.sparql.database.TableColumn;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.mapping.extension.ParameterDefinition;
import cz.iocb.chemweb.server.sparql.mapping.extension.ProcedureDefinition;
import cz.iocb.chemweb.server.sparql.mapping.extension.ResultDefinition;
import cz.iocb.chemweb.server.sparql.parser.model.VariableOrBlankNode;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;
import cz.iocb.chemweb.server.sparql.translator.UsedVariable;
import cz.iocb.chemweb.server.sparql.translator.UsedVariables;



public class SqlProcedureCall extends SqlIntercode
{
    private static final String resultName = "@res";

    private final SqlIntercode child;
    private final ProcedureDefinition procedure;
    private final LinkedHashMap<ParameterDefinition, Node> parameters;
    private final LinkedHashMap<ResultDefinition, VariableOrBlankNode> results;
    private final Map<Column, Column> columnMap;


    protected SqlProcedureCall(UsedVariables variables, ProcedureDefinition procedure,
            LinkedHashMap<ParameterDefinition, Node> parameters,
            LinkedHashMap<ResultDefinition, VariableOrBlankNode> results, SqlIntercode child,
            Map<Column, Column> columnMap)
    {
        super(variables, child.isDeterministic()); //TODO: is procedure deterministic?

        this.child = child;
        this.procedure = procedure;
        this.parameters = parameters;
        this.results = results;
        this.columnMap = columnMap;
    }


    public static SqlIntercode create(ProcedureDefinition procedure,
            LinkedHashMap<ParameterDefinition, Node> parameters,
            LinkedHashMap<ResultDefinition, VariableOrBlankNode> results, SqlIntercode child)
    {
        UsedVariables callVariables = new UsedVariables();

        for(Entry<ParameterDefinition, Node> entry : parameters.entrySet())
        {
            ParameterDefinition definition = entry.getKey();
            Node node = entry.getValue();

            if(node instanceof VariableOrBlankNode)
            {
                String name = ((VariableOrBlankNode) node).getSqlName();
                ResourceClass resClass = definition.getParameterClass();

                UsedVariable variable = callVariables.get(name);

                if(variable != null && !variable.getClasses().contains(definition.getParameterClass()))
                    return SqlNoSolution.get();

                List<Column> columns = getColumns(child, name, resClass);

                if(columns == null)
                    return SqlNoSolution.get();

                callVariables.add(new UsedVariable(name, resClass, columns, false));
            }
            else if(node == null || !definition.getParameterClass().match(node))
            {
                return SqlNoSolution.get();
            }
        }


        for(Entry<ResultDefinition, VariableOrBlankNode> entry : results.entrySet())
        {
            ResultDefinition definition = entry.getKey();
            VariableOrBlankNode node = entry.getValue();

            if(node == null)
                return SqlNoSolution.get();

            String name = node.getSqlName();
            ResourceClass resClass = definition.getResultClass();

            List<Column> columns = getSqlResultColumns(definition.getSqlTypeFields());
            callVariables.add(new UsedVariable(name, resClass, columns, false));
        }


        SqlIntercode originalChild = null;

        if(parameters.values().stream().noneMatch(n -> n instanceof VariableOrBlankNode)
                && child != SqlEmptySolution.get())
        {
            originalChild = child;
            child = SqlEmptySolution.get();
        }


        Map<Column, Column> columnMap = new HashMap<Column, Column>();
        UsedVariables variables = getJoinUsedVariables(callVariables, child.getVariables(), null, null, null,
                columnMap);

        SqlProcedureCall call = new SqlProcedureCall(variables, procedure, parameters, results, child, columnMap);

        if(originalChild == null)
            return call;

        return SqlJoin.join(call, originalChild);
    }


    @Override
    public SqlIntercode optimize(Set<String> restrictions, boolean reduced)
    {
        if(restrictions == null)
            return this;

        LinkedHashMap<ResultDefinition, VariableOrBlankNode> restrictedResults = new LinkedHashMap<>();

        for(Entry<ResultDefinition, VariableOrBlankNode> result : results.entrySet())
            if(restrictions.contains(result.getValue().getSqlName()))
                restrictedResults.put(result.getKey(), result.getValue());


        HashSet<String> childRestrictions = new HashSet<String>(restrictions);

        for(Node paramater : parameters.values())
            if(paramater instanceof VariableOrBlankNode)
                childRestrictions.add(((VariableOrBlankNode) paramater).getSqlName());

        //FIXME: is procedure deterministic?
        SqlIntercode optimized = child.optimize(childRestrictions, reduced);


        return create(procedure, parameters, restrictedResults, optimized);
    }


    @Override
    public String translate()
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
        generateInnerSelect(builder);
        builder.append(" ) AS tab ");

        return builder.toString();
    }


    private void generateInnerSelect(StringBuilder builder)
    {
        builder.append("SELECT ");

        builder.append(procedure.getSqlProcedure());

        boolean hasParameter = false;
        builder.append("(");

        for(Entry<ParameterDefinition, Node> entry : parameters.entrySet())
        {
            appendComma(builder, hasParameter);
            hasParameter = true;

            ResourceClass resClass = entry.getKey().getParameterClass();
            Node node = entry.getValue();

            if(node instanceof VariableOrBlankNode)
            {
                String varName = ((VariableOrBlankNode) node).getSqlName();
                UsedVariable variable = child.getVariables().get(varName);
                List<Column> columns = variable.toResource(resClass);

                for(int i = 0; i < resClass.getColumnCount(); i++)
                {
                    appendComma(builder, i > 0);
                    builder.append(columns.get(i));
                }
            }
            else
            {
                List<Column> columns = resClass.toColumns(entry.getValue());

                for(int i = 0; i < resClass.getColumnCount(); i++)
                {
                    appendComma(builder, i > 0);
                    builder.append(columns.get(i));
                }
            }
        }

        builder.append(")");

        builder.append(" AS \"");
        builder.append(resultName);
        builder.append('"');

        for(Column column : child.getVariables().restrict(this.getVariables().getNames()).getNonConstantColumns())
        {
            builder.append(", ");
            builder.append(column);
        }

        if(child != SqlEmptySolution.get())
        {
            builder.append(" FROM (");
            builder.append(child.translate());
            builder.append(" ) AS tab");
        }
    }


    private static List<Column> getColumns(SqlIntercode child, String name, ResourceClass resClass)
    {
        UsedVariable variable = child.getVariables().get(name);

        if(variable == null)
            return null;

        Set<ResourceClass> classes = variable.getCompatibleClasses(resClass);

        if(classes.isEmpty())
            return null;

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
