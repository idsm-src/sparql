package cz.iocb.sparql.engine.imcode;

import static cz.iocb.sparql.engine.imcode.expression.SqlBinaryArithmetic.ArithmeticOperator.MULTIPLY;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerIri;
import static java.util.stream.Collectors.joining;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import cz.iocb.sparql.engine.config.EngineSettings;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.Condition;
import cz.iocb.sparql.engine.database.Conditions;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.SqlBuilder;
import cz.iocb.sparql.engine.database.SqlCode;
import cz.iocb.sparql.engine.database.SqlType;
import cz.iocb.sparql.engine.imcode.expression.SqlBinaryArithmetic;
import cz.iocb.sparql.engine.imcode.expression.SqlBuiltinCall;
import cz.iocb.sparql.engine.imcode.expression.SqlExpressionIntercode;
import cz.iocb.sparql.engine.imcode.expression.SqlLiteral;
import cz.iocb.sparql.engine.imcode.expression.SqlNull;
import cz.iocb.sparql.engine.imcode.expression.SqlVariable;
import cz.iocb.sparql.engine.mapping.classes.ClassRelations;
import cz.iocb.sparql.engine.mapping.classes.InternalResourceClass;
import cz.iocb.sparql.engine.mapping.classes.IriClass;
import cz.iocb.sparql.engine.mapping.classes.IriClass.OrderTable;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.TypedLiteral;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.VariableBinding;
import cz.iocb.sparql.engine.translator.VariableBindings;



