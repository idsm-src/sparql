package cz.iocb.chemweb.server.sparql.parser.model;

import cz.iocb.chemweb.server.sparql.parser.ElementVisitor;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Expression;



/**
 * Condition from a GROUP BY clause.
 */
public class GroupCondition extends ExpressionVariableBase
{
    public GroupCondition(Expression expression)
    {
        this(expression, null);
    }

    public GroupCondition(Expression expression, Variable variable)
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
