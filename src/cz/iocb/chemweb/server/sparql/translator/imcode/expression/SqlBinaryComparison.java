package cz.iocb.chemweb.server.sparql.translator.imcode.expression;

import static cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlEffectiveBooleanValue.booleanClassSet;
import cz.iocb.chemweb.server.sparql.parser.model.expression.BinaryExpression.Operator;
import cz.iocb.chemweb.server.sparql.translator.expression.VariableAccessor;



public abstract class SqlBinaryComparison extends SqlBinary
{
    protected final Operator operator;
    private final boolean isAlwaysDifferentIfNotNull;


    public SqlBinaryComparison(Operator operator, SqlExpressionIntercode left, SqlExpressionIntercode right,
            boolean canBeNull, boolean isAlwaysDifferentIfNotNull)
    {
        super(left, right, booleanClassSet, false, canBeNull);
        this.operator = operator;
        this.isAlwaysDifferentIfNotNull = isAlwaysDifferentIfNotNull;
    }


    public static SqlExpressionIntercode create(Operator operator, SqlExpressionIntercode left,
            SqlExpressionIntercode right)
    {
        if(left instanceof SqlNodeValue && right instanceof SqlNodeValue)
            return SqlNodeBinaryComparison.create(operator, (SqlNodeValue) left, (SqlNodeValue) right);
        else
            return SqlCommonBinaryComparison.create(operator, left, right);
    }


    @Override
    public SqlExpressionIntercode optimize(VariableAccessor variableAccessor)
    {
        SqlExpressionIntercode left = getLeft().optimize(variableAccessor);
        SqlExpressionIntercode right = getRight().optimize(variableAccessor);
        return create(operator, left, right);
    }


    public boolean isAlwaysFalseOrNull()
    {
        return operator == Operator.Equals && isAlwaysDifferentIfNotNull;
    }


    public boolean isAlwaysTrueOrNull()
    {
        return operator == Operator.NotEquals && isAlwaysDifferentIfNotNull;
    }
}
