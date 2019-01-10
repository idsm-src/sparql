package cz.iocb.chemweb.server.sparql.translator.imcode.expression;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDecimal;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDouble;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdFloat;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdInteger;
import java.util.HashSet;
import java.util.Set;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.translator.expression.VariableAccessor;



public class SqlUnaryArithmetic extends SqlUnary
{
    private final boolean isMinus;


    protected SqlUnaryArithmetic(boolean isMinus, SqlExpressionIntercode operand, Set<ResourceClass> resourceClasses,
            boolean canBeNull)
    {
        super(operand, resourceClasses, resourceClasses.size() > 1, canBeNull);
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
    public SqlExpressionIntercode optimize(VariableAccessor variableAccessor)
    {
        SqlExpressionIntercode operand = getOperand().optimize(variableAccessor);
        return create(isMinus, operand);
    }


    @Override
    public String translate()
    {
        String operandCode = translateAsNumeric(getOperand(), getExpressionResourceClass());

        if(!isMinus)
            return operandCode;

        return "sparql.uminus_" + getResourceName() + "(" + operandCode + ")";
    }
}
