package cz.iocb.sparql.engine.imcode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.Condition.ColumnComparison;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.database.TableStatistics;
import cz.iocb.sparql.engine.imcode.expression.SqlBinaryComparison;
import cz.iocb.sparql.engine.imcode.expression.SqlBinaryLogical;
import cz.iocb.sparql.engine.imcode.expression.SqlBinaryLogical.LogicalOperator;
import cz.iocb.sparql.engine.imcode.expression.SqlBuiltinCall;
import cz.iocb.sparql.engine.imcode.expression.SqlExpressionIntercode;
import cz.iocb.sparql.engine.imcode.expression.SqlInExpression;
import cz.iocb.sparql.engine.imcode.expression.SqlUnary;
import cz.iocb.sparql.engine.imcode.expression.SqlUnaryLogical;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.VariableBinding;



/**
 * Chooses the tree and the methods of the joins of the children of a {@link SqlJoin} from the statistics the engine
 * keeps of the tables (see {@link TableStatistics}), for the planner of the database to keep them (see
 * {@link SparqlDatabaseConfiguration#setJoinOptimizer}): the planner estimates every join of a table holding many kinds
 * of rows at once, such as a table of all the triples of a dataset, at one row, and then nests the joins in a wrong
 * order. The size of a join of some of the children is estimated independently of the order: the product of the sizes
 * of the children, divided for every variable they share by the distinct values of the variable in each of them but the
 * one with the fewest, so that the children sharing a variable are joined on it like a key. A join of two sides costs
 * the hashing of the smaller side and the probing of the other, or the pairwise comparison of the sides when no
 * equality can be hashed (a variable that may be unbound, or bound in several classes, on a side compares by a
 * disjunction); a child that is a table access looked up by an index of a variable the other side binds may be joined
 * by an index nested loop instead, costing a lookup per outer row. The tree that minimises the sum of the costs and of
 * the sizes of the intermediate results is found by dynamic programming over the connected subsets of the children, up
 * to {@link #EXHAUSTIVE_LIMIT} of them, and greedily above that. The costs are in units of one row scanned or hashed,
 * calibrated on the NeXtProt queries over a single table of triples. A filter above the join counts as well: a
 * condition that one variable decides alone reduces the child the join takes the variable from (see
 * {@link SqlIntercode.Restrictions#getFilterConditions}), because the database evaluates it there.
 */
final class JoinOptimizer
{
    /**
     * Cost of the lookup of the rows matching one outer row by an index, in units of one row scanned or hashed.
     */
    static final double LOOKUP_COST = 8;

    /**
     * Cost of hashing one row of the smaller side of a hash join; the probe of one row of the other side costs 1.
     */
    static final double HASH_COST = 2.5;

    /**
     * Cost of comparing one pair of rows when the sides have to be compared pairwise.
     */
    static final double COMPARISON_COST = 0.3;

    /**
     * Rows of a child nothing is known about.
     */
    static final double DEFAULT_ROWS = 1000;

    /**
     * Selectivity of an equality in a filter (the default of the planner).
     */
    static final double EQUALITY_SELECTIVITY = 0.005;

    /**
     * Selectivity of an inequality in a filter (the default of the planner).
     */
    static final double INEQUALITY_SELECTIVITY = 1.0 / 3;

    /**
     * Selectivity of a filter condition nothing is known about.
     */
    static final double DEFAULT_SELECTIVITY = 0.5;

    /**
     * Largest number of children whose join trees are enumerated exhaustively.
     */
    static final int EXHAUSTIVE_LIMIT = 15;


    /**
     * A node of the join tree.
     */
    sealed interface Node permits Leaf, Lookup, HashJoin
    {
    }


    /**
     * A child by itself.
     *
     * @param child index of the child
     */
    record Leaf(int child) implements Node
    {
    }


    /**
     * A table access looked up by an index for every row of the outer tree.
     *
     * @param outer the outer tree
     * @param child index of the child looked up
     */
    record Lookup(Node outer, int child) implements Node
    {
    }


    /**
     * Two sides joined by hashing (or compared pairwise when none of their equalities can be hashed).
     *
     * @param left the left side
     * @param right the right side
     */
    record HashJoin(Node left, Node right) implements Node
    {
    }


