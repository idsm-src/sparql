package cz.iocb.chemweb.server.sparql.translator.imcode.expression;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDecimal;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDouble;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdFloat;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdInteger;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.ExpressionResourceClass;
import cz.iocb.chemweb.server.sparql.parser.model.expression.BinaryExpression.Operator;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;
import cz.iocb.chemweb.server.sparql.translator.expression.VariableAccessor;



public class SqlBinaryArithmetic extends SqlBinary
{
    private final Operator operator;


    public SqlBinaryArithmetic(Operator operator, SqlExpressionIntercode left, SqlExpressionIntercode right,
            Set<ExpressionResourceClass> resourceClasses, boolean canBeNull)
    {
        super(left, right, resourceClasses, resourceClasses.size() > 1, canBeNull);
        this.operator = operator;
    }


    public static SqlExpressionIntercode create(Operator operator, SqlExpressionIntercode left,
            SqlExpressionIntercode right)
    {
        Set<ExpressionResourceClass> resultClasses = new HashSet<ExpressionResourceClass>();

        for(ExpressionResourceClass leftClass : left.getResourceClasses())
            for(ExpressionResourceClass rightClass : right.getResourceClasses())
                if(isNumeric(leftClass) && isNumeric(rightClass))
                    resultClasses.add(determineResultClass(operator, leftClass, rightClass));

        if(resultClasses.isEmpty())
            return SqlNull.get();


        ExpressionResourceClass requestedClass = null;

        if(resultClasses.size() == 1)
            requestedClass = resultClasses.iterator().next();

        left = SqlNumeric.create(left, requestedClass);
        right = SqlNumeric.create(right, requestedClass);

        boolean canBeNull = left.canBeNull() || right.canBeNull()
                || operator == Operator.Divide && resultClasses.contains(xsdDecimal) && canBeDecimalZero(right);

        return new SqlBinaryArithmetic(operator, left, right, resultClasses, canBeNull);
    }


    private static boolean canBeDecimalZero(SqlExpressionIntercode operand)
    {
        if(operand instanceof SqlNumeric)
            operand = ((SqlNumeric) operand).getOperand();

        if(!(operand instanceof SqlLiteral))
            return true;

        Literal literal = ((SqlLiteral) operand).getLiteral();
        Object value = literal.getValue();

        if(value instanceof Short)
            return (Short) value == 0;
        else if(value instanceof Integer)
            return (Integer) value == 0;
        else if(value instanceof Long)
            return (Long) value == 0l;
        else if(value instanceof BigInteger)
            return ((BigInteger) value).equals(BigInteger.ZERO);
        else if(value instanceof BigDecimal)
            return ((BigDecimal) value).equals(BigDecimal.ZERO);

        return false;
    }


    private static ExpressionResourceClass determineResultClass(Operator operator, ExpressionResourceClass leftClass,
            ExpressionResourceClass rightClass)
    {
        if(leftClass == xsdDouble || rightClass == xsdDouble)
            return xsdDouble;
        else if(leftClass == xsdFloat || rightClass == xsdFloat)
            return xsdFloat;
        else if(leftClass == xsdDecimal || rightClass == xsdDecimal || operator == Operator.Divide)
            return xsdDecimal;
        else
            return xsdInteger;
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
        String leftCode = getLeft().translate();
        String rightCode = getRight().translate();

        return "sparql." + operator.getName() + "_" + getResourceName() + "(" + leftCode + ", " + rightCode + ")";
    }
}
