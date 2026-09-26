package cz.iocb.sparql.engine.model.expression;

import cz.iocb.sparql.engine.model.base.BaseElement;
import cz.iocb.sparql.engine.model.visitor.ElementVisitor;



/**
 * Represents a binary expression: two expressions ({@link #getLeft}, {@link #getRight}), joined by an operator between
 * them ({@link #getOperator} ).
 *
 * <p>
 * Corresponds to the following rules in the SPARQL grammar, except for the cases with no operators:
 * <ul>
 * <li>[128] ConditionalOrExpression
 * <li>[129] ConditionalAndExpression
 * <li>[131] RelationalExpression, except the IN and NOT IN cases
 * <li>[133] AdditiveExpression
 * <li>[134] MultiplicativeExpression
 * </ul>
 */
public class BinaryExpression extends BaseElement implements Expression
{
    /**
     * Binary operator with its SPARQL spelling ({@link #getText}).
     */
    public enum Operator
    {
        /**
         * Logical or.
         */
        Or("||"),

        /**
         * Logical and.
         */
        And("&&"),

        /**
         * Equality.
         */
        Equals("="),

        /**
         * Inequality.
         */
        NotEquals("!="),

        /**
         * Less than.
         */
        LessThan("<"),

        /**
         * Greater than.
         */
        GreaterThan(">"),

        /**
         * Less than or equal.
         */
        LessThanOrEqual("<="),

        /**
         * Greater than or equal.
         */
        GreaterThanOrEqual(">="),

        /**
         * Multiplication.
         */
        Multiply("*"),

        /**
         * Division.
         */
        Divide("/"),

        /**
         * Addition.
         */
        Add("+"),

        /**
         * Subtraction.
         */
        Subtract("-");

        /**
         * SPARQL spelling.
         */
        private final String text;

        /**
         * Creates the operator with its SPARQL spelling.
         *
         * @param text the text
         */
        Operator(String text)
        {
            this.text = text;
        }


        /**
         * SPARQL spelling of the operator.
         *
         * @return SPARQL spelling of the operator
         */
        public String getText()
        {
            return text;
        }
    }


    /**
     * The operator.
     */
    private Operator operator;

    /**
     * Left operand.
     */
    private Expression left;

    /**
     * Right operand.
     */
    private Expression right;


    /**
     * Creates the expression.
     *
     * @param operator the operator
     * @param left the left side
     * @param right the right side
     */
    public BinaryExpression(Operator operator, Expression left, Expression right)
    {
        setOperator(operator);
        setLeft(left);
        setRight(right);
    }


    /**
     * The operator.
     *
     * @return the operator
     */
    public Operator getOperator()
    {
        return operator;
    }


    /**
     * Sets the operator (required).
     *
     * @param operator the operator
     */
    public void setOperator(Operator operator)
    {
        if(operator == null)
            throw new IllegalArgumentException();

        this.operator = operator;
    }


    /**
     * Left operand.
     *
     * @return left operand
     */
    public Expression getLeft()
    {
        return left;
    }


    /**
     * Sets the left operand.
     *
     * @param left the left side
     */
    public void setLeft(Expression left)
    {
        this.left = left;
    }


    /**
     * Right operand.
     *
     * @return right operand
     */
    public Expression getRight()
    {
        return right;
    }


    /**
     * Sets the right operand.
     *
     * @param right the right side
     */
    public void setRight(Expression right)
    {
        this.right = right;
    }


    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
