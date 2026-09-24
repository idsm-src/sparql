package cz.iocb.sparql.engine.model;

import cz.iocb.sparql.engine.model.base.BaseElement;
import cz.iocb.sparql.engine.model.expression.Expression;



/**
 * Base type for a pair of expression and variable.
 */
abstract class ExpressionVariableBase extends BaseElement
{
    /**
     * The variable; may be null for a plain GROUP BY expression.
     */
    VariableNode variable;

    /**
     * The expression; may be null for a plain projected variable.
     */
    Expression expression;


    /**
     * Creates the pair; the setters are overridable so subclasses can validate.
     *
     * @param expression the expression
     * @param variable the variable
     */
    public ExpressionVariableBase(Expression expression, VariableNode variable)
    {
        // virtual call in constructor is intentional here
        setExpression(expression);
        setVariable(variable);
    }


    /**
     * The expression, or null.
     *
     * @return the expression, or null
     */
    public Expression getExpression()
    {
        return expression;
    }


    /**
     * Sets the expression.
     *
     * @param expression the expression
     */
    public void setExpression(Expression expression)
    {
        this.expression = expression;
    }


    /**
     * The variable, or null.
     *
     * @return the variable, or null
     */
    public VariableNode getVariable()
    {
        return variable;
    }


    /**
     * Sets the variable.
     *
     * @param variable the variable
     */
    public void setVariable(VariableNode variable)
    {
        this.variable = variable;
    }
}
