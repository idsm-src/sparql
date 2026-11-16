package cz.iocb.sparql.engine.imcode;

import static cz.iocb.sparql.engine.imcode.expression.SqlLiteral.falseValue;
import static cz.iocb.sparql.engine.imcode.expression.SqlLiteral.trueValue;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdBoolean;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import cz.iocb.sparql.engine.database.AliasTable;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.CommonTable;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.InputTable;
import cz.iocb.sparql.engine.database.SqlBuilder;
import cz.iocb.sparql.engine.database.SqlCode;
import cz.iocb.sparql.engine.imcode.expression.SqlBooleanExpression;
import cz.iocb.sparql.engine.imcode.expression.SqlExists;
import cz.iocb.sparql.engine.imcode.expression.SqlExpressionIntercode;
import cz.iocb.sparql.engine.imcode.expression.SqlExpressionIntercode.Restriction;
import cz.iocb.sparql.engine.imcode.expression.SqlNull;
import cz.iocb.sparql.engine.mapping.classes.ClassRelations;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.VariableBinding;
import cz.iocb.sparql.engine.translator.VariableBindings;



/**
 * Left outer join implementing OPTIONAL: every solution of the left side is kept, extended by the compatible solutions
 * of the right side that satisfy the conditions, or by unbound variables when there is none. A leading OPTIONAL (the
 * left side being the single empty solution) whose variables are joined later stands for a union instead (see
 * {@link #toUnion}), and an OPTIONAL walk of zero or more steps from nodes the left side binds always, which has a
 * solution for every solution of the left side, is a join (see {@link SqlRecursive#matchesEverySolutionOf}). Another
 * walk on the right side whose start the left side binds always is seeded with the distinct values of the start read
 * from the left side, shared so that the statement evaluates it once (see {@link SqlShared}). A condition that the
 * solutions of the right side decide by themselves filters that side (see {@link SqlIntercode#keepsValues}): it is
 * handed down to the contexts of the SERVICE calls of the right side when the side can take it in (see
 * {@link SqlIntercode#sink}), and it becomes a filter of the right side otherwise, unless that side is a union, over
 * whose branches a filter is distributed, or a walk the left side may still seed, or the condition tests a pattern (see
 * {@link SqlExpressionIntercode#hasPattern}). With the join optimizer on, the engine chooses the method of the join as
 * well (see {@link JoinOptimizer#planLeftJoin}): the right side is evaluated once and hashed, or it is looked up for
 * every solution of the left side (see {@link #translate}), also by a variable the left side may leave unbound, for the
 * solutions that bind it (see {@link #translateForSolution}). Where the sides share only such variables and the right
 * side is evaluated once, their equality is a disjunction the database could just compare pairwise, so the solutions
 * binding them are joined apart from the others, by a plain equality (see {@link #translateForBound}).
 */
public final class SqlLeftJoin extends SqlIntercode
{
    /**
     * Alias of the right side read by the solutions of the left side that leave a variable unbound (see
     * {@link #translateForBound}).
     */
    private static final AliasTable unboundTable = new AliasTable("unbound");

    /**
     * Alias of the solutions of a join whose right side is read from two subqueries (see {@link #translateForBound}).
     */
    private static final AliasTable joinTable = new AliasTable("tab");

    /**
     * Left side, always kept.
     */
    private final SqlIntercode left;

    /**
     * Right side, optional.
     */
    private final SqlIntercode right;

    /**
     * Join conditions from the filters of the optional pattern.
     */
    private final List<SqlExpressionIntercode> conditions;

    /**
     * Alias of the left side (see {@link Scope#getLeftInputTable}), fixed when the join is created, because the column
     * map and the conditions address the columns of the side through it.
     */
    private final InputTable leftTable;

    /**
     * Alias of the right side (see {@link Scope#getRightInputTable}), fixed when the join is created like the alias of
     * the left side.
     */
    private final InputTable rightTable;

    /**
     * For each output column, the side column it is taken from.
     */
    private final Map<Column, Column> columnMap;

    /**
     * Per variable the left side binds always, the conditions the filters above keep the solutions of the join by (see
     * {@link Restrictions#getFilterConditions}): the database evaluates a condition that one such variable decides
     * alone on the left side, before the join, so the optimizer chooses the method of the join for the solutions of the
     * left side the conditions leave.
     */
    private final Map<Variable, Set<SqlExpressionIntercode>> filterConditions;


    /**
     * Creates the node.
     *
     * @param bindings the variable bindings
     * @param left the left side
     * @param right the right side
     * @param conditions the conditions
     * @param leftTable alias of the left side
     * @param rightTable alias of the right side
     * @param columnMap the column map
     * @param filterConditions per variable the left side binds always, the conditions the filters above keep the
     *            solutions by
     */
    protected SqlLeftJoin(VariableBindings bindings, SqlIntercode left, SqlIntercode right,
            List<SqlExpressionIntercode> conditions, InputTable leftTable, InputTable rightTable,
            Map<Column, Column> columnMap, Map<Variable, Set<SqlExpressionIntercode>> filterConditions)
    {
        super(bindings, left.isDeterministic() && right.isDeterministic()
                && conditions.stream().allMatch(c -> c.isDeterministic()));

        this.left = left;
        this.right = right;
        this.conditions = conditions;
        this.leftTable = leftTable;
        this.rightTable = rightTable;
        this.columnMap = columnMap;
        this.filterConditions = filterConditions;
    }


