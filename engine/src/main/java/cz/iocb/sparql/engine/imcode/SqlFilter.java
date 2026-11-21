package cz.iocb.sparql.engine.imcode;

import static cz.iocb.sparql.engine.imcode.expression.SqlBinaryComparison.ComparisonOperator.EQUAL;
import static cz.iocb.sparql.engine.imcode.expression.SqlBinaryComparison.ComparisonOperator.NOT_EQUAL;
import static cz.iocb.sparql.engine.imcode.expression.SqlLiteral.falseValue;
import static cz.iocb.sparql.engine.imcode.expression.SqlLiteral.trueValue;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdBoolean;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.InputTable;
import cz.iocb.sparql.engine.database.SqlBuilder;
import cz.iocb.sparql.engine.database.SqlCode;
import cz.iocb.sparql.engine.imcode.expression.SqlBinaryComparison;
import cz.iocb.sparql.engine.imcode.expression.SqlBinaryLogical;
import cz.iocb.sparql.engine.imcode.expression.SqlBooleanExpression;
import cz.iocb.sparql.engine.imcode.expression.SqlBuiltinCall;
import cz.iocb.sparql.engine.imcode.expression.SqlExists;
import cz.iocb.sparql.engine.imcode.expression.SqlExpressionIntercode;
import cz.iocb.sparql.engine.imcode.expression.SqlExpressionIntercode.Restriction;
import cz.iocb.sparql.engine.imcode.expression.SqlNull;
import cz.iocb.sparql.engine.imcode.expression.SqlUnaryLogical;
import cz.iocb.sparql.engine.imcode.expression.SqlVariable;
import cz.iocb.sparql.engine.mapping.classes.BuiltinClasses;
import cz.iocb.sparql.engine.mapping.classes.ClassRelations;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Request.NodeTerm;
import cz.iocb.sparql.engine.translator.Multiset;
import cz.iocb.sparql.engine.translator.VariableBinding;
import cz.iocb.sparql.engine.translator.VariableBindings;



/**
 * Keeps the solutions of the child for which every condition evaluates to true (FILTER). Optimisation distributes the
 * filter over the branches of a union, and the conditions the child can take in are handed down to the contexts of its
 * SERVICE calls (see {@link SqlIntercode#sink}), which a filter standing above a call does not restrict. A condition
 * requiring a constant term of a variable ({@code sameTerm}, an equality with an IRI, or with a literal that only one
 * term the variable can be is equal to) makes the table accesses of the child take the term into their own conditions
 * (see {@link SqlIntercode#addAcceptedValues}), and it is left out once the child is restricted to the term for good
 * (see {@link SqlIntercode#getUnsatisfied}). With the join optimizer on, the engine chooses whether the pattern of a
 * condition that is an existence test is evaluated once or for every solution of the child (see {@link #translate}).
 */
public final class SqlFilter extends SqlIntercode
{
    /**
     * Filtered solutions.
     */
    private final SqlIntercode child;

    /**
     * Conditions, all of which must hold.
     */
    private final List<SqlExpressionIntercode> conditions;


    /**
     * Creates the node.
     *
     * @param bindings the variable bindings
     * @param child the child node
     * @param conditions the conditions
     */
    protected SqlFilter(VariableBindings bindings, SqlIntercode child, List<SqlExpressionIntercode> conditions)
    {
        super(bindings, child.isDeterministic() && conditions.stream().allMatch(c -> c.isDeterministic()));

        this.child = child;
        this.conditions = conditions;
    }


    /**
     * Filter of the child by the conjunction of the conditions.
     *
     * @param request the current request
     * @param conditions the conditions
     * @param child the child node
     * @return filter of the child by the conjunction of the conditions
     */
    public static SqlIntercode filter(Request request, List<SqlExpressionIntercode> conditions, SqlIntercode child)
    {
        return filter(request, conditions, child, null);
    }


    /**
     * Filter exposing only what the parent needs.
     *
     * @param request the current request
     * @param conditions the conditions
     * @param child the child node
     * @param restrictions what the parent needs of the variables
     * @return filter exposing only what the parent needs
     */
    protected static SqlIntercode filter(Request request, List<SqlExpressionIntercode> conditions, SqlIntercode child,
            Restrictions restrictions)
    {
        VariableBindings bindings = child.getVariableBindings().restrict(request.getConfiguration(), restrictions);

        return new SqlFilter(bindings, child, conditions);
    }


