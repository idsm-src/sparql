package cz.iocb.chemweb.server.sparql.parser.model;

import cz.iocb.chemweb.server.sparql.parser.BaseElement;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Expression;



/**
 * Base type for a pair of expression and variable.
 */
abstract class ExpressionVariableBase extends BaseElement
{
    Variable variable;
    Expression expression;

    public ExpressionVariableBase(Expression expression, Variable variable)
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

    public Variable getVariable()
    {
        return variable;
    }

    public void setVariable(Variable variable)
    {
        this.variable = variable;
    }
}
