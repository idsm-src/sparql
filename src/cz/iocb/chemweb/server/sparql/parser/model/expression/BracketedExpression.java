package cz.iocb.chemweb.server.sparql.parser.model.expression;

import cz.iocb.chemweb.server.sparql.parser.BaseElement;
import cz.iocb.chemweb.server.sparql.parser.ElementVisitor;



/**
 * Represents an expression ({@link #getChild}) enclosed in brackets.
 *
 * <p>
 * Corresponds to the rule [120] BrackettedExpression in the SPARQL grammar.
 */
public class BracketedExpression extends BaseElement implements Expression
{
    private Expression child;

    public BracketedExpression(Expression child)
    {
        setChild(child);
    }

    public Expression getChild()
    {
        return child;
    }

    public void setChild(Expression child)
    {
        if(child == null)
            throw new IllegalArgumentException();

        this.child = child;
    }

    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
