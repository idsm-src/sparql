package cz.iocb.chemweb.server.sparql.translator.imcode.expression;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.iri;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.rdfLangString;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.unsupportedLiteral;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdBoolean;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDate;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDateTime;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDayTimeDuration;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDecimal;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDouble;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdFloat;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdInteger;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdLong;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdShort;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdString;
import java.util.Set;
import java.util.stream.Collectors;
import cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses;
import cz.iocb.chemweb.server.sparql.mapping.classes.DateConstantZoneClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.DateTimeConstantZoneClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.IriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.LiteralClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.translator.expression.VariableAccessor;



public class SqlCast extends SqlUnary
{
    private final ResourceClass resourceClass;


    protected SqlCast(SqlExpressionIntercode operand, ResourceClass resourceClass, Set<ResourceClass> resourceClasses,
            boolean canBeNull)
    {
        super(operand, resourceClasses, canBeNull);
        this.resourceClass = resourceClass;
    }


    public static SqlExpressionIntercode create(ResourceClass resourceClass, SqlExpressionIntercode operand)
    {
        if(operand.getResourceClasses().size() == 1 && operand.getResourceClasses().contains(resourceClass))
            return operand;

        if(resourceClass == xsdDateTime && operand.getResourceClasses().stream().allMatch(r -> isDateTime(r)))
            return operand;

        if(resourceClass == xsdDate && operand.getResourceClasses().stream().allMatch(r -> isDate(r)))
            return operand;


        Set<ResourceClass> resultClasses = operand.getResourceClasses().stream()
                .map(r -> resultCastClass(r, resourceClass)).filter(r -> r != null).collect(Collectors.toSet());

        if(resultClasses.isEmpty())
            return SqlNull.get();

        boolean canBeNull = operand.getResourceClasses().stream().anyMatch(r -> canBeNull(r, resourceClass));

        return new SqlCast(operand, resourceClass, resultClasses, operand.canBeNull() || canBeNull);
    }


    @Override
    public SqlExpressionIntercode optimize(VariableAccessor variableAccessor)
    {
        return create(resourceClass, getOperand().optimize(variableAccessor));
    }


