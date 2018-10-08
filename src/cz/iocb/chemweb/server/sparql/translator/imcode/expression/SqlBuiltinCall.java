package cz.iocb.chemweb.server.sparql.translator.imcode.expression;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDate;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDateTime;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDayTimeDuration;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDecimal;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDouble;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdFloat;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdInteger;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdString;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import cz.iocb.chemweb.server.sparql.mapping.classes.DateConstantZoneClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.DateTimeConstantZoneClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.SimpleLiteralClass;
import cz.iocb.chemweb.server.sparql.translator.expression.VariableAccessor;



public class SqlBuiltinCall extends SqlExpressionIntercode
{
    private final String function;
    private final List<SqlExpressionIntercode> arguments;


    SqlBuiltinCall(String function, List<SqlExpressionIntercode> arguments, Set<ResourceClass> resourceClasses,
            boolean isBoxed, boolean canBeNull)
    {
        super(resourceClasses, isBoxed, canBeNull);
        this.function = function;
        this.arguments = arguments;
    }


    public static SqlExpressionIntercode create(String function, List<SqlExpressionIntercode> arguments)
    {
        switch(function)
        {
            // functions on numerics
            case "rand":
                return new SqlBuiltinCall(function, arguments, asSet(xsdDouble), false, false);

            case "abs":
            case "round":
            case "ceil":
            case "floor":
            {
                SqlExpressionIntercode operand = arguments.get(0);
                Set<ResourceClass> resourceClasses = operand.getResourceClasses();

                if(resourceClasses.stream().noneMatch(r -> isNumeric(r)))
                    return SqlNull.get();

                Set<ResourceClass> resultClasses = resourceClasses.stream().filter(r -> isNumeric(r))
                        .map(r -> determineNumericResultClass(r)).collect(Collectors.toSet());

                return new SqlBuiltinCall(function, arguments, resultClasses, resultClasses.size() > 1,
                        operand.canBeNull() || resourceClasses.stream().anyMatch(r -> !isNumeric(r)));
            }


            // functions on dates and times:
            case "now":
                return new SqlBuiltinCall(function, arguments, asSet(DateTimeConstantZoneClass.get(0)), false, false);

            case "year":
            case "month":
            case "day":
            case "timezone":
            case "tz":
            {
                SqlExpressionIntercode operand = arguments.get(0);
                Set<ResourceClass> resourceClasses = operand.getResourceClasses();

                if(resourceClasses.stream().noneMatch(r -> isDateTime(r) || isDate(r)))
                    return SqlNull.get();

                if(function.equals("timezone") && operand.getResourceClasses().stream()
                        .allMatch(r -> !(isDateTime(r) || isDate(r))
                                || r instanceof DateTimeConstantZoneClass
                                        && ((DateTimeConstantZoneClass) r).getZone() == Integer.MIN_VALUE
                                || r instanceof DateConstantZoneClass
                                        && ((DateConstantZoneClass) r).getZone() == Integer.MIN_VALUE))
                    return SqlNull.get();


                Set<ResourceClass> resultClasses = new HashSet<ResourceClass>();

                if(function.equals("timezone"))
                    resultClasses.add(xsdDayTimeDuration);
                else if(function.equals("tz"))
                    resultClasses.add(xsdString);
                else
                    resultClasses.add(xsdInteger);


                boolean canBeNull = resourceClasses.stream().anyMatch(r -> !isDateTime(r) && !isDate(r));

                if(function.equals("timezone"))
                    canBeNull |= operand.getResourceClasses().stream()
                            .anyMatch(r -> r == xsdDateTime || r == xsdDate
                                    || r instanceof DateTimeConstantZoneClass
                                            && ((DateTimeConstantZoneClass) r).getZone() == Integer.MIN_VALUE
                                    || r instanceof DateConstantZoneClass
                                            && ((DateConstantZoneClass) r).getZone() == Integer.MIN_VALUE);

                return new SqlBuiltinCall(function, arguments, resultClasses, false, operand.canBeNull() || canBeNull);
            }

            case "hours":
            case "minutes":
            case "seconds":
            {
                SqlExpressionIntercode operand = arguments.get(0);
                Set<ResourceClass> resourceClasses = operand.getResourceClasses();

                if(resourceClasses.stream().noneMatch(r -> isDateTime(r)))
                    return SqlNull.get();

                return new SqlBuiltinCall(function, arguments,
                        asSet(function.equals("seconds") ? xsdDecimal : xsdInteger), false,
                        operand.canBeNull() || resourceClasses.stream().anyMatch(r -> !isDateTime(r)));
            }


            // hash functions:
            case "md5":
            case "sha1":
            case "sha256":
            case "sha384":
            case "sha512":
            {
                SqlExpressionIntercode operand = arguments.get(0);

                if(!operand.getResourceClasses().contains(xsdString))
                    return SqlNull.get();

                return new SqlBuiltinCall(function, arguments, asSet(xsdString), false,
                        operand.canBeNull() || operand.getResourceClasses().size() > 1);
            }
        }

        return null;
    }


    @Override
    public SqlExpressionIntercode optimize(VariableAccessor variableAccessor)
    {
        List<SqlExpressionIntercode> optimized = new LinkedList<SqlExpressionIntercode>();

        for(SqlExpressionIntercode argument : arguments)
            optimized.add(argument.optimize(variableAccessor));

        return create(function, optimized);
    }


