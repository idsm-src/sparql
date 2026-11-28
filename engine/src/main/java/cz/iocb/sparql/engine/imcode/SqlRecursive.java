package cz.iocb.sparql.engine.imcode;

import static cz.iocb.sparql.engine.database.Table.toTableColumns;
import static cz.iocb.sparql.engine.imcode.expression.SqlLiteral.falseValue;
import static cz.iocb.sparql.engine.imcode.expression.SqlLiteral.trueValue;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdBoolean;
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
import cz.iocb.sparql.engine.config.EngineSettings;
import cz.iocb.sparql.engine.database.AliasTable;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.Condition.ColumnComparison;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.LateralTable;
import cz.iocb.sparql.engine.database.SqlBuilder;
import cz.iocb.sparql.engine.database.SqlCode;
import cz.iocb.sparql.engine.database.SqlType;
import cz.iocb.sparql.engine.database.Table;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.database.ValueColumn;
import cz.iocb.sparql.engine.imcode.expression.SqlBuiltinCall;
import cz.iocb.sparql.engine.imcode.expression.SqlExpressionIntercode;
import cz.iocb.sparql.engine.imcode.expression.SqlExpressionIntercode.Restriction;
import cz.iocb.sparql.engine.imcode.expression.SqlNull;
import cz.iocb.sparql.engine.imcode.expression.SqlUnaryLogical;
import cz.iocb.sparql.engine.imcode.expression.SqlVariable;
import cz.iocb.sparql.engine.mapping.classes.ClassRelations;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.RdfTerm;
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
 * variables in, so whatever becomes of the walk keeps them (see {@link #turn}, {@link #delivered}). A sibling that
 * binds the start always without being distinct over it cannot seed the walk, but the walk then starts only from the
 * nodes the sibling binds the start to (see {@link #startTest}).
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
     * Alias of the distinct starts of a walk evaluated for every start by itself (see {@link #isWalkedPerStart}).
     */
    private static final AliasTable startsTable = new AliasTable("starts");

    /**
     * Alias of the recursion of a walk evaluated for every start by itself, joined laterally with the start.
     */
    private static final AliasTable pathsTable = new AliasTable("paths");

    /**
     * Alias of the sibling the starts of a walk are tested against (see {@link #startTest}).
     */
    private static final AliasTable siblingTable = new AliasTable("sibling");

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
     * The test of the start against a sibling of the walk in a join that binds the start variable always without being
     * distinct over it, so that it cannot seed the walk (see {@link #seed}): the walk starts only from the nodes the
     * sibling binds the variable to, of whose paths alone the join keeps any, and the sibling keeps its place in the
     * join (see {@link #testStart}). The recursion of a walk evaluated for every start by itself is then joined only
     * with the starts the sibling binds, so the walk keeps being evaluated so (see {@link #isWalkedPerStart}), and the
     * recursion of a walk evaluated as a whole takes only the initial solutions starting in such a node (see
     * {@link #translateStartTest}). The test holds only for the solutions of the walk that the join compares with the
     * sibling, so it is given up wherever the walk stops starting from the variable: when the walk is turned or walked
     * without its start (see {@link #optimize}). Null when the start is not tested.
     */
    private final StartTest startTest;


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
     * The test of the start of a walk against a sibling (see {@link #startTest}).
     *
     * @param sibling the solutions of the sibling, which bind the start variable always, optimised for it alone
     * @param table alias of the solutions the test is evaluated for, fixed when the test is made and kept by every
     *            rebuilt walk: the sibling evaluated for every solution refers to the solution through it, across the
     *            boundaries of nested subqueries, so it has to be unique (see {@link Request#createLateralTable})
     */
    private record StartTest(SqlIntercode sibling, LateralTable table)
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
     * @param startTest the test of the start against a sibling, or null
     */
    protected SqlRecursive(VariableBindings bindings, SqlIntercode init, SqlIntercode next, VariableBinding endBinding,
            Variable joinVar, Variable beginVar, List<Column> iv, List<Column> ev, Variable graphVar,
            SqlIntercode reverseNext, Variable reverseJoinVar, SqlIntercode reverseInit,
            Set<ResourceClass> segmentClasses, VariableBindings carried, boolean seeded,
            Map<Variable, SqlExpressionIntercode> start, Set<ResourceClass> delivered, StartTest startTest)
    {
        super(bindings,
                init.isDeterministic() && next.isDeterministic()
                        && start.values().stream().allMatch(SqlExpressionIntercode::isDeterministic)
                        && (startTest == null || startTest.sibling().isDeterministic()));
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
        this.startTest = startTest;
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
                Map.of(), null, null);
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
                reverseNext != null ? init : null, null, false, start, null, null);
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
     * @param startTest the test of the start against a sibling (see {@link #startTest}), or null; a walk without a
     *            start variable takes none
     * @return recursion exposing only what the parent needs; the carried variables get fresh output columns; no
     *         solution when the initial solutions do not bind the end variable, or when no node is delivered; null when
     *         the nodes take a class that overlaps a class to deliver without being its subclass, as the nodes of that
     *         class to deliver cannot be told apart
     */
    protected static SqlIntercode create(Request request, SqlIntercode init, SqlIntercode next, Variable beginVar,
            Variable joinVar, Variable endVar, Variable graphVar, Restrictions restrictions, SqlIntercode reverseNext,
            Variable reverseJoinVar, SqlIntercode reverseInit, Set<ResourceClass> segmentClasses, boolean seeded,
            Map<Variable, SqlExpressionIntercode> start, Set<ResourceClass> delivered, StartTest startTest)
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
                seeded, start, endClasses, beginVar != null ? startTest : null);
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
     * the pattern the walk lies in comparing its solutions with the current solution. The test of the start against a
     * sibling (see {@link #startTest}) is kept, unless the walk is turned, as its start then becomes the end.
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
                    null, null, null, null, true, Map.of(), turned.delivered(), null);

        return create(request, seeded, next, startVar, joinVar, stopVar, graphVar, seedRestrictions, null, null, null,
                null, true, Map.of(), delivered, startTest);
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
     * True if the start of the walk may be tested against a sibling (see {@link #startTest}): the walk lies between two
     * variables, its start is not tested yet, and it does not start from the values of expressions, which give a single
     * node.
     *
     * @return true if the start of the walk may be tested against a sibling, false otherwise
     */
    protected boolean canTestStart()
    {
        return beginVar != null && startTest == null && start.isEmpty();
    }


    /**
     * True if the walk may be turned to start from its end, tested against a sibling (see {@link #testStart}): its
     * start may be tested, the walk may be turned (see {@link #canTurn}), and the parent restricts its start neither to
     * a constant term nor to the terms it accepts, which restrict the start more than a sibling does, the walk being
     * turned back by them otherwise (see {@link #startsBetterFromEnd}).
     *
     * @param restrictions what the parent and the siblings of the walk need of its variables
     * @return true if the walk may be turned to start from its end tested against a sibling, false otherwise
     */
    protected boolean canTestEnd(Restrictions restrictions)
    {
        return canTestStart() && canTurn(restrictions) && !isConstant(restrictions.getAcceptedValue(beginVar))
                && restrictions.getAcceptedTerms(beginVar) == null;
    }


    /**
     * The walk whose start is tested against a sibling (see {@link #startTest}), or, when the sibling binds the end
     * variable, the walk turned to start from it (see {@link #turn(Request, Scope)}) and tested so; the turned walk is
     * not turned any more, which would give the test up. The sibling is optimised for the start alone (see
     * {@link #optimizeStartTest}).
     *
     * @param request the current request
     * @param scope the scope of the walk
     * @param sibling the sibling, which binds the variable always
     * @param fromEnd true when the sibling binds the end variable, so that the walk is turned; only when the walk may
     *            be turned so (see {@link #canTestEnd})
     * @param restrictions what the parent and the siblings of the walk need of its variables
     * @return the walk whose start is tested against the sibling; null when the walk cannot be turned, when the test
     *         could not be hashed (see {@link #optimizeStartTest}), or when the walk could not tell the nodes it
     *         delivers apart any more (see {@link #create})
     */
    protected SqlIntercode testStart(Request request, Scope scope, SqlIntercode sibling, boolean fromEnd,
            Restrictions restrictions)
    {
        Variable endVar = endBinding.getVariable();
        Turned turned = fromEnd ? turn(request, scope) : null;

        if(fromEnd && turned == null)
            return null;

        StartTest test = optimizeStartTest(request, scope, fromEnd ? turned.init() : init, fromEnd ? endVar : beginVar,
                new StartTest(sibling, request.createLateralTable()), false);

        if(test == null)
            return null;

        if(!fromEnd)
            return create(request, init, next, beginVar, joinVar, endVar, graphVar, restrictions, reverseNext,
                    reverseJoinVar, reverseInit, segmentClasses, seeded, start, delivered, test);

        return create(request, turned.init(), reverseNext, endVar, reverseJoinVar, beginVar, graphVar, restrictions,
                null, null, null, null, seeded, Map.of(), turned.delivered(), test);
    }


    /**
     * The test of the start against the sibling optimised for the initial solutions (see {@link #startTest}): the
     * sibling optimised for the start variable alone, the duplicates not mattering to the test, and presenting the
     * variable in the classes the initial solutions hold it in (see {@link Restrictions#getJoinClasses}), so that the
     * test compares a single pair of classes, which the database can hash or look up; a union of the classes of a walk
     * is split into segments of a class each, each of which optimises the test for its own class (see
     * {@link #optimize}). Null when the test still compares several classes of either side, or the sibling may leave
     * the variable unbound: the database could only compare the starts with the sibling pairwise.
     *
     * @param request the current request
     * @param scope the scope of the walk
     * @param solutions the initial solutions the walk starts from
     * @param variable the start variable
     * @param test the test of the start
     * @param evalServices whether SERVICE stubs are evaluated
     * @return the test optimised for the initial solutions; the test itself when nothing has changed; null when the
     *         test could not be hashed
     */
    private static StartTest optimizeStartTest(Request request, Scope scope, SqlIntercode solutions, Variable variable,
            StartTest test, boolean evalServices)
    {
        VariableBindings start = new VariableBindings();
        VariableBinding binding = solutions.getVariable(variable);

        if(binding == null)
            return null;

        start.add(binding);

        Restrictions restrictions = new Restrictions(Set.of(variable));
        restrictions.addJoinClasses(variable, binding.getClasses());

        SqlIntercode sibling = test.sibling().optimize(request, scope, restrictions, true, evalServices);

        if(!JoinOptimizer.isHashable(start, sibling.getVariableBindings()))
            return null;

        return sibling.equals(test.sibling()) ? test : new StartTest(sibling, test.table());
    }


    /**
     * Estimated cost the test of the start against the sibling spares the walk (see {@link #startTest}), less the cost
     * of the test itself; positive when the test is expected to pay. Every start the sibling does not bind spares at
     * least a lookup of its initial solutions and a lookup of the step from each of them, the starts the sibling binds
     * being taken to lie among those of the walk, whereas the test costs a probe of the sibling for every solution it
     * is evaluated for, or a pass over the sibling (see {@link #getStartTestCost}). A path mostly leads far beyond its
     * first step, so the walk is mostly spared much more.
     *
     * @param request the current request
     * @return estimated cost the test spares the walk, less the cost of the test
     */
    protected double getStartTestGain(Request request)
    {
        double starts = Math.max(JoinOptimizer.estimateDistinct(request, init, beginVar), 1);
        double passed = Math.min(JoinOptimizer.estimateDistinct(request, startTest.sibling(), beginVar), starts);
        double spared = (1 - passed / starts) * (starts + JoinOptimizer.estimateRows(request, init))
                * request.getEngineSettings().lookupCost();

        return spared - getStartTestCost(request, getTestedSolutions(request));
    }


    /**
     * Estimated number of the solutions the start is tested for (see {@link #startTest}): the distinct starts of a walk
     * evaluated for every start by itself (see {@link #isWalkedPerStart}), the initial solutions of a walk evaluated as
     * a whole.
     *
     * @param request the current request
     * @return estimated number of the solutions the start is tested for
     */
    private double getTestedSolutions(Request request)
    {
        return isWalkedPerStart(request) ? JoinOptimizer.estimateDistinct(request, init, beginVar) :
                JoinOptimizer.estimateRows(request, init);
    }


    /**
     * Estimated cost of the test of the start against the sibling for the given number of solutions (see
     * {@link #startTest}): a probe of the sibling for every solution, which an index of every table the sibling reads
     * serves and which stops at the first solution of the sibling found (see {@link #isProbed}), or a pass over the
     * sibling whose distinct solutions the database hashes, whichever costs less.
     *
     * @param request the current request
     * @param solutions estimated number of the solutions the start is tested for
     * @return estimated cost of the test
     */
    private double getStartTestCost(Request request, double solutions)
    {
        double hashed = getStartTestHashCost(request, solutions);

        return canProbeSibling(request) ? Math.min(hashed, solutions * request.getEngineSettings().lookupCost()) :
                hashed;
    }


    /**
     * Estimated cost of the test of the start against the sibling hashed (see {@link #startTest}): a pass over the
     * sibling, the hashing of its distinct solutions, and a probe of the hash table for every solution tested.
     *
     * @param request the current request
     * @param solutions estimated number of the solutions the start is tested for
     * @return estimated cost of the hashed test
     */
    private double getStartTestHashCost(Request request, double solutions)
    {
        SqlIntercode sibling = startTest.sibling();

        return JoinOptimizer.estimateRows(request, sibling)
                + request.getEngineSettings().hashCost() * JoinOptimizer.estimateDistinct(request, sibling, beginVar)
                + solutions;
    }


    /**
     * True if the sibling the start is tested against (see {@link #startTest}) can be probed for a start: evaluated for
     * the start, it is restricted to the solutions compatible with it (see {@link JoinOptimizer#estimateForRow}), and
     * every table it reads can be looked up by an index for the value of the start (see {@link #canLookUpEvery}), so
     * that the database finds the first solution of a probe at once.
     *
     * @param request the current request
     * @return true if the sibling can be probed for a start, false otherwise
     */
    private boolean canProbeSibling(Request request)
    {
        JoinOptimizer.Outer outer = getStartTestRows();

        return !outer.bindings().getVariables().isEmpty()
                && canLookUpEvery(request, startTest.sibling(), beginVar, null)
                && JoinOptimizer.estimateForRow(request, startTest.sibling(), outer).restricted();
    }


    /**
     * True if the sibling the start is tested against (see {@link #startTest}) is probed for every solution tested,
     * which costs less than hashing it (see {@link #getStartTestCost}).
     *
     * @param request the current request
     * @param solutions estimated number of the solutions the start is tested for
     * @return true if the sibling is probed for every solution, false when it is hashed
     */
    private boolean isProbed(Request request, double solutions)
    {
        return canProbeSibling(request)
                && solutions * request.getEngineSettings().lookupCost() < getStartTestHashCost(request, solutions);
    }


    /**
     * The solutions the start is tested for (see {@link #startTest}), as the sibling evaluated for every one of them
     * sees them: their binding of the start variable, addressed through the alias of the test.
     *
     * @return the solutions the start is tested for
     */
    private JoinOptimizer.Outer getStartTestRows()
    {
        return JoinOptimizer.Outer.of(getStartBinding(), startTest.sibling().getVariableBindings(), startTest.table());
    }


    /**
     * The binding of the start variable in the initial solutions, which the solutions the start is tested for (see
     * {@link #startTest}) hold.
     *
     * @return the binding of the start variable in the initial solutions, empty when they do not bind it
     */
    private VariableBindings getStartBinding()
    {
        VariableBindings bindings = new VariableBindings();
        VariableBinding binding = init.getVariable(beginVar);

        if(binding != null)
            bindings.add(binding);

        return bindings;
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
     * True if the node is a walk between two variables that is not seeded yet, or a join holding one: such a walk may
     * still be seeded from an end with the values of the other side of an OPTIONAL (see {@link #seedVariable}), which
     * looks for it only there, so the node is not put under another node before that.
     *
     * @param node the node
     * @return true if the node is a walk that may still be seeded or a join holding one, false otherwise
     */
    protected static boolean holdsSeedableWalk(SqlIntercode node)
    {
        if(node instanceof SqlJoin join)
            return join.getChilds().stream().anyMatch(SqlRecursive::holdsSeedableWalk);

        return node instanceof SqlRecursive walk && walk.beginVar != null && !walk.seeded;
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
        // a walk starting from the values of expressions or from the starts a sibling binds has no path from the other
        // nodes, and a walk that does not deliver every node leaves out the zero-length paths of the others
        if(beginVar == null || !start.isEmpty() || startTest != null || delivered != null)
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
     * filter requiring one does (see {@link SqlIntercode#addAcceptedValues}), and none for the start; or it accepts
     * only some terms for the end, with which the turned walk is seeded (see {@link #seedWithAcceptedTerms}), and any
     * term for the start; or it accepts the end in narrower classes than the start (every accepted class of the end is
     * a subclass of an accepted class of the start, but not the other way round), or restricts the end alone. Walked
     * from the end, the recursion enumerates only the pairs ending in nodes the parent accepts, instead of every pair
     * starting anywhere, of which the parent keeps the same ones. The parent does not restrict the end alone when the
     * initial solutions have been restricted to some classes of the start already (see {@link #startsInEveryClass}):
     * having seen the bindings of the walk, it accepts the start in any class, because nothing is left for it to drop.
     * A walk whose start is tested against a sibling (see {@link #startTest}) starts from fewer nodes than the classes
     * of the end tell, so only a constant term or the terms accepted of the end turn it.
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

        // the terms accepted of an end restrict it more than its classes do
        if(restrictions.getAcceptedTerms(beginVar) != null)
            return false;

        if(restrictions.getAcceptedTerms(endBinding.getVariable()) != null)
            return true;

        if(startTest != null)
            return false;

        Set<ResourceClass> end = restrictions.getAcceptedClasses(endBinding.getVariable());
        Set<ResourceClass> begin = restrictions.getAcceptedClasses(beginVar);

        if(end == null)
            return false;

        if(begin == null)
            return startsInEveryClass();

        return isNarrower(end, begin) && !isNarrower(begin, end);
    }


    /**
     * The walk seeded with the terms the parent accepts of its start variable (see
     * {@link Restrictions#getAcceptedTerms}), as inline data binding the variable to those terms would seed it when
     * joined with the walk (see {@link #seed}): the recursion then enumerates only the paths from those terms, all of
     * which the filter accepting the terms keeps, instead of the paths from every node, of which it keeps the same
     * ones. A walk the parent accepts only some terms at the end of is turned first (see {@link #startsBetterFromEnd}).
     * A term the parent requires of the start (see {@link Restrictions#getAcceptedValue}) restricts the walk more, the
     * initial solutions taking it into their conditions, so the walk is not seeded then.
     *
     * @param request the current request
     * @param scope the scope of the walk
     * @param restrictions what the parent needs of the variables
     * @return the seeded walk; no solution when none of the accepted terms is of a class the walk binds its start in;
     *         null when the parent accepts any term of the start, or the walk cannot be seeded
     */
    private SqlIntercode seedWithAcceptedTerms(Request request, Scope scope, Restrictions restrictions)
    {
        VariableBinding binding = init.getVariable(beginVar);
        Set<RdfTerm> terms = restrictions.getAcceptedTerms(beginVar);

        if(terms == null || binding == null || isConstant(restrictions.getAcceptedValue(beginVar)))
            return null;

        SqlIntercode seed = getTermSeed(request, scope, binding, terms);

        if(seed.equals(SqlNoSolution.get()))
            return seed;

        return seed(request, scope, List.of(seed), false, restrictions);
    }


    /**
     * Inline data binding a variable to the given terms, to seed a walk with (see {@link #seedWithAcceptedTerms}): one
     * row per term, the terms that are stored alike, like the lexical forms of one number, making one row, as a seed
     * has to be distinct over the variable; the terms of the classes the walk never binds the variable in are left out.
     *
     * @param request the current request
     * @param scope the scope of the walk
     * @param binding the binding of the variable in the walk
     * @param terms the terms
     * @return inline data binding the variable to the terms, or no solution when the walk binds the variable in the
     *         class of none of them
     */
    private static SqlIntercode getTermSeed(Request request, Scope scope, VariableBinding binding, Set<RdfTerm> terms)
    {
        Map<List<Object>, List<RdfTerm>> lines = new LinkedHashMap<>();

        for(RdfTerm term : terms)
        {
            ResourceClass resClass = request.getResourceClass(term);

            lines.putIfAbsent(List.of(resClass, request.getColumns(resClass, term)), List.of(term));
        }

        Restrictions restrictions = new Restrictions(List.of(binding.getVariable()));
        restrictions.addAcceptedClasses(binding.getVariable(), binding.getClasses());

        return SqlValues.create(request, List.of(binding.getVariable()), new ArrayList<>(lines.values()))
                .optimize(request, scope, restrictions, false, false);
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
     * values of expressions and its start is tested against no sibling, and its initial solutions have lost no class of
     * the start (see {@link #startsInEveryClass}). Only then a node ends a path of the walk iff it ends an initial
     * solution.
     *
     * @return true if the walk starts from all the initial solutions it was created with, false otherwise
     */
    private boolean startsAnywhere()
    {
        return reverseNext != null && segmentClasses == null && start.isEmpty() && startTest == null
                && startsInEveryClass();
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
            // the recursion then enumerates the nodes the walk reaches, not the pairs of a start and a node; a test of
            // the start against a sibling, which the parent then does not compare with the walk, is given up
            if(beginVar != null && start.isEmpty() && !restrictions.containsVar(beginVar))
            {
                SqlIntercode walk = create(request, init, next, null, joinVar, endVar, graphVar, restrictions, null,
                        null, null, null, seeded, Map.of(), delivered, null);

                if(walk != null)
                    return walk.optimize(request, scope, restrictions, true, evalServices);
            }
        }

        ClassRelations relations = request.getConfiguration();

        // a walk between two variables starts from the end the parent restricts more: the same step walked the other
        // way round, with the end variable for the start and the start variable for the end, from the initial solutions
        // the walk was created with, as the restrictions of the start no longer apply to them (see turn), nor does a
        // test of the start against a sibling; a walk starting from the values of expressions starts from a single node
        // already
        if(start.isEmpty() && canTurn(restrictions) && startsBetterFromEnd(restrictions))
        {
            Turned turned = turn(request, scope);

            SqlIntercode walk = turned == null ? null :
                    create(request, turned.init(), reverseNext, endVar, reverseJoinVar, beginVar, graphVar,
                            restrictions, next, joinVar, reverseInit, null, seeded, Map.of(), turned.delivered(), null);

            if(walk != null)
                return walk.optimize(request, scope, restrictions, reduced, evalServices);
        }

        // a walk between two variables of whose start the parent accepts only some terms starts from those terms,
        // seeded with them as inline data joined with the walk would seed it; a walk starting from the values of
        // expressions starts from a single node already
        if(beginVar != null && !seeded && start.isEmpty())
        {
            SqlIntercode walk = seedWithAcceptedTerms(request, scope, restrictions);

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
                // start to, which it may be turned only when the parent accepts alone (see canTurn), and the test of
                // the start, which it optimises for its own class (see optimizeStartTest)
                for(SqlIntercode child : segs)
                    childs.add(create(request, child, nextOpt, beginVar, joinVar, endBinding.getVariable(), graphVar,
                            restrictions, reverseNext, reverseJoinVar, reverseInit,
                            Set.copyOf(child.getVariable(beginVar).getClasses()), seeded, optStart, delivered,
                            startTest == null ? null :
                                    optimizeStartTest(request, scope, child, beginVar, startTest, evalServices)));

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


        // the sibling the start is tested against is optimised again for the initial solutions as they are now
        StartTest optTest = startTest == null ? null :
                optimizeStartTest(request, scope, initOpt, beginVar, startTest, evalServices);

        if(initOpt == init && nextOpt == next && optStart == start && optTest == startTest
                && restrictions.isOptimized(relations, bindings))
            return this;

        SqlIntercode walk = create(request, initOpt, nextOpt, beginVar, joinVar, endBinding.getVariable(), graphVar,
                restrictions, reverseNext, reverseJoinVar, reverseInit, segmentClasses, seeded, optStart, delivered,
                optTest);

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
     * True if the node is a table access, or a union of table accesses, whose rows with a given value of the variable
     * can be looked up by an index of each table (see {@link #canLookUp}).
     *
     * @param request the current request
     * @param node the initial solutions, the step, or the sibling the start is tested against
     * @param variable the variable whose value the rows are looked up by
     * @param graphVar the graph variable the walk carries, whose columns count as bound, or null
     * @return true if the rows of the node with a given value of the variable can be looked up by an index
     */
    private static boolean canLookUpEvery(Request request, SqlIntercode node, Variable variable, Variable graphVar)
    {
        if(node instanceof SqlUnion union)
            return union.getChilds().stream().allMatch(branch -> canLookUp(request, branch, variable, graphVar));

        return canLookUp(request, node, variable, graphVar);
    }


    /**
     * True if the branch is a table access whose rows with a given value of the variable can be looked up by an index
     * of the table, the columns of the carried graph and the columns compared with constants counting as bound (see
     * {@link DatabaseSchema#canLookUp}).
     *
     * @param request the current request
     * @param branch a branch of the initial solutions, of the step, or of the sibling the start is tested against
     * @param variable the variable whose value the rows are looked up by: the start variable of the initial solutions
     *            and of the sibling, or the variable of the step bound to the node it departs from
     * @param graphVar the graph variable the walk carries, whose columns count as bound, or null
     * @return true if the rows of the branch with a given value of the variable can be looked up by an index
     */
    private static boolean canLookUp(Request request, SqlIntercode branch, Variable variable, Variable graphVar)
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

        VariableBinding start = access.getInternalVariableBinding(variable);
        Set<Column> columns = start != null ? start.getNonConstantColumns() : Set.of();

        return columns.size() == 1 && columns.iterator().next() instanceof TableColumn column
                && schema.canLookUp(access.getTable(), column, bound);
    }


    /**
     * True if the walk is evaluated for every start by itself (see {@link #translate}): the recursion deduplicates the
     * pairs of a start and a node it has reached, so the database keeps every pair the walk has found in a hash table
     * that no setting of the memory limits it, and a walk between two variables from all the initial solutions keeps
     * the whole closure (the pairs within a component of 31745 nodes related to each other took about 70 GB), whereas
     * evaluated for one start, which the pairs of the other starts can never be equal to, it keeps only the nodes that
     * start reaches. A walk is evaluated so when it starts from all its initial solutions (it is not seeded and starts
     * from no values of expressions, either of which makes its starts few) and carries its start, and its graph, bound
     * always in a single class, so that the starts are told apart by the plain comparison of their columns, and when
     * both its initial solutions and its step can be looked up by an index (see {@link #canLookUpEvery}): the initial
     * solutions are then read for one start at a time, and every step for the few nodes of one start, instead of being
     * read whole for every start. Evaluated so, the walk of the nextProt terms related to each other took 15.6 minutes
     * and 48 MB instead of an estimated 20 minutes and 70 to 90 GB; a walk of many starts each of which reaches a few
     * nodes is evaluated about a tenth slower.
     *
     * @param request the current request
     * @return true if the walk is evaluated for every start by itself, false otherwise
     */
    private boolean isWalkedPerStart(Request request)
    {
        if(beginVar == null || seeded || !start.isEmpty() || iv.isEmpty())
            return false;

        for(Variable variable : carried.getVariables())
        {
            VariableBinding binding = init.getVariable(variable);

            if(binding == null || binding.canBeNull() || binding.getClasses().size() != 1
                    || binding.getMappings().values().contains(null))
                return false;
        }

        return canLookUpEvery(request, init, beginVar, graphVar) && canLookUpEvery(request, next, joinVar, graphVar);
    }


    /**
     * SQL condition that a row of the initial solutions starts in the start of the walk evaluated for every start by
     * itself (see {@link #isWalkedPerStart}): the columns of the carried variables in the initial solutions are the
     * same terms as the columns of the start, by the identity of their types, which the database looks the initial
     * solutions up by.
     *
     * @return SQL condition over the columns of the initial solutions and of the start
     */
    private SqlCode getStartRestriction()
    {
        List<SqlCode> conditions = new ArrayList<>();

        for(VariableBinding binding : getCarriedInitBindings().getValues())
        {
            ResourceClass resClass = binding.getClasses().iterator().next();
            List<Column> columns = binding.getMapping(resClass);

            for(int i = 0; i < columns.size(); i++)
            {
                // a constant is the same in every initial solution
                if(columns.get(i) instanceof ConstantColumn)
                    continue;

                Column left = columns.get(i).fromTable(initTable);
                Column right = columns.get(i).fromTable(startsTable);
                SqlType type = resClass.getSqlTypes().get(i);

                conditions.add(resClass.isOptionalColumn(i) ? type.notDistinct(left, right) : type.equal(left, right));
            }
        }

        return conditions.stream().collect(SqlCode.joining(" AND "));
    }


    /**
     * Bindings of the carried variables in the initial solutions, the columns of which the distinct starts of a walk
     * evaluated for every start by itself hold (see {@link #translate}).
     *
     * @return bindings of the carried variables in the initial solutions
     */
    private VariableBindings getCarriedInitBindings()
    {
        VariableBindings bindings = new VariableBindings();

        for(Variable variable : carried.getVariables())
            bindings.add(init.getVariable(variable));

        return bindings;
    }


    /**
     * True if the step, a table access looked up for every row of the recursion, is looked up by a nested loop that
     * keeps the rows found for every node it is looked up for (Memoize, see
     * {@link cz.iocb.sparql.engine.database.PlanAdvice#MEMOIZED_LOOKUP}): plan advice makes the planner look the step
     * up (see {@link Request#isAdvised}), outside a pattern of an expression, where the lookups of a join stay fenced
     * as well (see {@link SqlJoin}), the step can be looked up in place (see
     * {@link SqlTableAccess#canBeLookedUpInPlace}) by the row of the recursion alone, and the walk departs from several
     * starts. The recursion holds a node apart for every start reaching it, in its rows or in the recursion of each
     * start of a walk evaluated for every start by itself (see {@link #isWalkedPerStart}), so a node reached from
     * several starts is looked up for each of them, and the rows found for it serve them all. A step that refers to the
     * start as well (the graph of a walk evaluated for every start by itself) is looked up by the values of two
     * sources, which stays fenced, and the database would drop the rows kept whenever the start changes. How many
     * starts reach a node no statistics tell, so the step is memoized wherever as many starts as the lookups of a value
     * have to repeat for the rows to be kept (see {@link EngineSettings#memoizeRatio()}) may reach one: the walk of the
     * nextProt terms related to each other took 7.5 minutes instead of 17.3, whereas a node reached from a single start
     * costs about half a lookup more.
     *
     * @param request the current request
     * @param scope the scope of the walk
     * @param access the step
     * @param byRow SQL condition restricting the step to the node, and the carried graph, of a row of the recursion, or
     *            null
     * @param byStart SQL condition restricting the step to the start the recursion is joined with, or null
     * @return true if the step is looked up by a memoized nested loop, false otherwise
     */
    private boolean isStepMemoized(Request request, Scope scope, SqlTableAccess access, SqlCode byRow, SqlCode byStart)
    {
        if(!request.isAdvised() || scope.isWithinExpression() || !access.canBeLookedUpInPlace())
            return false;

        if(byRow == null || byStart != null || beginVar == null || carried.get(beginVar) == null)
            return false;

        double starts = JoinOptimizer.estimateDistinct(request, init, beginVar);

        // the test against a sibling keeps only the starts the sibling binds
        if(startTest != null)
            starts = Math.min(starts, JoinOptimizer.estimateDistinct(request, startTest.sibling(), beginVar));

        return starts >= request.getEngineSettings().memoizeRatio();
    }


    /**
     * SQL code of the solutions whose start passes the test against the sibling (see {@link #startTest}): the
     * solutions, under the alias of the test, for which a compatible solution of the sibling exists. With the join
     * optimizer on, the sibling is probed for every solution when that costs less than hashing it (see
     * {@link #isProbed}): it is then restricted to its solutions compatible with the solution (see
     * {@link SqlIntercode#translateForRow}), which the database looks up by an index and stops at the first of;
     * otherwise its solutions are deduplicated (see {@link SqlDistinct#deduplicate}), so that the database evaluates
     * the sibling once and hashes it. Without the optimizer the database chooses either way itself.
     *
     * @param request the current request
     * @param scope the scope of the walk
     * @param code SQL code of the solutions: the initial solutions, or the distinct starts
     * @param columns columns of the solutions the code exposes, which the code returned keeps
     * @param solutions estimated number of the solutions
     * @return SQL code of the solutions whose start passes the test
     */
    private SqlCode translateStartTest(Request request, Scope scope, SqlCode code, Collection<Column> columns,
            double solutions)
    {
        ClassRelations relations = request.getConfiguration();
        SqlIntercode sibling = startTest.sibling();
        boolean optimizer = request.getConfiguration().isJoinOptimizerEnabled();

        SqlCode tested;

        if(optimizer && isProbed(request, solutions))
        {
            tested = sibling.translateForRow(request, scope, getStartTestRows());
        }
        else
        {
            tested = sibling.translate(request, scope);

            if(optimizer && tested.getOuterTables().isEmpty())
                tested = SqlDistinct.deduplicate(request, sibling, tested);
        }

        SqlBuilder builder = new SqlBuilder();

        builder.append("SELECT ");

        if(!columns.isEmpty())
            builder.append(columns.stream().collect(SqlCode.joining(", ")));
        else
            builder.append("1");

        builder.append(" FROM (");
        builder.append(code);
        builder.append(") AS ");
        builder.appendAlias(startTest.table());
        builder.append(" WHERE EXISTS (SELECT 1 FROM (");
        builder.append(tested);
        builder.append(") AS ");
        builder.append(siblingTable);

        SqlCode condition = generateJoinCondition(relations, getStartBinding(), sibling.getVariableBindings(),
                startTest.table(), siblingTable);

        if(condition != null)
        {
            builder.append(" WHERE ");
            builder.append(condition);
        }

        builder.append(")");

        return builder.toCode();
    }


    /**
     * The walk evaluated as a whole, from all its initial solutions, or for every start by itself (see
     * {@link #isWalkedPerStart}): the distinct starts of the initial solutions (the carried columns, with the grouping
     * keys of the values that are equal without being the same terms, see {@link SqlType#getGroupingKey}), of which
     * only those whose start passes the test against a sibling are kept (see {@link #startTest}), joined laterally with
     * the recursion whose initial solutions are restricted to the start (see {@link #getStartRestriction}).
     */
    @Override
    public SqlCode translate(Request request, Scope scope)
    {
        if(!isWalkedPerStart(request))
            return translateRecursion(request, scope, false);

        SqlBuilder builder = new SqlBuilder();

        builder.append("SELECT ");

        Set<Column> columns = getVariableBindings().getNonConstantColumns();

        // the start and the graph are those of the start, the end is reached by the recursion
        if(!columns.isEmpty())
            builder.append(columns.stream().map(c -> ev.contains(c) ?
                    SqlCode.of(iv.get(ev.indexOf(c)).fromTable(startsTable), " AS ", c) : c.fromTable(pathsTable))
                    .collect(SqlCode.joining(", ")));
        else
            builder.append("1");

        SqlBuilder starts = new SqlBuilder();

        starts.append("SELECT DISTINCT ");
        starts.append(iv.stream().collect(SqlCode.joining(", ")));

        for(Column column : iv)
        {
            if(column.getType().hasGroupingKey())
            {
                starts.append(", ");
                starts.append(column.getType().getGroupingKey(column));
            }
        }

        starts.append(" FROM (");
        starts.append(init.translate(request, scope));
        starts.append(") AS ");
        starts.append(initTable);

        builder.append(" FROM (");
        builder.append(startTest == null ? starts.toCode() : translateStartTest(request, scope, starts.toCode(), iv,
                JoinOptimizer.estimateDistinct(request, init, beginVar)));
        builder.append(") AS ");
        builder.append(startsTable);
        builder.append(" CROSS JOIN LATERAL (");
        builder.append(translateRecursion(request, scope, true));
        builder.append(") AS ");
        builder.append(pathsTable);

        return builder.toCode();
    }


    /**
     * SQL code of the recursion: the non-recursive term reading the initial solutions, and the recursive term joining
     * the recursion with the step. The recursion of a walk evaluated for every start by itself (see
     * {@link #isWalkedPerStart}) reads only the initial solutions of the start it is joined with laterally (see
     * {@link #getStartRestriction}) and carries the end node alone, the start and the graph being the same in all its
     * rows: the step is joined with the graph of the start.
     *
     * @param request the current request
     * @param scope the scope of the walk
     * @param perStart whether the walk is evaluated for every start by itself, joined laterally with the start
     * @return SQL code of the recursion
     */
    private SqlCode translateRecursion(Request request, Scope scope, boolean perStart)
    {
        ClassRelations relations = request.getConfiguration();

        // the recursion of a walk evaluated for every start by itself carries neither the start nor the graph, the
        // same in all its rows: they are read from the start the recursion is joined with
        List<Column> carriedColumns = perStart ? List.of() : ev;
        List<Column> initColumns = perStart ? List.of() : iv;

        List<ResourceClass> endVarClasses = new LinkedList<>(endBinding.getClasses()); // to stable order

        SqlBuilder builder = new SqlBuilder();

        builder.append("WITH RECURSIVE recursion(");

        builder.append(carriedColumns.stream().collect(SqlCode.joining(", ")));

        boolean hasRecursionVariable = !carriedColumns.isEmpty();

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

        /* NOTE: UNION takes two rows for the same when their values are equal, whereas equal values of some types need
         * not be identical (see SqlType.getGroupingKey), so the recursion carries the grouping keys of such values as
         * columns of its own.
         */
        List<SqlCode> initKeys = new ArrayList<>();
        List<SqlCode> stepKeys = new ArrayList<>();

        for(int i = 0; i < carriedColumns.size(); i++)
            if(carriedColumns.get(i).getType().hasGroupingKey())
                initKeys.add(carriedColumns.get(i).getType().getGroupingKey(initColumns.get(i)));

        int keys = initKeys.size();

        for(ResourceClass resClass : endVarClasses)
            for(Column column : endBinding.getMapping(resClass))
                if(!(column instanceof ConstantColumn) && column.getType().hasGroupingKey())
                    keys++;

        for(int i = 0; i < keys; i++)
        {
            appendComma(builder, hasRecursionVariable);
            hasRecursionVariable = true;

            builder.append("\"@key" + i + "\"");
        }

        if(!hasRecursionVariable)
            builder.append("\"@none\"");

        builder.append(") AS (SELECT ");

        builder.append(initColumns.stream().collect(SqlCode.joining(", ")));

        boolean hasInitSelect = !initColumns.isEmpty();

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

                SqlType type = endBinding.getMapping(resClass).get(j).getType();

                if(type.hasGroupingKey())
                    initKeys.add(type.getGroupingKey(columns.get(j)));
            }
        }

        for(SqlCode key : initKeys)
        {
            appendComma(builder, hasInitSelect);
            hasInitSelect = true;

            builder.append(key);
        }

        if(!hasInitSelect)
            builder.append("1");

        // a walk evaluated as a whole takes only the initial solutions whose start passes the test against a sibling;
        // the starts of a walk evaluated for every start by itself have passed it already
        builder.append(" FROM (");
        builder.append(perStart || startTest == null ? init.translate(request, scope) :
                translateStartTest(request, scope, init.translate(request, scope),
                        init.getVariableBindings().getNonConstantColumns(), JoinOptimizer.estimateRows(request, init)));
        builder.append(") AS ");
        builder.append(initTable);

        // a walk starting from the values of expressions takes only the initial solutions compatible with them; the
        // values are columns of the current solution of the enclosing EXISTS pattern, addressed already, and they are
        // compared here unless the table accesses of the initial solutions have taken them into their conditions
        Map<Variable, SqlExpressionIntercode> compared = new LinkedHashMap<>(start);
        compared.entrySet().removeIf(e -> init.isRestricted(e.getKey(), e.getValue()));

        // a walk evaluated for every start by itself takes only the initial solutions of that start
        SqlCode startCondition = and(generateJoinCondition(relations, getStartBindings(compared),
                init.getVariableBindings(), null, initTable), perStart ? getStartRestriction() : null);

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
                        && union.getChilds().stream().allMatch(b -> canLookUp(request, b, joinVar, graphVar))));
        AliasTable left = lateral ? walkTable : leftTable;
        AliasTable right = lateral ? stepTable : rightTable;

        builder.append(carriedColumns.stream().map(c -> c.fromTable(left)).collect(SqlCode.joining(", ")));

        boolean hasUnionSelect = !carriedColumns.isEmpty();

        for(Column column : carriedColumns)
            if(column.getType().hasGroupingKey())
                stepKeys.add(column.getType().getGroupingKey(column.fromTable(left)));

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

                SqlType type = endBinding.getMapping(resClass).get(j).getType();

                if(type.hasGroupingKey())
                    stepKeys.add(columns != null ? type.getGroupingKey(columns.get(j)) : new SqlCode("NULL::boolean"));
            }
        }

        for(SqlCode key : stepKeys)
        {
            appendComma(builder, hasUnionSelect);
            hasUnionSelect = true;

            builder.append(key);
        }

        if(!hasUnionSelect)
            builder.append("1");

        builder.append(" FROM recursion AS ");
        builder.append(left);

        // the step is joined with the carried variables as the recursion holds them, so that a path stays in its graph
        // also when the parent needs the graph variable in fewer classes or not at all; a walk evaluated for every
        // start by itself joins it with them as the start holds them
        VariableBindings tmp = perStart ? new VariableBindings() : new VariableBindings(carried);
        tmp.add(new VariableBinding(joinVar, endBinding.getMappings(), endBinding.canBeNull()));
        VariableBindings starting = perStart ? getCarriedInitBindings() : new VariableBindings();

        // a row of the step leading back to the node it departs from is left out of every form of the join (see
        // getNoLoopCondition)
        if(lateral)
        {
            builder.append(" CROSS JOIN LATERAL (");
            builder.append(((SqlUnion) next).translate(request, scope, (branch, alias) -> and(
                    and(generateJoinCondition(relations, tmp, branch.getVariableBindings(), left, alias),
                            generateJoinCondition(relations, starting, branch.getVariableBindings(), startsTable,
                                    alias)),
                    getNoLoopCondition(request, branch.getVariable(joinVar), branch.getVariable(endVar), alias))));
            builder.append(") AS ");
            builder.append(right);
        }
        else if(request.getConfiguration().isJoinOptimizerEnabled() && next instanceof SqlTableAccess access
                && access.getTable() != null)
        {
            // with the join optimizer on, the planner is told not to nest loops, so a step reading a table is looked
            // up laterally through an index of the table for every row of the recursion instead of being hashed whole
            // in every round (see JoinOptimizer); the node reached is converted to the class of the columns of the
            // step, so that the index serves the lookup also when the path leads through nodes of another class
            SqlCode byRow = access.getLookupCondition(relations, tmp, left, Map.of());
            SqlCode byStart = access.getLookupCondition(relations, starting, startsTable, Map.of());
            SqlCode lookup = and(byRow, byStart);
            SqlCode condition = and(lookup, getNoLoopCondition(request, access.getInternalVariableBinding(joinVar),
                    access.getInternalVariableBinding(endVar), null));

            Set<Column> strict = new HashSet<>(access.getLookupComparedColumns(relations, tmp));
            strict.addAll(access.getLookupComparedColumns(relations, starting));

            builder.append(" CROSS JOIN LATERAL (");

            // where plan advice makes the planner look the step up, the step of a walk from several starts is a plain
            // join, which the advice makes a nested loop keeping the rows found for every node (see isStepMemoized)
            if(isStepMemoized(request, scope, access, byRow, byStart))
                builder.append(access.translateInPlace(request, condition, strict, true));
            else
                builder.append(access.translateLateral(request, condition != null ? condition : SqlCode.TRUE,
                        lookup != null ? strict : Set.of()));

            builder.append(") AS ");
            builder.append(right);
        }
        else
        {
            builder.append(", (");
            builder.append(next.translate(request, scope));
            builder.append(") AS ");
            builder.append(right);

            SqlCode condition = and(
                    and(generateJoinCondition(relations, tmp, next.getVariableBindings(), left, right),
                            generateJoinCondition(relations, starting, next.getVariableBindings(), startsTable, right)),
                    getNoLoopCondition(request, next.getVariable(joinVar), next.getVariable(endVar), right));

            if(condition != null)
            {
                builder.append(" WHERE ");
                builder.append(condition);
            }
        }

        builder.append(") SELECT ");

        // the start and the graph of a walk evaluated for every start by itself are read from the start
        Set<Column> columns = getVariableBindings().getNonConstantColumns();

        if(perStart)
            columns.removeAll(ev);

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
     * SQL condition that a row of the step leads to another node than the one it departs from. A row leading back to
     * its node gives the recursion the pair of a start and a node that it holds already, with the same carried
     * variables, which the union of the recursion would drop as a duplicate, so the row is left out before the database
     * joins it with the rows of the recursion and looks the pair up among them; a property every node of which is
     * related to itself makes as many such rows as the walk has solutions. The nodes are compared as {@code sameTerm}
     * compares them, as equal values of some types are different terms (see {@link SqlType#getGroupingKey}). Null when
     * a node may be missing or when the two nodes never take the same term.
     *
     * @param request the current request
     * @param from binding of the node the step departs from, or null
     * @param to binding of the node the step reaches, or null
     * @param table alias the columns of the bindings are addressed through, or null when they are written as they are
     * @return SQL condition over the columns of the step, or null when no row has to be left out
     */
    private static SqlCode getNoLoopCondition(Request request, VariableBinding from, VariableBinding to, Table table)
    {
        if(from == null || to == null || from.canBeNull() || to.canBeNull())
            return null;

        ClassRelations relations = request.getConfiguration();

        SqlExpressionIntercode same = SqlBuiltinCall.create(request, "sameterm", false,
                List.of(SqlVariable.create(relations, qualify(from, table)),
                        SqlVariable.create(relations, qualify(to, table))));

        SqlExpressionIntercode different = SqlUnaryLogical.create(relations, same);

        if(different.equals(trueValue))
            return null;

        if(different.equals(falseValue))
            return SqlCode.FALSE;

        return different.get(relations, xsdBoolean).get(0).getCode();
    }


    /**
     * The binding with its columns addressed through the table.
     *
     * @param binding the binding
     * @param table the alias of the table, or null to keep the columns as they are
     * @return the binding with its columns addressed through the table
     */
    private static VariableBinding qualify(VariableBinding binding, Table table)
    {
        if(table == null)
            return binding;

        VariableBinding qualified = new VariableBinding(binding.getVariable(), binding.canBeNull());

        for(Entry<ResourceClass, List<Column>> entry : binding.getMappings().entrySet())
            qualified.addMapping(entry.getKey(), toTableColumns(table, entry.getValue()));

        return qualified;
    }


    /**
     * Conjunction of two SQL conditions, either of which may be absent.
     *
     * @param first the first condition, or null
     * @param second the second condition, or null
     * @return conjunction of the conditions, or null when both are absent
     */
    private static SqlCode and(SqlCode first, SqlCode second)
    {
        if(first == null)
            return second;

        if(second == null)
            return first;

        return SqlCode.of(first, " AND ", second);
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
     * Binding of the end variable over the disjoint classes it takes in the initial and the recursive step. A column
     * that both the initial solutions and the step bind to one constant keeps the constant, as a union keeps it, so
     * that the recursion neither carries nor compares it: the constant is in every row of both sides, so its class is
     * the only class of the end.
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
        ClassRelations relations = request.getConfiguration();
        VariableBinding initEndBinding = init.getVariableBindings().get(endVar);
        VariableBinding nextEndBinding = next.getVariableBindings().get(endVar);

        List<VariableBinding> defs = Stream.of(initEndBinding, nextEndBinding).filter(Objects::nonNull).toList();

        VariableBinding endBinding = new VariableBinding(endVar, false);

        for(ResourceClass resClass : unionResourceClasses(relations, defs))
        {
            List<Column> columns = new ArrayList<>(resClass.createColumns(request.getColumnMap(), endVar));

            List<Column> initColumns = initEndBinding != null ? initEndBinding.deriveMapping(relations, resClass) :
                    null;
            List<Column> nextColumns = nextEndBinding != null ? nextEndBinding.deriveMapping(relations, resClass) :
                    null;

            if(initColumns != null && nextColumns != null)
                for(int i = 0; i < columns.size(); i++)
                    if(initColumns.get(i) instanceof ValueColumn value && value.equals(nextColumns.get(i)))
                        columns.set(i, value);

            endBinding.addMapping(resClass, columns);
        }

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
                || start.values().stream().anyMatch(SqlExpressionIntercode::hasServiceSubpattern)
                || startTest != null && startTest.sibling().hasServiceSubpattern();
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

        if(startTest != null)
        {
            indentInfo(builder, indent, true);
            builder.append("from ");
            builder.append(beginVar);
            builder.append(" in ");
            startTest.sibling().generateExplanation(builder, getIndent(indent, false) + "  ");
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

        if(!Objects.equals(startTest, imcode.startTest))
            return false;

        return true;
    }


    @Override
    protected int getHashCode()
    {
        int hash = Objects.hash(graphVar, beginVar, joinVar, endBinding, init, next, reverseNext, reverseJoinVar,
                reverseInit, segmentClasses, start, delivered);

        // a walk without a test is hashed by its other parts alone
        return startTest == null ? hash : 31 * hash + startTest.hashCode();
    }
}
