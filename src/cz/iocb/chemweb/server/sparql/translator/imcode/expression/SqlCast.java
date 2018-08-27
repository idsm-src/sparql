package cz.iocb.chemweb.server.sparql.translator.imcode.expression;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.iri;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.rdfLangStringExpr;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.unsupportedLiteralExpr;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdBoolean;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDate;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDateExpr;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDateTime;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDateTimeExpr;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDayTimeDuration;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDecimal;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDouble;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdFloat;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdInteger;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdLong;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdShort;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdString;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses;
import cz.iocb.chemweb.server.sparql.mapping.classes.DatePatternClassWithConstantZone;
import cz.iocb.chemweb.server.sparql.mapping.classes.DateTimePatternClassWithConstantZone;
import cz.iocb.chemweb.server.sparql.mapping.classes.IriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.UnsupportedLiteralClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.DateClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.DateTimeClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.ExpressionLiteralClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.ExpressionResourceClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.LangStringClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.PatternResourceClass;
import cz.iocb.chemweb.server.sparql.translator.expression.VariableAccessor;



public class SqlCast extends SqlUnary
{
    private final ExpressionResourceClass resourceClass;


    protected SqlCast(SqlExpressionIntercode operand, Set<ExpressionResourceClass> resourceClasses, boolean canBeNull)
    {
        super(operand, resourceClasses, false, canBeNull);
        resourceClass = resourceClasses.iterator().next();
    }


    public static SqlExpressionIntercode create(ExpressionResourceClass resourceClass, SqlExpressionIntercode operand)
    {
        if(operand.isBoxed() == false && operand.getResourceClasses().contains(resourceClass))
            return operand;

        Set<ExpressionResourceClass> compatible = operand.getResourceClasses().stream()
                .filter(r -> canBeCast(r, resourceClass)).collect(Collectors.toSet());

        if(compatible.isEmpty())
            return SqlNull.get();

        boolean canBeNull = operand.getResourceClasses().stream().anyMatch(r -> canBeNull(r, resourceClass));

        Set<ExpressionResourceClass> resourceClasses = new HashSet<ExpressionResourceClass>();
        resourceClasses.add(resourceClass);

        return new SqlCast(operand, resourceClasses, operand.canBeNull() || canBeNull);
    }


    @Override
    public SqlExpressionIntercode optimize(VariableAccessor variableAccessor)
    {
        return create(resourceClass, getOperand().optimize(variableAccessor));
    }


