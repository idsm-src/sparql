package cz.iocb.sparql.engine.imcode;

import static cz.iocb.sparql.engine.mapping.classes.ResourceClass.areDisjunct;
import static java.util.stream.Collectors.joining;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import cz.iocb.sparql.engine.database.AliasTable;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.database.VirtualTable;
import cz.iocb.sparql.engine.imcode.expression.SqlVariable;
import cz.iocb.sparql.engine.mapping.classes.ClassRelations;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.VariableBinding;
import cz.iocb.sparql.engine.translator.VariableBindingPair;
import cz.iocb.sparql.engine.translator.VariableBindings;



/**
 * Transitive closure of a path step for the {@code +} and {@code *} property paths, translated to
 * {@code WITH RECURSIVE}: {@code init} yields the end nodes reachable in the first step (or in zero steps) from the
 * begin variable, and {@code next}, whose start node is {@code joinVar}, extends a path from its current end node to a
 * new {@code endVar}. {@code graphVar} carries the graph when the pattern is evaluated in a variable graph. The
 * recursion carries the start and graph variables along unchanged, and so every other variable of the initial solutions
 * the parent needs: a join seeds a walk with its siblings (see {@link #seed}), whose solutions then start the recursion
 * and whose variables it delivers in place of the join. When the parent needs neither an end of the walk nor the
 * multiplicities, the walk reduces to its initial solutions, which reach the same nodes (see {@link #optimize}), and
 * since the zero-length paths match every node, an OPTIONAL walk of zero or more steps from nodes the left side binds
 * always is a join (see {@link #matchesEverySolutionOf}).
 */
public final class SqlRecursive extends SqlIntercode
{
    /**
     * Alias of the recursion in the recursive term.
     */
    private static final AliasTable leftTable = new AliasTable("tab0");

    /**
     * Alias of the step in the recursive term.
     */
    private static final AliasTable rightTable = new AliasTable("tab1");

    /**
     * Alias of the recursion in the recursive term when the step is joined laterally; the branches of the step then
     * carry their own aliases {@code tab0}, {@code tab1}, ... inside the lateral subquery, which would shadow it.
     */
    private static final AliasTable walkTable = new AliasTable("walk");

    /**
     * Alias of the step in the recursive term when it is joined laterally.
     */
    private static final AliasTable stepTable = new AliasTable("step");

    /**
     * Solutions starting the recursion.
     */
    public final SqlIntercode init;

    /**
     * Step extending a path by one traversal.
     */
    public final SqlIntercode next;

    /**
     * Binding of the end variable in the result.
     */
    public final VariableBinding endBinding;

    /**
     * Variable of the step bound to the current end node.
     */
    public final Variable joinVar;

    /**
     * Variable of the start node; null for a constant start.
     */
    public final Variable beginVar;

    /**
     * Columns of the start and graph variables inside the recursion.
     */
    public final List<Column> iv;

    /**
     * Columns of the start and graph variables exposed by the recursion.
     */
    public final List<Column> ev;

    /**
     * Graph variable; null when the graph is not a variable.
     */
    public final Variable graphVar;

    /**
     * The step written for the walk in the other direction, from the start variable to {@link #reverseJoinVar}: a walk
     * between two variables may start from either of them, and {@link #optimize} starts it from the end variable when
     * the parent restricts that one more (the step then takes {@code reverseJoinVar} for the current node and the start
     * variable for the new one). Null when the walk cannot be turned (a constant start, or the same variable at both
     * ends).
     */
    public final SqlIntercode reverseNext;

    /**
     * Variable of {@link #reverseNext} bound to the node it reaches; null when there is no reverse step.
     */
    public final Variable reverseJoinVar;

    /**
     * Variables of the initial solutions carried through the recursion unchanged (the start and graph variables and the
     * others the parent needs), including those the bindings do not expose.
     */
    private final Set<Variable> carried;


    /**
     * Creates the node.
     *
     * @param bindings the variable bindings
     * @param init the initial solutions
     * @param next the recursive step
     * @param endBinding binding of the end variable in the result
     * @param joinVar variable of the step bound to the current end node
     * @param beginVar the start variable, or null
     * @param iv columns of the start and graph variables inside the recursion
     * @param ev columns of the start and graph variables exposed by the recursion
     * @param graphVar the graph variable, or null
     * @param reverseNext the step of the walk in the other direction, or null
     * @param reverseJoinVar variable of the reverse step bound to the node it reaches, or null
     * @param carried variables of the initial solutions carried through the recursion
     */
    protected SqlRecursive(VariableBindings bindings, SqlIntercode init, SqlIntercode next, VariableBinding endBinding,
            Variable joinVar, Variable beginVar, List<Column> iv, List<Column> ev, Variable graphVar,
            SqlIntercode reverseNext, Variable reverseJoinVar, Set<Variable> carried)
    {
        super(bindings, init.isDeterministic() && next.isDeterministic());
        this.init = init;
        this.next = next;
        this.endBinding = endBinding;
        this.joinVar = joinVar;
        this.beginVar = beginVar;
        this.iv = iv;
        this.ev = ev;
        this.graphVar = graphVar;
        this.reverseNext = reverseNext;
        this.reverseJoinVar = reverseJoinVar;
        this.carried = carried;
    }


