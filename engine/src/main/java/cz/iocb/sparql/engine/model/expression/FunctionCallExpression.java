package cz.iocb.sparql.engine.model.expression;

import java.util.Collection;
import cz.iocb.sparql.engine.model.IriNode;
import cz.iocb.sparql.engine.model.visitor.ElementVisitor;



/**
 * Represents an extension function, defined by an IRI.
 */
public class FunctionCallExpression extends CallExpression
{
    /**
     * IRI of the function.
     */
    private IriNode function;


    /**
     * Creates a call without arguments.
     *
     * @param function IRI of the function
     */
    public FunctionCallExpression(IriNode function)
    {
        this.function = function;
    }


    /**
     * Creates a call with the given arguments.
     *
     * @param function IRI of the function
     * @param arguments the arguments
     */
    public FunctionCallExpression(IriNode function, Collection<Expression> arguments)
    {
        super(arguments);
        this.function = function;
    }


    /**
     * IRI of the function.
     *
     * @return IRI of the function
     */
    public IriNode getFunction()
    {
        return function;
    }


    /**
     * Sets the IRI of the function.
     *
     * @param function IRI of the function
     */
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