    /**
     * The join tree chosen for the children with the estimated number of rows of its result.
     *
     * @param root the root of the tree
     * @param rows estimated number of rows of the result
     */
    record Plan(Node root, double rows)
    {
    }


    /**
     * A variable shared by several children.
     *
     * @param variable the variable
     * @param mask the children binding the variable
     * @param distinct estimated distinct values of the variable in each child (unused entries for the others)
     * @param hashable whether the equality of the variable can be hashed: every child binding it binds it always and in
     *            a single class
     */
    private record Shared(Variable variable, int mask, double[] distinct, boolean hashable)
    {
    }


    /**
     * Not instantiated.
     */
    private JoinOptimizer()
    {
    }


    /**
     * Describes a join tree for an explanation: a child by its index, a lookup as {@code (outer -> child)} and a hash
     * join as {@code (left + right)}.
     *
     * @param node the tree
     * @return description of the tree
     */
    static String describe(Node node)
    {
        if(node instanceof Leaf leaf)
            return String.valueOf(leaf.child());

        if(node instanceof Lookup lookup)
            return "(" + describe(lookup.outer()) + " -> " + lookup.child() + ")";

        HashJoin join = (HashJoin) node;

        return "(" + describe(join.left()) + " + " + describe(join.right()) + ")";
    }


    /**
     * Chooses the join tree of the children whose solutions the filters above the join keep by the given conditions.
     * Such a condition is decided by one variable alone, so the database evaluates it where the variable is bound,
     * before the joins: the child the join takes the variable from gives the joins only the fraction of its solutions
     * the condition is estimated to keep (see {@link #selectivity}), and the tree is chosen for the children so
     * reduced. The estimated rows of the plan are those of the join itself, as the filters are no part of it.
     *
     * @param request the current request
     * @param childs the children
     * @param conditions per variable, the conditions the filters above keep the solutions of the join by
     * @return the join tree with the estimated rows of the join
     */
    static Plan plan(Request request, List<SqlIntercode> childs, Map<Variable, Set<SqlExpressionIntercode>> conditions)
    {
        double[] unfiltered = new double[childs.size()];
        double[] kept = new double[childs.size()];

        Arrays.fill(unfiltered, 1);
        Arrays.fill(kept, 1);

        for(Map.Entry<Variable, Set<SqlExpressionIntercode>> entry : conditions.entrySet())
        {
            int child = getDecidingChild(childs, entry.getKey());

            if(child != -1)
                for(SqlExpressionIntercode condition : entry.getValue())
                    kept[child] *= selectivity(condition);
        }

        Plan plan = plan(request, childs, kept);

        return Arrays.equals(kept, unfiltered) ? plan : new Plan(plan.root(), plan(request, childs, unfiltered).rows());
    }


    /**
     * The child the join takes the values of the variable from, so that a condition on the variable above the join is
     * evaluated on the solutions of that child: the first child binding the variable always, or the first one binding
     * it at all; -1 when no child binds the variable.
     *
     * @param childs the children
     * @param variable the variable
     * @return index of the child the join takes the values of the variable from, or -1
     */
    private static int getDecidingChild(List<SqlIntercode> childs, Variable variable)
    {
        int nullable = -1;

        for(int i = 0; i < childs.size(); i++)
        {
            VariableBinding binding = childs.get(i).getVariable(variable);

            if(binding != null && !binding.canBeNull())
                return i;

            if(binding != null && nullable == -1)
                nullable = i;
        }

        return nullable;
    }


