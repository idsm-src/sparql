package cz.iocb.sparql.engine.imcode;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.IntStream;
import cz.iocb.sparql.engine.database.AliasTable;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ColumnPair;
import cz.iocb.sparql.engine.database.Condition;
import cz.iocb.sparql.engine.database.Conditions;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.VirtualTable;
import cz.iocb.sparql.engine.imcode.expression.SqlExpressionIntercode;
import cz.iocb.sparql.engine.mapping.classes.ClassRelations;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.Estimator;
import cz.iocb.sparql.engine.translator.Multiset;
import cz.iocb.sparql.engine.translator.VariableBinding;
import cz.iocb.sparql.engine.translator.VariableBindingPair;
import cz.iocb.sparql.engine.translator.VariableBindingPair.ResourceClassPair;
import cz.iocb.sparql.engine.translator.VariableBindings;



/**
 * Natural join of several children on their shared variables. Optimisation flattens nested joins, merges accesses to
 * the same table or along keys ({@link SqlTableAccess}), distributes the join over unions (a leading OPTIONAL sharing a
 * variable with another child being turned into one first, see {@link SqlLeftJoin#toUnion}), seeds the walks of
 * property paths with the siblings that bind their start (see {@link SqlRecursive#seed}; a walk binding the start
 * without being distinct over it is shared, so that its distinct values seed the other walk, see {@link SqlShared}),
 * evaluates nondeterministic children last, and decides the evaluation of SERVICE stubs by row estimates when service
 * reordering is enabled.
 */
public final class SqlJoin extends SqlIntercode
{
    /**
     * Rank of the seeds of a walk that are expected to have as many rows as a whole table (see {@link #seedRank}).
     */
    private static final int wholeTableRank = 2;

    /**
     * Alias of each child.
     */
    private final List<AliasTable> tables;

    /**
     * Joined children.
     */
    private final List<SqlIntercode> childs;

    /**
     * For each output column, the child column it is taken from.
     */
    private final Map<Column, Column> columnMap;

    /**
     * The join tree chosen by the optimizer (see {@link #getPlan}), once computed.
     */
    private JoinOptimizer.Plan plan;


    /**
     * Creates the node.
     *
     * @param childs the child nodes
     * @param tables the tables
     * @param bindings the variable bindings
     * @param columnMap the column map
     */
    protected SqlJoin(List<SqlIntercode> childs, List<AliasTable> tables, VariableBindings bindings,
            Map<Column, Column> columnMap)
    {
        super(bindings, childs.stream().allMatch(c -> c.isDeterministic()));

        this.childs = childs;
        this.columnMap = columnMap;
        this.tables = tables;
    }


    /**
     * Join of two children.
     *
     * @param request the current request
     * @param left the left side
     * @param right the right side
     * @return join of two children
     */
    public static SqlIntercode join(Request request, SqlIntercode left, SqlIntercode right)
    {
        return join(request, List.of(left, right), null);
    }


    /**
     * Join of the children.
     *
     * @param request the current request
     * @param childs the child nodes
     * @return join of the children
     */
    protected static SqlIntercode join(Request request, List<SqlIntercode> childs)
    {
        return join(request, childs, null);
    }


    /**
     * Join of the children exposing only what the parent needs.
     *
     * @param request the current request
     * @param childs the child nodes
     * @param restrictions what the parent needs of the variables
     * @return join of the children exposing only what the parent needs
     */
    protected static SqlIntercode join(Request request, List<SqlIntercode> childs, Restrictions restrictions)
    {
        List<AliasTable> tables = IntStream.range(0, childs.size()).mapToObj(i -> new AliasTable("tab" + i)).toList();

        Map<Column, Column> columnMap = new HashMap<>();
        List<VariableBindings> allVars = childs.stream().map(c -> c.getVariableBindings()).toList();
        VariableBindings bindings = getJoinVariableBindings(request, allVars, tables, restrictions, columnMap);

        return new SqlJoin(childs, tables, bindings, columnMap);
    }


