package cz.iocb.chemweb.server.sparql.parser.model.expression;

import cz.iocb.chemweb.server.sparql.parser.BaseElement;
import cz.iocb.chemweb.server.sparql.parser.ElementVisitor;



/**
 * Represents a binary expression: two expressions ({@link #getLeft}, {@link #getRight}), joined by an operator between
 * them ({@link #getOperator} ).
 *
 * <p>
 * Corresponds to the following rules in the SPARQL grammar, except for the cases with no operators:
 * <ul>
 * <li>[111] ConditionalOrExpression
 * <li>[112] ConditionalAndExpression
 * <li>[114] RelationalExpression, except the IN and NOT IN cases
 * <li>[116] AdditiveExpression
 * <li>[117] MultiplicativeExpression
 * </ul>
 */
public class BinaryExpression extends BaseElement implements Expression
{
    public enum Operator
    {
        Or("||", " OR "),
        And("&&", " AND "),
        Equals("=", "="),
        NotEquals("!=", "<>"),
        LessThan("<", "<"),
        GreaterThan(">", ">"),
        LessThanOrEqual("<=", "<="),
        GreaterThanOrEqual(">=", ">="),
        Multiply("*", "*"),
        Divide("/", "/"),
        Add("+", "+"),
        Subtract("-", "-");

        private final String text;
        private final String code;

        Operator(String text, String code)
        {
            this.text = text;
            this.code = code;
        }

        public String getText()
        {
            return text;
        }

        public String getCode()
        {
            return code;
        }
    }

    private Operator operator;
    private Expression left;
    private Expression right;

    public BinaryExpression(Operator operator, Expression left, Expression right)
    {
        setOperator(operator);
        setLeft(left);
        setRight(right);
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

    public Expression getLeft()
    {
        return left;
    }

    public void setLeft(Expression left)
    {
        if(left == null)
            throw new IllegalArgumentException();

        this.left = left;
    }

    public Expression getRight()
    {
        return right;
    }

    public void setRight(Expression right)
    {
        if(right == null)
            throw new IllegalArgumentException();

        this.right = right;
    }

    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