    /**
     * Chooses the join tree of the children, each giving the joins the given fraction of its solutions.
     *
     * @param request the current request
     * @param childs the children
     * @param kept the fraction of its solutions each child gives the joins
     * @return the join tree with its estimated rows
     */
    private static Plan plan(Request request, List<SqlIntercode> childs, double[] kept)
    {
        int n = childs.size();

        double[] rows = new double[n];

        for(int i = 0; i < n; i++)
            rows[i] = Math.max(estimateRows(request, childs.get(i)) * kept[i], 1);


        Map<Variable, Integer> masks = new HashMap<>();

        for(int i = 0; i < n; i++)
            for(Variable variable : childs.get(i).getVariableBindings().getVariables())
                masks.merge(variable, 1 << i, (a, b) -> a | b);

        List<Shared> shared = new ArrayList<>();

        for(Map.Entry<Variable, Integer> entry : masks.entrySet())
        {
            int mask = entry.getValue();

            if(Integer.bitCount(mask) < 2)
                continue;

            double[] distinct = new double[n];
            boolean hashable = true;

            for(int i = 0; i < n; i++)
            {
                if((mask >> i & 1) == 0)
                    continue;

                SqlIntercode child = childs.get(i);
                VariableBinding binding = child.getVariable(entry.getKey());

                distinct[i] = Math.max(Math.min(estimateDistinct(request, child, entry.getKey()), rows[i]), 1);

                long classes = binding.getMappings().values().stream().filter(columns -> columns != null).count();

                if(binding.canBeNull() || classes != 1)
                    hashable = false;
            }

            shared.add(new Shared(entry.getKey(), mask, distinct, hashable));
        }


        // the children adjacent to each child through a shared variable, and through a hashable one
        int[] adjacent = new int[n];
        int[] hashableAdjacent = new int[n];

        for(Shared s : shared)
        {
            for(int i = 0; i < n; i++)
            {
                if((s.mask() >> i & 1) == 0)
                    continue;

                adjacent[i] |= s.mask() & ~(1 << i);

                if(s.hashable())
                    hashableAdjacent[i] |= s.mask() & ~(1 << i);
            }
        }

        // the children whose rows can be the outer rows of an index lookup into each child
        int[] lookupable = new int[n];

        for(int j = 0; j < n; j++)
            for(Shared s : shared)
                if((s.mask() >> j & 1) != 0 && s.hashable() && canLookUp(request, childs.get(j), s.variable()))
                    lookupable[j] |= s.mask() & ~(1 << j);


        Sizes sizes = new Sizes(n, rows, shared);

        if(n <= EXHAUSTIVE_LIMIT)
            return planExhaustively(n, rows, adjacent, hashableAdjacent, lookupable, sizes);

        return planGreedily(n, rows, hashableAdjacent, lookupable, sizes);
    }


    /**
     * True if the child, a table access, can be looked up by an index of the column holding the variable, the variable
     * being bound to a single column of the table and the columns bound to values by the access counting as bound for
     * the index.
     *
     * @param request the current request
     * @param child the child
     * @param variable the variable
     * @return true if the child can be looked up by an index of the column holding the variable
     */
    private static boolean canLookUp(Request request, SqlIntercode child, Variable variable)
    {
        DatabaseSchema schema = request.getConfiguration().getDatabaseSchema();

        if(schema == null || !(child instanceof SqlTableAccess access) || access.getTable() == null)
            return false;

        Set<Column> bound = new HashSet<>();

        for(ColumnComparison comparison : access.getConstantConditions(request.getConfiguration()).getAreEqual())
        {
            if(comparison.getLeft() instanceof TableColumn column && !(comparison.getRight() instanceof TableColumn))
                bound.add(column);
            else if(comparison.getRight() instanceof TableColumn column
                    && !(comparison.getLeft() instanceof TableColumn))
                bound.add(column);
        }

        VariableBinding internal = access.getInternalVariableBinding(variable);
        Set<Column> columns = internal != null ? internal.getNonConstantColumns() : Set.of();

        return columns.size() == 1 && columns.iterator().next() instanceof TableColumn column
                && schema.canLookUp(access.getTable(), column, bound);
    }


    /**
     * Estimated sizes of the joins of subsets of the children, computed on demand and kept.
     */
    private static final class Sizes
    {
        /**
         * Number of children.
         */
        private final int n;

        /**
         * Estimated rows of each child.
         */
        private final double[] rows;

        /**
         * The shared variables.
         */
        private final List<Shared> shared;

        /**
         * The sizes computed, by the subset, when the subsets are few enough.
         */
        private final double[] array;

        /**
         * The sizes computed, by the subset, otherwise.
         */
        private final Map<Integer, Double> map;