    @Override
    public SqlIntercode optimize(Request request, Scope scope, Restrictions restrictions, boolean reduced,
            boolean evalServices)
    {
        ClassRelations relations = request.getConfiguration();

        // a reordered SERVICE call is evaluated for the solutions of its siblings, which a statement of its own reads,
        // so they must not take in the values of a current solution of an EXISTS pattern
        if(request.isServiceReorderEnabled() && childs.stream().anyMatch(SqlIntercode::hasServiceSubpattern))
        {
            restrictions = new Restrictions(restrictions);
            restrictions.removeAcceptedValues();
        }

        List<SqlIntercode> flatChilds = flatChilds(optimize(request, scope, flatChilds(childs), restrictions, reduced));

        boolean hasService = false;

        if(evalServices)
        {
            List<SqlIntercode> tmp = new ArrayList<>();

            for(SqlIntercode l : flatChilds)
            {
                if(l.hasServiceSubpattern() && !(request.isServiceReorderEnabled() && l instanceof SqlServiceStub))
                    tmp.add(l.optimize(request, scope, getRestrictions(relations, l, flatChilds, restrictions), reduced,
                            true));
                else
                    tmp.add(l);

                if(request.isServiceReorderEnabled() && l instanceof SqlServiceStub)
                    hasService = true;
            }

            flatChilds = flatChilds(tmp);
        }

        List<SqlIntercode> deterministic = new ArrayList<>();
        List<SqlIntercode> nondeterministic = new ArrayList<>();

        for(SqlIntercode child : flatChilds)
        {
            if(child.isDeterministic())
                deterministic.add(child);
            else
                nondeterministic.add(child);
        }


        Restrictions newRestrictions = new Restrictions(restrictions);

        for(SqlIntercode child : nondeterministic)
            for(VariableBinding v : child.getVariableBindings().getValues())
                newRestrictions.add(v.getVariable(), v.getClasses());


        List<SqlIntercode> optChilds = deterministic;

        DatabaseSchema schema = request.getConfiguration().getDatabaseSchema();

        // the children could swap their presentations of a shared variable in every round
        Settlement settlement = new Settlement();

        while(true)
        {
            List<SqlIntercode> optimized = optimize(request, scope, optChilds, newRestrictions, reduced, evalServices,
                    settlement.getClasses());
            List<SqlIntercode> newChilds = reduceDistinctUnion(relations,
                    reduceJoin(relations, optimized, newRestrictions, schema), schema);

            newChilds = expandLeadingOptionals(request, scope, newChilds, evalServices);
            newChilds = seedWalks(request, scope, newChilds, newRestrictions);
            newChilds = shareSeeds(request, scope, newChilds, evalServices);

            if(!Objects.equals(new Multiset<>(newChilds), new Multiset<>(optChilds)))
                optChilds = newChilds;

            if(newChilds.size() > 1 && newChilds.stream().anyMatch(c -> c instanceof SqlUnion || c instanceof SqlJoin))
            {
                newChilds = List
                        .of(SqlUnion
                                .union(request,
                                        expandJoin(relations, newChilds).stream()
                                                .map(l -> join(request, l, newRestrictions)).toList())
                                .optimize(request, scope, newRestrictions, reduced, evalServices));

                if(newChilds.size() == 1 && newChilds.get(0) instanceof SqlJoin join)
                    newChilds = join.getChilds();

                if(!Objects.equals(new Multiset<>(newChilds), new Multiset<>(optChilds)))
                    optChilds = newChilds;
            }

            if(optChilds != newChilds)
                break;

            // the children in their order, as the next round depends on it (the first walk that can be seeded is)
            if(!settlement.record(List.copyOf(optChilds), optChilds))
                break;
        }


        if(evalServices && hasService)
        {
            Estimator estimator = new Estimator(request);

            record Stats(Set<Variable> vars, long count, int lost)
            {
            }

            Map<SqlServiceStub, Stats> stats = new HashMap<>();


            for(SqlIntercode item : nondeterministic)
            {
                if(!(item instanceof SqlServiceStub service))
                    continue;

                Collection<Variable> vars = service.getServiceVariables().values();

                Restrictions serviceRestrictions = new Restrictions(vars);

                SqlIntercode join = join(request, optChilds).optimize(request, scope, serviceRestrictions, false,
                        false);

                Map<Variable, Long> basic = new HashMap<>();

                for(Variable var : vars)
                {
                    if(join.bindings.getVariables().contains(var))
                    {
                        try
                        {
                            basic.put(var, estimator.estimateDistinct(request, scope, join, var));
                        }
                        catch(SQLException e)
                        {
                            e.printStackTrace();
                        }
                    }
                }


                ArrayDeque<Variable> order = new ArrayDeque<>(basic.entrySet().stream()
                        .sorted(Map.Entry.<Variable, Long> comparingByValue(Comparator.reverseOrder()))
                        .map(Map.Entry::getKey).toList());

                Set<Variable> set = new HashSet<>(order);
                long count = estimator.estimateDistinct(request, scope, join, set);

                while(count > 20000l) //TODO: export limit
                {
                    set.remove(order.pop());
                    count = estimator.estimateDistinct(request, scope, join, set);
                }

                stats.put(service, new Stats(set, count, vars.size() - set.size()));
            }

            Entry<SqlServiceStub, Stats> entry = stats
                    .entrySet().stream().sorted(Map.Entry.<SqlServiceStub, Stats> comparingByValue((a,
                            b) -> a.lost == b.lost ? Long.compare(a.count, b.count) : Integer.compare(a.lost, b.lost)))
                    .findFirst().get();

            SqlServiceStub service = entry.getKey();

            Set<Variable> vars = entry.getValue().vars;
            SqlIntercode context = SqlDistinct.create(request, join(request, optChilds), vars).optimize(request, scope,
                    new Restrictions(vars), false, false);

            SqlIntercode evaluated = service.eval(request, scope, context,
                    getRestrictions(relations, service, flatChilds, restrictions));


            nondeterministic.remove(service);

            optChilds = new ArrayList<>(optChilds);
            optChilds.add(evaluated);

            List<SqlIntercode> fullChilds = mergeChilds(request, scope, optChilds, nondeterministic, restrictions,
                    reduced);

            return join(request, fullChilds, restrictions).optimize(request, scope, restrictions, reduced, true);
        }


        List<SqlIntercode> fullChilds = mergeChilds(request, scope, optChilds, nondeterministic, restrictions, reduced);

        if(fullChilds.size() == 0)
            return SqlEmptySolution.get();

        if(fullChilds.size() == 1)
            return fullChilds.get(0);

        if(!isJoinable(relations, fullChilds))
            return SqlNoSolution.get();


        if(fullChilds.equals(childs) && restrictions.isOptimized(relations, bindings))
            return this;

        return join(request, fullChilds, restrictions);
    }


    /**
     * Flattens nested joins into their children.
     *
     * @param input child columns
     * @return the flattened children
     */
    private static List<SqlIntercode> flatChilds(List<SqlIntercode> input)
    {
        List<SqlIntercode> output = new ArrayList<>();

        Deque<SqlIntercode> stack = new ArrayDeque<>();

        stack.addAll(input);

        while(!stack.isEmpty())
        {
            SqlIntercode child = stack.pop();

            if(child instanceof SqlJoin join)
                stack.addAll(join.childs);
            else
                output.add(child);
        }

        return output;
    }


    /**
     * Joins the optimised deterministic children with the nondeterministic ones, distributing over a nondeterministic
     * union so that its branches are joined separately.
     *
     * @param request the current request
     * @param scope the scope of the join
     * @param deterministic the deterministic children
     * @param nondeterministic the nondeterministic children
     * @param restrictions what the parent needs of the variables
     * @param reduced whether duplicate solutions may be dropped
     * @return the joined children
     */
    private static List<SqlIntercode> mergeChilds(Request request, Scope scope, List<SqlIntercode> deterministic,
            List<SqlIntercode> nondeterministic, Restrictions restrictions, boolean reduced)
    {
        if(nondeterministic.size() == 1 && nondeterministic.get(0) instanceof SqlUnion union)
        {
            List<SqlIntercode> branches = new ArrayList<>();

            for(SqlIntercode child : union.getChilds())
            {
                List<SqlIntercode> l = new ArrayList<>();
                l.addAll(deterministic);
                l.add(child);

                branches.add(join(request, l, null));
            }

            return List.of(SqlUnion.union(request, branches).optimize(request, scope, restrictions, reduced, false));
        }


        List<SqlIntercode> result = new ArrayList<>();

        result.addAll(deterministic);
        result.addAll(nondeterministic);

        return result;
    }


