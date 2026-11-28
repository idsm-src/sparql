package cz.iocb.sparql.engine.imcode;

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
import java.util.Iterator;
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
import cz.iocb.sparql.engine.database.SqlBuilder;
import cz.iocb.sparql.engine.database.SqlCode;
import cz.iocb.sparql.engine.database.Table;
import cz.iocb.sparql.engine.imcode.expression.SqlBuiltinCall;
import cz.iocb.sparql.engine.imcode.expression.SqlExpressionIntercode;
import cz.iocb.sparql.engine.imcode.expression.SqlVariable;
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
 * tests the start of a walk against a sibling binding it without being distinct over it (see
 * {@link SqlRecursive#testStart}), evaluates nondeterministic children last, and evaluates the SERVICE calls that have
 * no context of their own (see {@link SqlServiceStub#isEvaluatedByJoin}) for the solutions of their siblings, deciding
 * by row estimates which of the shared variables the calls are asked about: a call keeps the variables whose distinct
 * combinations in the solutions of the siblings are estimated to fit into the context limit of its endpoint (see
 * {@link SqlServiceStub#getServiceSettings}), leaving out the variables of the most distinct values first.
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
     * Per variable, the conditions the filters above keep the solutions of the join by (see
     * {@link Restrictions#getFilterConditions}), which the optimizer chooses the join tree with.
     */
    private final Map<Variable, Set<SqlExpressionIntercode>> filterConditions;

    /**
     * The number of the solutions of the join that the parent reads at most (see {@link Restrictions#getNeededRows}),
     * which the optimizer chooses the join tree for.
     */
    private final double neededRows;

    /**
     * The join tree chosen by the optimizer (see {@link #getPlan}), once computed.
     */
    private JoinOptimizer.Plan plan;

    /**
     * The rows the join tree {@link #rowPlan} was chosen for, once it has been chosen.
     */
    private JoinOptimizer.Outer rowOuter;

    /**
     * The join tree chosen by the optimizer for the join evaluated for one of the rows {@link #rowOuter} (see
     * {@link #getPlan(Request, JoinOptimizer.Outer)}).
     */
    private JoinOptimizer.Plan rowPlan;


    /**
     * Creates the node.
     *
     * @param childs the child nodes
     * @param tables the tables
     * @param bindings the variable bindings
     * @param columnMap the column map
     * @param filterConditions per variable, the conditions the filters above keep the solutions by
     * @param neededRows the number of the solutions of the join that the parent reads at most
     */
    protected SqlJoin(List<SqlIntercode> childs, List<AliasTable> tables, VariableBindings bindings,
            Map<Column, Column> columnMap, Map<Variable, Set<SqlExpressionIntercode>> filterConditions,
            double neededRows)
    {
        super(bindings, childs.stream().allMatch(c -> c.isDeterministic()));

        this.childs = childs;
        this.columnMap = columnMap;
        this.tables = tables;
        this.filterConditions = filterConditions;
        this.neededRows = neededRows;
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

        return new SqlJoin(childs, tables, bindings, columnMap, getFilterConditions(childs, restrictions),
                restrictions == null ? Double.POSITIVE_INFINITY : restrictions.getNeededRows());
    }


    /**
     * The conditions the filters above keep the solutions of a join by, per variable the children bind (see
     * {@link Restrictions#getFilterConditions}).
     *
     * @param childs the child nodes
     * @param restrictions what the parent needs of the variables, or null
     * @return per variable, the conditions the filters above keep the solutions of the join by
     */
    private static Map<Variable, Set<SqlExpressionIntercode>> getFilterConditions(List<SqlIntercode> childs,
            Restrictions restrictions)
    {
        Map<Variable, Set<SqlExpressionIntercode>> conditions = new HashMap<>();

        if(restrictions == null)
            return conditions;

        for(SqlIntercode child : childs)
            for(Variable variable : child.getVariableBindings().getVariables())
                if(!restrictions.getFilterConditions(variable).isEmpty())
                    conditions.put(variable, restrictions.getFilterConditions(variable));

        return conditions;
    }


    @Override
    public SqlIntercode optimize(Request request, Scope scope, Restrictions restrictions, boolean reduced,
            boolean evalServices)
    {
        ClassRelations relations = request.getConfiguration();

        // a SERVICE call evaluated by its join (see SqlServiceStub.isEvaluatedByJoin) is evaluated for the solutions of
        // its siblings, which a statement of its own reads, so they must not take in the values of a current solution
        // of an EXISTS pattern
        if(request.isServiceReorderEnabled() && childs.stream().anyMatch(SqlIntercode::hasServiceSubpattern)
                || flatChilds(childs).stream().anyMatch(c -> isEvaluatedByJoin(request, c)))
        {
            restrictions = new Restrictions(restrictions);
            restrictions.removeCurrentValues();
        }

        List<SqlIntercode> flatChilds = flatChilds(optimize(request, scope, flatChilds(childs), restrictions, reduced));

        boolean hasService = false;

        if(evalServices)
        {
            List<SqlIntercode> tmp = new ArrayList<>();

            for(SqlIntercode l : flatChilds)
            {
                if(l.hasServiceSubpattern() && !isEvaluatedByJoin(request, l))
                    tmp.add(l.optimize(request, scope, getRestrictions(relations, l, flatChilds, restrictions), reduced,
                            true));
                else
                    tmp.add(l);

                if(isEvaluatedByJoin(request, l))
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

        // the children could swap their presentations of a shared variable in every round
        Settlement settlement = new Settlement();

        while(true)
        {
            List<SqlIntercode> optimized = optimize(request, scope, optChilds, newRestrictions, reduced, evalServices,
                    settlement.getClasses());
            List<SqlIntercode> newChilds = reduceDistinctUnion(request,
                    reduceJoin(request, optimized, newRestrictions));

            if(reduced)
                newChilds = reduceSubsumed(newChilds);

            newChilds = expandLeadingOptionals(request, scope, newChilds, evalServices);
            newChilds = seedWalks(request, scope, newChilds, newRestrictions);
            newChilds = shareSeeds(request, scope, newChilds, evalServices);
            newChilds = testWalkStarts(request, scope, newChilds, newRestrictions);

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

            // the siblings a statement of its own can read: a sibling referring to the current solution of an EXISTS
            // pattern the join lies in has a meaning only at its place, so the calls are not evaluated for it
            List<SqlIntercode> readable = optChilds.stream()
                    .filter(c -> !scope.refersToEnclosingInput(c.translate(request, scope))).toList();


            for(SqlIntercode item : nondeterministic)
            {
                if(!(item instanceof SqlServiceStub service) || !service.isEvaluatedByJoin(request))
                    continue;

                Variable endpoint = service.getEndpointVariable();

                Set<Variable> vars = new HashSet<>(service.getServiceVariables().values());

                if(endpoint != null)
                    vars.add(endpoint);

                Restrictions serviceRestrictions = new Restrictions(vars);

                SqlIntercode join = join(request, readable).optimize(request, scope, serviceRestrictions, false, false);

                // a call whose endpoint no sibling names waits for the calls whose solutions may name it
                if(endpoint != null && join.getVariable(endpoint) == null)
                    continue;

                Map<Variable, Long> basic = new HashMap<>();

                for(Variable var : vars)
                {
                    if(!var.equals(endpoint) && join.bindings.getVariables().contains(var))
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

                // every context solution has to name the endpoint, so the variable naming it is never left out
                Set<Variable> set = new HashSet<>(order);

                if(endpoint != null)
                    set.add(endpoint);

                // the call keeps the variables as long as the endpoint takes the estimated context solutions
                long limit = service.getServiceSettings(request.getConfiguration()).contextLimit();
                long count = estimator.estimateDistinct(request, scope, join, set);

                while(count > limit && !order.isEmpty())
                {
                    set.remove(order.pop());
                    count = estimator.estimateDistinct(request, scope, join, set);
                }

                // the variables of the pattern that no sibling binds are not asked about by any call, so only the
                // shared variables left out count
                stats.put(service, new Stats(set, count, basic.size() - order.size()));
            }

            // no sibling names the endpoint of any call left, so none of them is asked
            if(stats.isEmpty())
                return SqlNoSolution.get();

            Entry<SqlServiceStub, Stats> entry = stats
                    .entrySet().stream().sorted(Map.Entry.<SqlServiceStub, Stats> comparingByValue((a,
                            b) -> a.lost == b.lost ? Long.compare(a.count, b.count) : Integer.compare(a.lost, b.lost)))
                    .findFirst().get();

            SqlServiceStub service = entry.getKey();

            Set<Variable> vars = entry.getValue().vars;
            SqlIntercode distinct = SqlDistinct.create(request, join(request, readable), vars).optimize(request, scope,
                    new Restrictions(vars), false, false);

            Restrictions serviceRestrictions = getRestrictions(relations, service, flatChilds, restrictions);

            optChilds = new ArrayList<>(optChilds);

            /* NOTE: The received solutions are merged with the context solutions they were asked for, and the join
             * takes them back for the solutions of the siblings that the context solution stands for. Where the context
             * binds its variables always, those are the solutions agreeing with it in them, which the join compares
             * anyway. A variable that a solution may leave unbound agrees with any value, so a solution of the siblings
             * would take also the solutions received for the other context solutions: those asked with the variable
             * bound when it leaves the variable unbound, and the other way round. The siblings and the context
             * therefore tell, by a variable of their own that is always bound, whether they bind such a variable, which
             * the join compares as well.
             */
            List<Variable> unbound = vars.stream()
                    .filter(v -> distinct.getVariable(v) != null && distinct.getVariable(v).canBeNull())
                    .sorted(Comparator.comparing(Variable::getName)).toList();

            SqlIntercode context = distinct;

            if(!unbound.isEmpty())
            {
                SqlIntercode siblings = readable.size() == 1 ? readable.get(0) : join(request, readable);
                Set<Variable> keys = new HashSet<>(vars);

                for(Variable variable : unbound)
                {
                    Variable flag = service.getBoundFlag(variable);
                    SqlExpressionIntercode bound = SqlBuiltinCall.create(request, "bound", false,
                            List.of(SqlVariable.create(relations, siblings.getVariable(variable))));

                    siblings = SqlBind.bind(request, flag, bound, siblings);
                    keys.add(flag);
                    serviceRestrictions.add(flag);
                }

                context = SqlDistinct.create(request, siblings, keys).optimize(request, scope, new Restrictions(keys),
                        false, false);

                optChilds.removeAll(readable);
                optChilds.add(siblings);
            }

            SqlIntercode evaluated = service.eval(request, scope, context, serviceRestrictions);


            nondeterministic.remove(service);

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


        boolean unchanged = fullChilds.equals(childs) && restrictions.isOptimized(relations, bindings)
                && filterConditions.equals(getFilterConditions(fullChilds, restrictions))
                && neededRows == restrictions.getNeededRows();

        if(unchanged && restrictions.getNames().stream()
                .allMatch(v -> bindings.get(v) != null || fullChilds.stream().allMatch(c -> c.getVariable(v) == null)))
            return this;

        // a join that exposes less than the children bind of what the parent has come to need, like a node joining the
        // tables of a join mapping that is asked for in place of the term it stands for, is made anew
        SqlIntercode join = join(request, fullChilds, restrictions);

        return unchanged && join.getVariableBindings().equals(bindings) ? this : join;
    }


    /**
     * The join with the condition taken in by every child holding a SERVICE call whose solutions give the deciding
     * variables of the condition the values the solutions of the join give them. A call that is a child of the join and
     * is evaluated by it (see {@link SqlServiceStub#isEvaluatedByJoin}) is evaluated for the solutions of its
     * deterministic siblings (see {@link #optimize}), so with such a call the siblings are filtered by the condition
     * together, as the context the call is going to get, when they bind a deciding variable and give all of them their
     * values. They are not inside the pattern of an expression, where the call is evaluated only for the siblings that
     * do not refer to the current solution of the pattern, which filtering them together would hide.
     */
    @Override
    protected SqlIntercode sink(Request request, Scope scope, SqlExpressionIntercode condition,
            VariableBindings bindings)
    {
        Set<Variable> variables = getDecidingVariables(request.getConfiguration(), condition);

        List<SqlIntercode> members = new ArrayList<>(childs);
        boolean taken = false;

        for(int i = 0; i < childs.size(); i++)
        {
            int index = i;
            SqlIntercode child = childs.get(i);

            List<VariableBindings> others = IntStream.range(0, childs.size()).filter(j -> j != index)
                    .mapToObj(j -> childs.get(j).getVariableBindings()).toList();

            if(!child.hasServiceSubpattern() || !keepsValues(variables, child.getVariableBindings(), others))
                continue;

            SqlIntercode sunk = child.sink(request, scope, condition, bindings);

            if(sunk != null)
            {
                members.set(i, sunk);
                taken = true;
            }
        }

        List<SqlIntercode> siblings = members.stream().filter(c -> c.isDeterministic()).toList();

        if(!scope.isWithinExpression() && childs.stream().anyMatch(c -> isEvaluatedByJoin(request, c))
                && !siblings.isEmpty())
        {
            List<SqlIntercode> rest = members.stream().filter(c -> !c.isDeterministic()).toList();
            SqlIntercode context = siblings.size() == 1 ? siblings.get(0) : join(request, siblings);

            SqlIntercode restricted = null;

            if(variables.stream().anyMatch(v -> context.getVariable(v) != null) && keepsValues(variables,
                    context.getVariableBindings(), rest.stream().map(c -> c.getVariableBindings()).toList()))
                restricted = filterBy(request, scope, condition, bindings, context);

            if(restricted != null)
            {
                members = new ArrayList<>(rest);
                members.add(restricted);
                taken = true;
            }
        }

        return taken ? join(request, members) : null;
    }


    /**
     * True if the node is a SERVICE call that the join holding it evaluates for the solutions of its siblings (see
     * {@link SqlServiceStub#isEvaluatedByJoin}).
     *
     * @param request the current request
     * @param node the node
     * @return true if the node is a SERVICE call evaluated by the join holding it, false otherwise
     */
    private static boolean isEvaluatedByJoin(Request request, SqlIntercode node)
    {
        return node instanceof SqlServiceStub stub && stub.isEvaluatedByJoin(request);
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
     * @param request the current request
     * @param childs the child nodes
     * @param restrictions what the parent needs of the variables
     * @return the simplified children
     */
    private static List<SqlIntercode> reduceJoin(Request request, List<SqlIntercode> childs, Restrictions restrictions)
    {
        ClassRelations relations = request.getConfiguration();
        DatabaseSchema schema = request.getConfiguration().getDatabaseSchema();

        if(childs.stream().anyMatch(c -> c.equals(SqlNoSolution.get())))
            return List.of(SqlNoSolution.get());

        childs = childs.stream().filter(c -> !c.equals(SqlEmptySolution.get())).toList();

        if(childs.size() == 0)
            return List.of(SqlEmptySolution.get());

        if(!isJoinable(relations, childs))
            return List.of(SqlNoSolution.get());


        Map<Variable, List<VariableBinding>> constants = new HashMap<>();

        // a variable that a solution may leave unbound agrees with any value, so only a constant bound always restricts
        // the other children, and only those binding the variable always (see below)
        for(SqlIntercode child : childs)
            for(VariableBinding v : child.getVariableBindings().getValues())
                if(v.isConstant() && !v.canBeNull()) // children are joinable, so constants should be consistent
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

                        if(v == null || v.isConstant() || v.canBeNull())
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
                        SqlIntercode merged = SqlTableAccess.tryReduceJoinWithValues(request, right, left, null);

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
                        merged = SqlTableAccess.tryReduceJoin(request, left, right, null);
                    }
                    else if(optChilds.get(j) instanceof SqlDistinct d && d.getChild() instanceof SqlTableAccess right)
                    {
                        merged = tryReduceDistinct(request, left, right);
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
     * @param request the current request
     * @param childs the child nodes
     * @return the simplified children
     */
    private static List<SqlIntercode> reduceDistinctUnion(Request request, List<SqlIntercode> childs)
    {
        ClassRelations relations = request.getConfiguration();
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

                        SqlIntercode intercode = tryReduceDistinct(request, candidate, distinctPart);

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
     * Leaves out the children that only repeat what another child gives, where duplicate solutions do not matter (an
     * elimination inspired by the self-join elimination of Ontop over the same terms in a cardinality-insensitive
     * context): an access whose rows are all rows of another access to the same table, giving the same values (see
     * {@link SqlTableAccess#isSubsumedBy}), and a union that has such an access among its branches and binds no
     * variable the other access does not bind. For every solution of the other access, the row it is made of gives the
     * child left out a solution agreeing with it, and every solution of the child agrees with it in all its variables,
     * so the join with the child gives the solutions of the other access only, some of them several times. No key of
     * the table is needed for that, unlike for a merge of the two accesses (see {@link SqlTableAccess#tryReduceJoin}).
     *
     * @param childs the children of a join whose duplicate solutions do not matter
     * @return the children without the ones that only repeat what another child gives
     */
    private static List<SqlIntercode> reduceSubsumed(List<SqlIntercode> childs)
    {
        List<SqlIntercode> result = new ArrayList<>(childs);

        loop:
        for(int i = 0; i < result.size(); i++)
        {
            SqlIntercode child = result.get(i);

            for(int j = 0; j < result.size(); j++)
            {
                if(i != j && result.get(j) instanceof SqlTableAccess other && isSubsumedBy(child, other))
                {
                    result.remove(i);
                    i--;
                    continue loop;
                }
            }
        }

        return result;
    }


    /**
     * True if the child only repeats what the access gives where duplicate solutions do not matter (see
     * {@link #reduceSubsumed}).
     *
     * @param child the child
     * @param access the access
     * @return true if the child only repeats what the access gives
     */
    private static boolean isSubsumedBy(SqlIntercode child, SqlTableAccess access)
    {
        if(child instanceof SqlTableAccess other)
            return other.isSubsumedBy(access);

        if(!(child instanceof SqlUnion union)
                || !access.getVariableBindings().getVariables().containsAll(union.getVariableBindings().getVariables()))
            return false;

        return union.getChilds().stream()
                .anyMatch(b -> b instanceof SqlTableAccess branch && branch.isSubsumedBy(access));
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
     * @param request the current request
     * @param candidate the access joined with the distinct pattern
     * @param distinct the deduplicated access
     * @return the merged access, or null
     */
    private static SqlIntercode tryReduceDistinct(Request request, SqlTableAccess candidate, SqlTableAccess distinct)
    {
        ClassRelations relations = request.getConfiguration();
        DatabaseSchema schema = request.getConfiguration().getDatabaseSchema();

        if(Objects.equals(distinct.getTable(), candidate.getTable()))
        {
            // the distinct part denotes a set over all the columns it binds
            Set<Column> distinctColumns = distinct.getInternalVariableBindings().getNonConstantColumns();

            if(!SqlTableAccess.canBeJoinedByDistinctColumns(relations, candidate, distinct, distinctColumns))
                return null;

            return SqlTableAccess.joinByDistinctColumns(request, candidate, distinct, null);
        }
        else
        {
            // expressions are not moved onto the columns of the other table
            if(distinct.hasExpression())
                return null;

            Set<ColumnPair> columns = SqlTableAccess.getJoinColumnPairs(relations, distinct, candidate);

            // the conditions and the values the distinct part is restricted to are moved onto the columns of the
            // candidate that reference theirs
            Set<Column> parentColumns = new HashSet<>();
            parentColumns.addAll(distinct.getConditions().getNonConstantColumns());
            parentColumns.addAll(distinct.getInternalVariableBindings().getNonConstantColumns());
            parentColumns.addAll(distinct.getValueColumns(false));

            if(!columns.stream().map(p -> p.getLeft()).collect(toSet()).containsAll(parentColumns))
                return null;

            List<Set<ColumnPair>> keys = schema.getForeignKeys(distinct.getTable(), candidate.getTable());

            // the distinct part is dropped, which requires that each candidate row references a distinct row; a
            // foreign key matched simply guarantees that only for the rows with no null in any of its child columns,
            // also those not joined
            for(Set<ColumnPair> key : keys)
                if(key.containsAll(columns)
                        && key.stream().allMatch(p -> candidate.isKnownNotNull(schema, p.getRight())))
                    return SqlTableAccess.joinByForeignKey(request, distinct, candidate, columns, null);
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
            plan = JoinOptimizer.plan(request, childs, filterConditions, null, neededRows);

        return plan;
    }


    /**
     * The join tree chosen by the optimizer for the join evaluated for the rows of an outer table one by one (see
     * {@link #translateForRow}), where the children are restricted to the solutions compatible with a row; kept for the
     * rows it was chosen for last.
     *
     * @param request the current request
     * @param outer the rows the join is evaluated for one by one, or null when it is evaluated once
     * @return the join tree chosen by the optimizer for the children
     */
    JoinOptimizer.Plan getPlan(Request request, JoinOptimizer.Outer outer)
    {
        if(outer == null)
            return getPlan(request);

        if(!outer.equals(rowOuter))
        {
            rowPlan = JoinOptimizer.plan(request, childs, filterConditions, outer, Double.POSITIVE_INFINITY);
            rowOuter = outer;
        }

        return rowPlan;
    }


    @Override
    public SqlCode translate(Request request, Scope scope)
    {
        return translateJoin(request, scope, null);
    }


    /**
     * The join of the children evaluated for the row of the outer table, in the tree the optimizer chooses for them so
     * evaluated: a solution of the join is made of a solution of every child, so it is compatible with the row only if
     * each of them is.
     */
    @Override
    SqlCode translateForRow(Request request, Scope scope, JoinOptimizer.Outer outer)
    {
        return translateJoin(request, scope, outer);
    }


    /**
     * SQL code of the join, evaluated once or for one of the rows of an outer table (see {@link #translateForRow}).
     *
     * @param request the current request
     * @param scope the scope of the join
     * @param outer the rows the join is evaluated for one by one, or null when it is evaluated once
     * @return SQL code of the join
     */
    private SqlCode translateJoin(Request request, Scope scope, JoinOptimizer.Outer outer)
    {
        ClassRelations relations = request.getConfiguration();

        Set<Column> columns = bindings.getNonConstantColumns();

        SqlBuilder builder = new SqlBuilder();

        builder.append("SELECT ");

        if(!columns.isEmpty())
            builder.append(columns.stream()
                    .map(c -> columnMap.get(c) != null ? SqlCode.of(columnMap.get(c), " AS ", c) : c.getCode())
                    .collect(SqlCode.joining(", ")));
        else
            builder.append("1");

        builder.append(" FROM ");

        // a join of more children than the optimizer chooses the tree of is left to the planner (see JoinOptimizer)
        if(request.getConfiguration().isJoinOptimizerEnabled() && childs.size() > 1
                && getPlan(request, outer).root() != null)
        {
            Map<Variable, Set<ResourceClass>> joined = getJoinedClasses(relations,
                    childs.stream().map(c -> c.getVariableBindings()).toList());

            JoinOptimizer.Plan plan = getPlan(request, outer);

            translateTree(request, scope, plan.root(), plan, joined, outer, new HashMap<>(), builder);
            return builder.toCode();
        }

        for(int i = 0; i < childs.size(); i++)
        {
            if(i > 0)
                builder.append(", ");

            builder.append("(");
            builder.append(translateChild(request, scope, childs.get(i), outer));
            builder.append(") AS ");
            builder.append(tables.get(i));
        }

        SqlCode condition = generateJoinCondition(relations, childs.stream().map(c -> c.getVariableBindings()).toList(),
                tables);

        if(condition != null)
        {
            builder.append(" WHERE ");
            builder.append(condition);
        }

        return builder.toCode();
    }


    /**
     * Writes the join tree chosen by the optimizer: a child as its subquery, an index lookup as a lateral subquery of
     * the access restricted by its equalities with the children of the outer tree (see
     * {@link SqlTableAccess#translateLateral}), and a hash join as an explicit join of its sides on the equalities
     * between their children (a cross join when there are none), so that the planner keeps the tree (see
     * {@link cz.iocb.sparql.engine.request.Request#execute}). The equalities compare only the classes of a variable
     * that meet a class it takes in the whole join (see {@link #getJoinedClasses}). An equality of two variables that
     * the filters above the join require and the children are joined on (see {@link JoinOptimizer.Equality}) is
     * compared where its children meet, like a variable they share: in the condition of their hash join, or in the
     * restriction of the access looked up. When the join is evaluated for the rows of an outer table one by one, every
     * child is (see {@link #translateForRow}), a looked up access being restricted by the values of the row as well.
     * The access that a tree chosen for the first solutions of the join starts with (see
     * {@link JoinOptimizer#planFirstRows}) is fenced by {@code OFFSET 0}: the planner then chooses how to read it by
     * all the rows of the access, whereas it takes the rows meeting the conditions of an access it reads the first rows
     * of to lie evenly in the table and scans the table for them, also where they all lie at its end, like the triples
     * of a predicate in a table ordered by the predicate.
     * <p>
     * Where the database may evaluate the statement by parallel workers (see {@link Request#isParallel}), or the join
     * is written in place for the rows of an outer table (see {@link JoinOptimizer.Outer#inPlace}), an access that can
     * be looked up in place is looked up so (see {@link SqlTableAccess#translateInPlace}): the database merges it into
     * the join and looks it up by a nested loop it can hand to the workers, whereas it evaluates a fenced subquery
     * reading the outer tree only in the process reading the tree. The comparisons of such a lookup are no equalities
     * to the database, but the database would take the equalities that a hash join above compares a variable of the
     * access and of another child of its side by, both with a third child, for members of one class of equal values,
     * and derive from it an equality of the access and the other child, by which it would hash them instead of looking
     * the access up. So a hash join leaves the variables that such a lookup compares strictly out of the comparisons of
     * the access: the child of the outer tree it compares them with is on the same side of the hash join and holds the
     * same values, and the hash join compares that child (or, where that one is looked up in place by the variable as
     * well, the child it is looked up by). Such a lookup compares a variable with one source of its values only, and an
     * access the database could look up only by the values of several sources is fenced as before (see
     * {@link #getLeftOut}).
     * <p>
     * Where plan advice makes the planner keep the joins (see {@link Request#isAdvised}), an access is looked up in
     * place likewise, but compared by plain equalities, and the advice makes the planner look it up by a nested loop
     * (see {@link cz.iocb.sparql.engine.database.PlanAdvice}), which keeps the rows found for the values looked up
     * where the tree says the values repeat (see {@link JoinOptimizer.Plan#memoized}). An access looked up by the
     * values of several sources stays fenced then too: the database looks such an access up by the values of a part of
     * the sources wherever it takes the other comparisons for cheap filters. So does an access of a join evaluated for
     * every row of an outer table, like the pattern of a test (see {@link #translateForRow}), or lying in a pattern of
     * an expression (see {@link Scope#isWithinExpression}): the database evaluates such a join for every row anew, and
     * in place it reads the rows of the access by its constants alone wherever it takes them for few, whereas a fenced
     * subquery looks them up by all the values.
     *
     * @param request the current request
     * @param scope the scope of the join
     * @param node the tree
     * @param plan the plan the tree belongs to
     * @param joined classes per variable in the whole join (see {@link #getJoinedClasses})
     * @param outer the rows the join is evaluated for one by one, or null when it is evaluated once
     * @param replaced the variables of each child looked up in place that its lookup compares with a child of the outer
     *            tree giving them a value that is never unbound (see {@link #getLeftOut}), which the hash joins above
     *            compare by the other child; filled in as the lookups of the tree are written
     * @param builder the SQL code being built
     * @return the children of the tree, as a bit per child
     */
    private long translateTree(Request request, Scope scope, JoinOptimizer.Node node, JoinOptimizer.Plan plan,
            Map<Variable, Set<ResourceClass>> joined, JoinOptimizer.Outer outer, Map<Integer, Set<Variable>> replaced,
            SqlBuilder builder)
    {
        ClassRelations relations = request.getConfiguration();
        List<JoinOptimizer.Equality> equalities = plan.equalities();

        if(node instanceof JoinOptimizer.Leaf leaf)
        {
            builder.append("(");
            builder.append(translateChild(request, scope, childs.get(leaf.child()), outer));

            if(leaf.child() == plan.first())
                builder.append(" OFFSET 0");

            builder.append(") AS ");
            builder.append(tables.get(leaf.child()));

            return 1L << leaf.child();
        }

        if(node instanceof JoinOptimizer.Lookup lookup)
        {
            long outerChilds = translateTree(request, scope, lookup.outer(), plan, joined, outer, replaced, builder);
            SqlTableAccess access = (SqlTableAccess) childs.get(lookup.child());

            // the values the access is looked up by: those of the children of the outer tree, those an outer child
            // gives a variable that a filter requires to be the variable of the access, and those of the row the join
            // is evaluated for
            List<LookupSource> sources = new ArrayList<>();

            for(int i = 0; i < childs.size(); i++)
                if((outerChilds >> i & 1L) != 0)
                    sources.add(new LookupSource(i, childs.get(i).getVariableBindings(), tables.get(i), joined));

            for(JoinOptimizer.Equality equality : equalities)
            {
                int other = equality.left() == lookup.child() ? equality.right() :
                        equality.right() == lookup.child() ? equality.left() : -1;

                if(other == -1 || (outerChilds >> other & 1L) == 0)
                    continue;

                VariableBindings value = new VariableBindings();
                value.add(equality.getRenamed(childs, other));
                sources.add(new LookupSource(-1, value, tables.get(other), Map.of()));
            }

            if(outer != null)
            {
                VariableBindings row = new VariableBindings();

                for(Variable variable : access.getRowVariables(relations, outer))
                    row.add(outer.bindings().get(variable));

                sources.add(new LookupSource(-1, row, outer.table(), Map.of()));
            }

            // the access is looked up in place where plan advice makes the planner look it up, in a join evaluated once
            // outside a pattern of an expression, and where the database may hand the nested loop to parallel workers,
            // unless it is looked up by the values of several sources (see getLeftOut)
            boolean advised = request.isAdvised() && outer == null && !scope.isWithinExpression();
            Map<Integer, Set<Variable>> leftOut = (advised || (outer == null ? request.isParallel() : outer.inPlace()))
                    && access.canBeLookedUpInPlace() ? getLeftOut(relations, access, sources) : null;
            boolean inPlace = leftOut != null;

            // in place, the values are compared by conditions the database cannot hash, unless the advice makes the
            // planner look the access up
            boolean unhashable = inPlace && !request.isAdvised();

            List<SqlCode> conditions = new ArrayList<>();

            // the columns of the access the conditions compare strictly, whose null tests the access leaves out
            Set<Column> compared = new HashSet<>();

            for(int s = 0; s < sources.size(); s++)
            {
                LookupSource source = sources.get(s);
                VariableBindings values = inPlace ? withoutVariables(source.bindings(), leftOut.get(s)) :
                        source.bindings();

                SqlCode condition = access.getLookupCondition(relations, values, source.table(), source.joined(),
                        unhashable);

                if(condition != null)
                {
                    conditions.add(condition);
                    compared.addAll(access.getLookupComparedColumns(relations, values));
                }

                // the variables the access is compared by with the child giving them a value that is never unbound,
                // which the hash joins above compare instead of the access
                if(unhashable && source.child() >= 0)
                    for(VariableBinding value : values.getValues())
                        if(!value.canBeNull() && access.getVariable(value.getVariable()) != null)
                            replaced.computeIfAbsent(lookup.child(), _ -> new HashSet<>()).add(value.getVariable());
            }

            SqlCode condition = conditions.isEmpty() ? SqlCode.TRUE :
                    conditions.stream().collect(SqlCode.joining(" AND ", "(", ")"));

            builder.append(" CROSS JOIN LATERAL (");
            builder.append(inPlace ?
                    access.translateInPlace(request, condition, compared,
                            (plan.memoized() >> lookup.child() & 1L) != 0) :
                    access.translateLateral(request, condition, compared));
            builder.append(") AS ");
            builder.append(tables.get(lookup.child()));

            return outerChilds | 1L << lookup.child();
        }

        JoinOptimizer.HashJoin join = (JoinOptimizer.HashJoin) node;

        long left = translateTree(request, scope, join.left(), plan, joined, outer, replaced, builder);

        SqlBuilder rightBuilder = new SqlBuilder();
        long right = translateTree(request, scope, join.right(), plan, joined, outer, replaced, rightBuilder);

        List<SqlCode> conditions = new ArrayList<>();

        for(int i = 0; i < childs.size(); i++)
        {
            if((left >> i & 1L) == 0)
                continue;

            for(int j = 0; j < childs.size(); j++)
            {
                if((right >> j & 1L) == 0)
                    continue;

                SqlCode condition = generateJoinCondition(relations,
                        withoutVariables(childs.get(i).getVariableBindings(), replaced.get(i)),
                        withoutVariables(childs.get(j).getVariableBindings(), replaced.get(j)), tables.get(i),
                        tables.get(j), joined);

                if(condition != null)
                    conditions.add(condition);
            }
        }

        // the equalities of the filters above whose children the join brings together
        for(JoinOptimizer.Equality equality : equalities)
        {
            long ends = 1L << equality.left() | 1L << equality.right();

            if((left & ends) == 0 || (right & ends) == 0)
                continue;

            SqlCode condition = generateJoinCondition(relations,
                    childs.get(equality.left()).getVariable(equality.leftVariable()),
                    equality.getRenamed(childs, equality.right()), tables.get(equality.left()),
                    tables.get(equality.right()));

            if(condition != null)
                conditions.add(condition);
        }

        boolean nested = !(join.right() instanceof JoinOptimizer.Leaf);

        builder.append(conditions.isEmpty() ? " CROSS JOIN " : " JOIN ");

        if(nested)
            builder.append("(");

        builder.append(rightBuilder.toCode());

        if(nested)
            builder.append(")");

        if(!conditions.isEmpty())
        {
            builder.append(" ON ");
            builder.append(conditions.stream().collect(SqlCode.joining(" AND ", "(", ")")));
        }

        return left | right;
    }


    /**
     * SQL code of a child, evaluated once or for one of the rows of an outer table the join is evaluated for.
     *
     * @param request the current request
     * @param scope the scope of the join
     * @param child the child
     * @param outer the rows the join is evaluated for one by one, or null when it is evaluated once
     * @return SQL code of the child
     */
    private static SqlCode translateChild(Request request, Scope scope, SqlIntercode child, JoinOptimizer.Outer outer)
    {
        return outer == null ? child.translate(request, scope) : child.translateForRow(request, scope, outer);
    }


    /**
     * The bindings without those of the given variables (see {@link #translateTree}).
     *
     * @param bindings the bindings
     * @param variables the variables left out, or null for none
     * @return the bindings without those of the variables
     */
    private static VariableBindings withoutVariables(VariableBindings bindings, Set<Variable> variables)
    {
        if(variables == null || variables.isEmpty())
            return bindings;

        VariableBindings result = new VariableBindings();

        for(VariableBinding binding : bindings.getValues())
            if(!variables.contains(binding.getVariable()))
                result.add(binding);

        return result;
    }


    /**
     * A source of the values an access is looked up by (see {@link #translateTree}): a child of the outer tree, the
     * value an outer child gives a variable that a filter requires to be the variable of the access, or the row the
     * join is evaluated for.
     *
     * @param child the index of the child of the outer tree, or -1 for a value of a filter or of the row
     * @param bindings the bindings of the values, under the names of the variables of the access
     * @param table alias the columns of the values are addressed through
     * @param joined classes per variable in the whole join that the comparisons are restricted to (see
     *            {@link #getJoinedClasses})
     */
    private record LookupSource(int child, VariableBindings bindings, Table table,
            Map<Variable, Set<ResourceClass>> joined)
    {
    }


    /**
     * The comparisons that an access looked up in place leaves out (see {@link SqlTableAccess#translateInPlace}), per
     * source of the values it is looked up by, or null when the access is to be fenced as before.
     * <p>
     * A child of the outer tree compares the variables it shares with every other child of the outer tree, by the joins
     * of that tree, so where a child gives a variable a value that is never unbound, the other children hold the same
     * value of the variable or leave it unbound, and comparing the access with that child, the representative of the
     * variable, gives the solutions that comparing it with every child gives. A comparison of each of them is left out:
     * the database would take the comparisons by arrays for independent conditions, unlike equalities, which it puts
     * into one class of equal values, and multiply their selectivities, so that it would look the access up by an index
     * of fewer columns, filtering the rows of the others. Where the representative can serve the lookup, the comparison
     * of a value of a filter or of the row is left out as well, as the filter above the join or the condition of the
     * left join the row comes from compares it. A child the access can be looked up by every such variable by is the
     * representative of them all.
     * <p>
     * The database parameterises a looked up access by any subset of the tables it is looked up by, and where it takes
     * fewer rows for the lookup by one of them than there are, it looks the access up by that table only and filters
     * the rows by the others, which then cost what scanning the access for every row costs; the fenced subquery reads
     * the values of all of them as its parameters, which its index condition compares together. So an access that would
     * be looked up by the values of two or more sources stays fenced.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param access the access looked up
     * @param sources the sources of the values the access is looked up by
     * @return the variables left out of the comparisons of the access per source, by its index in the sources, or null
     *         when the access is to be fenced
     */
    private static Map<Integer, Set<Variable>> getLeftOut(ClassRelations relations, SqlTableAccess access,
            List<LookupSource> sources)
    {
        // per variable, the children giving it a value that is never unbound, and those of them the access can be
        // looked up by
        Map<Variable, List<Integer>> bound = new HashMap<>();
        Map<Variable, List<Integer>> lookedUp = new HashMap<>();

        for(int s = 0; s < sources.size(); s++)
        {
            if(sources.get(s).child() < 0)
                continue;

            for(VariableBinding value : sources.get(s).bindings().getValues())
            {
                if(access.getVariable(value.getVariable()) == null || value.canBeNull())
                    continue;

                bound.computeIfAbsent(value.getVariable(), _ -> new ArrayList<>()).add(s);

                if(access.isLookedUpBy(relations, value))
                    lookedUp.computeIfAbsent(value.getVariable(), _ -> new ArrayList<>()).add(s);
            }
        }

        // a child the access can be looked up by every variable a child can look it up by
        int single = IntStream.range(0, sources.size())
                .filter(s -> !lookedUp.isEmpty() && lookedUp.values().stream().allMatch(l -> l.contains(s))).findFirst()
                .orElse(-1);

        Map<Integer, Set<Variable>> leftOut = new HashMap<>();

        // the sources whose values the access is looked up by
        Set<Integer> lookups = new HashSet<>();

        for(Entry<Variable, List<Integer>> entry : bound.entrySet())
        {
            Variable variable = entry.getKey();
            List<Integer> candidates = lookedUp.get(variable);
            int representative = candidates == null ? entry.getValue().get(0) :
                    single != -1 ? single : candidates.get(0);

            if(candidates != null)
                lookups.add(representative);

            for(int s = 0; s < sources.size(); s++)
                if(s != representative && sources.get(s).bindings().get(variable) != null
                        && (sources.get(s).child() != -1 || candidates != null))
                    leftOut.computeIfAbsent(s, _ -> new HashSet<>()).add(variable);
        }

        for(int s = 0; s < sources.size(); s++)
            if(sources.get(s).child() == -1)
                for(VariableBinding value : sources.get(s).bindings().getValues())
                    if(!leftOut.getOrDefault(s, Set.of()).contains(value.getVariable())
                            && access.getVariable(value.getVariable()) != null && access.isLookedUpBy(relations, value))
                        lookups.add(s);

        return lookups.size() > 1 ? null : leftOut;
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

            SqlIntercode seeded = walk.seed(request, scope, seeds.stream().map(j -> childs.get(j)).toList(), fromEnd,
                    walkRestrictions);

            // the seeds would make the walk take its end in a class it could not tell the delivered nodes of apart
            if(seeded == null)
                continue;

            List<SqlIntercode> result = new ArrayList<>();

            for(int j = 0; j < childs.size(); j++)
                if(j != i && !seeds.contains(j))
                    result.add(childs.get(j));

            result.add(seeded);

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
     * Tests the start of a walk between two variables against a sibling that binds the start variable always but cannot
     * seed the walk, not being distinct over it (see {@link SqlRecursive#testStart}): the walk then starts only from
     * the nodes the sibling binds the variable to, of whose paths alone the join keeps any, and the sibling keeps its
     * place, so its solutions are not multiplied by the recursion. A sibling binding the end variable so turns the walk
     * to start from it, when the walk may be turned (see {@link SqlRecursive#canTestEnd}). The siblings considered are
     * deterministic, hold no SERVICE call and are no walks, whose values seed another walk instead (see
     * {@link #shareSeeds}), and an end that siblings seed directly is left to them (see {@link #seedWalks}); of the
     * tests the one expected to spare the walk the most is taken, and only when it is expected to pay (see
     * {@link SqlRecursive#getStartTestGain}). One walk is tested per call; the optimisation loop comes back for the
     * others.
     *
     * @param request the current request
     * @param scope the scope of the join
     * @param childs the child nodes
     * @param restrictions what the parent needs of the variables
     * @return the children, with the first walk whose start pays to be tested replaced by its tested form
     */
    private static List<SqlIntercode> testWalkStarts(Request request, Scope scope, List<SqlIntercode> childs,
            Restrictions restrictions)
    {
        for(int i = 0; i < childs.size(); i++)
        {
            if(!(childs.get(i) instanceof SqlRecursive walk) || !walk.canTestStart())
                continue;

            Variable beginVar = walk.beginVar;
            Variable endVar = walk.endBinding.getVariable();

            // the tested walk exposes what the parent and the remaining siblings need of it
            Restrictions walkRestrictions = getRestrictions(request.getConfiguration(), walk, childs, restrictions);

            SqlRecursive best = null;
            double bestGain = 0;

            for(Variable variable : walk.canTestEnd(walkRestrictions) ? List.of(beginVar, endVar) : List.of(beginVar))
            {
                Variable other = variable.equals(beginVar) ? endVar : beginVar;

                if(!selectSeeds(request, childs, i, variable, other, walk.graphVar).isEmpty())
                    continue;

                for(int j = 0; j < childs.size(); j++)
                {
                    SqlIntercode child = childs.get(j);
                    VariableBinding binding = child.getVariable(variable);

                    if(j == i || binding == null || binding.canBeNull() || child instanceof SqlRecursive
                            || !child.isDeterministic() || child.hasServiceSubpattern())
                        continue;

                    if(!(walk.testStart(request, scope, child, variable.equals(endVar),
                            walkRestrictions) instanceof SqlRecursive candidate))
                        continue;

                    double gain = candidate.getStartTestGain(request);

                    if(gain > bestGain)
                    {
                        best = candidate;
                        bestGain = gain;
                    }
                }
            }

            if(best == null)
                continue;

            List<SqlIntercode> result = new ArrayList<>(childs);
            result.set(i, best);

            return result;
        }

        return childs;
    }


    /**
     * True if the database estimates fewer distinct values of the end variable in the seeds of the end than of the
     * begin variable in the seeds of the begin (see {@link Estimator}); false on a tie, when an estimate fails, or when
     * seeds refer to a current solution (see {@link Scope#refersToCurrentSolution}), that of an EXISTS or of the left
     * side of a lateral join, because such seeds cannot be explained on their own.
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

        for(SqlIntercode join : List.of(endJoin, beginJoin))
            if(scope.refersToCurrentSolution(join.translate(request, scope)))
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
     * alternatives), dropping unjoinable ones. The combinations are built child by child and checked in the context of
     * the whole join (see {@link #getJoinedClasses}): the components of the first children were restricted against the
     * children coming later too, so a component may lack the columns of a class that a later child excludes. Such a
     * class meets no class the variable takes in the whole join, and as a solution of a complete combination binds the
     * variable to a value of one of those classes, no combination the check drops completes into one with a solution.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param childs the child nodes
     * @return alternatives of joined components of a join of the children (cartesian combination of the children's
     *         alternatives), dropping unjoinable ones
     */
    private static List<List<SqlIntercode>> expandJoin(ClassRelations relations, List<SqlIntercode> childs)
    {
        Map<Variable, Set<ResourceClass>> context = getJoinedClasses(relations,
                childs.stream().map(c -> c.getVariableBindings()).toList());

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

                    if(areJoinable(relations, merged.stream().map(c -> c.getVariableBindings()).toList(), context))
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


    /**
     * The solutions of the join are distinct over the selected variables when the children are (see
     * {@link #isDistinct(Request, List, Collection)}), so that a deduplication over them is left out.
     */
    @Override
    public boolean isDistinct(Request request, Collection<Variable> selected)
    {
        return isDistinct(request, childs, selected);
    }


    /**
     * True if the solutions of a join of the children projected to the selected variables contain no duplicates. The
     * inference is inspired by the unique constraints that Ontop derives for the joins of its intermediate queries: two
     * solutions of the join that agree in the selected variables are made of the same solution of every child when the
     * children can be taken one by one so that each has distinct solutions over variables the two solutions are known
     * to agree in, which are the selected ones at first and then also the variables of every child taken, as the same
     * solution of a child gives them the same values. So a child joined by a key of its solutions to the children taken
     * before (a table referenced by a foreign key, a grouping by the shared variable) keeps the solutions of the join
     * distinct, and so does a child that a constant restricts to a single solution. A variable tells the solutions of a
     * child apart only where the child gives the join its value (see {@link #getGivenVariables}).
     *
     * @param request the current request
     * @param childs the children of the join
     * @param selected the selected variables
     * @return true if the solutions of the join projected to the selected variables are known to contain no duplicates,
     *         false otherwise
     */
    static boolean isDistinct(Request request, List<SqlIntercode> childs, Collection<Variable> selected)
    {
        Set<Variable> agreed = new HashSet<>(selected);
        List<Integer> undetermined = new ArrayList<>(IntStream.range(0, childs.size()).boxed().toList());
        boolean progress = true;

        while(progress && !undetermined.isEmpty())
        {
            progress = false;

            for(Iterator<Integer> iterator = undetermined.iterator(); iterator.hasNext();)
            {
                int index = iterator.next();
                Set<Variable> given = getGivenVariables(childs, index);
                Set<Variable> known = new HashSet<>(given);
                known.retainAll(agreed);

                if(childs.get(index).isDistinct(request, known))
                {
                    iterator.remove();
                    agreed.addAll(given);
                    progress = true;
                }
            }
        }

        return undetermined.isEmpty();
    }


    /**
     * The variables whose values in a solution of a join are those of the solution of the child at the index it is made
     * of: the variables the child binds always, which the other children may only agree with, and those no other child
     * binds, which the join takes from the child also where it leaves them unbound.
     *
     * @param childs the children of the join
     * @param index the index of the child
     * @return the variables whose values in a solution of the join are those of the child
     */
    private static Set<Variable> getGivenVariables(List<SqlIntercode> childs, int index)
    {
        Set<Variable> given = new HashSet<>();

        for(VariableBinding binding : childs.get(index).getVariableBindings().getValues())
            if(!binding.canBeNull() || IntStream.range(0, childs.size())
                    .noneMatch(i -> i != index && childs.get(i).getVariable(binding.getVariable()) != null))
                given.add(binding.getVariable());

        return given;
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
    public String getCollation(Request request, Variable variable, ResourceClass resClass, int index)
    {
        return getCollation(request, childs, variable, resClass, index);
    }


    @Override
    public void generateExplanation(StringBuilder builder, String indent)
    {
        builder.append("join");

        // the tree chosen by the optimizer, when the join has been translated with it
        if(plan != null && plan.root() != null)
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

        if(!filterConditions.equals(imcode.filterConditions) || neededRows != imcode.neededRows)
            return false;

        return true;
    }


    @Override
    protected int getHashCode()
    {
        return Objects.hash(new Multiset<>(childs));
    }
}