    /**
     * A test of two variables for the same term.
     *
     * @param left a variable
     * @param right the other variable
     * @param negated whether the test holds when the terms are different
     */
    private record TermTest(Variable left, Variable right, boolean negated)
    {
    }


    /**
     * The test of two variables for the same term that a condition is: a test of the same term whatever the classes of
     * the variables are, and an equality or an inequality where one of the variables is an IRI or a blank node always,
     * which is equal to the same term only and is compared with any other term without an error.
     *
     * @param condition the condition
     * @param bindings the variable bindings giving the classes of the variables
     * @return the test of two variables for the same term, or null when the condition is none
     */
    private static TermTest getTermTest(SqlExpressionIntercode condition, VariableBindings bindings)
    {
        if(condition instanceof SqlBuiltinCall call && call.getFunction().equals("sameterm")
                && call.getArguments().get(0) instanceof SqlVariable left
                && call.getArguments().get(1) instanceof SqlVariable right)
            return new TermTest(left.getVariable(), right.getVariable(), false);

        if(!(condition instanceof SqlBinaryComparison comparison) || !(comparison.getLeft() instanceof SqlVariable left)
                || !(comparison.getRight() instanceof SqlVariable right)
                || comparison.getOperator() != EQUAL && comparison.getOperator() != NOT_EQUAL)
            return null;

        for(Variable variable : List.of(left.getVariable(), right.getVariable()))
            if(bindings.get(variable) != null
                    && bindings.get(variable).getClasses().stream().allMatch(BuiltinClasses::isReference))
                return new TermTest(left.getVariable(), right.getVariable(), comparison.getOperator() == NOT_EQUAL);

        return null;
    }


    /**
     * The variables that a condition tests for the same term, two by two (see {@link #getTermTest}), also under the
     * logical operators.
     *
     * @param condition the condition
     * @param bindings the variable bindings giving the classes of the variables
     * @return the variables that the condition tests for the same term
     */
    private static Set<Variable> getTestedVariables(SqlExpressionIntercode condition, VariableBindings bindings)
    {
        Set<Variable> variables = new LinkedHashSet<>();

        if(condition instanceof SqlBinaryLogical logical)
        {
            variables.addAll(getTestedVariables(logical.getLeft(), bindings));
            variables.addAll(getTestedVariables(logical.getRight(), bindings));
        }
        else if(condition instanceof SqlUnaryLogical negation)
        {
            variables.addAll(getTestedVariables(negation.getOperand(), bindings));
        }
        else if(getTermTest(condition, bindings) instanceof TermTest test)
        {
            variables.addAll(List.of(test.left(), test.right()));
        }

        return variables;
    }


    /**
     * The condition with its tests of two variables for the same term (see {@link #getTermTest}) turned into tests of
     * the nodes standing in for the terms (see {@link SqlIntercode#getDeterminingNodes}), where the solutions of the
     * child bind both variables and such nodes always and the two nodes read their terms alike (see
     * {@link #readAlike}): the terms are then the same exactly when the nodes are, so the child need not read the terms
     * for the test.
     *
     * @param request the current request
     * @param condition the condition
     * @param child the child, asked for the nodes as well
     * @return the condition comparing the nodes where it can
     */
    private static SqlExpressionIntercode compareNodes(Request request, SqlExpressionIntercode condition,
            SqlIntercode child)
    {
        ClassRelations relations = request.getConfiguration();

        if(condition instanceof SqlBinaryLogical logical)
        {
            SqlExpressionIntercode left = compareNodes(request, logical.getLeft(), child);
            SqlExpressionIntercode right = compareNodes(request, logical.getRight(), child);

            return left == logical.getLeft() && right == logical.getRight() ? condition :
                    SqlBinaryLogical.create(relations, logical.getOperator(), left, right);
        }

        if(condition instanceof SqlUnaryLogical negation)
        {
            SqlExpressionIntercode operand = compareNodes(request, negation.getOperand(), child);

            return operand == negation.getOperand() ? condition : SqlUnaryLogical.create(relations, operand);
        }

        TermTest test = getTermTest(condition, child.getVariableBindings());

        if(test == null || test.left().equals(test.right()))
            return condition;

        for(Variable variable : List.of(test.left(), test.right()))
            if(child.getVariable(variable) == null || child.getVariable(variable).canBeNull())
                return condition;

        for(Variable leftNode : getDeterminingNodes(request, test.left()))
        {
            for(Variable rightNode : getDeterminingNodes(request, test.right()))
            {
                VariableBinding leftBinding = child.getVariable(leftNode);
                VariableBinding rightBinding = child.getVariable(rightNode);

                if(leftBinding == null || leftBinding.canBeNull() || rightBinding == null || rightBinding.canBeNull()
                        || !readAlike(request, leftNode, rightNode))
                    continue;

                List<SqlExpressionIntercode> nodes = List.of(SqlVariable.create(relations, leftBinding),
                        SqlVariable.create(relations, rightBinding));

                SqlExpressionIntercode same = SqlBuiltinCall.create(request, "sameterm", false, nodes);

                return test.negated() ? SqlUnaryLogical.create(relations, same) : same;
            }
        }

        return condition;
    }