    /**
     * Optimises each child with the restrictions implied by the parent and by the other children.
     *
     * @param request the current request
     * @param scope the scope of the join
     * @param childs the child nodes
     * @param restrictions what the parent needs of the variables
     * @param reduced whether duplicate solutions may be dropped
     * @param evalServices whether SERVICE stubs are evaluated
     * @return the optimised children
     */
    public static List<SqlIntercode> optimize(Request request, Scope scope, List<SqlIntercode> childs,
            Restrictions restrictions, boolean reduced, boolean evalServices)
    {
        return optimize(request, scope, childs, restrictions, reduced, evalServices, Map.of());
    }


    /**
     * Optimises each child with the restrictions implied by the parent, by the other children and by the settled
     * classes of the optimisation loop (see {@link Settlement}).
     *
     * @param request the current request
     * @param scope the scope of the join
     * @param childs the child nodes
     * @param restrictions what the parent needs of the variables
     * @param reduced whether duplicate solutions may be dropped
     * @param evalServices whether SERVICE stubs are evaluated
     * @param settled settled classes per variable, see {@link Settlement#getClasses}
     * @return the optimised children
     */
    private static List<SqlIntercode> optimize(Request request, Scope scope, List<SqlIntercode> childs,
            Restrictions restrictions, boolean reduced, boolean evalServices, Map<Variable, Set<ResourceClass>> settled)
    {
        ClassRelations relations = request.getConfiguration();

        List<SqlIntercode> optimized = new ArrayList<>(childs.size());

        for(SqlIntercode child : childs)
            optimized.add(child.optimize(request, scope,
                    getRestrictions(relations, child, childs, restrictions, settled), reduced, evalServices));

        return optimized;
    }


    /**
     * Optimises each child with the restrictions implied by the parent and by the other children, without evaluating
     * services.
     *
     * @param request the current request
     * @param scope the scope of the join
     * @param childs the child nodes
     * @param restrictions what the parent needs of the variables
     * @param reduced whether duplicate solutions may be dropped
     * @return the optimised children
     */
    public static List<SqlIntercode> optimize(Request request, Scope scope, List<SqlIntercode> childs,
            Restrictions restrictions, boolean reduced)
    {
        return optimize(request, scope, childs, restrictions, reduced, false);
    }


