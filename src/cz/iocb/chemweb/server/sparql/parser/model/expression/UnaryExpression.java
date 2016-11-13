package cz.iocb.chemweb.server.sparql.parser.model.expression;

import cz.iocb.chemweb.server.sparql.parser.BaseElement;
import cz.iocb.chemweb.server.sparql.parser.ElementVisitor;



/**
 * Represents a unary expression: single expression ({@link #getOperand}), with an operator before it
 * ({@link #getOperator}).
 *
 * <p>
 * Corresponds to the rule [118] UnaryExpression in the SPARQL grammar, except for the case with no operator.
 */
public class UnaryExpression extends BaseElement implements Expression
{
    public enum Operator
    {
        Not("!"), Plus("+"), Minus("-");

        private final String text;

        Operator(String text)
        {
            this.text = text;
        }

        public String getText()
        {
            return text;
        }
    }

    private Operator operator;
    private Expression operand;

    public UnaryExpression(Operator operator, Expression operand)
    {
        setOperator(operator);
        setOperand(operand);
    }

    public Operator getOperator()
    {
        return operator;
    }

    public void setOperator(Operator operator)
    {
        if(operator == null)
            throw new IllegalArgumentException();

        this.operator = operator;
    }

    public Expression getOperand()
    {
        return operand;
    }

    public void setOperand(Expression operand)
    {
        if(operand == null)
            throw new IllegalArgumentException();

        this.operand = operand;
    }

    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