    /**
     * True if two nodes read their terms alike: from the same columns of one table, in the same class and by the same
     * join columns, so that the two nodes are the same exactly when the terms they stand in for are.
     *
     * @param request the current request
     * @param left a node
     * @param right another node
     * @return true if the two nodes read their terms alike, false otherwise
     */
    private static boolean readAlike(Request request, Variable left, Variable right)
    {
        NodeTerm leftTerm = request.getNodeTerms().get(left);
        NodeTerm rightTerm = request.getNodeTerms().get(right);

        if(!(leftTerm.reader() instanceof SqlTableAccess leftReader)
                || !(rightTerm.reader() instanceof SqlTableAccess rightReader))
            return false;

        return leftReader.getTable().equals(rightReader.getTable())
                && leftReader.getVariable(left).getMappings().equals(rightReader.getVariable(right).getMappings())
                && leftReader.getVariable(leftTerm.variable()).getMappings()
                        .equals(rightReader.getVariable(rightTerm.variable()).getMappings());
    }


    /**
     * The number of the solutions of the child that the filter reads for the solutions its parent reads of its own (see
     * {@link Restrictions#getNeededRows}): as many more as the conditions are estimated to drop (see
     * {@link JoinOptimizer#selectivity(SqlExpressionIntercode)}). A condition handed down as a filter condition of the
     * variables deciding it (see {@link #addFilterConditions}) does not count, because the child that orders its joins
     * with it in mind counts its own solutions without those the condition drops.
     *
     * @param request the current request
     * @param needed the number of the solutions of the filter that its parent reads at most
     * @param conditions the conditions of the filter
     * @return the number of the solutions of the child the filter reads at most
     */
    private double getNeededRows(Request request, double needed, List<SqlExpressionIntercode> conditions)
    {
        if(Double.isInfinite(needed))
            return needed;

        ClassRelations relations = request.getConfiguration();

        Restrictions handed = new Restrictions();
        addFilterConditions(request, handed, conditions, child.getVariableBindings());

        for(SqlExpressionIntercode conjunct : getConjuncts(conditions))
            if(getDecidingVariables(relations, conjunct).stream()
                    .noneMatch(v -> handed.getFilterConditions(v).contains(conjunct)))
                needed /= JoinOptimizer.selectivity(conjunct);

        return needed;
    }