    /**
     * Simplifies the children: propagates no-solution, drops empty solutions, detects unjoinable children, pushes
     * constants and VALUES into table accesses and merges table accesses joined along keys.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param childs the child nodes
     * @param restrictions what the parent needs of the variables
     * @param schema the database schema
     * @return the simplified children
     */
    private static List<SqlIntercode> reduceJoin(ClassRelations relations, List<SqlIntercode> childs,
            Restrictions restrictions, DatabaseSchema schema)
    {
        if(childs.stream().anyMatch(c -> c.equals(SqlNoSolution.get())))
            return List.of(SqlNoSolution.get());

        childs = childs.stream().filter(c -> !c.equals(SqlEmptySolution.get())).toList();

        if(childs.size() == 0)
            return List.of(SqlEmptySolution.get());

        if(!isJoinable(relations, childs))
            return List.of(SqlNoSolution.get());


        Map<Variable, List<VariableBinding>> constants = new HashMap<>();

        for(SqlIntercode child : childs)
            for(VariableBinding v : child.getVariableBindings().getValues())
                if(v.isConstant()) // children are joinable, so constants should be consistent
                    constants.computeIfAbsent(v.getVariable(), _ -> new ArrayList<>()).add(v);

        List<SqlIntercode> optChilds = new ArrayList<>(childs);

        if(!constants.isEmpty())
        {
            for(int i = 0; i < optChilds.size(); i++)
            {
                SqlIntercode child = optChilds.get(i);

                if(child instanceof SqlTableAccess access)
                {
                    Condition cnd = new Condition();
                    Map<Variable, VariableBinding> skip = new HashMap<>();

                    for(Entry<Variable, List<VariableBinding>> e : constants.entrySet())
                    {
                        VariableBinding v = access.getInternalVariableBinding(e.getKey());

                        if(v == null || v.isConstant())
                            continue;

                        for(VariableBinding o : e.getValue())
                        {
                            if(v.getMapping(o.getClasses().iterator().next()) != null)
                            {
                                ResourceClass r = o.getClasses().iterator().next();
                                List<Column> values = o.getMapping(r);
                                List<Column> columns = v.getMapping(r);

                                cnd.addAreEqual(values, columns, SqlTableAccess.needsNullSafeEquality(schema,
                                        access.getTable(), r, values, columns));
                                skip.put(e.getKey(), o);
                            }
                        }
                    }

                    if(!skip.isEmpty())
                    {
                        Conditions cnds = Conditions.and(access.getConditions(), cnd);

                        if(cnds.isFalse())
                            return List.of(SqlNoSolution.get());

                        VariableBindings internal = new VariableBindings();

                        for(Variable variable : access.getVariableBindings().getVariables())
                        {
                            if(skip.containsKey(variable))
                                internal.add(new VariableBinding(skip.get(variable)));
                            else
                                internal.add(access.getInternalVariableBinding(variable));
                        }

                        optChilds.set(i, access.with(cnds, internal));
                    }
                }
            }
        }


        for(int i = 0; i < optChilds.size(); i++)
        {
            if(optChilds.get(i) instanceof SqlValues values)
            {
                if(values.getSize() > 1)
                {
                    List<SqlIntercode> copyChilds = new ArrayList<>(optChilds);

                    boolean[] mask = new boolean[values.getSize()];

                    for(int j = 0; j < values.getSize(); j++)
                    {
                        SqlIntercode line = values.getSlice(j);
                        copyChilds.set(i, line);

                        mask[j] = isJoinable(relations, copyChilds);
                    }


                    optChilds.set(i, values.strip(mask));
                }
            }
        }


        for(int i = 0; i < optChilds.size(); i++)
        {
            if(optChilds.get(i) instanceof SqlValues left)
            {
                for(int j = 0; j < optChilds.size(); j++)
                {
                    if(optChilds.get(j) instanceof SqlTableAccess right)
                    {
                        SqlIntercode merged = SqlTableAccess.tryReduceJoinWithValues(relations, schema, right, left,
                                null);

                        if(Objects.equals(merged, SqlNoSolution.get()))
                            return List.of(SqlNoSolution.get());

                        if(merged != null)
                        {
                            optChilds.set(j, merged);
                            optChilds.remove(i);

                            if(j < i)
                                i--;

                            break;
                        }
                    }
                }
            }
        }


        for(int i = 0; i < optChilds.size(); i++)
        {
            if(optChilds.get(i) instanceof SqlTableAccess left)
            {
                for(int j = 0; j < i; j++)
                {
                    if(optChilds.get(j) instanceof SqlTableAccess right)
                    {
                        Condition additionalLeft = new Condition();
                        Condition additionalRight = new Condition();

                        for(VariableBindingPair pair : VariableBindingPair.getPairs(relations,
                                left.getVariableBindings(), right.getVariableBindings()))
                        {
                            VariableBinding leftBinding = pair.getLeftVariableBinding();
                            VariableBinding rightBinding = pair.getRightVariableBinding();

                            //NOTE: currently, only simple join is taken into the account

                            if(pair.getClasses().size() > 1)
                                continue;

                            if(leftBinding.canBeNull() || rightBinding.canBeNull())
                                continue;

                            for(ResourceClassPair pairedClass : pair.getClasses())
                            {
                                if(!Objects.equals(pairedClass.getLeftClass(), pairedClass.getRightClass()))
                                    continue;

                                List<Column> leftCols = leftBinding.getMapping(pairedClass.getLeftClass());
                                List<Column> rightCols = rightBinding.getMapping(pairedClass.getRightClass());

                                for(int c = 0; c < leftCols.size(); c++)
                                {
                                    Column leftCol = leftCols.get(c);
                                    Column rightCol = rightCols.get(c);
                                    boolean optional = pairedClass.getLeftClass().isOptionalColumn(c);

                                    if(leftCol instanceof ConstantColumn && optional && leftCol.canBeNull()
                                            && SqlTableAccess.mayBeNull(schema, right.getTable(), rightCol))
                                        additionalRight.addAreNotDistinct(leftCol, rightCol);
                                    else if(leftCol instanceof ConstantColumn)
                                        additionalRight.addAreEqual(leftCol, rightCol);

                                    if(rightCol instanceof ConstantColumn && optional && rightCol.canBeNull()
                                            && SqlTableAccess.mayBeNull(schema, left.getTable(), leftCol))
                                        additionalLeft.addAreNotDistinct(leftCol, rightCol);
                                    else if(rightCol instanceof ConstantColumn)
                                        additionalLeft.addAreEqual(leftCol, rightCol);
                                }
                            }
                        }

                        if(Conditions.and(left.getConditions(), additionalLeft).isFalse()
                                || Conditions.and(right.getConditions(), additionalRight).isFalse())
                            return List.of(SqlNoSolution.get());
                    }
                }
            }
        }


        for(int i = 0; i < optChilds.size(); i++)
        {
            if(optChilds.get(i) instanceof SqlTableAccess left)
            {
                for(int j = 0; j < i; j++)
                {
                    SqlIntercode merged = null;

                    if(optChilds.get(j) instanceof SqlTableAccess right)
                    {
                        merged = SqlTableAccess.tryReduceJoin(relations, schema, left, right, null);
                    }
                    else if(optChilds.get(j) instanceof SqlDistinct d && d.getChild() instanceof SqlTableAccess right)
                    {
                        merged = tryReduceDistinct(relations, schema, left, right);
                    }

                    if(Objects.equals(merged, SqlNoSolution.get()))
                        return List.of(SqlNoSolution.get());

                    if(merged != null)
                    {
                        optChilds.set(i, merged);
                        optChilds.remove(j);

                        i -= 2;
                        break;
                    }
                }
            }
        }


        return optChilds;
    }


    /**
     * Replaces a distinct union of table accesses joined with another access to a related table by the merged access,
     * when the distinct part only restricts the rows of that access. The merged access keeps the variables of both
     * children it replaces (see {@link #tryReduceDistinct}), and the scan starts again after every replacement, which
     * changes the children.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param childs the child nodes
     * @param schema the database schema
     * @return the simplified children
     */
    private static List<SqlIntercode> reduceDistinctUnion(ClassRelations relations, List<SqlIntercode> childs,
            DatabaseSchema schema)
    {
        List<SqlIntercode> newChilds = new ArrayList<>(childs);

        loop:
        while(true)
        {
            for(SqlIntercode child : newChilds)
            {
                if(!(child instanceof SqlDistinct distinct && distinct.getChild() instanceof SqlUnion union))
                    continue;

                for(SqlIntercode c : newChilds)
                {
                    if(!(c instanceof SqlTableAccess candidate)
                            || !canBeDistinctUnionReduced(relations, distinct, candidate))
                        continue;

                    for(SqlIntercode d : union.getChilds())
                    {
                        if(!(d instanceof SqlTableAccess distinctPart))
                            continue;

                        SqlIntercode intercode = tryReduceDistinct(relations, schema, candidate, distinctPart);

                        if(intercode instanceof SqlTableAccess a && a.getConditions().equals(candidate.getConditions())
                                && a.hasSameValues(candidate))
                        {
                            newChilds.remove(distinct);
                            newChilds.remove(candidate);
                            newChilds.add(intercode);
                            continue loop;
                        }
                    }
                }
            }

            return newChilds;
        }
    }


    /**
     * True if all variables shared by the distinct pattern and the candidate are always bound in a single common class
     * and cover all non-constant columns of the distinct pattern.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param distinct the distinct node
     * @param candidate the access joined with the distinct pattern
     * @return true if all variables shared by the distinct pattern and the candidate are always bound in a single
     *         common class and cover all non-constant columns of the distinct pattern, false otherwise
     */
    private static boolean canBeDistinctUnionReduced(ClassRelations relations, SqlDistinct distinct,
            SqlIntercode candidate)
    {
        Set<Column> columns = new HashSet<>();

        for(VariableBindingPair pair : VariableBindingPair.getPairs(relations, distinct.getVariableBindings(),
                candidate.getVariableBindings()))
        {
            VariableBinding distinctBinding = pair.getLeftVariableBinding();
            VariableBinding candidateBinding = pair.getRightVariableBinding();

            //NOTE: currently, only simple join is taken into the account

            if(pair.getClasses().size() > 1)
                return false;

            if(distinctBinding.canBeNull() || candidateBinding.canBeNull())
                return false;

            for(ResourceClassPair pairedClass : pair.getClasses())
            {
                if(!Objects.equals(pairedClass.getLeftClass(), pairedClass.getRightClass()))
                    return false;

                columns.addAll(distinctBinding.getMapping(pairedClass.getLeftClass()));
            }
        }

        return columns.containsAll(distinct.getVariableBindings().getNonConstantColumns());
    }


