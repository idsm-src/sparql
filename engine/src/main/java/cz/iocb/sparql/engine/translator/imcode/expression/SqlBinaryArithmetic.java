package cz.iocb.sparql.engine.translator.imcode.expression;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDecimal;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDouble;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdFloat;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdLong;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdShort;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.parser.model.expression.BinaryExpression.Operator;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.UsedVariables;
import cz.iocb.sparql.engine.translator.imcode.SqlIntercode.Restrictions;



public final class SqlBinaryArithmetic extends SqlBinary
{
    private final Operator operator;


    protected SqlBinaryArithmetic(Operator operator, SqlExpressionIntercode left, SqlExpressionIntercode right,
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
        if(isDouble(leftClass) || isDouble(rightClass))
            return xsdDouble;
        else if(isFloat(leftClass) || isFloat(rightClass))
            return xsdFloat;
        else if(isDecimal(leftClass) || isDecimal(rightClass) || operator == Operator.Divide)
            return xsdDecimal;
        else if(isInteger(leftClass) || isInteger(rightClass))
            return xsdInteger;

        throw new IllegalArgumentException();
    }


    @Override
    public Restrictions getRequirements(Set<ResourceClass> expected)
    {
        Set<ResourceClass> set = new HashSet<ResourceClass>();

        if(expected == null || expected.contains(xsdDouble))
        {
            set.add(xsdDouble);
            set.add(xsdFloat);
            set.add(xsdDecimal);
            set.add(xsdInteger);
            set.add(xsdShort);
            set.add(xsdInt);
            set.add(xsdLong);
        }
        else if(expected.contains(xsdFloat))
        {
            set.add(xsdFloat);
            set.add(xsdDecimal);
            set.add(xsdInteger);
            set.add(xsdShort);
            set.add(xsdInt);
            set.add(xsdLong);
        }
        else if(expected.contains(xsdDecimal))
        {
            set.add(xsdDecimal);
            set.add(xsdInteger);
            set.add(xsdShort);
            set.add(xsdInt);
            set.add(xsdLong);
        }
        else if(expected.contains(xsdInteger) && operator != Operator.Divide)
        {
            set.add(xsdInteger);
            set.add(xsdShort);
            set.add(xsdInt);
            set.add(xsdLong);
        }
        else
        {
            return new Restrictions();
        }

        return new Restrictions(left.getRequirements(set), right.getRequirements(set));
    }


    @Override
    public SqlExpressionIntercode optimize(Request request, UsedVariables variables, boolean evalServices)
    {
        SqlExpressionIntercode optLeft = left.optimize(request, variables, evalServices);
        SqlExpressionIntercode optRight = right.optimize(request, variables, evalServices);

        if(optLeft == left && optRight == right)
            return this;

        return create(operator, optLeft, optRight);
    }


    @Override
    public String translate(Request request)
    {
        ResourceClass expressionResourceClass = getExpressionResourceClass();

        if(expressionResourceClass == null)
        {
            String leftCode = translateAsBoxedOperand(request, left, left.getResourceClasses(r -> isNumeric(r)));
            String rightCode = translateAsBoxedOperand(request, right, right.getResourceClasses(r -> isNumeric(r)));

            return "(" + leftCode + " operator(sparql." + operator.getText() + ") " + rightCode + ")";
        }
        else
        {
            String leftCode = translateAsUnboxedOperand(request, left, getExpressionResourceClass());
            String rightCode = translateAsUnboxedOperand(request, right, getExpressionResourceClass());

            return "(" + leftCode + " operator(sparql." + operator.getText() + ") " + rightCode + ")";
        }
    }


    @Override
    public void generateExplanation(StringBuilder builder, String indent, int priority)
    {
        int myPriortity = operator == Operator.Multiply || operator == Operator.Divide ? 4 : 5;

        if(myPriortity > priority)
            builder.append("(");

        left.generateExplanation(builder, indent, myPriortity);
        builder.append(" ");
        builder.append(operator.getText());
        builder.append(" ");
        right.generateExplanation(builder, indent, myPriortity);

        if(myPriortity > priority)
            builder.append(")");
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(!(object instanceof SqlBinaryArithmetic imcode))
            return false;

        if(!super.equals(imcode))
            return false;

        if(!Objects.equals(operator, imcode.operator))
            return false;

        if(!left.equals(imcode.left) || !right.equals(imcode.right))
            return false;

        return true;
    }


    @Override
    protected int getHashCode()
    {
        return Objects.hash(operator, left, right);
    }
}
