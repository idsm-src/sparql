package cz.iocb.chemweb.server.sparql.translator.imcode.expression;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.unsupportedLiteral;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdBoolean;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDecimal;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDouble;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdFloat;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdInteger;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdLong;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdShort;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinDataTypes.xsdBooleanType;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toSet;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Set;
import cz.iocb.chemweb.server.sparql.database.Column;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;
import cz.iocb.chemweb.server.sparql.translator.UsedVariables;



public class SqlEffectiveBooleanValue extends SqlUnary
{
    private static final List<IRI> ebvTypes = asList(xsdBoolean.getTypeIri(), xsdShort.getTypeIri(),
            xsdInt.getTypeIri(), xsdLong.getTypeIri(), xsdInteger.getTypeIri(), xsdDecimal.getTypeIri(),
            xsdFloat.getTypeIri(), xsdDouble.getTypeIri(), xsdString.getTypeIri());

    public static final SqlEffectiveBooleanValue trueValue = new SqlEffectiveBooleanValue(
            SqlLiteral.create(new Literal("true", xsdBooleanType)), false);
    public static final SqlEffectiveBooleanValue falseValue = new SqlEffectiveBooleanValue(
            SqlLiteral.create(new Literal("false", xsdBooleanType)), false);


    protected SqlEffectiveBooleanValue(SqlExpressionIntercode operand, boolean canBeNull)
    {
        super(operand, asSet(xsdBoolean), canBeNull);
    }


    public static SqlExpressionIntercode create(SqlExpressionIntercode operand)
    {
        if(operand instanceof SqlLiteral)
        {
            ResourceClass literalClass = operand.getResourceClasses().iterator().next();

            if(!isEffectiveBooleanClass(literalClass))
                return SqlNull.get();

            Literal literal = ((SqlLiteral) operand).getLiteral();
            Object value = literal.getValue();


            if(literalClass == unsupportedLiteral && ebvTypes.contains(literal.getTypeIri()))
                return falseValue;
            else if(literalClass == xsdBoolean)
                return getConstantCode((Boolean) value);
            else if(literalClass == xsdShort)
                return getConstantCode((Short) value != 0);
            else if(literalClass == xsdInt)
                return getConstantCode((Integer) value != 0);
            else if(literalClass == xsdLong)
                return getConstantCode((Long) value != 0l);
            else if(literalClass == xsdInteger)
                return getConstantCode(!((BigInteger) value).equals(BigInteger.ZERO));
            else if(literalClass == xsdDecimal)
                return getConstantCode(!((BigDecimal) value).equals(BigDecimal.ZERO));
            else if(literalClass == xsdFloat)
                return getConstantCode((Float) value != 0 && !((Float) value).isNaN());
            else if(literalClass == xsdDouble)
                return getConstantCode((Double) value != 0 && !((Double) value).isNaN());
            else if(literalClass == xsdString)
                return getConstantCode(!((String) value).isEmpty());
            else
                return SqlNull.get();
        }

        if(operand instanceof SqlIri)
            return SqlNull.get();

        if(operand == SqlNull.get())
            return SqlNull.get();


        Set<ResourceClass> operandClasses = operand.getResourceClasses();

        if(!operand.isBoxed() && operandClasses.contains(xsdBoolean))
            return operand;


        Set<ResourceClass> compatibleClasses = operandClasses.stream().filter(r -> isEffectiveBooleanClass(r))
                .collect(toSet());

        if(compatibleClasses.isEmpty())
            return SqlNull.get();


        return new SqlEffectiveBooleanValue(operand,
                operand.canBeNull() || operandClasses.size() > compatibleClasses.size());
    }


    private static SqlExpressionIntercode getConstantCode(boolean constant)
    {
        return constant ? trueValue : falseValue;
    }


    @Override
    public SqlExpressionIntercode optimize(UsedVariables variables)
    {
        return create(getOperand().optimize(variables));
    }


    @Override
    public String translate()
    {
        if(getOperand() instanceof SqlVariable)
        {
            SqlVariable variable = (SqlVariable) getOperand();

            Set<ResourceClass> compatibleClasses = variable.getResourceClasses().stream()
                    .filter(r -> isEffectiveBooleanClass(r)).collect(toSet());


            StringBuilder builder = new StringBuilder();
            boolean hasAlternative = false;

            if(compatibleClasses.size() > 1)
                builder.append("coalesce(");

            for(ResourceClass resourceClass : variable.getResourceClasses())
            {
                if(!compatibleClasses.contains(resourceClass))
                    continue;

                appendComma(builder, hasAlternative);
                hasAlternative = true;

                Column column = variable.getExpressionValue(resourceClass, isBoxed());

                if(resourceClass != xsdBoolean)
                {
                    builder.append("sparql.ebv_");
                    builder.append(resourceClass.getName());
                    builder.append("(");
                    builder.append(column);
                    builder.append(")");
                }
                else
                {
                    builder.append(column);
                }
            }

            if(compatibleClasses.size() > 1)
                builder.append(")");

            return builder.toString();
        }
        else if(getOperand().isBoxed() || !getOperand().getResourceClasses().contains(xsdBoolean))
        {
            return "sparql.ebv_" + getOperand().getResourceName() + "(" + getOperand().translate() + ")";
        }
        else
        {
            return getOperand().translate();
        }
    }
}