    /**
     * Recursive closure of {@code next} started from {@code init}; {@code beginVar} may be null when the start is a
     * constant.
     *
     * @param request the current request
     * @param init the initial solutions
     * @param next the recursive step
     * @param beginVar the start variable, or null
     * @param joinVar variable of the step bound to the current end node
     * @param endVar the end variable
     * @param graphVar the graph variable, or null
     * @return recursive closure of {@code next} started from {@code init}; {@code beginVar} may be null when the start
     *         is a constant
     */
    public static SqlIntercode create(Request request, SqlIntercode init, SqlIntercode next, Variable beginVar,
            Variable joinVar, Variable endVar, Variable graphVar)
    {
        return create(request, init, next, beginVar, joinVar, endVar, graphVar, null, null, null);
    }


    /**
     * Recursive closure of {@code next} started from {@code init} between two variables, which may also be walked the
     * other way round by {@code reverseNext} (see {@link #reverseNext}).
     *
     * @param request the current request
     * @param init the initial solutions
     * @param next the recursive step
     * @param beginVar the start variable
     * @param joinVar variable of the step bound to the current end node
     * @param endVar the end variable
     * @param graphVar the graph variable, or null
     * @param reverseNext the step of the walk in the other direction
     * @param reverseJoinVar variable of the reverse step bound to the node it reaches
     * @return recursive closure of {@code next} started from {@code init}
     */
    public static SqlIntercode create(Request request, SqlIntercode init, SqlIntercode next, Variable beginVar,
            Variable joinVar, Variable endVar, Variable graphVar, SqlIntercode reverseNext, Variable reverseJoinVar)
    {
        return create(request, init, next, beginVar, joinVar, endVar, graphVar, null, reverseNext, reverseJoinVar);
    }


    /**
     * Recursion exposing only what the parent needs; the start and graph variables get fresh output columns, and so
     * does every other variable of the initial solutions the parent needs, which the recursion carries along unchanged
     * (the variables of the siblings a walk is seeded with, see {@link #seed}). A variable of the initial solutions
     * nobody needs is left out, because carrying it would multiply the paths walked by its values.
     *
     * @param request the current request
     * @param init the initial solutions
     * @param next the recursive step
     * @param beginVar the start variable, or null
     * @param joinVar variable of the step bound to the current end node
     * @param endVar the end variable
     * @param graphVar the graph variable, or null
     * @param restrictions what the parent needs of the variables; null to carry the start and graph variables alone
     * @param reverseNext the step of the walk in the other direction, or null
     * @param reverseJoinVar variable of the reverse step bound to the node it reaches, or null
     * @return recursion exposing only what the parent needs; the carried variables get fresh output columns
     */
    protected static SqlIntercode create(Request request, SqlIntercode init, SqlIntercode next, Variable beginVar,
            Variable joinVar, Variable endVar, Variable graphVar, Restrictions restrictions, SqlIntercode reverseNext,
            Variable reverseJoinVar)
    {
        ClassRelations relations = request.getConfiguration();

        Map<Column, Column> map = new HashMap<>();
        VariableBindings bindings = new VariableBindings();
        VariableBindings carried = new VariableBindings();

        for(VariableBinding binding : init.getVariableBindings().getValues())
        {
            Variable variable = binding.getVariable();

            if(variable.equals(endVar))
                continue;

            if(!variable.equals(beginVar) && !variable.equals(graphVar)
                    && (restrictions == null || !restrictions.containsVar(variable)))
                continue;

            carried.add(binding);

            VariableBinding v = new VariableBinding(variable, binding.canBeNull());

            for(Entry<ResourceClass, List<Column>> e : binding.getMappings().entrySet())
            {
                if(e.getValue() == null)
                {
                    v.addMapping(e.getKey(), null);
                    continue;
                }

                List<Column> safeNames = e.getKey().createColumns(request.getColumnMap(), variable);

                v.addMapping(e.getKey(), IntStream.range(0, e.getValue().size()).mapToObj(i -> {
                    Column c = e.getValue().get(i);

                    if(c instanceof ConstantColumn)
                        return c;
                    else
                        return map.computeIfAbsent(c, _ -> safeNames.get(i));
                }).toList());
            }

            bindings.add(v);
        }

        VariableBinding endBinding = createEndVariableBinding(request, endVar, init, next);

        if(endBinding != null)
            bindings.add(endBinding);


        List<Column> innerColumns = new ArrayList<>(carried.getNonConstantColumns());
        List<Column> outerColumns = innerColumns.stream().map(c -> map.get(c)).toList();

        return new SqlRecursive(bindings.restrict(relations, restrictions), init, next, endBinding, joinVar, beginVar,
                innerColumns, outerColumns, graphVar, reverseNext, reverseJoinVar, carried.getVariables());
    }


