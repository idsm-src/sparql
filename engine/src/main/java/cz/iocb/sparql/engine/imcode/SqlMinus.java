package cz.iocb.sparql.engine.imcode;

import static cz.iocb.sparql.engine.imcode.expression.SqlLiteral.falseValue;
import static cz.iocb.sparql.engine.imcode.expression.SqlLiteral.trueValue;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdBoolean;
import static java.util.stream.Collectors.joining;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import cz.iocb.sparql.engine.database.AliasTable;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.VirtualTable;
import cz.iocb.sparql.engine.imcode.expression.SqlExpressionIntercode;
import cz.iocb.sparql.engine.imcode.expression.SqlExpressionIntercode.Restriction;
import cz.iocb.sparql.engine.imcode.expression.SqlNull;
import cz.iocb.sparql.engine.mapping.classes.ClassRelations;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.VariableBinding;
import cz.iocb.sparql.engine.translator.VariableBindingPair;
import cz.iocb.sparql.engine.translator.VariableBindings;



/**
 * Set difference (MINUS): the solutions of the left side for which there is no compatible solution of the right side
 * sharing at least one bound variable. Inside an EXISTS pattern, every solution of both sides extends the current
 * solution the pattern is evaluated for, so the sides share its variables without binding them (see
 * {@link #solutionBound}). A walk on the right side whose start the left side binds always is seeded with the distinct
 * values of the start read from the left side, shared so that the statement evaluates it once (see {@link SqlShared}).
 */
public final class SqlMinus extends SqlIntercode
{
    /**
     * Alias of the left side.
     */
    private static final AliasTable leftTable = new AliasTable("tab0");

    /**
     * Alias of the right side.
     */
    private static final AliasTable rightTable = new AliasTable("tab1");

    /**
     * Left side, whose solutions are kept.
     */
    private final SqlIntercode left;

    /**
     * Right side, whose compatible solutions remove left ones.
     */
    private final SqlIntercode right;

    /**
     * Variable of the enclosing GRAPH clause that the triples of both sides bind to their graph, or null. Both sides
     * are evaluated in the same graph, so the variable has to agree like a shared one, but it is no variable of their
     * solutions and does not count as the shared variable a removal requires.
     */
    private final Variable graph;

    /**
     * Condition that the current solution of the EXISTS pattern the difference lies in binds a variable, or null
     * outside such a pattern and when that solution binds none. Every solution of both sides extends the current
     * solution, so a variable it binds is bound on both sides, although the sides do not bind it themselves: a right
     * solution then removes every left solution compatible with it.
     */
    private final SqlExpressionIntercode solutionBound;


    /**
     * Creates the node.
     *
     * @param bindings the variable bindings
     * @param left the left side
     * @param right the right side
     * @param graph variable of the enclosing GRAPH clause bound by the triples of both sides, or null
     * @param solutionBound condition that the current solution of the enclosing EXISTS pattern binds a variable, or
     *            null
     */
    protected SqlMinus(VariableBindings bindings, SqlIntercode left, SqlIntercode right, Variable graph,
            SqlExpressionIntercode solutionBound)
    {
        super(bindings, left.isDeterministic() && right.isDeterministic());

        this.left = left;
        this.right = right;
        this.graph = graph;
        this.solutionBound = solutionBound;
    }


    /**
     * Difference of the two sides.
     *
     * @param request the current request
     * @param left the left side
     * @param right the right side
     * @return difference of the two sides
     */
    public static SqlIntercode minus(Request request, SqlIntercode left, SqlIntercode right)
    {
        return minus(request, left, right, null, null, null);
    }


    /**
     * Difference of the two sides evaluated in the graph of the given variable: the variable is required to agree, but
     * it does not count as a shared variable.
     *
     * @param request the current request
     * @param left the left side
     * @param right the right side
     * @param graph variable of the enclosing GRAPH clause bound by the triples of both sides, or null
     * @return difference of the two sides evaluated in the graph of the given variable
     */
    public static SqlIntercode minus(Request request, SqlIntercode left, SqlIntercode right, Variable graph)
    {
        return minus(request, left, right, graph, null, null);
    }