    /**
     * Merges the distinct table access into the candidate access when the join along shared columns or a foreign key
     * pins the distinct rows; null otherwise. The merged access binds the variables of both accesses, as the other
     * children of the join may join any of them; the next round of the optimisation restricts it (see
     * {@link #getRestrictions}).
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param schema the database schema
     * @param candidate the access joined with the distinct pattern
     * @param distinct the deduplicated access
     * @return the merged access, or null
     */
    private static SqlIntercode tryReduceDistinct(ClassRelations relations, DatabaseSchema schema,
            SqlTableAccess candidate, SqlTableAccess distinct)
    {
        if(Objects.equals(distinct.getTable(), candidate.getTable()))
        {
            // the distinct part denotes a set over all the columns it binds
            Set<Column> distinctColumns = distinct.getInternalVariableBindings().getNonConstantColumns();

            if(!SqlTableAccess.canBeJoinedByDistinctColumns(relations, candidate, distinct, distinctColumns))
                return null;

            return SqlTableAccess.joinByDistinctColumns(relations, candidate, distinct, null);
        }
        else
        {
            // neither expressions nor values of a current solution are moved onto the columns of the other table
            if(distinct.hasExpression() || distinct.hasValues())
                return null;

            Set<ColumnPair> columns = SqlTableAccess.getJoinColumnPairs(relations, distinct, candidate);

            Set<Column> parentColumns = new HashSet<>();
            parentColumns.addAll(distinct.getConditions().getNonConstantColumns());
            parentColumns.addAll(distinct.getInternalVariableBindings().getNonConstantColumns());

            if(!columns.stream().map(p -> p.getLeft()).collect(toSet()).containsAll(parentColumns))
                return null;

            List<Set<ColumnPair>> keys = schema.getForeignKeys(distinct.getTable(), candidate.getTable());

            // the distinct part is dropped, which requires that each candidate row references a distinct row; a
            // foreign key matched simply guarantees that only for the rows with no null in any of its child columns,
            // also those not joined
            for(Set<ColumnPair> key : keys)
                if(key.containsAll(columns)
                        && key.stream().allMatch(p -> candidate.isKnownNotNull(schema, p.getRight())))
                    return SqlTableAccess.joinByForeignKey(relations, distinct, candidate, columns, null);
        }

        return null;
    }


    /**
     * The join tree chosen by the optimizer for the children (see {@link JoinOptimizer}), computed on the first call.
     *
     * @param request the current request
     * @return the join tree chosen by the optimizer for the children
     */
    JoinOptimizer.Plan getPlan(Request request)
    {
        if(plan == null)
            plan = JoinOptimizer.plan(request, childs);

        return plan;
    }


    @Override
    public String translate(Request request, Scope scope)
    {
        ClassRelations relations = request.getConfiguration();

        Set<Column> columns = bindings.getNonConstantColumns();

        StringBuilder builder = new StringBuilder();

        builder.append("SELECT ");

        if(!columns.isEmpty())
            builder.append(columns.stream().map(c -> (columnMap.get(c) != null ? columnMap.get(c) + " AS " : "") + c)
                    .collect(joining(", ")));
        else
            builder.append("1");

        builder.append(" FROM ");

        if(request.getConfiguration().isJoinOptimizerEnabled() && childs.size() > 1)
        {
            Map<Variable, Set<ResourceClass>> joined = getJoinedClasses(relations,
                    childs.stream().map(c -> c.getVariableBindings()).toList());

            translateTree(request, scope, getPlan(request).root(), joined, builder);
            return builder.toString();
        }

        for(int i = 0; i < childs.size(); i++)
        {
            if(i > 0)
                builder.append(", ");

            builder.append("(");
            builder.append(childs.get(i).translate(request, scope));
            builder.append(") AS ");
            builder.append(tables.get(i));
        }

        String condition = generateJoinCondition(relations, childs.stream().map(c -> c.getVariableBindings()).toList(),
                tables);

        if(condition != null)
        {
            builder.append(" WHERE ");
            builder.append(condition);
        }

        return builder.toString();
    }