    /**
     * Left join with the given filter conditions of the optional pattern.
     *
     * @param request the current request
     * @param scope the scope of the join
     * @param left the left side
     * @param right the right side
     * @param conditions the conditions
     * @return left join with the given filter conditions of the optional pattern
     */
    public static SqlIntercode leftJoin(Request request, Scope scope, SqlIntercode left, SqlIntercode right,
            List<SqlExpressionIntercode> conditions)
    {
        return leftJoin(request, scope, left, right, conditions, null);
    }


    /**
     * Left join exposing only what the parent needs; constant columns of the right side are turned into real columns so
     * they can be tested for NULL.
     *
     * @param request the current request
     * @param scope the scope of the join
     * @param left the left side
     * @param right the right side
     * @param conditions the conditions
     * @param restrictions what the parent needs of the variables
     * @return left join exposing only what the parent needs; constant columns of the right side are turned into real
     *         columns so they can be tested for NULL
     */
    protected static SqlIntercode leftJoin(Request request, Scope scope, SqlIntercode left, SqlIntercode right,
            List<SqlExpressionIntercode> conditions, Restrictions restrictions)
    {
        boolean hasConstantColumn = false;

        for(VariableBinding binding : right.getVariableBindings().getValues())
            for(Entry<ResourceClass, List<Column>> entry : binding.getMappings().entrySet())
                if(entry.getValue() != null)
                    for(Column column : entry.getValue())
                        if(column instanceof ConstantColumn)
                            hasConstantColumn = true;

        if(hasConstantColumn)
            right = SqlStripConstantColumns.strip(right);

        InputTable leftTable = scope.getLeftInputTable();
        InputTable rightTable = scope.getRightInputTable();

        Map<Column, Column> map = new HashMap<>();
        VariableBindings bindings = getJoinVariableBindings(request, left.getVariableBindings(),
                setCanBeNull(right.getVariableBindings()), leftTable, rightTable, restrictions, map);

        return new SqlLeftJoin(bindings, left, right, conditions, leftTable, rightTable, map,
                getFilterConditions(left, restrictions));
    }


    /**
     * The conditions the filters above keep the solutions of a left join by, per variable its left side binds always
     * (see {@link Restrictions#getFilterConditions}): every solution of the join takes such a variable from the left
     * side, so the database evaluates the conditions there.
     *
     * @param left the left side
     * @param restrictions what the parent needs of the variables, or null
     * @return per variable the left side binds always, the conditions the filters above keep the solutions by
     */
    private static Map<Variable, Set<SqlExpressionIntercode>> getFilterConditions(SqlIntercode left,
            Restrictions restrictions)
    {
        Map<Variable, Set<SqlExpressionIntercode>> conditions = new HashMap<>();

        if(restrictions == null)
            return conditions;

        for(VariableBinding binding : left.getVariableBindings().getValues())
            if(!binding.canBeNull() && !restrictions.getFilterConditions(binding.getVariable()).isEmpty())
                conditions.put(binding.getVariable(), restrictions.getFilterConditions(binding.getVariable()));

        return conditions;
    }


    /**
     * False if the sides can never match or a condition is always false or an error.
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param left the left side
     * @param right the right side
     * @param conditions the conditions
     * @return false if the sides can never match or a condition is always false or an error
     */
    private static boolean isJoinable(ClassRelations relations, SqlIntercode left, SqlIntercode right,
            List<SqlExpressionIntercode> conditions)
    {
        if(conditions.stream().anyMatch(f -> f.equals(SqlNull.get()) || f.equals(falseValue)
                || f instanceof SqlBooleanExpression b && b.isFalseOrError()))
            return false;

        return isJoinable(relations, left, right);
    }


    /**
     * The bindings with every variable marked as possibly unbound.
     *
     * @param bindings the variable bindings
     * @return the bindings with every variable marked as possibly unbound
     */
    private static VariableBindings setCanBeNull(VariableBindings bindings)
    {
        VariableBindings result = new VariableBindings();

        for(VariableBinding binding : bindings.getValues())
            result.add(new VariableBinding(binding.getVariable(), binding.getMappings(), true));

        return result;
    }


    /**
     * Bindings under which the join conditions are evaluated: the variables of both sides addressed through the aliases
     * of the join, which are those of a left join created in the given scope (see {@link Scope#getLeftInputTable}).
     *
     * @param request the current request
     * @param scope the scope of the join
     * @param left bindings of the left side
     * @param right bindings of the right side
     * @return bindings under which the join conditions are evaluated: the variables of both sides addressed through the
     *         aliases of the join
     */
    public static VariableBindings getExpressionVariableBindings(Request request, Scope scope, VariableBindings left,
            VariableBindings right)
    {
        InputTable leftTable = scope.getLeftInputTable();
        InputTable rightTable = scope.getRightInputTable();

        Map<Column, Column> map = new HashMap<>();
        VariableBindings joinBindings = getJoinVariableBindings(request, left, right, leftTable, rightTable, null, map);

        VariableBindings bindings = new VariableBindings();

        for(VariableBinding variableBinding : (joinBindings != null ? joinBindings : left).getValues())
        {
            VariableBinding binding = new VariableBinding(variableBinding.getVariable(), variableBinding.canBeNull());

            for(ResourceClass resClass : variableBinding.getClasses())
            {
                List<Column> cols = variableBinding.getMapping(resClass);

                if(cols == null)
                    binding.addMapping(resClass, null);
                else if(joinBindings != null)
                    binding.addMapping(resClass, cols.stream().map(c -> map.getOrDefault(c, c)).toList());
                else
                    binding.addMapping(resClass, cols.stream().map(c -> c.fromTable(leftTable)).toList());
            }

            bindings.add(binding);
        }

        return bindings;
    }


