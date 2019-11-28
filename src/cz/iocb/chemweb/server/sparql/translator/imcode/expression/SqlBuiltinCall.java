package cz.iocb.chemweb.server.sparql.translator.imcode.expression;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.bnodeIntBlankNode;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.bnodeStrBlankNode;
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
    private final boolean distinct;
    private final List<SqlExpressionIntercode> arguments;


    SqlBuiltinCall(String function, boolean distinct, List<SqlExpressionIntercode> arguments,
            Set<ResourceClass> resourceClasses, boolean canBeNull)
    {
        super(resourceClasses, canBeNull,
                !function.equals("rand") && arguments.stream().allMatch(r -> r.isDeterministic()));
        this.function = function;
        this.distinct = distinct;
        this.arguments = arguments;

        for(SqlExpressionIntercode argument : arguments)
            this.variables.addAll(argument.getVariables());
    }


    SqlBuiltinCall(String function, List<SqlExpressionIntercode> arguments, Set<ResourceClass> resourceClasses,
            boolean canBeNull)
    {
        this(function, false, arguments, resourceClasses, canBeNull);
    }


    public static SqlExpressionIntercode create(String function, boolean distinct,
            List<SqlExpressionIntercode> arguments)
    {
        switch(function)
        {
            // aggregate functions
            case "count":
                return new SqlBuiltinCall(function, distinct, arguments, asSet(xsdInteger), false);

            case "sum":
            case "avg":
            {
                SqlExpressionIntercode argument = arguments.get(0);

                Set<ResourceClass> numerics = argument.getResourceClasses().stream().filter(r -> isNumeric(r))
                        .collect(Collectors.toSet());
                ResourceClass baseType = function.equals("avg") ? xsdDecimal : xsdInteger;
                Set<ResourceClass> resourceClass = numerics.stream()
                        .map(r -> isNumericCompatibleWith(r, baseType) ? baseType : r).collect(Collectors.toSet());
                boolean canBeNull = argument.canBeNull() || argument.getResourceClasses().size() > numerics.size();

                if(resourceClass.size() == 0)
                    return SqlNull.get();

                return new SqlBuiltinCall(function, distinct, arguments, resourceClass, canBeNull);
            }

            case "min":
            case "max":
            case "sample":
            {
                SqlExpressionIntercode argument = arguments.get(0);

                if(argument.getResourceClasses().size() == 0)
                    return SqlNull.get();

                return new SqlBuiltinCall(function, distinct, arguments, argument.getResourceClasses(),
                        argument.canBeNull());
            }

            case "group_concat":
            {
                SqlExpressionIntercode argument = arguments.get(0);

                Set<ResourceClass> resourceClass = argument.getResourceClasses().stream()
                        .filter(r -> isStringLiteral(r)).collect(Collectors.toSet());
                boolean canBeNull = argument.canBeNull() || argument.getResourceClasses().size() > resourceClass.size();

                if(resourceClass.size() == 0)
                    return SqlNull.get();

                return new SqlBuiltinCall(function, distinct, arguments, asSet(xsdString), canBeNull);
            }


            // functional forms
            case "bound":
            {
                if(arguments.get(0) == SqlNull.get())
                    return SqlEffectiveBooleanValue.falseValue;

                if(!arguments.get(0).canBeNull())
                    return SqlEffectiveBooleanValue.trueValue;

                return new SqlBuiltinCall(function, arguments, asSet(xsdBoolean), false);
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

                return new SqlBuiltinCall(function, arguments, resourceClasses,
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

                return new SqlBuiltinCall(function, realArguments, resourceClasses, canBeNull);
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

                return new SqlBuiltinCall(function, arguments, asSet(xsdBoolean),
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

                return new SqlBuiltinCall(function, arguments, asSet(xsdBoolean), operand.canBeNull());
            }

            case "str":
            {
                SqlExpressionIntercode operand = arguments.get(0);

                if(operand.getResourceClasses().stream().noneMatch(r -> isIri(r) || isLiteral(r)))
                    return SqlNull.get();

                if(operand.getResourceClasses().contains(xsdString) && operand.getResourceClasses().size() == 1)
                    return operand;

                return new SqlBuiltinCall(function, arguments, asSet(xsdString), operand.canBeNull()
                        || operand.getResourceClasses().stream().anyMatch(r -> !isIri(r) && !isLiteral(r)));
            }

            case "lang":
            {
                SqlExpressionIntercode operand = arguments.get(0);

                if(operand.getResourceClasses().stream().noneMatch(r -> isLiteral(r)))
                    return SqlNull.get();

                return new SqlBuiltinCall(function, arguments, asSet(xsdString),
                        operand.canBeNull() || operand.getResourceClasses().stream().anyMatch(r -> !isLiteral(r)));
            }

            case "datatype":
            {
                SqlExpressionIntercode operand = arguments.get(0);

                if(operand.getResourceClasses().stream().noneMatch(r -> isLiteral(r)))
                    return SqlNull.get();

                //NEXT: determine resource classes more precisely
                return new SqlBuiltinCall(function, arguments, asSet(iri),
                        operand.canBeNull() || operand.getResourceClasses().stream().anyMatch(r -> !isLiteral(r)));
            }

            case "iri":
            case "uri":
            {
                SqlExpressionIntercode operand = arguments.get(0);

                if(operand.getResourceClasses().stream().allMatch(r -> isIri(r)))
                    return operand;

                if(operand.getResourceClasses().stream().noneMatch(r -> isIri(r) || r == xsdString))
                    return SqlNull.get();

                Set<ResourceClass> resourceSet = operand.getResourceClasses().contains(xsdString) ? asSet(iri) :
                        operand.getResourceClasses().stream().filter(r -> isIri(r)).collect(Collectors.toSet());

                return new SqlBuiltinCall(function, arguments, resourceSet, operand.canBeNull()
                        || operand.getResourceClasses().stream().anyMatch(r -> !isIri(r) && r != xsdString));
            }

            case "bnode":
            {
                if(arguments.size() == 0)
                {
                    return new SqlBuiltinCall(function, arguments, asSet(bnodeIntBlankNode), false);
                }
                else
                {
                    SqlExpressionIntercode operand = arguments.get(0);

                    if(!operand.getResourceClasses().contains(xsdString))
                        return SqlNull.get();

                    return new SqlBuiltinCall(function, arguments, asSet(bnodeStrBlankNode),
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

                    if(resourceClass == xsdString && operand.getExpressionResourceClass() == xsdString)
                        return operand;
                    else if(resourceClass == xsdString)
                        return new SqlBuiltinCall(function, arguments, asSet(xsdString), canBeNull);
                    else if(resourceClass == null)
                        return new SqlBuiltinCall(function, arguments, asSet(unsupportedLiteral), canBeNull);
                    else
                        return new SqlBuiltinCall(function, arguments, asSet(resourceClass, unsupportedLiteral),
                                canBeNull);
                }
                else
                {
                    Set<ResourceClass> resourceClasses = asSet(unsupportedLiteral);
                    resourceClasses.addAll(BuiltinClasses.getLiteralClasses());

                    return new SqlBuiltinCall(function, arguments, resourceClasses,
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

                    return new SqlBuiltinCall(function, arguments, asSet(LangStringConstantTagClass.get(tag)),
                            operand.canBeNull() || operand.getResourceClasses().size() > 1);
                }

                return new SqlBuiltinCall(function, arguments, asSet(rdfLangString), true);
            }

            case "uuid":
                return new SqlBuiltinCall(function, arguments, asSet(iri), false);

            case "struuid":
                return new SqlBuiltinCall(function, arguments, asSet(xsdString), false);


            // functions on strings
            case "strlen":
            {
                SqlExpressionIntercode operand = arguments.get(0);

                if(operand.getResourceClasses().stream().noneMatch(r -> isStringLiteral(r)))
                    return SqlNull.get();

                return new SqlBuiltinCall(function, arguments, asSet(xsdInteger), operand.canBeNull()
                        || operand.getResourceClasses().stream().anyMatch(r -> !isStringLiteral(r)));
            }

            case "substr":
            {
                SqlExpressionIntercode operand = arguments.get(0);
                SqlExpressionIntercode location = arguments.get(1);
                SqlExpressionIntercode length = arguments.size() > 2 ? arguments.get(2) : null;

                Set<ResourceClass> resourceClasses = operand.getResourceClasses().stream()
                        .filter(r -> isStringLiteral(r)).collect(Collectors.toSet());

                if(resourceClasses.size() == 0
                        || location.getResourceClasses().stream().noneMatch(r -> isNumericCompatibleWith(r, xsdInteger))
                        || (length != null && length.getResourceClasses().stream()
                                .noneMatch(r -> isNumericCompatibleWith(r, xsdInteger))))
                    return SqlNull.get();

                return new SqlBuiltinCall(function, arguments, resourceClasses, operand.canBeNull()
                        || location.canBeNull()
                        || location.getResourceClasses().stream().anyMatch(r -> !isNumericCompatibleWith(r, xsdInteger))
                        || (length != null && (length.canBeNull() || length.getResourceClasses().stream()
                                .anyMatch(r -> !isNumericCompatibleWith(r, xsdInteger))))
                        || operand.getResourceClasses().size() > resourceClasses.size());
            }

            case "ucase":
            case "lcase":
            {
                SqlExpressionIntercode operand = arguments.get(0);

                Set<ResourceClass> resourceClasses = operand.getResourceClasses().stream()
                        .filter(r -> isStringLiteral(r)).collect(Collectors.toSet());

                if(resourceClasses.size() == 0)
                    return SqlNull.get();

                return new SqlBuiltinCall(function, arguments, resourceClasses,
                        operand.canBeNull() || operand.getResourceClasses().size() > resourceClasses.size());
            }

            case "strstarts":
            case "strends":
            case "contains":
            {
                SqlExpressionIntercode left = arguments.get(0);
                SqlExpressionIntercode right = arguments.get(1);

                boolean isValid = false;
                boolean canBeNull = left.canBeNull() || right.canBeNull();

                for(ResourceClass leftResClass : left.getResourceClasses())
                {
                    for(ResourceClass rightResClass : right.getResourceClasses())
                    {
                        if(isStringLiteral(leftResClass) && rightResClass == xsdString
                                || leftResClass == rightResClass && leftResClass instanceof LangStringConstantTagClass)
                        {
                            isValid = true;
                        }
                        else
                        {
                            canBeNull = true;

                            if(leftResClass == rdfLangString && isLangString(rightResClass)
                                    || isLangString(leftResClass) && rightResClass == rdfLangString)
                                isValid = true;
                        }
                    }
                }

                if(!isValid)
                    return SqlNull.get();

                return new SqlBuiltinCall(function, arguments, asSet(xsdBoolean), canBeNull);
            }

            case "strbefore":
            case "strafter":
            {
                SqlExpressionIntercode left = arguments.get(0);
                SqlExpressionIntercode right = arguments.get(1);

                Set<ResourceClass> resourceClasses = new HashSet<ResourceClass>();
                boolean canBeNull = left.canBeNull() || right.canBeNull();

                for(ResourceClass leftResClass : left.getResourceClasses())
                {
                    for(ResourceClass rightResClass : right.getResourceClasses())
                    {
                        if(isStringLiteral(leftResClass) && rightResClass == xsdString
                                || leftResClass == rightResClass && leftResClass instanceof LangStringConstantTagClass)
                        {
                            resourceClasses.add(leftResClass);
                        }
                        else
                        {
                            canBeNull = true;

                            if(leftResClass == rdfLangString && isLangString(rightResClass)
                                    || isLangString(leftResClass) && rightResClass == rdfLangString)
                                resourceClasses.add(leftResClass);
                        }
                    }
                }

                if(resourceClasses.size() == 0)
                    return SqlNull.get();

                resourceClasses.add(xsdString);

                return new SqlBuiltinCall(function, arguments, resourceClasses, canBeNull);
            }

            case "encode_for_uri":
            {
                SqlExpressionIntercode operand = arguments.get(0);

                if(operand.getResourceClasses().stream().noneMatch(r -> isStringLiteral(r)))
                    return SqlNull.get();

                return new SqlBuiltinCall(function, arguments, asSet(xsdString), operand.canBeNull()
                        || operand.getResourceClasses().stream().anyMatch(r -> !isStringLiteral(r)));
            }

            case "concat":
            {
                Set<ResourceClass> resultClasses = arguments.get(0).getResourceClasses();
                boolean canBeNull = arguments.get(0).canBeNull();

                for(int i = 0; i < arguments.size(); i++)
                {
                    Set<ResourceClass> nextResourceClasses = new HashSet<ResourceClass>();
                    SqlExpressionIntercode next = arguments.get(i);

                    for(ResourceClass resultClass : resultClasses)
                    {
                        for(ResourceClass nextClass : next.getResourceClasses())
                        {
                            if(isStringLiteral(resultClass) && isStringLiteral(nextClass))
                            {
                                if(resultClass == nextClass)
                                    nextResourceClasses.add(nextClass);

                                if(resultClass == rdfLangString && nextClass instanceof LangStringConstantTagClass)
                                    nextResourceClasses.add(nextClass);

                                if(resultClass instanceof LangStringConstantTagClass && nextClass == rdfLangString)
                                    nextResourceClasses.add(resultClass);

                                if(resultClass != nextClass || resultClass == rdfLangString)
                                    nextResourceClasses.add(xsdString);
                            }
                            else
                            {
                                canBeNull = true;
                            }
                        }
                    }

                    if(nextResourceClasses.contains(rdfLangString))
                        nextResourceClasses = nextResourceClasses.stream()
                                .filter(r -> !(r instanceof LangStringConstantTagClass)).collect(Collectors.toSet());

                    canBeNull |= next.canBeNull();
                    resultClasses = nextResourceClasses;
                }

                if(resultClasses.isEmpty())
                    return SqlNull.get();

                return new SqlBuiltinCall(function, arguments, resultClasses, canBeNull);
            }

            case "langmatches":
            {
                SqlExpressionIntercode lang = arguments.get(0);
                SqlExpressionIntercode pattern = arguments.get(1);

                if(!lang.getResourceClasses().contains(xsdString) || !pattern.getResourceClasses().contains(xsdString))
                    return SqlNull.get();

                return new SqlBuiltinCall(function, arguments, asSet(xsdBoolean),
                        lang.canBeNull() || pattern.canBeNull() || lang.getResourceClasses().size() > 1
                                || pattern.getResourceClasses().size() > 1);
            }

            case "regex":
            {
                SqlExpressionIntercode operand = arguments.get(0);
                SqlExpressionIntercode pattern = arguments.get(1);
                SqlExpressionIntercode flags = arguments.size() > 2 ? arguments.get(2) : null;

                if(operand.getResourceClasses().stream().noneMatch(r -> isStringLiteral(r))
                        || !pattern.getResourceClasses().contains(xsdString)
                        || (flags != null && !flags.getResourceClasses().contains(xsdString)))
                    return SqlNull.get();

                return new SqlBuiltinCall(function, arguments, asSet(xsdBoolean), true);
            }

            case "replace":
            {
                SqlExpressionIntercode operand = arguments.get(0);
                SqlExpressionIntercode pattern = arguments.get(1);
                SqlExpressionIntercode replacement = arguments.get(2);
                SqlExpressionIntercode flags = arguments.size() > 3 ? arguments.get(3) : null;

                Set<ResourceClass> resourceClasses = operand.getResourceClasses().stream()
                        .filter(r -> isStringLiteral(r)).collect(Collectors.toSet());

                if(resourceClasses.size() == 0 || !pattern.getResourceClasses().contains(xsdString)
                        || !replacement.getResourceClasses().contains(xsdString)
                        || (flags != null && !flags.getResourceClasses().contains(xsdString)))
                    return SqlNull.get();

                return new SqlBuiltinCall(function, arguments, resourceClasses, true);
            }


            // functions on numerics
            case "rand":
                return new SqlBuiltinCall(function, arguments, asSet(xsdDouble), false);

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

                return new SqlBuiltinCall(function, arguments, resultClasses,
                        operand.canBeNull() || resourceClasses.stream().anyMatch(r -> !isNumeric(r)));
            }


            // functions on dates and times:
            case "now":
                return new SqlBuiltinCall(function, arguments, asSet(DateTimeConstantZoneClass.get(0)), false);

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

                return new SqlBuiltinCall(function, arguments, resultClasses, operand.canBeNull() || canBeNull);
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
                        asSet(function.equals("seconds") ? xsdDecimal : xsdInteger),
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

        return create(function, distinct, optimized);
    }


    @Override
    public String translate()
    {
        switch(function)
        {
            // aggregate functions
            case "count":
            {
                StringBuilder builder = new StringBuilder();

                builder.append("count(");

                if(distinct)
                    builder.append("DISTINCT ");

                if(arguments.size() == 0)
                {
                    builder.append("1");
                }
                else if(arguments.size() == 1 && !(arguments.get(0) instanceof SqlVariable))
                {
                    builder.append(arguments.get(0).translate());
                }
                else
                {
                    int columns = 0;

                    for(int i = 0; i < arguments.size(); i++)
                        for(ResourceClass resClass : arguments.get(i).getResourceClasses())
                            columns += resClass.getPatternPartsCount();

                    if(columns > 1)
                        builder.append("row(");

                    boolean hasColumn = false;

                    for(int i = 0; i < arguments.size(); i++)
                    {
                        if(arguments.get(i) == SqlNull.get())
                            continue;

                        SqlVariable variable = (SqlVariable) arguments.get(i);

                        for(ResourceClass resClass : variable.getResourceClasses())
                        {
                            for(int j = 0; j < resClass.getPatternPartsCount(); j++)
                            {
                                appendComma(builder, hasColumn);
                                hasColumn = true;

                                builder.append(resClass.getSqlColumn(variable.getName(), j));
                            }
                        }
                    }

                    if(!hasColumn)
                        builder.append("NULL");

                    if(columns > 1)
                        builder.append(")");
                }

                builder.append(")::decimal");

                return builder.toString();
            }

            case "sum":
            case "avg":
            {
                StringBuilder builder = new StringBuilder();

                builder.append("sparql.");
                builder.append(function);
                builder.append("(");

                if(distinct)
                    builder.append("DISTINCT ");

                SqlExpressionIntercode argument = arguments.get(0);

                if(!isBoxed())
                    builder.append(translateAsUnboxedOperand(argument, getExpressionResourceClass()));
                else
                    builder.append(translateAsBoxedOperand(argument, argument.getResourceClasses().stream()
                            .filter(r -> isNumeric(r)).collect(Collectors.toSet())));

                builder.append(")");

                return builder.toString();
            }

            case "min":
            case "max":
            case "sample":
            {
                StringBuilder builder = new StringBuilder();

                builder.append("sparql.");
                builder.append(function);
                builder.append("(");

                if(distinct)
                    builder.append("DISTINCT ");

                SqlExpressionIntercode argument = arguments.get(0);
                builder.append(argument.translate());
                builder.append(")");

                return builder.toString();
            }

            case "group_concat":
            {
                StringBuilder builder = new StringBuilder();

                builder.append("sparql.group_concat(");

                if(distinct)
                    builder.append("DISTINCT ");

                SqlExpressionIntercode argument = arguments.get(0);

                if(argument instanceof SqlVariable)
                {
                    SqlVariable variable = (SqlVariable) argument;

                    Set<ResourceClass> compatible = argument.getResourceClasses().stream()
                            .filter(r -> isStringLiteral(r)).collect(Collectors.toSet());

                    if(compatible.size() > 1)
                        builder.append("COALESCE(");

                    boolean hasVariant = false;

                    for(ResourceClass resClass : compatible)
                    {
                        appendComma(builder, hasVariant);
                        hasVariant = true;

                        builder.append(variable.getNodeAccess(resClass, 0));
                    }

                    if(compatible.size() > 1)
                        builder.append(")");
                }
                else if(argument.isBoxed())
                {
                    builder.append("rdfbox_extract_string_literal(");
                    builder.append(argument.translate());
                    builder.append(")");
                }
                else
                {
                    builder.append(argument.translate());
                }

                builder.append(", ");

                if(arguments.size() == 1)
                    builder.append("' '::varchar");
                else
                    builder.append(arguments.get(1).translate());

                builder.append(")");

                return builder.toString();
            }


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

                if(left != SqlNull.get())
                {
                    builder.append(" WHEN true THEN ");

                    if(expressionResourceClass != null)
                        builder.append(translateAsUnboxedOperand(left, expressionResourceClass));
                    else
                        builder.append(translateAsBoxedOperand(left, left.getResourceClasses()));
                }

                if(right != SqlNull.get())
                {
                    builder.append(" WHEN false THEN ");

                    if(expressionResourceClass != null)
                        builder.append(translateAsUnboxedOperand(right, expressionResourceClass));
                    else
                        builder.append(translateAsBoxedOperand(right, right.getResourceClasses()));
                }

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

                    if(expressionResourceClass != null)
                        builder.append(translateAsUnboxedOperand(argument, expressionResourceClass));
                    else
                        builder.append(translateAsBoxedOperand(argument, argument.getResourceClasses()));
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

                    for(ResourceClass leftClass : leftNode.getResourceClasses())
                    {
                        for(ResourceClass rightClass : rightNode.getResourceClasses())
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
                                    builder.append("'" + ((SqlIri) right).getIri().getValue() + "'");
                            }
                            else if(leftClass instanceof IriClass && rightClass == iri)
                            {
                                appendComma(builder, variants++ > 0);

                                if(left instanceof SqlVariable)
                                    builder.append(((SqlVariable) left).getExpressionValue(leftClass, false));
                                else
                                    builder.append("'" + ((SqlIri) left).getIri().getValue() + "'");

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
                                builder.append(translateAsNullCheck(left, false));

                            if(left.canBeNull() && right.canBeNull())
                                builder.append(" OR ");

                            if(right.canBeNull())
                                builder.append(translateAsNullCheck(right, false));

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
                                builder.append(translateAsNullCheck(left, false));

                            if(left.canBeNull() && right.canBeNull())
                                builder.append(" OR ");

                            if(right.canBeNull())
                                builder.append(translateAsNullCheck(right, false));

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
                        builder.append(translateAsBoxedOperand(left, left.getResourceClasses()));
                        builder.append(", ");
                        builder.append(translateAsBoxedOperand(right, right.getResourceClasses()));
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
                    else if(leftExpressionResourceClass == xsdDateTime || rightExpressionResourceClass == xsdDateTime)
                    {
                        builder.append("sparql.zoneddatetime_same(");
                        builder.append(translateAsUnboxedOperand(left, xsdDateTime));
                        builder.append(", ");
                        builder.append(translateAsUnboxedOperand(right, xsdDateTime));
                        builder.append(")");
                    }
                    else if(leftExpressionResourceClass == xsdDate || rightExpressionResourceClass == xsdDate)
                    {
                        builder.append("sparql.zoneddate_same(");
                        builder.append(translateAsUnboxedOperand(left, xsdDate));
                        builder.append(", ");
                        builder.append(translateAsUnboxedOperand(right, xsdDate));
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
                        String datatype = ((LiteralClass) operandClass).getTypeIri().getValue();

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
                            String datatype = ((LiteralClass) resClass).getTypeIri().getValue();

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
            case "uri":
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
                return arguments.size() == 0 ? "sparql.bnode()" :
                        translateAsUnboxedOperand(arguments.get(0), xsdString);

            case "strdt":
                return "sparql.cast_as_rdfbox_from_typed_literal("
                        + translateAsUnboxedOperand(arguments.get(0), xsdString) + ", "
                        + translateAsUnboxedOperand(arguments.get(1), iri) + ")";

            case "strlang":
                if(this.getExpressionResourceClass() instanceof LangStringConstantTagClass)
                    return translateAsUnboxedOperand(arguments.get(0), xsdString);
                else
                    return "sparql.cast_as_rdfbox_from_lang_string("
                            + translateAsUnboxedOperand(arguments.get(0), xsdString) + ", "
                            + translateAsUnboxedOperand(arguments.get(1), xsdString) + ")";

            case "uuid":
                return "sparql.uuid()";

            case "struuid":
                return "sparql.struuid()";


            // functions on strings
            case "strlen":
            {
                SqlExpressionIntercode operand = arguments.get(0);

                if(operand instanceof SqlVariable)
                {
                    SqlVariable variable = (SqlVariable) operand;
                    String varName = variable.getName();

                    Set<ResourceClass> applicable = operand.getResourceClasses().stream()
                            .filter(r -> isStringLiteral(r)).collect(Collectors.toSet());

                    StringBuilder builder = new StringBuilder();

                    if(applicable.size() > 1)
                        builder.append("COALESCE(");

                    boolean hasAlternative = false;

                    for(ResourceClass resClass : applicable)
                    {
                        appendComma(builder, hasAlternative);
                        hasAlternative = true;

                        builder.append("sparql.strlen_string(");
                        builder.append(variable.getVariableAccessor().getSqlVariableAccess(varName, resClass, 0));
                        builder.append(")");
                    }

                    if(applicable.size() > 1)
                        builder.append(")");

                    return builder.toString();
                }
                else
                {
                    if(operand.isBoxed())
                        return "sparql.strlen_rdfbox(" + operand.translate() + ")";
                    else
                        return "sparql.strlen_string(" + operand.translate() + ")";
                }
            }

            case "substr":
            {
                SqlExpressionIntercode operand = arguments.get(0);
                SqlExpressionIntercode location = arguments.get(1);
                SqlExpressionIntercode length = arguments.size() > 2 ? arguments.get(2) : null;

                StringBuilder builder = new StringBuilder();

                if(isBoxed())
                {
                    builder.append("sparql.substr_rdfbox(");
                    builder.append(translateAsBoxedOperand(operand, getResourceClasses()));
                }
                else
                {
                    builder.append("sparql.substr_string(");
                    builder.append(translateAsStringLiteral(operand, getExpressionResourceClass()));
                }

                builder.append(", ");
                builder.append(translateAsUnboxedOperand(location, xsdInteger));

                if(length != null)
                {
                    builder.append(", ");
                    builder.append(translateAsUnboxedOperand(length, xsdInteger));
                }

                builder.append(")");

                return builder.toString();
            }

            case "ucase":
            case "lcase":
            {
                SqlExpressionIntercode operand = arguments.get(0);
                StringBuilder builder = new StringBuilder();

                builder.append("sparql.");
                builder.append(function);

                if(isBoxed())
                {
                    builder.append("_rdfbox(");
                    builder.append(translateAsBoxedOperand(operand, getResourceClasses()));
                }
                else
                {
                    builder.append("_string(");
                    builder.append(translateAsStringLiteral(operand, getExpressionResourceClass()));
                }

                builder.append(")");

                return builder.toString();
            }

            case "strstarts":
            case "strends":
            case "contains":
            {
                //TODO: add other optimized variants

                SqlExpressionIntercode left = arguments.get(0);
                SqlExpressionIntercode right = arguments.get(1);

                StringBuilder builder = new StringBuilder();

                if(left instanceof SqlNodeValue && right instanceof SqlNodeValue)
                {
                    SqlNodeValue leftNode = (SqlNodeValue) left;
                    SqlNodeValue rightNode = (SqlNodeValue) right;
                    int variants = 0;

                    for(ResourceClass leftResClass : left.getResourceClasses())
                    {
                        for(ResourceClass rightResClass : right.getResourceClasses())
                        {
                            if(isStringLiteral(leftResClass) && rightResClass == xsdString
                                    || leftResClass == rightResClass
                                            && leftResClass instanceof LangStringConstantTagClass)
                            {
                                appendComma(builder, variants++ > 0);

                                builder.append("sparql.");
                                builder.append(function);
                                builder.append("_string_string(");
                                builder.append(leftNode.getNodeAccess(leftResClass, 0));
                                builder.append(", ");
                                builder.append(rightNode.getNodeAccess(rightResClass, 0));
                                builder.append(")");
                            }
                            else if(leftResClass == rdfLangString && isLangString(rightResClass)
                                    || isLangString(leftResClass) && rightResClass == rdfLangString)
                            {
                                appendComma(builder, variants++ > 0);

                                builder.append("CASE WHEN ");

                                if(leftResClass == rdfLangString)
                                {
                                    builder.append(leftNode.getNodeAccess(leftResClass, 1));
                                }
                                else
                                {
                                    builder.append("'");
                                    builder.append(((LangStringConstantTagClass) leftResClass).getTag());
                                    builder.append("'::varchar");
                                }

                                builder.append(" = ");

                                if(rightResClass == rdfLangString)
                                {
                                    builder.append(rightNode.getNodeAccess(rightResClass, 1));
                                }
                                else
                                {
                                    builder.append("'");
                                    builder.append(((LangStringConstantTagClass) rightResClass).getTag());
                                    builder.append("'::varchar");
                                }

                                builder.append(" THEN sparql.");
                                builder.append(function);
                                builder.append("_string_string(");
                                builder.append(leftNode.getNodeAccess(leftResClass, 0));
                                builder.append(", ");
                                builder.append(rightNode.getNodeAccess(rightResClass, 0));
                                builder.append(") END");
                            }
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
                    builder.append("sparql.");
                    builder.append(function);

                    ResourceClass rightResClass = right.getExpressionResourceClass();

                    if(!left.isBoxed() && !right.isBoxed())
                    {
                        builder.append("_string_string(");
                        builder.append(left.translate());
                        builder.append(", ");
                        builder.append(right.translate());
                        builder.append(")");
                    }
                    else if(left.isBoxed() && (rightResClass == xsdString
                            || rightResClass instanceof LangStringConstantTagClass && left.getResourceClasses().stream()
                                    .allMatch(r -> r == rightResClass || isStringLiteral(r))))
                    {
                        builder.append("_rdfbox_string(");
                        builder.append(left.translate());
                        builder.append(", ");
                        builder.append(right.translate());
                        builder.append(")");
                    }
                    else
                    {
                        Set<ResourceClass> efectiveLeftSet = new HashSet<ResourceClass>();
                        Set<ResourceClass> efectiveRightSet = new HashSet<ResourceClass>();

                        for(ResourceClass l : left.getResourceClasses())
                        {
                            for(ResourceClass r : right.getResourceClasses())
                            {
                                if(isStringLiteral(l) && r == xsdString
                                        || l == r && l instanceof LangStringConstantTagClass
                                        || l == rdfLangString && isLangString(r)
                                        || isLangString(l) && r == rdfLangString)
                                {
                                    efectiveLeftSet.add(l);
                                    efectiveRightSet.add(r);
                                }
                            }
                        }

                        builder.append("_rdfbox_rdfbox(");
                        builder.append(translateAsBoxedOperand(left, efectiveLeftSet));
                        builder.append(", ");
                        builder.append(translateAsBoxedOperand(right, efectiveRightSet));
                        builder.append(")");
                    }
                }

                return builder.toString();
            }

            case "strbefore":
            case "strafter":
            {
                SqlExpressionIntercode left = arguments.get(0);
                SqlExpressionIntercode right = arguments.get(1);

                StringBuilder builder = new StringBuilder();

                if(left instanceof SqlNodeValue && right instanceof SqlNodeValue && !isBoxed())
                {
                    SqlNodeValue leftNode = (SqlNodeValue) left;
                    SqlNodeValue rightNode = (SqlNodeValue) right;

                    builder.append("sparql.");
                    builder.append(function);
                    builder.append("_string_string(");
                    builder.append(leftNode.getNodeAccess(xsdString, 0));
                    builder.append(", ");
                    builder.append(rightNode.getNodeAccess(xsdString, 0));
                    builder.append(")");
                }
                else
                {
                    builder.append("sparql.");
                    builder.append(function);

                    ResourceClass leftResClass = left.getExpressionResourceClass();
                    ResourceClass rightResClass = right.getExpressionResourceClass();

                    if(leftResClass == xsdString)
                    {
                        builder.append("_string_string(");
                        builder.append(left.translate());
                        builder.append(", ");

                        if(right.isBoxed())
                            builder.append("sparql.rdfbox_extract_string(");

                        builder.append(right.translate());

                        if(right.isBoxed())
                            builder.append(")");

                        builder.append(")");
                    }
                    else if(left.isBoxed() && (rightResClass == xsdString
                            || rightResClass instanceof LangStringConstantTagClass && left.getResourceClasses().stream()
                                    .allMatch(r -> r == rightResClass || !isStringLiteral(r))))
                    {
                        builder.append("_rdfbox_string(");
                        builder.append(left.translate());
                        builder.append(", ");
                        builder.append(right.translate());
                        builder.append(")");
                    }
                    else
                    {
                        Set<ResourceClass> efectiveLeftSet = new HashSet<ResourceClass>();
                        Set<ResourceClass> efectiveRightSet = new HashSet<ResourceClass>();

                        for(ResourceClass l : left.getResourceClasses())
                        {
                            for(ResourceClass r : right.getResourceClasses())
                            {
                                if(isStringLiteral(l) && r == xsdString
                                        || l == r && l instanceof LangStringConstantTagClass
                                        || l == rdfLangString && isLangString(r)
                                        || isLangString(l) && r == rdfLangString)
                                {
                                    efectiveLeftSet.add(l);
                                    efectiveRightSet.add(r);
                                }
                            }
                        }

                        builder.append("_rdfbox_rdfbox(");
                        builder.append(translateAsBoxedOperand(left, efectiveLeftSet));
                        builder.append(", ");
                        builder.append(translateAsBoxedOperand(right, efectiveRightSet));
                        builder.append(")");
                    }
                }

                return builder.toString();
            }

            case "encode_for_uri":
            {
                SqlExpressionIntercode operand = arguments.get(0);

                if(operand instanceof SqlVariable)
                {
                    SqlVariable variable = (SqlVariable) operand;
                    String varName = variable.getName();

                    Set<ResourceClass> applicable = operand.getResourceClasses().stream()
                            .filter(r -> isStringLiteral(r)).collect(Collectors.toSet());

                    StringBuilder builder = new StringBuilder();

                    if(applicable.size() > 1)
                        builder.append("COALESCE(");

                    boolean hasAlternative = false;

                    for(ResourceClass resClass : applicable)
                    {
                        appendComma(builder, hasAlternative);
                        hasAlternative = true;

                        builder.append("sparql.encode_for_uri_string(");
                        builder.append(variable.getVariableAccessor().getSqlVariableAccess(varName, resClass, 0));
                        builder.append(")");
                    }

                    if(applicable.size() > 1)
                        builder.append(")");

                    return builder.toString();
                }
                else
                {
                    if(operand.isBoxed())
                        return "sparql.encode_for_uri_rdfbox(" + operand.translate() + ")";
                    else
                        return "sparql.encode_for_uri_string(" + operand.translate() + ")";
                }
            }

            case "concat":
            {
                //TODO: add other optimized variants

                StringBuilder builder = new StringBuilder();

                for(int i = 1; i < arguments.size(); i++)
                    builder.append(isBoxed() ? "concat_rdfbox_rdfbox(" : "concat_string_string(");

                for(int i = 0; i < arguments.size(); i++)
                {
                    appendComma(builder, i > 0);

                    SqlExpressionIntercode argument = arguments.get(i);
                    Set<ResourceClass> applicable = argument.getResourceClasses().stream()
                            .filter(r -> isStringLiteral(r)).collect(Collectors.toSet());

                    if(isBoxed())
                    {
                        builder.append(translateAsBoxedOperand(argument, applicable));
                    }
                    else if(argument instanceof SqlVariable)
                    {
                        SqlVariable variable = (SqlVariable) argument;
                        String varName = variable.getName();

                        if(applicable.size() > 1)
                            builder.append("COALESCE(");

                        boolean hasAlternative = false;

                        for(ResourceClass resClass : applicable)
                        {
                            appendComma(builder, hasAlternative);
                            hasAlternative = true;

                            builder.append(variable.getVariableAccessor().getSqlVariableAccess(varName, resClass, 0));
                        }

                        if(applicable.size() > 1)
                            builder.append(")");
                    }
                    else if(argument.isBoxed())
                    {
                        builder.append("sparql.rdfbox_extract_string_literal(");
                        builder.append(argument.translate());
                        builder.append(")");
                    }
                    else
                    {
                        builder.append(argument.translate());
                    }


                    if(i > 0)
                        builder.append(")");
                }

                return builder.toString();
            }

            case "langmatches":
            {
                SqlExpressionIntercode lang = arguments.get(0);
                SqlExpressionIntercode pattern = arguments.get(1);

                if(lang.isBoxed() && pattern.isBoxed() && !(lang instanceof SqlVariable)
                        && !(pattern instanceof SqlVariable))
                {
                    return "sparql.langmatches_rdfbox_rdfbox(" + lang.translate() + ", " + pattern.translate() + ")";
                }
                else
                {
                    return "sparql.langmatches_string_string(" + translateAsUnboxedOperand(lang, xsdString) + ", "
                            + translateAsUnboxedOperand(pattern, xsdString) + ")";
                }
            }

            case "regex":
            {
                SqlExpressionIntercode operand = arguments.get(0);
                SqlExpressionIntercode pattern = arguments.get(1);
                SqlExpressionIntercode flags = arguments.size() > 2 ? arguments.get(2) : null;

                StringBuilder builder = new StringBuilder();

                if(!operand.isBoxed() || operand instanceof SqlVariable)
                    builder.append("sparql.regex_string(");
                else
                    builder.append("sparql.regex_rdfbox(");

                if(operand instanceof SqlVariable)
                {
                    SqlVariable variable = (SqlVariable) operand;
                    String varName = variable.getName();

                    Set<ResourceClass> applicable = operand.getResourceClasses().stream()
                            .filter(r -> isStringLiteral(r)).collect(Collectors.toSet());

                    if(applicable.size() > 1)
                        builder.append("COALESCE(");

                    boolean hasAlternative = false;

                    for(ResourceClass resClass : applicable)
                    {
                        appendComma(builder, hasAlternative);
                        hasAlternative = true;

                        builder.append(variable.getVariableAccessor().getSqlVariableAccess(varName, resClass, 0));
                    }

                    if(applicable.size() > 1)
                        builder.append(")");
                }
                else
                {
                    builder.append(operand.translate());
                }

                builder.append(", ");
                builder.append(translateAsUnboxedOperand(pattern, xsdString));

                if(flags != null)
                {
                    builder.append(", ");
                    builder.append(translateAsUnboxedOperand(flags, xsdString));
                }

                builder.append(")");

                return builder.toString();
            }

            case "replace":
            {
                SqlExpressionIntercode operand = arguments.get(0);
                SqlExpressionIntercode pattern = arguments.get(1);
                SqlExpressionIntercode replacement = arguments.get(2);
                SqlExpressionIntercode flags = arguments.size() > 3 ? arguments.get(3) : null;

                StringBuilder builder = new StringBuilder();

                if(isBoxed())
                {
                    builder.append("sparql.replace_rdfbox(");
                    builder.append(translateAsBoxedOperand(operand, this.getResourceClasses()));
                }
                else
                {
                    builder.append("sparql.replace_string(");
                    builder.append(translateAsStringLiteral(operand, getExpressionResourceClass()));
                }

                builder.append(", ");
                builder.append(translateAsUnboxedOperand(pattern, xsdString));

                builder.append(", ");
                builder.append(translateAsUnboxedOperand(replacement, xsdString));

                if(flags != null)
                {
                    builder.append(", ");
                    builder.append(translateAsUnboxedOperand(flags, xsdString));
                }

                builder.append(")");

                return builder.toString();
            }


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
                    System.err.println("HERE");

                    String sqlFunction = "sparql." + function + "_" + getResourceName();
                    String sqlArgument = operand.translate();

                    if(!isBoxed() && operand.isBoxed())
                        sqlArgument = "sparql.rdfbox_extract_derivated_from_" + getResourceName() + "(" + sqlArgument
                                + ")";

                    if(!isBoxed() && !operand.isBoxed() && !getResourceClasses().equals(operand.getResourceClasses()))
                        sqlArgument = "sparql.cast_as_integer_from_" + operand.getResourceName() + "(" + sqlArgument
                                + ")";

                    if(!function.equals("abs") && getExpressionResourceClass() == xsdInteger)
                        return sqlArgument;

                    return sqlFunction + "(" + sqlArgument + ")";
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

                        if(function.equals("abs") || effectiveClass != xsdInteger)
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
