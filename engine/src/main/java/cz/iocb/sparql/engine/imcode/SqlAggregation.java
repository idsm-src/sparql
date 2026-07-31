package cz.iocb.sparql.engine.imcode;

import static cz.iocb.sparql.engine.imcode.expression.SqlBinaryArithmetic.ArithmeticOperator.MULTIPLY;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerIri;
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
import cz.iocb.sparql.engine.imcode.expression.SqlBinaryArithmetic;
import cz.iocb.sparql.engine.imcode.expression.SqlBuiltinCall;
import cz.iocb.sparql.engine.imcode.expression.SqlExpressionIntercode;
import cz.iocb.sparql.engine.imcode.expression.SqlLiteral;
import cz.iocb.sparql.engine.imcode.expression.SqlNull;
import cz.iocb.sparql.engine.imcode.expression.SqlVariable;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.TypedLiteral;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.VariableBinding;
import cz.iocb.sparql.engine.translator.VariableBindings;



public final class SqlAggregation extends SqlIntercode
{
    private final SqlIntercode child;
    private final Set<Variable> groupVariables;
    private final Map<Variable, SqlExpressionIntercode> aggregations;


    protected SqlAggregation(VariableBindings bindings, boolean isDeterministic, Set<Variable> groupVariables,
            Map<Variable, SqlExpressionIntercode> aggregations, SqlIntercode child)
    {
        super(bindings, isDeterministic);

        this.child = child;
        this.groupVariables = groupVariables;
        this.aggregations = aggregations;
    }


    public static SqlIntercode aggregate(Request request, Set<Variable> groupVariables,
            Map<Variable, SqlExpressionIntercode> aggregations, SqlIntercode child)
    {
        return aggregate(request, groupVariables, aggregations, child, null);
    }


    protected static SqlIntercode aggregate(Request request, Set<Variable> groupVariables,
            Map<Variable, SqlExpressionIntercode> aggregations, SqlIntercode child, Restrictions restrictions)
    {
        VariableBindings bindings = new VariableBindings();

        for(Variable variable : groupVariables)
            if(child.getVariableBindings().get(variable) != null)
                bindings.add(child.getVariableBindings().get(variable));

        for(Entry<Variable, SqlExpressionIntercode> entry : aggregations.entrySet())
        {
            Set<ResourceClass> resClasses = entry.getValue().getResourceClasses();
            VariableBinding binding = new VariableBinding(entry.getKey(), entry.getValue().canBeNull());
            resClasses.stream()
                    .forEach(res -> binding.addMapping(res, res.createColumns(request.getColumnMap(), entry.getKey())));
            bindings.add(binding);
        }

        bindings = bindings.restrict(restrictions);

        boolean isDeterministic = child.isDeterministic();

        for(Entry<Variable, SqlExpressionIntercode> entry : aggregations.entrySet())
            if(restrictions == null || restrictions.contains(entry.getKey(), entry.getValue().getResourceClasses()))
                isDeterministic &= entry.getValue().isDeterministic();

        return new SqlAggregation(bindings, isDeterministic, groupVariables, aggregations, child);
    }


