package cz.iocb.sparql.engine.translator.imcode;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinDataTypes.xsdIntegerType;
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
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.parser.model.expression.BinaryExpression.Operator;
import cz.iocb.sparql.engine.parser.model.expression.Literal;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.UsedVariable;
import cz.iocb.sparql.engine.translator.UsedVariables;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlBinaryArithmetic;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlBuiltinCall;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlExpressionIntercode;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlLiteral;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlNull;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlVariable;



public final class SqlAggregation extends SqlIntercode
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


    public static SqlIntercode aggregate(Request request, Set<String> groupVariables,
            Map<String, SqlExpressionIntercode> aggregations, SqlIntercode child)
    {
        return aggregate(request, groupVariables, aggregations, child, null);
    }


    protected static SqlIntercode aggregate(Request request, Set<String> groupVariables,
            Map<String, SqlExpressionIntercode> aggregations, SqlIntercode child, Restrictions restrictions)
    {
        UsedVariables variables = new UsedVariables();

        for(String variable : groupVariables)
            if(child.getVariables().get(variable) != null)
                variables.add(child.getVariables().get(variable));

        for(Entry<String, SqlExpressionIntercode> entry : aggregations.entrySet())
        {
            Set<ResourceClass> resClasses = entry.getValue().getResourceClasses();
            UsedVariable variable = new UsedVariable(entry.getKey(), entry.getValue().canBeNull());
            resClasses.stream().forEach(
                    res -> variable.addMapping(res, res.createColumns(request.getColumnMap(), entry.getKey())));
            variables.add(variable);
        }

        variables = variables.restrict(restrictions);

        boolean isDeterministic = child.isDeterministic();

        for(Entry<String, SqlExpressionIntercode> entry : aggregations.entrySet())
            if(restrictions == null || restrictions.contains(entry.getKey(), entry.getValue().getResourceClasses()))
                isDeterministic &= entry.getValue().isDeterministic();

        return new SqlAggregation(variables, isDeterministic, groupVariables, aggregations, child);
    }


    @Override
    public SqlIntercode optimize(Request request, Restrictions restrictions, boolean reduced, boolean evalServices)
    {
        SqlIntercode optChild = child;
        Map<String, SqlExpressionIntercode> optAggregations = aggregations;

        boolean childReduce = optAggregations.values().stream()
                .allMatch(a -> a instanceof SqlBuiltinCall c && c.isDistinct());

        Restrictions childRestrictions = getChildRestrictions(groupVariables, optAggregations, restrictions);

        while(true)
        {
            optChild = optChild.optimize(request, childRestrictions, childReduce, evalServices);
            optAggregations = optimizeAggregations(request, optAggregations, optChild, restrictions, evalServices);

            boolean newChildReduce = optAggregations.values().stream()
                    .allMatch(a -> a instanceof SqlBuiltinCall c && c.isDistinct());

            Restrictions newChildRestrictions = getChildRestrictions(groupVariables, optAggregations, restrictions);

            if(newChildReduce == childReduce && newChildRestrictions.equals(childRestrictions))
                break;

            childReduce = newChildReduce;
            childRestrictions = newChildRestrictions;
        }

        /* expand union if its child are grouped by disjoint values */
        if(optChild instanceof SqlUnion union && !optChild.hasConstantVariables(groupVariables))
        {
            List<SqlIntercode> segs = SqlDistinct.expandUnionByResourceClasses(request, union, groupVariables);

            if(segs.size() > 1)
            {
                List<SqlIntercode> childs = new ArrayList<SqlIntercode>();

                for(SqlIntercode child : segs)
                {
                    Map<String, SqlExpressionIntercode> aggregations = optimizeAggregations(request, optAggregations,
                            child, restrictions, evalServices);
                    childs.add(aggregate(request, groupVariables, aggregations, child));
                }

                return SqlUnion.union(request, childs).optimize(request, restrictions, true, evalServices);
            }
        }

        /* expand count(*) to not compute same union branches twice */
        if(optChild instanceof SqlUnion union && optChild.hasConstantVariables(groupVariables)
                && optAggregations.size() == 1
                && optAggregations.values().stream().allMatch(e -> e instanceof SqlBuiltinCall call
                        && call.getFunction().equals("card") && !call.isDistinct()))
        {
            Map<SqlIntercode, Integer> counts = new HashMap<SqlIntercode, Integer>();

            for(SqlIntercode child : union.getChilds())
                counts.merge(child, 1, Integer::sum);

            List<SqlIntercode> unionList = new ArrayList<SqlIntercode>();

            for(Entry<SqlIntercode, Integer> entry : counts.entrySet())
            {
                SqlIntercode code = entry.getKey();
                Integer count = entry.getValue();

                if(count > 1)
                {
                    Map<String, SqlExpressionIntercode> subAggregations = Map.of("@card",
                            SqlBuiltinCall.create(request, "card", false, new ArrayList<SqlExpressionIntercode>()));

                    SqlIntercode aggregate = aggregate(request, groupVariables, subAggregations, code);

                    SqlExpressionIntercode card = SqlVariable.create(aggregate.getVariable("@card"));
                    SqlExpressionIntercode factor = SqlLiteral.create(request,
                            new Literal(count.toString(), xsdIntegerType));
                    SqlExpressionIntercode expression = SqlBinaryArithmetic.create(Operator.Multiply, factor, card);

                    unionList.add(SqlBind.bind(request, "@bind", expression, aggregate));
                }
                else
                {
                    var subAggregations = Map.of("@bind", SqlBuiltinCall.create(request, "card", false, List.of()));
                    SqlIntercode aggregate = aggregate(request, groupVariables, subAggregations, code);
                    unionList.add(aggregate);
                }
            }

            SqlIntercode optUnion = SqlUnion.union(request, unionList);

            List<SqlExpressionIntercode> args = List.of(SqlVariable.create(optUnion.getVariable("@bind")));
            SqlExpressionIntercode expr = SqlBuiltinCall.create(request, "sum", false, args);

            Map<String, SqlExpressionIntercode> outerAggregations = new LinkedHashMap<String, SqlExpressionIntercode>();
            outerAggregations.put(optAggregations.keySet().iterator().next(), expr);

            return aggregate(request, groupVariables, outerAggregations, optUnion).optimize(request, restrictions,
                    reduced, evalServices);
        }


        /* change count(distinct v) to count(v) */
        if(optAggregations.size() == 1 && optAggregations.values().iterator().next() instanceof SqlBuiltinCall call
                && call.getFunction().equals("count") && call.getArgument() instanceof SqlVariable var
                && call.isDistinct())
        {
            Set<String> distinctVars = new HashSet<String>();
            distinctVars.add(var.getName());

            for(String v : groupVariables)
                if(restrictions.containsVar(v) || !optChild.hasConstantVariable(v))
                    distinctVars.add(v);

            SqlIntercode child = SqlDistinct.create(request, optChild, distinctVars);
            List<SqlExpressionIntercode> args = List.of(SqlVariable.create(child.getVariable(var.getName())));

            Map<String, SqlExpressionIntercode> subAggregations = Map.of(optAggregations.keySet().iterator().next(),
                    SqlBuiltinCall.create(request, "count", false, args));

            return aggregate(request, groupVariables, subAggregations, child).optimize(request, restrictions, reduced,
                    evalServices);
        }


        /* implicit group with eliminated aggregates */
        if(groupVariables.isEmpty() && optAggregations.values().stream()
                .noneMatch(r -> r instanceof SqlBuiltinCall c && c.isAggregateFunction()))
        {
            SqlIntercode result = SqlEmptySolution.get();

            for(Entry<String, SqlExpressionIntercode> entry : optAggregations.entrySet())
                if(!entry.getValue().equals(SqlNull.get()))
                    result = SqlBind.bind(request, entry.getKey(), entry.getValue(), result);

            return result.optimize(request, restrictions, reduced, evalServices);
        }


        if(optAggregations.equals(aggregations) && optChild == child && restrictions.isOptimized(variables))
            return this;

        return aggregate(request, groupVariables, optAggregations, optChild, restrictions);
    }


    private Map<String, SqlExpressionIntercode> optimizeAggregations(Request request,
            Map<String, SqlExpressionIntercode> aggregations, SqlIntercode child, Restrictions restrictions,
            boolean evalServices)
    {
        DatabaseSchema schema = request.getConfiguration().getDatabaseSchema();

        LinkedHashMap<String, SqlExpressionIntercode> opt = new LinkedHashMap<String, SqlExpressionIntercode>();

        for(Entry<String, SqlExpressionIntercode> e : aggregations.entrySet())
        {
            SqlExpressionIntercode optExpr = e.getValue().optimize(request, child.getVariables(),
                    restrictions.getRestriction(e.getKey()), evalServices);

            if(optExpr.getResourceClasses().stream().anyMatch(r -> restrictions.contains(e.getKey(), r)))
                opt.put(e.getKey(), optExpr);
        }


        /* change count(var) on count(*) if possible  */
        for(Map.Entry<String, SqlExpressionIntercode> entry : opt.entrySet())
            if(entry.getValue() instanceof SqlBuiltinCall call && call.getFunction().equals("count")
                    && !call.isDistinct() && !call.getArguments().get(0).canBeNull())
                opt.put(entry.getKey(), SqlBuiltinCall.create(request, "card", false, List.of()));


        /* change count(distinct var) on count(*) if possible  */
        if(child instanceof SqlTableAccess tab && tab.getTable() != null)
            for(Map.Entry<String, SqlExpressionIntercode> entry : opt.entrySet())
                if(entry.getValue() instanceof SqlBuiltinCall call && call.getFunction().equals("count")
                        && call.isDistinct() && call.getArguments().get(0) instanceof SqlVariable var
                        && schema.isKey(tab.getTable(), var.getUsedVariable().getNonConstantColumns()))
                    opt.put(entry.getKey(), SqlBuiltinCall.create(request, "card", false, List.of()));

        return opt;
    }


    private static Restrictions getChildRestrictions(Set<String> groupVariables,
            Map<String, SqlExpressionIntercode> aggregations, Restrictions restrictions)
    {
        Restrictions childRestrictions = new Restrictions(groupVariables);

        for(Entry<String, SqlExpressionIntercode> entry : aggregations.entrySet())
            if(restrictions.contains(entry.getKey(), entry.getValue().getResourceClasses()))
                childRestrictions.add(entry.getValue().getRequirements());

        return childRestrictions;
    }


    @Override
    public String translate(Request request)
    {
        Set<Column> groupByColumns = new HashSet<Column>();

        for(String variableName : groupVariables)
            if(child.getVariables().get(variableName) != null)
                groupByColumns.addAll(child.getVariables().get(variableName).getNonConstantColumns());


        StringBuilder builder = new StringBuilder();

        builder.append("SELECT ");
        boolean hasSelect = false;

        for(Entry<String, SqlExpressionIntercode> entry : aggregations.entrySet())
        {
            String variableName = entry.getKey();
            SqlExpressionIntercode expression = entry.getValue();
            UsedVariable variable = getVariables().get(variableName);

            for(Entry<ResourceClass, List<Column>> e : expression.getUsedVariable().getMappings().entrySet())
            {
                ResourceClass resClass = e.getKey();
                List<Column> names = variable.getMapping(resClass);
                List<Column> cols = expression.get(resClass);

                for(int i = 0; i < resClass.getColumnCount(); i++)
                {
                    appendComma(builder, hasSelect);
                    hasSelect = true;

                    builder.append(cols.get(i));
                    builder.append(" AS ");
                    builder.append(names.get(i));
                }
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
        builder.append(child.translate(request));
        builder.append(" ) AS tab");

        if(!groupByColumns.isEmpty())
            builder.append(groupByColumns.stream().map(Object::toString).collect(joining(", ", " GROUP BY ", "")));
        else if(!groupVariables.isEmpty())
            builder.append(" GROUP BY true::boolean");
        else if(aggregations.values().stream().noneMatch(e -> e instanceof SqlBuiltinCall c && c.isAggregateFunction()))
            builder.append(" GROUP BY ()");


        return builder.toString();
    }


    @Override
    public boolean hasServiceSubpattern()
    {
        return child.hasServiceSubpattern();
    }


    @Override
    public void generateExplanation(StringBuilder builder, String indent)
    {
        builder.append("aggregate");

        for(Entry<String, SqlExpressionIntercode> e : aggregations.entrySet())
        {
            indentInfo(builder, indent, true);
            e.getValue().generateExplanation(builder, getIndent(indent, false) + "  ");
            builder.append(" as ");
            builder.append(e.getKey());
        }

        if(!groupVariables.isEmpty())
            builder.append(groupVariables.stream().collect(joining(" ", " over ", "")));

        indentChild(builder, indent, true);
        child.generateExplanation(builder, getIndent(indent, true));
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(!(object instanceof SqlAggregation imcode))
            return false;

        if(!super.equals(imcode))
            return false;

        if(!Objects.equals(aggregations, imcode.aggregations))
            return false;

        if(!Objects.equals(groupVariables, imcode.groupVariables))
            return false;

        if(!Objects.equals(child, imcode.child))
            return false;

        return true;
    }


    @Override
    protected int getHashCode()
    {
        return Objects.hash(aggregations, groupVariables, child);
    }
}
