package cz.iocb.sparql.engine.imcode;

import static java.util.stream.Collectors.joining;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import cz.iocb.sparql.engine.database.AliasTable;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.VirtualTable;
import cz.iocb.sparql.engine.mapping.classes.ClassRelations;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.VariableBinding;
import cz.iocb.sparql.engine.translator.VariableBindingPair;
import cz.iocb.sparql.engine.translator.VariableBindings;



/**
 * Set difference (MINUS): the solutions of the left side for which there is no compatible solution of the right side
 * sharing at least one bound variable.
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
     * Creates the node.
     *
     * @param bindings the variable bindings
     * @param left the left side
     * @param right the right side
     * @param graph variable of the enclosing GRAPH clause bound by the triples of both sides, or null
     */
    protected SqlMinus(VariableBindings bindings, SqlIntercode left, SqlIntercode right, Variable graph)
    {
        super(bindings, left.isDeterministic() && right.isDeterministic());

        this.left = left;
        this.right = right;
        this.graph = graph;
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
        return minus(request, left, right, null, null);
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
        return minus(request, left, right, graph, null);
    }


    /**
     * Difference exposing only what the parent needs.
     *
     * @param request the current request
     * @param left the left side
     * @param right the right side
     * @param graph variable of the enclosing GRAPH clause bound by the triples of both sides, or null
     * @param restrictions what the parent needs of the variables
     * @return difference exposing only what the parent needs
     */
    protected static SqlIntercode minus(Request request, SqlIntercode left, SqlIntercode right, Variable graph,
            Restrictions restrictions)
    {
        return new SqlMinus(left.getVariableBindings().restrict(request.getConfiguration(), restrictions), left, right,
                graph);
    }


    @Override
    public SqlIntercode optimize(Request request, Restrictions restrictions, boolean reduced, boolean evalServices)
    {
        ClassRelations relations = request.getConfiguration();

        SqlIntercode optLeft = left;
        SqlIntercode optRight = right;

        boolean leftReduced = reduced & optRight.isDeterministic;
        Restrictions leftRestrictions = getJoinRestrictions(relations, optLeft.getVariableBindings(),
                optRight.getVariableBindings(), restrictions);
        Restrictions rightRestrictions = getJoinRestrictions(relations, optRight.getVariableBindings(),
                optLeft.getVariableBindings(), new Restrictions());

        while(true)
        {
            optLeft = optLeft.optimize(request, leftRestrictions, leftReduced, evalServices);
            optRight = optRight.optimize(request, rightRestrictions, true, evalServices);

            if(optRight instanceof SqlUnion union)
            {
                List<SqlIntercode> unionList = new ArrayList<>();

                for(SqlIntercode child : union.getChilds())
                    if(isJoinable(relations, optLeft, child))
                        unionList.add(child);

                if(!unionList.equals(union.getChilds()))
                    optRight = SqlUnion.union(request, unionList).optimize(request, restrictions, true, evalServices);
            }

            boolean newLeftReduced = reduced & optRight.isDeterministic;
            Restrictions newLeftRestrictions = getJoinRestrictions(relations, optLeft.getVariableBindings(),
                    optRight.getVariableBindings(), restrictions);
            Restrictions newRightRestrictions = getJoinRestrictions(relations, optRight.getVariableBindings(),
                    optLeft.getVariableBindings(), new Restrictions());



            if(newLeftReduced == leftReduced && newLeftRestrictions.equals(leftRestrictions)
                    && newRightRestrictions.equals(rightRestrictions))
                break;

            leftRestrictions = newLeftRestrictions;
            rightRestrictions = newRightRestrictions;
            leftReduced = newLeftReduced;
        }


        boolean shareVariables = false;

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
                childs.add(minus(request, child, optRight, graph, restrictions));

            return SqlUnion.union(request, childs).optimize(request, restrictions, reduced, evalServices);
        }


        if(restrictions.isOptimized(relations, bindings) && optLeft == left && optRight == right)
            return this;

        return minus(request, optLeft, optRight, graph, restrictions);
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
     * compatible and at least one of them (the graph variable excluded) is bound on both sides.
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
        return getVirtualTables(left, right);
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

        return true;
    }


    @Override
    protected int getHashCode()
    {
        return Objects.hash(left, right, graph);
    }
}