    /**
     * The walk seeded with the solutions of its siblings in a join: the initial solutions are joined with the seeds on
     * the start variable (on the end variable when the walk is turned, the reverse step then walking from it), so that
     * the recursion enumerates only the paths that start in their values, and the variables of the seeds are carried
     * through the recursion, so that the join needs the seeds no more and they are evaluated once. A seed has to bind
     * the start variable without a null and distinct solutions over it: the recursion unites its rows as a set, so
     * several solutions of a seed for one start would collapse into one, and the paths walked would multiply by their
     * number. A seed read from a table that is a source of the zero-length paths delivers nodes of the graph, so the
     * zero-length paths are its own rows; otherwise plain table accesses and values are joined under the deduplication
     * of the zero-length paths (the join is then distributed over the sources, which the database looks up by their
     * indexes), and other seeds are joined with the initial solutions as a whole, so that they are not repeated. The
     * seeded walk is not turned any more, because the carried variables belong to its start.
     *
     * @param request the current request
     * @param seeds the siblings seeding the walk, every one distinct over the start variable and binding it always
     * @param fromEnd true when the seeds bind the end variable, so that the walk is turned (see {@link #reverseNext})
     * @param restrictions what the parent and the siblings of the walk need of its variables
     * @return the seeded walk
     */
    protected SqlIntercode seed(Request request, List<SqlIntercode> seeds, boolean fromEnd, Restrictions restrictions)
    {
        Variable startVar = fromEnd ? endBinding.getVariable() : beginVar;
        Variable stopVar = fromEnd ? beginVar : endBinding.getVariable();

        SqlIntercode seeded = seedInit(request, init, seeds, startVar, stopVar);

        Restrictions seedRestrictions = new Restrictions(restrictions);

        for(SqlIntercode seed : seeds)
            for(VariableBinding binding : seed.getVariableBindings().getValues())
                seedRestrictions.add(binding.getVariable(), binding.getClasses());

        if(fromEnd)
            return create(request, seeded, reverseNext, startVar, reverseJoinVar, stopVar, graphVar, seedRestrictions,
                    null, null);

        return create(request, seeded, next, startVar, joinVar, stopVar, graphVar, seedRestrictions, null, null);
    }


    /**
     * Initial solutions of a seeded walk (see {@link #seed}): the seeds alone, with the end variable bound to the
     * start, when one of them reads a source of the zero-length paths; the seeds joined with the initial solutions
     * otherwise.
     *
     * @param request the current request
     * @param init the initial solutions
     * @param seeds the siblings seeding the walk
     * @param startVar the start variable
     * @param endVar the end variable
     * @return initial solutions of the seeded walk
     */
    private static SqlIntercode seedInit(Request request, SqlIntercode init, List<SqlIntercode> seeds,
            Variable startVar, Variable endVar)
    {
        for(SqlIntercode seed : seeds)
        {
            if(seed instanceof SqlTableAccess access && enumeratesNodesOf(init, access, startVar, endVar))
            {
                SqlIntercode joined = seeds.size() == 1 ? seed : SqlJoin.join(request, seeds);

                return SqlBind.bind(request, endVar,
                        SqlVariable.create(request.getConfiguration(), joined.getVariableBindings().get(startVar)),
                        joined);
            }
        }

        return joinSeeds(request, seeds, init);
    }


