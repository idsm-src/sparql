package cz.iocb.chemweb.server.sparql.translator.imcode.expression;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdBoolean;
import static cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlEffectiveBooleanValue.falseValue;
import static cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlEffectiveBooleanValue.trueValue;
import java.util.Set;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.parser.model.expression.BinaryExpression.Operator;
import cz.iocb.chemweb.server.sparql.translator.expression.VariableAccessor;



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

        if(left == SqlNull.get() || right == SqlNull.get())
        {
            if(operator == Operator.Or)
            {
                if(left == falseValue || right == falseValue)
                    return SqlNull.get();

                if(left == trueValue || right == trueValue)
                    return trueValue;

                if(left instanceof SqlBinaryComparison && ((SqlBinaryComparison) left).isAlwaysFalseOrNull())
                    return SqlNull.get();

                if(right instanceof SqlBinaryComparison && ((SqlBinaryComparison) right).isAlwaysFalseOrNull())
                    return SqlNull.get();
            }

            if(operator == Operator.And)
            {
                if(left == falseValue || right == falseValue)
                    return falseValue;

                if(left == trueValue || right == trueValue)
                    return SqlNull.get();

                if(left instanceof SqlBinaryComparison && ((SqlBinaryComparison) left).isAlwaysTrueOrNull())
                    return SqlNull.get();

                if(right instanceof SqlBinaryComparison && ((SqlBinaryComparison) right).isAlwaysTrueOrNull())
                    return SqlNull.get();
            }
        }

        return new SqlBinaryLogical(operator, left, right, asSet(xsdBoolean), left.canBeNull() || right.canBeNull());
    }


    @Override
    public SqlExpressionIntercode optimize(VariableAccessor variableAccessor)
    {
        SqlExpressionIntercode left = getLeft().optimize(variableAccessor);
        SqlExpressionIntercode right = getRight().optimize(variableAccessor);
        return create(operator, left, right);
    }


    @Override
    public String translate()
    {
        return "(" + getLeft().translate() + " " + operator.getName() + " " + getRight().translate() + ")";
    }
}
