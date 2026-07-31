package cz.iocb.sparql.engine.model;

import cz.iocb.sparql.engine.model.expression.Expression;
import cz.iocb.sparql.engine.model.visitor.ElementVisitor;



/**
 * Used to select the results of a SELECT query.
 */
public class Projection extends ExpressionVariableBase
{
    public Projection(VariableNode variable)
    {
        this(null, variable);
    }


    public Projection(Expression expression, VariableNode variable)
    {
        super(expression, variable);
    }


    @Override
    public void setVariable(VariableNode variable)
    {
        if(variable == null)
            throw new IllegalArgumentException();

        super.setVariable(variable);
    }


    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
