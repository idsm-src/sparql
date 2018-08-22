package cz.iocb.chemweb.server.sparql.translator.imcode.expression;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDecimal;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDouble;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdFloat;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdInteger;
import java.util.HashSet;
import java.util.Set;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.ExpressionResourceClass;
import cz.iocb.chemweb.server.sparql.translator.expression.VariableAccessor;



public class SqlUnaryArithmetic extends SqlUnary
{
    private final boolean isMinus;


    protected SqlUnaryArithmetic(boolean isMinus, SqlExpressionIntercode operand,
            Set<ExpressionResourceClass> resourceClasses, boolean canBeNull)
    {
        super(operand, resourceClasses, resourceClasses.size() > 1, canBeNull);
        this.isMinus = isMinus;
    }


    public static SqlExpressionIntercode create(boolean isMinus, SqlExpressionIntercode operand)
    {
        Set<ExpressionResourceClass> resultClasses = new HashSet<ExpressionResourceClass>();

        for(ExpressionResourceClass operandClass : operand.getResourceClasses())
            if(isNumeric(operandClass))
                resultClasses.add(determineResultClass(operandClass));

        if(resultClasses.isEmpty())
            return SqlNull.get();


        ExpressionResourceClass requestedClass = null;

        if(resultClasses.size() == 1)
            requestedClass = resultClasses.iterator().next();

        operand = SqlNumeric.create(operand, requestedClass);

        boolean canBeNull = operand.canBeNull();

        return new SqlUnaryArithmetic(isMinus, operand, resultClasses, canBeNull);
    }


    private static ExpressionResourceClass determineResultClass(ExpressionResourceClass operandClass)
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
        if(!isMinus)
            getOperand().translate();

        return "sparql.uminus_" + getResourceName() + "(" + getOperand().translate() + ")";
    }
}
