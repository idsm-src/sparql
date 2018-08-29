package cz.iocb.chemweb.server.sparql.translator.imcode.expression;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.rdfLangStringExpr;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.unsupportedLiteralExpr;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDate;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDateExpr;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDateTime;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDateTimeExpr;
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
import cz.iocb.chemweb.server.sparql.mapping.classes.DatePatternClassWithConstantZone;
import cz.iocb.chemweb.server.sparql.mapping.classes.DateTimePatternClassWithConstantZone;
import cz.iocb.chemweb.server.sparql.mapping.classes.SimpleLiteralClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.DateClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.DateTimeClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.ExpressionResourceClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.PatternResourceClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.ResourceClass;
import cz.iocb.chemweb.server.sparql.translator.expression.VariableAccessor;



public class SqlBuiltinCall extends SqlExpressionIntercode
{
    private final String function;
    private final List<SqlExpressionIntercode> arguments;


    SqlBuiltinCall(String function, List<SqlExpressionIntercode> arguments,
            Set<ExpressionResourceClass> resourceClasses, boolean canBeNull)
    {
        super(resourceClasses, resourceClasses.size() > 1 || resourceClasses.contains(rdfLangStringExpr)
                || resourceClasses.contains(unsupportedLiteralExpr), canBeNull);
        this.function = function;
        this.arguments = arguments;
    }


    public static SqlExpressionIntercode create(String function, List<SqlExpressionIntercode> arguments)
    {
        switch(function)
        {
            // functions on numerics
            case "rand":
                return new SqlBuiltinCall(function, arguments, asSet(xsdDouble), false);

            case "abs":
            case "round":
            case "ceil":
            case "floor":
            {
                SqlExpressionIntercode operand = arguments.get(0);
                Set<ExpressionResourceClass> resourceClasses = operand.getResourceClasses();

                if(resourceClasses.stream().noneMatch(r -> isNumeric(r)))
                    return SqlNull.get();

                Set<ExpressionResourceClass> resultClasses = resourceClasses.stream().filter(r -> isNumeric(r))
                        .map(r -> determineNumericResultClass(r)).collect(Collectors.toSet());

                return new SqlBuiltinCall(function, arguments, resultClasses,
                        operand.canBeNull() || resourceClasses.stream().anyMatch(r -> !isNumeric(r)));
            }


            // functions on dates and times:
            case "now":
                return new SqlBuiltinCall(function, arguments, asSet(xsdDateTimeExpr), false);

            case "year":
            case "month":
            case "day":
            case "timezone":
            case "tz":
            {
                SqlExpressionIntercode operand = arguments.get(0);
                Set<ExpressionResourceClass> resourceClasses = operand.getResourceClasses();

                if(!resourceClasses.contains(xsdDateTimeExpr) && !resourceClasses.contains(xsdDateExpr))
                    return SqlNull.get();

                if(function.equals("timezone") && operand instanceof SqlVariable && ((SqlVariable) operand)
                        .getPatternResourceClasses().stream()
                        .allMatch(r -> !(r instanceof DateTimeClass || r instanceof DateClass)
                                || r instanceof DateTimePatternClassWithConstantZone
                                        && ((DateTimePatternClassWithConstantZone) r).getZone() == Integer.MIN_VALUE
                                || r instanceof DatePatternClassWithConstantZone
                                        && ((DatePatternClassWithConstantZone) r).getZone() == Integer.MIN_VALUE))
                    return SqlNull.get();


                Set<ExpressionResourceClass> resultClasses = new HashSet<ExpressionResourceClass>();

                if(function.equals("timezone"))
                    resultClasses.add(xsdDayTimeDuration);
                else if(function.equals("tz"))
                    resultClasses.add(xsdString);
                else
                    resultClasses.add(xsdInteger);


                boolean canBeNull = resourceClasses.stream().anyMatch(r -> r != xsdDateTimeExpr && r != xsdDateExpr);

                if(function.equals("timezone"))
                    canBeNull |= !(operand instanceof SqlVariable) || ((SqlVariable) operand)
                            .getPatternResourceClasses().stream()
                            .anyMatch(r -> r == xsdDateTime || r == xsdDate
                                    || r instanceof DateTimePatternClassWithConstantZone
                                            && ((DateTimePatternClassWithConstantZone) r).getZone() == Integer.MIN_VALUE
                                    || r instanceof DatePatternClassWithConstantZone
                                            && ((DatePatternClassWithConstantZone) r).getZone() == Integer.MIN_VALUE);


                return new SqlBuiltinCall(function, arguments, resultClasses, operand.canBeNull() || canBeNull);
            }

            case "hours":
            case "minutes":
            case "seconds":
            {
                SqlExpressionIntercode operand = arguments.get(0);
                Set<ExpressionResourceClass> resourceClasses = operand.getResourceClasses();

                if(!resourceClasses.contains(xsdDateTimeExpr))
                    return SqlNull.get();

                return new SqlBuiltinCall(function, arguments,
                        asSet(function.equals("seconds") ? xsdDecimal : xsdInteger),
                        operand.canBeNull() || resourceClasses.stream().anyMatch(r -> r != xsdDateTimeExpr));
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

                return new SqlBuiltinCall(function, arguments, asSet(xsdString),
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

                    List<PatternResourceClass> compatible = variable.getPatternResourceClasses().stream()
                            .filter(r -> isNumeric(r)).collect(Collectors.toList());

                    StringBuilder builder = new StringBuilder();

                    if(compatible.size() > 1)
                        builder.append("COALESCE(");

                    boolean hasAlternative = false;

                    for(PatternResourceClass resClass : compatible)
                    {
                        appendComma(builder, hasAlternative);
                        hasAlternative = true;

                        PatternResourceClass effectiveClass = determineNumericResultClass(resClass);

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
                return "sparql.zoneddatetime_create(now(), 0)";

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

                if(!(operand instanceof SqlVariable))
                    return "sparql." + function + "_" + operand.getResourceName() + "(" + operand.translate() + ")";

                SqlVariable variable = (SqlVariable) operand;

                List<PatternResourceClass> compatible;

                if(function.equals("hours") || function.equals("minutes") || function.equals("seconds"))
                    compatible = variable.getPatternResourceClasses().stream().filter(r -> r instanceof DateTimeClass)
                            .collect(Collectors.toList());
                else
                    compatible = variable.getPatternResourceClasses().stream()
                            .filter(r -> r instanceof DateTimeClass || r instanceof DateClass)
                            .collect(Collectors.toList());


                StringBuilder builder = new StringBuilder();

                if(compatible.size() > 1)
                    builder.append("COALESCE(");

                boolean hasAlternative = false;

                for(PatternResourceClass resClass : compatible)
                {
                    appendComma(builder, hasAlternative);
                    hasAlternative = true;

                    builder.append("sparql.");
                    builder.append(function);
                    builder.append("_");
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

                if(compatible.size() > 1)
                    builder.append(")");

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


    private static Set<ExpressionResourceClass> asSet(ExpressionResourceClass... resourceClasses)
    {
        Set<ExpressionResourceClass> set = new HashSet<ExpressionResourceClass>();

        for(ExpressionResourceClass resourceClass : resourceClasses)
            set.add(resourceClass);

        return set;
    }


    private static SimpleLiteralClass determineNumericResultClass(ResourceClass operandClass)
    {
        if(operandClass == xsdDouble || operandClass == xsdFloat || operandClass == xsdDecimal)
            return (SimpleLiteralClass) operandClass;
        else
            return xsdInteger;
    }
}
