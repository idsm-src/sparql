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
import cz.iocb.sparql.engine.database.SqlBuilder;
import cz.iocb.sparql.engine.database.SqlCode;
import cz.iocb.sparql.engine.imcode.expression.SqlBinaryArithmetic;
import cz.iocb.sparql.engine.imcode.expression.SqlBuiltinCall;
import cz.iocb.sparql.engine.imcode.expression.SqlExpressionIntercode;
import cz.iocb.sparql.engine.imcode.expression.SqlLiteral;
import cz.iocb.sparql.engine.imcode.expression.SqlNull;
import cz.iocb.sparql.engine.imcode.expression.SqlVariable;
import cz.iocb.sparql.engine.mapping.classes.ClassRelations;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.TypedLiteral;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.VariableBinding;
import cz.iocb.sparql.engine.translator.VariableBindings;



/**
 * GROUP BY over the group variables with the aggregate expressions computed per group; without group variables all
 * solutions form one group.
 */
public final class SqlAggregation extends SqlIntercode
{
    /**
     * Solutions being grouped.
     */
    private final SqlIntercode child;

    /**
     * Grouping variables.
     */
    private final Set<Variable> groupVariables;

    /**
     * Aggregate expression of each result variable.
     */
    private final Map<Variable, SqlExpressionIntercode> aggregations;


    /**
     * Creates the node.
     *
     * @param bindings the variable bindings
     * @param isDeterministic whether the node is deterministic
     * @param groupVariables the grouping variables
     * @param aggregations aggregate expression of each result variable
     * @param child the child node
     */
    protected SqlAggregation(VariableBindings bindings, boolean isDeterministic, Set<Variable> groupVariables,
            Map<Variable, SqlExpressionIntercode> aggregations, SqlIntercode child)
    {
        super(bindings, isDeterministic);

        this.child = child;
        this.groupVariables = groupVariables;
        this.aggregations = aggregations;
    }


    /**
     * Aggregation of the child, binding each aggregate expression to its variable.
     *
     * @param request the current request
     * @param groupVariables the grouping variables
     * @param aggregations aggregate expression of each result variable
     * @param child the child node
     * @return aggregation of the child, binding each aggregate expression to its variable
     */
    public static SqlIntercode aggregate(Request request, Set<Variable> groupVariables,
            Map<Variable, SqlExpressionIntercode> aggregations, SqlIntercode child)
    {
        return aggregate(request, groupVariables, aggregations, child, null);
    }


    /**
     * Aggregation of the child restricted to the parent's needs; a result variable is deterministic only if its
     * aggregate is.
     *
     * @param request the current request
     * @param groupVariables the grouping variables
     * @param aggregations aggregate expression of each result variable
     * @param child the child node
     * @param restrictions what the parent needs of the variables
     * @return aggregation of the child restricted to the parent's needs; a result variable is deterministic only if its
     *         aggregate is
     */
    protected static SqlIntercode aggregate(Request request, Set<Variable> groupVariables,
            Map<Variable, SqlExpressionIntercode> aggregations, SqlIntercode child, Restrictions restrictions)
    {
        ClassRelations relations = request.getConfiguration();

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

        bindings = bindings.restrict(relations, restrictions);

        boolean isDeterministic = child.isDeterministic();

        for(Entry<Variable, SqlExpressionIntercode> entry : aggregations.entrySet())
            if(restrictions == null || restrictions.containsVar(entry.getKey()))
                isDeterministic &= entry.getValue().isDeterministic();

        return new SqlAggregation(bindings, isDeterministic, groupVariables, aggregations, child);
    }


