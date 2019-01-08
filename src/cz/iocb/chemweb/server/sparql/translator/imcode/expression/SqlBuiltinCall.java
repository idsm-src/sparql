package cz.iocb.chemweb.server.sparql.translator.imcode.expression;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.intBlankNode;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.iri;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.rdfLangString;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.strBlankNode;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.unsupportedLiteral;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdBoolean;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDate;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDateTime;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDayTimeDuration;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDecimal;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDouble;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdFloat;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdInteger;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlEffectiveBooleanValue.falseValue;
import static cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlEffectiveBooleanValue.trueValue;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses;
import cz.iocb.chemweb.server.sparql.mapping.classes.DateConstantZoneClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.DateTimeConstantZoneClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.IriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.LangStringConstantTagClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.LiteralClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.SimpleLiteralClass;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
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
            // functional forms
            case "bound":
            {
                if(!arguments.get(0).canBeNull())
                    return SqlEffectiveBooleanValue.trueValue;

                return new SqlBuiltinCall(function, arguments, asSet(xsdBoolean), false, false);
            }

            case "if":
            {
                SqlExpressionIntercode condition = SqlEffectiveBooleanValue.create(arguments.get(0));
                SqlExpressionIntercode left = arguments.get(1);
                SqlExpressionIntercode right = arguments.get(2);

                if(condition == SqlNull.get() || left == SqlNull.get() && right == SqlNull.get())
                    return SqlNull.get();

                if(condition == SqlEffectiveBooleanValue.trueValue)
                    return left;

                if(condition == SqlEffectiveBooleanValue.falseValue)
                    return right;

                Set<ResourceClass> resourceClasses = joinResourceClasses(left.getResourceClasses(),
                        right.getResourceClasses());

                ResourceClass expressionResourceClass = getExpressionResourceClass(resourceClasses);

                return new SqlBuiltinCall(function, arguments, resourceClasses, expressionResourceClass == null,
                        condition.canBeNull() || left.canBeNull() || right.canBeNull());
            }

            case "coalesce":
            {
                boolean canBeNull = true;
                Set<ResourceClass> resourceClasses = new HashSet<ResourceClass>();
                List<SqlExpressionIntercode> realArguments = new LinkedList<SqlExpressionIntercode>();

                for(SqlExpressionIntercode argument : arguments)
                {
                    if(argument == SqlNull.get())
                        continue;

                    canBeNull &= argument.canBeNull();
                    resourceClasses = joinResourceClasses(resourceClasses, argument.getResourceClasses());
                    realArguments.add(argument);

                    if(!argument.canBeNull())
                        break;
                }

                if(realArguments.isEmpty())
                    return SqlNull.get();

                if(realArguments.size() == 1 || !realArguments.get(0).canBeNull())
                    return realArguments.get(0);

                ResourceClass expressionResourceClass = getExpressionResourceClass(resourceClasses);

                return new SqlBuiltinCall(function, realArguments, resourceClasses, expressionResourceClass == null,
                        canBeNull);
            }

            case "sameterm":
            {
                SqlExpressionIntercode left = arguments.get(0);
                SqlExpressionIntercode right = arguments.get(1);

                if(left == SqlNull.get() || right == SqlNull.get())
                    return SqlNull.get();

                if(intersectResourceClasses(left.getResourceClasses(), right.getResourceClasses()).isEmpty()
                        && !left.canBeNull() && !right.canBeNull())
                    return SqlEffectiveBooleanValue.falseValue;

                return new SqlBuiltinCall(function, arguments, asSet(xsdBoolean), false,
                        left.canBeNull() || right.canBeNull());
            }


            // functions on RDF terms
            case "isiri":
            case "isuri":
            case "isblank":
            case "isliteral":
            case "isnumeric":
            {
                Function<ResourceClass, Boolean> is = getIsFunction(function);
                SqlExpressionIntercode operand = arguments.get(0);

                if(operand instanceof SqlNull)
                    return SqlNull.get();

                if(!operand.canBeNull() && operand.getResourceClasses().stream().allMatch(r -> is.apply(r)))
                    return trueValue;

                if(!operand.canBeNull() && operand.getResourceClasses().stream().noneMatch(r -> is.apply(r)))
                    return falseValue;

                return new SqlBuiltinCall(function, arguments, asSet(xsdBoolean), false, operand.canBeNull());
            }

            case "str":
            {
                SqlExpressionIntercode operand = arguments.get(0);

                if(operand.getResourceClasses().stream().noneMatch(r -> isIri(r) || isLiteral(r)))
                    return SqlNull.get();

                if(operand.getResourceClasses().contains(xsdString) && operand.getResourceClasses().size() == 1)
                    return operand;

                return new SqlBuiltinCall(function, arguments, asSet(xsdString), false, operand.canBeNull()
                        || operand.getResourceClasses().stream().anyMatch(r -> !isIri(r) && !isLiteral(r)));
            }

            case "lang":
            {
                SqlExpressionIntercode operand = arguments.get(0);

                if(operand.getResourceClasses().stream().noneMatch(r -> isLiteral(r)))
                    return SqlNull.get();

                return new SqlBuiltinCall(function, arguments, asSet(xsdString), false,
                        operand.canBeNull() || operand.getResourceClasses().stream().anyMatch(r -> !isLiteral(r)));
            }

            case "datatype":
            {
                SqlExpressionIntercode operand = arguments.get(0);

                if(operand.getResourceClasses().stream().noneMatch(r -> isLiteral(r)))
                    return SqlNull.get();

                //NEXT: determine resource classes more precisely
                return new SqlBuiltinCall(function, arguments, asSet(iri), false,
                        operand.canBeNull() || operand.getResourceClasses().stream().anyMatch(r -> !isLiteral(r)));
            }

            case "iri":
            {
                SqlExpressionIntercode operand = arguments.get(0);

                if(operand.getResourceClasses().stream().allMatch(r -> isIri(r)))
                    return operand;

                if(operand.getResourceClasses().stream().noneMatch(r -> isIri(r) || r == xsdString))
                    return SqlNull.get();

                Set<ResourceClass> resourceSet = operand.getResourceClasses().contains(xsdString) ? asSet(iri) :
                        operand.getResourceClasses().stream().filter(r -> isIri(r)).collect(Collectors.toSet());

                return new SqlBuiltinCall(function, arguments, resourceSet, false, operand.canBeNull()
                        || operand.getResourceClasses().stream().anyMatch(r -> !isIri(r) && r != xsdString));
            }

            case "bnode":
            {
                if(arguments.size() == 0)
                {
                    return new SqlBuiltinCall(function, arguments, asSet(intBlankNode), false, false);
                }
                else
                {
                    SqlExpressionIntercode operand = arguments.get(0);

                    if(!operand.getResourceClasses().contains(xsdString))
                        return SqlNull.get();

                    return new SqlBuiltinCall(function, arguments, asSet(strBlankNode), false,
                            operand.canBeNull() || operand.getResourceClasses().size() > 1);
                }
            }

            case "strdt":
            {
                SqlExpressionIntercode operand = arguments.get(0);
                SqlExpressionIntercode type = arguments.get(1);

                if(!operand.getResourceClasses().contains(xsdString))
                    return SqlNull.get();

                if(type.getResourceClasses().stream().noneMatch(r -> isIri(r)))
                    return SqlNull.get();


                if(type instanceof SqlIri)
                {
                    IRI iri = ((SqlIri) type).getIri();

                    ResourceClass resourceClass = BuiltinClasses.getLiteralClasses().stream()
                            .filter(r -> r.getTypeIri().equals(iri)).findFirst().orElse(null);

                    boolean canBeNull = operand.canBeNull() || operand.getResourceClasses().size() > 1;

                    if(resourceClass == xsdString)
                        return new SqlBuiltinCall(function, arguments, asSet(xsdString), false, canBeNull);
                    else if(resourceClass == null)
                        return new SqlBuiltinCall(function, arguments, asSet(unsupportedLiteral), true, canBeNull);
                    else
                        return new SqlBuiltinCall(function, arguments, asSet(resourceClass, unsupportedLiteral), true,
                                canBeNull);
                }
                else
                {
                    Set<ResourceClass> resourceClasses = asSet(unsupportedLiteral);
                    resourceClasses.addAll(BuiltinClasses.getLiteralClasses());

                    return new SqlBuiltinCall(function, arguments, resourceClasses, true,
                            operand.canBeNull() || operand.getResourceClasses().size() > 1 || type.canBeNull()
                                    || type.getResourceClasses().stream().anyMatch(r -> !isIri(r)));
                }
            }

            case "strlang":
            {
                SqlExpressionIntercode operand = arguments.get(0);
                SqlExpressionIntercode lang = arguments.get(1);

                if(!operand.getResourceClasses().contains(xsdString))
                    return SqlNull.get();

                if(!lang.getResourceClasses().contains(xsdString))
                    return SqlNull.get();

                if(lang instanceof SqlLiteral)
                {
                    String tag = ((SqlLiteral) lang).getLiteral().getStringValue();

                    if(!tag.matches("([A-Za-z]{2,3}(-[A-Za-z]{3}){0,3}|[A-Za-z]{4,8})"
                            + "(-[A-Za-z]{4})?(-([A-Za-z]{2}|[0-9]{3}))?(-([A-Za-z0-9]{5,8}|[0-9][A-Za-z0-9]{3}))*"
                            + "(-[0-9A-WY-Za-wy-z](-[A-Za-z0-9]{2,8})+)*(-x(-[A-Za-z0-9]{1,8})+)?|x(-[A-Za-z0-9]{1,8})+"
                            + "|i-ami|i-bnn|i-default|i-enochian|i-hak|i-klingon|i-lux|i-mingo|i-navajo|i-pwn"
                            + "|i-tao|i-tay|i-tsu|sgn-BE-FR|sgn-BE-NL|sgn-CH-DE"))
                        return SqlNull.get();

                    return new SqlBuiltinCall(function, arguments, asSet(LangStringConstantTagClass.get(tag)), false,
                            operand.canBeNull() || operand.getResourceClasses().size() > 1);
                }

                return new SqlBuiltinCall(function, arguments, asSet(rdfLangString), true, true);
            }

            case "uuid":
                return new SqlBuiltinCall(function, arguments, asSet(iri), false, false);

            case "struuid":
                return new SqlBuiltinCall(function, arguments, asSet(xsdString), false, false);


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
            // functional forms
            case "bound":
            {
                SqlVariable variable = (SqlVariable) arguments.get(0);
                StringBuilder builder = new StringBuilder();

                builder.append("(");
                boolean hasVariants = false;

                for(ResourceClass resourceClass : variable.getResourceClasses())
                {
                    appendOr(builder, hasVariants);
                    hasVariants = true;

                    for(int i = 0; i < resourceClass.getPatternPartsCount(); i++)
                    {
                        appendAnd(builder, i > 0);
                        builder.append(variable.getNodeAccess(resourceClass, i));
                        builder.append(" IS NOT NULL");
                    }
                }

                builder.append(")");

                return builder.toString();
            }

            case "if":
            {
                SqlExpressionIntercode condition = SqlEffectiveBooleanValue.create(arguments.get(0));
                SqlExpressionIntercode left = arguments.get(1);
                SqlExpressionIntercode right = arguments.get(2);

                ResourceClass expressionResourceClass = getExpressionResourceClass();
                StringBuilder builder = new StringBuilder();

                builder.append("CASE ");
                builder.append(condition.translate());
                builder.append(" WHEN true THEN ");
                builder.append(translateAsResourceClass(left, expressionResourceClass));
                builder.append(" WHEN false THEN ");
                builder.append(translateAsResourceClass(right, expressionResourceClass));
                builder.append(" END");

                return builder.toString();
            }

            case "coalesce":
            {
                ResourceClass expressionResourceClass = getExpressionResourceClass();
                StringBuilder builder = new StringBuilder();

                builder.append("COALESCE(");
                boolean hasVariants = false;

                for(SqlExpressionIntercode argument : arguments)
                {
                    appendComma(builder, hasVariants);
                    hasVariants = true;

                    builder.append(translateAsResourceClass(argument, expressionResourceClass));
                }

                builder.append(")");

                return builder.toString();
            }

            case "sameterm":
            {
                SqlExpressionIntercode left = arguments.get(0);
                SqlExpressionIntercode right = arguments.get(1);

                ResourceClass leftExpressionResourceClass = left.getExpressionResourceClass();
                ResourceClass rightExpressionResourceClass = right.getExpressionResourceClass();

                StringBuilder builder = new StringBuilder();

                if(left instanceof SqlNodeValue && right instanceof SqlNodeValue)
                {
                    boolean incomparable = false;
                    int variants = 0;

                    SqlNodeValue leftNode = (SqlNodeValue) left;
                    SqlNodeValue rightNode = (SqlNodeValue) right;

                    for(ResourceClass leftClass : leftNode.resourceClasses)
                    {
                        for(ResourceClass rightClass : rightNode.resourceClasses)
                        {
                            if(leftClass == rightClass)
                            {
                                appendComma(builder, variants++ > 0);

                                for(int i = 0; i < leftClass.getPatternPartsCount(); i++)
                                {
                                    appendAnd(builder, i > 0);
                                    builder.append(leftNode.getNodeAccess(leftClass, i));
                                    builder.append(" = ");
                                    builder.append(rightNode.getNodeAccess(rightClass, i));
                                }
                            }
                            else if(leftClass == xsdDateTime && rightClass instanceof DateTimeConstantZoneClass)
                            {
                                appendComma(builder, variants++ > 0);

                                builder.append(leftNode.getNodeAccess(leftClass, 0));
                                builder.append(" = ");
                                builder.append(rightNode.getNodeAccess(rightClass, 0));
                                builder.append(" AND ");
                                builder.append(leftNode.getNodeAccess(leftClass, 1));
                                builder.append(" = CASE WHEN ");
                                builder.append(rightNode.getNodeAccess(rightClass, 0));
                                builder.append(" IS NOT NULL THEN '");
                                builder.append(((DateTimeConstantZoneClass) rightClass).getZone());
                                builder.append("'::int4 END");
                            }
                            else if(leftClass instanceof DateTimeConstantZoneClass && rightClass == xsdDateTime)
                            {
                                appendComma(builder, variants++ > 0);

                                builder.append(leftNode.getNodeAccess(leftClass, 0));
                                builder.append(" = ");
                                builder.append(rightNode.getNodeAccess(rightClass, 0));
                                builder.append(" AND CASE WHEN ");
                                builder.append(leftNode.getNodeAccess(leftClass, 0));
                                builder.append(" IS NOT NULL THEN '");
                                builder.append(((DateTimeConstantZoneClass) leftClass).getZone());
                                builder.append("'::int4 END = ");
                                builder.append(rightNode.getNodeAccess(rightClass, 1));
                            }
                            else if(leftClass == xsdDate && rightClass instanceof DateConstantZoneClass)
                            {
                                appendComma(builder, variants++ > 0);

                                builder.append(leftNode.getNodeAccess(leftClass, 0));
                                builder.append(" = ");
                                builder.append(rightNode.getNodeAccess(rightClass, 0));
                                builder.append(" AND ");
                                builder.append(leftNode.getNodeAccess(leftClass, 1));
                                builder.append(" = CASE WHEN ");
                                builder.append(rightNode.getNodeAccess(rightClass, 0));
                                builder.append(" IS NOT NULL THEN '");
                                builder.append(((DateConstantZoneClass) rightClass).getZone());
                                builder.append("'::int4 END");
                            }
                            else if(leftClass instanceof DateConstantZoneClass && rightClass == xsdDate)
                            {
                                appendComma(builder, variants++ > 0);

                                builder.append(leftNode.getNodeAccess(leftClass, 0));
                                builder.append(" = ");
                                builder.append(rightNode.getNodeAccess(rightClass, 0));
                                builder.append(" AND CASE WHEN ");
                                builder.append(leftNode.getNodeAccess(leftClass, 0));
                                builder.append(" IS NOT NULL THEN '");
                                builder.append(((DateConstantZoneClass) leftClass).getZone());
                                builder.append("'::int4 END = ");
                                builder.append(rightNode.getNodeAccess(rightClass, 1));
                            }
                            else if(leftClass == rdfLangString && rightClass instanceof LangStringConstantTagClass)
                            {
                                appendComma(builder, variants++ > 0);

                                builder.append(leftNode.getNodeAccess(leftClass, 0));
                                builder.append(" = ");
                                builder.append(rightNode.getNodeAccess(rightClass, 0));
                                builder.append(" AND ");
                                builder.append(leftNode.getNodeAccess(leftClass, 1));
                                builder.append(" = CASE WHEN ");
                                builder.append(rightNode.getNodeAccess(rightClass, 0));
                                builder.append(" IS NOT NULL THEN '");
                                builder.append(((LangStringConstantTagClass) rightClass).getTag());
                                builder.append("'::varchar END");
                            }
                            else if(leftClass instanceof LangStringConstantTagClass && rightClass == rdfLangString)
                            {
                                appendComma(builder, variants++ > 0);

                                builder.append(leftNode.getNodeAccess(leftClass, 0));
                                builder.append(" = ");
                                builder.append(rightNode.getNodeAccess(rightClass, 0));
                                builder.append(" AND CASE WHEN ");
                                builder.append(leftNode.getNodeAccess(leftClass, 0));
                                builder.append(" IS NOT NULL THEN '");
                                builder.append(((LangStringConstantTagClass) leftClass).getTag());
                                builder.append("'::varchar END = ");
                                builder.append(rightNode.getNodeAccess(rightClass, 1));
                            }
                            else if(leftClass == iri && rightClass instanceof IriClass)
                            {
                                appendComma(builder, variants++ > 0);

                                builder.append(leftNode.getNodeAccess(leftClass, 0));
                                builder.append(" = ");

                                if(right instanceof SqlVariable)
                                    builder.append(((SqlVariable) right).getExpressionValue(leftClass, false));
                                else
                                    builder.append("'" + ((SqlIri) right).getIri().getUri() + "'");
                            }
                            else if(leftClass instanceof IriClass && rightClass == iri)
                            {
                                appendComma(builder, variants++ > 0);

                                if(left instanceof SqlVariable)
                                    builder.append(((SqlVariable) left).getExpressionValue(leftClass, false));
                                else
                                    builder.append("'" + ((SqlIri) left).getIri().getUri() + "'");

                                builder.append(" = ");
                                builder.append(rightNode.getNodeAccess(rightClass, 0));
                            }
                            else
                            {
                                incomparable = true;
                            }
                        }
                    }

                    if(incomparable)
                    {
                        appendComma(builder, variants++ > 0);

                        if(left.canBeNull() || right.canBeNull())
                        {
                            builder.append("NULLIF(");

                            if(left.canBeNull())
                                buildNullCheck(builder, left, false);

                            if(left.canBeNull() && right.canBeNull())
                                builder.append(" OR ");

                            if(right.canBeNull())
                                buildNullCheck(builder, right, false);

                            builder.append(", true)");
                        }
                        else
                        {
                            builder.append("false");
                        }
                    }

                    if(variants > 1)
                    {
                        builder.insert(0, "COALESCE(");
                        builder.append(")");
                    }
                }
                else
                {
                    if(intersectResourceClasses(left.getResourceClasses(), right.getResourceClasses()).isEmpty())
                    {
                        if(left.canBeNull() || right.canBeNull())
                        {
                            builder.append("NULLIF(");

                            if(left.canBeNull())
                                buildNullCheck(builder, left, false);

                            if(left.canBeNull() && right.canBeNull())
                                builder.append(" OR ");

                            if(right.canBeNull())
                                buildNullCheck(builder, right, false);

                            builder.append(", true)");
                        }
                        else
                        {
                            builder.append("false");
                        }
                    }
                    else if(left.isBoxed() || right.isBoxed())
                    {
                        builder.append("sparql.same_term_rdfbox(");
                        buildBoxedOperand(builder, left, left.getResourceClasses());
                        builder.append(", ");
                        buildBoxedOperand(builder, right, right.getResourceClasses());
                        builder.append(")");
                    }
                    else if(leftExpressionResourceClass == rightExpressionResourceClass
                            || isIri(leftExpressionResourceClass) && isIri(rightExpressionResourceClass))
                    {
                        builder.append("(");
                        builder.append(left.translate());
                        builder.append(" = ");
                        builder.append(right.translate());
                        builder.append(")");
                    }
                    else if(leftExpressionResourceClass == xsdDateTime)
                    {
                        builder.append("sparql.zoneddatetime_same(");
                        builder.append(left.translate());
                        builder.append(", sparql.zoneddatetime_create(");
                        builder.append(right.translate());
                        builder.append(", '");
                        builder.append(((DateTimeConstantZoneClass) rightExpressionResourceClass).getZone());
                        builder.append("'::int4))");
                    }
                    else if(rightExpressionResourceClass == xsdDateTime)
                    {
                        builder.append("sparql.zoneddatetime_same(sparql.zoneddatetime_create(");
                        builder.append(left.translate());
                        builder.append(", '");
                        builder.append(((DateTimeConstantZoneClass) leftExpressionResourceClass).getZone());
                        builder.append("'::int4), ");
                        builder.append(right.translate());
                        builder.append(")");
                    }
                    else if(leftExpressionResourceClass == xsdDate)
                    {
                        builder.append("sparql.zoneddate_same(");
                        builder.append(left.translate());
                        builder.append(", sparql.zoneddate_create(");
                        builder.append(right.translate());
                        builder.append(", '");
                        builder.append(((DateConstantZoneClass) rightExpressionResourceClass).getZone());
                        builder.append("'::int4))");

                    }
                    else if(rightExpressionResourceClass == xsdDate)
                    {
                        builder.append("sparql.zoneddate_same(sparql.zoneddate_create(");
                        builder.append(left.translate());
                        builder.append(", '");
                        builder.append(((DateConstantZoneClass) leftExpressionResourceClass).getZone());
                        builder.append("'::int4), ");
                        builder.append(right.translate());
                        builder.append(")");
                    }
                }

                return builder.toString();
            }


            // functions on RDF terms
            case "isiri":
            case "isuri":
            case "isblank":
            case "isliteral":
            case "isnumeric":
            {
                Function<ResourceClass, Boolean> is = getIsFunction(function);
                SqlExpressionIntercode operand = arguments.get(0);

                if(operand instanceof SqlVariable)
                {
                    SqlVariable variable = (SqlVariable) operand;

                    StringBuilder builder = new StringBuilder();

                    if(operand.getResourceClasses().size() > 1)
                        builder.append("COALESCE(");

                    boolean hasVariants = false;

                    for(ResourceClass resourceClass : operand.getResourceClasses())
                    {
                        appendComma(builder, hasVariants);
                        hasVariants = true;

                        if(is.apply(resourceClass))
                        {
                            builder.append("NULLIF(");

                            for(int i = 0; i < resourceClass.getPatternPartsCount(); i++)
                            {
                                appendAnd(builder, i > 0);
                                builder.append(variable.getNodeAccess(resourceClass, i));
                                builder.append(" IS NOT NULL");
                            }

                            builder.append(", false)");
                        }
                        else
                        {
                            builder.append("NULLIF(");

                            for(int i = 0; i < resourceClass.getPatternPartsCount(); i++)
                            {
                                appendOr(builder, i > 0);
                                builder.append(variable.getNodeAccess(resourceClass, i));
                                builder.append(" IS NULL");
                            }

                            builder.append(", true)");
                        }
                    }

                    if(operand.getResourceClasses().size() > 1)
                        builder.append(")");

                    return builder.toString();
                }
                else
                {
                    if(operand.canBeNull() && operand.getResourceClasses().stream().allMatch(r -> is.apply(r)))
                        return "NULLIF(" + operand.translate() + " IS NOT NULL, false)";

                    if(operand.canBeNull() && operand.getResourceClasses().stream().noneMatch(r -> is.apply(r)))
                        return "NULLIF(" + operand.translate() + " IS NULL, true)";

                    String sqlFunction = function.toLowerCase().replaceFirst("is", "is_").replaceFirst("uri", "iri");
                    return "sparql." + sqlFunction + "_rdfbox(" + operand.translate() + ")";
                }
            }

            case "str":
            {
                SqlExpressionIntercode operand = arguments.get(0);
                StringBuilder builder = new StringBuilder();

                if(!(operand instanceof SqlVariable))
                {
                    ResourceClass operandClass = operand.getExpressionResourceClass();

                    if(operandClass instanceof DateTimeConstantZoneClass)
                    {
                        builder.append("sparql.cast_as_string_from_datetime(");
                        builder.append(operand.translate());
                        builder.append(", '");
                        builder.append(((DateTimeConstantZoneClass) operandClass).getZone());
                        builder.append("'::int4)");
                    }
                    else if(operandClass instanceof DateConstantZoneClass)
                    {
                        builder.append("sparql.cast_as_string_from_date(");
                        builder.append(operand.translate());
                        builder.append(", '");
                        builder.append(((DateConstantZoneClass) operandClass).getZone());
                        builder.append("'::int4)");
                    }
                    else if(operandClass instanceof LangStringConstantTagClass)
                    {
                        builder.append(operand.translate());
                    }
                    else if(operandClass == xsdString || operandClass == iri)
                    {
                        builder.append(operand.translate());
                    }
                    else if(operand.isBoxed())
                    {
                        builder.append("sparql.str_rdfbox");
                        builder.append("(");
                        builder.append(operand.translate());
                        builder.append(")");
                    }
                    else
                    {
                        builder.append("sparql.cast_as_string_from_");
                        builder.append(operand.getResourceName());
                        builder.append("(");
                        builder.append(operand.translate());
                        builder.append(")");
                    }
                }
                else
                {
                    SqlVariable variable = (SqlVariable) operand;
                    String varName = variable.getName();

                    Set<ResourceClass> convertible = variable.getResourceClasses().stream()
                            .filter(r -> isIri(r) || isLiteral(r)).collect(Collectors.toSet());

                    if(convertible.size() > 1)
                        builder.append("COALESCE(");

                    boolean hasAlternative = false;

                    for(ResourceClass resClass : convertible)
                    {
                        appendComma(builder, hasAlternative);
                        hasAlternative = true;

                        if(resClass instanceof DateTimeConstantZoneClass)
                        {
                            builder.append("sparql.cast_as_string_from_datetime(");
                            builder.append(variable.getExpressionValue(resClass, false));
                            builder.append(", '");
                            builder.append(((DateTimeConstantZoneClass) resClass).getZone());
                            builder.append("'::int4)");
                        }
                        else if(resClass instanceof DateConstantZoneClass)
                        {
                            builder.append("sparql.cast_as_string_from_date(");
                            builder.append(variable.getExpressionValue(resClass, false));
                            builder.append(", '");
                            builder.append(((DateConstantZoneClass) resClass).getZone());
                            builder.append("'::int4)");
                        }
                        else if(resClass instanceof LangStringConstantTagClass)
                        {
                            builder.append(variable.getExpressionValue(resClass, false));
                        }
                        else if(resClass == xsdDate)
                        {
                            builder.append("sparql.cast_as_string_from_date(");
                            builder.append(variable.getVariableAccessor().getSqlVariableAccess(varName, resClass, 0));
                            builder.append(", ");
                            builder.append(variable.getVariableAccessor().getSqlVariableAccess(varName, resClass, 1));
                            builder.append(")");
                        }
                        else if(resClass == xsdDateTime)
                        {
                            builder.append("sparql.cast_as_string_from_datetime(");
                            builder.append(variable.getVariableAccessor().getSqlVariableAccess(varName, resClass, 0));
                            builder.append(", ");
                            builder.append(variable.getVariableAccessor().getSqlVariableAccess(varName, resClass, 1));
                            builder.append(")");
                        }
                        else if(resClass == rdfLangString || resClass == unsupportedLiteral)
                        {
                            builder.append(variable.getVariableAccessor().getSqlVariableAccess(varName, resClass, 0));
                        }
                        else if(resClass == xsdString || resClass instanceof IriClass)
                        {
                            builder.append(variable.getExpressionValue(resClass, false));
                        }
                        else
                        {
                            builder.append("sparql.cast_as_string_from_");
                            builder.append(resClass.getName());
                            builder.append("(");
                            builder.append(variable.getExpressionValue(resClass, false));
                            builder.append(")");
                        }
                    }

                    if(convertible.size() > 1)
                        builder.append(")");
                }

                return builder.toString();
            }

            case "lang":
            {
                SqlExpressionIntercode operand = arguments.get(0);

                if(!(operand instanceof SqlVariable))
                {
                    ResourceClass operandClass = operand.getExpressionResourceClass();

                    if(operand.isBoxed())
                    {
                        return "sparql.lang_rdfbox(" + operand.translate() + ")";
                    }
                    else
                    {
                        String tag = operandClass instanceof LangStringConstantTagClass ?
                                ((LangStringConstantTagClass) operandClass).getTag() : "";

                        if(operand.canBeNull())
                            return "CASE WHEN " + operand.translate() + " IS NOT NULL THEN '" + tag + "' END";
                        else
                            return "'" + tag + "'";
                    }
                }
                else
                {
                    SqlVariable variable = (SqlVariable) operand;
                    String varName = variable.getName();

                    Set<ResourceClass> applicable = variable.getResourceClasses().stream().filter(r -> isLiteral(r))
                            .collect(Collectors.toSet());

                    StringBuilder builder = new StringBuilder();

                    if(applicable.size() > 1)
                        builder.append("COALESCE(");

                    boolean hasAlternative = false;

                    for(ResourceClass resClass : applicable)
                    {
                        appendComma(builder, hasAlternative);
                        hasAlternative = true;

                        if(resClass == rdfLangString)
                        {
                            builder.append(variable.getVariableAccessor().getSqlVariableAccess(varName, resClass, 1));
                        }
                        else
                        {
                            String tag = resClass instanceof LangStringConstantTagClass ?
                                    ((LangStringConstantTagClass) resClass).getTag() : "";

                            if(operand.canBeNull() || variable.getResourceClasses().size() > 1)
                            {
                                builder.append("CASE WHEN ");

                                for(int i = 0; i < resClass.getPatternPartsCount(); i++)
                                {
                                    appendAnd(builder, i > 0);
                                    builder.append(variable.getNodeAccess(resClass, i));
                                    builder.append(" IS NOT NULL");
                                }

                                builder.append(" THEN '" + tag + "' END");
                            }
                            else
                            {
                                builder.append("'" + tag + "'");
                            }
                        }
                    }

                    if(applicable.size() > 1)
                        builder.append(")");

                    return builder.toString();
                }
            }

            case "datatype":
            {
                SqlExpressionIntercode operand = arguments.get(0);

                if(!(operand instanceof SqlVariable))
                {
                    ResourceClass operandClass = operand.getExpressionResourceClass();

                    if(operand.isBoxed())
                    {
                        return "sparql.datatype_rdfbox(" + operand.translate() + ")";
                    }
                    else
                    {
                        String datatype = ((LiteralClass) operandClass).getTypeIri().getUri().toString();

                        if(operand.canBeNull())
                            return "CASE WHEN " + operand.translate() + " IS NOT NULL THEN '" + datatype + "' END";
                        else
                            return "'" + datatype + "'";
                    }
                }
                else
                {
                    SqlVariable variable = (SqlVariable) operand;
                    String varName = variable.getName();

                    Set<ResourceClass> applicable = variable.getResourceClasses().stream().filter(r -> isLiteral(r))
                            .collect(Collectors.toSet());

                    StringBuilder builder = new StringBuilder();

                    if(applicable.size() > 1)
                        builder.append("COALESCE(");

                    boolean hasAlternative = false;

                    for(ResourceClass resClass : applicable)
                    {
                        appendComma(builder, hasAlternative);
                        hasAlternative = true;

                        if(resClass == unsupportedLiteral)
                        {
                            builder.append(variable.getVariableAccessor().getSqlVariableAccess(varName, resClass, 1));
                        }
                        else
                        {
                            String datatype = ((LiteralClass) resClass).getTypeIri().getUri().toString();

                            if(operand.canBeNull() || variable.getResourceClasses().size() > 1)
                            {
                                builder.append("CASE WHEN ");

                                for(int i = 0; i < resClass.getPatternPartsCount(); i++)
                                {
                                    appendAnd(builder, i > 0);
                                    builder.append(variable.getNodeAccess(resClass, i));
                                    builder.append(" IS NOT NULL");
                                }

                                builder.append(" THEN '" + datatype + "' END");
                            }
                            else
                            {
                                builder.append("'" + datatype + "'");
                            }
                        }
                    }

                    if(applicable.size() > 1)
                        builder.append(")");

                    return builder.toString();
                }
            }

            case "iri":
            {
                SqlExpressionIntercode operand = arguments.get(0);
                SqlExpressionIntercode base = arguments.get(1);

                if(!(operand instanceof SqlVariable))
                {
                    if(operand.isBoxed())
                        return "sparql.iri_rdfbox(" + base.translate() + ", " + operand.translate() + ")";
                    else
                        return "sparql.iri_string(" + base.translate() + ", " + operand.translate() + ")";
                }
                else
                {
                    SqlVariable variable = (SqlVariable) operand;

                    Set<ResourceClass> applicable = variable.getResourceClasses().stream()
                            .filter(r -> isIri(r) || r == xsdString).collect(Collectors.toSet());

                    StringBuilder builder = new StringBuilder();

                    if(applicable.size() > 1)
                        builder.append("COALESCE(");

                    boolean hasAlternative = false;

                    for(ResourceClass resClass : applicable)
                    {
                        appendComma(builder, hasAlternative);
                        hasAlternative = true;


                        if(resClass == xsdString)
                        {
                            builder.append("sparql.iri_string(");
                            builder.append(base.translate());
                            builder.append(", ");
                            builder.append(variable.getExpressionValue(resClass, false));
                            builder.append(")");
                        }
                        else
                        {
                            builder.append(variable.getExpressionValue(resClass, false));
                        }
                    }

                    if(applicable.size() > 1)
                        builder.append(")");

                    return builder.toString();
                }
            }

            case "bnode":
                return arguments.size() == 0 ? "sparql.bnode()" : translate_as_xsd_string(arguments.get(0));

            case "strdt":
                return "sparql.cast_as_rdfbox_from_typed_literal(" + translate_as_xsd_string(arguments.get(0)) + ", "
                        + translate_as_iri(arguments.get(1)) + ")";

            case "strlang":
                if(this.getExpressionResourceClass() instanceof LangStringConstantTagClass)
                    return translate_as_xsd_string(arguments.get(0));
                else
                    return "sparql.cast_as_rdfbox_from_lang_string(" + translate_as_xsd_string(arguments.get(0)) + ", "
                            + translate_as_xsd_string(arguments.get(1)) + ")";

            case "uuid":
                return "sparql.uuid()";

            case "struuid":
                return "sparql.struuid()";


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


    private String translateAsResourceClass(SqlExpressionIntercode operand, ResourceClass expressionResourceClass)
    {
        ResourceClass operandExpressionResourceClass = operand.getExpressionResourceClass();

        if(expressionResourceClass == operandExpressionResourceClass)
        {
            return operand.translate();
        }
        else if(expressionResourceClass == xsdDateTime)
        {
            int zone = ((DateTimeConstantZoneClass) operandExpressionResourceClass).getZone();
            return "sparql.zoneddatetime_create(" + operand.translate() + ", '" + zone + "'::int4)";
        }
        else if(expressionResourceClass == xsdDate)
        {
            int zone = ((DateConstantZoneClass) operandExpressionResourceClass).getZone();
            return "sparql.zoneddate_create(" + operand.translate() + ", '" + zone + "'::int4)";
        }
        else if(expressionResourceClass == iri)
        {
            return operand.translate();
        }
        else if(operandExpressionResourceClass instanceof DateTimeConstantZoneClass)
        {
            int zone = ((DateTimeConstantZoneClass) operandExpressionResourceClass).getZone();
            return "sparql.cast_as_rdfbox_from_datetime(" + operand.translate() + ", '" + zone + "'::int4)";
        }
        else if(operandExpressionResourceClass instanceof DateConstantZoneClass)
        {
            int zone = ((DateConstantZoneClass) operandExpressionResourceClass).getZone();
            return "sparql.cast_as_rdfbox_from_date(" + operand.translate() + ", '" + zone + "'::int4)";
        }
        else if(operandExpressionResourceClass instanceof LangStringConstantTagClass)
        {
            String tag = ((LangStringConstantTagClass) operandExpressionResourceClass).getTag();
            return "sparql.cast_as_rdfbox_from_lang_string(" + operand.translate() + ", '" + tag + "'::varchar)";
        }
        else if(operandExpressionResourceClass instanceof IriClass)
        {
            return "sparql.cast_as_rdfbox_from_iri(" + operand.translate() + ")";
        }
        else if(operand.isBoxed())
        {
            return operand.translate();
        }
        else
        {
            return "sparql.cast_as_rdfbox_from_" + operand.getResourceName() + "(" + operand.translate() + ")";
        }
    }


    private String translate_as_iri(SqlExpressionIntercode operand)
    {
        if(!(operand instanceof SqlVariable))
        {
            if(operand.isBoxed())
                return "sparql.rdfbox_extract_iri(" + operand.translate() + ")";
            else
                return operand.translate();
        }
        else
        {
            SqlVariable variable = (SqlVariable) operand;

            Set<ResourceClass> applicable = variable.getResourceClasses().stream().filter(r -> isIri(r))
                    .collect(Collectors.toSet());

            StringBuilder builder = new StringBuilder();

            if(applicable.size() > 1)
                builder.append("COALESCE(");

            boolean hasAlternative = false;

            for(ResourceClass resClass : applicable)
            {
                appendComma(builder, hasAlternative);
                hasAlternative = true;

                builder.append(variable.getExpressionValue(resClass, false));
            }

            if(applicable.size() > 1)
                builder.append(")");

            return builder.toString();
        }
    }


    private String translate_as_xsd_string(SqlExpressionIntercode operand)
    {
        if(operand instanceof SqlVariable)
            return ((SqlVariable) operand).getExpressionValue(xsdString, false);
        else if(operand.isBoxed())
            return "sparql.rdfbox_extract_string(" + operand.translate() + ")";
        else
            return operand.translate();
    }


    private static SimpleLiteralClass determineNumericResultClass(ResourceClass operandClass)
    {
        if(operandClass == xsdDouble || operandClass == xsdFloat || operandClass == xsdDecimal)
            return (SimpleLiteralClass) operandClass;
        else
            return xsdInteger;
    }


    private static Function<ResourceClass, Boolean> getIsFunction(String function)
    {
        if(function.equals("isiri") || function.equals("isuri"))
            return SqlExpressionIntercode::isIri;
        else if(function.equals("isblank"))
            return SqlExpressionIntercode::isBlankNode;
        else if(function.equals("isliteral"))
            return SqlExpressionIntercode::isLiteral;
        else if(function.equals("isnumeric"))
            return SqlExpressionIntercode::isNumeric;
        return null;
    }
}
