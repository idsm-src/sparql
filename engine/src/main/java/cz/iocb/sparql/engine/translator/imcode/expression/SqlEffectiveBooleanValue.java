package cz.iocb.sparql.engine.translator.imcode.expression;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.unsupportedLiteral;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdBoolean;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDecimal;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDouble;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdFloat;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdLong;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdShort;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.engine.translator.imcode.expression.SqlLiteral.falseValue;
import static cz.iocb.sparql.engine.translator.imcode.expression.SqlLiteral.trueValue;
import static java.util.stream.Collectors.toSet;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.parser.model.IRI;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.UsedVariables;
import cz.iocb.sparql.engine.translator.imcode.SqlIntercode.Restrictions;



public class SqlEffectiveBooleanValue extends SqlUnary
{
    private static final Set<ResourceClass> operandRequirements = Set.of(xsdBoolean, xsdShort, xsdInt, xsdLong,
            xsdInteger, xsdDecimal, xsdFloat, xsdDouble, xsdString);

    private static final List<IRI> ebvTypes = List.of(xsdBoolean.getTypeIri(), xsdShort.getTypeIri(),
            xsdInt.getTypeIri(), xsdLong.getTypeIri(), xsdInteger.getTypeIri(), xsdDecimal.getTypeIri(),
            xsdFloat.getTypeIri(), xsdDouble.getTypeIri(), xsdString.getTypeIri());


    protected SqlEffectiveBooleanValue(SqlExpressionIntercode operand, boolean canBeNull)
    {
        super(operand, asSet(xsdBoolean), canBeNull);
    }


    public static SqlExpressionIntercode create(SqlExpressionIntercode operand)
    {
        if(operand instanceof SqlLiteral literal)
        {
            ResourceClass literalClass = operand.getResourceClasses().iterator().next();

            if(!isEffectiveBooleanClass(literalClass))
                return SqlNull.get();

            Object value = literal.getLiteral().getValue();

            if(literalClass == unsupportedLiteral && ebvTypes.contains(literal.getLiteral().getTypeIri()))
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
    public Restrictions getRequirements(Set<ResourceClass> expected)
    {
        return getOperand().getRequirements(operandRequirements);
    }


    @Override
    public SqlExpressionIntercode optimize(Request request, UsedVariables variables, boolean evalServices)
    {
        SqlExpressionIntercode optOperand = getOperand().optimize(request, variables, evalServices);

        if(optOperand == getOperand())
            return this;

        return create(optOperand);
    }


    @Override
    public String translate(Request request)
    {
        SqlExpressionIntercode operand = getOperand();

        if(operand instanceof SqlNodeValue variable)
        {
            Set<ResourceClass> compatibleClasses = variable.getResourceClasses().stream()
                    .filter(r -> isEffectiveBooleanClass(r)).collect(toSet());


            StringBuilder builder = new StringBuilder();
            boolean hasAlternative = false;

            if(compatibleClasses.size() > 1)
                builder.append("coalesce(");

            for(ResourceClass resourceClass : compatibleClasses)
            {
                appendComma(builder, hasAlternative);
                hasAlternative = true;

                List<Column> columns = variable.asResource(request, resourceClass.getGeneralClass());
                Column column = resourceClass.getGeneralClass().toExpression(columns);

                String sqlType = resourceClass.getGeneralClass().getSqlTypes().get(0);

                if(isString(resourceClass))
                    builder.append("(octet_length(" + column + ") != 0)");
                if(isFloat(resourceClass) || isDouble(resourceClass))
                    builder.append("(" + column + " not in ('0'::" + sqlType + ", 'NaN'::" + sqlType + "))");
                else if(isNumeric(resourceClass))
                    builder.append("(" + column + " != '0'::" + sqlType + ")");
                else if(isBoolean(resourceClass))
                    builder.append(column);
                else
                    throw new IllegalArgumentException();
            }

            if(compatibleClasses.size() > 1)
                builder.append(")");

            return builder.toString();
        }
        else if(operand.isBoxed())
        {
            return "sparql.ebv_rdfbox(" + operand.translate(request) + ")";
        }
        else if(operand.getResourceClasses().stream().allMatch(r -> isString(r)))
        {
            String code = operand.getExpressionResourceClass().toGeneralExpression(operand.translate(request));

            return "(octet_length(" + code + ") != 0)";
        }
        else if(operand.getResourceClasses().stream().allMatch(r -> isFloat(r) || isDouble(r)))
        {
            ResourceClass resClass = operand.getExpressionResourceClass();
            String sqlType = resClass.getGeneralClass().getSqlTypes().get(0);
            String code = resClass.toGeneralExpression(operand.translate(request));

            return "(" + code + " NOT IN ('0'::" + sqlType + ", 'NaN'::" + sqlType + "))";
        }
        else if(operand.getResourceClasses().stream().allMatch(r -> isNumeric(r)))
        {
            ResourceClass resClass = operand.getExpressionResourceClass();
            String sqlType = resClass.getGeneralClass().getSqlTypes().get(0);
            String code = resClass.toGeneralExpression(operand.translate(request));

            return "(" + code + " != '0'::" + sqlType + ")";
        }
        else if(operand.getResourceClasses().stream().allMatch(r -> isBoolean(r)))
        {
            return operand.getExpressionResourceClass().toGeneralExpression(operand.translate(request));
        }
        else
        {
            throw new IllegalArgumentException();
        }
    }


    @Override
    public void generateExplanation(StringBuilder builder, String indent, int priority)
    {
        builder.append("evb(");
        getOperand().generateExplanation(builder, indent, 10);
        builder.append(")");
    }
}
