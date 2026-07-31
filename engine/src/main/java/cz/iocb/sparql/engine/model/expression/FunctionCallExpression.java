package cz.iocb.sparql.engine.model.expression;

import java.util.Collection;
import cz.iocb.sparql.engine.model.IriNode;
import cz.iocb.sparql.engine.model.visitor.ElementVisitor;



/**
 * Represents an extension function, defined by an IRI.
 */
public class FunctionCallExpression extends CallExpression
{
    private IriNode function;


    public FunctionCallExpression(IriNode function)
    {
        this.function = function;
    }


    public FunctionCallExpression(IriNode function, Collection<Expression> arguments)
    {
        super(arguments);
        this.function = function;
    }


    public IriNode getFunction()
    {
        return function;
    }


    public void setFunction(IriNode function)
    {
        this.function = function;
    }


    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