    /**
     * The seeds joined with the initial solutions: branch by branch of a union, under the deduplication of the
     * zero-length paths when the seeds are plain table accesses or values, and as a whole otherwise.
     *
     * @param request the current request
     * @param seeds the siblings seeding the walk
     * @param init the initial solutions
     * @return the seeds joined with the initial solutions
     */
    private static SqlIntercode joinSeeds(Request request, List<SqlIntercode> seeds, SqlIntercode init)
    {
        if(init instanceof SqlUnion union)
            return SqlUnion.union(request,
                    union.getChilds().stream().map(branch -> joinSeeds(request, seeds, branch)).toList());

        if(init instanceof SqlDistinct distinct
                && seeds.stream().allMatch(s -> s instanceof SqlTableAccess || s instanceof SqlValues))
        {
            // the seeds are distinct over the variables they share with the deduplicated solutions, so the join may
            // be deduplicated as a whole
            Set<Variable> variables = new HashSet<>(distinct.getDistinctVariables());

            for(SqlIntercode seed : seeds)
                variables.addAll(seed.getVariableBindings().getVariables());

            return SqlDistinct.create(request, joinSeeds(request, seeds, distinct.getChild()), variables);
        }

        List<SqlIntercode> childs = new ArrayList<>(seeds);
        childs.add(init);

        return SqlJoin.join(request, childs);
    }


    /**
     * True if the initial solutions are the zero-length paths over the nodes of the graph, and one of their sources is
     * the table the seed reads, under conditions no stronger than those of the seed, with the start variable bound to
     * the columns the seed binds it to: every value of the seed is then a node of the graph.
     *
     * @param init the initial solutions
     * @param seed a sibling seeding the walk
     * @param startVar the start variable
     * @param endVar the end variable
     * @return true if the initial solutions enumerate the nodes of the graph and a source of them subsumes the seed,
     *         false otherwise
     */
    private static boolean enumeratesNodesOf(SqlIntercode init, SqlTableAccess seed, Variable startVar, Variable endVar)
    {
        if(init instanceof SqlDistinct distinct)
            return enumeratesNodesOf(distinct.getChild(), seed, startVar, endVar);

        if(init instanceof SqlUnion union)
            return union.getChilds().stream().anyMatch(branch -> enumeratesNodesOf(branch, seed, startVar, endVar));

        if(!(init instanceof SqlTableAccess source) || source.getTable() == null
                || !source.getTable().equals(seed.getTable()))
            return false;

        if(!seed.getConditions().implies(source.getConditions()))
            return false;

        VariableBinding start = source.getInternalVariableBinding(startVar);

        // a zero-length path binds its end to the columns of its start
        if(start == null || !sameColumns(start, seed.getInternalVariableBinding(startVar))
                || !sameColumns(start, source.getInternalVariableBinding(endVar)))
            return false;

        // the other variables of the source (the graph) have to agree with the seed
        for(Variable variable : source.getInternalVariableBindings().getVariables())
            if(!variable.equals(startVar) && !variable.equals(endVar)
                    && !sameColumns(source.getInternalVariableBinding(variable),
                            seed.getInternalVariableBinding(variable)))
                return false;

        return true;
    }


    /**
     * True if both bindings exist, are never null and map the same classes to the same columns.
     *
     * @param binding a binding
     * @param other another binding, or null
     * @return true if both bindings exist, are never null and map the same classes to the same columns, false otherwise
     */
    private static boolean sameColumns(VariableBinding binding, VariableBinding other)
    {
        return binding != null && other != null && !binding.canBeNull() && !other.canBeNull()
                && binding.getMappings().equals(other.getMappings());
    }


    /**
     * True if the walk has a solution for every solution of the node, so that a left join with the walk is a join (see
     * {@link SqlLeftJoin#optimize}): the walk is one of zero or more steps between two variables, the node binds one of
     * them always and shares no other variable with the walk but the graph, and every solution of the node takes the
     * variable (and the graph) from a source of the nodes of the graph, which the zero-length paths enumerate, so that
     * a zero-length path matches it.
     *
     * @param node the node
     * @return true if the walk has a solution for every solution of the node, false otherwise
     */
    protected boolean matchesEverySolutionOf(SqlIntercode node)
    {
        if(beginVar == null)
            return false;

        Variable endVar = endBinding.getVariable();
        VariableBindings nodeBindings = node.getVariableBindings();

        Variable startVar = nodeBindings.get(beginVar) != null ? beginVar : endVar;
        VariableBinding binding = nodeBindings.get(startVar);

        if(binding == null || binding.canBeNull())
            return false;

        for(Variable variable : nodeBindings.getVariables())
            if(!variable.equals(startVar) && !variable.equals(graphVar) && getVariableBindings().get(variable) != null)
                return false;

        return bindsNodes(node, startVar, startVar.equals(beginVar) ? endVar : beginVar);
    }