        /**
         * Creates the sizes.
         *
         * @param n number of children
         * @param rows estimated rows of each child
         * @param shared the shared variables
         */
        Sizes(int n, double[] rows, List<Shared> shared)
        {
            this.n = n;
            this.rows = rows;
            this.shared = shared;
            this.array = n <= EXHAUSTIVE_LIMIT ? new double[1 << n] : null;
            this.map = n <= EXHAUSTIVE_LIMIT ? null : new HashMap<>();

            if(array != null)
                Arrays.fill(array, Double.NaN);
        }


        /**
         * Estimated size of the join of the subset of the children.
         *
         * @param mask the subset
         * @return estimated size of the join of the subset of the children
         */
        double get(int mask)
        {
            if(array != null)
            {
                if(Double.isNaN(array[mask]))
                    array[mask] = compute(mask);

                return array[mask];
            }

            return map.computeIfAbsent(mask, this::compute);
        }


        /**
         * Estimates the size of the join of the subset of the children.
         *
         * @param mask the subset
         * @return estimated size of the join of the subset of the children
         */
        private double compute(int mask)
        {
            double size = 1;

            for(int i = 0; i < n; i++)
                if((mask >> i & 1) != 0)
                    size *= rows[i];

            for(Shared s : shared)
            {
                double smallest = Double.NaN;
                double divisor = 1;

                for(int i = 0; i < n; i++)
                {
                    if(((mask & s.mask()) >> i & 1) == 0)
                        continue;

                    double distinct = s.distinct()[i];

                    if(Double.isNaN(smallest))
                    {
                        smallest = distinct;
                    }
                    else if(distinct < smallest)
                    {
                        divisor *= smallest;
                        smallest = distinct;
                    }
                    else
                    {
                        divisor *= distinct;
                    }
                }

                size /= divisor;
            }

            return Math.max(size, 1);
        }
    }


    /**
     * Cost of joining two subsets of the children by the cheapest method and the node doing it.
     *
     * @param left the left subset
     * @param right the right subset
     * @param leftTree the tree of the left subset
     * @param rightTree the tree of the right subset
     * @param hashableAdjacent the children adjacent to each child through a hashable variable
     * @param lookupable the children whose rows can be the outer rows of an index lookup into each child
     * @param sizes the sizes of the joins
     * @return the cost and the node
     */
    private static Map.Entry<Double, Node> join(int left, int right, Node leftTree, Node rightTree,
            int[] hashableAdjacent, int[] lookupable, Sizes sizes)
    {
        double leftSize = sizes.get(left);
        double rightSize = sizes.get(right);
        double size = sizes.get(left | right);

        boolean hashable = false;

        for(int i = 0; i < hashableAdjacent.length; i++)
            if((left >> i & 1) != 0 && (hashableAdjacent[i] & right) != 0)
                hashable = true;

        double cost = hashable ? HASH_COST * Math.min(leftSize, rightSize) + Math.max(leftSize, rightSize) + size :
                leftSize * rightSize * COMPARISON_COST + size;
        Node node = new HashJoin(leftTree, rightTree);

        if((right & right - 1) == 0)
        {
            int child = Integer.numberOfTrailingZeros(right);

            if((lookupable[child] & left) != 0)
            {
                double lookup = leftSize * LOOKUP_COST + 2 * size;

                if(lookup < cost)
                {
                    cost = lookup;
                    node = new Lookup(leftTree, child);
                }
            }
        }

        return Map.entry(cost, node);
    }


