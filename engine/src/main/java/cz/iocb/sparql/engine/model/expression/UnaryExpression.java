package cz.iocb.sparql.engine.model.expression;

import cz.iocb.sparql.engine.model.base.BaseElement;
import cz.iocb.sparql.engine.model.visitor.ElementVisitor;



/**
 * Represents a unary expression: single expression ({@link #getOperand}), with an operator before it
 * ({@link #getOperator}).
 *
 * <p>
 * Corresponds to the rule [135] UnaryExpression in the SPARQL grammar, except for the case with no operator.
 */
public class UnaryExpression extends BaseElement implements Expression
{
    /**
     * Unary operator with its SPARQL spelling ({@link #getText}) and SQL spelling ({@link #getCode}).
     */
    public enum Operator
    {
        /**
         * Logical not.
         */
        Not("!", "NOT "),

        /**
         * Unary plus.
         */
        Plus("+", "+"),

        /**
         * Unary minus.
         */
        Minus("-", "-");

        /**
         * SPARQL spelling.
         */
        private final String text;

        /**
         * SQL spelling.
         */
        private final String code;

        /**
         * Creates the operator with its SPARQL and SQL spellings.
         *
         * @param text the text
         * @param code the SQL spelling
         */
        Operator(String text, String code)
        {
            this.text = text;
            this.code = code;
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


        /**
         * SQL spelling of the operator.
         *
         * @return SQL spelling of the operator
         */
        public String getCode()
        {
            return code;
        }
    }


    /**
     * The operator.
     */
    private Operator operator;

    /**
     * The operand.
     */
    private Expression operand;


    /**
     * Creates the expression.
     *
     * @param operator the operator
     * @param operand the operand
     */
    public UnaryExpression(Operator operator, Expression operand)
    {
        setOperator(operator);
        setOperand(operand);
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
     * Sets the operator.
     *
     * @param operator the operator
     */
    public void setOperator(Operator operator)
    {
        this.operator = operator;
    }


    /**
     * The operand.
     *
     * @return the operand
     */
    public Expression getOperand()
    {
        return operand;
    }


    /**
     * Sets the operand.
     *
     * @param operand the operand
     */
    public void setOperand(Expression operand)
    {
        this.operand = operand;
    }


    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