    /**
     * Writes the join tree chosen by the optimizer: a child as its subquery, an index lookup as a lateral subquery of
     * the access restricted by its equalities with the children of the outer tree (see
     * {@link SqlTableAccess#translateLateral}), and a hash join as an explicit join of its sides on the equalities
     * between their children (a cross join when there are none), so that the planner keeps the tree (see
     * {@link cz.iocb.sparql.engine.request.Request#execute}). The equalities compare only the classes of a variable
     * that meet a class it takes in the whole join (see {@link #getJoinedClasses}).
     *
     * @param request the current request
     * @param scope the scope of the join
     * @param node the tree
     * @param joined classes per variable in the whole join (see {@link #getJoinedClasses})
     * @param builder the SQL code being built
     * @return the children of the tree, as a bit per child
     */
    private int translateTree(Request request, Scope scope, JoinOptimizer.Node node,
            Map<Variable, Set<ResourceClass>> joined, StringBuilder builder)
    {
        ClassRelations relations = request.getConfiguration();

        if(node instanceof JoinOptimizer.Leaf leaf)
        {
            builder.append("(");
            builder.append(childs.get(leaf.child()).translate(request, scope));
            builder.append(") AS ");
            builder.append(tables.get(leaf.child()));

            return 1 << leaf.child();
        }

        if(node instanceof JoinOptimizer.Lookup lookup)
        {
            int outer = translateTree(request, scope, lookup.outer(), joined, builder);
            SqlTableAccess access = (SqlTableAccess) childs.get(lookup.child());

            List<String> conditions = new ArrayList<>();

            for(int i = 0; i < childs.size(); i++)
            {
                if((outer >> i & 1) == 0)
                    continue;

                String condition = generateJoinCondition(relations, childs.get(i).getVariableBindings(),
                        access.getInternalVariableBindings(), tables.get(i), null, joined);

                if(condition != null)
                    conditions.add(condition);
            }

            builder.append(" CROSS JOIN LATERAL (");
            builder.append(access.translateLateral(request,
                    conditions.isEmpty() ? "true" : conditions.stream().collect(joining(" AND ", "(", ")"))));
            builder.append(") AS ");
            builder.append(tables.get(lookup.child()));

            return outer | 1 << lookup.child();
        }

        JoinOptimizer.HashJoin join = (JoinOptimizer.HashJoin) node;

        int left = translateTree(request, scope, join.left(), joined, builder);

        StringBuilder rightBuilder = new StringBuilder();
        int right = translateTree(request, scope, join.right(), joined, rightBuilder);

        List<String> conditions = new ArrayList<>();

        for(int i = 0; i < childs.size(); i++)
        {
            if((left >> i & 1) == 0)
                continue;

            for(int j = 0; j < childs.size(); j++)
            {
                if((right >> j & 1) == 0)
                    continue;

                String condition = generateJoinCondition(relations, childs.get(i).getVariableBindings(),
                        childs.get(j).getVariableBindings(), tables.get(i), tables.get(j), joined);

                if(condition != null)
                    conditions.add(condition);
            }
        }

        boolean nested = !(join.right() instanceof JoinOptimizer.Leaf);

        builder.append(conditions.isEmpty() ? " CROSS JOIN " : " JOIN ");

        if(nested)
            builder.append("(");

        builder.append(rightBuilder);

        if(nested)
            builder.append(")");

        if(!conditions.isEmpty())
        {
            builder.append(" ON ");
            builder.append(conditions.stream().collect(joining(" AND ", "(", ")")));
        }

        return left | right;
    }


    /**
     * Restrictions for one child given the other children.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param child the child node
     * @param childs the child nodes
     * @param restrictions what the parent needs of the variables
     * @return restrictions for one child given the other children
     */
    private static Restrictions getRestrictions(ClassRelations relations, SqlIntercode child, List<SqlIntercode> childs,
            Restrictions restrictions)
    {
        return getRestrictions(relations, child, childs, restrictions, Map.of());
    }


    /**
     * Restrictions for one child given the other children and the settled classes of the optimisation loop (see
     * {@link Settlement}).
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param child the child node
     * @param childs the child nodes
     * @param restrictions what the parent needs of the variables
     * @param settled settled classes per variable, see {@link Settlement#getClasses}
     * @return restrictions for one child given the other children and the settled classes
     */
    private static Restrictions getRestrictions(ClassRelations relations, SqlIntercode child, List<SqlIntercode> childs,
            Restrictions restrictions, Map<Variable, Set<ResourceClass>> settled)
    {
        // a node may be a child twice (the shared named graphs of two zero-length paths), each copy counting the other
        List<VariableBindings> others = new ArrayList<>(childs.size());
        boolean skipped = false;

        for(SqlIntercode other : childs)
        {
            if(other == child && !skipped)
                skipped = true;
            else
                others.add(other.getVariableBindings());
        }

        return getJoinRestrictions(relations, child.getVariableBindings(), others, restrictions, true, settled);
    }


    /**
     * Replaces each leading OPTIONAL sharing a variable with another child by the union it stands for (see
     * {@link SqlLeftJoin#toUnion}), so that the join is expanded over the branches and compares the shared variables by
     * plain equality.
     *
     * @param request the current request
     * @param scope the scope of the join
     * @param childs the child nodes
     * @param evalServices whether SERVICE stubs are evaluated
     * @return the children, the leading OPTIONALs sharing a variable with another child replaced by unions
     */
    private static List<SqlIntercode> expandLeadingOptionals(Request request, Scope scope, List<SqlIntercode> childs,
            boolean evalServices)
    {
        List<SqlIntercode> result = new ArrayList<>(childs.size());

        for(int i = 0; i < childs.size(); i++)
        {
            SqlIntercode child = childs.get(i);

            if(child instanceof SqlLeftJoin join && join.isLeadingOptional() && sharesVariable(childs, i))
                result.add(join.toUnion(request, scope, evalServices));
            else
                result.add(child);
        }

        return result;
    }