    /**
     * Difference of the two sides inside an EXISTS pattern, evaluated in the graph of the given variable (see
     * {@link #minus(Request, SqlIntercode, SqlIntercode, Variable)}): the sides share the variables of the current
     * solution the pattern is evaluated for.
     *
     * @param request the current request
     * @param left the left side
     * @param right the right side
     * @param graph variable of the enclosing GRAPH clause bound by the triples of both sides, or null
     * @param solutionBound condition that the current solution of the EXISTS pattern binds a variable, an expression
     *            over the values of that solution only, or null
     * @return difference of the two sides inside an EXISTS pattern
     */
    public static SqlIntercode minus(Request request, SqlIntercode left, SqlIntercode right, Variable graph,
            SqlExpressionIntercode solutionBound)
    {
        return minus(request, left, right, graph, solutionBound, null);
    }


    /**
     * Difference exposing only what the parent needs.
     *
     * @param request the current request
     * @param left the left side
     * @param right the right side
     * @param graph variable of the enclosing GRAPH clause bound by the triples of both sides, or null
     * @param solutionBound condition that the current solution of the enclosing EXISTS pattern binds a variable, or
     *            null
     * @param restrictions what the parent needs of the variables
     * @return difference exposing only what the parent needs
     */
    protected static SqlIntercode minus(Request request, SqlIntercode left, SqlIntercode right, Variable graph,
            SqlExpressionIntercode solutionBound, Restrictions restrictions)
    {
        return new SqlMinus(left.getVariableBindings().restrict(request.getConfiguration(), restrictions), left, right,
                graph, solutionBound);
    }