    /**
     * Variables the left side always binds. The right side keeps its accepted classes only for them: a solution of the
     * right side binding one of them to a value of a class the left side cannot join never reaches the result, whereas
     * for a variable the left side may leave unbound, or does not bind at all, the solution joins (or, left out, leaves
     * unbound) a value the parent may still accept.
     *
     * @param bindings the variable bindings of the left side
     * @return the variables the left side always binds
     */
    private static Set<Variable> getNonNullVariables(VariableBindings bindings)
    {
        Set<Variable> variables = new HashSet<>();

        for(VariableBinding binding : bindings.getValues())
            if(!binding.canBeNull())
                variables.add(binding.getVariable());

        return variables;
    }


    @Override
    public SqlIntercode optimize(Request request, Scope scope, Restrictions restrictions, boolean reduced,
            boolean evalServices)
    {
        ClassRelations relations = request.getConfiguration();

        SqlIntercode right = this.right;

        if(right instanceof SqlStripConstantColumns strip)
            right = strip.getChild();

        // the conditions the right side decides by itself filter it before the sides are optimised
        SqlIntercode filteredRight = filterRight(request, scope, left, right, conditions, restrictions);

        if(filteredRight != null)
            return filteredRight.optimize(request, scope, restrictions, reduced, evalServices);

        SqlIntercode optLeft = left;
        SqlIntercode optRight = right;
        List<SqlExpressionIntercode> optConditions = conditions;
        boolean optReduced = reduced & optConditions.stream().allMatch(r -> r.isDeterministic());


        Restrictions cndRestrictions = new Restrictions(restrictions);

        for(SqlExpressionIntercode condition : optConditions)
            cndRestrictions.add(condition.getRequirements(relations));

        // a solution of the right side binding a variable to a value of a class in which no condition can hold joins
        // no solution of the left side, so the right side may leave such solutions out; the classes are read once from
        // the sides as built, whose bindings still carry every class
        Restrictions accepted = new Restrictions();
        addAcceptedClasses(request, scope, accepted, conditions,
                getExpressionVariableBindings(request, scope, left.getVariableBindings(), right.getVariableBindings()));
        addAcceptedValues(request, accepted, conditions, right.getVariableBindings());

        Restrictions leftRestrictions = getJoinRestrictions(relations, optLeft.getVariableBindings(),
                optRight.getVariableBindings(), cndRestrictions, false);
        Restrictions rightRestrictions = getJoinRestrictions(relations, optRight.getVariableBindings(),
                optLeft.getVariableBindings(), cndRestrictions, true);
        rightRestrictions.retainAcceptedClasses(getNonNullVariables(optLeft.getVariableBindings()));
        rightRestrictions.add(accepted);

        // the database evaluates on the right side the conditions of the join that the variables of that side alone
        // decide (a variable the left side binds is taken from it), but no condition of a filter above
        rightRestrictions.removeFilterConditions();
        addFilterConditions(request, rightRestrictions, optConditions, right.getVariableBindings());
        rightRestrictions.removeFilterConditions(optLeft.getVariableBindings().getVariables());


        boolean offered = false;

        // the sides could swap their presentations of a shared variable in every round
        Settlement settlement = new Settlement();

        while(true)
        {
            optLeft = optLeft.optimize(request, scope, leftRestrictions, optReduced, evalServices);

            // a join on the right side holding a walk whose start the left side binds always is offered the distinct
            // values of the start read from the left side, shared so that the statement evaluates it once, before its
            // own siblings seed the walk (a walk alone is offered them after the loop, when it has not become a join)
            if(!offered && !(optLeft instanceof SqlShared) && !SqlRecursive.isWalkAlone(optRight))
            {
                offered = true;

                Variable seedVar = SqlRecursive.seedVariable(optRight, optLeft.getVariableBindings());

                if(seedVar != null && SqlShared.canShare(request, scope, optLeft))
                {
                    optLeft = SqlShared.share(optLeft);
                    optRight = SqlJoin.join(request, SqlShared.seed(request, scope, optLeft, seedVar, evalServices),
                            optRight);
                }
            }

            optRight = optRight.optimize(request, scope, rightRestrictions, optReduced, evalServices);
            optConditions = optimize(request, scope, optConditions, optLeft.getVariableBindings(),
                    optRight.getVariableBindings(), evalServices);

            if(optRight instanceof SqlUnion union)
            {
                List<SqlIntercode> unionList = new ArrayList<>();

                for(SqlIntercode child : union.getChilds())
                    if(isJoinable(relations, optLeft, child, optimize(request, scope, optConditions,
                            optLeft.getVariableBindings(), child.getVariableBindings(), evalServices)))
                        unionList.add(child);

                if(!unionList.equals(union.getChilds()))
                {
                    optRight = SqlUnion.union(request, unionList).optimize(request, scope, rightRestrictions,
                            optReduced, evalServices);

                    optConditions = optimize(request, scope, optConditions, optLeft.getVariableBindings(),
                            optRight.getVariableBindings(), evalServices);
                }
            }

            // a condition requiring a term of a variable holds for every solution of a right side that binds the
            // variable to the term for good, whatever solution of the left side it extends
            List<SqlExpressionIntercode> unsatisfied = new ArrayList<>();

            for(SqlExpressionIntercode condition : optConditions)
                unsatisfied.addAll(getUnsatisfied(request, condition, optRight));

            optConditions = unsatisfied;


            boolean newOptReduced = optReduced & optConditions.stream().allMatch(r -> r.isDeterministic());

            Restrictions newCndRestrictions = new Restrictions(restrictions);

            for(SqlExpressionIntercode condition : optConditions)
                newCndRestrictions.add(condition.getRequirements(relations));

            Restrictions newLeftRestrictions = getJoinRestrictions(relations, optLeft.getVariableBindings(),
                    Set.of(optRight.getVariableBindings()), newCndRestrictions, false, settlement.getClasses());
            Restrictions newRightRestrictions = getJoinRestrictions(relations, optRight.getVariableBindings(),
                    Set.of(optLeft.getVariableBindings()), newCndRestrictions, true, settlement.getClasses());
            newRightRestrictions.retainAcceptedClasses(getNonNullVariables(optLeft.getVariableBindings()));
            newRightRestrictions.add(accepted);
            newRightRestrictions.removeFilterConditions();
            addFilterConditions(request, newRightRestrictions, optConditions, right.getVariableBindings());
            newRightRestrictions.removeFilterConditions(optLeft.getVariableBindings().getVariables());


            if(newOptReduced == optReduced && newLeftRestrictions.equals(leftRestrictions)
                    && newRightRestrictions.equals(rightRestrictions))
                break;

            if(!settlement.record(List.of(optLeft, optRight, optConditions, newOptReduced, offered),
                    List.of(optLeft, optRight)))
                break;

            optReduced = newOptReduced;
            leftRestrictions = newLeftRestrictions;
            rightRestrictions = newRightRestrictions;
        }


        if(optLeft.equals(SqlNoSolution.get()))
            return SqlNoSolution.get();

        if(optRight.equals(SqlNoSolution.get()) || optRight.equals(SqlEmptySolution.get())
                || !isJoinable(relations, optLeft, optRight, optConditions))
            return optLeft.optimize(request, scope, restrictions, optReduced, evalServices);

        // the conditions (their conjuncts) that the solutions of the right side decide by themselves filter that side,
        // so the ones it can take in restrict the contexts of its SERVICE calls, which a join condition does not reach
        if(!evalServices && optRight.hasServiceSubpattern())
        {
            List<VariableBindings> others = List.of(optLeft.getVariableBindings());
            VariableBindings known = getExpressionVariableBindings(request, scope, optLeft.getVariableBindings(),
                    optRight.getVariableBindings());

            SqlIntercode sunk = optRight;
            List<SqlExpressionIntercode> kept = new ArrayList<>();

            for(SqlExpressionIntercode condition : getConjuncts(optConditions))
            {
                SqlIntercode taken = null;

                if(isSinkable(request, scope, condition) && keepsValues(getDecidingVariables(relations, condition),
                        optRight.getVariableBindings(), others))
                    taken = sunk.sink(request, scope, condition, known);

                if(taken != null)
                    sunk = taken;
                else
                    kept.add(condition);
            }

            if(sunk != optRight)
                return leftJoin(request, scope, optLeft, sunk, kept, restrictions).optimize(request, scope,
                        restrictions, optReduced, evalServices);
        }

        if(optLeft instanceof SqlLeftJoin join && join.isLeadingOptional() && !Collections
                .disjoint(optLeft.getVariableBindings().getVariables(), optRight.getVariableBindings().getVariables()))
            optLeft = join.toUnion(request, scope, evalServices);

        if(optLeft instanceof SqlUnion union)
        {
            List<SqlIntercode> unionList = new ArrayList<>();

            for(SqlIntercode child : union.getChilds())
            {
                List<SqlExpressionIntercode> cnds = optimize(request, scope, optConditions, child.getVariableBindings(),
                        optRight.getVariableBindings(), evalServices);

                unionList.add(leftJoin(request, scope, child, optRight, cnds, restrictions));
            }

            return SqlUnion.union(request, unionList).optimize(request, scope, restrictions, optReduced, evalServices);
        }

        // the zero-length paths of a walk match every node of the graph, so a walk of zero or more steps from nodes
        // the left side always binds has a solution for every solution of the left side: the OPTIONAL is a join (which
        // may then seed the walk with the left side)
        if(optConditions.isEmpty() && optRight instanceof SqlRecursive walk && walk.matchesEverySolutionOf(optLeft))
            return SqlJoin.join(request, optLeft, optRight).optimize(request, scope, restrictions, optReduced,
                    evalServices);

        // a walk on the right side whose start the left side binds always is seeded with the distinct values of the
        // start read from the left side, shared so that the statement evaluates it once
        Variable seedVar = optLeft instanceof SqlShared ? null :
                SqlRecursive.seedVariable(optRight, optLeft.getVariableBindings());

        if(seedVar != null && SqlShared.canShare(request, scope, optLeft))
        {
            SqlIntercode shared = SqlShared.share(optLeft);
            SqlIntercode seed = SqlShared.seed(request, scope, shared, seedVar, evalServices);

            return leftJoin(request, scope, shared, SqlJoin.join(request, seed, optRight), optConditions, restrictions)
                    .optimize(request, scope, restrictions, optReduced, evalServices);
        }

        // the conditions the right side decides by itself only now filter it (a walk seeded in the meantime, a union
        // reduced to a branch), which the sides are optimised for again
        SqlIntercode filtered = filterRight(request, scope, optLeft, optRight, optConditions, restrictions);

        if(filtered != null)
            return filtered.optimize(request, scope, restrictions, optReduced, evalServices);

        if(optLeft instanceof SqlTableAccess l && optRight instanceof SqlTableAccess r && optConditions.isEmpty())
        {
            DatabaseSchema schema = request.getConfiguration().getDatabaseSchema();

            SqlIntercode merge = SqlTableAccess.tryReduceLeftJoin(relations, schema, l, r, restrictions);

            if(merge != null)
                return merge;
        }


        if(optLeft == left && optRight == right && optConditions.equals(conditions)
                && restrictions.isOptimized(relations, bindings)
                && filterConditions.equals(getFilterConditions(optLeft, restrictions)))
            return this;

        return leftJoin(request, scope, optLeft, optRight, optConditions, restrictions);
    }