    /**
     * True if every solution of the node takes the variable (and the graph variable) from a source of the nodes of the
     * graph enumerated by the zero-length paths of the walk (see {@link #enumeratesNodesOf}): the node is such a table
     * access, a join with such a child, a union of such nodes, a filter, a deduplication or a bind of another variable
     * over such a node, or an OPTIONAL or MINUS whose left side is such a node.
     *
     * @param node the node
     * @param startVar the variable, an end of the walk
     * @param otherVar the other end of the walk
     * @return true if every solution of the node takes the variable from a source of the nodes, false otherwise
     */
    private boolean bindsNodes(SqlIntercode node, Variable startVar, Variable otherVar)
    {
        if(node instanceof SqlTableAccess access)
            return enumeratesNodesOf(init, access, startVar, otherVar);

        if(node instanceof SqlJoin join)
            return join.getChilds().stream().anyMatch(child -> bindsNodes(child, startVar, otherVar));

        if(node instanceof SqlUnion union)
            return union.getChilds().stream().allMatch(child -> bindsNodes(child, startVar, otherVar));

        if(node instanceof SqlFilter filter)
            return bindsNodes(filter.getChild(), startVar, otherVar);

        if(node instanceof SqlDistinct distinct)
            return bindsNodes(distinct.getChild(), startVar, otherVar);

        if(node instanceof SqlBind bind)
            return !bind.getVariable().equals(startVar) && bindsNodes(bind.getChild(), startVar, otherVar);

        if(node instanceof SqlLeftJoin join)
            return bindsNodes(join.getLeft(), startVar, otherVar);

        if(node instanceof SqlMinus minus)
            return bindsNodes(minus.getLeft(), startVar, otherVar);

        if(node instanceof SqlStripConstantColumns strip)
            return bindsNodes(strip.getChild(), startVar, otherVar);

        return false;
    }


    /**
     * True if the walk should start from its end variable: the parent accepts the end in narrower classes than the
     * start (every accepted class of the end is a subclass of an accepted class of the start, but not the other way
     * round), or restricts the end alone. Walked from the end, the recursion enumerates only the pairs ending in nodes
     * the parent accepts, instead of every pair starting anywhere, of which the parent keeps the same ones.
     *
     * @param restrictions what the parent needs of the variables
     * @return true if the walk should start from its end variable, false otherwise
     */
    private boolean startsBetterFromEnd(Restrictions restrictions)
    {
        Set<ResourceClass> end = restrictions.getAcceptedClasses(endBinding.getVariable());
        Set<ResourceClass> begin = restrictions.getAcceptedClasses(beginVar);

        if(end == null)
            return false;

        if(begin == null)
            return true;

        return isNarrower(end, begin) && !isNarrower(begin, end);
    }


    /**
     * True if every class of the first set is a subclass of (or equal to) some class of the second set.
     *
     * @param classes the classes
     * @param others the other classes
     * @return true if every class of the first set is a subclass of some class of the second set, false otherwise
     */
    private static boolean isNarrower(Set<ResourceClass> classes, Set<ResourceClass> others)
    {
        return classes.stream().allMatch(c -> others.stream().anyMatch(o -> c.equals(o) || c.isSubclassOf(o)));
    }