    @Override
    public SqlIntercode optimize(Request request, Restrictions restrictions, boolean reduced, boolean evalServices)
    {
        ClassRelations relations = request.getConfiguration();

        SqlIntercode optLeft = left;
        SqlIntercode optRight = right;

        boolean leftReduced = reduced & optRight.isDeterministic;
        // a right solution binding a shared variable to a value of a class the left side cannot bind is compatible
        // with no left solution and removes nothing, so the right side may leave such solutions out (the left side
        // keeps every solution, matched or not)
        Restrictions leftRestrictions = getJoinRestrictions(relations, optLeft.getVariableBindings(),
                optRight.getVariableBindings(), restrictions, false);
        Restrictions rightRestrictions = getJoinRestrictions(relations, optRight.getVariableBindings(),
                optLeft.getVariableBindings(), new Restrictions(), true);

        boolean offered = false;

        // the sides could swap their presentations of a shared variable in every round
        Settlement settlement = new Settlement();

        while(true)
        {
            optLeft = optLeft.optimize(request, leftRestrictions, leftReduced, evalServices);

            // a join on the right side holding a walk whose start the left side binds always is offered the distinct
            // values of the start read from the left side, shared so that the statement evaluates it once, before its
            // own siblings seed the walk (a walk alone is offered them after the loop)
            if(!offered && !(optLeft instanceof SqlShared) && !SqlRecursive.isWalkAlone(optRight))
            {
                offered = true;

                Variable seedVar = SqlRecursive.seedVariable(optRight, optLeft.getVariableBindings());

                if(seedVar != null && SqlShared.canShare(request, optLeft))
                {
                    optLeft = SqlShared.share(optLeft);
                    optRight = SqlJoin.join(request, SqlShared.seed(request, optLeft, seedVar, evalServices), optRight);
                }
            }

            optRight = optRight.optimize(request, rightRestrictions, true, evalServices);

            if(optRight instanceof SqlUnion union)
            {
                List<SqlIntercode> unionList = new ArrayList<>();

                for(SqlIntercode child : union.getChilds())
                    if(isJoinable(relations, optLeft, child))
                        unionList.add(child);

                if(!unionList.equals(union.getChilds()))
                    optRight = SqlUnion.union(request, unionList).optimize(request, rightRestrictions, true,
                            evalServices);
            }

            boolean newLeftReduced = reduced & optRight.isDeterministic;
            Restrictions newLeftRestrictions = getJoinRestrictions(relations, optLeft.getVariableBindings(),
                    Set.of(optRight.getVariableBindings()), restrictions, false, settlement.getClasses());
            Restrictions newRightRestrictions = getJoinRestrictions(relations, optRight.getVariableBindings(),
                    Set.of(optLeft.getVariableBindings()), new Restrictions(), true, settlement.getClasses());



            if(newLeftReduced == leftReduced && newLeftRestrictions.equals(leftRestrictions)
                    && newRightRestrictions.equals(rightRestrictions))
                break;

            if(!settlement.record(List.of(optLeft, optRight, newLeftReduced, offered), List.of(optLeft, optRight)))
                break;

            leftRestrictions = newLeftRestrictions;
            rightRestrictions = newRightRestrictions;
            leftReduced = newLeftReduced;
        }


        // the condition follows the current solution as the node holding the EXISTS expression provides it now; a
        // solution binding no variable leaves the sides with the variables they bind themselves
        SqlExpressionIntercode optSolutionBound = solutionBound == null ? null :
                solutionBound.optimize(request, new VariableBindings(), new Restriction(xsdBoolean), evalServices);

        if(optSolutionBound != null && (optSolutionBound.equals(SqlNull.get()) || optSolutionBound.equals(falseValue)))
            optSolutionBound = null;

        boolean shareVariables = optSolutionBound != null;

        for(VariableBindingPair pair : VariableBindingPair.getPairs(relations, optLeft.getVariableBindings(),
                optRight.getVariableBindings()))
        {
            if(!pair.getVariable().equals(graph))
                shareVariables = true;

            if(!pair.isJoinable())
                return optLeft.optimize(request, restrictions, reduced, evalServices);
        }

        if(!shareVariables)
            return optLeft.optimize(request, restrictions, reduced, evalServices);

        if(optLeft instanceof SqlUnion union)
        {
            List<SqlIntercode> childs = new ArrayList<>();

            for(SqlIntercode child : union.getChilds())
                childs.add(minus(request, child, optRight, graph, optSolutionBound, restrictions));

            return SqlUnion.union(request, childs).optimize(request, restrictions, reduced, evalServices);
        }


        // a walk on the right side whose start the left side binds always is seeded with the distinct values of the
        // start read from the left side, shared so that the statement evaluates it once
        Variable seedVar = optLeft instanceof SqlShared ? null :
                SqlRecursive.seedVariable(optRight, optLeft.getVariableBindings());

        if(seedVar != null && SqlShared.canShare(request, optLeft))
        {
            SqlIntercode shared = SqlShared.share(optLeft);
            SqlIntercode seed = SqlShared.seed(request, shared, seedVar, evalServices);

            return minus(request, shared, SqlJoin.join(request, seed, optRight), graph, optSolutionBound, restrictions)
                    .optimize(request, restrictions, reduced, evalServices);
        }

        if(restrictions.isOptimized(relations, bindings) && optLeft == left && optRight == right
                && Objects.equals(optSolutionBound, solutionBound))
            return this;

        return minus(request, optLeft, optRight, graph, optSolutionBound, restrictions);
    }


    @Override
    public String translate(Request request)
    {
        ClassRelations relations = request.getConfiguration();

        StringBuilder builder = new StringBuilder();

        builder.append("SELECT ");

        Set<Column> columns = getVariableBindings().getNonConstantColumns();

        if(!columns.isEmpty())
            builder.append(columns.stream().map(Object::toString).collect(joining(", ")));
        else
            builder.append("1");

        builder.append(" FROM (");
        builder.append(left.translate(request));
        builder.append(") AS ");
        builder.append(leftTable);

        builder.append(" WHERE NOT EXISTS (SELECT 1 FROM (");
        builder.append(right.translate(request));
        builder.append(") AS ");
        builder.append(rightTable);

        String condition = generateCondition(relations, left.getVariableBindings(), right.getVariableBindings(),
                leftTable, rightTable);

        if(condition != null)
        {
            builder.append(" WHERE ");
            builder.append(condition);
        }

        builder.append(")");

        return builder.toString();
    }


