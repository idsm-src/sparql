package cz.iocb.chemweb.server.sparql.translator.imcode.expression;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdBoolean;
import java.util.LinkedList;
import java.util.List;
import cz.iocb.chemweb.server.sparql.parser.model.expression.BinaryExpression.Operator;
import cz.iocb.chemweb.server.sparql.translator.expression.VariableAccessor;



public class SqlInExpression extends SqlExpressionIntercode
{
    static private class OperandWrapper extends SqlExpressionIntercode
    {
        private SqlExpressionIntercode operand;

        protected OperandWrapper(SqlExpressionIntercode operand)
        {
            super(operand.getResourceClasses(), operand.canBeNull());
            this.operand = operand;
        }

        public static SqlExpressionIntercode create(SqlExpressionIntercode operand)
        {
            if(operand instanceof SqlNodeValue)
                return operand;

            return new OperandWrapper(operand);
        }

        @Override
        public SqlExpressionIntercode optimize(VariableAccessor variableAccessor)
        {
            return create(operand.optimize(variableAccessor));
        }

        @Override
        public String translate()
        {
            return "\"expr\"";
        }
    }


    private boolean negated;
    private SqlExpressionIntercode left;
    private List<SqlExpressionIntercode> rights;
    private SqlExpressionIntercode expression;


    public SqlInExpression(boolean negated, SqlExpressionIntercode left, List<SqlExpressionIntercode> rights,
            SqlExpressionIntercode expression)
    {
        super(asSet(xsdBoolean), expression.canBeNull());

        this.negated = negated;
        this.left = left;
        this.rights = rights;
        this.expression = expression;
    }


    public static SqlExpressionIntercode create(boolean negated, SqlExpressionIntercode left,
            List<SqlExpressionIntercode> rights)
    {
        SqlExpressionIntercode expression = null;

        SqlExpressionIntercode wrappedLeft = OperandWrapper.create(left);
        Operator compareOperator = negated ? Operator.NotEquals : Operator.Equals;
        Operator logicalOperator = negated ? Operator.And : Operator.Or;

        for(SqlExpressionIntercode right : rights)
        {
            SqlExpressionIntercode compare = SqlBinaryComparison.create(compareOperator, wrappedLeft, right);
            expression = expression != null ? SqlBinaryLogical.create(logicalOperator, expression, compare) : compare;
        }

        if(expression instanceof SqlEffectiveBooleanValue || expression instanceof SqlNull)
            return expression;

        return new SqlInExpression(negated, left, rights, expression);
    }


    @Override
    public SqlExpressionIntercode optimize(VariableAccessor variableAccessor)
    {
        List<SqlExpressionIntercode> optimized = new LinkedList<SqlExpressionIntercode>();

        for(SqlExpressionIntercode right : rights)
            optimized.add(right.optimize(variableAccessor));

        return create(negated, left.optimize(variableAccessor), optimized);
    }


    @Override
    public String translate()
    {
        if(left instanceof SqlNodeValue)
            return expression.translate();


        StringBuilder builder = new StringBuilder();
        builder.append("(SELECT ");
        builder.append(expression.translate());
        builder.append(" FROM (VALUES (");
        builder.append(left.translate());
        builder.append(")) AS \"tab\"(\"expr\"))");
        return builder.toString();
    }
}