    /**
     * The left join of the sides in which the conditions that the right side decides by itself filter that side, or
     * null when no condition does: a condition (a conjunct of one) whose deciding variables the right side binds
     * always, or nothing else binds (see {@link SqlIntercode#keepsValues}), holds for a solution of the join exactly
     * when it holds for the solution of the right side it is made of. The sides are then joined without the solutions
     * the condition drops, also where the condition tests a variable the left side binds too, which the join itself
     * would compare only when it joins the sides. A condition stays in the join when the right side is a union (see
     * {@link #isUnionAlone}), as a filter is distributed over the branches of a union, which are then no longer read by
     * a single scan (see {@link SqlUnion}); when the right side is a walk the left side may still seed (see
     * {@link SqlRecursive#holdsSeedableWalk}); when it is not deterministic; when it tests a pattern, which the
     * database evaluates in the join for the pairs of solutions it compares, whereas in a filter it would join the
     * pattern, evaluating whole a pattern that cannot be looked up (see {@link SqlExpressionIntercode#hasPattern}); and
     * when the bindings of the right side do not let rebuild it (see {@link SqlIntercode#filterBy}).
     *
     * @param request the current request
     * @param scope the scope of the join
     * @param left the left side
     * @param right the right side
     * @param conditions the conditions
     * @param restrictions what the parent needs of the variables
     * @return the left join whose right side is filtered by the conditions it decides by itself, or null
     */
    private static SqlIntercode filterRight(Request request, Scope scope, SqlIntercode left, SqlIntercode right,
            List<SqlExpressionIntercode> conditions, Restrictions restrictions)
    {
        if(conditions.isEmpty() || isUnionAlone(right) || SqlRecursive.holdsSeedableWalk(right))
            return null;

        ClassRelations relations = request.getConfiguration();

        List<VariableBindings> others = List.of(left.getVariableBindings());
        VariableBindings known = getExpressionVariableBindings(request, scope, left.getVariableBindings(),
                right.getVariableBindings());

        SqlIntercode filtered = right;
        List<SqlExpressionIntercode> kept = new ArrayList<>();

        for(SqlExpressionIntercode condition : getConjuncts(conditions))
        {
            SqlIntercode taken = null;

            if(condition.isDeterministic() && !condition.hasPattern()
                    && keepsValues(getDecidingVariables(relations, condition), right.getVariableBindings(), others))
                taken = filterBy(request, scope, condition, known, filtered);

            if(taken != null)
                filtered = taken;
            else
                kept.add(condition);
        }

        return filtered == right ? null : leftJoin(request, scope, left, filtered, kept, restrictions);
    }