/**
 * GROUP BY over the group variables with the aggregate expressions computed per group; without group variables all
 * solutions form one group. The solutions of a join that multiplies the solutions of its children are counted by the
 * counts of the children (see {@link #countJoin}).
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

        // a grouping variable can be grouped over by a node standing in for its term, which the child is asked for as
        // well, because the solutions may come to bind it always only once they are optimised
        getGroupNodes(request, optAggregations, restrictions).values().forEach(childRestrictions::add);

        while(true)
        {
            optChild = optChild.optimize(request, scope, childRestrictions, childReduce, evalServices);
            optAggregations = optimizeAggregations(request, scope, optAggregations, optChild, restrictions,
                    evalServices);

            boolean newChildReduce = optAggregations.values().stream()
                    .allMatch(a -> a instanceof SqlBuiltinCall c && c.isDistinct());

            Restrictions newChildRestrictions = getChildRestrictions(relations, groupVariables, optAggregations,
                    restrictions);
            getGroupNodes(request, optAggregations, restrictions).values().forEach(newChildRestrictions::add);

            if(newChildReduce == childReduce && newChildRestrictions.equals(childRestrictions))
                break;

            childReduce = newChildReduce;
            childRestrictions = newChildRestrictions;
        }

        // no solutions make no group of a GROUP BY clause, whereas without the clause they make the one group there is
        if(optChild.equals(SqlNoSolution.get()) && !groupVariables.isEmpty())
            return SqlNoSolution.get();

        // MIN and MAX compare the IRIs of a class whose order column a table holds by the order column that an
        // OPTIONAL of the solutions reads from the table, instead of looking it up for every solution
        SqlIntercode ordered = readOrderColumns(request, scope, optAggregations, optChild);

        if(ordered != null)
            return ordered.optimize(request, scope, restrictions, reduced, evalServices);

        // the solutions binding a grouping variable and a node standing in for its term always make the same groups
        // whether grouped over the variable or over the node, and the child grouped over the node reads no term; the
        // terms the parent needs are read for the groups
        Map<Variable, Variable> standIns = getStandIns(optChild, getGroupNodes(request, optAggregations, restrictions));

        if(!standIns.isEmpty())
        {
            Set<Variable> replaced = new HashSet<>(groupVariables);
            replaced.removeAll(standIns.keySet());
            replaced.addAll(standIns.values());

            return readTerms(request, aggregate(request, replaced, optAggregations, optChild), standIns, restrictions)
                    .optimize(request, scope, restrictions, reduced, evalServices);
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


        /* count the solutions of a join multiplying the solutions of its children by the counts of the children */
        SqlIntercode counted = countJoin(request, optAggregations, optChild);

        if(counted != null)
            return counted.optimize(request, scope, restrictions, reduced, evalServices);


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

            // a variable bound always is counted by the deduplicated solutions, which then need not give its term
            Map<Variable, SqlExpressionIntercode> subAggregations = Map.of(optAggregations.keySet().iterator().next(),
                    var.canBeNull() ? SqlBuiltinCall.create(request, "count", false, args) :
                            SqlBuiltinCall.create(request, "card", false, List.of()));

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
     * The aggregation counting the solutions of a join by the counts of the solutions of its children (an eager
     * aggregation, which PostgreSQL makes only below a GROUP BY and only when its estimates prefer it), or null when
     * the count is not made so. A solution of the join is a combination of solutions of its children compatible with
     * each other, whether two solutions are compatible depends only on the values they give the variables their
     * children share (unbound ones included), and the solutions of a group agree on the grouping variables, so the
     * number of the solutions of a group is the sum, over the compatible combinations of those values, of the products
     * of the numbers of the solutions of each child giving them. A child is therefore counted grouped by its variables
     * that another child binds as well and by the grouping variables it binds, the counted children are joined with the
     * others, and the products of their counts are summed per group: the join of 28 and 20 million solutions sharing
     * 41544 values of a variable, which has 4.09e10 solutions, was counted in 0.71 s instead of 311 s. Only a child
     * whose solutions the grouping is estimated to reduce is counted, because the children bind only what the join
     * needs of them, the variables they share and the grouping variables, so a child may be grouped by all it binds,
     * which leaves solutions without duplicates as they are (the triples of a property joined with the classes of their
     * subjects and of their objects and counted per pair of the classes); any other child is joined as it is, each of
     * its solutions counting once. The aggregates have to count the solutions (COUNT(*)), the join has to be made of
     * deterministic children holding no SERVICE call, each binding a variable to group it by, as a child counted
     * without grouping makes a group of no solutions, some child has to be counted, and the join has to be estimated to
     * have as many times more solutions than the counted children together with the join of their counts as the
     * settings of the engine say (see {@link EngineSettings#countedJoinRatio()}).
     *
     * @param request the current request
     * @param aggregations aggregate expression of each result variable
     * @param child the child node
     * @return the aggregation counting the solutions of the join by the counts of its children, or null
     */
    private SqlIntercode countJoin(Request request, Map<Variable, SqlExpressionIntercode> aggregations,
            SqlIntercode child)
    {
        ClassRelations relations = request.getConfiguration();

        if(aggregations.isEmpty() || !aggregations.values().stream().allMatch(
                a -> a instanceof SqlBuiltinCall call && call.getFunction().equals("card") && !call.isDistinct()))
            return null;

        if(!(child instanceof SqlJoin join) || !join.isDeterministic() || join.hasServiceSubpattern())
            return null;

        List<SqlIntercode> childs = join.getChilds();
        List<SqlIntercode> joinedChilds = new ArrayList<>();
        List<Variable> counts = new ArrayList<>();

        // the solutions the counted children are grouped from
        double grouped = 0;

        for(int i = 0; i < childs.size(); i++)
        {
            SqlIntercode member = childs.get(i);
            Set<Variable> keys = new LinkedHashSet<>();

            for(Variable variable : member.getVariableBindings().getVariables())
            {
                if(groupVariables.contains(variable))
                    keys.add(variable);

                for(int j = 0; j < childs.size(); j++)
                    if(j != i && childs.get(j).getVariable(variable) != null)
                        keys.add(variable);
            }

            if(keys.isEmpty())
                return null;

            Variable count = getCountVariable(i);
            SqlIntercode counted = aggregate(request, keys,
                    Map.of(count, SqlBuiltinCall.create(request, "card", false, List.of())), member);
            double rows = JoinOptimizer.estimateRows(request, member);

            // a child that the grouping is not estimated to reduce is joined as it is, each solution counting once
            if(JoinOptimizer.estimateRows(request, counted) < rows)
            {
                joinedChilds.add(counted);
                counts.add(count);
                grouped += rows;
            }
            else
            {
                joinedChilds.add(member);
            }
        }

        if(counts.isEmpty())
            return null;

        // the counted children are each read and grouped once and their counts joined with the other children, whereas
        // the join hands every solution it makes to the count, so the counts pay only where the join multiplies the
        // solutions of the counted children; the join of the counts is planned only where they may pay
        double solutions = JoinOptimizer.estimateRows(request, join);
        double ratio = request.getEngineSettings().countedJoinRatio();

        if(solutions < ratio * Math.max(grouped, 1))
            return null;

        SqlIntercode joined = SqlJoin.join(request, joinedChilds);

        if(solutions < ratio * Math.max(grouped + JoinOptimizer.estimateRows(request, joined), 1))
            return null;

        SqlExpressionIntercode product = null;

        for(Variable count : counts)
        {
            SqlExpressionIntercode value = SqlVariable.create(relations, joined.getVariable(count));
            product = product == null ? value : SqlBinaryArithmetic.create(relations, MULTIPLY, product, value);
        }

        Variable productVariable = new Variable("@product");
        SqlIntercode multiplied = SqlBind.bind(request, productVariable, product, joined);
        SqlExpressionIntercode sum = SqlBuiltinCall.create(request, "sum", false,
                List.of(SqlVariable.create(relations, multiplied.getVariable(productVariable))));

        Map<Variable, SqlExpressionIntercode> sums = new LinkedHashMap<>();

        for(Variable variable : aggregations.keySet())
            sums.put(variable, sum);

        return aggregate(request, groupVariables, sums, multiplied);
    }


    /**
     * The variable holding the count of the solutions of a child of a join counted by the counts of its children (see
     * {@link #countJoin}).
     *
     * @param index the index of the child
     * @return the variable holding the count of the solutions of the child
     */
    private static Variable getCountVariable(int index)
    {
        return new Variable("@count" + index);
    }


    /**
     * The nodes that the solutions may be grouped over in place of the grouping variables (see
     * {@link #getStandInNodes}): those of the grouping variables that no aggregate refers to, as an aggregate reads the
     * terms of the solutions of a group.
     *
     * @param request the current request
     * @param aggregations aggregate expression of each result variable
     * @param restrictions what the parent needs of the variables
     * @return the nodes that can stand in for each grouping variable that has some
     */
    private Map<Variable, List<Variable>> getGroupNodes(Request request,
            Map<Variable, SqlExpressionIntercode> aggregations, Restrictions restrictions)
    {
        Set<Variable> keys = new HashSet<>(groupVariables);

        for(SqlExpressionIntercode aggregation : aggregations.values())
            keys.removeAll(aggregation.getReferencedVariables());

        return getStandInNodes(request, keys, restrictions);
    }


    /**
     * The aggregation of the solutions extended by the order column of the IRIs that MIN and MAX compare, read from the
     * table holding it (see {@link IriClass#getOrderTable}), or null when no aggregate compares such IRIs. For a
     * variable that the solutions bind always, in a single IRI class with such a table, and not to a constant, where
     * the table holds a value in a single row at most, an OPTIONAL of the solutions reads the order column from the row
     * of the table holding the value into an internal variable, which the aggregates of the variable take as their
     * second argument, giving the IRI of the least or the greatest order column (see {@link SqlBuiltinCall}). The
     * OPTIONAL is merged into an access to the table that the solutions are made of, which then reads the order column
     * from the rows it reads anyway (see {@link SqlLeftJoin}), and the database joins the table otherwise, whereas a
     * lookup of the order column of every value is a correlated subquery, which it evaluates row by row and never in
     * parallel workers. A value that no row holds has no order column, as a lookup finds none for it.
     *
     * @param request the current request
     * @param scope the scope of the aggregation
     * @param aggregations aggregate expression of each result variable
     * @param child the child node
     * @return the aggregation of the solutions extended by the order column of the compared IRIs, or null
     */
    private SqlIntercode readOrderColumns(Request request, Scope scope,
            Map<Variable, SqlExpressionIntercode> aggregations, SqlIntercode child)
    {
        ClassRelations relations = request.getConfiguration();
        DatabaseSchema schema = request.getConfiguration().getDatabaseSchema();

        if(schema == null)
            return null;

        Map<Variable, Variable> keys = new HashMap<>();
        SqlIntercode solutions = child;

        for(SqlExpressionIntercode aggregation : aggregations.values())
        {
            Variable variable = getComparedVariable(aggregation);
            VariableBinding binding = variable == null ? null : child.getVariable(variable);

            // a solution leaving the variable unbound would be compatible with every row of the table
            if(binding == null || binding.canBeNull() || keys.containsKey(variable) || binding.getClasses().size() != 1
                    || binding.getNonConstantColumns().isEmpty())
                continue;

            ResourceClass resClass = binding.getClasses().iterator().next();
            IriClass iriClass = resClass.getEffectiveClass() instanceof IriClass c ? c : null;
            OrderTable table = iriClass != null ? iriClass.getOrderTable() : null;
            Variable key = new Variable("@order_" + variable.getName());

            // a value held by several rows would make several solutions of one
            if(table == null || !schema.isKey(table.table(), new HashSet<>(table.values()))
                    || child.getVariable(key) != null)
                continue;

            SqlIntercode access = getOrderAccess(request, table, variable, resClass, key);

            solutions = SqlLeftJoin.leftJoin(request, scope, solutions, access, List.of(), false, null);
            keys.put(variable, key);
        }

        if(keys.isEmpty())
            return null;

        Map<Variable, SqlExpressionIntercode> extended = new LinkedHashMap<>();

        for(Entry<Variable, SqlExpressionIntercode> entry : aggregations.entrySet())
        {
            Variable variable = getComparedVariable(entry.getValue());

            if(keys.containsKey(variable) && entry.getValue() instanceof SqlBuiltinCall call)
            {
                List<SqlExpressionIntercode> arguments = List.of(
                        SqlVariable.create(relations, solutions.getVariable(variable)),
                        SqlVariable.create(relations, solutions.getVariable(keys.get(variable))));

                extended.put(entry.getKey(),
                        SqlBuiltinCall.create(request, call.getFunction(), call.isDistinct(), arguments));
            }
            else
            {
                extended.put(entry.getKey(), entry.getValue());
            }
        }

        return aggregate(request, groupVariables, extended, solutions);
    }


    /**
     * The variable whose values MIN or MAX compares by the order columns computed from them, or null when the aggregate
     * is another one, or one of something else than a variable, or one comparing the order column read along with the
     * values already.
     *
     * @param aggregation the aggregate expression
     * @return the variable whose values the aggregate compares by the order columns computed from them, or null
     */
    private static Variable getComparedVariable(SqlExpressionIntercode aggregation)
    {
        return aggregation instanceof SqlBuiltinCall call
                && (call.getFunction().equals("min") || call.getFunction().equals("max"))
                && call.getArguments().size() == 1 && call.getArguments().get(0) instanceof SqlVariable argument ?
                        argument.getVariable() : null;
    }


    /**
     * Access to the table holding the order column of the IRIs of a class (see {@link IriClass#getOrderTable}) that
     * binds the variable to the values of the class the rows hold, and the internal variable to the order column of the
     * IRIs of the values, in a class of its own; a row holding a null in their columns is left out.
     *
     * @param request the current request
     * @param table the table holding the order column
     * @param variable the variable of the values
     * @param resClass the class the solutions bind the variable in
     * @param key the internal variable of the order column
     * @return the access to the table holding the order column
     */
    private static SqlIntercode getOrderAccess(Request request, OrderTable table, Variable variable,
            ResourceClass resClass, Variable key)
    {
        DatabaseSchema schema = request.getConfiguration().getDatabaseSchema();
        Condition condition = new Condition();

        for(Column column : Stream.concat(table.values().stream(), Stream.of(table.order())).toList())
            if(schema.isNullableColumn(table.table(), column))
                condition.addIsNotNull(column);

        VariableBindings bindings = new VariableBindings();
        bindings.add(new VariableBinding(variable, resClass, table.values(), false));
        bindings.add(new VariableBinding(key, new InternalResourceClass(List.of(table.order().getType())),
                List.of(table.order()), false));

        return SqlTableAccess.create(request, table.table(), new Conditions(condition), bindings);
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
     * variable keeps the classes and the value the parent accepts: the groups keyed by other values are dropped above
     * as a whole, so the child may leave their solutions out, whereas the solutions behind an aggregate have to stay.
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
        {
            if(restrictions.getAcceptedClasses(variable) != null)
                childRestrictions.addAcceptedClasses(variable, restrictions.getAcceptedClasses(variable));

            if(restrictions.getAcceptedValue(variable) != null)
                childRestrictions.setAcceptedValue(variable, restrictions.getAcceptedValue(variable));

            if(restrictions.getAcceptedTerms(variable) != null)
                childRestrictions.addAcceptedTerms(variable, restrictions.getAcceptedTerms(variable));

            for(SqlExpressionIntercode condition : restrictions.getFilterConditions(variable))
                childRestrictions.addFilterCondition(variable, condition);
        }

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


    /**
     * The aggregation of its child that has taken the condition in: the solutions of a group agree on the grouping
     * variables, so a condition they decide holds for a group exactly when it holds for the solutions the group is made
     * of, whereas an aggregate and a variable of the child that is not grouped by are no variables of those solutions.
     * Without grouping variables the solutions form one group even when there is none of them, which a filter of the
     * child would not drop.
     */
    @Override
    protected SqlIntercode sink(Request request, Scope scope, SqlExpressionIntercode condition,
            VariableBindings bindings)
    {
        if(groupVariables.isEmpty())
            return null;

        for(Variable variable : getDecidingVariables(request.getConfiguration(), condition))
            if(!groupVariables.contains(variable)
                    && (aggregations.containsKey(variable) || child.getVariable(variable) != null))
                return null;

        SqlIntercode sunk = child.sink(request, scope, condition, bindings);

        return sunk == null ? null : aggregate(request, groupVariables, aggregations, sunk);
    }


    @Override
    public SqlCode translate(Request request, Scope scope)
    {
        return translate(request, scope, null);
    }


    /**
     * The rows the child is evaluated for when the grouping is evaluated for the given rows: a grouping variable
     * restricts the child as it restricts the groups, because the solutions of one value of it make groups of their
     * own, which no other solution belongs to.
     *
     * @param outer the rows the grouping is evaluated for one by one
     * @return the rows the child is evaluated for one by one, or null when it is evaluated whole
     */
    JoinOptimizer.Outer getChildRows(JoinOptimizer.Outer outer)
    {
        return outer.retain(groupVariables);
    }


    /**
     * The grouping of the child evaluated for the row of the outer table, as far as the grouping variables restrict it
     * (see {@link #getChildRows}).
     */
    @Override
    SqlCode translateForRow(Request request, Scope scope, JoinOptimizer.Outer outer)
    {
        return translate(request, scope, getChildRows(outer));
    }


    /**
     * SQL code of the grouping, evaluated once or for one of the rows of an outer table (see {@link #translateForRow}).
     *
     * @param request the current request
     * @param scope the scope of the grouping
     * @param outer the rows the child is evaluated for one by one, or null when it is evaluated once
     * @return SQL code of the grouping
     */
    private SqlCode translate(Request request, Scope scope, JoinOptimizer.Outer outer)
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
        builder.append(outer == null ? child.translate(request, scope) : child.translateForRow(request, scope, outer));
        builder.append(" ) AS ");
        builder.appendAlias(scope.getInputTable());

        if(!groupByColumns.isEmpty())
            builder.append(" GROUP BY ").append(SqlType.getGroupingKeys(groupByColumns));
        else if(!groupVariables.isEmpty())
            builder.append(" GROUP BY true::boolean");
        else if(!aggregated)
            builder.append(" GROUP BY ()");


        return builder.toCode();
    }


    /**
     * Solutions being grouped.
     *
     * @return solutions being grouped
     */
    public final SqlIntercode getChild()
    {
        return child;
    }


    /**
     * Grouping variables.
     *
     * @return grouping variables
     */
    public final Set<Variable> getGroupVariables()
    {
        return groupVariables;
    }


    /**
     * Every group gives a single solution (as Ontop takes the grouping variables of an aggregation for its unique
     * constraint), so the solutions are distinct over the selected variables when they hold every grouping variable
     * that the groups tell apart by columns and that the solutions keep; a grouping variable without such columns has
     * the same value in every group.
     */
    @Override
    public boolean isDistinct(Request request, Collection<Variable> selected)
    {
        for(Variable variable : groupVariables)
        {
            VariableBinding binding = child.getVariableBindings().get(variable);

            if(binding != null && !binding.getNonConstantColumns().isEmpty()
                    && (!selected.contains(variable) || getVariableBindings().get(variable) == null))
                return false;
        }

        return true;
    }


    @Override
    protected boolean isRestricted(Variable variable, SqlExpressionIntercode value)
    {
        //NOTE: a group binds a grouping variable to the value its solutions bind it to
        return groupVariables.contains(variable) && child.isRestricted(variable, value);
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