    /**
     * The cheapest join tree over every subset of the children by dynamic programming: a subset is joined from two
     * disjoint subsets connected by a shared variable, or from any two when none are connected.
     *
     * @param n number of children
     * @param rows estimated rows of each child
     * @param adjacent the children adjacent to each child through a shared variable
     * @param hashableAdjacent the children adjacent to each child through a hashable variable
     * @param lookupable the children whose rows can be the outer rows of an index lookup into each child
     * @param sizes the sizes of the joins
     * @return the join tree with its estimated rows
     */
    private static Plan planExhaustively(int n, double[] rows, int[] adjacent, int[] hashableAdjacent, int[] lookupable,
            Sizes sizes)
    {
        int full = (1 << n) - 1;

        int[] neighbours = new int[full + 1];

        for(int mask = 1; mask <= full; mask++)
        {
            int lowest = mask & -mask;
            neighbours[mask] = neighbours[mask ^ lowest] | adjacent[Integer.numberOfTrailingZeros(lowest)];
        }

        double[] costs = new double[full + 1];
        Node[] trees = new Node[full + 1];

        for(int i = 0; i < n; i++)
        {
            costs[1 << i] = rows[i];
            trees[1 << i] = new Leaf(i);
        }

        for(int mask = 1; mask <= full; mask++)
        {
            if(trees[mask] != null)
                continue;

            for(boolean connected : new boolean[] { true, false })
            {
                double bestCost = Double.POSITIVE_INFINITY;
                Node bestTree = null;

                for(int left = mask - 1 & mask; left > 0; left = left - 1 & mask)
                {
                    int right = mask ^ left;

                    if(trees[left] == null || trees[right] == null)
                        continue;

                    if(connected && (neighbours[left] & right) == 0)
                        continue;

                    // a hash join is symmetric, so each pair of sides is tried once, except that a single child on
                    // the right may be looked up
                    if(left < right && (right & right - 1) != 0)
                        continue;

                    Map.Entry<Double, Node> candidate = join(left, right, trees[left], trees[right], hashableAdjacent,
                            lookupable, sizes);

                    double cost = candidate.getKey() + costs[left] + costs[right];

                    if(cost < bestCost)
                    {
                        bestCost = cost;
                        bestTree = candidate.getValue();
                    }
                }

                if(bestTree != null)
                {
                    costs[mask] = bestCost;
                    trees[mask] = bestTree;
                    break;
                }
            }
        }

        return new Plan(trees[full], sizes.get(full));
    }


    /**
     * A left-deep join tree built greedily, the cheapest join of the next child taken at every step, from the start
     * giving the cheapest tree.
     *
     * @param n number of children
     * @param rows estimated rows of each child
     * @param hashableAdjacent the children adjacent to each child through a hashable variable
     * @param lookupable the children whose rows can be the outer rows of an index lookup into each child
     * @param sizes the sizes of the joins
     * @return the join tree with its estimated rows
     */
    private static Plan planGreedily(int n, double[] rows, int[] hashableAdjacent, int[] lookupable, Sizes sizes)
    {
        int full = (1 << n) - 1;

        double bestCost = Double.POSITIVE_INFINITY;
        Node bestTree = null;

        for(int start = 0; start < n; start++)
        {
            Node tree = new Leaf(start);
            int mask = 1 << start;
            double cost = rows[start];

            while(mask != full)
            {
                double stepCost = Double.POSITIVE_INFINITY;
                Node stepTree = null;
                int stepChild = -1;

                for(int i = 0; i < n; i++)
                {
                    if((mask >> i & 1) != 0)
                        continue;

                    Map.Entry<Double, Node> candidate = join(mask, 1 << i, tree, new Leaf(i), hashableAdjacent,
                            lookupable, sizes);

                    if(candidate.getKey() + rows[i] < stepCost)
                    {
                        stepCost = candidate.getKey() + rows[i];
                        stepTree = candidate.getValue();
                        stepChild = i;
                    }
                }

                cost += stepCost;
                tree = stepTree;
                mask |= 1 << stepChild;
            }

            if(cost < bestCost)
            {
                bestCost = cost;
                bestTree = tree;
            }
        }

        return new Plan(bestTree, sizes.get(full));
    }