    /**
     * True if the node is a union alone: a union, or a join of a union with empty solutions only, as a group pattern
     * holding nothing but a union is translated, which the optimisation makes the union itself.
     *
     * @param node the node
     * @return true if the node is a union alone, false otherwise
     */
    private static boolean isUnionAlone(SqlIntercode node)
    {
        if(node instanceof SqlUnion)
            return true;

        if(!(node instanceof SqlJoin join))
            return false;

        List<SqlIntercode> childs = join.getChilds().stream().filter(c -> !c.equals(SqlEmptySolution.get())).toList();

        return childs.size() == 1 && isUnionAlone(childs.get(0));
    }


    /**
     * The left join whose left side has taken the condition in: every solution of the join is made of a solution of the
     * left side, which gives a deciding variable of the condition its value unless the left side may leave the variable
     * unbound and the right side binds it. The right side takes no condition from above in, because a solution of the
     * left side it does not extend is kept.
     */
    @Override
    protected SqlIntercode sink(Request request, Scope scope, SqlExpressionIntercode condition,
            VariableBindings bindings)
    {
        Set<Variable> variables = getDecidingVariables(request.getConfiguration(), condition);

        if(!keepsValues(variables, left.getVariableBindings(), List.of(right.getVariableBindings())))
            return null;

        SqlIntercode sunk = left.sink(request, scope, condition, bindings);

        if(sunk == null)
            return null;

        return leftJoin(request, scope, sunk, right instanceof SqlStripConstantColumns strip ? strip.getChild() : right,
                conditions);
    }


    /**
     * Optimises the conditions over the joined bindings, dropping those that are always true.
     *
     * @param request the current request
     * @param scope the scope of the join
     * @param conditions the conditions
     * @param left bindings of the left side
     * @param right bindings of the right side
     * @param evalServices whether SERVICE stubs are evaluated
     * @return the optimised conditions
     */
    private static List<SqlExpressionIntercode> optimize(Request request, Scope scope,
            List<SqlExpressionIntercode> conditions, VariableBindings left, VariableBindings right,
            boolean evalServices)
    {
        VariableBindings bindings = getExpressionVariableBindings(request, scope, left, right);

        List<SqlExpressionIntercode> result = new ArrayList<>(conditions.size());

        conditions.stream().map(f -> f.optimize(request, scope, bindings, new Restriction(xsdBoolean), evalServices))
                .filter(f -> !f.equals(trueValue)).forEach(f -> result.add(f));

        return result;
    }


    /**
     * Whether this is a leading OPTIONAL: the left join of the single empty solution with the optional pattern, as a
     * group graph pattern starting with OPTIONAL gives.
     *
     * @return whether this is a leading OPTIONAL
     */
    public boolean isLeadingOptional()
    {
        return left.equals(SqlEmptySolution.get());
    }