    @Override
    public SqlIntercode optimize(Request request, Restrictions restrictions, boolean reduced, boolean evalServices)
    {
        SqlIntercode optChild = child;
        Map<Variable, SqlExpressionIntercode> optAggregations = aggregations;

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
                List<SqlIntercode> childs = new ArrayList<>();

                for(SqlIntercode child : segs)
                {
                    Map<Variable, SqlExpressionIntercode> aggregations = optimizeAggregations(request, optAggregations,
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
            Map<SqlIntercode, Integer> counts = new HashMap<>();

            for(SqlIntercode child : union.getChilds())
                counts.merge(child, 1, Integer::sum);

            List<SqlIntercode> unionList = new ArrayList<>();

            for(Entry<SqlIntercode, Integer> entry : counts.entrySet())
            {
                SqlIntercode code = entry.getKey();
                Integer count = entry.getValue();

                if(count > 1)
                {
                    Map<Variable, SqlExpressionIntercode> subAggregations = Map.of(new Variable("@card"),
                            SqlBuiltinCall.create(request, "card", false, new ArrayList<>()));

                    SqlIntercode aggregate = aggregate(request, groupVariables, subAggregations, code);

                    SqlExpressionIntercode card = SqlVariable.create(aggregate.getVariable(new Variable("@card")));
                    SqlExpressionIntercode factor = SqlLiteral.create(request,
                            new TypedLiteral(count.toString(), xsdIntegerIri));
                    SqlExpressionIntercode expression = SqlBinaryArithmetic.create(MULTIPLY, factor, card);

                    unionList.add(SqlBind.bind(request, new Variable("@bind"), expression, aggregate));
                }
                else
                {
                    var subAggregations = Map.of(new Variable("@bind"),
                            SqlBuiltinCall.create(request, "card", false, List.of()));
                    SqlIntercode aggregate = aggregate(request, groupVariables, subAggregations, code);
                    unionList.add(aggregate);
                }
            }

            SqlIntercode optUnion = SqlUnion.union(request, unionList);

            List<SqlExpressionIntercode> args = List
                    .of(SqlVariable.create(optUnion.getVariable(new Variable("@bind"))));
            SqlExpressionIntercode expr = SqlBuiltinCall.create(request, "sum", false, args);

            Map<Variable, SqlExpressionIntercode> outerAggregations = new LinkedHashMap<>();
            outerAggregations.put(optAggregations.keySet().iterator().next(), expr);

            return aggregate(request, groupVariables, outerAggregations, optUnion).optimize(request, restrictions,
                    reduced, evalServices);
        }


        /* change count(distinct v) to count(v) */
        if(optAggregations.size() == 1 && optAggregations.values().iterator().next() instanceof SqlBuiltinCall call
                && call.getFunction().equals("count") && call.getArgument() instanceof SqlVariable var
                && call.isDistinct())
        {
            Set<Variable> distinctVars = new HashSet<>();
            distinctVars.add(var.getVariable());

            for(Variable v : groupVariables)
                if(restrictions.containsVar(v) || !optChild.hasConstantVariable(v))
                    distinctVars.add(v);

            SqlIntercode child = SqlDistinct.create(request, optChild, distinctVars);
            List<SqlExpressionIntercode> args = List.of(SqlVariable.create(child.getVariable(var.getVariable())));

            Map<Variable, SqlExpressionIntercode> subAggregations = Map.of(optAggregations.keySet().iterator().next(),
                    SqlBuiltinCall.create(request, "count", false, args));

            return aggregate(request, groupVariables, subAggregations, child).optimize(request, restrictions, reduced,
                    evalServices);
        }


        /* implicit group with eliminated aggregates */
        if(groupVariables.isEmpty() && optAggregations.values().stream()
                .noneMatch(r -> r instanceof SqlBuiltinCall c && c.isAggregateFunction()))
        {
            SqlIntercode result = SqlEmptySolution.get();

            for(Entry<Variable, SqlExpressionIntercode> entry : optAggregations.entrySet())
                if(!entry.getValue().equals(SqlNull.get()))
                    result = SqlBind.bind(request, entry.getKey(), entry.getValue(), result);

            return result.optimize(request, restrictions, reduced, evalServices);
        }


        if(optAggregations.equals(aggregations) && optChild == child && restrictions.isOptimized(bindings))
            return this;

        return aggregate(request, groupVariables, optAggregations, optChild, restrictions);
    }


    private Map<Variable, SqlExpressionIntercode> optimizeAggregations(Request request,
            Map<Variable, SqlExpressionIntercode> aggregations, SqlIntercode child, Restrictions restrictions,
            boolean evalServices)
    {
        DatabaseSchema schema = request.getConfiguration().getDatabaseSchema();

        LinkedHashMap<Variable, SqlExpressionIntercode> opt = new LinkedHashMap<>();

        for(Entry<Variable, SqlExpressionIntercode> e : aggregations.entrySet())
        {
            SqlExpressionIntercode optExpr = e.getValue().optimize(request, child.getVariableBindings(),
                    restrictions.getRestriction(e.getKey()), evalServices);

            if(optExpr.getResourceClasses().stream().anyMatch(r -> restrictions.contains(e.getKey(), r)))
                opt.put(e.getKey(), optExpr);
        }


        /* change count(var) on count(*) if possible  */
        for(Map.Entry<Variable, SqlExpressionIntercode> entry : opt.entrySet())
            if(entry.getValue() instanceof SqlBuiltinCall call && call.getFunction().equals("count")
                    && !call.isDistinct() && !call.getArguments().get(0).canBeNull())
                opt.put(entry.getKey(), SqlBuiltinCall.create(request, "card", false, List.of()));


        /* change count(distinct var) on count(*) if possible  */
        if(child instanceof SqlTableAccess tab && tab.getTable() != null)
            for(Map.Entry<Variable, SqlExpressionIntercode> entry : opt.entrySet())
                if(entry.getValue() instanceof SqlBuiltinCall call && call.getFunction().equals("count")
                        && call.isDistinct() && call.getArguments().get(0) instanceof SqlVariable var
                        && schema.isKey(tab.getTable(), var.getBinding().getNonConstantColumns()))
                    opt.put(entry.getKey(), SqlBuiltinCall.create(request, "card", false, List.of()));

        return opt;
    }


    private static Restrictions getChildRestrictions(Set<Variable> groupVariables,
            Map<Variable, SqlExpressionIntercode> aggregations, Restrictions restrictions)
    {
        Restrictions childRestrictions = new Restrictions(groupVariables);

        for(Entry<Variable, SqlExpressionIntercode> entry : aggregations.entrySet())
            if(restrictions.contains(entry.getKey(), entry.getValue().getResourceClasses()))
                childRestrictions.add(entry.getValue().getRequirements());

        return childRestrictions;
    }


    @Override
    public String translate(Request request)
    {
        Set<Column> groupByColumns = new HashSet<>();

        for(Variable var : groupVariables)
            if(child.getVariableBindings().get(var) != null)
                groupByColumns.addAll(child.getVariableBindings().get(var).getNonConstantColumns());


        StringBuilder builder = new StringBuilder();

        builder.append("SELECT ");
        boolean hasSelect = false;

        for(Entry<Variable, SqlExpressionIntercode> entry : aggregations.entrySet())
        {
            Variable var = entry.getKey();
            SqlExpressionIntercode expression = entry.getValue();
            VariableBinding binding = getVariableBindings().get(var);

            for(Entry<ResourceClass, List<Column>> e : expression.getBinding().getMappings().entrySet())
            {
                ResourceClass resClass = e.getKey();
                List<Column> names = binding.getMapping(resClass);
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

        for(Entry<Variable, SqlExpressionIntercode> e : aggregations.entrySet())
        {
            indentInfo(builder, indent, true);
            e.getValue().generateExplanation(builder, getIndent(indent, false) + "  ");
            builder.append(" as ");
            builder.append(e.getKey());
        }

        if(!groupVariables.isEmpty())
            builder.append(groupVariables.stream().map(v -> v.toString()).collect(joining(" ", " over ", "")));

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
