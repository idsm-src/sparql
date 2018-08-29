package cz.iocb.chemweb.server.sparql.translator.imcode.expression;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.ExpressionResourceClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.PatternResourceClass;
import cz.iocb.chemweb.server.sparql.translator.expression.VariableAccessor;



public class SqlNumeric extends SqlUnary
{
    private final ExpressionResourceClass requestedClass;


    protected SqlNumeric(ExpressionResourceClass requestedClass, SqlExpressionIntercode operand,
            Set<ExpressionResourceClass> resourceClasses, boolean isBoxed, boolean canBeNull)
    {
        super(operand, resourceClasses, isBoxed, canBeNull);
        this.requestedClass = requestedClass;
    }


    /**
     * Converts the operand on the requested numeric class. Only widening conversions are used. If the requested class
     * is null, the boxed value is returned.
     *
     * Warning: If there is no request on specific numeric type (the requested class is null) and if the operand is
     * boxed, then the result is also boxed, but no special filtration code is generated. However, the result types and
     * canBeNull flag is set as though the operand was filtered.
     *
     * @param operand Operand
     * @param requestedClass Requested numeric class
     * @return Instance of SqlExpressionIntercode
     */
    public static SqlExpressionIntercode create(SqlExpressionIntercode operand, ExpressionResourceClass requestedClass)
    {
        if(operand == SqlNull.get())
            return SqlNull.get();


        Set<ExpressionResourceClass> operandClasses = operand.getResourceClasses();

        if(requestedClass != null && !operand.isBoxed() && operandClasses.contains(requestedClass))
            return operand; // operand already has requested numeric class


        Set<ExpressionResourceClass> compatibleClasses = operandClasses.stream()
                .filter(r -> isNumericCompatibleWith(r, requestedClass)).collect(Collectors.toSet());

        if(compatibleClasses.isEmpty())
            return SqlNull.get();

        if(requestedClass == null && operand.isBoxed() && operandClasses.size() == compatibleClasses.size())
            return operand; // rdfbox to rdfbox cast is not needed


        Set<ExpressionResourceClass> resultClasses = new HashSet<ExpressionResourceClass>();

        if(requestedClass == null)
            resultClasses.addAll(compatibleClasses);
        else
            resultClasses.add(requestedClass);

        return new SqlNumeric(requestedClass, operand, resultClasses, requestedClass == null,
                operand.canBeNull() || operandClasses.size() > compatibleClasses.size());
    }


    @Override
    public SqlExpressionIntercode optimize(VariableAccessor variableAccessor)
    {
        return getOperand().optimize(variableAccessor);
    }


    @Override
    public String translate()
    {
        if(getOperand() instanceof SqlVariable)
        {
            SqlVariable variable = (SqlVariable) getOperand();

            Set<ExpressionResourceClass> compatibleClasses = variable.getResourceClasses().stream()
                    .filter(r -> isNumericCompatibleWith(r, requestedClass)).collect(Collectors.toSet());


            StringBuilder builder = new StringBuilder();
            boolean hasAlternative = false;

            if(compatibleClasses.size() > 1)
                builder.append("COALESCE(");

            for(ExpressionResourceClass resourceClass : compatibleClasses)
            {
                appendComma(builder, hasAlternative);
                hasAlternative = true;

                String code = variable.getExpressionValue((PatternResourceClass) resourceClass, isBoxed());

                if(!isBoxed() && resourceClass != requestedClass)
                {
                    builder.append("sparql.cast_as_");
                    builder.append(requestedClass.getName());
                    builder.append("_from_");
                    builder.append(resourceClass.getName());
                    builder.append("(");
                    builder.append(code);
                    builder.append(")");
                }
                else
                {
                    builder.append(code);
                }
            }

            if(compatibleClasses.size() > 1)
                builder.append(")");

            return builder.toString();
        }
        else
        {
            String code = "(" + getOperand().translate() + ")";

            if(!getOperand().isBoxed())
                return "sparql.cast_as_" + getResourceName() + "_from_" + getOperand().getResourceName() + "(" + code
                        + ")";
            else if(!isBoxed())
                return "sparql.rdfbox_extract_derivated_from_" + getResourceName() + "(" + code + ")";
            else
                return code;
        }
    }
}
