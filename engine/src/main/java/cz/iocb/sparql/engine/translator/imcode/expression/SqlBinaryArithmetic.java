package cz.iocb.sparql.engine.translator.imcode.expression;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDecimal;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDouble;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdFloat;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInteger;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.parser.model.expression.BinaryExpression.Operator;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.UsedVariables;



public class SqlBinaryArithmetic extends SqlBinary
{
    private final Operator operator;


    public SqlBinaryArithmetic(Operator operator, SqlExpressionIntercode left, SqlExpressionIntercode right,
            Set<ResourceClass> resourceClasses, boolean canBeNull)
    {
        super(left, right, resourceClasses, canBeNull);
        this.operator = operator;
    }


    public static SqlExpressionIntercode create(Operator operator, SqlExpressionIntercode left,
            SqlExpressionIntercode right)
    {
        Set<ResourceClass> resultClasses = new HashSet<ResourceClass>();

        for(ResourceClass leftClass : left.getResourceClasses())
            for(ResourceClass rightClass : right.getResourceClasses())
                if(isNumeric(leftClass) && isNumeric(rightClass))
                    resultClasses.add(determineResultClass(operator, leftClass, rightClass));

        if(resultClasses.isEmpty())
            return SqlNull.get();

        return new SqlBinaryArithmetic(operator, left, right, resultClasses, left.canBeNull() || right.canBeNull()
                || left.getResourceClasses().stream().anyMatch(r -> !isNumeric(r))
                || right.getResourceClasses().stream().anyMatch(r -> !isNumeric(r))
                || operator == Operator.Divide && resultClasses.contains(xsdDecimal) && canBeDecimalZero(right));
    }


    private static boolean canBeDecimalZero(SqlExpressionIntercode operand)
    {
        if(!(operand instanceof SqlLiteral literal))
            return true;

        return switch(literal.getLiteral().getValue())
        {
            case Short number -> number == 0;
            case Integer number -> number == 0;
            case Long number -> number == 0l;
            case BigInteger number -> number.equals(BigInteger.ZERO);
            case BigDecimal number -> number.equals(BigDecimal.ZERO);
            default -> false;
        };
    }


    private static ResourceClass determineResultClass(Operator operator, ResourceClass leftClass,
            ResourceClass rightClass)
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
    public SqlExpressionIntercode optimize(Request request, UsedVariables variables)
    {
        SqlExpressionIntercode left = getLeft().optimize(request, variables);
        SqlExpressionIntercode right = getRight().optimize(request, variables);
        return create(operator, left, right);
    }


    @Override
    public String translate(Request request)
    {
        ResourceClass expressionResourceClass = getExpressionResourceClass();

        if(expressionResourceClass == null)
        {
            String leftCode = translateAsBoxedOperand(request, getLeft(),
                    getLeft().getResourceClasses(r -> isNumeric(r)));
            String rightCode = translateAsBoxedOperand(request, getRight(),
                    getRight().getResourceClasses(r -> isNumeric(r)));

            return "(" + leftCode + " operator(sparql." + operator.getText() + ") " + rightCode + ")";
        }
        else
        {
            String leftCode = translateAsUnboxedOperand(request, getLeft(), getExpressionResourceClass());
            String rightCode = translateAsUnboxedOperand(request, getRight(), getExpressionResourceClass());

            return "(" + leftCode + " operator(sparql." + operator.getText() + ") " + rightCode + ")";
        }
    }
}