    @Override
    public SqlIntercode optimize(Request request, Restrictions restrictions, boolean reduced, boolean evalServices)
    {
        // when the parent needs neither an end of the walk nor the multiplicities (a DISTINCT over the other end, the
        // right side of a MINUS, an EXISTS pattern), only the existence of a path from or to a node matters, and a node
        // has a path iff it has an initial solution (a zero-length path, or a first step, whose last step is one too),
        // so the walk reduces to its initial solutions; the begin may be left out only when the initial solutions
        // start anywhere, which those of a seeded walk do not
        if(reduced && (!restrictions.containsVar(endBinding.getVariable())
                || reverseNext != null && !restrictions.containsVar(beginVar)))
            return init.optimize(request, restrictions, true, evalServices);

        ClassRelations relations = request.getConfiguration();

        // a walk between two variables starts from the end the parent restricts more: the same step walked the other
        // way round, with the end variable for the start and the start variable for the end
        if(reverseNext != null && startsBetterFromEnd(restrictions))
            return create(request, init, reverseNext, endBinding.getVariable(), reverseJoinVar, beginVar, graphVar,
                    restrictions, next, joinVar).optimize(request, restrictions, reduced, evalServices);

        Variable endVar = endBinding.getVariable();

        Restrictions childRestrictions = new Restrictions(restrictions);

        childRestrictions.add(endVar);
        childRestrictions.add(joinVar); //TODO: set appropriate resource classes

        // the children deliver the nodes of the walk, not only the values the parent sees: a node of a class the
        // parent does not accept may still lead to one it does, so the children have to keep every node (the step
        // branches that cannot lead to an accepted class are left out below)
        childRestrictions.removeAcceptedClasses(endVar);
        childRestrictions.removeAcceptedClasses(joinVar);

        if(graphVar != null)
            childRestrictions.add(graphVar);

        if(beginVar != null)
            childRestrictions.add(beginVar);

        SqlIntercode initOpt = init.optimize(request, childRestrictions, reduced, evalServices);
        SqlIntercode nextOpt = next.optimize(request, childRestrictions, reduced, evalServices);


        if(initOpt.equals(SqlNoSolution.get()))
            return SqlNoSolution.get();

        if(beginVar != null && initOpt instanceof SqlUnion union)
        {
            List<SqlIntercode> segs = SqlDistinct.expandUnionByResourceClasses(request, union, Set.of(beginVar));

            if(segs.size() > 1)
            {
                List<SqlIntercode> childs = new ArrayList<>();

                for(SqlIntercode child : segs)
                    childs.add(create(request, child, nextOpt, beginVar, joinVar, endBinding.getVariable(), graphVar,
                            restrictions, reverseNext, reverseJoinVar));

                // the segments are walks, which care about the multiplicities as much as their parent does
                return SqlUnion.union(request, childs).optimize(request, restrictions, reduced, evalServices);
            }
        }

        while(nextOpt instanceof SqlUnion union)
        {
            VariableBinding endBinding = createEndVariableBinding(request, endVar, initOpt, nextOpt);

            List<SqlIntercode> childs = new ArrayList<>();

            for(SqlIntercode child : union.getChilds())
                if((new VariableBindingPair(relations, endBinding, child.getVariableBindings().get(joinVar)))
                        .isJoinable())
                    childs.add(child);

            if(childs.size() == union.getChilds().size())
                break;

            nextOpt = SqlUnion.union(request, childs).optimize(request, childRestrictions, true, evalServices);
        }


        // a branch is left out when the walk cannot take it: its start is disjoint with every class reachable from
        // the initial nodes, or it delivers only nodes of classes the parent does not accept from which no branch
        // leads to an accepted class (the parent drops the nodes it delivers, and so everything reached from them)
        Set<ResourceClass> accepted = restrictions.getAcceptedClasses(endVar);

        while(nextOpt instanceof SqlUnion union)
        {
            Set<ResourceClass> reachable = expand(relations, union.getChilds(),
                    initOpt.getVariable(endVar).getClasses(), joinVar, endVar);
            Set<ResourceClass> useful = accepted == null ? null :
                    expand(relations, union.getChilds(), accepted, endVar, joinVar);

            List<SqlIntercode> childs = union.getChilds().stream()
                    .filter(c -> overlaps(relations, c, joinVar, reachable)
                            && (useful == null || overlaps(relations, c, endVar, useful)))
                    .toList();

            if(childs.size() == union.getChilds().size())
                break;

            nextOpt = SqlUnion.union(request, childs);
        }

        if(accepted != null && !(nextOpt instanceof SqlUnion) && !overlaps(relations, nextOpt, endVar, accepted))
            nextOpt = SqlNoSolution.get();


        if(nextOpt.equals(SqlNoSolution.get()))
            return SqlDistinct.create(request, initOpt, initOpt.getVariableBindings().getVariables());

        if(!(new VariableBindingPair(relations, initOpt.getVariableBindings().get(endVar),
                nextOpt.getVariableBindings().get(joinVar))).isJoinable())
            return SqlDistinct.create(request, initOpt, initOpt.getVariableBindings().getVariables());


        if(initOpt == init && nextOpt == next && restrictions.isOptimized(relations, bindings))
            return this;

        return create(request, initOpt, nextOpt, beginVar, joinVar, endBinding.getVariable(), graphVar, restrictions,
                reverseNext, reverseJoinVar);
    }


