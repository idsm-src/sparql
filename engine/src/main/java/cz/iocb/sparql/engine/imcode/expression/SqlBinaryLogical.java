package cz.iocb.sparql.engine.imcode.expression;

import static cz.iocb.sparql.engine.imcode.expression.SqlBinaryLogical.LogicalOperator.AND;
import static cz.iocb.sparql.engine.imcode.expression.SqlBinaryLogical.LogicalOperator.OR;
import static cz.iocb.sparql.engine.imcode.expression.SqlBooleanExpression.NonConstantBooleanValue.ANY;
import static cz.iocb.sparql.engine.imcode.expression.SqlBooleanExpression.NonConstantBooleanValue.FALSE_OR_ERROR;
import static cz.iocb.sparql.engine.imcode.expression.SqlBooleanExpression.NonConstantBooleanValue.TRUE_OR_ERROR;
import static cz.iocb.sparql.engine.imcode.expression.SqlLiteral.falseValue;
import static cz.iocb.sparql.engine.imcode.expression.SqlLiteral.trueValue;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genBoolean;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdBoolean;
import static java.util.Collections.singletonMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ExpressionColumn;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.VariableBindings;



/**
 * Logical AND and OR with the three-valued SPARQL semantics: an error operand yields an error only when the other
 * operand does not decide the result.
 */
public final class SqlBinaryLogical extends SqlBinary implements SqlBooleanExpression
{
    /**
     * Logical operator with its SPARQL spelling and the suffix of its {@code sparql.*} SQL function.
     */
    public static enum LogicalOperator
    {
        /**
         * Logical or.
         */
        OR("||", "or"),

        /**
         * Logical and.
         */
        AND("&&", "and");

        /**
         * SPARQL spelling.
         */
        private final String text;

        /**
         * Suffix of the SQL function.
         */
        private final String name;

        /**
         * Creates the operator.
         *
         * @param text the text
         * @param name the name
         */
        LogicalOperator(String text, String name)
        {
            this.text = text;
            this.name = name;
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
         * Suffix of the {@code sparql.*} SQL function implementing the operator.
         *
         * @return suffix of the {@code sparql.*} SQL function implementing the operator
         */
        public String getName()
        {
            return name;
        }
    }


    /**
     * The operator.
     */
    private final LogicalOperator operator;

    /**
     * Values the expression can take besides an error.
     */
    private final NonConstantBooleanValue value;


    /**
     * Creates the expression.
     *
     * @param operator the operator
     * @param left the left operand
     * @param right the right operand
     * @param mappings columns per resource class
     * @param canBeNull whether the value may be null
     * @param value values the expression can take besides an error
     */
    private SqlBinaryLogical(LogicalOperator operator, SqlExpressionIntercode left, SqlExpressionIntercode right,
            Map<ResourceClass, List<Column>> mappings, boolean canBeNull, NonConstantBooleanValue value)
    {
        super(left, right, mappings, canBeNull);

        this.operator = operator;
        this.value = value;
    }


    /**
     * Logical expression over boolean operands.
     *
     * @param operator the operator
     * @param left the left operand
     * @param right the right operand
     * @return logical expression over boolean operands
     */
    public static SqlExpressionIntercode create(LogicalOperator operator, SqlExpressionIntercode left,
            SqlExpressionIntercode right)
    {
        return create(operator, left, right, Restriction.ALL);
    }


