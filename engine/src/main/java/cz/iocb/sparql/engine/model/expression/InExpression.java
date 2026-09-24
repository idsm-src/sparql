package cz.iocb.sparql.engine.model.expression;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import cz.iocb.sparql.engine.model.base.BaseElement;
import cz.iocb.sparql.engine.model.visitor.ElementVisitor;



/**
 * An expression that checks, whether an element ({@link #getLeft}) is contained in a collection ({@link #getRight}), or
 * not ({@link #isNegated}).
 *
 * <p>
 * Corresponds to the {@code IN} and {@code NOT IN} cases in the rule [114] RelationalExpression in the SPARQL grammar.
 */
public class InExpression extends BaseElement implements Expression
{
    /**
     * Tested expression.
     */
    private Expression left;

    /**
     * Expressions to compare against.
     */
    private List<Expression> right;

    /**
     * True for NOT IN.
     */
    private boolean negated;


    /**
     * Creates the expression.
     *
     * @param left the left side
     * @param right the right side
     * @param negated whether the test is negated
     */
    public InExpression(Expression left, Collection<Expression> right, boolean negated)
    {
        setLeft(left);
        this.right = new ArrayList<>(right);
        this.negated = negated;
    }


    /**
     * Tested expression.
     *
     * @return tested expression
     */
    public Expression getLeft()
    {
        return left;
    }


    /**
     * Sets the tested expression.
     *
     * @param left the left side
     */
    public void setLeft(Expression left)
    {
        this.left = left;
    }


    /**
     * Expressions to compare against.
     *
     * @return expressions to compare against
     */
    public List<Expression> getRight()
    {
        return right;
    }


    /**
     * True for NOT IN.
     *
     * @return true for NOT IN, false otherwise
     */
    public boolean isNegated()
    {
        return negated;
    }


    /**
     * Sets whether the test is negated.
     *
     * @param negated whether the test is negated
     */
    public void setNegated(boolean negated)
    {
        this.negated = negated;
    }


    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