    @Override
    public String translate(Request request)
    {
        ClassRelations relations = request.getConfiguration();

        List<ResourceClass> endVarClasses = new LinkedList<>(endBinding.getClasses()); // to stable order

        StringBuilder builder = new StringBuilder();

        builder.append("WITH RECURSIVE recursion(");

        builder.append(ev.stream().map(Object::toString).collect(joining(", ")));

        boolean hasRecursionVariable = !ev.isEmpty();

        for(ResourceClass resClass : endVarClasses)
        {
            List<Column> columns = endBinding.getMapping(resClass);

            for(int j = 0; j < resClass.getColumnCount(); j++)
            {
                if(columns.get(j) instanceof ConstantColumn)
                    continue;

                appendComma(builder, hasRecursionVariable);
                hasRecursionVariable = true;

                builder.append(columns.get(j));
            }
        }

        if(!hasRecursionVariable)
            builder.append("\"@none\"");

        builder.append(") AS (SELECT ");

        builder.append(iv.stream().map(Object::toString).collect(joining(", ")));

        boolean hasInitSelect = !iv.isEmpty();

        Variable endVar = endBinding.getVariable();
        VariableBinding initEndBinding = init.getVariableBindings().get(endVar);

        for(ResourceClass resClass : endVarClasses)
        {
            List<Column> columns = initEndBinding.deriveMapping(relations, resClass);

            for(int j = 0; j < resClass.getColumnCount(); j++)
            {
                if(endBinding.getMapping(resClass).get(j) instanceof ConstantColumn)
                    continue;

                appendComma(builder, hasInitSelect);
                hasInitSelect = true;

                // each column of the non-recursive term has to have the collation of the whole query, which the
                // recursive term changes when it delivers another collation than the default one
                String collation = getCollation(request, List.of(next), endVar, resClass, j);

                if(collation != null && !collation.equals(getCollation(request, List.of(init), endVar, resClass, j)))
                {
                    builder.append("(");
                    builder.append(columns.get(j));
                    builder.append(") COLLATE ");
                    builder.append(collation);
                }
                else
                {
                    builder.append(columns.get(j));
                }

                builder.append(" AS ");
                builder.append(endBinding.getMapping(resClass).get(j));
            }
        }

        if(!hasInitSelect)
            builder.append("1");

        builder.append(" FROM (");
        builder.append(init.translate(request));
        builder.append(") AS tab");

        builder.append(" UNION SELECT ");

        // a step that is a union whose branches deliver the node in several disjoint classes is joined laterally,
        // branch by branch with a condition of its own: the condition of the union as a whole is a disjunction over the
        // class pairs, which the database can neither hash nor look up by an index, whereas it joins a single pair
        // through the index of the branch (the step of a single class stays a plain join, so that the database may
        // still choose a hash join when the recursion grows large)
        boolean lateral = next instanceof SqlUnion && endBinding.getClasses().size() > 1;
        AliasTable left = lateral ? walkTable : leftTable;
        AliasTable right = lateral ? stepTable : rightTable;

        builder.append(ev.stream().map(c -> c.fromTable(left).toString()).collect(joining(", ")));

        boolean hasUnionSelect = !ev.isEmpty();

        VariableBinding nextEndBinding = next.getVariableBindings().get(endVar);

        for(ResourceClass resClass : endVarClasses)
        {
            // the step delivers the node through its alias, and a conversion to the class of the recursion cannot be
            // qualified afterwards, so it is built over the qualified columns
            List<Column> columns = nextEndBinding != null ? nextEndBinding.deriveMapping(relations, resClass, right) :
                    null;

            for(int j = 0; j < resClass.getColumnCount(); j++)
            {
                if(endBinding.getMapping(resClass).get(j) instanceof ConstantColumn)
                    continue;

                appendComma(builder, hasUnionSelect);
                hasUnionSelect = true;

                if(columns != null)
                    builder.append(columns.get(j));
                else
                    builder.append("NULL");
            }
        }

        if(!hasUnionSelect)
            builder.append("1");

        builder.append(" FROM recursion AS ");
        builder.append(left);

        VariableBindings tmp = new VariableBindings(bindings);
        tmp.remove(endBinding.getVariable());
        tmp.add(new VariableBinding(joinVar, endBinding.getMappings(), endBinding.canBeNull()));

        if(lateral)
        {
            builder.append(" CROSS JOIN LATERAL (");
            builder.append(((SqlUnion) next).translate(request, (branch, alias) -> generateJoinCondition(relations, tmp,
                    branch.getVariableBindings(), left, alias)));
            builder.append(") AS ");
            builder.append(right);
        }
        else
        {
            builder.append(", (");
            builder.append(next.translate(request));
            builder.append(") AS ");
            builder.append(right);

            String condition = generateJoinCondition(relations, tmp, next.getVariableBindings(), left, right);

            if(condition != null)
            {
                builder.append(" WHERE ");
                builder.append(condition);
            }
        }

        builder.append(") SELECT ");

        Set<Column> columns = getVariableBindings().getNonConstantColumns();

        if(!columns.isEmpty())
            builder.append(columns.stream().map(Object::toString).collect(joining(", ")));
        else
            builder.append("1");

        builder.append(" FROM recursion");

        return builder.toString();
    }


