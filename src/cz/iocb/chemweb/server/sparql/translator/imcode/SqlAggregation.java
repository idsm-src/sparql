package cz.iocb.chemweb.server.sparql.translator.imcode;

import static cz.iocb.chemweb.server.sparql.translator.imcode.SqlBind.isExpressionExpansionNeeded;
import static cz.iocb.chemweb.server.sparql.translator.imcode.SqlBind.translateExpressionExpansion;
import static java.util.stream.Collectors.joining;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;
import cz.iocb.chemweb.server.sparql.database.Column;
import cz.iocb.chemweb.server.sparql.database.TableColumn;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.translator.UsedVariable;
import cz.iocb.chemweb.server.sparql.translator.UsedVariables;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlBuiltinCall;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlExpressionIntercode;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlNull;



public class SqlAggregation extends SqlIntercode
{
    private final SqlIntercode child;
    private final HashSet<String> groupVariables;
    private final LinkedHashMap<String, SqlExpressionIntercode> aggregations;


    protected SqlAggregation(UsedVariables variables, boolean isDeterministic, HashSet<String> groupVariables,
            LinkedHashMap<String, SqlExpressionIntercode> aggregations, SqlIntercode child)
    {
        super(variables, isDeterministic);

        this.child = child;
        this.groupVariables = groupVariables;
        this.aggregations = aggregations;
    }


    public static SqlIntercode aggregate(HashSet<String> groupVariables,
            LinkedHashMap<String, SqlExpressionIntercode> aggregations, SqlIntercode child)
    {
        return aggregate(groupVariables, aggregations, child, null);
    }


    protected static SqlIntercode aggregate(HashSet<String> groupVariables,
            LinkedHashMap<String, SqlExpressionIntercode> aggregations, SqlIntercode child, Set<String> restrictions)
    {
        /* special cases */

        //NOTE: special case - implicit group with eliminated aggregates
        if(groupVariables.isEmpty() && aggregations.values().stream().noneMatch(r -> r instanceof SqlBuiltinCall))
        {
            SqlIntercode result = SqlEmptySolution.get();

            for(Entry<String, SqlExpressionIntercode> entry : aggregations.entrySet())
                if(entry.getValue() != SqlNull.get())
                    result = SqlBind.bind(entry.getKey(), entry.getValue(), result);

            return result;
        }


        /* standard aggregates */

        UsedVariables variables = new UsedVariables();

        for(String variable : groupVariables)
            if(child.getVariables().get(variable) != null && (restrictions == null || restrictions.contains(variable)))
                variables.add(child.getVariables().get(variable));

        for(Entry<String, SqlExpressionIntercode> entry : aggregations.entrySet())
        {
            if(entry.getValue() != SqlNull.get() && (restrictions == null || restrictions.contains(entry.getKey())))
            {
                UsedVariable variable = new UsedVariable(entry.getKey(), entry.getValue().canBeNull());

                for(ResourceClass resClass : entry.getValue().getResourceClasses())
                    variable.addMapping(resClass, resClass.createColumns(entry.getKey()));

                variables.add(variable);
            }
        }

        boolean isDeterministic = child.isDeterministic();

        for(Entry<String, SqlExpressionIntercode> entry : aggregations.entrySet())
            if(restrictions == null || restrictions.contains(entry.getKey()))
                isDeterministic &= entry.getValue().isDeterministic();

        return new SqlAggregation(variables, isDeterministic, groupVariables, aggregations, child);
    }


    @Override
    public SqlIntercode optimize(Set<String> restrictions, boolean reduced)
    {
        if(restrictions == null)
            return this;

        HashSet<String> childRestrictions = new HashSet<String>(groupVariables);

        for(Entry<String, SqlExpressionIntercode> entry : aggregations.entrySet())
            if(restrictions == null || restrictions.contains(entry.getKey()))
                childRestrictions.addAll(entry.getValue().getReferencedVariables());

        SqlIntercode optimizedChild = child.optimize(childRestrictions, false);

        LinkedHashMap<String, SqlExpressionIntercode> optimizedAggregations = new LinkedHashMap<String, SqlExpressionIntercode>();
        aggregations.forEach((k, v) -> optimizedAggregations.put(k, v.optimize(optimizedChild.getVariables())));

        return aggregate(groupVariables, optimizedAggregations, optimizedChild, restrictions);
    }


    @Override
    public String translate()
    {
        boolean useTwoPhases = aggregations.values().stream().anyMatch(e -> isExpressionExpansionNeeded(e));

        Set<Column> groupByColumns = new HashSet<Column>();

        for(String variableName : groupVariables)
            if(child.getVariables().get(variableName) != null)
                groupByColumns.addAll(child.getVariables().get(variableName).getNonConstantColumns());


        StringBuilder builder = new StringBuilder();

        if(useTwoPhases)
        {
            builder.append("SELECT ");
            boolean hasSelect = false;

            for(Entry<String, SqlExpressionIntercode> entry : aggregations.entrySet())
            {
                String variableName = entry.getKey();
                SqlExpressionIntercode expression = entry.getValue();
                UsedVariable variable = getVariables().get(variableName);

                if(variable == null)
                    continue;

                if(!isExpressionExpansionNeeded(expression))
                {
                    Set<Column> columns = variable.getNonConstantColumns();

                    if(!columns.isEmpty())
                    {
                        appendComma(builder, hasSelect);
                        hasSelect = true;

                        builder.append(columns.stream().map(Object::toString).collect(joining(", ")));
                    }
                }
                else
                {
                    appendComma(builder, hasSelect);
                    hasSelect = true;

                    Column column = new TableColumn(variableName + "##expression");
                    builder.append(translateExpressionExpansion(column, variable));
                }
            }

            if(!groupByColumns.isEmpty())
            {
                appendComma(builder, hasSelect);
                hasSelect = true;
                builder.append(groupByColumns.stream().map(Object::toString).collect(joining(", ")));
            }

            if(!hasSelect)
                builder.append("1");

            builder.append(" FROM (");
        }


        builder.append("SELECT ");
        boolean hasSelect = false;

        for(Entry<String, SqlExpressionIntercode> entry : aggregations.entrySet())
        {
            String variableName = entry.getKey();
            SqlExpressionIntercode expression = entry.getValue();
            UsedVariable variable = getVariables().get(variableName);

            if(variable == null)
                continue;

            appendComma(builder, hasSelect);
            hasSelect = true;

            boolean expand = isExpressionExpansionNeeded(expression);
            Column column = expand ? new TableColumn(variableName + "##expression") :
                    variable.getMapping(variable.getClasses().iterator().next()).get(0);

            builder.append(expression.translate());
            builder.append(" AS ");
            builder.append(column);
        }

        if(!groupByColumns.isEmpty())
        {
            appendComma(builder, hasSelect);
            hasSelect = true;
            builder.append(groupByColumns.stream().map(Object::toString).collect(joining(", ")));
        }

        if(!hasSelect)
            builder.append("1");

        builder.append(" FROM (");
        builder.append(child.translate());
        builder.append(" ) AS tab");

        if(!groupByColumns.isEmpty())
        {
            builder.append(" GROUP BY ");
            builder.append(groupByColumns.stream().map(Object::toString).collect(joining(", ")));
        }
        else if(!groupVariables.isEmpty())
        {
            builder.append(" GROUP BY true");
        }


        if(useTwoPhases)
            builder.append(" ) AS tab");

        return builder.toString();
    }
}