    @Override
    public String translate()
    {
        if(getOperand().isBoxed() == false && getOperand().getResourceClasses().stream().allMatch(r -> isIri(r)))
            return getOperand().translate(); // iri is already a string


        if(!(getOperand() instanceof SqlVariable))
        {
            ResourceClass operandClass = getOperand().getExpressionResourceClass();
            StringBuilder builder = new StringBuilder();

            if(operandClass instanceof DateTimeConstantZoneClass)
            {
                builder.append("sparql.cast_as_");
                builder.append(getResourceName());
                builder.append("_from_datetime(");
                builder.append(getOperand().translate());
                builder.append(", '");
                builder.append(((DateTimeConstantZoneClass) operandClass).getZone());
                builder.append("'::int4)");
            }
            else if(operandClass instanceof DateConstantZoneClass)
            {
                builder.append("sparql.cast_as_");
                builder.append(getResourceName());
                builder.append("_from_date(");
                builder.append(getOperand().translate());
                builder.append(", '");
                builder.append(((DateConstantZoneClass) operandClass).getZone());
                builder.append("'::int4)");
            }
            else if(operandClass == iri)
            {
                builder.append(getOperand().translate());
            }
            else
            {
                builder.append("sparql.cast_as_");
                builder.append(getResourceName());
                builder.append("_from_");
                builder.append(getOperand().getResourceName());
                builder.append("(");
                builder.append(getOperand().translate());
                builder.append(")");
            }

            return builder.toString();
        }



        SqlVariable variable = (SqlVariable) getOperand();
        ResourceClass castClass = getExpressionResourceClass();
        StringBuilder builder = new StringBuilder();

        Set<ResourceClass> convertible = variable.getResourceClasses().stream()
                .filter(r -> resultCastClass(r, resourceClass) != null).collect(Collectors.toSet());

        if(convertible.size() > 1)
            builder.append("COALESCE(");

        boolean hasAlternative = false;

        for(ResourceClass resClass : convertible)
        {
            appendComma(builder, hasAlternative);
            hasAlternative = true;

            /* identity cats */
            if(resClass == castClass)
            {
                builder.append(variable.getExpressionValue(resClass, false));
            }

            /* special casts from datetime */
            else if(resClass == xsdDateTime && castClass == xsdDate)
            {
                builder.append("sparql.cast_as_date_from_datetime(");
                builder.append(variable.getVariableAccessor().getSqlVariableAccess(variable.getName(), resClass, 0));
                builder.append(", ");
                builder.append(variable.getVariableAccessor().getSqlVariableAccess(variable.getName(), resClass, 1));
                builder.append(")");
            }
            else if(resClass instanceof DateTimeConstantZoneClass && castClass == xsdDate)
            {
                builder.append("sparql.cast_as_date_from_datetime(");
                builder.append(variable.getExpressionValue(resClass, false));
                builder.append(", '");
                builder.append(((DateTimeConstantZoneClass) resClass).getZone());
                builder.append("'::int4)");
            }
            else if(resClass instanceof DateTimeConstantZoneClass && castClass == xsdDateTime)
            {
                builder.append("sparql.zoneddatetime_create(");
                builder.append(variable.getExpressionValue(resClass, false));
                builder.append(", '");
                builder.append(((DateTimeConstantZoneClass) resClass).getZone());
                builder.append("'::int4)");
            }
            else if(resClass instanceof DateTimeConstantZoneClass && castClass instanceof DateConstantZoneClass)
            {
                builder.append("sparql.cast_as_plain_date_from_datetime(");
                builder.append(variable.getExpressionValue(resClass, false));
                builder.append(", '");
                builder.append(((DateTimeConstantZoneClass) resClass).getZone());
                builder.append("'::int4)");
            }

            /* special casts from date */
            else if(resClass == xsdDate && castClass == xsdDateTime)
            {
                builder.append("sparql.cast_as_datetime_from_date(");
                builder.append(variable.getVariableAccessor().getSqlVariableAccess(variable.getName(), resClass, 0));
                builder.append(", ");
                builder.append(variable.getVariableAccessor().getSqlVariableAccess(variable.getName(), resClass, 1));
                builder.append(")");
            }
            else if(resClass instanceof DateConstantZoneClass && castClass == xsdDateTime)
            {
                builder.append("sparql.cast_as_datetime_from_date(");
                builder.append(variable.getExpressionValue(resClass, false));
                builder.append(", '");
                builder.append(((DateConstantZoneClass) resClass).getZone());
                builder.append("'::int4)");
            }
            else if(resClass instanceof DateConstantZoneClass && castClass == xsdDate)
            {
                builder.append("sparql.zoneddate_create(");
                builder.append(variable.getExpressionValue(resClass, false));
                builder.append(", '");
                builder.append(((DateConstantZoneClass) resClass).getZone());
                builder.append("'::int4)");
            }
            else if(resClass instanceof DateConstantZoneClass && castClass instanceof DateTimeConstantZoneClass)
            {
                builder.append("sparql.cast_as_plain_datetime_from_date(");
                builder.append(variable.getExpressionValue(resClass, false));
                builder.append(", '");
                builder.append(((DateConstantZoneClass) resClass).getZone());
                builder.append("'::int4)");
            }

            /* special to-string casts */
            else if(resClass == xsdDate && castClass == xsdString)
            {
                builder.append("sparql.cast_as_string_from_date(");
                builder.append(variable.getVariableAccessor().getSqlVariableAccess(variable.getName(), resClass, 0));
                builder.append(", ");
                builder.append(variable.getVariableAccessor().getSqlVariableAccess(variable.getName(), resClass, 1));
                builder.append(")");
            }
            else if(resClass == xsdDateTime && castClass == xsdString)
            {
                builder.append("sparql.cast_as_string_from_datetime(");
                builder.append(variable.getVariableAccessor().getSqlVariableAccess(variable.getName(), resClass, 0));
                builder.append(", ");
                builder.append(variable.getVariableAccessor().getSqlVariableAccess(variable.getName(), resClass, 1));
                builder.append(")");
            }
            else if(resClass instanceof DateTimeConstantZoneClass && castClass == xsdString)
            {
                builder.append("sparql.cast_as_string_from_datetime(");
                builder.append(variable.getExpressionValue(resClass, false));
                builder.append(", '");
                builder.append(((DateTimeConstantZoneClass) resClass).getZone());
                builder.append("'::int4)");
            }
            else if(resClass instanceof DateConstantZoneClass && castClass == xsdString)
            {
                builder.append("sparql.cast_as_string_from_date(");
                builder.append(variable.getExpressionValue(resClass, false));
                builder.append(", '");
                builder.append(((DateConstantZoneClass) resClass).getZone());
                builder.append("'::int4)");
            }
            else if(resClass instanceof IriClass)
            {
                builder.append(variable.getExpressionValue(resClass, false));
            }
            else if(resClass == unsupportedLiteral)
            {
                builder.append(variable.getVariableAccessor().getSqlVariableAccess(variable.getName(), resClass, 0));
            }

            /* standard literal casts */
            else if(resClass instanceof LiteralClass)
            {
                builder.append("sparql.cast_as_");
                builder.append(getResourceName());
                builder.append("_from_");
                builder.append(resClass.getName());
                builder.append("(");
                builder.append(variable.getExpressionValue(resClass, false));
                builder.append(")");
            }
            else
            {
                assert false;
            }
        }

        if(convertible.size() > 1)
            builder.append(")");

        return builder.toString();
    }


