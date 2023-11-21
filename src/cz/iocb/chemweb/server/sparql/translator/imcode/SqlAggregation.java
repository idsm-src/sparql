package cz.iocb.chemweb.server.sparql.translator.imcode;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinDataTypes.xsdIntegerType;
import static cz.iocb.chemweb.server.sparql.translator.imcode.SqlBind.isExpressionExpansionNeeded;
import static cz.iocb.chemweb.server.sparql.translator.imcode.SqlBind.translateExpressionExpansion;
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
import cz.iocb.chemweb.server.sparql.database.TableColumn;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.parser.model.expression.BinaryExpression.Operator;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;
import cz.iocb.chemweb.server.sparql.translator.UsedVariable;
import cz.iocb.chemweb.server.sparql.translator.UsedVariables;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlBinaryArithmetic;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlBuiltinCall;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlExpressionIntercode;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlLiteral;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlNull;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlVariable;



public class SqlAggregation extends SqlIntercode
{
    private final SqlIntercode child;
    private final Set<String> groupVariables;
    private final Map<String, SqlExpressionIntercode> aggregations;


    protected SqlAggregation(UsedVariables variables, boolean isDeterministic, Set<String> groupVariables,
            Map<String, SqlExpressionIntercode> aggregations, SqlIntercode child)
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


    protected static SqlIntercode aggregate(Set<String> groupVariables,
            Map<String, SqlExpressionIntercode> aggregations, SqlIntercode child, Set<String> restrictions)
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



        class CodeWrapper
        {
            final SqlIntercode item;

            CodeWrapper(SqlIntercode item)
            {
                this.item = item;
            }

            @Override
            public int hashCode()
            {
                if(item instanceof SqlTableAccess)
                    return ((SqlTableAccess) item).getTable() == null ? 0 :
                            ((SqlTableAccess) item).getTable().hashCode();

                return super.hashCode();
            }

            @Override
            public boolean equals(Object other)
            {
                if(!(item instanceof SqlTableAccess && ((CodeWrapper) other).item instanceof SqlTableAccess))
                    return false;

                SqlTableAccess left = (SqlTableAccess) item;
                SqlTableAccess right = (SqlTableAccess) ((CodeWrapper) other).item;

                if(left.getTable() == null && right.getTable() != null
                        || left.getTable() != null && !left.getTable().equals(right.getTable()))
                    return false;

                if(!left.getConditions().equals(right.getConditions()))
                    return false;

                return true;
            }
        }


        if(groupVariables.isEmpty() && optimizedChild instanceof SqlUnion && aggregations.size() == 1
                && aggregations.values().stream().allMatch(e -> e instanceof SqlBuiltinCall
                        && ((SqlBuiltinCall) e).getFunction().equals("card") && !((SqlBuiltinCall) e).isDistinct()))
        {
            Map<CodeWrapper, Integer> counts = new HashMap<CodeWrapper, Integer>();

            for(SqlIntercode child : ((SqlUnion) optimizedChild).getChilds())
            {
                CodeWrapper wrapper = new CodeWrapper(child);

                Integer count = counts.get(wrapper);

                if(count == null)
                    counts.put(wrapper, 1);
                else
                    counts.put(wrapper, count + 1);
            }


            Set<String> cardRestriction = new HashSet<String>();
            cardRestriction.add("@card");

            Set<String> bindRestriction = new HashSet<String>();
            bindRestriction.add("@bind");

            Map<String, SqlExpressionIntercode> subAggregations = new LinkedHashMap<String, SqlExpressionIntercode>();
            subAggregations.put("@card", SqlBuiltinCall.create("card", false, new ArrayList<SqlExpressionIntercode>()));


            List<SqlIntercode> unionList = new ArrayList<SqlIntercode>();

            for(Entry<CodeWrapper, Integer> entry : counts.entrySet())
            {
                SqlIntercode code = entry.getKey().item;
                Integer count = entry.getValue();

                SqlIntercode aggregate = aggregate(groupVariables, subAggregations, code, cardRestriction);

                SqlExpressionIntercode card = SqlVariable.create("@card", aggregate.getVariables());
                SqlExpressionIntercode factor = SqlLiteral.create(new Literal(count.toString(), xsdIntegerType));
                SqlExpressionIntercode expression = SqlBinaryArithmetic.create(Operator.Multiply, factor, card);

                unionList.add(SqlBind.bind("@bind", expression, aggregate, bindRestriction, false));
            }

            SqlIntercode union = SqlUnion.union(unionList);


            List<SqlExpressionIntercode> args = List.of(SqlVariable.create("@bind", union.getVariables()));
            SqlExpressionIntercode expr = SqlBuiltinCall.create("sum", false, args);

            Map<String, SqlExpressionIntercode> outerAggregations = new LinkedHashMap<String, SqlExpressionIntercode>();
            outerAggregations.put(aggregations.keySet().iterator().next(), expr);

            return aggregate(groupVariables, outerAggregations, union, restrictions);
        }


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
            builder.append(" GROUP BY true::boolean");
        }


        if(useTwoPhases)
            builder.append(" ) AS tab");

        return builder.toString();
    }
}
