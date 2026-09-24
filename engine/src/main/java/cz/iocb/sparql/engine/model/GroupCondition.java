package cz.iocb.sparql.engine.model;

import cz.iocb.sparql.engine.model.expression.Expression;
import cz.iocb.sparql.engine.model.visitor.ElementVisitor;



/**
 * Condition from a GROUP BY clause.
 */
public class GroupCondition extends ExpressionVariableBase
{
    /**
     * Grouping by an expression without a variable.
     *
     * @param expression the expression
     */
    public GroupCondition(Expression expression)
    {
        this(expression, null);
    }


    /**
     * Grouping by an expression bound to a variable ({@code GROUP BY (expr AS ?v)}); the variable may be null.
     *
     * @param expression the expression
     * @param variable the variable
     */
    public GroupCondition(Expression expression, VariableNode variable)
    {
        super(expression, variable);
    }


    @Override
    public void setExpression(Expression expression)
    {
        if(expression == null)
            throw new IllegalArgumentException();

        super.setExpression(expression);
    }


    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
