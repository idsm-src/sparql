package cz.iocb.sparql.engine.translator.imcode.expression;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdBoolean;
import static cz.iocb.sparql.engine.translator.imcode.expression.SqlLiteral.falseValue;
import static cz.iocb.sparql.engine.translator.imcode.expression.SqlLiteral.trueValue;
import java.util.Set;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.parser.model.expression.BinaryExpression.Operator;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.UsedVariables;
import cz.iocb.sparql.engine.translator.imcode.SqlIntercode.Restrictions;



public class SqlBinaryLogical extends SqlBinary
{
    private static final Set<ResourceClass> operandRequirements = Set.of(xsdBoolean);

    private final Operator operator;


    public SqlBinaryLogical(Operator operator, SqlExpressionIntercode left, SqlExpressionIntercode right,
            Set<ResourceClass> resourceClasses, boolean canBeNull)
    {
        super(left, right, resourceClasses, canBeNull);
        this.operator = operator;
    }


    public static SqlExpressionIntercode create(Operator operator, SqlExpressionIntercode left,
            SqlExpressionIntercode right)
    {
        left = SqlEffectiveBooleanValue.create(left);
        right = SqlEffectiveBooleanValue.create(right);

        if(left == SqlNull.get() && right == SqlNull.get())
            return SqlNull.get();

        if(operator == Operator.Or)
        {
            if(left == falseValue)
                return right;

            if(right == falseValue)
                return left;

            if(left == trueValue || right == trueValue)
                return trueValue;

            if(right == SqlNull.get() && left instanceof SqlBinaryComparison cmp && cmp.isAlwaysFalseOrNull())
                return SqlNull.get();

            if(left == SqlNull.get() && right instanceof SqlBinaryComparison cmp && cmp.isAlwaysFalseOrNull())
                return SqlNull.get();
        }

        if(operator == Operator.And)
        {
            if(left == trueValue)
                return right;

            if(right == trueValue)
                return left;

            if(left == falseValue || right == falseValue)
                return falseValue;

            if(right == SqlNull.get() && left instanceof SqlBinaryComparison cmp && cmp.isAlwaysTrueOrNull())
                return SqlNull.get();

            if(left == SqlNull.get() && right instanceof SqlBinaryComparison cmp && cmp.isAlwaysTrueOrNull())
                return SqlNull.get();
        }

        return new SqlBinaryLogical(operator, left, right, asSet(xsdBoolean), left.canBeNull() || right.canBeNull());
    }


    @Override
    public Restrictions getRequirements(Set<ResourceClass> expected)
    {
        return new Restrictions(getLeft().getRequirements(operandRequirements),
                getRight().getRequirements(operandRequirements));
    }


    @Override
    public SqlExpressionIntercode optimize(Request request, UsedVariables variables, boolean evalServices)
    {
        SqlExpressionIntercode optLeft = getLeft().optimize(request, variables, evalServices);
        SqlExpressionIntercode optRight = getRight().optimize(request, variables, evalServices);

        if(optLeft == getLeft() && optRight == getRight())
            return this;

        return create(operator, optLeft, optRight);
    }


    @Override
    public String translate(Request request)
    {
        return "(" + getLeft().translate(request) + " " + operator.getName() + " " + getRight().translate(request)
                + ")";
    }


    @Override
    public void generateExplanation(StringBuilder builder, String indent, int priority)
    {
        int myPriortity = operator == Operator.Or ? 9 : 8;

        if(myPriortity > priority)
            builder.append("(");

        getLeft().generateExplanation(builder, indent, myPriortity);
        builder.append(" ");
        builder.append(operator.getText());
        builder.append(" ");
        getRight().generateExplanation(builder, indent, myPriortity);

        if(myPriortity > priority)
            builder.append(")");
    }
}
