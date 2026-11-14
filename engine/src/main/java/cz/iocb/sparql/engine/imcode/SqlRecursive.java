package cz.iocb.sparql.engine.imcode;

import static cz.iocb.sparql.engine.mapping.classes.ResourceClass.areDisjunct;
import static java.util.stream.Collectors.toSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
import cz.iocb.sparql.engine.database.Condition.ColumnComparison;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.SqlBuilder;
import cz.iocb.sparql.engine.database.SqlCode;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.imcode.expression.SqlExpressionIntercode;
import cz.iocb.sparql.engine.imcode.expression.SqlExpressionIntercode.Restriction;
import cz.iocb.sparql.engine.imcode.expression.SqlNull;
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
 * and whose variables it delivers in place of the join. When the parent needs neither the end of the walk nor the
 * multiplicities, the walk reduces to its initial solutions, which start in the same nodes, and so does a walk starting
 * anywhere of which the parent does not need the start; another walk of which it does not need the start is walked
 * without it (see {@link #optimize}). Since the zero-length paths match every node, an OPTIONAL walk of zero or more
 * steps from nodes the left side binds always is a join (see {@link #matchesEverySolutionOf}). A walk inside an EXISTS
 * pattern may start directly from the value of an expression, the node the current solution of the pattern gives (see
 * {@link #start}): the recursion then takes only the initial solutions that start in the value, unless siblings
 * expected to be fewer seed the walk from its other end. The optimisation restricts the initial solutions of a walk to
 * the classes of the start the parent accepts, and the nodes above then rely on the classes the walk binds its
 * variables in, so whatever becomes of the walk keeps them (see {@link #turn}, {@link #delivered}).
 */
public final class SqlRecursive extends SqlIntercode
{
    /**
     * Alias of the initial solutions in the non-recursive term.
     */
    private static final AliasTable initTable = new AliasTable("tab");

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
     * Initial solutions of the walk turned the other way round (see {@link #reverseNext}): those the walk was created
     * with (the first steps, or the zero-length paths), valid in either direction, before the optimisation restricted
     * them to what the start variable may take (the classes the parent accepts, the classes of a segment, or the value
     * the walk starts from, see {@link #start}), which is no restriction of the initial solutions of the turned walk,
     * as the start variable becomes its end: the turned walk passes through the nodes of every class and delivers its
     * end in the classes this walk starts in (see {@link #turn}). Null when the walk cannot be turned.
     */
    private final SqlIntercode reverseInit;

    /**
     * Classes of the start variable the initial solutions of a segment are restricted to: a walk whose initial
     * solutions bind the start variable in disjoint classes is split into a union of segments, one per class (see
     * {@link #optimize}). Null when the walk is no segment.
     */
    private final Set<ResourceClass> segmentClasses;

    /**
     * Bindings of the variables of the initial solutions carried through the recursion unchanged (the start and graph
     * variables and the others the parent needs) in the columns of the recursion, as the initial solutions bind them:
     * also the variables and the classes the bindings leave out, since the bindings hold what the parent needs, whereas
     * the recursion carries the variables whole and joins the step with them.
     */
    private final VariableBindings carried;

    /**
     * True when the initial solutions are seeds taken from the siblings (see {@link #seed}); such a walk is seeded no
     * more.
     */
    public final boolean seeded;

    /**
     * Values the walk starts from, by the variables of the initial solutions they are the values of (the start
     * variable, and the graph variable when the walk stays in one graph): expressions over the current solution of the
     * EXISTS pattern the walk lies in, which every solution of the walk extends. The recursion takes only the initial
     * solutions compatible with the values, so it enumerates the paths from the node the current solution gives instead
     * of the paths from all nodes, of which the pattern would keep the same ones: the table accesses of the initial
     * solutions take the values into their conditions (see {@link Restrictions#getAcceptedValue}), so that the database
     * looks the node up by the indexes of the tables, and the non-recursive term compares the values they have not
     * taken; the steps of a walk that starts in a graph are restricted to that graph likewise. The values restrict the
     * recursion only: a walk reduced to its initial solutions (see {@link #optimize}) takes them as it was created with
     * them, when it has kept them (see {@link #reverseInit}), and leaves the comparison with the current solution to
     * the pattern, which the database then evaluates once for all current solutions, and so does a walk its siblings
     * seed from its other end (see {@link #seed}), which gives the values up. Such a walk is not turned by the
     * restrictions of its parent, because it starts from a single node already. Empty when the walk starts from all its
     * initial solutions.
     */
    private final Map<Variable, SqlExpressionIntercode> start;

    /**
     * Classes of the end variable the walk delivers its solutions in (classes of {@link #endBinding}): a solution
     * ending in a node of another class is left out of the result, though the recursion keeps such nodes, because a
     * path may lead through them to a node that is delivered. A walk turned the other way round (see {@link #turn})
     * delivers its end, the start of the walk it replaces, only in the classes that walk started in: its initial
     * solutions were restricted to them, and the nodes above rely on the classes of the bindings they were given (a
     * filter leaves out a condition that those classes decide), so a solution of another class must not come back. Null
     * when the walk delivers every node.
     */
    private final Set<ResourceClass> delivered;


    /**
     * The parts a walk turned the other way round differs in (see {@link #turn}).
     *
     * @param init the initial solutions of the turned walk
     * @param delivered classes the turned walk delivers its end in, the classes the walk it replaces starts in; null
     *            when the turned walk reaches nodes of these classes only
     */
    private record Turned(SqlIntercode init, Set<ResourceClass> delivered)
    {
    }


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
     * @param reverseInit the initial solutions of the walk turned the other way round, or null
     * @param segmentClasses classes of the start variable the initial solutions of a segment are restricted to, or null
     * @param carried bindings of the variables of the initial solutions carried through the recursion in its columns
     * @param seeded whether the initial solutions are seeds taken from the siblings
     * @param start values the walk starts from, by the variables of the initial solutions
     * @param delivered classes of the end variable the walk delivers its solutions in, or null
     */
    protected SqlRecursive(VariableBindings bindings, SqlIntercode init, SqlIntercode next, VariableBinding endBinding,
            Variable joinVar, Variable beginVar, List<Column> iv, List<Column> ev, Variable graphVar,
            SqlIntercode reverseNext, Variable reverseJoinVar, SqlIntercode reverseInit,
            Set<ResourceClass> segmentClasses, VariableBindings carried, boolean seeded,
            Map<Variable, SqlExpressionIntercode> start, Set<ResourceClass> delivered)
    {
        super(bindings, init.isDeterministic() && next.isDeterministic()
                && start.values().stream().allMatch(SqlExpressionIntercode::isDeterministic));
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
        this.reverseInit = reverseInit;
        this.segmentClasses = segmentClasses;
        this.carried = carried;
        this.seeded = seeded;
        this.start = start;
        this.delivered = delivered;
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
        return create(request, init, next, beginVar, joinVar, endVar, graphVar, null, null, null, null, null, false,
                Map.of(), null);
    }


    /**
     * Recursive closure of {@code next} started from {@code init} between two variables, which may also be walked the
     * other way round by {@code reverseNext} (see {@link #reverseNext}), and which may start directly from the values
     * of expressions (see {@link #start}): the initial solutions, which start in all nodes, are then restricted to
     * those compatible with the values, and only the siblings of the walk make it walk the other way round (see
     * {@link #seed}).
     *
     * @param request the current request
     * @param init the initial solutions
     * @param next the recursive step
     * @param beginVar the start variable
     * @param joinVar variable of the step bound to the current end node
     * @param endVar the end variable
     * @param graphVar the graph variable, or null
     * @param reverseNext the step of the walk in the other direction, or null when the walk cannot be turned
     * @param reverseJoinVar variable of the reverse step bound to the node it reaches, or null
     * @param start values the walk starts from, by the variables of the initial solutions (the start variable, and
     *            possibly the graph variable); empty when the walk starts from all its initial solutions
     * @return recursive closure of {@code next} started from {@code init}
     */
    public static SqlIntercode create(Request request, SqlIntercode init, SqlIntercode next, Variable beginVar,
            Variable joinVar, Variable endVar, Variable graphVar, SqlIntercode reverseNext, Variable reverseJoinVar,
            Map<Variable, SqlExpressionIntercode> start)
    {
        return create(request, init, next, beginVar, joinVar, endVar, graphVar, null, reverseNext, reverseJoinVar,
                reverseNext != null ? init : null, null, false, start, null);
    }


    /**
     * Recursion exposing only what the parent needs; the start and graph variables get fresh output columns, and so
     * does every other variable of the initial solutions the parent needs, which the recursion carries along unchanged
     * (the variables of the siblings a walk is seeded with, see {@link #seed}). A variable of the initial solutions
     * nobody needs is left out, because carrying it would multiply the paths walked by its values. The end variable is
     * exposed in the classes the walk delivers it in (see {@link #delivered}).
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
     * @param reverseInit the initial solutions of the walk turned the other way round, or null
     * @param segmentClasses classes of the start variable the initial solutions of a segment are restricted to, or null
     * @param seeded whether the initial solutions are seeds taken from the siblings
     * @param start values the walk starts from, by the variables of the initial solutions (see {@link #start})
     * @param delivered classes the walk delivers its end in: of the classes its nodes take, those that are subclasses
     *            of the given ones (see {@link #delivered}); null to deliver every node
     * @return recursion exposing only what the parent needs; the carried variables get fresh output columns; no
     *         solution when the initial solutions do not bind the end variable, or when no node is delivered; null when
     *         the nodes take a class that overlaps a class to deliver without being its subclass, as the nodes of that
     *         class to deliver cannot be told apart
     */
    protected static SqlIntercode create(Request request, SqlIntercode init, SqlIntercode next, Variable beginVar,
            Variable joinVar, Variable endVar, Variable graphVar, Restrictions restrictions, SqlIntercode reverseNext,
            Variable reverseJoinVar, SqlIntercode reverseInit, Set<ResourceClass> segmentClasses, boolean seeded,
            Map<Variable, SqlExpressionIntercode> start, Set<ResourceClass> delivered)
    {
        // initial solutions that do not bind the end variable have no solution, so no path starts
        if(init.getVariableBindings().get(endVar) == null)
            return SqlNoSolution.get();

        ClassRelations relations = request.getConfiguration();

        Map<Column, Column> map = new HashMap<>();
        VariableBindings inner = new VariableBindings();
        VariableBindings carried = new VariableBindings();

        for(VariableBinding binding : init.getVariableBindings().getValues())
        {
            Variable variable = binding.getVariable();

            if(variable.equals(endVar))
                continue;

            if(!variable.equals(beginVar) && !variable.equals(graphVar)
                    && (restrictions == null || !restrictions.containsVar(variable)))
                continue;

            inner.add(binding);

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

            carried.add(v);
        }

        VariableBindings bindings = new VariableBindings(carried);
        VariableBinding endBinding = createEndVariableBinding(request, endVar, init, next);

        // the classes of the nodes that are delivered, when the solutions ending in the other nodes are left out
        Set<ResourceClass> endClasses = null;

        if(endBinding != null && delivered != null)
        {
            endClasses = new HashSet<>();

            for(ResourceClass resClass : endBinding.getClasses())
            {
                if(isNarrower(Set.of(resClass), delivered))
                    endClasses.add(resClass);
                else if(!areDisjunct(relations, Set.of(resClass), delivered))
                    return null;
            }

            if(endClasses.isEmpty())
                return SqlNoSolution.get();

            if(endClasses.size() == endBinding.getClasses().size())
                endClasses = null;
        }

        if(endBinding != null && endClasses == null)
            bindings.add(endBinding);
        else if(endBinding != null)
            bindings.add(getDeliveredBinding(endBinding, endClasses));


        List<Column> innerColumns = new ArrayList<>(inner.getNonConstantColumns());
        List<Column> outerColumns = innerColumns.stream().map(c -> map.get(c)).toList();

        return new SqlRecursive(bindings.restrict(relations, restrictions), init, next, endBinding, joinVar, beginVar,
                innerColumns, outerColumns, graphVar, reverseNext, reverseJoinVar, reverseInit, segmentClasses, carried,
                seeded, start, endClasses);
    }


    /**
     * Binding of the end variable in the classes the walk delivers it in.
     *
     * @param endBinding binding of the end variable in the classes of the nodes of the walk
     * @param delivered the classes the walk delivers its end in
     * @return binding of the end variable in the delivered classes
     */
    private static VariableBinding getDeliveredBinding(VariableBinding endBinding, Set<ResourceClass> delivered)
    {
        VariableBinding binding = new VariableBinding(endBinding.getVariable(), endBinding.canBeNull());

        for(Entry<ResourceClass, List<Column>> entry : endBinding.getMappings().entrySet())
            if(delivered.contains(entry.getKey()))
                binding.addMapping(entry.getKey(), entry.getValue());

        return binding;
    }


    /**
     * The walk seeded with the solutions of its siblings in a join: the initial solutions are joined with the seeds on
     * the start variable (on the end variable when the walk is turned, the reverse step then walking from it), so that
     * the recursion enumerates only the paths that start in their values, and the variables of the seeds are carried
     * through the recursion, so that the join needs the seeds no more and they are evaluated once. A seed has to bind
     * the start variable without a null and distinct solutions over it: the recursion unites its rows as a set, so
     * several solutions of a seed for one start would collapse into one, and the paths walked would multiply by their
     * number. A seed binding the graph the walk carries without a null needs distinct solutions over the start and the
     * graph only, as the carried graph keeps the solutions of different graphs apart. A seed read from a table that is
     * a source of the zero-length paths delivers nodes of the graph, so the zero-length paths are its own rows;
     * otherwise plain table accesses and values are joined under the deduplication of the zero-length paths (the join
     * is then distributed over the sources, which the database looks up by their indexes), and other seeds are joined
     * with the initial solutions as a whole, so that they are not repeated. A walk seeded from its end is turned (see
     * {@link #turn}), starting from the initial solutions it was created with and delivering its new end in the classes
     * it started in. The seeded walk is not turned any more, because the carried variables belong to its start, and it
     * starts from the seeds alone: the values the walk would start from otherwise (see {@link #start}) are given up,
     * the pattern the walk lies in comparing its solutions with the current solution.
     *
     * @param request the current request
     * @param scope the scope of the walk
     * @param seeds the siblings seeding the walk, every one binding the start variable always and distinct over it (or
     *            over it and the graph the walk carries, which it binds always)
     * @param fromEnd true when the seeds bind the end variable, so that the walk is turned (see {@link #reverseNext});
     *            only when the walk may be seeded from its end so (see {@link #canSeedFromEnd})
     * @param restrictions what the parent and the siblings of the walk need of its variables
     * @return the seeded walk, or null when the walk cannot be turned to be seeded from its end, or when the seeds make
     *         it take its end in a class of which it could not tell the nodes it delivers apart (see
     *         {@link #delivered})
     */
    protected SqlIntercode seed(Request request, Scope scope, List<SqlIntercode> seeds, boolean fromEnd,
            Restrictions restrictions)
    {
        Variable startVar = fromEnd ? endBinding.getVariable() : beginVar;
        Variable stopVar = fromEnd ? beginVar : endBinding.getVariable();

        Turned turned = fromEnd ? turn(request, scope, seeds) : null;

        if(fromEnd && turned == null)
            return null;

        SqlIntercode seeded = seedInit(request, fromEnd ? turned.init() : init, seeds, startVar, stopVar);

        Restrictions seedRestrictions = new Restrictions(restrictions);

        for(SqlIntercode seed : seeds)
            for(VariableBinding binding : seed.getVariableBindings().getValues())
                seedRestrictions.add(binding.getVariable(), binding.getClasses());

        if(fromEnd)
            return create(request, seeded, reverseNext, startVar, reverseJoinVar, stopVar, graphVar, seedRestrictions,
                    null, null, null, null, true, Map.of(), turned.delivered());

        return create(request, seeded, next, startVar, joinVar, stopVar, graphVar, seedRestrictions, null, null, null,
                null, true, Map.of(), delivered);
    }


    /**
     * True if the walk may be seeded from its end with the given siblings (see {@link #seed}): it may be turned (see
     * {@link #canTurn}) and the initial solutions of the turned walk agree with the seeds (see
     * {@link #turn(Request, Scope, List)}).
     *
     * @param request the current request
     * @param scope the scope of the walk
     * @param seeds the siblings seeding the walk, every one distinct over the end variable and binding it always
     * @param restrictions what the parent and the siblings of the walk need of its variables
     * @return true if the walk may be seeded from its end with the siblings, false otherwise
     */
    protected boolean canSeedFromEnd(Request request, Scope scope, List<SqlIntercode> seeds, Restrictions restrictions)
    {
        return canTurn(restrictions) && turn(request, scope, seeds) != null;
    }


    /**
     * True if the walk starts directly from the values of expressions (see {@link #start}), so from a single node: its
     * siblings then seed it only from its other end, and only when they are expected to be fewer than anything else.
     *
     * @return true if the walk starts from the values of expressions, false otherwise
     */
    protected boolean startsFromValues()
    {
        return !start.isEmpty();
    }


    /**
     * The walk turned the other way round (see {@link #reverseNext}) so that it delivers its variables in the classes
     * this walk delivers them in: the nodes above rely on the classes of the bindings they were given (a filter leaves
     * out a condition that those classes decide, and an expression reads its operands by them), so the turned walk must
     * neither bring back a solution of a class this walk has left out nor present a value in another class. It starts
     * from the initial solutions the walk was created with (see {@link #reverseInit}), of which only those are taken
     * whose start (the end of this walk) and graph take a class this walk delivers a value in, as no other reaches a
     * start of this walk; and it delivers its end (the start of this walk) in the classes this walk starts in, to which
     * the initial solutions of this walk have been restricted, whereas those of the turned walk have to keep the nodes
     * of the other classes, through which a path may lead (see {@link #delivered}). Null when the turned walk cannot do
     * so: when its initial solutions keep a class of the start or of the graph that this walk does not deliver, or when
     * it reaches nodes of a class that overlaps a class this walk starts in without being its subclass, whose values of
     * that class it could not present.
     *
     * @param request the current request
     * @param scope the scope of the walk
     * @return the initial solutions and the delivered classes of the turned walk, or null when the turned walk could
     *         not deliver its variables in the classes of this walk
     */
    private Turned turn(Request request, Scope scope)
    {
        ClassRelations relations = request.getConfiguration();

        Variable endVar = endBinding.getVariable();
        VariableBinding begin = init.getVariable(beginVar);
        VariableBinding graph = graphVar != null ? init.getVariable(graphVar) : null;

        Map<Variable, Set<ResourceClass>> classes = new HashMap<>();
        classes.put(endVar, delivered != null ? delivered : endBinding.getClasses());

        if(graph != null)
            classes.put(graphVar, graph.getClasses());

        SqlIntercode turned = restrict(request, scope, reverseInit, classes);

        if(turned == null || begin == null)
            return null;

        // initial solutions that do not bind the end variable have no solution, and neither has the walk then
        if(turned.getVariable(beginVar) == null)
            return new Turned(turned, null);

        // the nodes of a class to deliver have to be told apart by their class (see create)
        for(ResourceClass resClass : createEndVariableBinding(request, beginVar, turned, reverseNext).getClasses())
            if(!isNarrower(Set.of(resClass), begin.getClasses())
                    && !areDisjunct(relations, Set.of(resClass), begin.getClasses()))
                return null;

        return new Turned(turned, Set.copyOf(begin.getClasses()));
    }


    /**
     * The walk turned to be seeded from its end (see {@link #seed}, {@link #turn(Request, Scope)}). Null also when some
     * initial solution of the turned walk could meet a value of a seed in a class the seed leaves without columns (a
     * class of the initial solutions overlapping such a class and another one), as the turned walk would read it; the
     * seeds, restricted against this walk, leave the classes this walk delivers no value in without columns.
     *
     * @param request the current request
     * @param scope the scope of the walk
     * @param seeds the siblings seeding the walk from its end
     * @return the initial solutions and the delivered classes of the turned walk, or null when the walk cannot be
     *         turned to be seeded so
     */
    private Turned turn(Request request, Scope scope, List<SqlIntercode> seeds)
    {
        ClassRelations relations = request.getConfiguration();

        Turned turned = turn(request, scope);

        if(turned == null)
            return null;

        for(SqlIntercode seed : seeds)
        {
            for(VariableBinding binding : seed.getVariableBindings().getValues())
            {
                VariableBinding other = turned.init().getVariable(binding.getVariable());

                if(other == null)
                    continue;

                for(Entry<ResourceClass, List<Column>> entry : binding.getMappings().entrySet())
                    if(entry.getValue() == null && other.contains(relations, entry.getKey()))
                        return null;
            }
        }

        return turned;
    }


    /**
     * The solutions of the node that bind the given variables in the given classes, as the node leaves the others out
     * when it is told that they are not accepted (see {@link Restrictions#getAcceptedClasses}); the node keeps the
     * columns it has. Null when the node keeps a class of a variable that is no subclass of the given ones, as its
     * solutions of that class cannot be told apart.
     *
     * @param request the current request
     * @param scope the scope of the node
     * @param node the node
     * @param classes the classes per variable
     * @return the solutions of the node that bind the variables in the classes, or null when the node cannot leave the
     *         others out
     */
    private static SqlIntercode restrict(Request request, Scope scope, SqlIntercode node,
            Map<Variable, Set<ResourceClass>> classes)
    {
        // the node keeps the columns it has: an optimised node may have left the columns of some classes out for good
        Restrictions restrictions = new Restrictions();

        for(VariableBinding binding : node.getVariableBindings().getValues())
            restrictions.set(binding.getVariable(), binding.getMappings().entrySet().stream()
                    .filter(e -> e.getValue() != null).map(Entry::getKey).collect(toSet()));

        classes.forEach(restrictions::addAcceptedClasses);

        SqlIntercode restricted = node.optimize(request, scope, restrictions, false, false);

        for(Entry<Variable, Set<ResourceClass>> entry : classes.entrySet())
        {
            VariableBinding binding = restricted.getVariable(entry.getKey());

            if(binding != null && !isNarrower(binding.getClasses(), entry.getValue()))
                return null;
        }

        return restricted;
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
     * zero-length paths when the seeds are plain table accesses, values or shared solutions, and as a whole otherwise.
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

        if(init instanceof SqlDistinct distinct && seeds.stream()
                .allMatch(s -> s instanceof SqlTableAccess || s instanceof SqlValues || s instanceof SqlShared))
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

        // a source restricted to a value of the current solution of an EXISTS pattern does not hold every node
        if(!(init instanceof SqlTableAccess source) || source.getTable() == null
                || !source.getTable().equals(seed.getTable()) || source.hasValues())
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
     * The variable from which a walk in the node may be seeded with the values the bindings give it: an end of a walk
     * between two variables, not seeded yet, that the bindings bind always (the begin before the end), the node being
     * the walk or a join holding it; null when there is none. The seeds have to be distinct over the variable (see
     * {@link SqlShared#seed}).
     *
     * @param node the node
     * @param bindings the bindings
     * @return the variable from which a walk in the node may be seeded, or null when there is none
     */
    protected static Variable seedVariable(SqlIntercode node, VariableBindings bindings)
    {
        if(node instanceof SqlJoin join)
        {
            for(SqlIntercode child : join.getChilds())
            {
                Variable variable = seedVariable(child, bindings);

                if(variable != null)
                    return variable;
            }

            return null;
        }

        if(!(node instanceof SqlRecursive walk) || walk.beginVar == null || walk.seeded)
            return null;

        VariableBinding begin = bindings.get(walk.beginVar);
        VariableBinding end = bindings.get(walk.endBinding.getVariable());

        if(begin != null && !begin.canBeNull())
            return walk.beginVar;

        if(walk.reverseNext != null && end != null && !end.canBeNull())
            return walk.endBinding.getVariable();

        return null;
    }


    /**
     * True if the node is a walk alone: a walk, or a join of a walk with empty solutions only, as a group pattern
     * holding nothing but a walk is translated. Such a walk is offered the values of the other side of an OPTIONAL or
     * MINUS once optimised, when it may still turn the OPTIONAL into a join (see {@link #matchesEverySolutionOf}),
     * whereas a walk with siblings is offered them first, before the siblings seed it.
     *
     * @param node the node
     * @return true if the node is a walk alone, false otherwise
     */
    protected static boolean isWalkAlone(SqlIntercode node)
    {
        if(node instanceof SqlRecursive)
            return true;

        if(!(node instanceof SqlJoin join))
            return false;

        List<SqlIntercode> childs = join.getChilds().stream().filter(c -> !c.equals(SqlEmptySolution.get())).toList();

        return childs.size() == 1 && isWalkAlone(childs.get(0));
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
        // a walk starting from the values of expressions has no path from the other nodes, and a walk that does not
        // deliver every node leaves out the zero-length paths of the others
        if(beginVar == null || !start.isEmpty() || delivered != null)
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
     * True if the walk should start from its end variable: the parent accepts a single constant term for the end, as a
     * filter requiring one does (see {@link SqlIntercode#addAcceptedValues}), and none for the start; or it accepts the
     * end in narrower classes than the start (every accepted class of the end is a subclass of an accepted class of the
     * start, but not the other way round), or restricts the end alone. Walked from the end, the recursion enumerates
     * only the pairs ending in nodes the parent accepts, instead of every pair starting anywhere, of which the parent
     * keeps the same ones. The parent does not restrict the end alone when the initial solutions have been restricted
     * to some classes of the start already (see {@link #startsInEveryClass}): having seen the bindings of the walk, it
     * accepts the start in any class, because nothing is left for it to drop.
     *
     * @param restrictions what the parent needs of the variables
     * @return true if the walk should start from its end variable, false otherwise
     */
    private boolean startsBetterFromEnd(Restrictions restrictions)
    {
        // a value of a current solution starts a walk by itself (see #start); a constant restricts the end the most
        if(isConstant(restrictions.getAcceptedValue(beginVar)))
            return false;

        if(isConstant(restrictions.getAcceptedValue(endBinding.getVariable())))
            return true;

        Set<ResourceClass> end = restrictions.getAcceptedClasses(endBinding.getVariable());
        Set<ResourceClass> begin = restrictions.getAcceptedClasses(beginVar);

        if(end == null)
            return false;

        if(begin == null)
            return startsInEveryClass();

        return isNarrower(end, begin) && !isNarrower(begin, end);
    }


    /**
     * True if the accepted value is a constant: a term the parent requires, and no value of a current solution.
     *
     * @param value expression giving an accepted value, or null
     * @return true if the accepted value is a constant, false otherwise
     */
    private static boolean isConstant(SqlExpressionIntercode value)
    {
        return value != null && !value.isCurrentValue();
    }


    /**
     * True if the initial solutions of a walk that may be turned bind the start variable in every class those the walk
     * was created with (see {@link #reverseInit}) bind it in. Otherwise the optimisation has restricted them to some
     * classes of the start (the classes the parent accepts, the classes of a segment, the classes of a value the walk
     * starts from), and the nodes above, having seen the bindings, rely on it: a filter whose condition these classes
     * decide is gone. Initial solutions restricted without the loss of a class are restricted by a node above that
     * still needs the start variable, as the bindings do not show it anything.
     *
     * @return true if the initial solutions bind the start variable in every class those the walk was created with do,
     *         false otherwise
     */
    private boolean startsInEveryClass()
    {
        VariableBinding created = reverseInit.getVariable(beginVar);
        VariableBinding current = init.getVariable(beginVar);

        return created != null && current != null && isNarrower(created.getClasses(), current.getClasses());
    }


    /**
     * True if the walk starts from all the initial solutions it was created with, as far as the nodes above can tell:
     * it is a walk between two variables that is not seeded (it may be turned), it is no segment, it starts from no
     * values of expressions, and its initial solutions have lost no class of the start (see
     * {@link #startsInEveryClass}). Only then a node ends a path of the walk iff it ends an initial solution.
     *
     * @return true if the walk starts from all the initial solutions it was created with, false otherwise
     */
    private boolean startsAnywhere()
    {
        return reverseNext != null && segmentClasses == null && start.isEmpty() && startsInEveryClass();
    }


    /**
     * True if the walk may be turned (see {@link #reverseNext}): the turned walk starts from all the initial solutions
     * the walk was created with (see {@link #reverseInit}) and walks the paths of all the segments (see
     * {@link #segmentClasses}), of which it delivers those of its own (see {@link #turn}), so a segment is turned only
     * when the parent accepts the start variable in the classes of the segment alone: the other segments then have no
     * solution the parent keeps, and the paths are walked once.
     *
     * @param restrictions what the parent needs of the variables
     * @return true if the walk may be turned, false otherwise
     */
    protected boolean canTurn(Restrictions restrictions)
    {
        if(reverseNext == null)
            return false;

        if(segmentClasses == null)
            return true;

        Set<ResourceClass> accepted = restrictions.getAcceptedClasses(beginVar);

        return accepted != null && isNarrower(accepted, segmentClasses);
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
    public SqlIntercode optimize(Request request, Scope scope, Restrictions restrictions, boolean reduced,
            boolean evalServices)
    {
        Variable endVar = endBinding.getVariable();

        if(reduced)
        {
            // when the parent needs neither the end of the walk nor the multiplicities (a DISTINCT over the start, the
            // right side of a MINUS, an EXISTS pattern), only the existence of a path from a node matters, and a node
            // starts a path iff it starts an initial solution (a zero-length path, or a first step), so the walk
            // reduces to its initial solutions, unless it leaves the paths to some nodes out (see delivered)
            if(!restrictions.containsVar(endVar) && delivered == null)
                return getStartSolutions(request, scope).optimize(request, scope, restrictions, true, evalServices);

            // when the parent does not need the start, only the existence of a path to a node matters, and a node ends
            // a path iff it ends an initial solution (the last step of a path is a first step too), but only of a walk
            // that starts anywhere: a node may be reached from the starts of another walk through nodes that start
            // none of its initial solutions; the nodes the walk does not deliver are left out
            if(restrictions.containsVar(endVar) && !restrictions.containsVar(beginVar) && startsAnywhere())
            {
                SqlIntercode solutions = delivered == null ? init :
                        restrict(request, scope, init, Map.of(endVar, delivered));

                if(solutions != null)
                    return solutions.optimize(request, scope, restrictions, true, evalServices);
            }

            // a walk that does not start anywhere has to be walked, but without the start the parent does not need:
            // the recursion then enumerates the nodes the walk reaches, not the pairs of a start and a node
            if(beginVar != null && start.isEmpty() && !restrictions.containsVar(beginVar))
            {
                SqlIntercode walk = create(request, init, next, null, joinVar, endVar, graphVar, restrictions, null,
                        null, null, null, seeded, Map.of(), delivered);

                if(walk != null)
                    return walk.optimize(request, scope, restrictions, true, evalServices);
            }
        }

        ClassRelations relations = request.getConfiguration();

        // a walk between two variables starts from the end the parent restricts more: the same step walked the other
        // way round, with the end variable for the start and the start variable for the end, from the initial solutions
        // the walk was created with, as the restrictions of the start no longer apply to them (see turn); a walk
        // starting from the values of expressions starts from a single node already
        if(start.isEmpty() && canTurn(restrictions) && startsBetterFromEnd(restrictions))
        {
            Turned turned = turn(request, scope);

            SqlIntercode walk = turned == null ? null :
                    create(request, turned.init(), reverseNext, endVar, reverseJoinVar, beginVar, graphVar,
                            restrictions, next, joinVar, reverseInit, null, seeded, Map.of(), turned.delivered());

            if(walk != null)
                return walk.optimize(request, scope, restrictions, reduced, evalServices);
        }

        Restrictions childRestrictions = new Restrictions(restrictions);

        childRestrictions.add(endVar);
        childRestrictions.add(joinVar);

        // the children deliver the nodes of the walk, not only the values the parent sees: a node of a class the
        // parent does not accept may still lead to one it does, so the children have to keep every node (the step
        // branches that cannot lead to an accepted class are left out below)
        childRestrictions.removeAcceptedClasses(endVar);
        childRestrictions.removeAcceptedClasses(joinVar);

        // the database evaluates no condition of a filter above the walk inside the recursion
        childRestrictions.removeFilterConditions();

        // the nodes the walk delivers are told apart by their classes, so the children must not present the nodes of
        // several classes as nodes of one class for the joins above
        if(delivered != null)
            childRestrictions.removeJoinClasses(endVar);

        // the recursion compares the node a step departs from with the nodes of the walk, so the step presents it as
        // they are held: a step whose branches bind the node in several classes of one class of the nodes is then
        // joined by a single equality instead of a disjunction over the pairs of the classes; these are the classes
        // of the nodes themselves, never those the parent accepts of the end
        childRestrictions.addJoinClasses(joinVar, endBinding.getClasses());

        if(graphVar != null)
            childRestrictions.add(graphVar);

        if(beginVar != null)
            childRestrictions.add(beginVar);

        // the values the walk starts from are read from the current solution as it is now, and the initial solutions
        // are optimised as a join with them would optimise them: only an initial solution holding the values starts a
        // path, so their table accesses take the values into their conditions (see Restrictions.getAcceptedValue)
        Map<Variable, SqlExpressionIntercode> optStart = optimizeStart(request, scope, evalServices);
        VariableBindings startBindings = getStartBindings(optStart);

        Restrictions initRestrictions = childRestrictions;
        Restrictions nextRestrictions = childRestrictions;

        if(!optStart.isEmpty())
        {
            initRestrictions = getJoinRestrictions(relations, init.getVariableBindings(), startBindings,
                    childRestrictions, true);
            optStart.forEach(initRestrictions::setAcceptedValue);

            // the steps of a walk that starts in a graph stay in that graph
            if(graphVar != null && optStart.containsKey(graphVar))
            {
                nextRestrictions = new Restrictions(childRestrictions);
                nextRestrictions.setAcceptedValue(graphVar, optStart.get(graphVar));
            }
        }

        SqlIntercode initOpt = init.optimize(request, scope, initRestrictions, reduced, evalServices);
        SqlIntercode nextOpt = next.optimize(request, scope, nextRestrictions, reduced, evalServices);


        if(initOpt.equals(SqlNoSolution.get()))
            return SqlNoSolution.get();

        // no path starts when no initial solution can be compatible with the values the walk starts from
        for(VariableBindingPair pair : VariableBindingPair.getPairs(relations, startBindings,
                initOpt.getVariableBindings()))
            if(!pair.isJoinable())
                return SqlNoSolution.get();

        if(beginVar != null && initOpt instanceof SqlUnion union)
        {
            List<SqlIntercode> segs = SqlDistinct.expandUnionByResourceClasses(request, union, Set.of(beginVar));

            if(segs.size() > 1)
            {
                List<SqlIntercode> childs = new ArrayList<>();

                // a segment keeps the initial solutions of the whole walk for turning, and the classes it restricts the
                // start to, which it may be turned only when the parent accepts alone (see canTurn)
                for(SqlIntercode child : segs)
                    childs.add(create(request, child, nextOpt, beginVar, joinVar, endBinding.getVariable(), graphVar,
                            restrictions, reverseNext, reverseJoinVar, reverseInit,
                            Set.copyOf(child.getVariable(beginVar).getClasses()), seeded, optStart, delivered));

                // the segments are walks, which care about the multiplicities as much as their parent does
                if(!childs.contains(null))
                    return SqlUnion.union(request, childs).optimize(request, scope, restrictions, reduced,
                            evalServices);
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

            nextOpt = SqlUnion.union(request, childs).optimize(request, scope, nextRestrictions, true, evalServices);
        }


        // a branch is left out when the walk cannot take it: its start is disjoint with every class reachable from
        // the initial nodes, or it delivers only nodes of classes that do not reach the result (the classes the parent
        // does not accept and those the walk does not deliver) from which no branch leads to a class that does (the
        // nodes it delivers are dropped, and so is everything reached from them)
        Set<ResourceClass> accepted = getAcceptedEndClasses(relations, restrictions);

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

            // the union of the remaining branches keeps presenting the node a step departs from as the recursion
            // compares it
            nextOpt = SqlUnion.union(request, childs, nextRestrictions);
        }

        if(accepted != null && !(nextOpt instanceof SqlUnion) && !overlaps(relations, nextOpt, endVar, accepted))
            nextOpt = SqlNoSolution.get();


        // a walk no step of which follows its initial solutions is those solutions, of which the ones ending in a
        // node the walk does not deliver are left out; when they cannot be, the walk keeps the step it has
        if(nextOpt.equals(SqlNoSolution.get()) || !(new VariableBindingPair(relations,
                initOpt.getVariableBindings().get(endVar), nextOpt.getVariableBindings().get(joinVar))).isJoinable())
        {
            SqlIntercode solutions = delivered == null ? initOpt :
                    restrict(request, scope, initOpt, Map.of(endVar, delivered));

            if(solutions != null)
                return SqlDistinct.create(request, solutions, solutions.getVariableBindings().getVariables());

            nextOpt = next;
        }


        if(initOpt == init && nextOpt == next && optStart == start && restrictions.isOptimized(relations, bindings))
            return this;

        SqlIntercode walk = create(request, initOpt, nextOpt, beginVar, joinVar, endBinding.getVariable(), graphVar,
                restrictions, reverseNext, reverseJoinVar, reverseInit, segmentClasses, seeded, optStart, delivered);

        // the walk that could not tell the nodes it delivers apart any more stays as it is
        return walk != null ? walk : this;
    }


    /**
     * The initial solutions a walk reduces to when the parent needs neither its end nor the multiplicities (see
     * {@link #optimize}): those of the walk; but those of a walk starting from the values of expressions are taken as
     * the walk was created with them (see {@link #reverseInit}), not restricted to the values, when the walk has kept
     * them (it may be turned and is no segment), and they are compared with the current solution by the pattern they
     * lie in, which the database then evaluates once for all the current solutions. They are taken in the classes of
     * the start and of the graph the walk starts in, on which the nodes above rely (see {@link #turn}); when they
     * cannot be, the walk reduces to its own initial solutions.
     *
     * @param request the current request
     * @param scope the scope of the walk
     * @return the initial solutions the walk reduces to
     */
    private SqlIntercode getStartSolutions(Request request, Scope scope)
    {
        if(start.isEmpty() || reverseInit == null || segmentClasses != null)
            return init;

        Map<Variable, Set<ResourceClass>> classes = new HashMap<>();

        for(Variable variable : Stream.of(beginVar, graphVar).filter(Objects::nonNull).toList())
            if(init.getVariable(variable) != null)
                classes.put(variable, init.getVariable(variable).getClasses());

        SqlIntercode created = restrict(request, scope, reverseInit, classes);

        return created != null ? created : init;
    }


    /**
     * Classes of the end variable whose nodes may reach the result: the classes the walk delivers its end in (see
     * {@link #delivered}) that overlap a class the parent accepts.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param restrictions what the parent needs of the variables
     * @return classes of the end variable whose nodes may reach the result, or null when the nodes of every class may
     */
    private Set<ResourceClass> getAcceptedEndClasses(ClassRelations relations, Restrictions restrictions)
    {
        Set<ResourceClass> accepted = restrictions.getAcceptedClasses(endBinding.getVariable());

        if(delivered == null || accepted == null)
            return delivered == null ? accepted : delivered;

        return delivered.stream().filter(c -> !areDisjunct(relations, Set.of(c), accepted)).collect(toSet());
    }


    /**
     * The values the walk starts from as the current solution provides them now (see {@link #start}); a value the
     * current solution does not bind is left out, so the walk starts from every node then.
     *
     * @param request the current request
     * @param scope the scope of the walk
     * @param evalServices whether SERVICE stubs are evaluated
     * @return the values the walk starts from, by the variables of the initial solutions; the map of this walk when
     *         nothing has changed
     */
    private Map<Variable, SqlExpressionIntercode> optimizeStart(Request request, Scope scope, boolean evalServices)
    {
        Map<Variable, SqlExpressionIntercode> result = new LinkedHashMap<>();

        for(Entry<Variable, SqlExpressionIntercode> entry : start.entrySet())
        {
            SqlExpressionIntercode value = entry.getValue().optimize(request, scope, new VariableBindings(),
                    Restriction.ALL, evalServices);

            if(!value.equals(SqlNull.get()))
                result.put(entry.getKey(), value);
        }

        return result.equals(start) ? start : result;
    }


    /**
     * The values a walk starts from as bindings of the variables of the initial solutions, which the initial solutions
     * are joined with.
     *
     * @param start values the walk starts from, by the variables of the initial solutions
     * @return bindings of the variables to the values
     */
    private static VariableBindings getStartBindings(Map<Variable, SqlExpressionIntercode> start)
    {
        VariableBindings bindings = new VariableBindings();

        for(Entry<Variable, SqlExpressionIntercode> entry : start.entrySet())
            bindings.add(
                    new VariableBinding(entry.getKey(), entry.getValue().getMappings(), entry.getValue().canBeNull()));

        return bindings;
    }


    /**
     * True if the branch of the step is a table access whose rows with a given start node can be looked up by an index
     * of the table, the columns of the carried graph and the columns compared with constants counting as bound (see
     * {@link DatabaseSchema#canLookUp}).
     *
     * @param request the current request
     * @param branch a branch of the step
     * @return true if the rows of the branch with a given start node can be looked up by an index
     */
    private boolean canLookUpStep(Request request, SqlIntercode branch)
    {
        DatabaseSchema schema = request.getConfiguration().getDatabaseSchema();

        if(schema == null || !(branch instanceof SqlTableAccess access) || access.getTable() == null)
            return false;

        Set<Column> bound = new HashSet<>();

        for(ColumnComparison comparison : access.getConditions().getAreEqual())
        {
            if(comparison.getLeft() instanceof TableColumn column && !(comparison.getRight() instanceof TableColumn))
                bound.add(column);
            else if(comparison.getRight() instanceof TableColumn column
                    && !(comparison.getLeft() instanceof TableColumn))
                bound.add(column);
        }

        VariableBinding graph = graphVar != null ? access.getInternalVariableBinding(graphVar) : null;

        if(graph != null)
            for(Column column : graph.getNonConstantColumns())
                if(column instanceof TableColumn)
                    bound.add(column);

        VariableBinding start = access.getInternalVariableBinding(joinVar);
        Set<Column> columns = start != null ? start.getNonConstantColumns() : Set.of();

        return columns.size() == 1 && columns.iterator().next() instanceof TableColumn column
                && schema.canLookUp(access.getTable(), column, bound);
    }


    @Override
    public SqlCode translate(Request request, Scope scope)
    {
        ClassRelations relations = request.getConfiguration();

        List<ResourceClass> endVarClasses = new LinkedList<>(endBinding.getClasses()); // to stable order

        SqlBuilder builder = new SqlBuilder();

        builder.append("WITH RECURSIVE recursion(");

        builder.append(ev.stream().collect(SqlCode.joining(", ")));

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

        builder.append(iv.stream().collect(SqlCode.joining(", ")));

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
        builder.append(init.translate(request, scope));
        builder.append(") AS ");
        builder.append(initTable);

        // a walk starting from the values of expressions takes only the initial solutions compatible with them; the
        // values are columns of the current solution of the enclosing EXISTS pattern, addressed already, and they are
        // compared here unless the table accesses of the initial solutions have taken them into their conditions
        Map<Variable, SqlExpressionIntercode> compared = new LinkedHashMap<>(start);
        compared.entrySet().removeIf(e -> init.isRestricted(e.getKey(), e.getValue()));

        SqlCode startCondition = generateJoinCondition(relations, getStartBindings(compared),
                init.getVariableBindings(), null, initTable);

        if(startCondition != null)
        {
            builder.append(" WHERE ");
            builder.append(startCondition);
        }

        builder.append(" UNION SELECT ");

        // a step that is a union whose branches deliver the node in several disjoint classes is joined laterally,
        // branch by branch with a condition of its own: the condition of the union as a whole is a disjunction over the
        // class pairs, which the database can neither hash nor look up by an index, whereas it joins a single pair
        // through the index of the branch (the step of a single class stays a plain join, so that the database may
        // still choose a hash join when the recursion grows large, and so does a union stepping from several classes
        // when the walk holds its nodes in a single class: the union presents the node it departs from in that class,
        // see optimize, so one equality joins it); a step of a walk carrying the graph in several classes keeps the
        // equality of the node, which the database can hash, and only its condition on the graph is a disjunction over
        // the class pairs, so it is joined laterally only when every branch can be looked up by an index (otherwise
        // the database would scan the branch for every row of the recursion)
        VariableBinding carriedGraph = graphVar != null ? carried.get(graphVar) : null;
        boolean lateral = next instanceof SqlUnion union
                && (endBinding.getClasses().size() > 1 || (carriedGraph != null && carriedGraph.getClasses().size() > 1
                        && union.getChilds().stream().allMatch(b -> canLookUpStep(request, b))));
        AliasTable left = lateral ? walkTable : leftTable;
        AliasTable right = lateral ? stepTable : rightTable;

        builder.append(ev.stream().map(c -> c.fromTable(left)).collect(SqlCode.joining(", ")));

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

        // the step is joined with the carried variables as the recursion holds them, so that a path stays in its graph
        // also when the parent needs the graph variable in fewer classes or not at all
        VariableBindings tmp = new VariableBindings(carried);
        tmp.add(new VariableBinding(joinVar, endBinding.getMappings(), endBinding.canBeNull()));

        if(lateral)
        {
            builder.append(" CROSS JOIN LATERAL (");
            builder.append(((SqlUnion) next).translate(request, scope, (branch,
                    alias) -> generateJoinCondition(relations, tmp, branch.getVariableBindings(), left, alias)));
            builder.append(") AS ");
            builder.append(right);
        }
        else if(request.getConfiguration().isJoinOptimizerEnabled() && next instanceof SqlTableAccess access
                && access.getTable() != null)
        {
            // with the join optimizer on, the planner is told not to nest loops, so a step reading a table is looked
            // up laterally through an index of the table for every row of the recursion instead of being hashed whole
            // in every round (see JoinOptimizer)
            SqlCode condition = generateJoinCondition(relations, tmp, access.getInternalVariableBindings(), left, null);

            builder.append(" CROSS JOIN LATERAL (");
            builder.append(access.translateLateral(request, condition != null ? condition : SqlCode.TRUE));
            builder.append(") AS ");
            builder.append(right);
        }
        else
        {
            builder.append(", (");
            builder.append(next.translate(request, scope));
            builder.append(") AS ");
            builder.append(right);

            SqlCode condition = generateJoinCondition(relations, tmp, next.getVariableBindings(), left, right);

            if(condition != null)
            {
                builder.append(" WHERE ");
                builder.append(condition);
            }
        }

        builder.append(") SELECT ");

        Set<Column> columns = getVariableBindings().getNonConstantColumns();

        if(!columns.isEmpty())
            builder.append(columns.stream().collect(SqlCode.joining(", ")));
        else
            builder.append("1");

        builder.append(" FROM recursion");

        // the solutions ending in a node of a class the walk does not deliver are left out
        if(delivered != null)
        {
            builder.append(" WHERE ");
            builder.append(getDeliveredBinding(endBinding, delivered).getIsNotNull());
        }

        return builder.toCode();
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

        return carried.getValues().stream().allMatch(b -> selected.contains(b.getVariable()) || b.isConstant());
    }


    @Override
    public boolean hasServiceSubpattern()
    {
        return init.hasServiceSubpattern() || next.hasServiceSubpattern()
                || start.values().stream().anyMatch(SqlExpressionIntercode::hasServiceSubpattern);
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

        for(Entry<Variable, SqlExpressionIntercode> entry : start.entrySet())
        {
            indentInfo(builder, indent, true);
            builder.append("from ");
            builder.append(entry.getKey());
            builder.append(" = ");
            entry.getValue().generateExplanation(builder, getIndent(indent, false) + "  ");
        }

        if(delivered != null)
        {
            indentInfo(builder, indent, true);
            builder.append("to ");
            builder.append(endBinding.getVariable());
            builder.append(" as ");
            builder.append(String.join(", ", delivered.stream().map(ResourceClass::toString).sorted().toList()));
        }

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

        if(!Objects.equals(reverseInit, imcode.reverseInit))
            return false;

        if(!Objects.equals(segmentClasses, imcode.segmentClasses))
            return false;

        if(!Objects.equals(start, imcode.start))
            return false;

        if(!Objects.equals(delivered, imcode.delivered))
            return false;

        return true;
    }


    @Override
    protected int getHashCode()
    {
        return Objects.hash(graphVar, beginVar, joinVar, endBinding, init, next, reverseNext, reverseJoinVar,
                reverseInit, segmentClasses, start, delivered);
    }
}