    /**
     * Seeds a walk between two variables with its siblings (see {@link SqlRecursive#seed}): the siblings that bind the
     * start variable always and distinct solutions over it, without binding the end variable, or those binding the end
     * variable so, the walk being turned then when it may be (see {@link SqlRecursive#canSeedFromEnd}); when both ends
     * have such siblings, the end whose seeds are expected to be the fewer wins (see {@link #seedRank}, and the row
     * estimates of the database on a tie, see {@link #fewerSeeds}), the start otherwise. A walk that starts from a node
     * of the current solution of an EXISTS pattern (see {@link SqlRecursive#startsFromValues}) is seeded only from its
     * end, by siblings expected to be fewer than a whole table; otherwise it keeps starting from that node. The seeds
     * leave the join, carried through the recursion instead. One walk is seeded per call; the optimisation loop comes
     * back for the others.
     *
     * @param request the current request
     * @param scope the scope of the join
     * @param childs the child nodes
     * @param restrictions what the parent needs of the variables
     * @return the children, with the first walk that can be seeded replaced by its seeded form and its seeds removed
     */
    private static List<SqlIntercode> seedWalks(Request request, Scope scope, List<SqlIntercode> childs,
            Restrictions restrictions)
    {
        for(int i = 0; i < childs.size(); i++)
        {
            if(!(childs.get(i) instanceof SqlRecursive walk) || walk.beginVar == null || walk.seeded)
                continue;

            Variable beginVar = walk.beginVar;
            Variable endVar = walk.endBinding.getVariable();

            // the seeded walk exposes what the parent and the remaining siblings need of it (the needs of the parent
            // alone would drop an end variable a sibling joins)
            Restrictions walkRestrictions = getRestrictions(request.getConfiguration(), walk, childs, restrictions);

            List<Integer> seeds = selectSeeds(request, childs, i, beginVar, endVar, walk.graphVar);
            List<Integer> endSeeds = walk.reverseNext != null ?
                    selectSeeds(request, childs, i, endVar, beginVar, walk.graphVar) : List.of();

            // the walk turned to start from its end has to agree with the seeds (see SqlRecursive.canSeedFromEnd)
            if(!endSeeds.isEmpty() && !walk.canSeedFromEnd(request, scope, endSeeds.stream().map(childs::get).toList(),
                    walkRestrictions))
                endSeeds = List.of();

            boolean fromEnd;

            if(walk.startsFromValues())
            {
                // the walk starts from a single node already, the node the current solution of an EXISTS pattern gives,
                // so its siblings seed it only from its end, when they are expected to be fewer than a whole table and
                // than the siblings binding its start
                fromEnd = !endSeeds.isEmpty() && seedRank(childs, endSeeds) < wholeTableRank
                        && (seeds.isEmpty() || seedRank(childs, endSeeds) < seedRank(childs, seeds));

                if(!fromEnd)
                    continue;
            }
            else
            {
                fromEnd = !endSeeds.isEmpty()
                        && (seeds.isEmpty() || seedRank(childs, endSeeds) < seedRank(childs, seeds)
                                || seedRank(childs, endSeeds) == seedRank(childs, seeds)
                                        && fewerSeeds(request, scope, childs, endSeeds, endVar, seeds, beginVar));
            }

            if(fromEnd)
                seeds = endSeeds;

            if(seeds.isEmpty())
                continue;

            List<SqlIntercode> result = new ArrayList<>();

            for(int j = 0; j < childs.size(); j++)
                if(j != i && !seeds.contains(j))
                    result.add(childs.get(j));

            result.add(walk.seed(request, scope, seeds.stream().map(j -> childs.get(j)).toList(), fromEnd,
                    walkRestrictions));

            return result;
        }

        return childs;
    }


    /**
     * Shares the first walk that binds an end of another walk, not seeded yet, always, without binding its other end
     * (the walks of a chain), so that the distinct values of the end read from it seed the other walk (see
     * {@link SqlShared#seed}) while it keeps its place: the walk is replaced by its shared form and the seeds join the
     * children, to be taken into the other walk by {@link #seedWalks} in the next round. Nothing is shared when the
     * other walk has siblings that seed it directly.
     *
     * @param request the current request
     * @param scope the scope of the join
     * @param childs the child nodes
     * @param evalServices whether SERVICE stubs are evaluated
     * @return the children, with the first such walk shared and the seeds read from it added
     */
    private static List<SqlIntercode> shareSeeds(Request request, Scope scope, List<SqlIntercode> childs,
            boolean evalServices)
    {
        for(int i = 0; i < childs.size(); i++)
        {
            if(!(childs.get(i) instanceof SqlRecursive walk) || walk.beginVar == null || walk.seeded)
                continue;

            Variable beginVar = walk.beginVar;
            Variable endVar = walk.endBinding.getVariable();

            for(Variable variable : walk.reverseNext != null ? List.of(beginVar, endVar) : List.of(beginVar))
            {
                Variable other = variable.equals(beginVar) ? endVar : beginVar;

                if(!selectSeeds(request, childs, i, variable, other, walk.graphVar).isEmpty())
                    continue;

                for(int j = 0; j < childs.size(); j++)
                {
                    SqlIntercode child = childs.get(j);
                    VariableBinding binding = child.getVariable(variable);

                    if(j == i || !(child instanceof SqlRecursive) || binding == null || binding.canBeNull()
                            || child.getVariable(other) != null || !SqlShared.canShare(request, scope, child))
                        continue;

                    SqlIntercode shared = SqlShared.share(child);

                    List<SqlIntercode> result = new ArrayList<>(childs);
                    result.set(j, shared);
                    result.add(SqlShared.seed(request, scope, shared, variable, evalServices));

                    return result;
                }
            }
        }

        return childs;
    }


    /**
     * True if the database estimates fewer distinct values of the end variable in the seeds of the end than of the
     * begin variable in the seeds of the begin (see {@link Estimator}); false on a tie, when an estimate fails, or when
     * seeds refer to the current solution of an EXISTS or to a lateral join, which cannot be explained on their own.
     *
     * @param request the current request
     * @param scope the scope of the join
     * @param childs the child nodes
     * @param endSeeds indexes of the seeds of the end
     * @param endVar the end variable
     * @param beginSeeds indexes of the seeds of the begin
     * @param beginVar the begin variable
     * @return true if the seeds of the end are estimated to have the fewer distinct values, false otherwise
     */
    private static boolean fewerSeeds(Request request, Scope scope, List<SqlIntercode> childs, List<Integer> endSeeds,
            Variable endVar, List<Integer> beginSeeds, Variable beginVar)
    {
        SqlIntercode endJoin = join(request, endSeeds.stream().map(childs::get).toList());
        SqlIntercode beginJoin = join(request, beginSeeds.stream().map(childs::get).toList());

        for(String sql : List.of(endJoin.translate(request, scope), beginJoin.translate(request, scope)))
            if(sql.contains("\"lateral") || scope.refersToEnclosingInput(sql))
                return false;

        Estimator estimator = new Estimator(request);

        long end = estimator.estimateDistinct(request, scope, endJoin, Set.of(endVar));
        long begin = estimator.estimateDistinct(request, scope, beginJoin, Set.of(beginVar));

        if(end == Long.MAX_VALUE || begin == Long.MAX_VALUE)
            return false;

        return end < begin;
    }


    /**
     * How many rows the seeds are expected to have, roughly, without asking the database: 0 when one of them is inline
     * data, 1 when one of them is a table access restricted by a constant, {@link #wholeTableRank} otherwise (whole
     * tables, or anything else); the distinct values read from shared solutions (see {@link SqlShared#seed}) count as
     * the solutions they are read from. Joined together, the seeds have no more rows than the smallest of them.
     *
     * @param childs the child nodes
     * @param seeds indexes of the seeds
     * @return the rank of the seeds, the lower the fewer rows they are expected to have
     */
    private static int seedRank(List<SqlIntercode> childs, List<Integer> seeds)
    {
        int rank = wholeTableRank;

        for(int i : seeds)
        {
            SqlIntercode seed = childs.get(i);

            while(seed instanceof SqlShared || seed instanceof SqlDistinct)
                seed = seed instanceof SqlShared shared ? shared.getChild() : ((SqlDistinct) seed).getChild();

            if(seed instanceof SqlValues)
                return 0;

            if(seed instanceof SqlTableAccess access && access.getConditions().getAreEqual().stream()
                    .anyMatch(c -> c.getLeft() instanceof ConstantColumn || c.getRight() instanceof ConstantColumn))
                rank = 1;
        }

        return rank;
    }