    /**
     * Estimated number of rows of the solutions of the node: a table access from the statistics of its table, inline
     * data by its rows, a join by its plan, a union by the sum of its branches, a filter by the selectivities of its
     * conditions, a distinct by the distinct values of its variables, an OPTIONAL by the fan-out of its right side, a
     * MINUS by its left side, a lateral join by the product of its sides, and the nodes passing the solutions of a
     * child on by the child; {@link #DEFAULT_ROWS} for the other nodes.
     *
     * @param request the current request
     * @param node the node
     * @return estimated number of rows of the solutions of the node
     */
    static double estimateRows(Request request, SqlIntercode node)
    {
        if(node instanceof SqlTableAccess access)
        {
            if(access.getTable() == null)
                return 1;

            TableStatistics statistics = request.getConfiguration().getTableStatistics(access.getTable());

            return Math.max(statistics.estimateRows(request, access.getConstantConditions(request.getConfiguration())),
                    0);
        }

        if(node instanceof SqlValues values)
            return values.getSize();

        if(node instanceof SqlJoin join)
            return join.getPlan(request).rows();

        if(node instanceof SqlUnion union)
        {
            double rows = 0;

            for(SqlIntercode child : union.getChilds())
                rows += estimateRows(request, child);

            return rows;
        }

        if(node instanceof SqlFilter filter)
        {
            double rows = estimateRows(request, filter.getChild());

            for(SqlExpressionIntercode condition : filter.getConditions())
                rows *= selectivity(condition);

            return rows;
        }

        if(node instanceof SqlDistinct distinct)
        {
            double rows = estimateRows(request, distinct.getChild());
            double values = 1;

            for(Variable variable : distinct.getDistinctVariables())
                values *= Math.max(estimateDistinct(request, distinct.getChild(), variable), 1);

            return Math.min(rows, values);
        }

        if(node instanceof SqlLeftJoin leftJoin)
        {
            double rows = estimateRows(request, leftJoin.getLeft());
            double fanout = estimateRows(request, leftJoin.getRight());

            Set<Variable> variables = new HashSet<>(leftJoin.getLeft().getVariableBindings().getVariables());
            variables.retainAll(leftJoin.getRight().getVariableBindings().getVariables());

            for(Variable variable : variables)
                fanout /= Math.max(estimateDistinct(request, leftJoin.getRight(), variable), 1);

            return rows * Math.max(fanout, 1);
        }

        if(node instanceof SqlMinus minus)
            return estimateRows(request, minus.getLeft());

        if(node instanceof SqlLateralJoin lateral)
            return estimateRows(request, lateral.getLeft()) * estimateRows(request, lateral.getRight());

        if(node instanceof SqlBind bind)
            return estimateRows(request, bind.getChild());

        if(node instanceof SqlStripConstantColumns strip)
            return estimateRows(request, strip.getChild());

        if(node instanceof SqlShared shared)
            return estimateRows(request, shared.getChild());

        if(node instanceof SqlEmptySolution)
            return 1;

        if(node instanceof SqlNoSolution)
            return 0;

        return DEFAULT_ROWS;
    }


    /**
     * Estimated number of distinct values of the variable in the solutions of the node, at most the estimated number of
     * the solutions: 1 for a variable the node binds to a constant or does not bind, from the statistics of its table
     * for a table access, the fewest among the children binding it for a join, the sum over the branches for a union,
     * and the count in the child binding it for the other nodes; the number of solutions when nothing better is known.
     *
     * @param request the current request
     * @param node the node
     * @param variable the variable
     * @return estimated number of distinct values of the variable in the solutions of the node
     */
    static double estimateDistinct(Request request, SqlIntercode node, Variable variable)
    {
        VariableBinding binding = node.getVariable(variable);

        if(binding == null || binding.isConstant())
            return 1;

        double rows = Math.max(estimateRows(request, node), 1);
        double distinct = rows;

        if(node instanceof SqlTableAccess access)
        {
            VariableBinding internal = access.getInternalVariableBinding(variable);
            Set<Column> columns = internal != null ? internal.getNonConstantColumns() : Set.of();

            if(access.getTable() == null || columns.isEmpty())
                return 1;

            Column column = columns.stream().sorted().findFirst().get();
            TableStatistics statistics = request.getConfiguration().getTableStatistics(access.getTable());

            distinct = statistics.estimateDistinct(request, column,
                    access.getConstantConditions(request.getConfiguration()));
        }
        else if(node instanceof SqlJoin join)
        {
            for(SqlIntercode child : join.getChilds())
                if(child.getVariable(variable) != null)
                    distinct = Math.min(distinct, estimateDistinct(request, child, variable));
        }
        else if(node instanceof SqlUnion union)
        {
            distinct = 0;

            for(SqlIntercode child : union.getChilds())
                if(child.getVariable(variable) != null)
                    distinct += estimateDistinct(request, child, variable);
        }
        else if(node instanceof SqlLeftJoin leftJoin)
        {
            SqlIntercode side = leftJoin.getLeft().getVariable(variable) != null ? leftJoin.getLeft() :
                    leftJoin.getRight();
            distinct = estimateDistinct(request, side, variable);
        }
        else if(node instanceof SqlLateralJoin lateral)
        {
            SqlIntercode side = lateral.getLeft().getVariable(variable) != null ? lateral.getLeft() :
                    lateral.getRight();
            distinct = estimateDistinct(request, side, variable);
        }
        else if(node instanceof SqlMinus minus)
        {
            distinct = estimateDistinct(request, minus.getLeft(), variable);
        }
        else if(node instanceof SqlDistinct d)
        {
            distinct = estimateDistinct(request, d.getChild(), variable);
        }
        else if(node instanceof SqlFilter filter)
        {
            distinct = estimateDistinct(request, filter.getChild(), variable);
        }
        else if(node instanceof SqlBind bind && bind.getChild().getVariable(variable) != null)
        {
            distinct = estimateDistinct(request, bind.getChild(), variable);
        }
        else if(node instanceof SqlStripConstantColumns strip)
        {
            distinct = estimateDistinct(request, strip.getChild(), variable);
        }
        else if(node instanceof SqlShared shared)
        {
            distinct = estimateDistinct(request, shared.getChild(), variable);
        }

        return Math.max(Math.min(distinct, rows), 1);
    }