    @Override
    public String translate()
    {
        if(getOperand().isBoxed() == false && getOperand().getResourceClasses().contains(iri))
            return getOperand().translate();

        if(getOperand() instanceof SqlVariable)
        {
            SqlVariable variable = (SqlVariable) getOperand();

            Set<PatternResourceClass> convertible = variable.getPatternResourceClasses().stream()
                    .filter(r -> canBeCast(r.getExpressionResourceClass(), resourceClass)).collect(Collectors.toSet());

            StringBuilder builder = new StringBuilder();

            if(convertible.size() > 1)
                builder.append("COALESCE(");

            boolean hasAlternative = false;
            for(PatternResourceClass resClass : convertible)
            {
                appendComma(builder, hasAlternative);
                hasAlternative = true;

                if(resClass == resourceClass)
                {
                    builder.append(variable.getExpressionValue(resClass, false));
                }
                else if(resClass instanceof ExpressionLiteralClass)
                {
                    builder.append("sparql.cast_as_");
                    builder.append(getResourceName());
                    builder.append("_from_");
                    builder.append(resClass.getName());
                    builder.append("(");
                    builder.append(variable.getExpressionValue(resClass, false));
                    builder.append(")");
                }
                else if(resClass instanceof DateTimeClass && resourceClass == xsdDateTimeExpr)
                {
                    builder.append("sparql.zoneddatetime_create(");
                    builder.append(variable.getVariableAccessor().variableAccess(variable.getName(), resClass, 0));
                    builder.append(", ");

                    if(resClass == xsdDateTime)
                        builder.append(variable.getVariableAccessor().variableAccess(variable.getName(), resClass, 1));
                    else
                        builder.append("'" + ((DateTimePatternClassWithConstantZone) resClass).getZone() + "'::int4");

                    builder.append(")");
                }
                else if(resClass instanceof DateClass && resourceClass == xsdDateExpr)
                {
                    builder.append("sparql.zoneddate_create(");
                    builder.append(variable.getVariableAccessor().variableAccess(variable.getName(), resClass, 0));
                    builder.append(", ");

                    if(resClass == xsdDate)
                        builder.append(variable.getVariableAccessor().variableAccess(variable.getName(), resClass, 1));
                    else
                        builder.append("'" + ((DatePatternClassWithConstantZone) resClass).getZone() + "'::int4");

                    builder.append(")");
                }
                else if(resClass instanceof DateTimeClass || resClass instanceof DateClass)
                {
                    builder.append("sparql.cast_as_");
                    builder.append(getResourceName());
                    builder.append("_from_");
                    builder.append(resClass.getExpressionResourceClass().getName());
                    builder.append("(");
                    builder.append(variable.getVariableAccessor().variableAccess(variable.getName(), resClass, 0));
                    builder.append(", ");

                    if(resClass == xsdDateTime || resClass == xsdDate)
                        builder.append(variable.getVariableAccessor().variableAccess(variable.getName(), resClass, 1));
                    else if(resClass instanceof DateTimePatternClassWithConstantZone)
                        builder.append("'" + ((DateTimePatternClassWithConstantZone) resClass).getZone() + "'::int4");
                    else if(resClass instanceof DatePatternClassWithConstantZone)
                        builder.append("'" + ((DatePatternClassWithConstantZone) resClass).getZone() + "'::int4");

                    builder.append(")");
                }
                else if(resClass instanceof LangStringClass || resClass instanceof UnsupportedLiteralClass)
                {
                    builder.append(variable.getVariableAccessor().variableAccess(variable.getName(), resClass, 0));
                }
                else if(resClass instanceof IriClass)
                {
                    builder.append(variable.getExpressionValue(resClass, false));
                }

                assert false;
            }

            if(convertible.size() > 1)
                builder.append(")");

            return builder.toString();
        }

        return "sparql.cast_as_" + getResourceName() + "_from_" + getOperand().getResourceName() + "("
                + getOperand().translate() + ")";
    }


    // https://www.w3.org/TR/xpath-functions/#casting-from-primitive-to-primitive
    private static boolean canBeCast(ExpressionResourceClass from, ExpressionResourceClass to)
    {
        if((from == rdfLangStringExpr || from == unsupportedLiteralExpr || from == iri) && to == xsdString)
            return true;

        if(!BuiltinClasses.getExpressionLiteralClasses().contains(from))
            return false;

        if(from == to)
            return true;

        if(from == xsdString || to == xsdString)
            return true;

        if(from == xsdDayTimeDuration || to == xsdDayTimeDuration)
            return false;

        if(from == xsdDateTimeExpr || from == xsdDateExpr)
            return to == xsdDateTimeExpr || to == xsdDateExpr;

        if(to == xsdDateTimeExpr || to == xsdDateExpr)
            return false;

        return true;
    }


    // https://www.w3.org/TR/xpath-functions/#casting-from-primitive-to-primitive
    private static boolean canBeNull(ExpressionResourceClass from, ExpressionResourceClass to)
    {
        if((from == rdfLangStringExpr || from == unsupportedLiteralExpr || from == iri) && to == xsdString)
            return false;

        if(!BuiltinClasses.getExpressionLiteralClasses().contains(from))
            return true;

        if(from == to)
            return false;

        if(to == xsdString)
            return false;

        if((from == xsdDateTimeExpr || from == xsdDateExpr) && (to == xsdDateTimeExpr || to == xsdDateExpr))
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
