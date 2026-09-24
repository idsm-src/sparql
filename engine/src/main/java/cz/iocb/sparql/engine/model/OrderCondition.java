package cz.iocb.sparql.engine.model;

import cz.iocb.sparql.engine.model.base.BaseElement;
import cz.iocb.sparql.engine.model.expression.Expression;
import cz.iocb.sparql.engine.model.visitor.ElementVisitor;



/**
 * Condition from an ORDER BY clause.
 */
public class OrderCondition extends BaseElement
{
    /**
     * Direction of the condition.
     */
    public enum Direction
    {
        /**
         * No direction given; ascending by default.
         */
        Unspecified(null),

        /**
         * Explicit ASC.
         */
        Ascending("ASC"),

        /**
         * Explicit DESC.
         */
        Descending("DESC");

        /**
         * SPARQL keyword, null when unspecified.
         */
        private final String text;

        /**
         * Creates the direction with its keyword.
         *
         * @param text the text
         */
        Direction(String text)
        {
            this.text = text;
        }


        /**
         * SPARQL keyword of the direction; null when unspecified.
         *
         * @return SPARQL keyword of the direction; null when unspecified
         */
        public String getText()
        {
            return text;
        }
    }


    /**
     * Sort direction.
     */
    private Direction direction;

    /**
     * Sort expression.
     */
    private Expression expression;


    /**
     * Creates a condition without an explicit direction.
     *
     * @param expression the expression
     */
    public OrderCondition(Expression expression)
    {
        this(Direction.Unspecified, expression);
    }


    /**
     * Creates a condition with the given direction.
     *
     * @param direction the sort direction
     * @param expression the expression
     */
    public OrderCondition(Direction direction, Expression expression)
    {
        setDirection(direction);
        setExpression(expression);
    }


    /**
     * Sort direction.
     *
     * @return sort direction
     */
    public Direction getDirection()
    {
        return direction;
    }


    /**
     * Sets the sort direction (required).
     *
     * @param direction the sort direction
     */
    public void setDirection(Direction direction)
    {
        if(direction == null)
            throw new IllegalArgumentException();

        this.direction = direction;
    }


    /**
     * Sort expression.
     *
     * @return sort expression
     */
    public Expression getExpression()
    {
        return expression;
    }


    /**
     * Sets the sort expression (required).
     *
     * @param expression the expression
     */
    public void setExpression(Expression expression)
    {
        if(expression == null)
            throw new IllegalArgumentException();

        this.expression = expression;
    }


    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