    private static ResourceClass resultCastClass(ResourceClass from, ResourceClass to)
    {
        assert BuiltinClasses.getLiteralClasses().contains(to);

        if(from == rdfLangString || from == unsupportedLiteral)
            return null;

        if(isIri(from) && to == xsdString)
            return xsdString;

        if(!BuiltinClasses.getLiteralClasses().contains(from) && !isDateTime(from) && !isDate(from))
            return null;

        if(from == to)
            return to;

        if(from == xsdString || to == xsdString)
            return to;

        if(from == xsdDayTimeDuration || to == xsdDayTimeDuration)
            return null;

        if(isDateTime(from))
        {
            if(to == xsdDateTime)
                return from;

            if(to == xsdDate && from == xsdDateTime)
                return to;

            if(to == xsdDate)
                return DateConstantZoneClass.get(((DateTimeConstantZoneClass) from).getZone());

            return null;
        }

        if(isDate(from))
        {
            if(to == xsdDate)
                return from;

            if(to == xsdDateTime && from == xsdDate)
                return to;

            if(to == xsdDateTime)
                return DateTimeConstantZoneClass.get(((DateConstantZoneClass) from).getZone());

            return null;
        }

        if(to == xsdDateTime || to == xsdDate)
            return null;

        return to;
    }


    private static boolean canBeNull(ResourceClass from, ResourceClass to)
    {
        // https://www.w3.org/TR/xpath-functions/#casting-from-primitive-to-primitive

        if(from == rdfLangString || from == unsupportedLiteral)
            return true;

        if(from == iri && to == xsdString)
            return false;

        if(!BuiltinClasses.getLiteralClasses().contains(from))
            return true;

        if(from == to)
            return false;

        if(to == xsdString)
            return false;

        if(isDate(from) && isDateTime(to))
            return true;

        if((isDateTime(from) || isDate(from)) && (isDateTime(to) || isDate(to)))
            return false;

        if(isNumeric(from) && to == xsdBoolean || from == xsdBoolean && isNumeric(to))
            return false;

        if(isNumeric(from) && (to == xsdFloat || to == xsdDouble))
            return false;

        if((from == xsdDecimal || from == xsdInteger) && (to == xsdDecimal || to == xsdInteger))
            return false;

        if(from == xsdLong && isNumeric(to) && to != xsdInt && to != xsdShort)
            return false;

        if(from == xsdInt && isNumeric(to) && to != xsdShort)
            return false;

        if(from == xsdShort && isNumeric(to))
            return false;

        return true;
    }
}