    @Override
    public SqlIntercode optimize(Request request, Scope scope, Restrictions restrictions, boolean reduced,
            boolean evalServices)
    {
        ClassRelations relations = request.getConfiguration();

        SqlIntercode optChild = child;
        List<SqlExpressionIntercode> optCnds = conditions;

        boolean childReduced = reduced && optCnds.stream().allMatch(r -> r.isDeterministic());

        Restrictions childRestrictions = new Restrictions(restrictions);

        for(SqlExpressionIntercode cnd : optCnds)
            childRestrictions.add(cnd.getRequirements(relations));

        // the classes of a variable in which no condition can hold never pass the filter, so the child may leave their
        // solutions out; they are read once from the child as built, whose bindings still carry every class; nor does
        // a solution binding a variable to another term than the one a condition requires of it pass
        Restrictions accepted = new Restrictions();
        addAcceptedClasses(request, scope, accepted, conditions, child.getVariableBindings());
        addAcceptedValues(request, accepted, conditions, child.getVariableBindings());
        childRestrictions.add(accepted);

        // the other conditions leave the child as it is, but its joins are ordered with them in mind
        addFilterConditions(request, childRestrictions, optCnds, child.getVariableBindings());
        childRestrictions.setNeededRows(getNeededRows(request, restrictions.getNeededRows(), optCnds));

        // two variables tested for the same term can be compared by the nodes standing in for their terms, which the
        // child is asked for as well, because the solutions may come to bind them always only once they are optimised
        for(SqlExpressionIntercode cnd : optCnds)
            for(Variable variable : getTestedVariables(cnd, child.getVariableBindings()))
                childRestrictions.add(getDeterminingNodes(request, variable));

        while(true)
        {
            SqlIntercode newOptChild = optChild.optimize(request, scope, childRestrictions, childReduced, evalServices);

            List<SqlExpressionIntercode> newOptCnds = new LinkedList<>();

            for(SqlExpressionIntercode cnd : optCnds)
            {
                SqlExpressionIntercode expression = cnd.optimize(request, scope, newOptChild.getVariableBindings(),
                        new Restriction(xsdBoolean), evalServices);

                if(expression.equals(SqlNull.get()) || expression.equals(falseValue))
                    return SqlNoSolution.get();
                else if(expression instanceof SqlBooleanExpression expr && expr.isFalseOrError())
                    return SqlNoSolution.get();
                else if(!expression.equals(trueValue))
                    newOptCnds.addAll(getUnsatisfied(request, expression, newOptChild));
            }

            optChild = newOptChild;
            optCnds = newOptCnds;

            boolean newChildReduced = reduced && optCnds.stream().allMatch(r -> r.isDeterministic());
            Restrictions newChildRestrictions = new Restrictions(restrictions);
            newOptCnds.forEach(c -> newChildRestrictions.add(c.getRequirements(relations)));
            newChildRestrictions.add(accepted);
            addFilterConditions(request, newChildRestrictions, newOptCnds, child.getVariableBindings());
            newChildRestrictions.setNeededRows(getNeededRows(request, restrictions.getNeededRows(), newOptCnds));

            for(SqlExpressionIntercode cnd : newOptCnds)
                for(Variable variable : getTestedVariables(cnd, optChild.getVariableBindings()))
                    newChildRestrictions.add(getDeterminingNodes(request, variable));

            if(newChildReduced == childReduced && newChildRestrictions.equals(childRestrictions))
                break;

            childRestrictions = newChildRestrictions;
            childReduced = newChildReduced;
        }


        if(optChild.equals(SqlNoSolution.get()))
            return SqlNoSolution.get();

        if(optCnds.isEmpty())
            return optChild.optimize(request, scope, restrictions, reduced, evalServices);

        // the tests of the same term whose variables the solutions bind always along with nodes standing in for their
        // terms compare the nodes, so that the child reads no term for them
        SqlIntercode tested = optChild;
        List<SqlExpressionIntercode> byNodes = optCnds.stream().map(c -> compareNodes(request, c, tested)).toList();

        if(!byNodes.equals(optCnds))
            return filter(request, byNodes, optChild).optimize(request, scope, restrictions, reduced, evalServices);

        if(optChild instanceof SqlFilter filter)
        {
            List<SqlExpressionIntercode> merged = new ArrayList<>();
            merged.addAll(filter.conditions);
            merged.addAll(optCnds);

            List<SqlExpressionIntercode> cnds = merged.stream().map(c -> c.optimize(request, scope,
                    filter.child.getVariableBindings(), new Restriction(xsdBoolean), evalServices)).toList();

            return filter(request, cnds, filter.child).optimize(request, scope, restrictions, reduced, evalServices);
        }

        if(optChild instanceof SqlUnion union)
        {
            List<SqlIntercode> childs = new ArrayList<>();

            for(SqlIntercode child : union.getChilds())
            {
                List<SqlExpressionIntercode> cnds = optCnds.stream().map(c -> c.optimize(request, scope,
                        child.getVariableBindings(), new Restriction(xsdBoolean), evalServices)).toList();

                childs.add(filter(request, cnds, child, restrictions));
            }

            return SqlUnion.union(request, childs).optimize(request, scope, restrictions, reduced, evalServices);
        }

        // the conditions (their conjuncts) the child can take in restrict the contexts of its SERVICE calls, which the
        // filter itself does not reach: a context is read before its call is made
        if(!evalServices && optChild.hasServiceSubpattern())
        {
            SqlIntercode sunk = optChild;
            List<SqlExpressionIntercode> kept = new ArrayList<>();

            for(SqlExpressionIntercode cnd : getConjuncts(optCnds))
            {
                SqlIntercode taken = !isSinkable(request, scope, cnd) ? null :
                        sunk.sink(request, scope, cnd, optChild.getVariableBindings());

                if(taken != null)
                    sunk = taken;
                else
                    kept.add(cnd);
            }

            if(sunk != optChild)
                return (kept.isEmpty() ? sunk : filter(request, kept, sunk)).optimize(request, scope, restrictions,
                        reduced, evalServices);
        }


        if(optCnds.equals(conditions) && optChild == child && restrictions.isOptimized(relations, bindings))
            return this;

        return filter(request, optCnds, optChild, restrictions);
    }