    @Override
    public String translate()
    {
        switch(function)
        {
            // functions on numerics
            case "rand":
                return "sparql.rand()";

            case "abs":
            case "round":
            case "ceil":
            case "floor":
            {
                SqlExpressionIntercode operand = arguments.get(0);

                if(!(operand instanceof SqlVariable))
                {
                    String sqlFunction = "sparql." + function + "_" + getResourceName();
                    String sqlArgument = "(" + operand.translate() + ")";

                    if(!isBoxed() && operand.isBoxed())
                        sqlArgument = "(sparql.rdfbox_extract_derivated_from_" + getResourceName() + sqlArgument + ")";

                    if(!isBoxed() && !operand.isBoxed() && !getResourceClasses().equals(operand.getResourceClasses()))
                        sqlArgument = "(sparql.cast_as_integer_from_" + operand.getResourceName() + sqlArgument + ")";

                    return sqlFunction + sqlArgument;
                }
                else
                {
                    SqlVariable variable = (SqlVariable) operand;

                    List<ResourceClass> compatible = variable.getResourceClasses().stream().filter(r -> isNumeric(r))
                            .collect(Collectors.toList());

                    StringBuilder builder = new StringBuilder();

                    if(compatible.size() > 1)
                        builder.append("COALESCE(");

                    boolean hasAlternative = false;

                    for(ResourceClass resClass : compatible)
                    {
                        appendComma(builder, hasAlternative);
                        hasAlternative = true;

                        ResourceClass effectiveClass = determineNumericResultClass(resClass);

                        String code = variable.getExpressionValue(resClass, false);

                        if(resClass != effectiveClass)
                            code = "sparql.cast_as_integer_from_" + resClass.getName() + "(" + code + ")";

                        code = "sparql." + function + "_" + effectiveClass.getName() + "(" + code + ")";

                        if(isBoxed())
                            code = "sparql.cast_as_rdfbox_from_" + effectiveClass.getName() + "(" + code + ")";

                        builder.append(code);
                    }

                    if(compatible.size() > 1)
                        builder.append(")");

                    return builder.toString();
                }
            }


            // functions on dates and times:
            case "now":
                return "now()";

            case "year":
            case "month":
            case "day":
            case "hours":
            case "minutes":
            case "seconds":
            case "timezone":
            case "tz":
            {
                SqlExpressionIntercode operand = arguments.get(0);

                StringBuilder builder = new StringBuilder();

                if(!(operand instanceof SqlVariable))
                {
                    ResourceClass expressionClass = operand.getExpressionResourceClass();

                    if(expressionClass instanceof DateTimeConstantZoneClass)
                    {
                        builder.append("sparql.");
                        builder.append(function);
                        builder.append("_datetime(");
                        builder.append(operand.translate());
                        builder.append(", '");
                        builder.append(((DateTimeConstantZoneClass) expressionClass).getZone());
                        builder.append("'::int4)");
                    }
                    else if(expressionClass instanceof DateConstantZoneClass)
                    {
                        builder.append("sparql.");
                        builder.append(function);
                        builder.append("_date(");
                        builder.append(operand.translate());
                        builder.append(", '");
                        builder.append(((DateConstantZoneClass) expressionClass).getZone());
                        builder.append("'::int4)");
                    }
                    else
                    {
                        builder.append("sparql.");
                        builder.append(function);
                        builder.append("_");
                        builder.append(operand.getResourceName());
                        builder.append("(");
                        builder.append(operand.translate());
                        builder.append(")");
                    }
                }
                else
                {
                    SqlVariable variable = (SqlVariable) operand;

                    List<ResourceClass> compatible;

                    if(function.equals("hours") || function.equals("minutes") || function.equals("seconds"))
                        compatible = variable.getResourceClasses().stream().filter(r -> isDateTime(r))
                                .collect(Collectors.toList());
                    else
                        compatible = variable.getResourceClasses().stream().filter(r -> isDateTime(r) || isDate(r))
                                .collect(Collectors.toList());


                    if(compatible.size() > 1)
                        builder.append("COALESCE(");

                    boolean hasAlternative = false;

                    for(ResourceClass resClass : compatible)
                    {
                        appendComma(builder, hasAlternative);
                        hasAlternative = true;

                        builder.append("sparql.");
                        builder.append(function);
                        builder.append("_");
                        builder.append(isDateTime(resClass) ? "datetime" : "date");
                        builder.append("(");

                        builder.append(
                                variable.getVariableAccessor().getSqlVariableAccess(variable.getName(), resClass, 0));
                        builder.append(", ");

                        if(resClass == xsdDateTime || resClass == xsdDate)
                            builder.append(variable.getVariableAccessor().getSqlVariableAccess(variable.getName(),
                                    resClass, 1));
                        else if(resClass instanceof DateTimeConstantZoneClass)
                            builder.append("'" + ((DateTimeConstantZoneClass) resClass).getZone() + "'::int4");
                        else if(resClass instanceof DateConstantZoneClass)
                            builder.append("'" + ((DateConstantZoneClass) resClass).getZone() + "'::int4");

                        builder.append(")");
                    }

                    if(compatible.size() > 1)
                        builder.append(")");
                }

                return builder.toString();
            }


            // hash functions:
            case "md5":
            case "sha1":
            case "sha256":
            case "sha384":
            case "sha512":
            {
                SqlExpressionIntercode operand = arguments.get(0);
                String sqlFunction = "sparql." + function + "_string";

                if(operand instanceof SqlVariable)
                    return sqlFunction + "(" + ((SqlVariable) operand).getExpressionValue(xsdString, false) + ")";
                else if(operand.isBoxed())
                    return sqlFunction + "(sparql.rdfbox_extract_string(" + operand.translate() + "))";
                else
                    return sqlFunction + "(" + operand.translate() + ")";
            }
        }

        return null;
    }


    private static SimpleLiteralClass determineNumericResultClass(ResourceClass operandClass)
    {
        if(operandClass == xsdDouble || operandClass == xsdFloat || operandClass == xsdDecimal)
            return (SimpleLiteralClass) operandClass;
        else
            return xsdInteger;
    }
}