    /**
     * True if the selectivity of the condition is estimated by what the condition says (see {@link #selectivity}): it
     * is a comparison, a call of a built-in function, a membership test, or it is made of such conditions by the
     * logical operators. A test of a pattern is not, nor is any other condition nothing is known about.
     *
     * @param condition the condition
     * @return true if the selectivity of the condition is estimated by what the condition says, false otherwise
     */
    static boolean isEstimated(SqlExpressionIntercode condition)
    {
        if(condition instanceof SqlBinaryLogical logical)
            return isEstimated(logical.getLeft()) && isEstimated(logical.getRight());

        if(condition instanceof SqlBinaryComparison || condition instanceof SqlBuiltinCall
                || condition instanceof SqlInExpression)
            return true;

        return condition instanceof SqlUnary unary && isEstimated(unary.getOperand());
    }


    /**
     * Estimated fraction of the solutions a filter condition keeps, after the defaults of the planner: an equality
     * keeps {@link #EQUALITY_SELECTIVITY}, an inequality {@link #INEQUALITY_SELECTIVITY}, a conjunction the product and
     * a disjunction the complement of the product of the complements of the selectivities of its operands, a negation
     * the complement, a membership test what the disjunction of the equalities with its members does, a test of the
     * characters of a string what an equality does, and any other condition {@link #DEFAULT_SELECTIVITY}.
     *
     * @param condition the condition
     * @return estimated fraction of the solutions the condition keeps
     */
    static double selectivity(SqlExpressionIntercode condition)
    {
        if(condition instanceof SqlBinaryLogical logical)
        {
            double left = selectivity(logical.getLeft());
            double right = selectivity(logical.getRight());

            return logical.getOperator() == LogicalOperator.AND ? left * right : left + right - left * right;
        }

        if(condition instanceof SqlUnaryLogical negation)
            return 1 - selectivity(negation.getOperand());

        if(condition instanceof SqlBinaryComparison comparison)
        {
            return switch(comparison.getOperator())
            {
                case EQUAL -> EQUALITY_SELECTIVITY;
                case NOT_EQUAL -> 1 - EQUALITY_SELECTIVITY;
                default -> INEQUALITY_SELECTIVITY;
            };
        }

        if(condition instanceof SqlInExpression in)
        {
            double others = Math.pow(1 - EQUALITY_SELECTIVITY, in.getRights().size());

            return in.isNegated() ? others : 1 - others;
        }

        if(condition instanceof SqlBuiltinCall call)
        {
            return switch(call.getFunction())
            {
                case "regex", "contains", "strstarts", "strends", "sameterm" -> EQUALITY_SELECTIVITY;
                default -> DEFAULT_SELECTIVITY;
            };
        }

        if(condition instanceof SqlUnary unary)
            return selectivity(unary.getOperand());

        return DEFAULT_SELECTIVITY;
    }
}