    /**
     * SQL condition that a right solution removes a left one: the shared variables (the graph variable included) are
     * compatible and at least one of them (the graph variable excluded) is bound on both sides, a variable the current
     * solution of the enclosing EXISTS pattern binds counting as bound on both sides (see {@link #solutionBound}).
     *
     * @param relations declarations which unrelated user IRI classes may overlap
     * @param left bindings of the left side
     * @param right bindings of the right side
     * @param leftTable the left table
     * @param rightTable the right table
     * @return SQL condition that a right solution removes a left one: the shared variables (the graph variable
     *         included) are compatible and at least one of them (the graph variable excluded) is bound on both sides
     */
    private String generateCondition(ClassRelations relations, VariableBindings left, VariableBindings right,
            AliasTable leftTable, AliasTable rightTable)
    {
        String joinCondition = generateJoinCondition(relations, left, right, leftTable, rightTable);

        List<VariableBindingPair> pairs = VariableBindingPair.getPairs(relations, left, right).stream()
                .filter(p -> !p.getVariable().equals(graph)).toList();

        for(VariableBindingPair pair : pairs)
            if(!pair.getLeftVariableBinding().canBeNull() && !pair.getRightVariableBinding().canBeNull())
                return joinCondition;

        if(solutionBound != null && solutionBound.equals(trueValue))
            return joinCondition;


        List<String> condition = new ArrayList<>();

        for(VariableBindingPair pair : pairs)
        {
            VariableBinding leftBinding = pair.getLeftVariableBinding();
            VariableBinding rightBinding = pair.getRightVariableBinding();

            StringBuilder builder = new StringBuilder();

            if(leftBinding.canBeNull())
                builder.append(leftBinding.getIsNotNull(leftTable));

            if(leftBinding.canBeNull() && rightBinding.canBeNull())
                builder.append(" AND ");

            if(rightBinding.canBeNull())
                builder.append(rightBinding.getIsNotNull(rightTable));

            condition.add(builder.toString());
        }

        if(solutionBound != null)
            condition.add(solutionBound.get(relations, xsdBoolean).get(0).toString());

        String domCondition = condition.stream().sorted().collect(joining(" OR "));

        if(domCondition.isEmpty())
            domCondition = "false";


        if(joinCondition == null)
            return domCondition;
        else
            return "(" + joinCondition + ") AND (" + domCondition + ")";
    }


    @Override
    public boolean isDistinct(Request request, Collection<Variable> selected)
    {
        return left.isDistinct(request, selected);
    }


    /**
     * Left side, whose solutions are kept.
     *
     * @return left side, whose solutions are kept
     */
    public final SqlIntercode getLeft()
    {
        return left;
    }


    /**
     * Right side, whose compatible solutions remove left ones.
     *
     * @return right side, whose compatible solutions remove left ones
     */
    public final SqlIntercode getRight()
    {
        return right;
    }


    @Override
    public boolean hasServiceSubpattern()
    {
        return left.hasServiceSubpattern() || right.hasServiceSubpattern();
    }


    @Override
    public Set<VirtualTable> getVirtualTables()
    {
        //NOTE: the condition on the current solution reads no table
        return getVirtualTables(left, right);
    }


    @Override
    public String getCollation(Request request, Variable variable, ResourceClass resClass, int index)
    {
        return getCollation(request, List.of(left), variable, resClass, index);
    }


    /**
     * Variable of the enclosing GRAPH clause that the triples of both sides bind to their graph, or null.
     *
     * @return variable of the enclosing GRAPH clause that the triples of both sides bind to their graph, or null
     */
    public final Variable getGraph()
    {
        return graph;
    }


    @Override
    public void generateExplanation(StringBuilder builder, String indent)
    {
        builder.append("minus");

        if(graph != null)
        {
            indentInfo(builder, indent, true);
            builder.append("in graph ");
            builder.append(graph);
        }

        if(solutionBound != null)
        {
            indentInfo(builder, indent, true);
            builder.append("sharing the current solution");
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

        if(!(object instanceof SqlMinus imcode))
            return false;

        if(!super.equals(imcode))
            return false;

        if(!Objects.equals(left, imcode.left))
            return false;

        if(!Objects.equals(right, imcode.right))
            return false;

        if(!Objects.equals(graph, imcode.graph))
            return false;

        if(!Objects.equals(solutionBound, imcode.solutionBound))
            return false;

        return true;
    }


    @Override
    protected int getHashCode()
    {
        return Objects.hash(left, right, graph, solutionBound);
    }
}