    @Override
    public SqlIntercode optimize(Request request, Scope scope, Restrictions restrictions, boolean reduced,
            boolean evalServices)
    {
        ClassRelations relations = request.getConfiguration();

        SqlIntercode optChild = child;
        Map<Variable, SqlExpressionIntercode> optAggregations = aggregations;

        boolean childReduce = optAggregations.values().stream()
                .allMatch(a -> a instanceof SqlBuiltinCall c && c.isDistinct());

        Restrictions childRestrictions = getChildRestrictions(relations, groupVariables, optAggregations, restrictions);

        while(true)
        {
            optChild = optChild.optimize(request, scope, childRestrictions, childReduce, evalServices);
            optAggregations = optimizeAggregations(request, scope, optAggregations, optChild, restrictions,
                    evalServices);

            boolean newChildReduce = optAggregations.values().stream()
                    .allMatch(a -> a instanceof SqlBuiltinCall c && c.isDistinct());

            Restrictions newChildRestrictions = getChildRestrictions(relations, groupVariables, optAggregations,
                    restrictions);

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
                    Map<Variable, SqlExpressionIntercode> aggregations = optimizeAggregations(request, scope,
                            optAggregations, child, restrictions, evalServices);
                    childs.add(aggregate(request, groupVariables, aggregations, child));
                }

                return SqlUnion.union(request, childs).optimize(request, scope, restrictions, true, evalServices);
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

                    SqlExpressionIntercode card = SqlVariable.create(relations,
                            aggregate.getVariable(new Variable("@card")));
                    SqlExpressionIntercode factor = SqlLiteral.create(request,
                            new TypedLiteral(count.toString(), xsdIntegerIri));
                    SqlExpressionIntercode expression = SqlBinaryArithmetic.create(relations, MULTIPLY, factor, card);

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
                    .of(SqlVariable.create(relations, optUnion.getVariable(new Variable("@bind"))));
            SqlExpressionIntercode expr = SqlBuiltinCall.create(request, "sum", false, args);

            Map<Variable, SqlExpressionIntercode> outerAggregations = new LinkedHashMap<>();
            outerAggregations.put(optAggregations.keySet().iterator().next(), expr);

            return aggregate(request, groupVariables, outerAggregations, optUnion).optimize(request, scope,
                    restrictions, reduced, evalServices);
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
            List<SqlExpressionIntercode> args = List
                    .of(SqlVariable.create(relations, child.getVariable(var.getVariable())));

            Map<Variable, SqlExpressionIntercode> subAggregations = Map.of(optAggregations.keySet().iterator().next(),
                    SqlBuiltinCall.create(request, "count", false, args));

            return aggregate(request, groupVariables, subAggregations, child).optimize(request, scope, restrictions,
                    reduced, evalServices);
        }


        /* implicit group with eliminated aggregates */
        if(groupVariables.isEmpty() && optAggregations.values().stream()
                .noneMatch(r -> r instanceof SqlBuiltinCall c && c.isAggregateFunction()))
        {
            SqlIntercode result = SqlEmptySolution.get();

            for(Entry<Variable, SqlExpressionIntercode> entry : optAggregations.entrySet())
                if(!entry.getValue().equals(SqlNull.get()))
                    result = SqlBind.bind(request, entry.getKey(), entry.getValue(), result);

            return result.optimize(request, scope, restrictions, reduced, evalServices);
        }


        if(optAggregations.equals(aggregations) && optChild == child && restrictions.isOptimized(relations, bindings))
            return this;

