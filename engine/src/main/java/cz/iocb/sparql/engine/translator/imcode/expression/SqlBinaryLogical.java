package cz.iocb.sparql.engine.translator.imcode.expression;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdBoolean;
import static cz.iocb.sparql.engine.translator.imcode.expression.SqlLiteral.falseValue;
import static cz.iocb.sparql.engine.translator.imcode.expression.SqlLiteral.trueValue;
import java.util.Set;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.parser.model.expression.BinaryExpression.Operator;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.UsedVariables;



public class SqlBinaryLogical extends SqlBinary
{
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
    public SqlExpressionIntercode optimize(Request request, UsedVariables variables)
    {
        SqlExpressionIntercode left = getLeft().optimize(request, variables);
        SqlExpressionIntercode right = getRight().optimize(request, variables);
        return create(operator, left, right);
    }


    @Override
    public String translate(Request request)
    {
        return "(" + getLeft().translate(request) + " " + operator.getName() + " " + getRight().translate(request)
                + ")";
    }
}