    /**
     * The filter over its child that has taken the condition in: a filter gives the variables the values its child
     * gives them.
     */
    @Override
    protected SqlIntercode sink(Request request, Scope scope, SqlExpressionIntercode condition,
            VariableBindings bindings)
    {
        SqlIntercode sunk = child.sink(request, scope, condition, bindings);

        return sunk == null ? null : filter(request, conditions, sunk);
    }


    @Override
    public SqlCode translate(Request request, Scope scope)
    {
        return translate(request, scope, null);
    }


    /**
     * The filter of the child evaluated for the row of the outer table: the conditions keep a solution or drop it by
     * the solution alone.
     */
    @Override
    SqlCode translateForRow(Request request, Scope scope, JoinOptimizer.Outer outer)
    {
        return translate(request, scope, outer);
    }


    /**
     * SQL code of the filter, evaluated once or for one of the rows of an outer table (see {@link #translateForRow}).
     *
     * @param request the current request
     * @param scope the scope of the filter
     * @param outer the rows the filter is evaluated for one by one, or null when it is evaluated once
     * @return SQL code of the filter
     */
    private SqlCode translate(Request request, Scope scope, JoinOptimizer.Outer outer)
    {
        SqlBuilder builder = new SqlBuilder();

        builder.append("SELECT ");

        Set<Column> columns = getVariableBindings().getNonConstantColumns();

        if(!columns.isEmpty())
            builder.append(columns.stream().collect(SqlCode.joining(", ")));
        else
            builder.append("1");

        builder.append(" FROM (");
        builder.append(outer == null ? child.translate(request, scope) : child.translateForRow(request, scope, outer));
        builder.append(" ) AS ");
        builder.appendAlias(scope.getInputTable());

        builder.append(" WHERE ");
        builder.append(translateConditions(request, scope, outer));

        return builder.toCode();
    }


    /**
     * SQL code of the conjunction of the conditions as the filter itself evaluates them (see
     * {@link #translateConditions(Request)}). With the join optimizer on, the pattern of a condition that is an
     * existence test (see {@link SqlExists}) is written the way the optimizer chooses (see
     * {@link JoinOptimizer#planTest}): with its solutions deduplicated (see {@link SqlDistinct#deduplicate}), for the
     * database to evaluate the pattern once and to join it with the solutions of the child by hashing, or evaluated for
     * every solution of the child that the other conditions keep, restricted to the solutions compatible with it (see
     * {@link SqlIntercode#translateForRow}), so that the database looks them up.
     *
     * @param request the current request
     * @param scope the scope of the filter
     * @param outer the rows the filter is evaluated for one by one, or null when it is evaluated once
     * @return SQL code of the conjunction of the conditions
     */
    private SqlCode translateConditions(Request request, Scope scope, JoinOptimizer.Outer outer)
    {
        ClassRelations relations = request.getConfiguration();

        if(!request.getConfiguration().isJoinOptimizerEnabled()
                || conditions.stream().noneMatch(c -> c instanceof SqlExists))
            return translateConditions(request);

        List<SqlExpressionIntercode> others = conditions.stream().filter(c -> !(c instanceof SqlExists)).toList();

        double solutions = (outer == null ? JoinOptimizer.estimateRows(request, child) :
                JoinOptimizer.estimateForRow(request, child, outer).rows())
                * (others.isEmpty() ? 1 : JoinOptimizer.selectivity(request, child, getConjuncts(others)));

        return conditions.stream().map(c -> c instanceof SqlExists test ?
                translateTest(request, scope, test, solutions) : c.get(relations, xsdBoolean).get(0).getCode())
                .collect(SqlCode.joining(" AND "));
    }