    /**
     * Union a leading OPTIONAL stands for: the solutions of the optional pattern satisfying the conditions, and the
     * empty solution when there is none. The left join keeps the variables of the pattern possibly unbound, so a join
     * on them admits their unbound values by a disjunction the database can neither hash nor serve by an index; a join
     * with the union is expanded over its branches instead, the first compared by plain equality and the second guarded
     * by an existence test of the pattern evaluated once.
     *
     * @param request the current request
     * @param scope the scope of the join
     * @param evalServices whether SERVICE stubs are evaluated
     * @return union a leading OPTIONAL stands for
     */
    protected SqlIntercode toUnion(Request request, Scope scope, boolean evalServices)
    {
        SqlIntercode pattern = right instanceof SqlStripConstantColumns strip ? strip.getChild() : right;
        VariableBindings patternBindings = pattern.getVariableBindings();

        List<SqlExpressionIntercode> cnds = conditions.stream()
                .map(c -> c.optimize(request, scope, patternBindings, new Restriction(xsdBoolean), evalServices))
                .toList();

        if(!cnds.isEmpty())
            pattern = SqlFilter.filter(request, cnds, pattern);

        SqlExpressionIntercode missing = SqlExists.create(request, scope, true, pattern, new VariableBindings());
        SqlIntercode none = SqlFilter.filter(request, List.of(missing), SqlEmptySolution.get());

        return SqlUnion.union(request, List.of(pattern, none));
    }


    /**
     * The method the optimizer chooses for the join of the sides (see {@link JoinOptimizer#planLeftJoin}), the right
     * side referring to the solutions of the left side through the alias of that side.
     *
     * @param request the current request
     * @param outer the rows the whole join is evaluated for one by one, or null when it is evaluated once
     * @return the method chosen for the join
     */
    JoinOptimizer.LeftJoinPlan getPlan(Request request, JoinOptimizer.Outer outer)
    {
        return JoinOptimizer.planLeftJoin(request, left, right, leftTable, filterConditions, outer);
    }


    /**
     * SQL code of the left join. With the join optimizer on, the right side is written the way the optimizer chooses
     * (see {@link #getPlan}): as it is, for the database to evaluate it once and hash the sides, or as a lateral
     * subquery evaluated for every solution of the left side and restricted to the solutions compatible with it (see
     * {@link SqlIntercode#translateForRow}), which the database joins by a nested loop over the left side; the subquery
     * may tell the solutions binding the variables the left side may leave unbound from the others (see
     * {@link #translateForSolution}). The condition of the join is the same in all cases, so the restriction only
     * spares the database the solutions the condition would drop. A right side evaluated once that shares with the left
     * side only variables that side may leave unbound is joined apart with the solutions binding them (see
     * {@link #translateForBound}). Without the optimizer the join is written plainly: the subquery that form guards the
     * right side by cannot be merged into the query around it, and the database then no longer evaluates a pattern
     * holding the join just for the rows of a join around the pattern, which it does by itself otherwise.
     */
    @Override
    public SqlCode translate(Request request, Scope scope)
    {
        return translate(request, scope, null);
    }


    /**
     * The left join whose left side is evaluated for the row of the outer table: a solution of the join is made of a
     * solution of the left side, which gives every variable it binds its value. The right side is joined by the method
     * chosen for the solutions the left side has for one row.
     */
    @Override
    SqlCode translateForRow(Request request, Scope scope, JoinOptimizer.Outer outer)
    {
        return translate(request, scope, outer);
    }


    /**
     * SQL code of the left join, evaluated once (see {@link #translate(Request, Scope)}) or for one of the rows of an
     * outer table (see {@link #translateForRow}).
     *
     * @param request the current request
     * @param scope the scope of the join
     * @param outer the rows the join is evaluated for one by one, or null when it is evaluated once
     * @return SQL code of the left join
     */
    private SqlCode translate(Request request, Scope scope, JoinOptimizer.Outer outer)
    {
        ClassRelations relations = request.getConfiguration();

        // the method the optimizer chooses for the join
        JoinOptimizer.LeftJoinPlan plan = request.getConfiguration().isJoinOptimizerEnabled() ?
                getPlan(request, outer) : null;

        SqlCode whole = plan == null || plan.hashed().isEmpty() ? null : right.translate(request, scope);

        // a right side referring to a table that a common table expression cannot see is written once
        if(whole != null && whole.getOuterTables().isEmpty())
            return translateForBound(request, scope, outer, plan.hashed(), whole);

        SqlBuilder builder = new SqlBuilder();

        Set<Column> columns = bindings.getNonConstantColumns();

        builder.append("SELECT ");

        if(!columns.isEmpty())
            builder.append(columns.stream()
                    .map(c -> columnMap.get(c) != null ? SqlCode.of(columnMap.get(c), " AS ", c) : c.getCode())
                    .collect(SqlCode.joining(", ")));
        else
            builder.append("1");

        builder.append(" FROM (");
        builder.append(outer == null ? left.translate(request, scope) : left.translateForRow(request, scope, outer));
        builder.append(" ) AS ");
        builder.appendAlias(leftTable);

        if(plan != null && plan.bound() != null)
        {
            builder.append(" LEFT JOIN LATERAL (");
            builder.append(translateForSolution(request, scope, plan));
        }
        else if(plan != null && plan.outer() != null)
        {
            builder.append(" LEFT JOIN LATERAL (");
            builder.append(right.translateForRow(request, scope, plan.outer()));
        }
        else
        {
            builder.append(" LEFT JOIN (");
            builder.append(whole != null ? whole : right.translate(request, scope));
        }

        builder.append(" ) AS ");
        builder.appendAlias(rightTable);
        builder.append(" ON ");
        appendCondition(builder, relations, left.bindings);

        return builder.toCode();
    }


