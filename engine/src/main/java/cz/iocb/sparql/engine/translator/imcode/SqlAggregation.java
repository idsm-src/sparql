package cz.iocb.sparql.engine.translator.imcode;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinDataTypes.xsdIntegerType;
import static cz.iocb.sparql.engine.translator.imcode.SqlBind.isExpressionExpansionNeeded;
import static cz.iocb.sparql.engine.translator.imcode.SqlBind.translateExpressionExpansion;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
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
import cz.iocb.sparql.engine.database.TableColumn;
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
            if(entry.getValue() != SqlNull.get())
            {
                Set<ResourceClass> resClasses = entry.getValue().getResourceClasses();

                for(ResourceClass resClass : resClasses)
                {
                    if(!resClass.canBeDerivatedFromGeneral())
                    {
                        ResourceClass genClass = resClass.getGeneralClass();

                        if(resClasses.stream().filter(c -> c.getGeneralClass() == genClass).count() > 1)
                        {
                            resClasses = resClasses.stream().filter(c -> c.getGeneralClass() != genClass)
                                    .collect(toSet());
                            resClasses.add(genClass);
                        }
                    }
                }

                UsedVariable variable = new UsedVariable(entry.getKey(), entry.getValue().canBeNull());
                resClasses.stream().forEach(
                        res -> variable.addMapping(res, res.createColumns(request.getColumnMap(), entry.getKey())));
                variables.add(variable);
            }
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
            optAggregations = optimizeAggregations(request, optAggregations, optChild, evalServices);

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
                            child, evalServices);
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
            record CodeWrapper(SqlIntercode item, String code)
            {
                @Override
                public int hashCode()
                {
                    return item instanceof SqlTableAccess tab ? Objects.hashCode(tab.getTable()) : item.hashCode();
                }

                @Override
                public boolean equals(Object other)
                {
                    if(other instanceof CodeWrapper o && item instanceof SqlTableAccess l
                            && o.item instanceof SqlTableAccess r && Objects.equals(l.getTable(), r.getTable())
                            && l.getConditions().equals(r.getConditions()) && l.getReduced() == r.getReduced())
                        return true;


                    if(other instanceof CodeWrapper o && item instanceof SqlDistinct pl
                            && o.item instanceof SqlDistinct pr && pl.getChild() instanceof SqlTableAccess l
                            && pr.getChild() instanceof SqlTableAccess r && Objects.equals(l.getTable(), r.getTable())
                            && l.getConditions().equals(r.getConditions()) /*&& l.getReduced() == r.getReduced()*/)
                    {
                        Set<Column> sl = new HashSet<Column>();
                        Set<Column> sr = new HashSet<Column>();

                        for(String v : pl.getVariables().getNames())
                            sl.addAll(l.getInternalVariable(v).getNonConstantColumns());

                        for(String v : pr.getVariables().getNames())
                            sr.addAll(r.getInternalVariable(v).getNonConstantColumns());

                        if(sl.equals(sr))
                            return true;
                    }

                    //FIXME: use a better approach to decide whether the two codes are equivalent
                    if(other instanceof CodeWrapper o && o.code.equals(code))
                        return true;

                    return false;
                }
            }


            Map<CodeWrapper, Integer> counts = new HashMap<CodeWrapper, Integer>();

            for(SqlIntercode child : union.getChilds())
                counts.merge(new CodeWrapper(child, child.translate(request)), 1, Integer::sum);

            List<SqlIntercode> unionList = new ArrayList<SqlIntercode>();

            for(Entry<CodeWrapper, Integer> entry : counts.entrySet())
            {
                SqlIntercode code = entry.getKey().item;
                Integer count = entry.getValue();

                if(count > 1)
                {
                    Map<String, SqlExpressionIntercode> subAggregations = Map.of("@card",
                            SqlBuiltinCall.create(request, "card", false, new ArrayList<SqlExpressionIntercode>()));

                    SqlIntercode aggregate = aggregate(request, groupVariables, subAggregations, code);

                    SqlExpressionIntercode card = SqlVariable.create("@card", aggregate.getVariables());
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

            List<SqlExpressionIntercode> args = List.of(SqlVariable.create("@bind", optUnion.getVariables()));
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
            List<SqlExpressionIntercode> args = List.of(SqlVariable.create(var.getName(), child.getVariables()));

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
                if(entry.getValue() != SqlNull.get())
                    result = SqlBind.bind(request, entry.getKey(), entry.getValue(), result);

            return result.optimize(request, restrictions, reduced, evalServices);
        }


        if(optAggregations.equals(aggregations) && optChild == child && restrictions.isOptimized(variables))
            return this;

        return aggregate(request, groupVariables, optAggregations, optChild, restrictions);
    }


    private Map<String, SqlExpressionIntercode> optimizeAggregations(Request request,
            Map<String, SqlExpressionIntercode> aggregations, SqlIntercode child, boolean evalServices)
    {
        DatabaseSchema schema = request.getConfiguration().getDatabaseSchema();

        LinkedHashMap<String, SqlExpressionIntercode> opt = new LinkedHashMap<String, SqlExpressionIntercode>();
        aggregations.forEach((k, v) -> opt.put(k, v.optimize(request, child.getVariables(), evalServices)));

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
                childRestrictions.add(entry.getValue().getRequirements(null));

        return childRestrictions;
    }


    @Override
    public String translate(Request request)
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
                else if(variable.hasMapping())
                {
                    appendComma(builder, hasSelect);
                    hasSelect = true;

                    Column column = new TableColumn(variableName + "##expression");
                    builder.append(translateExpressionExpansion(column, variable, expression.isBoxed()));
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

            builder.append(expression.translate(request));
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
        builder.append(child.translate(request));
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
        else if(aggregations.values().stream().noneMatch(e -> e instanceof SqlBuiltinCall c && c.isAggregateFunction()))
        {
            builder.append(" GROUP BY ()");
        }


        if(useTwoPhases)
            builder.append(" ) AS tab");

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
}