        return aggregate(request, groupVariables, optAggregations, optChild, restrictions);
    }


    /**
     * Optimises the aggregates over the child's bindings, drops those the parent does not need, and turns {@code
     * COUNT(x)} of a never-null argument and {@code COUNT(DISTINCT key)} over a table key into a plain row count.
     *
     * @param request the current request
     * @param scope the scope of the aggregation
     * @param aggregations aggregate expression of each result variable
     * @param child the child node
     * @param restrictions what the parent needs of the variables
     * @param evalServices whether SERVICE stubs are evaluated
     * @return the optimised aggregates by result variable
     */
    private Map<Variable, SqlExpressionIntercode> optimizeAggregations(Request request, Scope scope,
            Map<Variable, SqlExpressionIntercode> aggregations, SqlIntercode child, Restrictions restrictions,
            boolean evalServices)
    {
        DatabaseSchema schema = request.getConfiguration().getDatabaseSchema();

        LinkedHashMap<Variable, SqlExpressionIntercode> opt = new LinkedHashMap<>();

        for(Entry<Variable, SqlExpressionIntercode> e : aggregations.entrySet())
        {
            SqlExpressionIntercode optExpr = e.getValue().optimize(request, scope, child.getVariableBindings(),
                    restrictions.getRestriction(e.getKey()), evalServices);

            // the parent may need a result in no class (a join comparing it with classes it never takes), and it has
            // to stay bound then, as its classes decide whether the parent finds a solution; a result never bound is
            // the same as one not bound at all
            if(restrictions.containsVar(e.getKey()) && !optExpr.equals(SqlNull.get()))
                opt.put(e.getKey(), optExpr);
        }


        /* change count(var) on count(*) if possible  */
        for(Map.Entry<Variable, SqlExpressionIntercode> entry : opt.entrySet())
            if(entry.getValue() instanceof SqlBuiltinCall call && call.getFunction().equals("count")
                    && !call.isDistinct() && !call.getArguments().get(0).canBeNull())
                opt.put(entry.getKey(), SqlBuiltinCall.create(request, "card", false, List.of()));


        /* change count(distinct var) on count(*) if possible; a key does not make rows with NULL key values unique */
        if(child instanceof SqlTableAccess tab && tab.getTable() != null)
            for(Map.Entry<Variable, SqlExpressionIntercode> entry : opt.entrySet())
                if(entry.getValue() instanceof SqlBuiltinCall call && call.getFunction().equals("count")
                        && call.isDistinct() && call.getArguments().get(0) instanceof SqlVariable var
                        && var.getBinding().getNonConstantColumns().stream()
                                .allMatch(c -> tab.isKnownNotNull(schema, c))
                        && schema.isKey(tab.getTable(), var.getBinding().getNonConstantColumns()))
                    opt.put(entry.getKey(), SqlBuiltinCall.create(request, "card", false, List.of()));

        return opt;
    }


    /**
     * Restrictions for the child: the grouping variables plus the requirements of the needed aggregates. A grouping
     * variable keeps the classes the parent accepts: the groups keyed by values of other classes are dropped above as a
     * whole, so the child may leave their solutions out, whereas the solutions behind an aggregate have to stay.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param groupVariables the grouping variables
     * @param aggregations aggregate expression of each result variable
     * @param restrictions what the parent needs of the variables
     * @return restrictions for the child: the grouping variables plus the requirements of the needed aggregates
     */
    private static Restrictions getChildRestrictions(ClassRelations relations, Set<Variable> groupVariables,
            Map<Variable, SqlExpressionIntercode> aggregations, Restrictions restrictions)
    {
        Restrictions childRestrictions = new Restrictions(groupVariables);

        for(Variable variable : groupVariables)
            if(restrictions.getAcceptedClasses(variable) != null)
                childRestrictions.addAcceptedClasses(variable, restrictions.getAcceptedClasses(variable));

        // the variables of a needed aggregate stay bound even where none of their values is needed, as they give the
        // classes of the aggregate
        for(Entry<Variable, SqlExpressionIntercode> entry : aggregations.entrySet())
        {
            if(restrictions.containsVar(entry.getKey()))
            {
                childRestrictions.add(entry.getValue().getRequirements(relations));

                for(Variable referenced : entry.getValue().getReferencedVariables())
                    childRestrictions.add(referenced, Set.of());
            }
        }

        return childRestrictions;
    }


    @Override
    public SqlCode translate(Request request, Scope scope)
    {
        Set<Column> groupByColumns = new HashSet<>();

        for(Variable var : groupVariables)
            if(child.getVariableBindings().get(var) != null)
                groupByColumns.addAll(child.getVariableBindings().get(var).getNonConstantColumns());


        SqlBuilder builder = new SqlBuilder();

        builder.append("SELECT ");
        boolean hasSelect = false;

        // whether a selected value aggregates the solutions, which makes them a single group without a GROUP BY
        boolean aggregated = false;

        for(Entry<Variable, SqlExpressionIntercode> entry : aggregations.entrySet())
        {
            Variable var = entry.getKey();
            SqlExpressionIntercode expression = entry.getValue();
            VariableBinding binding = getVariableBindings().get(var);

            for(Entry<ResourceClass, List<Column>> e : expression.getBinding().getMappings().entrySet())
            {
                ResourceClass resClass = e.getKey();
                List<Column> names = binding.getMapping(resClass);

                // a class the parent needs no value of is not computed (a result may stay bound only for its classes)
                if(names == null)
                    continue;

                List<Column> cols = expression.get(request.getConfiguration(), resClass);
                aggregated |= expression instanceof SqlBuiltinCall c && c.isAggregateFunction();

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
            builder.append(groupByColumns.stream().collect(SqlCode.joining(", ")));
        }

        if(!hasSelect)
            builder.append("1");


        builder.append(" FROM (");
        builder.append(child.translate(request, scope));
        builder.append(" ) AS ");
        builder.appendAlias(scope.getInputTable());

        if(!groupByColumns.isEmpty())
            builder.append(groupByColumns.stream().collect(SqlCode.joining(", ", " GROUP BY ", "")));
        else if(!groupVariables.isEmpty())
            builder.append(" GROUP BY true::boolean");
        else if(!aggregated)
            builder.append(" GROUP BY ()");


        return builder.toCode();
    }


    @Override
    public boolean hasServiceSubpattern()
    {
        return child.hasServiceSubpattern() || aggregations.values().stream().anyMatch(a -> a.hasServiceSubpattern());
    }


    @Override
    public String getCollation(Request request, Variable variable, ResourceClass resClass, int index)
    {
        return getCollation(request, List.of(child), variable, resClass, index);
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