    /**
     * Appends the condition of the join: the compatibility of the shared variables of the sides, for the given bindings
     * of the left side, and the conditions of the optional pattern.
     *
     * @param builder the builder of the SQL code
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param leftBindings bindings of the left side
     */
    private void appendCondition(SqlBuilder builder, ClassRelations relations, VariableBindings leftBindings)
    {
        SqlCode condition = generateJoinCondition(relations, leftBindings, right.getVariableBindings(), leftTable,
                rightTable);

        if(condition != null)
            builder.append(condition);

        if(condition != null && !conditions.isEmpty())
            builder.append(" AND ");

        builder.append(translateConditions(relations, conditions));

        if(condition == null && conditions.isEmpty())
            builder.append("true");
    }


    /**
     * SQL code of the left join whose sides share only variables the left side may leave unbound (see
     * {@link JoinOptimizer#getHashedWhenBound}), so that the equality of the join is a disjunction the database can
     * neither hash nor look up. The right side is joined twice. The first join compares the variables by a plain
     * equality, which holds only for the solutions of the left side binding them all, so the database hashes those as
     * it would for variables bound always. The second join is a subquery evaluated for the other solutions only:
     * guarded by the test that the solution leaves a variable unbound, which the database evaluates once per solution,
     * it reads the right side from a common table expression the database keeps the result of (see
     * {@link Request#getMaterializedTableExpression}) and compares it with the solution by the condition of the join as
     * it is. The subquery is fenced by {@code OFFSET 0}, because the test pulled up into the join would be evaluated
     * for every pair. The first join reads that common table expression too, so that the right side is evaluated once,
     * unless the right side is an access to a table: the table is read in place then, so that nothing is kept while
     * every solution binds the variables, and a second time only when a solution leaves one unbound, which costs what
     * reading a kept result does. A solution of the left side is extended by one of the joins at most, so the columns
     * of the right side are those of the join that gave them. Both subqueries name the right side by its alias, which
     * the conditions and the column map address its columns through: the second one inside itself, and the columns
     * taken from the two joins again in the subquery that the solutions of the join are read from.
     *
     * @param request the current request
     * @param scope the scope of the join
     * @param outer the rows the join is evaluated for one by one, or null when it is evaluated once
     * @param hashed the variables the left side may leave unbound that the sides are joined by
     * @param whole SQL code of the right side, which refers to no outer table
     * @return SQL code of the left join
     */
    private SqlCode translateForBound(Request request, Scope scope, JoinOptimizer.Outer outer, Set<Variable> hashed,
            SqlCode whole)
    {
        ClassRelations relations = request.getConfiguration();

        // the left side as its solutions binding the variables bind them
        VariableBindings bound = new VariableBindings();
        List<VariableBinding> tested = new ArrayList<>();

        for(VariableBinding binding : left.getVariableBindings().getValues())
        {
            if(hashed.contains(binding.getVariable()))
            {
                tested.add(binding);
                bound.add(new VariableBinding(binding.getVariable(), binding.getMappings(), false));
            }
            else
            {
                bound.add(binding);
            }
        }

        Set<Column> columns = bindings.getNonConstantColumns();
        Set<Column> rightColumns = right.getVariableBindings().getNonConstantColumns();

        // a table is read in place, any other right side is evaluated once for both joins
        SqlIntercode pattern = right instanceof SqlStripConstantColumns strip ? strip.getChild() : right;
        CommonTable kept = request.getMaterializedTableExpression(whole);

        SqlBuilder builder = new SqlBuilder();

        builder.append("SELECT ");

        if(!columns.isEmpty())
            builder.append(
                    columns.stream().map(c -> SqlCode.of(c.fromTable(joinTable))).collect(SqlCode.joining(", ")));
        else
            builder.append("1");

        builder.append(" FROM (");
        builder.append(outer == null ? left.translate(request, scope) : left.translateForRow(request, scope, outer));
        builder.append(" ) AS ");
        builder.appendAlias(leftTable);
        builder.append(" LEFT JOIN ");

        if(pattern instanceof SqlTableAccess)
        {
            builder.append("(");
            builder.append(whole);
            builder.append(" )");
        }
        else
        {
            builder.append(kept);
        }

        builder.append(" AS ");
        builder.appendAlias(rightTable);
        builder.append(" ON ");
        appendCondition(builder, relations, bound);

        builder.append(" LEFT JOIN LATERAL (SELECT ");
        builder.append(rightColumns.isEmpty() ? SqlCode.of("1") : rightColumns.stream().collect(SqlCode.joining(", ")));
        builder.append(" FROM ");
        builder.append(kept);
        builder.append(" AS ");
        builder.appendAlias(rightTable);
        builder.append(" WHERE (");
        builder.append(tested.stream().map(b -> b.getIsNull(leftTable)).collect(SqlCode.joining(" OR ")));
        builder.append(") AND ");
        appendCondition(builder, relations, left.bindings);
        builder.append(" OFFSET 0) AS ");
        builder.appendAlias(unboundTable);
        builder.append(" ON true");

        builder.append(" CROSS JOIN LATERAL (SELECT ");

        if(!columns.isEmpty())
            builder.append(columns.stream()
                    .map(c -> columnMap.get(c) != null ? SqlCode.of(columnMap.get(c), " AS ", c) : c.getCode())
                    .collect(SqlCode.joining(", ")));
        else
            builder.append("1");

        builder.append(" FROM (SELECT ");

        if(!rightColumns.isEmpty())
            builder.append(rightColumns.stream().map(
                    c -> SqlCode.of("COALESCE(", c.fromTable(rightTable), ", ", c.fromTable(unboundTable), ") AS ", c))
                    .collect(SqlCode.joining(", ")));
        else
            builder.append("1");

        builder.append(") AS ");
        builder.appendAlias(rightTable);
        builder.append(") AS ");
        builder.appendAlias(joinTable);

        return builder.toCode();
    }