    /**
     * SQL code of an existence test among the conditions: the test of the distinct solutions of the pattern, which the
     * database evaluates once (the code the test was rendered with when the solutions are distinct already, or when the
     * pattern refers to an outer table and is evaluated for every row of it anyway), or the test of the pattern
     * evaluated for every solution of the child anew when the optimizer chooses so (see
     * {@link #translateConditions(Request, Scope, JoinOptimizer.Outer)}). The pattern is restricted only by the values
     * that the solutions of the child give, which the test addresses through the alias of the input of the filter. A
     * value of a current solution the filter itself lies in would make the pattern of that solution refer to it, and
     * the database would then evaluate that pattern for every such solution as well.
     *
     * @param request the current request
     * @param scope the scope of the filter
     * @param test the existence test
     * @param solutions estimated number of the solutions of the child the test is evaluated for
     * @return SQL code of the test
     */
    private SqlCode translateTest(Request request, Scope scope, SqlExists test, double solutions)
    {
        SqlCode rendered = test.get(request.getConfiguration(), xsdBoolean).get(0).getCode();
        VariableBindings solution = test.getSolution();

        if(solution == null || test.hasServiceSubpattern())
            return rendered;

        InputTable input = scope.getInputTable();
        VariableBindings own = new VariableBindings();

        for(VariableBinding binding : solution.getValues())
            if(binding.getMappings().values().stream().filter(Objects::nonNull).flatMap(List::stream)
                    .allMatch(c -> c.getCode().getOuterTables().stream().allMatch(input::equals)))
                own.add(binding);

        SqlIntercode source = test.getSource(request, scope);
        JoinOptimizer.Outer rows = JoinOptimizer.planTest(request, solutions, own, null, source).outer();

        if(rows == null)
        {
            SqlCode code = source.translate(request, scope.enterPattern(solution));

            if(!code.getOuterTables().isEmpty())
                return rendered;

            SqlCode distinct = SqlDistinct.deduplicate(request, source, code);

            return distinct == code ? rendered : test.translate(request, scope, distinct);
        }

        return test.translate(request, scope, source.translateForRow(request, scope.enterPattern(solution), rows));
    }


    /**
     * SQL code of the conjunction of the conditions. It reads the columns of the child by their names, so it belongs to
     * a query whose only source is the child, under the alias of the input of a node holding expressions (see
     * {@link Scope#getInputTable}), which the patterns of the conditions refer to; it reads the declared tables the
     * conditions read.
     *
     * @param request the current request
     * @return SQL code of the conjunction of the conditions
     */
    public SqlCode translateConditions(Request request)
    {
        return translateConditions(request.getConfiguration(), conditions);
    }


    @Override
    public boolean isDistinct(Request request, Collection<Variable> selected)
    {
        return child.isDistinct(request, selected);
    }


    /**
     * Filtered solutions.
     *
     * @return filtered solutions
     */
    public final SqlIntercode getChild()
    {
        return child;
    }


    /**
     * Conditions, all of which must hold.
     *
     * @return conditions, all of which must hold
     */
    public final List<SqlExpressionIntercode> getConditions()
    {
        return conditions;
    }


    @Override
    protected boolean isRestricted(Variable variable, SqlExpressionIntercode value)
    {
        return child.isRestricted(variable, value);
    }


    @Override
    public boolean hasServiceSubpattern()
    {
        return child.hasServiceSubpattern() || conditions.stream().anyMatch(c -> c.hasServiceSubpattern());
    }


    @Override
    public String getCollation(Request request, Variable variable, ResourceClass resClass, int index)
    {
        return getCollation(request, List.of(child), variable, resClass, index);
    }


    @Override
    public void generateExplanation(StringBuilder builder, String indent)
    {
        builder.append("filter");

        for(SqlExpressionIntercode cnd : conditions)
        {
            indentInfo(builder, indent, true);
            cnd.generateExplanation(builder, getIndent(indent, false));
        }

        indentChild(builder, indent, true);
        child.generateExplanation(builder, getIndent(indent, true));
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(!(object instanceof SqlFilter imcode))
            return false;

        if(!super.equals(imcode))
            return false;

        if(!Objects.equals(new Multiset<>(conditions), new Multiset<>(imcode.conditions)))
            return false;

        if(!Objects.equals(child, imcode.child))
            return false;

        return true;
    }


    @Override
    protected int getHashCode()
    {
        return Objects.hash(new Multiset<>(conditions), child);
    }
}
