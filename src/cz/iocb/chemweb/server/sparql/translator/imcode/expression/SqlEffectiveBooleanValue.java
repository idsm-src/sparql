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
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdBooleanIri;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdDecimalIri;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdDoubleIri;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdFloatIri;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdIntIri;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdIntegerIri;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdLongIri;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdShortIri;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdStringIri;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;
import cz.iocb.chemweb.server.sparql.translator.expression.VariableAccessor;



public class SqlEffectiveBooleanValue extends SqlUnary
{
    private static final List<IRI> ebvTypes = Arrays.asList(xsdBooleanIri, xsdShortIri, xsdIntIri, xsdLongIri,
            xsdIntegerIri, xsdDecimalIri, xsdFloatIri, xsdDoubleIri, xsdStringIri);

    public static final SqlEffectiveBooleanValue trueValue;
    public static final SqlEffectiveBooleanValue falseValue;
    public static final Set<ResourceClass> booleanClassSet;


    static
    {
        booleanClassSet = new HashSet<ResourceClass>();
        booleanClassSet.add(xsdBoolean);

        SqlExpressionIntercode trueOperand = SqlLiteral.create(new Literal("true", xsdBooleanIri));
        SqlExpressionIntercode falseOperand = SqlLiteral.create(new Literal("false", xsdBooleanIri));

        trueValue = new SqlEffectiveBooleanValue(trueOperand, false);
        falseValue = new SqlEffectiveBooleanValue(falseOperand, false);
    }


    protected SqlEffectiveBooleanValue(SqlExpressionIntercode operand, boolean canBeNull)
    {
        super(operand, booleanClassSet, canBeNull);
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
                .collect(Collectors.toSet());

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

            Set<ResourceClass> compatibleClasses = variable.getResourceClasses().stream()
                    .filter(r -> isEffectiveBooleanClass(r)).collect(Collectors.toSet());


            StringBuilder builder = new StringBuilder();
            boolean hasAlternative = false;

            if(compatibleClasses.size() > 1)
                builder.append("COALESCE(");

            for(ResourceClass resourceClass : variable.getResourceClasses())
            {
                if(!compatibleClasses.contains(resourceClass))
                    continue;

                appendComma(builder, hasAlternative);
                hasAlternative = true;

                String code = variable.getExpressionValue(resourceClass, isBoxed());

                if(resourceClass != xsdBoolean)
                {
                    builder.append("sparql.ebv_");
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