    /**
     * SQL code of the right side evaluated for one solution of the left side, as the plan tells the solutions apart
     * (see {@link JoinOptimizer.LeftJoinPlan}): a union of two branches, each guarded by a test of the solution, so
     * that the database evaluates just one of them. For a solution binding every variable the left side may leave
     * unbound, the right side is restricted by those variables too. For any other solution it is evaluated as the plan
     * says without them: restricted by the variables the left side binds always, or whole, in which case the solutions
     * read it from a common table expression the database keeps the result of (see
     * {@link Request#getMaterializedTableExpression}), so the right side is evaluated once however many solutions leave
     * a variable unbound, and not at all when none does. A right side referring to a table that such an expression
     * cannot see (see {@link SqlCode#getOuterTables}) is written in the place. The union is fenced by {@code OFFSET 0}:
     * the database then plans it as a unit that depends on the solution only through the values it is evaluated for, so
     * it may keep the results by those values (its Memoize node) and the solutions leaving a variable unbound, which
     * all read the same rows, cost what a pairwise comparison does; without the fence it keeps nothing, because the
     * tests of the branches pulled up into the join read the solution on their own.
     *
     * @param request the current request
     * @param scope the scope of the join
     * @param plan the method chosen for the join, telling the solutions of the left side apart
     * @return SQL code of the right side evaluated for one solution of the left side
     */
    private SqlCode translateForSolution(Request request, Scope scope, JoinOptimizer.LeftJoinPlan plan)
    {
        List<VariableBinding> tested = left.getVariableBindings().getValues().stream()
                .filter(b -> b.canBeNull() && plan.bound().bindings().get(b.getVariable()) != null).toList();

        Set<Column> columns = right.getVariableBindings().getNonConstantColumns();
        SqlCode list = columns.isEmpty() ? SqlCode.of("1") : columns.stream().collect(SqlCode.joining(", "));

        SqlBuilder builder = new SqlBuilder();

        builder.append("SELECT ");
        builder.append(list);
        builder.append(" FROM (");
        builder.append(right.translateForRow(request, scope, plan.bound()));
        builder.append(") AS tab WHERE ");
        builder.append(tested.stream().map(b -> b.getIsNotNull(leftTable)).collect(SqlCode.joining(" AND ")));

        builder.append(" UNION ALL SELECT ");
        builder.append(list);
        builder.append(" FROM ");

        SqlCode whole = plan.outer() != null ? null : right.translate(request, scope);

        if(whole != null && whole.getOuterTables().isEmpty())
        {
            builder.append(request.getMaterializedTableExpression(whole));
        }
        else
        {
            builder.append("(");
            builder.append(whole != null ? whole : right.translateForRow(request, scope, plan.outer()));
            builder.append(")");
        }

        builder.append(" AS tab WHERE ");
        builder.append(tested.stream().map(b -> b.getIsNull(leftTable)).collect(SqlCode.joining(" OR ")));

        // the fence makes the database plan the union as a unit, so that it can keep the results by the tested values
        builder.append(" OFFSET 0");

        return builder.toCode();
    }


    /**
     * Left side, always kept.
     *
     * @return left side, always kept
     */
    public final SqlIntercode getLeft()
    {
        return left;
    }


    /**
     * Right side, optional.
     *
     * @return right side, optional
     */
    public final SqlIntercode getRight()
    {
        return right;
    }


    /**
     * Join conditions from the filters of the optional pattern.
     *
     * @return join conditions from the filters of the optional pattern
     */
    public final List<SqlExpressionIntercode> getConditions()
    {
        return conditions;
    }


    @Override
    protected boolean isRestricted(Variable variable, SqlExpressionIntercode value)
    {
        //NOTE: a variable the left side binds always keeps its value in every solution of the left join
        return left.isRestricted(variable, value);
    }


    @Override
    public boolean hasServiceSubpattern()
    {
        return left.hasServiceSubpattern() || right.hasServiceSubpattern()
                || conditions.stream().anyMatch(c -> c.hasServiceSubpattern());
    }


    @Override
    public String getCollation(Request request, Variable variable, ResourceClass resClass, int index)
    {
        return getCollation(request, List.of(left, right), variable, resClass, index);
    }


    @Override
    public void generateExplanation(StringBuilder builder, String indent)
    {
        builder.append("left join");

        for(SqlExpressionIntercode cnd : conditions)
        {
            indentInfo(builder, indent, true);
            cnd.generateExplanation(builder, indent);
        }

        indentChild(builder, indent, false);
        left.generateExplanation(builder, getIndent(indent, false));

        indentChild(builder, indent, true);
        right.generateExplanation(builder, getIndent(indent, true));
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(!(object instanceof SqlLeftJoin imcode))
            return false;

        if(!super.equals(imcode))
            return false;

        if(!Objects.equals(conditions, imcode.conditions))
            return false;

        if(!filterConditions.equals(imcode.filterConditions))
            return false;

        if(!Objects.equals(left, imcode.left))
            return false;

        if(!Objects.equals(right, imcode.right))
            return false;

        return true;
    }


    @Override
    protected int getHashCode()
    {
        return Objects.hash(conditions, left, right);
    }
}
