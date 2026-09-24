package cz.iocb.sparql.engine.model.expression;

import cz.iocb.sparql.engine.model.base.BaseElement;
import cz.iocb.sparql.engine.model.visitor.ElementVisitor;



/**
 * Represents an expression ({@link #getChild}) enclosed in brackets.
 *
 * <p>
 * Corresponds to the rule [120] BrackettedExpression in the SPARQL grammar.
 */
public class BracketedExpression extends BaseElement implements Expression
{
    /**
     * The enclosed expression.
     */
    private Expression child;

    /**
     * Creates the expression around the child.
     *
     * @param child the enclosed expression
     */
    public BracketedExpression(Expression child)
    {
        setChild(child);
    }


    /**
     * The enclosed expression.
     *
     * @return the enclosed expression
     */
    public Expression getChild()
    {
        return child;
    }


    /**
     * Sets the enclosed expression.
     *
     * @param child the enclosed expression
     */
    public void setChild(Expression child)
    {
        this.child = child;
    }


    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
