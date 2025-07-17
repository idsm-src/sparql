package cz.iocb.sparql.engine.translator.imcode.expression;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDecimal;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDouble;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdFloat;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdLong;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdShort;
import java.util.HashSet;
import java.util.Set;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.UsedVariables;
import cz.iocb.sparql.engine.translator.imcode.SqlIntercode.Restrictions;



public class SqlUnaryArithmetic extends SqlUnary
{
    private final boolean isMinus;


    protected SqlUnaryArithmetic(boolean isMinus, SqlExpressionIntercode operand, Set<ResourceClass> resourceClasses,
            boolean canBeNull)
    {
        super(operand, resourceClasses, canBeNull);
        this.isMinus = isMinus;
    }


    public static SqlExpressionIntercode create(boolean isMinus, SqlExpressionIntercode operand)
    {
        Set<ResourceClass> resultClasses = new HashSet<ResourceClass>();

        for(ResourceClass operandClass : operand.getResourceClasses())
            if(isNumeric(operandClass))
                resultClasses.add(determineResultClass(operandClass));

        if(resultClasses.isEmpty())
            return SqlNull.get();

        return new SqlUnaryArithmetic(isMinus, operand, resultClasses,
                operand.canBeNull() || operand.getResourceClasses().stream().anyMatch(r -> !isNumeric(r)));
    }


    private static ResourceClass determineResultClass(ResourceClass operandClass)
    {
        if(operandClass == xsdDouble || operandClass == xsdFloat || operandClass == xsdDecimal)
            return operandClass;
        else
            return xsdInteger;
    }


    @Override
    public Restrictions getRequirements(Set<ResourceClass> expected)
    {
        Set<ResourceClass> set = new HashSet<ResourceClass>();

        if(expected == null)
        {
            set.add(xsdDouble);
            set.add(xsdFloat);
            set.add(xsdDecimal);
            set.add(xsdInteger);
            set.add(xsdShort);
            set.add(xsdInt);
            set.add(xsdLong);
        }
        else if(expected.contains(xsdDouble))
        {
            set.add(xsdDouble);
        }
        else if(expected.contains(xsdFloat))
        {
            set.add(xsdFloat);
        }
        else if(expected.contains(xsdDecimal))
        {
            set.add(xsdDecimal);
        }
        else if(expected.contains(xsdInteger))
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

        return getOperand().getRequirements(set);
    }


    @Override
    public SqlExpressionIntercode optimize(Request request, UsedVariables variables, boolean evalServices)
    {
        SqlExpressionIntercode optOperand = getOperand().optimize(request, variables, evalServices);

        if(optOperand == getOperand())
            return this;

        return create(isMinus, optOperand);
    }


    @Override
    public String translate(Request request)
    {
        ResourceClass expressionResourceClass = getExpressionResourceClass();

        if(expressionResourceClass == null)
        {
            String code = translateAsBoxedOperand(request, getOperand(),
                    getOperand().getResourceClasses(r -> isNumeric(r)));

            return "(operator(sparql.-) " + code + ")";
        }
        else
        {
            String code = translateAsUnboxedOperand(request, getOperand(), getExpressionResourceClass());

            return "(- " + code + ")";
        }
    }
}
