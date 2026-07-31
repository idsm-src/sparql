package cz.iocb.sparql.engine.model;

import cz.iocb.sparql.engine.model.base.BaseElement;
import cz.iocb.sparql.engine.model.expression.Expression;



/**
 * Base type for a pair of expression and variable.
 */
abstract class ExpressionVariableBase extends BaseElement
{
    VariableNode variable;
    Expression expression;


    public ExpressionVariableBase(Expression expression, VariableNode variable)
    {
        // virtual call in constructor is intentional here
        setExpression(expression);
        setVariable(variable);
    }


    public Expression getExpression()
    {
        return expression;
    }


    public void setExpression(Expression expression)
    {
        this.expression = expression;
    }


    public VariableNode getVariable()
    {
        return variable;
    }


    public void setVariable(VariableNode variable)
    {
        this.variable = variable;
    }
}