    /**
     * Logical expression materialising only the needed result; constant operands are folded.
     *
     * @param operator the operator
     * @param left the left operand
     * @param right the right operand
     * @param restriction the result classes the parent needs
     * @return logical expression materialising only the needed result; constant operands are folded
     */
    private static SqlExpressionIntercode create(LogicalOperator operator, SqlExpressionIntercode left,
            SqlExpressionIntercode right, Restriction restriction)
    {
        if(left.equals(SqlNull.get()) && right.equals(SqlNull.get()))
            return SqlNull.get();

        NonConstantBooleanValue value = ANY;

        if(operator == OR)
        {
            if(left.equals(falseValue))
                return right;

            if(right.equals(falseValue))
                return left;

            if(left.equals(trueValue) || right.equals(trueValue))
                return trueValue;

            if(right.equals(SqlNull.get()) && left instanceof SqlBooleanExpression e && e.isFalseOrError())
                return SqlNull.get();

            if(left.equals(SqlNull.get()) && right instanceof SqlBooleanExpression e && e.isFalseOrError())
                return SqlNull.get();

            if(left instanceof SqlBooleanExpression e1 && e1.isFalseOrError()
                    && right instanceof SqlBooleanExpression e2 && e2.isFalseOrError())
                value = FALSE_OR_ERROR;
        }

        if(operator == AND)
        {
            if(left.equals(trueValue))
                return right;

            if(right.equals(trueValue))
                return left;

            if(left.equals(falseValue) || right.equals(falseValue))
                return falseValue;

            if(right.equals(SqlNull.get()) && left instanceof SqlBooleanExpression e && e.isTrueOrError())
                return SqlNull.get();

            if(left.equals(SqlNull.get()) && right instanceof SqlBooleanExpression e && e.isTrueOrError())
                return SqlNull.get();

            if(left instanceof SqlBooleanExpression e1 && e1.isTrueOrError() && right instanceof SqlBooleanExpression e2
                    && e2.isTrueOrError())
                value = TRUE_OR_ERROR;
        }

        List<Column> columns = restriction.contains(xsdBoolean) ? translate(operator, left, right) : null;
        Map<ResourceClass, List<Column>> mappings = singletonMap(xsdBoolean, columns);

        return new SqlBinaryLogical(operator, left, right, mappings, left.canBeNull() || right.canBeNull(), value);
    }


    /**
     * SQL applying the {@code sparql.*} operator to the boolean values of the operands.
     *
     * @param operator the operator
     * @param left the left operand
     * @param right the right operand
     * @return SQL applying the {@code sparql.*} operator to the boolean values of the operands
     */
    private static List<Column> translate(LogicalOperator operator, SqlExpressionIntercode left,
            SqlExpressionIntercode right)
    {
        return List.of(new ExpressionColumn(
                "(" + left.get(genBoolean).get(0) + " " + operator.getName() + " " + right.get(genBoolean).get(0) + ")",
                left.canBeNull() || right.canBeNull()));
    }


    @Override
    public NonConstantBooleanValue getBooleanValue()
    {
        return value;
    }


    @Override
    public SqlExpressionIntercode optimize(Request request, VariableBindings bindings, Restriction restriction,
            boolean evalServices)
    {
        Restriction operandRestriction = new Restriction();

        if(restriction.contains(xsdBoolean))
            operandRestriction.add(genBoolean);

        SqlExpressionIntercode optLeft = left.optimize(request, bindings, operandRestriction, evalServices);
        SqlExpressionIntercode optRight = getRight().optimize(request, bindings, operandRestriction, evalServices);

        if(optLeft == left && optRight == getRight())
            return this;

        return create(operator, optLeft, optRight, restriction);
    }


    @Override
    public void generateExplanation(StringBuilder builder, String indent, int priority)
    {
        int myPriortity = operator == OR ? 9 : 8;

        if(myPriortity > priority)
            builder.append("(");

        left.generateExplanation(builder, indent, myPriortity);
        builder.append(" ");
        builder.append(operator.getText());
        builder.append(" ");
        getRight().generateExplanation(builder, indent, myPriortity);

        if(myPriortity > priority)
            builder.append(")");
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(!(object instanceof SqlBinaryLogical imcode))
            return false;

        if(!super.equals(imcode))
            return false;

        if(!Objects.equals(operator, imcode.operator))
            return false;

        if(!Objects.equals(value, imcode.value))
            return false;

        return true;
    }


    @Override
    protected int getHashCode()
    {
        return Objects.hash(operator, left, right);
    }
}
