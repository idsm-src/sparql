package cz.iocb.sparql.engine.imcode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.Condition.ColumnComparison;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.Table;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.database.TableStatistics;
import cz.iocb.sparql.engine.imcode.expression.SqlBinaryComparison;
import cz.iocb.sparql.engine.imcode.expression.SqlBinaryComparison.ComparisonOperator;
import cz.iocb.sparql.engine.imcode.expression.SqlBinaryLogical;
import cz.iocb.sparql.engine.imcode.expression.SqlBinaryLogical.LogicalOperator;
import cz.iocb.sparql.engine.imcode.expression.SqlBuiltinCall;
import cz.iocb.sparql.engine.imcode.expression.SqlEffectiveBooleanValue;
import cz.iocb.sparql.engine.imcode.expression.SqlExpressionIntercode;
import cz.iocb.sparql.engine.imcode.expression.SqlInExpression;
import cz.iocb.sparql.engine.imcode.expression.SqlIri;
import cz.iocb.sparql.engine.imcode.expression.SqlLiteral;
import cz.iocb.sparql.engine.imcode.expression.SqlNull;
import cz.iocb.sparql.engine.imcode.expression.SqlUnary;
import cz.iocb.sparql.engine.imcode.expression.SqlUnaryLogical;
import cz.iocb.sparql.engine.imcode.expression.SqlVariable;
import cz.iocb.sparql.engine.mapping.classes.ClassRelations;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.Estimator;
import cz.iocb.sparql.engine.translator.VariableBinding;
import cz.iocb.sparql.engine.translator.VariableBindings;



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
 * calibrated on the NeXtProt queries over a single table of triples. A filter above the join counts as well (see
 * {@link SqlIntercode.Restrictions#getFilterConditions}), because the database evaluates a condition as soon as the
 * variables deciding it are bound: a condition that one variable decides alone reduces the child the join takes the
 * variable from, and a condition requiring two variables taken from two children to be equal reduces every join that
 * holds both children, as a variable shared by them would.
 *
 * The sides of an OPTIONAL keep their order, so only the method of their join is chosen (see {@link #planLeftJoin}):
 * the right side is evaluated once and the sides are hashed, or the right side is evaluated for every solution of the
 * left side anew, restricted to the solutions compatible with it (see {@link SqlIntercode#translateForRow}), which is
 * the index nested loop of a join. The second way costs what the right side costs for one row (see
 * {@link #estimateForRow}): a table access looked up by an index costs a lookup, a join is planned again for the
 * children so restricted, and a node that cannot be restricted costs its whole evaluation.
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
     * @param cost estimated cost of the joins of the tree, together with the costs of the children when the tree is
     *            chosen for one of the rows the join is evaluated for (see {@link #estimateForRow})
     * @param restricted whether the row the tree is chosen for restricts a child (see {@link Evaluation})
     */
    record Plan(Node root, double rows, double cost, boolean restricted)
    {
    }


    /**
     * The rows a node is evaluated for one by one, as the inner side of a nested loop over them (see
     * {@link SqlIntercode#translateForRow}): the solutions of the left side of an OPTIONAL whose right side is looked
     * up for each of them.
     *
     * @param bindings bindings of the variables by whose values in a row the node can be restricted to its solutions
     *            compatible with the row: the variables every row binds, in a single class
     * @param table alias the columns of the rows are addressed through
     */
    record Outer(VariableBindings bindings, Table table)
    {
        /**
         * The rows with the given bindings for a node with the given bindings: of the variables the node binds too,
         * those are kept that every row binds in a single class, because the value of such a variable is compared with
         * the columns of the node by a plain equality.
         *
         * @param bindings bindings of the rows
         * @param node bindings of the node evaluated for the rows
         * @param table alias the columns of the rows are addressed through
         * @return the rows
         */
        static Outer of(VariableBindings bindings, VariableBindings node, Table table)
        {
            VariableBindings kept = new VariableBindings();

            for(VariableBinding binding : bindings.getValues())
                if(node.get(binding.getVariable()) != null && !binding.canBeNull() && binding.getMappings().size() == 1
                        && !binding.getMappings().containsValue(null))
                    kept.add(binding);

            return new Outer(kept, table);
        }
    }


    /**
     * Estimated evaluation of a node for one of the rows it is evaluated for (see {@link #estimateForRow}).
     *
     * @param rows estimated number of rows of the solutions of the node for one row
     * @param cost estimated cost of finding them, in units of one row scanned or hashed
     * @param restricted whether the row restricts the node anywhere, so that the node is not evaluated whole for it
     */
    record Evaluation(double rows, double cost, boolean restricted)
    {
    }


    /**
     * The method chosen for the join of the sides of an OPTIONAL (see {@link #planLeftJoin}).
     *
     * @param outer the solutions of the left side that the right side is evaluated for one by one, or null when the
     *            right side is evaluated once and the sides are hashed (or compared pairwise when no equality of theirs
     *            can be hashed)
     * @param rows estimated number of rows of the result
     * @param cost estimated cost of the join, the evaluation of the left side excluded
     */
    record LeftJoinPlan(Outer outer, double rows, double cost)
    {
    }


    /**
     * The children of a join tree with the cost of the tree (see {@link #cost}).
     *
     * @param mask the children of the tree, as a bit per child
     * @param cost cost of the tree
     */
    private record Cost(int mask, double cost)
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
     * A condition of the filters above the join that requires two variables taken from two children to be equal.
     *
     * @param mask the two children the join takes the variables of the condition from
     * @param selectivity estimated fraction of the solutions of a join of those children that the condition keeps
     */
    private record Filter(int mask, double selectivity)
    {
    }


    /**
     * What the filters above a join leave of the solutions of its children (see {@link #plan}).
     *
     * @param kept the fraction of its solutions each child gives the joins
     * @param values per child, the fraction of its values that each variable keeps in the solutions the child gives the
     *            joins, by the conditions the variable decides alone; no entry for a variable keeping all its values
     * @param equated the pairs of variables that the conditions require to be equal and that the join takes from two
     *            children
     */
    private record Filtering(double[] kept, List<Map<Variable, Double>> values, List<List<Variable>> equated)
    {
        /**
         * The filtering by no condition.
         *
         * @param n number of children
         * @return the filtering that leaves all the solutions of the children
         */
        static Filtering none(int n)
        {
            double[] kept = new double[n];
            Arrays.fill(kept, 1);

            return new Filtering(kept, Collections.nCopies(n, Map.of()), List.of());
        }
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
     * Chooses the join tree of the children whose solutions the filters above the join keep by the given conditions
     * (see {@link #getFilters}). The database evaluates such a condition as soon as the variables deciding it are
     * bound. A condition that one variable decides alone is evaluated before the joins: the child the join takes the
     * variable from gives the joins only the fraction of its solutions the conditions are estimated to keep (see
     * {@link #selectivity(Request, SqlIntercode, List)}). A condition requiring two variables to be equal (see
     * {@link #getEquated}) that the join takes from two children is evaluated where the second of them is joined, so
     * every join holding both has only the fraction of its solutions the condition is estimated to keep (see
     * {@link #equalitySelectivity}); when a child binds both variables, how many of its solutions hold the same value
     * twice is not known, so the condition does not count. The tree is chosen for the children and the joins so
     * reduced; the estimated rows of the plan are those of the join itself, as the filters are no part of it. When the
     * join is evaluated for the rows of an outer table one by one, the tree is chosen for the children as they are
     * evaluated for one of the rows (see {@link #estimateForRow}).
     *
     * @param request the current request
     * @param childs the children
     * @param conditions per variable, the conditions the filters above keep the solutions of the join by
     * @param outer the rows the join is evaluated for one by one, or null when it is evaluated once
     * @return the join tree with the estimated rows of the join
     */
    static Plan plan(Request request, List<SqlIntercode> childs, Map<Variable, Set<SqlExpressionIntercode>> conditions,
            Outer outer)
    {
        int n = childs.size();

        Map<SqlExpressionIntercode, Set<Variable>> filters = getFilters(request.getConfiguration(), conditions);

        // the conditions each child decides by one variable, taken together, so that the bounds of a range are
        // estimated as a range, and the pairs of variables taken from two children that a condition requires to be equal
        Map<Integer, List<SqlExpressionIntercode>> decided = new HashMap<>();
        List<List<Variable>> equated = new ArrayList<>();

        for(Map.Entry<SqlExpressionIntercode, Set<Variable>> entry : filters.entrySet())
        {
            if(entry.getValue().size() == 1)
            {
                int child = getDecidingChild(childs, entry.getValue().iterator().next());

                if(child != -1)
                    decided.computeIfAbsent(child, _ -> new ArrayList<>()).add(entry.getKey());
            }
            else
            {
                List<Variable> variables = getEquated(entry.getKey());

                // the values of two variables that one child binds are not independent of each other
                if(variables == null
                        || childs.stream().anyMatch(c -> variables.stream().allMatch(v -> c.getVariable(v) != null)))
                    continue;

                if(variables.stream().allMatch(v -> getDecidingChild(childs, v) != -1))
                    equated.add(variables);
            }
        }

        if(decided.isEmpty() && equated.isEmpty())
            return plan(request, childs, Filtering.none(n), outer);

        double[] kept = new double[n];
        Arrays.fill(kept, 1);

        List<Map<Variable, Double>> values = new ArrayList<>();

        for(int i = 0; i < n; i++)
            values.add(new HashMap<>());

        for(Map.Entry<Integer, List<SqlExpressionIntercode>> entry : decided.entrySet())
        {
            int index = entry.getKey();
            SqlIntercode child = childs.get(index);

            kept[index] = selectivity(request, child, entry.getValue());

            // a condition that a variable decides alone keeps a fraction of the values of the variable
            Map<Variable, List<SqlExpressionIntercode>> alone = new HashMap<>();

            for(SqlExpressionIntercode condition : entry.getValue())
                alone.computeIfAbsent(filters.get(condition).iterator().next(), _ -> new ArrayList<>()).add(condition);

            for(Map.Entry<Variable, List<SqlExpressionIntercode>> single : alone.entrySet())
                values.get(index).put(single.getKey(), single.getValue().size() == entry.getValue().size() ?
                        kept[index] : selectivity(request, child, single.getValue()));
        }

        Plan plan = plan(request, childs, new Filtering(kept, values, equated), outer);

        return new Plan(plan.root(), plan(request, childs, Filtering.none(n), outer).rows(), plan.cost(),
                plan.restricted());
    }


    /**
     * The conditions that the filters above a node keep its solutions by, out of the filter conditions of its
     * variables, each with the variables deciding it. A condition that several variables decide is a filter condition
     * of each of them (see {@link SqlIntercode.Restrictions#getFilterConditions}), and the solutions are filtered by it
     * only if it is a condition of them all: where it is missing for one of them, the node binding that variable is not
     * filtered by the conditions above, or the variable is not bound here at all.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param conditions per variable, its filter conditions
     * @return the conditions the solutions are filtered by, each with the variables deciding it
     */
    static Map<SqlExpressionIntercode, Set<Variable>> getFilters(ClassRelations relations,
            Map<Variable, Set<SqlExpressionIntercode>> conditions)
    {
        Map<SqlExpressionIntercode, Set<Variable>> filters = new LinkedHashMap<>();

        for(Set<SqlExpressionIntercode> set : conditions.values())
        {
            for(SqlExpressionIntercode condition : set)
            {
                if(filters.containsKey(condition))
                    continue;

                Set<Variable> variables = SqlIntercode.getDecidingVariables(relations, condition);

                if(variables.stream().allMatch(v -> conditions.getOrDefault(v, Set.of()).contains(condition)))
                    filters.put(condition, variables);
            }
        }

        return filters;
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
     * Chooses the join tree of the children, of whose solutions the filters above the join leave what is given.
     *
     * @param request the current request
     * @param childs the children
     * @param filtering what the filters above the join leave of the solutions of the children
     * @param outer the rows the join is evaluated for one by one, or null when it is evaluated once
     * @return the join tree with its estimated rows
     */
    private static Plan plan(Request request, List<SqlIntercode> childs, Filtering filtering, Outer outer)
    {
        int n = childs.size();

        double[] rows = new double[n];

        // what a child costs as a leaf of the tree: nothing beyond its rows when the join is evaluated once, its whole
        // evaluation when the join is evaluated for a row, as the child is then evaluated for every row anew
        double[] costs = new double[n];
        boolean restricted = false;

        for(int i = 0; i < n; i++)
        {
            Evaluation evaluation = outer == null ? null : estimateForRow(request, childs.get(i), outer);
            double solutions = evaluation == null ? estimateRows(request, childs.get(i)) : evaluation.rows();

            rows[i] = Math.max(solutions * filtering.kept()[i], 1);
            costs[i] = evaluation == null ? 0 : evaluation.cost();
            restricted |= evaluation != null && evaluation.restricted();
        }


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

                distinct[i] = estimateDistinct(request, child, entry.getKey(), rows[i], filtering.values().get(i));

                if(!isHashable(binding))
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


        List<Filter> filters = new ArrayList<>();

        for(List<Variable> equated : filtering.equated())
            filters.add(new Filter(
                    1 << getDecidingChild(childs, equated.get(0)) | 1 << getDecidingChild(childs, equated.get(1)),
                    equalitySelectivity(request, childs, rows, filtering.values(), equated)));

        Sizes sizes = new Sizes(n, rows, shared, filters);

        Node root = n <= EXHAUSTIVE_LIMIT ? planExhaustively(n, rows, adjacent, hashableAdjacent, lookupable, sizes) :
                planGreedily(n, rows, hashableAdjacent, lookupable, sizes);

        return new Plan(root, sizes.get((1 << n) - 1), cost(root, costs, hashableAdjacent, sizes).cost(), restricted);
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
     * True if an equality of the variable of the binding can be hashed: the variable is bound always and has columns in
     * a single class, so the equality is no disjunction.
     *
     * @param binding the binding of the variable, or null when the variable is not bound
     * @return true if an equality of the variable of the binding can be hashed, false otherwise
     */
    private static boolean isHashable(VariableBinding binding)
    {
        return binding != null && !binding.canBeNull()
                && binding.getMappings().values().stream().filter(columns -> columns != null).count() == 1;
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
         * The conditions of the filters above the join that require variables of two children to be equal.
         */
        private final List<Filter> filters;

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
         * @param filters the conditions of the filters above the join that require variables of two children to be
         *            equal
         */
        Sizes(int n, double[] rows, List<Shared> shared, List<Filter> filters)
        {
            this.n = n;
            this.rows = rows;
            this.shared = shared;
            this.filters = filters;
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

            // a condition is evaluated where the second of the children deciding it is joined
            for(Filter filter : filters)
                if((mask & filter.mask()) == filter.mask())
                    size *= filter.selectivity();

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
        double cost = hashCost(left, right, hashableAdjacent, sizes);
        Node node = new HashJoin(leftTree, rightTree);

        if((right & right - 1) == 0)
        {
            int child = Integer.numberOfTrailingZeros(right);

            if((lookupable[child] & left) != 0)
            {
                double lookup = lookupCost(left, right, sizes);

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
     * Cost of joining two subsets of the children by hashing the smaller one and probing it with the other one, or by
     * comparing them pairwise when no equality between them can be hashed.
     *
     * @param left the left subset
     * @param right the right subset
     * @param hashableAdjacent the children adjacent to each child through a hashable variable
     * @param sizes the sizes of the joins
     * @return the cost of the join
     */
    private static double hashCost(int left, int right, int[] hashableAdjacent, Sizes sizes)
    {
        boolean hashable = false;

        for(int i = 0; i < hashableAdjacent.length; i++)
            if((left >> i & 1) != 0 && (hashableAdjacent[i] & right) != 0)
                hashable = true;

        return hashCost(hashable, sizes.get(left), sizes.get(right), sizes.get(left | right));
    }


    /**
     * Cost of joining two sides of the given sizes by hashing the smaller one and probing it with the other one, or by
     * comparing them pairwise when no equality between them can be hashed.
     *
     * @param hashable whether an equality between the sides can be hashed
     * @param leftSize estimated rows of the left side
     * @param rightSize estimated rows of the right side
     * @param size estimated rows of the join
     * @return the cost of the join
     */
    private static double hashCost(boolean hashable, double leftSize, double rightSize, double size)
    {
        return hashable ? HASH_COST * Math.min(leftSize, rightSize) + Math.max(leftSize, rightSize) + size :
                leftSize * rightSize * COMPARISON_COST + size;
    }


    /**
     * Cost of joining a subset of the children with a child looked up by an index for every row of the subset.
     *
     * @param left the outer subset
     * @param right the child looked up, as a subset
     * @param sizes the sizes of the joins
     * @return the cost of the join
     */
    private static double lookupCost(int left, int right, Sizes sizes)
    {
        return sizes.get(left) * LOOKUP_COST + 2 * sizes.get(left | right);
    }


    /**
     * Cost of a join tree: the costs of its joins (see {@link #join}) and of the children that are its leaves, whereas
     * a child looked up by an index costs its lookups only.
     *
     * @param node the tree
     * @param costs the cost of each child as a leaf of the tree
     * @param hashableAdjacent the children adjacent to each child through a hashable variable
     * @param sizes the sizes of the joins
     * @return the children of the tree with its cost
     */
    private static Cost cost(Node node, double[] costs, int[] hashableAdjacent, Sizes sizes)
    {
        if(node instanceof Leaf leaf)
            return new Cost(1 << leaf.child(), costs[leaf.child()]);

        if(node instanceof Lookup lookup)
        {
            Cost outer = cost(lookup.outer(), costs, hashableAdjacent, sizes);
            int child = 1 << lookup.child();

            return new Cost(outer.mask() | child, outer.cost() + lookupCost(outer.mask(), child, sizes));
        }

        HashJoin join = (HashJoin) node;

        Cost left = cost(join.left(), costs, hashableAdjacent, sizes);
        Cost right = cost(join.right(), costs, hashableAdjacent, sizes);

        return new Cost(left.mask() | right.mask(),
                left.cost() + right.cost() + hashCost(left.mask(), right.mask(), hashableAdjacent, sizes));
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
     * @return the join tree
     */
    private static Node planExhaustively(int n, double[] rows, int[] adjacent, int[] hashableAdjacent, int[] lookupable,
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

        return trees[full];
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
     * @return the join tree
     */
    private static Node planGreedily(int n, double[] rows, int[] hashableAdjacent, int[] lookupable, Sizes sizes)
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

        return bestTree;
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
            return estimateRows(request, filter.getChild())
                    * selectivity(request, filter.getChild(), SqlIntercode.getConjuncts(filter.getConditions()));

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
     * Estimated evaluation of the node for one of the rows of an outer table it is evaluated for one by one (see
     * {@link SqlIntercode#translateForRow}), which is evaluated anew for every row: a table access restricted to the
     * rows compatible with the row (see {@link SqlTableAccess#getRowBindings}) has the rows that share the values of
     * the row and costs a lookup when an index of the table serves one of those values, a join is planned for its
     * children evaluated so, a union is the sum of its branches, an OPTIONAL costs its left side and the join chosen
     * for the rows that side has for one row (see {@link #planLeftJoin}), and a node passing the solutions of a child
     * on is evaluated as the child. Any other node, like a table access that cannot be restricted or looked up, is
     * evaluated whole for every row, which costs a pass over its solutions.
     *
     * @param request the current request
     * @param node the node
     * @param outer the rows the node is evaluated for one by one
     * @return estimated evaluation of the node for one of the rows
     */
    static Evaluation estimateForRow(Request request, SqlIntercode node, Outer outer)
    {
        if(node instanceof SqlTableAccess access)
        {
            double rows = estimateRows(request, access);
            double compatible = rows;
            boolean lookedUp = false;

            Set<Variable> variables = access.getRowBindings(outer).getVariables();

            for(Variable variable : variables)
            {
                compatible /= estimateDistinct(request, access, variable);
                lookedUp |= canLookUp(request, access, variable);
            }

            return new Evaluation(compatible, lookedUp ? LOOKUP_COST + 2 * compatible : Math.max(rows, 1),
                    !variables.isEmpty());
        }

        if(node instanceof SqlJoin join)
        {
            Plan plan = join.getPlan(request, outer);

            return new Evaluation(plan.rows(), plan.cost(), plan.restricted());
        }

        if(node instanceof SqlUnion union)
        {
            double rows = 0;
            double cost = 0;
            boolean restricted = false;

            for(SqlIntercode child : union.getChilds())
            {
                Evaluation evaluation = estimateForRow(request, child, outer);

                rows += evaluation.rows();
                cost += evaluation.cost();
                restricted |= evaluation.restricted();
            }

            return new Evaluation(rows, cost, restricted);
        }

        if(node instanceof SqlLeftJoin leftJoin)
        {
            Evaluation left = estimateForRow(request, leftJoin.getLeft(), outer);
            LeftJoinPlan plan = leftJoin.getPlan(request, outer);

            return new Evaluation(plan.rows(), left.cost() + plan.cost(), left.restricted());
        }

        if(node instanceof SqlFilter filter)
        {
            Evaluation child = estimateForRow(request, filter.getChild(), outer);

            return new Evaluation(child.rows()
                    * selectivity(request, filter.getChild(), SqlIntercode.getConjuncts(filter.getConditions())),
                    child.cost(), child.restricted());
        }

        if(node instanceof SqlBind bind)
            return estimateForRow(request, bind.getChild(), outer);

        if(node instanceof SqlStripConstantColumns strip)
            return estimateForRow(request, strip.getChild(), outer);

        double rows = estimateRows(request, node);

        return new Evaluation(rows, Math.max(rows, 1), false);
    }


    /**
     * Chooses the method of the join of the sides of an OPTIONAL, whose order is given. The right side is evaluated
     * once and the sides are joined by hashing, as the database does by itself (compared pairwise when no equality of
     * theirs can be hashed), or the right side is evaluated for every solution of the left side anew, restricted to the
     * solutions compatible with it, which costs what the right side costs for one row (see {@link #estimateForRow})
     * times the solutions of the left side. The costs are those of a join of the same sides (see {@link #join}), so an
     * OPTIONAL of a table access is looked up exactly when a join with it would be. A right side that is not
     * deterministic is evaluated once. The left side gives the join only the solutions that the conditions of the
     * filters above keep, as far as one of its variables decides them alone (see {@link #getFilters}), because the
     * database evaluates such a condition on the left side (see {@link #plan(Request, List, Map, Outer)}).
     *
     * @param request the current request
     * @param left the left side
     * @param right the right side
     * @param leftTable alias of the left side, through which the right side refers to its solutions
     * @param conditions per variable the left side binds always, the conditions the filters above keep the solutions of
     *            the join by
     * @param outer the rows the whole join is evaluated for one by one, or null when it is evaluated once
     * @return the method chosen for the join
     */
    static LeftJoinPlan planLeftJoin(Request request, SqlIntercode left, SqlIntercode right, Table leftTable,
            Map<Variable, Set<SqlExpressionIntercode>> conditions, Outer outer)
    {
        // only a condition that one variable decides alone is estimated on a single node
        List<SqlExpressionIntercode> filters = getFilters(request.getConfiguration(), conditions).entrySet().stream()
                .filter(e -> e.getValue().size() == 1).map(Map.Entry::getKey).toList();

        double kept = filters.isEmpty() ? 1 : selectivity(request, left, filters);

        double leftSolutions = outer == null ? estimateRows(request, left) :
                estimateForRow(request, left, outer).rows();

        double leftRows = Math.max(leftSolutions * kept, 1);
        double rightRows = Math.max(estimateRows(request, right), 1);

        double fanout = rightRows;
        boolean hashable = false;

        for(Variable variable : left.getVariableBindings().getVariables())
        {
            if(right.getVariable(variable) == null)
                continue;

            fanout /= Math.max(estimateDistinct(request, right, variable), 1);
            hashable |= isHashable(left.getVariable(variable)) && isHashable(right.getVariable(variable));
        }

        double rows = leftRows * Math.max(fanout, 1);
        double hashed = hashCost(hashable, leftRows, rightRows, rows);

        Outer solutions = Outer.of(left.getVariableBindings(), right.getVariableBindings(), leftTable);

        if(right.isDeterministic() && !solutions.bindings().getVariables().isEmpty())
        {
            Evaluation evaluation = estimateForRow(request, right, solutions);
            double lookedUp = leftRows * evaluation.cost();

            // a right side that no solution of the left side restricts would be evaluated whole for each of them
            if(evaluation.restricted() && lookedUp < hashed)
                return new LeftJoinPlan(solutions, rows, lookedUp);
        }

        return new LeftJoinPlan(null, rows, hashed);
    }


    /**
     * Estimated fraction of the solutions of a node that the conditions keep, all of which have to hold. The conditions
     * that only compare a variable with constants (see {@link #getComparison}) are estimated by the planner of the
     * database when the node is an access to a table (see {@link Estimator#estimateSelectivity}): the planner knows how
     * the values of a column are distributed, so it tells a range holding a few rows from one holding nearly all of
     * them, and it is asked about the conditions together, so that it sees both bounds of a range. The other
     * conditions, and all of them when the planner cannot be asked, are estimated by what they say (see
     * {@link #selectivity(SqlExpressionIntercode)}).
     *
     * @param request the current request
     * @param node the node
     * @param conditions the conditions, over variables the node binds
     * @return estimated fraction of the solutions of the node that the conditions keep
     */
    static double selectivity(Request request, SqlIntercode node, List<SqlExpressionIntercode> conditions)
    {
        boolean table = node instanceof SqlTableAccess access && access.getTable() != null;

        List<SqlExpressionIntercode> comparisons = new ArrayList<>();
        double compared = 1;
        double others = 1;

        for(SqlExpressionIntercode condition : conditions)
        {
            SqlExpressionIntercode comparison = !table ? null :
                    getComparison(request.getConfiguration(), condition, node.getVariableBindings());

            // a comparison that the classes of the node decide tells the planner nothing
            if(comparison == null || comparison instanceof SqlLiteral || comparison instanceof SqlNull)
            {
                others *= selectivity(condition);
                continue;
            }

            comparisons.add(comparison);
            compared *= selectivity(condition);
        }

        if(comparisons.isEmpty())
            return others;

        double estimated = new Estimator(request).estimateSelectivity(request, Scope.TOP, node, comparisons);

        return others * (Double.isNaN(estimated) ? compared : estimated);
    }


    /**
     * The condition rebuilt over the bindings when it only compares a variable with constants: a comparison of a
     * variable and a constant, a membership test of a variable among constants, or such conditions put together by the
     * logical operators; null for any other condition. The condition is made anew of the variable as the bindings give
     * it, because a condition reads its operands by the classes they had where it was optimised last.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param condition the condition
     * @param bindings the variable bindings
     * @return the condition rebuilt over the bindings when it only compares a variable with constants, or null
     */
    private static SqlExpressionIntercode getComparison(ClassRelations relations, SqlExpressionIntercode condition,
            VariableBindings bindings)
    {
        if(condition instanceof SqlEffectiveBooleanValue value)
        {
            SqlExpressionIntercode operand = getComparison(relations, value.getOperand(), bindings);

            return operand == null ? null : SqlEffectiveBooleanValue.create(relations, operand);
        }

        if(condition instanceof SqlUnaryLogical negation)
        {
            SqlExpressionIntercode operand = getComparison(relations, negation.getOperand(), bindings);

            return operand == null ? null : SqlUnaryLogical.create(relations, operand);
        }

        if(condition instanceof SqlBinaryLogical logical)
        {
            SqlExpressionIntercode left = getComparison(relations, logical.getLeft(), bindings);
            SqlExpressionIntercode right = getComparison(relations, logical.getRight(), bindings);

            return left == null || right == null ? null :
                    SqlBinaryLogical.create(relations, logical.getOperator(), left, right);
        }

        if(condition instanceof SqlBinaryComparison comparison)
        {
            SqlExpressionIntercode left = getComparedOperand(relations, comparison.getLeft(), bindings);
            SqlExpressionIntercode right = getComparedOperand(relations, comparison.getRight(), bindings);

            // one operand is the variable and the other one a constant
            if(left == null || right == null || left instanceof SqlVariable == right instanceof SqlVariable)
                return null;

            return SqlBinaryComparison.create(relations, comparison.getOperator(), left, right);
        }

        if(condition instanceof SqlInExpression in)
        {
            SqlExpressionIntercode left = getComparedOperand(relations, in.getLeft(), bindings);

            if(!(left instanceof SqlVariable))
                return null;

            if(!in.getRights().stream().allMatch(r -> r instanceof SqlLiteral || r instanceof SqlIri))
                return null;

            return SqlInExpression.create(relations, in.isNegated(), left, in.getRights());
        }

        return null;
    }


    /**
     * An operand of a comparison of a variable with a constant rebuilt over the bindings (see {@link #getComparison}):
     * the variable as the bindings give it, or the constant itself; null for any other operand and for a variable the
     * bindings do not bind.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param operand the operand
     * @param bindings the variable bindings
     * @return the variable as the bindings give it, the constant itself, or null
     */
    private static SqlExpressionIntercode getComparedOperand(ClassRelations relations, SqlExpressionIntercode operand,
            VariableBindings bindings)
    {
        if(operand instanceof SqlVariable variable)
        {
            VariableBinding binding = bindings.get(variable.getVariable());

            return binding == null ? null : SqlVariable.create(relations, binding);
        }

        return operand instanceof SqlLiteral || operand instanceof SqlIri ? operand : null;
    }


    /**
     * Estimated fraction of the solutions of a join of two children that a condition requiring a variable of each of
     * them to be equal keeps: the solutions in which the variable of the more distinct values takes the value of the
     * other one, as it would if the two children shared one variable.
     *
     * @param request the current request
     * @param childs the children
     * @param rows estimated rows each child gives the joins
     * @param values per child, the fraction of its values that each variable keeps in those rows
     * @param equated the two variables
     * @return estimated fraction of the solutions of a join of the two children that the condition keeps
     */
    private static double equalitySelectivity(Request request, List<SqlIntercode> childs, double[] rows,
            List<Map<Variable, Double>> values, List<Variable> equated)
    {
        double distinct = 1;

        for(Variable variable : equated)
        {
            int child = getDecidingChild(childs, variable);

            distinct = Math.max(distinct,
                    estimateDistinct(request, childs.get(child), variable, rows[child], values.get(child)));
        }

        return 1 / distinct;
    }


    /**
     * Estimated number of distinct values of the variable in the solutions a child gives the joins: the values the
     * variable has in the child (see {@link #estimateDistinct(Request, SqlIntercode, Variable)}), of which the
     * conditions the variable decides alone keep the given fraction, whereas a condition another variable decides keeps
     * solutions, so that the values are only as many at most as the solutions the child gives.
     *
     * @param request the current request
     * @param child the child
     * @param variable the variable
     * @param rows estimated rows the child gives the joins
     * @param values the fraction of its values that each variable keeps in those rows
     * @return estimated number of distinct values of the variable in the solutions the child gives the joins
     */
    private static double estimateDistinct(Request request, SqlIntercode child, Variable variable, double rows,
            Map<Variable, Double> values)
    {
        double distinct = estimateDistinct(request, child, variable) * values.getOrDefault(variable, 1.0);

        return Math.max(Math.min(distinct, rows), 1);
    }


    /**
     * The two variables a condition requires to be equal: the operands of an equality, or of a test of the same term,
     * when both of them are variables; null for any other condition.
     *
     * @param condition the condition
     * @return the two variables the condition requires to be equal, or null
     */
    static List<Variable> getEquated(SqlExpressionIntercode condition)
    {
        if(condition instanceof SqlEffectiveBooleanValue value)
            return getEquated(value.getOperand());

        List<SqlExpressionIntercode> operands = null;

        if(condition instanceof SqlBinaryComparison comparison && comparison.getOperator() == ComparisonOperator.EQUAL)
            operands = List.of(comparison.getLeft(), comparison.getRight());
        else if(condition instanceof SqlBuiltinCall call && call.getFunction().equals("sameterm"))
            operands = call.getArguments();

        if(operands != null && operands.size() == 2 && operands.get(0) instanceof SqlVariable left
                && operands.get(1) instanceof SqlVariable right)
            return List.of(left.getVariable(), right.getVariable());

        return null;
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