    /**
     * Indexes of the children that can seed the walk at the given index from the start variable: those binding it
     * always and distinct solutions over it, and not binding the end variable. A child binding the graph the walk
     * carries always needs distinct solutions over the start and the graph only, as the recursion keeps the solutions
     * of different graphs apart.
     *
     * @param request the current request
     * @param childs the child nodes
     * @param index index of the walk
     * @param startVar the variable the walk starts from
     * @param endVar the variable the walk ends in
     * @param graphVar the graph variable the walk carries, or null
     * @return indexes of the children that can seed the walk from the start variable
     */
    private static List<Integer> selectSeeds(Request request, List<SqlIntercode> childs, int index, Variable startVar,
            Variable endVar, Variable graphVar)
    {
        List<Integer> seeds = new ArrayList<>();

        for(int i = 0; i < childs.size(); i++)
        {
            SqlIntercode child = childs.get(i);
            VariableBinding binding = child.getVariable(startVar);

            if(i == index || binding == null || binding.canBeNull() || child.getVariable(endVar) != null)
                continue;

            VariableBinding graph = graphVar != null ? child.getVariable(graphVar) : null;

            if(child.isDistinct(request, Set.of(startVar))
                    || (graph != null && !graph.canBeNull() && child.isDistinct(request, Set.of(startVar, graphVar))))
                seeds.add(i);
        }

        return seeds;
    }


    /**
     * Whether the child at the index shares a variable with another child.
     *
     * @param childs the child nodes
     * @param index index of the child
     * @return whether the child at the index shares a variable with another child
     */
    private static boolean sharesVariable(List<SqlIntercode> childs, int index)
    {
        Set<Variable> variables = childs.get(index).getVariableBindings().getVariables();

        for(int i = 0; i < childs.size(); i++)
            if(i != index && !Collections.disjoint(variables, childs.get(i).getVariableBindings().getVariables()))
                return true;

        return false;
    }


    /**
     * Alternatives of joined components denoted by the child: the branches of a union, the expansion of a join, or the
     * child itself.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param child the child node
     * @return alternatives of joined components denoted by the child: the branches of a union, the expansion of a join,
     *         or the child itself
     */
    private static List<List<SqlIntercode>> expand(ClassRelations relations, SqlIntercode child)
    {
        if(child instanceof SqlUnion union)
            return expandUnion(relations, union.getChilds());
        else if(child instanceof SqlJoin join)
            return expandJoin(relations, join.getChilds());
        else
            return List.of(List.of(child));
    }


    /**
     * Alternatives of joined components of a join of the children (cartesian combination of the children's
     * alternatives), dropping unjoinable ones.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param childs the child nodes
     * @return alternatives of joined components of a join of the children (cartesian combination of the children's
     *         alternatives), dropping unjoinable ones
     */
    private static List<List<SqlIntercode>> expandJoin(ClassRelations relations, List<SqlIntercode> childs)
    {
        List<List<SqlIntercode>> result = List.of(List.of());

        for(SqlIntercode child : childs)
        {
            if(child.equals(SqlNoSolution.get()))
                return List.of();

            if(child.equals(SqlEmptySolution.get()))
                continue;

            List<List<SqlIntercode>> subresult = new ArrayList<>();

            for(List<SqlIntercode> e : expand(relations, child))
            {
                for(List<SqlIntercode> r : result)
                {
                    List<SqlIntercode> merged = new ArrayList<>();
                    merged.addAll(e);
                    merged.addAll(r);

                    if(isJoinable(relations, merged))
                        subresult.add(merged);
                }
            }

            result = subresult;
        }

        return result;
    }


    /**
     * Alternatives of joined components of a union of the children.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param childs the child nodes
     * @return alternatives of joined components of a union of the children
     */
    private static List<List<SqlIntercode>> expandUnion(ClassRelations relations, List<SqlIntercode> childs)
    {
        List<List<SqlIntercode>> result = new ArrayList<>();

        for(SqlIntercode child : childs)
            if(!child.equals(SqlNoSolution.get()))
                result.addAll(expand(relations, child));

        return result;
    }


    /**
     * The joined children.
     *
     * @return the joined children
     */
    public final List<SqlIntercode> getChilds()
    {
        return childs;
    }


    @Override
    protected boolean isRestricted(Variable variable, SqlExpressionIntercode value)
    {
        //NOTE: the join makes the other children agree with the one that binds the variable to the value
        return childs.stream().anyMatch(child -> child.isRestricted(variable, value));
    }


    @Override
    public boolean hasServiceSubpattern()
    {
        return childs.stream().anyMatch(c -> c.hasServiceSubpattern());
    }


    @Override
    public Set<VirtualTable> getVirtualTables()
    {
        return getVirtualTables(childs);
    }


    @Override
    public String getCollation(Request request, Variable variable, ResourceClass resClass, int index)
    {
        return getCollation(request, childs, variable, resClass, index);
    }


    @Override
    public void generateExplanation(StringBuilder builder, String indent)
    {
        builder.append("join");

        // the tree chosen by the optimizer, when the join has been translated with it
        if(plan != null)
            builder.append(" ").append(JoinOptimizer.describe(plan.root()));

        for(int i = 0; i < childs.size(); i++)
        {
            indentChild(builder, indent, i == childs.size() - 1);
            childs.get(i).generateExplanation(builder, getIndent(indent, i == childs.size() - 1));
        }
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(!(object instanceof SqlJoin imcode))
            return false;

        if(!super.equals(imcode))
            return false;

        if(!Objects.equals(new Multiset<>(childs), new Multiset<>(imcode.childs)))
            return false;

        return true;
    }


    @Override
    protected int getHashCode()
    {
        return Objects.hash(new Multiset<>(childs));
    }
}