    /**
     * Classes of the nodes the step branches connect with nodes of the given classes: the closure of the classes under
     * the branches, each taken from the variable {@code from} to the variable {@code to} (from the current end to the
     * new one for the nodes reachable from the initial ones, in the other direction for the nodes from which an
     * accepted class is reachable).
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param branches the branches of the step
     * @param classes the classes to start from
     * @param from variable of the branches that has to overlap the classes
     * @param to variable of the branches whose classes are added
     * @return classes of the nodes the step branches connect with nodes of the given classes
     */
    private static Set<ResourceClass> expand(ClassRelations relations, List<SqlIntercode> branches,
            Set<ResourceClass> classes, Variable from, Variable to)
    {
        Set<ResourceClass> result = new HashSet<>(classes);

        while(true)
        {
            Set<ResourceClass> additional = new HashSet<>(result);

            for(SqlIntercode branch : branches)
                if(overlaps(relations, branch, from, result) && branch.getVariable(to) != null)
                    additional.addAll(branch.getVariable(to).getClasses());

            if(additional.equals(result))
                return result;

            result = additional;
        }
    }


    /**
     * True if the branch binds the variable in a class overlapping one of the given classes.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param branch a branch of the step
     * @param variable the variable
     * @param classes the classes
     * @return true if the branch binds the variable in a class overlapping one of the given classes, false otherwise
     */
    private static boolean overlaps(ClassRelations relations, SqlIntercode branch, Variable variable,
            Set<ResourceClass> classes)
    {
        return branch.getVariable(variable) != null
                && !areDisjunct(relations, classes, branch.getVariable(variable).getClasses());
    }


    /**
     * Binding of the end variable over the disjoint classes it takes in the initial and the recursive step.
     *
     * @param request the current request
     * @param endVar the end variable
     * @param init the initial solutions
     * @param next the recursive step
     * @return binding of the end variable, or null when it is unbound on both sides
     */
    private static VariableBinding createEndVariableBinding(Request request, Variable endVar, SqlIntercode init,
            SqlIntercode next)
    {
        VariableBinding initEndBinding = init.getVariableBindings().get(endVar);
        VariableBinding nextEndBinding = next.getVariableBindings().get(endVar);

        List<VariableBinding> defs = Stream.of(initEndBinding, nextEndBinding).filter(Objects::nonNull).toList();

        VariableBinding endBinding = new VariableBinding(endVar, false);

        for(ResourceClass resClass : unionResourceClasses(request.getConfiguration(), defs))
            endBinding.addMapping(resClass, resClass.createColumns(request.getColumnMap(), endVar));

        //TODO: handle constant columns

        return endBinding;
    }


    @Override
    public boolean isDistinct(Request request, Collection<Variable> selected)
    {
        // the recursion unites its rows as a set over the carried columns and the end node, so the solutions are
        // distinct over the variables when they cover every carried variable that has columns, exposed or not
        if(!selected.contains(endBinding.getVariable()))
            return false;

        return carried.stream().allMatch(v -> selected.contains(v) || init.getVariableBindings().get(v).isConstant());
    }


    @Override
    public boolean hasServiceSubpattern()
    {
        return init.hasServiceSubpattern() || next.hasServiceSubpattern();
    }


    @Override
    public Set<VirtualTable> getVirtualTables()
    {
        return getVirtualTables(init, next);
    }


    @Override
    public String getCollation(Request request, Variable variable, ResourceClass resClass, int index)
    {
        // the end variable takes the collation of the recursive step when it differs (see translate)
        if(variable.equals(endBinding.getVariable()))
        {
            String collation = getCollation(request, List.of(next), variable, resClass, index);

            if(collation != null)
                return collation;
        }

        return getCollation(request, List.of(init), variable, resClass, index);
    }


    @Override
    public void generateExplanation(StringBuilder builder, String indent)
    {
        builder.append("recurse");

        indentChild(builder, indent, false);
        init.generateExplanation(builder, getIndent(indent, false));

        indentChild(builder, indent, true);
        next.generateExplanation(builder, getIndent(indent, true));
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(!(object instanceof SqlRecursive imcode))
            return false;

        if(!super.equals(imcode))
            return false;

        if(!Objects.equals(graphVar, imcode.graphVar))
            return false;

        if(!Objects.equals(beginVar, imcode.beginVar))
            return false;

        if(!Objects.equals(joinVar, imcode.joinVar))
            return false;

        if(!Objects.equals(endBinding.getVariable(), imcode.endBinding.getVariable()))
            return false;

        if(!Objects.equals(init, imcode.init))
            return false;

        if(!Objects.equals(next, imcode.next))
            return false;

        if(!Objects.equals(reverseNext, imcode.reverseNext))
            return false;

        if(!Objects.equals(reverseJoinVar, imcode.reverseJoinVar))
            return false;

        return true;
    }


    @Override
    protected int getHashCode()
    {
        return Objects.hash(graphVar, beginVar, joinVar, endBinding, init, next, reverseNext, reverseJoinVar);
    }
}
