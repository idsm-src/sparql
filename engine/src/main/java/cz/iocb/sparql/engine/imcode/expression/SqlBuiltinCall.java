package cz.iocb.sparql.engine.imcode.expression;

import static cz.iocb.sparql.engine.database.Column.coalesce;
import static cz.iocb.sparql.engine.database.SqlType.BOOL;
import static cz.iocb.sparql.engine.database.SqlType.FLOAT8;
import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.database.SqlType.INT8;
import static cz.iocb.sparql.engine.database.SqlType.NUMERIC;
import static cz.iocb.sparql.engine.database.SqlType.RDFBOX;
import static cz.iocb.sparql.engine.database.SqlType.VARCHAR;
import static cz.iocb.sparql.engine.imcode.expression.SqlExpressionIntercode.Restriction.ALL;
import static cz.iocb.sparql.engine.imcode.expression.SqlExpressionIntercode.Restriction.NONE;
import static cz.iocb.sparql.engine.imcode.expression.SqlLiteral.falseValue;
import static cz.iocb.sparql.engine.imcode.expression.SqlLiteral.trueValue;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.bnodeIntBlankNode;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.bnodeStrBlankNode;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.dirLanguageTaggedString;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genBoolean;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genByte;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genDate;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genDateTime;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genDayTimeDuration;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genDecimal;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genDouble;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genFloat;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genInt;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genLong;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genNegativeInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genNonNegativeInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genNonPositiveInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genPositiveInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genScalarDate;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genScalarDateTime;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genShort;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genUnsignedByte;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genUnsignedInt;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genUnsignedLong;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genUnsignedShort;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasBlankNode;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasDate;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasDateTime;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasDerivatedFromInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasDirLanguageTaggedString;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasIri;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasLangString;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasLanguageTaggedString;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasLiteral;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasLtrLangString;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasNumeric;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasReference;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasRtlLangString;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasString;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasStringLiteral;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasTripleTerm;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.integerNumeric;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.iri;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isBlankNode;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isDate;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isDateOrDateTime;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isDateTime;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isDerivatedFromInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isIri;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isLangString;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isLanguageTaggedString;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isLiteral;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isLtrLangString;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isNumeric;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isReference;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isRtlLangString;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isString;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isStringLiteral;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.lexBoolean;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.lexByte;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.lexDate;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.lexDateTime;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.lexDayTimeDuration;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.lexDecimal;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.lexDouble;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.lexFloat;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.lexInt;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.lexInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.lexLong;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.lexNegativeInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.lexNonNegativeInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.lexNonPositiveInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.lexPositiveInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.lexShort;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.lexUnsignedByte;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.lexUnsignedInt;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.lexUnsignedLong;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.lexUnsignedShort;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.literal;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.rdfLangString;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.rdfLtrLangString;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.rdfRtlLangString;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.reference;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.tripleTerm;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.unsupportedType;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdBoolean;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdByte;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDate;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDateTime;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDayTimeDuration;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDecimal;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDouble;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdFloat;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdLong;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdNegativeInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdNonNegativeInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdNonPositiveInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdPositiveInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdScalarDate;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdScalarDateTime;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdShort;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdUnsignedByte;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdUnsignedInt;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdUnsignedLong;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdUnsignedShort;
import static cz.iocb.sparql.engine.mapping.classes.DerivedClass.estimateAsUnion;
import static cz.iocb.sparql.engine.mapping.classes.DerivedClass.unionize;
import static cz.iocb.sparql.engine.mapping.classes.ResourceClass.areDisjunct;
import static cz.iocb.sparql.engine.mapping.classes.ResourceClass.getExpressionClass;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.partitioningBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ExpressionColumn;
import cz.iocb.sparql.engine.database.SqlType;
import cz.iocb.sparql.engine.database.ValueColumn;
import cz.iocb.sparql.engine.database.VirtualTable;
import cz.iocb.sparql.engine.imcode.SqlIntercode.Restrictions;
import cz.iocb.sparql.engine.mapping.classes.BuiltinClasses;
import cz.iocb.sparql.engine.mapping.classes.ClassRelations;
import cz.iocb.sparql.engine.mapping.classes.DateInZone;
import cz.iocb.sparql.engine.mapping.classes.DateInZoneBaseClass;
import cz.iocb.sparql.engine.mapping.classes.DateInZoneClass;
import cz.iocb.sparql.engine.mapping.classes.DateTimeInZone;
import cz.iocb.sparql.engine.mapping.classes.DateTimeInZoneBaseClass;
import cz.iocb.sparql.engine.mapping.classes.DateTimeInZoneClass;
import cz.iocb.sparql.engine.mapping.classes.DirLangStringWithTagClass;
import cz.iocb.sparql.engine.mapping.classes.LangStringWithTagClass;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;
import cz.iocb.sparql.engine.mapping.classes.PrimitiveResourceClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.mapping.classes.TripleTermClass;
import cz.iocb.sparql.engine.mapping.classes.TripleTermClass.Component;
import cz.iocb.sparql.engine.mapping.classes.UserLiteralBaseClass;
import cz.iocb.sparql.engine.mapping.classes.UserLiteralClass;
import cz.iocb.sparql.engine.mapping.classes.UserLiteralCompositeBaseClass;
import cz.iocb.sparql.engine.mapping.classes.UserLiteralCompositeClass;
import cz.iocb.sparql.engine.mapping.datatypes.Datatype;
import cz.iocb.sparql.engine.rdf.DirLangStringLiteral.Direction;
import cz.iocb.sparql.engine.rdf.TypedLiteral;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.VariableBindings;



/**
 * Call of a SPARQL built-in function or aggregate (functions on terms, triple terms, strings, numerics, dates and
 * hashes, the aggregates, and the internal {@code card} for {@code COUNT(*)}). The result classes are derived from the
 * classes of the arguments when the call is created, and the SQL of each class is emitted only when the parent needs
 * it. {@code RAND} is the only nondeterministic function.
 */
public final class SqlBuiltinCall extends SqlExpressionIntercode
{
    /**
     * Regular expression of a well-formed language tag, the one the extension checks.
     */
    private static final String languageTagPattern = """
            ([A-Za-z]{2,3}(-[A-Za-z]{3}){0,3}|[A-Za-z]{4,8})\
            (-[A-Za-z]{4})?(-([A-Za-z]{2}|[0-9]{3}))?(-([A-Za-z0-9]{5,8}|[0-9][A-Za-z0-9]{3}))*\
            (-[0-9A-WY-Za-wy-z](-[A-Za-z0-9]{2,8})+)*(-x(-[A-Za-z0-9]{1,8})+)?|x(-[A-Za-z0-9]{1,8})+\
            |i-ami|i-bnn|i-default|i-enochian|i-hak|i-klingon|i-lux|i-mingo|i-navajo|i-pwn\
            |i-tao|i-tay|i-tsu|sgn-BE-FR|sgn-BE-NL|sgn-CH-DE""";

    /**
     * String literals of every kind stored in a box column.
     */
    private static final ResourceClass boxedStringLiteral = unionize(
            Set.of(xsdString, rdfLangString, rdfLtrLangString, rdfRtlLangString), box);

    /**
     * Lower-case function name.
     */
    private final String function;

    /**
     * DISTINCT modifier of an aggregate.
     */
    private final boolean distinct;

    /**
     * Arguments in order.
     */
    private final List<SqlExpressionIntercode> arguments;


    /**
     * Creates the call; {@code RAND} with a materialised result is nondeterministic.
     *
     * @param function lower-case function name
     * @param distinct whether the mapping declares distinct rows
     * @param arguments the arguments
     * @param mappings columns per resource class
     * @param canBeNull whether the value may be null
     */
    protected SqlBuiltinCall(String function, boolean distinct, List<SqlExpressionIntercode> arguments,
            Map<ResourceClass, List<Column>> mappings, boolean canBeNull)
    {
        boolean isDeterministic = (!function.equals("rand") || mappings.get(xsdDouble) == null)
                && arguments.stream().allMatch(r -> r.isDeterministic());
        super(mappings, canBeNull, isDeterministic);

        this.function = function;
        this.distinct = distinct;
        this.arguments = arguments;

        for(SqlExpressionIntercode argument : arguments)
            this.referencedVariables.addAll(argument.getReferencedVariables());
    }


    /**
     * Creates a call without DISTINCT.
     *
     * @param function lower-case function name
     * @param arguments the arguments
     * @param mappings columns per resource class
     * @param canBeNull whether the value may be null
     */
    protected SqlBuiltinCall(String function, List<SqlExpressionIntercode> arguments,
            Map<ResourceClass, List<Column>> mappings, boolean canBeNull)
    {
        this(function, false, arguments, mappings, canBeNull);
    }


    /**
     * Call of the function (lower-case name) with the given arguments; {@code distinct} applies to aggregates.
     *
     * @param request the current request
     * @param function lower-case function name
     * @param distinct whether the mapping declares distinct rows
     * @param arguments the arguments
     * @return call of the function (lower-case name) with the given arguments; {@code distinct} applies to aggregates
     */
    public static SqlExpressionIntercode create(Request request, String function, boolean distinct,
            List<SqlExpressionIntercode> arguments)
    {
        return create(request, function, distinct, arguments, Restriction.ALL);
    }


    /**
     * Call of the function materialising only the needed result classes; each function derives its result classes and
     * SQL from the argument classes.
     *
     * @param request the current request
     * @param function lower-case function name
     * @param distinct whether the mapping declares distinct rows
     * @param arguments the arguments
     * @param restriction the result classes the parent needs
     * @return call of the function materialising only the needed result classes; each function derives its result
     *         classes and SQL from the argument classes
     */
    public static SqlExpressionIntercode create(Request request, String function, boolean distinct,
            List<SqlExpressionIntercode> arguments, Restriction restriction)
    {
        ClassRelations relations = request.getConfiguration();

        switch(function)
        {
            // aggregate functions:

            case "card":
            {
                if(!restriction.contains(relations, xsdInteger))
                    return new SqlBuiltinCall(function, distinct, arguments, singletonMap(xsdInteger, null), false);

                Set<Column> columns = arguments.stream().flatMap(a -> a.getBinding().getNonConstantColumns().stream())
                        .collect(toSet());

                StringBuilder builder = new StringBuilder();

                builder.append("count(");

                if(distinct)
                    builder.append("DISTINCT ");

                if(columns.size() > 1)
                    builder.append("row(");

                if(columns.size() > 0)
                    builder.append(columns.stream().map(Object::toString).collect(joining(", ")));
                else if(distinct)
                    builder.append("1");
                else
                    builder.append("*");

                if(columns.size() > 1)
                    builder.append(")");

                builder.append(")::decimal");

                List<Column> result = List.of(new ExpressionColumn(builder.toString(), NUMERIC, false));

                return new SqlBuiltinCall(function, distinct, arguments, Map.of(xsdInteger, result), false);
            }

            case "count":
            {
                if(!restriction.contains(relations, xsdInteger))
                    return new SqlBuiltinCall(function, distinct, arguments, singletonMap(xsdInteger, null), false);

                SqlExpressionIntercode argument = arguments.get(0);

                if(argument.equals(SqlNull.get()))
                    return SqlLiteral.create(request, new TypedLiteral("0", xsdIntegerIri));

                Set<Column> columns = argument.getBinding().getNonConstantColumns();

                StringBuilder builder = new StringBuilder();

                builder.append("count(");

                if(distinct)
                    builder.append("DISTINCT ");

                if(!argument.canBeNull() && !distinct)
                {
                    builder.append("1");
                }
                else if(argument.canBeNull() && columns.size() > 1
                        && columns.stream().anyMatch(c -> c instanceof ExpressionColumn)
                        || distinct && columns.size() > 1 && argument.getResourceClasses().stream()
                                .anyMatch(r -> IntStream.range(0, r.getColumnCount()).anyMatch(r::isOptionalColumn)))
                {
                    // rows with a NULL field are never equal, so terms with optional columns are counted as one value
                    ResourceClass resClass = getExpressionClass(argument.getResourceClasses());
                    builder.append(argument.get(relations, resClass).get(0));
                }
                else
                {
                    if(argument.canBeNull() && columns.size() > 1)
                        builder.append("CASE WHEN ").append(argument.getIsNotNull()).append(" THEN ");

                    if(columns.size() > 1)
                        builder.append("row(");

                    if(columns.size() > 0)
                        builder.append(columns.stream().map(Object::toString).collect(joining(", ")));
                    else if(argument.getResourceClasses().size() > 0)
                        builder.append("1");
                    else
                        builder.append("NULL");

                    if(columns.size() > 1)
                        builder.append(")");

                    if(argument.canBeNull() && columns.size() > 1)
                        builder.append(" END");
                }

                builder.append(")::decimal");

                List<Column> result = List.of(new ExpressionColumn(builder.toString(), NUMERIC, false));

                return new SqlBuiltinCall(function, distinct, arguments, Map.of(xsdInteger, result), false);
            }

            case "sum":
            case "avg":
            {
                SqlExpressionIntercode argument = arguments.get(0);

                boolean canBeNull = argument.getResourceClasses().stream().anyMatch(r -> !isNumeric(r));

                if(argument.equals(SqlNull.get()))
                {
                    if(restriction.contains(relations, xsdInteger))
                        return SqlLiteral.create(request, new TypedLiteral("0", xsdIntegerIri));
                    else
                        return new SqlBuiltinCall(function, distinct, arguments, singletonMap(xsdInteger, null), false);
                }

                Set<ResourceClass> relevantClasses = argument.getResourceClasses().stream().filter(r -> hasNumeric(r))
                        .collect(toSet());

                Set<ResourceClass> promotedClasses = relevantClasses.stream()
                        .flatMap(r -> getNumericClasses(r).stream()).map(r -> determineResultClass(r)).collect(toSet());

                if(relevantClasses.isEmpty())
                    return SqlNull.get();

                Set<ResourceClass> resultClasses = new HashSet<>(promotedClasses);
                resultClasses.add(xsdInteger);

                if(function.equals("avg") && promotedClasses.contains(xsdInteger))
                    resultClasses.add(xsdDecimal);

                ResourceClass resultClass = resultClasses.size() == 1 ? resultClasses.iterator().next() :
                        unionize(resultClasses, box);


                if(!restriction.contains(relations, resultClass))
                    return new SqlBuiltinCall(function, distinct, arguments, singletonMap(resultClass, null), false);


                StringBuilder builder = new StringBuilder();

                if(promotedClasses.size() == 1 && relevantClasses.size() == argument.getResourceClasses().size())
                {
                    ResourceClass promotedClass = promotedClasses.iterator().next();
                    Column op = argument.promoteNumericAs(relevantClasses, promotedClass);

                    builder.append("sparql.");
                    builder.append(function);
                    builder.append("_");
                    builder.append(getLiteralClassName(promotedClass));
                    builder.append("(");

                    if(distinct)
                        builder.append("DISTINCT ");

                    builder.append(op);
                    builder.append(")");

                    builder.toString();
                }
                else
                {
                    Column op = argument.get(relations, box).get(0);

                    builder.append("sparql.");
                    builder.append(function);
                    builder.append("_rdfbox(");

                    if(distinct)
                        builder.append("DISTINCT ");

                    builder.append(op);
                    builder.append(")");

                    builder.toString();
                }

                List<Column> result = List
                        .of(new ExpressionColumn(builder.toString(), resultClass.getSqlTypes().get(0), canBeNull));

                return new SqlBuiltinCall(function, distinct, arguments, Map.of(resultClass, result), canBeNull);
            }

            case "min":
            case "max":
            case "sample":
            {
                SqlExpressionIntercode argument = arguments.get(0);

                if(argument.equals(SqlNull.get()))
                    return SqlNull.get();

                ResourceClass resultClass = getExpressionClass(argument.getResourceClasses());

                StringBuilder builder = new StringBuilder();

                if(function.equals("sample"))
                    builder.append("sparql.sample");
                else if(function.equals("min"))
                    builder.append("sparql.min");
                else if(resultClass.getEffectiveClass().equals(box))
                    builder.append("sparql.max_rdfbox");
                else
                    builder.append("max");

                builder.append("(");

                if(distinct)
                    builder.append("DISTINCT ");

                builder.append(argument.get(relations, resultClass).get(0));
                builder.append(")");


                List<Column> result = List
                        .of(new ExpressionColumn(builder.toString(), resultClass.getSqlTypes().get(0)));

                return new SqlBuiltinCall(function, distinct, arguments, Map.of(resultClass, result), true);
            }

            case "group_concat":
            {
                SqlExpressionIntercode argument = arguments.get(0);

                boolean canBeNull = argument.getResourceClasses().stream().anyMatch(r -> !isStringLiteral(r));

                if(!restriction.contains(relations, xsdString))
                    return new SqlBuiltinCall(function, distinct, arguments, singletonMap(xsdString, null), canBeNull);

                if(argument.equals(SqlNull.get()))
                    return SqlLiteral.create(request, new TypedLiteral("", xsdStringIri));

                boolean simple = argument.getResourceClasses().stream().allMatch(r -> isStringLiteral(r))
                        && !argument.getResourceClasses().stream().allMatch(r -> r.getEffectiveClass().equals(box));

                Column op = simple ? argument.getStringLiteral() : argument.get(relations, box).get(0);

                StringBuilder builder = new StringBuilder();

                builder.append("sparql.group_concat_");
                builder.append(simple ? "string" : "rdfbox");
                builder.append("(");

                if(distinct)
                    builder.append("DISTINCT ");

                builder.append(op);

                builder.append(", ");

                if(arguments.size() == 1)
                    builder.append("' '::varchar");
                else
                    builder.append(arguments.get(1).get(relations, xsdString).get(0));

                builder.append(")");


                List<Column> result = List.of(new ExpressionColumn(builder.toString(), VARCHAR));

                return new SqlBuiltinCall(function, distinct, arguments, Map.of(xsdString, result), canBeNull);
            }


            // functional forms:

            case "bound":
            {
                SqlExpressionIntercode argument = arguments.get(0);

                if(!restriction.contains(relations, xsdBoolean))
                    return new SqlBuiltinCall(function, distinct, arguments, singletonMap(xsdBoolean, null), false);

                if(argument.equals(SqlNull.get()))
                    return falseValue;

                if(!argument.canBeNull())
                    return trueValue;

                List<Column> result = List.of(new ExpressionColumn(argument.getIsNotNull(), BOOL, false));

                return new SqlBuiltinCall(function, distinct, arguments, singletonMap(xsdBoolean, result), false);
            }

            case "if":
            {
                SqlExpressionIntercode condition = arguments.get(0);
                SqlExpressionIntercode left = arguments.get(1);
                SqlExpressionIntercode right = arguments.get(2);

                if(condition.equals(SqlNull.get()) || left.equals(SqlNull.get()) && right.equals(SqlNull.get()))
                    return SqlNull.get();

                if(condition.equals(trueValue))
                    return left;

                if(condition.equals(falseValue))
                    return right;


                Map<Boolean, Set<ResourceClass>> classes = Stream
                        .concat(left.getResourceClasses().stream(), right.getResourceClasses().stream())
                        .collect(partitioningBy(c -> restriction.contains(relations, c), toSet()));

                boolean canBeNull = condition.canBeNull() || left.canBeNull() || right.canBeNull();

                Map<ResourceClass, List<Column>> mappings = new HashMap<>();

                if(!classes.get(false).isEmpty())
                    mappings.put(unionize(classes.get(false)), null);

                if(!classes.get(true).isEmpty())
                {
                    ResourceClass unionClass = getExpressionClass(classes.get(true));

                    StringBuilder builder = new StringBuilder();
                    builder.append("CASE ");
                    builder.append(condition.get(relations, xsdBoolean).get(0));
                    builder.append(" WHEN true THEN ").append(left.get(relations, unionClass).get(0));
                    builder.append(" WHEN false THEN ").append(right.get(relations, unionClass).get(0));
                    builder.append(" END");

                    Column col = new ExpressionColumn(builder.toString(), unionClass.getSqlTypes().get(0),
                            canBeNull || !classes.get(false).isEmpty());
                    mappings.put(unionClass, List.of(col));
                }

                return new SqlBuiltinCall(function, distinct, arguments, mappings, canBeNull);
            }

            case "coalesce":
            {
                boolean canBeNull = true;
                Set<ResourceClass> resourceClasses = new HashSet<>();
                List<SqlExpressionIntercode> realArguments = new LinkedList<>();

                for(SqlExpressionIntercode argument : arguments)
                {
                    if(argument.equals(SqlNull.get()))
                        continue;

                    canBeNull &= argument.canBeNull();
                    resourceClasses.addAll(argument.getResourceClasses());
                    realArguments.add(argument);

                    if(!argument.canBeNull())
                        break;
                }

                if(realArguments.isEmpty())
                    return SqlNull.get();

                if(realArguments.size() == 1)
                    return realArguments.get(0);


                Map<Boolean, Set<ResourceClass>> classes = resourceClasses.stream()
                        .collect(partitioningBy(c -> restriction.contains(relations, c), toSet()));

                Map<ResourceClass, List<Column>> mappings = new HashMap<>();

                if(!classes.get(false).isEmpty())
                    mappings.put(unionize(classes.get(false)), null);

                if(!classes.get(true).isEmpty())
                {
                    ResourceClass unionClass = getExpressionClass(classes.get(true));

                    List<Column> cols = realArguments.stream().map(a -> a.get(relations, unionClass).get(0))
                            .collect(toList());

                    Column col = new ExpressionColumn(
                            cols.stream().map(Object::toString).collect(joining(",", "COALESCE(", ")")),
                            unionClass.getSqlTypes().get(0));

                    mappings.put(unionClass, List.of(col));
                }

                return new SqlBuiltinCall(function, distinct, arguments, mappings, canBeNull);
            }

            case "sameterm":
            {
                SqlExpressionIntercode left = arguments.get(0);
                SqlExpressionIntercode right = arguments.get(1);

                if(left.equals(SqlNull.get()) || right.equals(SqlNull.get()))
                    return SqlNull.get();

                boolean canBeNull = left.canBeNull() || right.canBeNull();

                if(!restriction.contains(relations, xsdBoolean))
                    return new SqlBuiltinCall(function, distinct, arguments, singletonMap(xsdBoolean, null), canBeNull);

                if(left.equals(right) && !canBeNull)
                    return trueValue;

                Set<List<ResourceClass>> set = new HashSet<>();
                Map<ResourceClass, Set<List<ResourceClass>>> map = Map.of(xsdBoolean, set);

                for(ResourceClass leftClass : left.getMappings().keySet())
                    for(ResourceClass rightClass : right.getMappings().keySet())
                        if(!areDisjunct(relations, leftClass, rightClass))
                            set.add(List.of(leftClass, rightClass));

                if(set.isEmpty() && !canBeNull)
                    return falseValue;

                // terms whose classes never pair are different, unless one of them is missing
                String absent = "NULLIF(" + left.getIsNull() + " OR " + right.getIsNull() + ", true)";

                if(set.isEmpty())
                {
                    List<Column> result = List.of(new ExpressionColumn(absent, BOOL, canBeNull));
                    return new SqlBuiltinCall(function, distinct, arguments, singletonMap(xsdBoolean, result),
                            canBeNull);
                }

                List<SqlExpressionIntercode> operands = List.of(left, right);
                Map<ResourceClass, Set<List<Set<ResourceClass>>>> resMap = processResultMap(relations, operands, map,
                        restriction);

                Set<String> variants = new HashSet<>();

                for(List<Set<ResourceClass>> v : resMap.get(xsdBoolean))
                {
                    Set<ResourceClass> classes = new HashSet<>();
                    classes.addAll(v.get(0));
                    classes.addAll(v.get(1));

                    ResourceClass unionClass = unionize(classes);

                    //TODO: optimize for special combinations

                    //NOTE: to ensure that the expression column is not used more than once during conversion
                    if(left.hasExpressionColumn(relations, v.get(0)) || right.hasExpressionColumn(relations, v.get(1)))
                        unionClass = getExpressionClass(classes);

                    List<Column> lcols = left.get(relations, unionClass);
                    List<Column> rcols = right.get(relations, unionClass);

                    //FIXME: use correct compare operator, when unionClass is rdfbox

                    variants.add(getIdentityConditions(unionClass, lcols, rcols).stream()
                            .collect(joining(" AND ", "(", ")")));
                }

                List<Column> result = List.of(new ExpressionColumn(
                        variants.stream().collect(joining(" OR ", "COALESCE((", "), " + absent + ")")), BOOL,
                        canBeNull));

                return new SqlBuiltinCall(function, distinct, arguments, singletonMap(xsdBoolean, result), canBeNull);
            }


            // functions on RDF terms:

            case "isiri":
            case "isuri":
            case "isblank":
            case "isliteral":
            case "isnumeric":
            case "istriple":
            case "haslang":
            case "haslangdir":
            {
                SqlExpressionIntercode argument = arguments.get(0);

                if(argument instanceof SqlNull)
                    return SqlNull.get();

                boolean canBeNull = argument.canBeNull();

                if(!restriction.contains(relations, xsdBoolean))
                    return new SqlBuiltinCall(function, distinct, arguments, singletonMap(xsdBoolean, null), canBeNull);

                Function<ResourceClass, Boolean> is = getIsFunction(function);
                Function<ResourceClass, Boolean> has = getHasFunction(function);

                if(!argument.canBeNull() && argument.getResourceClasses().stream().allMatch(r -> is.apply(r)))
                    return trueValue;

                if(!argument.canBeNull() && argument.getResourceClasses().stream().noneMatch(r -> has.apply(r)))
                    return falseValue;

                Set<Column> variants = new HashSet<>();

                for(ResourceClass r : argument.getResourceClasses())
                {
                    if(is.apply(r))
                        variants.add(new ExpressionColumn("NULLIF(" + argument.getIsNotNull(relations, r) + ", false)",
                                BOOL));
                    else if(!has.apply(r))
                        variants.add(
                                new ExpressionColumn("NULLIF(" + argument.getIsNull(relations, r) + ", true)", BOOL));
                    else
                        variants.add(new ExpressionColumn("sparql." + function + "_rdfbox("
                                + argument.get(relations, unionize(Set.of(r), box)).get(0) + ")", BOOL));
                }

                List<Column> result = List.of(coalesce(variants));

                return new SqlBuiltinCall(function, distinct, arguments, singletonMap(xsdBoolean, result), canBeNull);
            }

            case "str":
            {
                SqlExpressionIntercode argument = arguments.get(0);

                if(argument.getResourceClasses().stream().noneMatch(r -> hasIri(r) || hasLiteral(r)))
                    return SqlNull.get();

                if(argument.getResourceClasses().stream().allMatch(r -> isString(r)))
                    return argument;

                boolean canBeNull = argument.canBeNull()
                        || argument.getResourceClasses().stream().anyMatch(r -> hasBlankNode(r));


                if(!restriction.contains(relations, xsdString))
                    return new SqlBuiltinCall(function, distinct, arguments, singletonMap(xsdString, null), canBeNull);


                Set<Column> variants = new HashSet<>();

                for(ResourceClass argumentClass : argument.getResourceClasses())
                {
                    StringBuilder builder = new StringBuilder();

                    if(argumentClass instanceof DateTimeInZoneClass constantZoneClass)
                    {
                        builder.append("sparql.str_datetime(");
                        builder.append(argument.getMapping(argumentClass).get(0));
                        builder.append(", '");
                        builder.append(constantZoneClass.getZone());
                        builder.append("'::int4)");
                    }
                    else if(argumentClass instanceof DateTimeInZoneBaseClass)
                    {
                        builder.append(argument.getMapping(argumentClass).get(1));
                    }
                    else if(argumentClass instanceof DateInZoneClass constantZoneClass)
                    {
                        builder.append("sparql.str_date(");
                        builder.append(argument.getMapping(argumentClass).get(0));
                        builder.append(", '");
                        builder.append(constantZoneClass.getZone());
                        builder.append("'::int4)");
                    }
                    else if(argumentClass instanceof DateInZoneBaseClass)
                    {
                        builder.append(argument.getMapping(argumentClass).get(1));
                    }
                    else if(argumentClass instanceof LangStringWithTagClass)
                    {
                        builder.append(argument.getMapping(argumentClass).get(0));
                    }
                    else if(argumentClass instanceof DirLangStringWithTagClass)
                    {
                        builder.append(argument.getMapping(argumentClass).get(0));
                    }
                    else if(argument.canSafelyGeneralize(relations, argumentClass, xsdDateTime))
                    {
                        List<Column> columns = argumentClass.toGeneralClass(xsdDateTime,
                                argument.get(relations, argumentClass), true);

                        builder.append("sparql.str_datetime(");
                        builder.append(columns.get(0));
                        builder.append(", ");
                        builder.append(columns.get(1));
                        builder.append(")");
                    }
                    else if(argument.canSafelyGeneralize(relations, argumentClass, genDateTime))
                    {
                        List<Column> columns = argumentClass.toGeneralClass(xsdDateTime,
                                argument.get(relations, argumentClass), true);

                        builder.append("COALESCE(NULLIF(");
                        builder.append(columns.get(2));
                        builder.append(", ''::varchar), sparql.str_datetime");
                        builder.append("(");
                        builder.append(columns.get(0));
                        builder.append(", ");
                        builder.append(columns.get(1));
                        builder.append("))");
                    }
                    else if(argument.canSafelyGeneralize(relations, argumentClass, xsdDate))
                    {
                        List<Column> columns = argumentClass.toGeneralClass(xsdDate,
                                argument.get(relations, argumentClass), true);

                        builder.append("sparql.str_date(");
                        builder.append(columns.get(0));
                        builder.append(", ");
                        builder.append(columns.get(1));
                        builder.append(")");
                    }
                    else if(argument.canSafelyGeneralize(relations, argumentClass, genDate))
                    {
                        List<Column> columns = argumentClass.toGeneralClass(xsdDate,
                                argument.get(relations, argumentClass), true);

                        builder.append("COALESCE(NULLIF(");
                        builder.append(columns.get(2));
                        builder.append(", ''::varchar), sparql.str_date");
                        builder.append("(");
                        builder.append(columns.get(0));
                        builder.append(", ");
                        builder.append(columns.get(1));
                        builder.append("))");
                    }
                    else if(argument.canSafelyGeneralize(relations, argumentClass, rdfLangString))
                    {
                        List<Column> columns = argumentClass.toGeneralClass(rdfLangString,
                                argument.get(relations, argumentClass), true);

                        builder.append(columns.get(0));
                    }
                    else if(argument.canSafelyGeneralize(relations, argumentClass, rdfLtrLangString))
                    {
                        List<Column> columns = argumentClass.toGeneralClass(rdfLtrLangString,
                                argument.get(relations, argumentClass), true);

                        builder.append(columns.get(0));
                    }
                    else if(argument.canSafelyGeneralize(relations, argumentClass, rdfRtlLangString))
                    {
                        List<Column> columns = argumentClass.toGeneralClass(rdfRtlLangString,
                                argument.get(relations, argumentClass), true);

                        builder.append(columns.get(0));
                    }
                    else if(argumentClass instanceof UserLiteralClass)
                    {
                        builder.append("(" + argument.getMapping(argumentClass).get(0) + ")::varchar");
                    }
                    else if(argumentClass instanceof UserLiteralBaseClass)
                    {
                        List<Column> columns = argumentClass.toGeneralClass(argumentClass,
                                argument.get(relations, argumentClass), true);

                        builder.append("COALESCE(NULLIF(");
                        builder.append(columns.get(1));
                        builder.append(", ''::varchar), ");
                        builder.append("(");
                        builder.append(columns.get(0));
                        builder.append(")::varchar)");
                    }
                    else if(argumentClass instanceof UserLiteralCompositeClass)
                    {
                        builder.append(argument.getMapping(argumentClass).get(0));
                    }
                    else if(argumentClass instanceof UserLiteralCompositeBaseClass)
                    {
                        List<Column> columns = argumentClass.toGeneralClass(argumentClass,
                                argument.get(relations, argumentClass), true);

                        builder.append("COALESCE(NULLIF(");
                        builder.append(columns.get(2));
                        builder.append(", ''::varchar), ");
                        builder.append(columns.get(0));
                        builder.append(")");
                    }
                    else if(isString(argumentClass))
                    {
                        builder.append(argumentClass
                                .toGeneralClass(xsdString, argument.get(relations, argumentClass), true).get(0));
                    }
                    else if(isIri(argumentClass))
                    {
                        builder.append(
                                argumentClass.toGeneralClass(iri, argument.get(relations, argumentClass), true).get(0));
                    }
                    else if(argument.canSafelyGeneralize(relations, argumentClass, unsupportedType))
                    {
                        List<Column> columns = argumentClass.toGeneralClass(unsupportedType,
                                argument.get(relations, argumentClass), true);

                        builder.append(columns.get(0));
                    }
                    else if(!isBlankNode(argumentClass))
                    {
                        LiteralClass canonicalClass = Stream
                                .of(xsdBoolean, xsdByte, xsdUnsignedByte, xsdShort, xsdUnsignedShort, xsdInt,
                                        xsdUnsignedInt, xsdLong, xsdUnsignedLong, xsdInteger, xsdNonPositiveInteger,
                                        xsdNegativeInteger, xsdNonNegativeInteger, xsdPositiveInteger, xsdDecimal,
                                        xsdFloat, xsdDouble, xsdScalarDateTime, xsdScalarDate, xsdDayTimeDuration)
                                .filter(r -> argumentClass.isSubclassOf(r)).findFirst().orElse(null);

                        ResourceClass nonCanonicalClass = Stream
                                .of(lexBoolean, lexByte, lexUnsignedByte, lexShort, lexUnsignedShort, lexInt,
                                        lexUnsignedInt, lexLong, lexUnsignedLong, lexInteger, lexNonPositiveInteger,
                                        lexNegativeInteger, lexNonNegativeInteger, lexPositiveInteger, lexDecimal,
                                        lexFloat, lexDouble, lexDateTime, lexDate, lexDayTimeDuration)
                                .filter(r -> argumentClass.isSubclassOf(r)).findFirst().orElse(null);

                        LiteralClass baseClass = Stream
                                .of(genBoolean, genByte, genUnsignedByte, genShort, genUnsignedShort, genInt,
                                        genUnsignedInt, genLong, genUnsignedLong, genInteger, genNonPositiveInteger,
                                        genNegativeInteger, genNonNegativeInteger, genPositiveInteger, genDecimal,
                                        genFloat, genDouble, genScalarDateTime, genScalarDate, genDayTimeDuration)
                                .filter(r -> argumentClass.isSubclassOf(r)).findFirst().orElse(null);

                        if(canonicalClass != null)
                        {
                            List<Column> columns = argumentClass.toGeneralClass(canonicalClass,
                                    argument.get(relations, argumentClass), true);

                            builder.append("sparql.str_").append(getLiteralClassName(canonicalClass));
                            builder.append("(");
                            builder.append(columns.get(0));
                            builder.append(")");
                        }
                        else if(nonCanonicalClass != null)
                        {
                            List<Column> columns = argumentClass.toGeneralClass(nonCanonicalClass,
                                    argument.get(relations, argumentClass), true);
                            builder.append(columns.get(1));
                        }
                        else if(baseClass != null)
                        {
                            List<Column> columns = argumentClass.toGeneralClass(baseClass,
                                    argument.get(relations, argumentClass), true);

                            builder.append("COALESCE(NULLIF(");
                            builder.append(columns.get(1));
                            builder.append(", ''::varchar), sparql.str_").append(getLiteralClassName(baseClass));
                            builder.append("(");
                            builder.append(columns.get(0));
                            builder.append("))");
                        }
                        else
                        {
                            List<Column> columns = argumentClass.toGeneralClass(box,
                                    argument.get(relations, argumentClass), true);

                            builder.append("sparql.str_rdfbox");
                            builder.append("(");
                            builder.append(columns.get(0));
                            builder.append(")");
                        }
                    }

                    if(!builder.isEmpty())
                        variants.add(new ExpressionColumn(builder.toString(), VARCHAR));
                }


                List<Column> result = List.of(coalesce(variants));

                return new SqlBuiltinCall(function, distinct, arguments, singletonMap(xsdString, result), canBeNull);
            }

            case "lang":
            {
                SqlExpressionIntercode argument = arguments.get(0);

                if(argument.getResourceClasses().stream().noneMatch(r -> hasLiteral(r)))
                    return SqlNull.get();

                boolean canBeNull = argument.canBeNull()
                        || argument.getResourceClasses().stream().anyMatch(r -> !isLiteral(r));


                if(!restriction.contains(relations, xsdString))
                    return new SqlBuiltinCall(function, distinct, arguments, singletonMap(xsdString, null), canBeNull);

                boolean partCanBeBull = argument.canBeNull() || argument.getResourceClasses().size() > 1;

                Set<Column> variants = new HashSet<>();

                for(ResourceClass argumentClass : argument.getResourceClasses())
                {
                    StringBuilder builder = new StringBuilder();

                    if(argumentClass instanceof LangStringWithTagClass langClass)
                    {
                        if(partCanBeBull)
                            builder.append("CASE WHEN " + argument.getIsNotNull(relations, argumentClass) + " THEN '"
                                    + langClass.getTag() + "' END");
                        else
                            builder.append("'" + langClass.getTag() + "'");
                    }
                    else if(argumentClass instanceof DirLangStringWithTagClass langClass)
                    {
                        if(partCanBeBull)
                            builder.append("CASE WHEN " + argument.getIsNotNull(relations, argumentClass) + " THEN '"
                                    + langClass.getTag() + "' END");
                        else
                            builder.append("'" + langClass.getTag() + "'");
                    }
                    else if(argument.canSafelyGeneralize(relations, argumentClass, rdfLangString))
                    {
                        builder.append(argumentClass
                                .toGeneralClass(rdfLangString, argument.get(relations, argumentClass), partCanBeBull)
                                .get(1));
                    }
                    else if(argument.canSafelyGeneralize(relations, argumentClass, rdfLtrLangString))
                    {
                        builder.append(argumentClass
                                .toGeneralClass(rdfLtrLangString, argument.get(relations, argumentClass), partCanBeBull)
                                .get(1));
                    }
                    else if(argument.canSafelyGeneralize(relations, argumentClass, rdfRtlLangString))
                    {
                        builder.append(argumentClass
                                .toGeneralClass(rdfRtlLangString, argument.get(relations, argumentClass), partCanBeBull)
                                .get(1));
                    }
                    else if(isLiteral(argumentClass) && !hasLanguageTaggedString(argumentClass))
                    {
                        if(partCanBeBull)
                            builder.append(
                                    "CASE WHEN " + argument.getIsNotNull(relations, argumentClass) + " THEN '' END");
                        else
                            builder.append("''");
                    }
                    else if(hasLiteral(argumentClass))
                    {
                        Column col = argument.get(relations, unionize(Set.of(argumentClass), box)).get(0);
                        builder.append("sparql.lang_rdfbox(" + col + ")");
                    }

                    if(!builder.isEmpty())
                        variants.add(new ExpressionColumn(builder.toString(), VARCHAR));
                }


                List<Column> result = List.of(coalesce(variants));

                return new SqlBuiltinCall(function, distinct, arguments, singletonMap(xsdString, result), canBeNull);
            }

            case "langdir":
            {
                SqlExpressionIntercode argument = arguments.get(0);

                if(argument.getResourceClasses().stream().noneMatch(r -> hasLiteral(r)))
                    return SqlNull.get();

                boolean canBeNull = argument.canBeNull()
                        || argument.getResourceClasses().stream().anyMatch(r -> !isLiteral(r));


                if(!restriction.contains(relations, xsdString))
                    return new SqlBuiltinCall(function, distinct, arguments, singletonMap(xsdString, null), canBeNull);

                boolean partCanBeBull = argument.canBeNull() || argument.getResourceClasses().size() > 1;

                Set<Column> variants = new HashSet<>();

                for(ResourceClass argumentClass : argument.getResourceClasses())
                {
                    StringBuilder builder = new StringBuilder();

                    if(isLtrLangString(argumentClass) || isRtlLangString(argumentClass))
                    {
                        Direction direction = isLtrLangString(argumentClass) ? Direction.LTR : Direction.RTL;

                        if(partCanBeBull)
                            builder.append("CASE WHEN " + argument.getIsNotNull(relations, argumentClass) + " THEN '"
                                    + direction.getText() + "' END");
                        else
                            builder.append("'" + direction.getText() + "'");
                    }
                    else if(isLiteral(argumentClass) && !hasDirLanguageTaggedString(argumentClass))
                    {
                        if(partCanBeBull)
                            builder.append(
                                    "CASE WHEN " + argument.getIsNotNull(relations, argumentClass) + " THEN '' END");
                        else
                            builder.append("''");
                    }
                    else if(hasLiteral(argumentClass))
                    {
                        Column col = argument.get(relations, unionize(Set.of(argumentClass), box)).get(0);
                        builder.append("sparql.langdir_rdfbox(" + col + ")");
                    }

                    if(!builder.isEmpty())
                        variants.add(new ExpressionColumn(builder.toString(), VARCHAR));
                }


                List<Column> result = List.of(coalesce(variants));

                return new SqlBuiltinCall(function, distinct, arguments, singletonMap(xsdString, result), canBeNull);
            }


            case "datatype":
            {
                //TODO: if possible, specify the return class more specifically than just iri
                //TODO: if possible, return constant expression

                SqlExpressionIntercode argument = arguments.get(0);

                if(argument.getResourceClasses().stream().noneMatch(r -> hasLiteral(r)))
                    return SqlNull.get();

                boolean canBeNull = argument.canBeNull()
                        || argument.getResourceClasses().stream().anyMatch(r -> !isLiteral(r));


                if(!restriction.contains(relations, iri))
                    return new SqlBuiltinCall(function, distinct, arguments, singletonMap(iri, null), canBeNull);

                boolean partCanBeBull = argument.canBeNull() || argument.getResourceClasses().size() > 1;

                Set<Column> variants = new HashSet<>();

                for(ResourceClass argumentClass : argument.getResourceClasses())
                {
                    StringBuilder builder = new StringBuilder();

                    if(argumentClass.getEffectiveClass() instanceof LiteralClass literalClass
                            && literalClass.getTypeIri() != null)
                    {
                        String iriValue = literalClass.getTypeIri().getValue();

                        if(partCanBeBull)
                            builder.append("CASE WHEN " + argument.getIsNotNull(relations, argumentClass) + " THEN '"
                                    + iriValue + "' END");
                        else
                            builder.append("'" + iriValue + "'");
                    }
                    else if(argument.canSafelyGeneralize(relations, argumentClass, unsupportedType))
                    {
                        builder.append(argumentClass
                                .toGeneralClass(unsupportedType, argument.get(relations, argumentClass), partCanBeBull)
                                .get(1));
                    }
                    else if(hasLiteral(argumentClass))
                    {
                        Column col = argument.get(relations, unionize(Set.of(argumentClass), box)).get(0);
                        builder.append("sparql.datatype_rdfbox(" + col + ")");
                    }

                    if(!builder.isEmpty())
                        variants.add(new ExpressionColumn(builder.toString(), VARCHAR));
                }


                List<Column> result = List.of(coalesce(variants));

                return new SqlBuiltinCall(function, distinct, arguments, singletonMap(iri, result), canBeNull);
            }

            case "iri":
            case "uri":
            {
                final ResourceClass argClasses = unionize(xsdString, iri);

                SqlExpressionIntercode argument = arguments.get(0);

                if(argument.getResourceClasses().stream().noneMatch(r -> hasString(r) || hasIri(r)))
                    return SqlNull.get();

                if(argument.getResourceClasses().stream().allMatch(r -> isIri(r)))
                    return argument;

                boolean canBeNull = argument.canBeNull()
                        || argument.getResourceClasses().stream().anyMatch(r -> !r.isSubclassOf(argClasses));

                if(!restriction.contains(relations, iri))
                    return new SqlBuiltinCall(function, distinct, arguments, singletonMap(iri, null), canBeNull);

                boolean partCanBeBull = argument.canBeNull() || argument.getResourceClasses().size() > 1;
                Column base = arguments.get(1).get(relations, iri).get(0);

                Set<Column> variants = new HashSet<>();

                for(ResourceClass argClass : argument.getResourceClasses())
                {
                    StringBuilder builder = new StringBuilder();

                    if(isIri(argClass))
                    {
                        builder.append(
                                argClass.toGeneralClass(iri, argument.get(relations, argClass), partCanBeBull).get(0));
                    }
                    else if(isString(argClass))
                    {
                        Column col = argClass
                                .toGeneralClass(xsdString, argument.get(relations, argClass), partCanBeBull).get(0);

                        builder.append("sparql." + function + "_string(" + base + ", " + col + ")");
                    }
                    else if(hasIri(argClass) && !hasString(argClass))
                    {
                        ResourceClass boxClass = unionize(Set.of(argClass), box);
                        Column col = argClass.toGeneralClass(boxClass, argument.get(relations, argClass), partCanBeBull)
                                .get(0);

                        builder.append("sparql.rdfbox_get_iri(" + col + ")");
                    }
                    else if(hasIri(argClass) || hasString(argClass))
                    {
                        ResourceClass boxClass = unionize(Set.of(argClass), box);
                        Column col = argClass.toGeneralClass(boxClass, argument.get(relations, argClass), partCanBeBull)
                                .get(0);

                        builder.append("sparql." + function + "_rdfbox(" + base + ", " + col + ")");
                    }

                    if(!builder.isEmpty())
                        variants.add(new ExpressionColumn(builder.toString(), VARCHAR));
                }

                List<Column> result = List.of(coalesce(variants));

                return new SqlBuiltinCall(function, distinct, arguments, singletonMap(iri, result), canBeNull);
            }

            case "bnode":
            {
                if(arguments.isEmpty())
                {
                    if(!restriction.contains(relations, bnodeIntBlankNode))
                        return new SqlBuiltinCall(function, distinct, arguments, singletonMap(bnodeIntBlankNode, null),
                                false);

                    List<Column> result = List.of(new ExpressionColumn("sparql.bnode()", INT4, false));

                    return new SqlBuiltinCall(function, distinct, arguments, singletonMap(bnodeIntBlankNode, result),
                            false);
                }
                else
                {
                    SqlExpressionIntercode argument = arguments.get(0);

                    if(argument.getResourceClasses().stream().noneMatch(r -> hasString(r)))
                        return SqlNull.get();

                    boolean canBeNull = argument.canBeNull()
                            || argument.getResourceClasses().stream().anyMatch(r -> !isString(r));

                    if(!restriction.contains(relations, bnodeStrBlankNode))
                        return new SqlBuiltinCall(function, distinct, arguments, singletonMap(bnodeStrBlankNode, null),
                                canBeNull);

                    List<Column> result = argument.get(relations, xsdString);

                    return new SqlBuiltinCall(function, distinct, arguments, singletonMap(bnodeStrBlankNode, result),
                            canBeNull);
                }
            }

            case "strdt":
            {
                SqlExpressionIntercode argument = arguments.get(0);
                SqlExpressionIntercode type = arguments.get(1);

                if(argument.getResourceClasses().stream().noneMatch(r -> hasString(r)))
                    return SqlNull.get();

                if(type.getResourceClasses().stream().noneMatch(r -> hasIri(r)))
                    return SqlNull.get();

                boolean canBeNull = argument.canBeNull() || type.canBeNull()
                        || argument.getResourceClasses().stream().anyMatch(r -> !isString(r))
                        || type.getResourceClasses().stream().anyMatch(r -> !isIri(r));

                ResourceClass resultClass = literal;

                if(type instanceof SqlIri iri)
                {
                    // TODO: add a variant for case the argument is a constant

                    Datatype datatype = request.getConfiguration().getDatatype(iri.getIri());
                    ResourceClass resourceClass = datatype == null ? null : datatype.getBaseLiteralClass();

                    boolean argumentIsString = argument.getResourceClasses().stream().allMatch(r -> isString(r));

                    if(xsdString.equals(resourceClass) && argumentIsString
                            && restriction.contains(relations, xsdString))
                    {
                        return argument;
                    }
                    else if(xsdString.equals(resourceClass))
                    {
                        List<Column> result = !restriction.contains(relations, xsdString) ? null :
                                argument.get(relations, xsdString);
                        return new SqlBuiltinCall(function, arguments, singletonMap(xsdString, result), canBeNull);
                    }
                    else if(resourceClass == null && argumentIsString && !argument.canBeNull())
                    {
                        List<Column> result = !restriction.contains(relations, unsupportedType) ? null :
                                List.of(argument.get(relations, xsdString).get(0),
                                        new ValueColumn(iri.getIri().getValue(), VARCHAR));

                        return new SqlBuiltinCall(function, arguments, singletonMap(unsupportedType, result),
                                canBeNull);
                    }
                    else if(resourceClass != null)
                    {
                        resultClass = unionize(Set.of(unsupportedType, resourceClass), box);
                    }
                    else
                    {
                        resultClass = unionize(Set.of(unsupportedType), box);
                    }
                }

                if(!restriction.contains(relations, resultClass))
                    return new SqlBuiltinCall(function, distinct, arguments, singletonMap(resultClass, null),
                            canBeNull);

                List<Column> result = List.of(new ExpressionColumn("sparql.rdfbox_create_from_typedliteral("
                        + argument.get(relations, xsdString).get(0) + ", " + type.get(relations, iri).get(0) + ")",
                        RDFBOX, canBeNull));

                return new SqlBuiltinCall(function, distinct, arguments, singletonMap(resultClass, result), canBeNull);
            }

            case "strlang":
            {
                SqlExpressionIntercode argument = arguments.get(0);
                SqlExpressionIntercode lang = arguments.get(1);

                if(argument.getResourceClasses().stream().noneMatch(r -> hasString(r)))
                    return SqlNull.get();

                if(lang.getResourceClasses().stream().noneMatch(r -> hasString(r)))
                    return SqlNull.get();

                if(lang instanceof SqlLiteral literal)
                {
                    String tag = literal.getLiteral().getValue();

                    if(!tag.matches(languageTagPattern))
                        return SqlNull.get();

                    // TODO: add a variant for case the argument is a constant

                    boolean canBeNull = argument.canBeNull()
                            || argument.getResourceClasses().stream().anyMatch(r -> !isString(r));
                    LangStringWithTagClass resultClass = LangStringWithTagClass.get(tag);

                    if(!restriction.contains(relations, resultClass))
                        return new SqlBuiltinCall(function, arguments, singletonMap(resultClass, null), canBeNull);

                    List<Column> result = argument.get(relations, xsdString);

                    return new SqlBuiltinCall(function, arguments, singletonMap(resultClass, result), canBeNull);
                }

                if(!restriction.contains(relations, rdfLangString))
                    return new SqlBuiltinCall(function, arguments, singletonMap(rdfLangString, null), true);

                ResourceClass resultClass = unionize(Set.of(rdfLangString), box);

                List<Column> result = List.of(new ExpressionColumn(
                        "sparql.rdfbox_create_from_langstring(" + argument.get(relations, xsdString).get(0) + ", "
                                + lang.get(relations, xsdString).get(0) + ")",
                        RDFBOX));

                return new SqlBuiltinCall(function, arguments, singletonMap(resultClass, result), true);
            }

            case "strlangdir":
            {
                SqlExpressionIntercode argument = arguments.get(0);
                SqlExpressionIntercode lang = arguments.get(1);
                SqlExpressionIntercode dir = arguments.get(2);

                if(argument.getResourceClasses().stream().noneMatch(r -> hasString(r)))
                    return SqlNull.get();

                if(lang.getResourceClasses().stream().noneMatch(r -> hasString(r)))
                    return SqlNull.get();

                if(dir.getResourceClasses().stream().noneMatch(r -> hasString(r)))
                    return SqlNull.get();

                if(lang instanceof SqlLiteral langLiteral && dir instanceof SqlLiteral dirLiteral)
                {
                    String tag = langLiteral.getLiteral().getValue();
                    Direction direction = Direction.fromText(dirLiteral.getLiteral().getValue());

                    if(!tag.matches(languageTagPattern) || direction == null)
                        return SqlNull.get();

                    // TODO: add a variant for case the argument is a constant

                    boolean canBeNull = argument.canBeNull()
                            || argument.getResourceClasses().stream().anyMatch(r -> !isString(r));
                    DirLangStringWithTagClass resultClass = DirLangStringWithTagClass.get(direction, tag);

                    if(!restriction.contains(relations, resultClass))
                        return new SqlBuiltinCall(function, arguments, singletonMap(resultClass, null), canBeNull);

                    List<Column> result = argument.get(relations, xsdString);

                    return new SqlBuiltinCall(function, arguments, singletonMap(resultClass, result), canBeNull);
                }

                if(!restriction.contains(relations, dirLanguageTaggedString))
                    return new SqlBuiltinCall(function, arguments, singletonMap(dirLanguageTaggedString, null), true);

                List<Column> result = List
                        .of(new ExpressionColumn(
                                "sparql.strlangdir_string(" + argument.get(relations, xsdString).get(0) + ", "
                                        + lang.get(relations, xsdString).get(0) + ", "
                                        + dir.get(relations, xsdString).get(0) + ")",
                                dirLanguageTaggedString.getSqlTypes().get(0)));

                return new SqlBuiltinCall(function, arguments, singletonMap(dirLanguageTaggedString, result), true);
            }

            case "uuid":
            {
                if(!restriction.contains(relations, iri))
                    return new SqlBuiltinCall(function, distinct, arguments, singletonMap(iri, null), false);

                List<Column> result = List
                        .of(new ExpressionColumn("('urn:uuid:' || uuid.uuid_generate_v4())::varchar", VARCHAR, false));

                return new SqlBuiltinCall(function, distinct, arguments, singletonMap(iri, result), false);
            }

            case "struuid":
            {
                if(!restriction.contains(relations, xsdString))
                    return new SqlBuiltinCall(function, distinct, arguments, singletonMap(xsdString, null), false);

                List<Column> result = List.of(new ExpressionColumn("uuid.uuid_generate_v4()::varchar", VARCHAR, false));

                return new SqlBuiltinCall(function, distinct, arguments, singletonMap(xsdString, result), false);
            }


            // functions on triple terms:

            case "triple":
            {
                SqlExpressionIntercode subject = arguments.get(0);
                SqlExpressionIntercode predicate = arguments.get(1);
                SqlExpressionIntercode object = arguments.get(2);

                if(subject.equals(SqlNull.get()) || predicate.equals(SqlNull.get()) || object.equals(SqlNull.get()))
                    return SqlNull.get();

                // the subject has to be an IRI or a blank node and the predicate an IRI, anything else is an error
                Set<ResourceClass> subjectClasses = subject.getResourceClasses().stream().filter(r -> hasReference(r))
                        .collect(toSet());
                Set<ResourceClass> predicateClasses = predicate.getResourceClasses().stream().filter(r -> hasIri(r))
                        .collect(toSet());
                Set<ResourceClass> objectClasses = object.getResourceClasses();

                if(subjectClasses.isEmpty() || predicateClasses.isEmpty() || objectClasses.isEmpty())
                    return SqlNull.get();

                boolean canBeNull = subject.canBeNull() || predicate.canBeNull() || object.canBeNull()
                        || subject.getResourceClasses().stream().anyMatch(r -> !isReference(r))
                        || predicate.getResourceClasses().stream().anyMatch(r -> !isIri(r));

                boolean subjectCanBeNull = subject.canBeNull() || subject.getResourceClasses().size() > 1;
                boolean predicateCanBeNull = predicate.canBeNull() || predicate.getResourceClasses().size() > 1;
                boolean objectCanBeNull = object.canBeNull() || object.getResourceClasses().size() > 1;

                Map<ResourceClass, List<Column>> mappings = new HashMap<>();

                for(ResourceClass subjectClass : subjectClasses)
                {
                    // a subject that may be another kind of term stays in the box, where it can be tested
                    ResourceClass subjectComponent = isReference(subjectClass) ? subjectClass :
                            unionize(Set.of(subjectClass), box);

                    for(ResourceClass predicateClass : predicateClasses)
                    {
                        // a predicate that may be another kind of term is decoded into the IRI class
                        ResourceClass predicateComponent = isIri(predicateClass) ? predicateClass : iri;

                        for(ResourceClass objectClass : objectClasses)
                        {
                            TripleTermClass resultClass = new TripleTermClass(subjectComponent, predicateComponent,
                                    objectClass);

                            if(!restriction.contains(relations, resultClass))
                            {
                                mappings.put(resultClass, null);
                                continue;
                            }

                            // the columns of the term are null together: when a component is missing or is not of
                            // the kind its position requires, the whole term is an error
                            List<String> conditions = new ArrayList<>();
                            List<Column> columns = new ArrayList<>(resultClass.getColumnCount());

                            List<Column> subjectColumns = subject.get(relations, subjectClass);

                            if(subjectCanBeNull)
                                conditions.add(subject.getIsNotNull(relations, subjectClass));

                            if(!isReference(subjectClass))
                            {
                                subjectColumns = subjectClass.toGeneralClass(subjectComponent, subjectColumns,
                                        subjectCanBeNull);
                                conditions.add("(sparql.isiri_rdfbox(" + subjectColumns.get(0)
                                        + ") OR sparql.isblank_rdfbox(" + subjectColumns.get(0) + "))");
                            }

                            List<Column> predicateColumns = predicate.get(relations, predicateClass);

                            if(predicateCanBeNull)
                                conditions.add(predicate.getIsNotNull(relations, predicateClass));

                            if(!isIri(predicateClass))
                            {
                                Column boxed = predicateClass.toGeneralClass(unionize(Set.of(predicateClass), box),
                                        predicateColumns, predicateCanBeNull).get(0);
                                predicateColumns = List
                                        .of(new ExpressionColumn("sparql.rdfbox_get_iri(" + boxed + ")", VARCHAR));
                                conditions.add("sparql.isiri_rdfbox(" + boxed + ")");
                            }

                            List<Column> objectColumns = object.get(relations, objectClass);

                            if(objectCanBeNull)
                                conditions.add(object.getIsNotNull(relations, objectClass));

                            columns.addAll(subjectColumns);
                            columns.addAll(predicateColumns);
                            columns.addAll(objectColumns);

                            if(!conditions.isEmpty())
                            {
                                String condition = conditions.stream().collect(joining(" AND "));
                                List<SqlType> types = resultClass.getSqlTypes();

                                for(int i = 0; i < columns.size(); i++)
                                    columns.set(i,
                                            new ExpressionColumn(
                                                    "CASE WHEN " + condition + " THEN " + columns.get(i) + " END",
                                                    types.get(i)));
                            }

                            mappings.put(resultClass, columns);
                        }
                    }
                }

                return new SqlBuiltinCall(function, distinct, arguments, mappings, canBeNull);
            }

            case "subject":
            case "predicate":
            case "object":
            {
                SqlExpressionIntercode argument = arguments.get(0);

                Set<ResourceClass> classes = argument.getResourceClasses().stream().filter(r -> hasTripleTerm(r))
                        .collect(toSet());

                if(classes.isEmpty())
                    return SqlNull.get();

                Component component = switch(function)
                {
                    case "subject" -> Component.SUBJECT;
                    case "predicate" -> Component.PREDICATE;
                    default -> Component.OBJECT;
                };

                SqlValue value = SqlValue.tripleTermComponent(relations, argument, classes, component);

                Map<ResourceClass, List<Column>> mappings = new HashMap<>();

                for(Entry<ResourceClass, List<Column>> e : value.getMappings().entrySet())
                    mappings.put(e.getKey(), restriction.contains(relations, e.getKey()) ? e.getValue() : null);

                return new SqlBuiltinCall(function, distinct, arguments, mappings, value.canBeNull());
            }


            // functions on strings:

            case "strlen":
            {
                SqlExpressionIntercode argument = arguments.get(0);

                if(argument.getResourceClasses().stream().noneMatch(r -> hasStringLiteral(r)))
                    return SqlNull.get();

                boolean canBeNull = argument.canBeNull()
                        || argument.getResourceClasses().stream().anyMatch(r -> !isStringLiteral(r));


                if(!restriction.contains(relations, xsdInteger))
                    return new SqlBuiltinCall(function, distinct, arguments, singletonMap(xsdInteger, null), canBeNull);

                boolean partCanBeBull = argument.canBeNull() || argument.getResourceClasses().size() > 1;

                Set<Column> variants = new HashSet<>();

                for(ResourceClass argumentClass : argument.getResourceClasses())
                {
                    StringBuilder builder = new StringBuilder();

                    if(isFixedTagClass(argumentClass))
                    {
                        builder.append("length(");
                        builder.append(argument.getMapping(argumentClass).get(0));
                        builder.append(")::decimal");
                    }
                    else if(isString(argumentClass))
                    {
                        builder.append("length(");
                        builder.append(argumentClass
                                .toGeneralClass(xsdString, argument.get(relations, argumentClass), partCanBeBull)
                                .get(0));
                        builder.append(")::decimal");
                    }
                    else if(argument.canSafelyGeneralize(relations, argumentClass, rdfLangString))
                    {
                        builder.append("length(");
                        builder.append(argumentClass
                                .toGeneralClass(rdfLangString, argument.get(relations, argumentClass), partCanBeBull)
                                .get(0));
                        builder.append(")::decimal");
                    }
                    else if(argument.canSafelyGeneralize(relations, argumentClass, rdfLtrLangString))
                    {
                        builder.append("length(");
                        builder.append(argumentClass
                                .toGeneralClass(rdfLtrLangString, argument.get(relations, argumentClass), partCanBeBull)
                                .get(0));
                        builder.append(")::decimal");
                    }
                    else if(argument.canSafelyGeneralize(relations, argumentClass, rdfRtlLangString))
                    {
                        builder.append("length(");
                        builder.append(argumentClass
                                .toGeneralClass(rdfRtlLangString, argument.get(relations, argumentClass), partCanBeBull)
                                .get(0));
                        builder.append(")::decimal");
                    }
                    else if(hasStringLiteral(argumentClass))
                    {
                        builder.append("sparql.strlen_rdfbox(");
                        builder.append(argument.get(relations, unionize(Set.of(argumentClass), box)).get(0));
                        builder.append(")");
                    }

                    if(!builder.isEmpty())
                        variants.add(new ExpressionColumn(builder.toString(), NUMERIC));
                }

                List<Column> result = List.of(coalesce(variants));

                return new SqlBuiltinCall(function, distinct, arguments, singletonMap(xsdInteger, result), canBeNull);
            }


            case "substr":
            {
                SqlExpressionIntercode argument = arguments.get(0);
                SqlExpressionIntercode location = arguments.get(1);
                SqlExpressionIntercode length = arguments.size() > 2 ? arguments.get(2) : null;

                if(argument.getResourceClasses().stream().noneMatch(r -> hasStringLiteral(r)))
                    return SqlNull.get();

                if(location.getResourceClasses().stream().noneMatch(r -> hasDerivatedFromInteger(r)))
                    return SqlNull.get();

                if(length != null && length.getResourceClasses().stream().noneMatch(r -> hasDerivatedFromInteger(r)))
                    return SqlNull.get();

                boolean additionalCanBeNull = location.canBeNull() || length != null && length.canBeNull()
                        || location.getResourceClasses().stream().anyMatch(r -> !isDerivatedFromInteger(r))
                        || length != null
                                && length.getResourceClasses().stream().anyMatch(r -> !isDerivatedFromInteger(r));

                boolean canBeNull = additionalCanBeNull || argument.canBeNull()
                        || argument.getResourceClasses().stream().anyMatch(r -> !isStringLiteral(r));


                Set<ResourceClass> locationClasses = location.getResourceClasses().stream()
                        .filter(r -> hasDerivatedFromInteger(r)).collect(toSet());

                Column locationCol = location.promoteNumericAs(locationClasses, xsdInteger);

                Set<ResourceClass> lengthClasses = length == null ? null :
                        length.getResourceClasses().stream().filter(r -> hasDerivatedFromInteger(r)).collect(toSet());

                Column lengthCol = length == null ? null : length.promoteNumericAs(lengthClasses, xsdInteger);


                Map<ResourceClass, Set<List<ResourceClass>>> map = new HashMap<>();

                for(ResourceClass resClass : argument.getResourceClasses())
                {
                    if(!hasStringLiteral(resClass))
                        continue;

                    ResourceClass resultClass = getStringLiteralResultClass(resClass);

                    if((resultClass.equals(rdfLangString) || resultClass.equals(rdfLtrLangString)
                            || resultClass.equals(rdfRtlLangString)) && additionalCanBeNull)
                        resultClass = unionize(Set.of(resultClass), box);

                    List<ResourceClass> list = new ArrayList<>();

                    list.add(resClass);
                    list.add(integerNumeric);

                    if(length != null)
                        list.add(integerNumeric);

                    map.computeIfAbsent(resultClass, _ -> new HashSet<>()).add(list);
                }

                Map<ResourceClass, Set<List<Set<ResourceClass>>>> resMap = processResultMap(relations, arguments, map,
                        restriction, box);

                Map<ResourceClass, List<Column>> mappings = new HashMap<>();

                for(Entry<ResourceClass, Set<List<Set<ResourceClass>>>> e : resMap.entrySet())
                {
                    List<Column> result = null;

                    if(e.getValue() != null)
                    {
                        Set<ResourceClass> resClasses = e.getValue().stream().flatMap(a -> a.get(0).stream())
                                .collect(toSet());

                        if(xsdString.equals(e.getKey()) || isFixedTagClass(e.getKey()))
                        {
                            Column col = argument.getStringLiteral(relations, resClasses);

                            StringBuilder builder = new StringBuilder();

                            builder.append("substring(");
                            builder.append(col);
                            builder.append(", (");
                            builder.append(locationCol);
                            builder.append(")::integer");

                            if(length != null)
                            {
                                builder.append(", (");
                                builder.append(lengthCol);
                                builder.append(")::integer");
                            }

                            builder.append(")::varchar");

                            result = List.of(new ExpressionColumn(builder.toString(), VARCHAR));
                        }
                        else if(rdfLangString.equals(e.getKey()) || rdfLtrLangString.equals(e.getKey())
                                || rdfRtlLangString.equals(e.getKey()))
                        {
                            List<Column> cols = argument.get(relations,
                                    unionize(resClasses, (PrimitiveResourceClass) e.getKey()));

                            StringBuilder builder = new StringBuilder();

                            builder.append("substring(");
                            builder.append(cols.get(0));
                            builder.append(", (");
                            builder.append(locationCol);
                            builder.append(")::integer");

                            if(length != null)
                            {
                                builder.append(", (");
                                builder.append(lengthCol);
                                builder.append(")::integer");
                            }

                            builder.append(")::varchar");

                            result = List.of(new ExpressionColumn(builder.toString(), VARCHAR), cols.get(1));
                        }
                        else if(e.getKey() != null)
                        {
                            List<Column> cols = argument.get(relations, unionize(resClasses, box));

                            StringBuilder builder = new StringBuilder();

                            builder.append("sparql.substr_rdfbox(");
                            builder.append(cols.get(0));
                            builder.append(", ");
                            builder.append(locationCol);

                            if(length != null)
                            {
                                builder.append(", ");
                                builder.append(lengthCol);
                            }

                            builder.append(")");

                            result = List.of(new ExpressionColumn(builder.toString(), RDFBOX));
                        }
                    }

                    mappings.put(e.getKey(), result);
                }

                return new SqlBuiltinCall(function, distinct, arguments, mappings, canBeNull);
            }

            case "ucase":
            case "lcase":
            {
                SqlExpressionIntercode argument = arguments.get(0);

                if(argument.getResourceClasses().stream().noneMatch(r -> hasStringLiteral(r)))
                    return SqlNull.get();

                boolean canBeNull = argument.canBeNull()
                        || argument.getResourceClasses().stream().anyMatch(r -> !isStringLiteral(r));



                Map<ResourceClass, Set<List<ResourceClass>>> map = new HashMap<>();

                for(ResourceClass resClass : argument.getResourceClasses())
                {
                    if(!hasStringLiteral(resClass))
                        continue;

                    ResourceClass resultClass = getStringLiteralResultClass(resClass);
                    map.computeIfAbsent(resultClass, _ -> new HashSet<>()).add(List.of(resClass));
                }

                Map<ResourceClass, Set<List<Set<ResourceClass>>>> resMap = processResultMap(relations, arguments, map,
                        restriction, box);

                Map<ResourceClass, List<Column>> mappings = new HashMap<>();

                for(Entry<ResourceClass, Set<List<Set<ResourceClass>>>> e : resMap.entrySet())
                {
                    List<Column> result = null;

                    if(e.getValue() != null)
                    {
                        Set<ResourceClass> resClasses = e.getValue().stream().flatMap(a -> a.get(0).stream())
                                .collect(toSet());

                        if(xsdString.equals(e.getKey()) || isFixedTagClass(e.getKey()))
                        {
                            List<Column> cols = argument.get(relations,
                                    unionize(resClasses, (PrimitiveResourceClass) e.getKey()));

                            StringBuilder builder = new StringBuilder();

                            builder.append(function.equals("lcase") ? "lower(" : "upper(");
                            builder.append(cols.get(0));
                            builder.append(")::varchar");

                            result = List.of(new ExpressionColumn(builder.toString(), VARCHAR));
                        }
                        else if(rdfLangString.equals(e.getKey()) || rdfLtrLangString.equals(e.getKey())
                                || rdfRtlLangString.equals(e.getKey()))
                        {
                            List<Column> cols = argument.get(relations,
                                    unionize(resClasses, (PrimitiveResourceClass) e.getKey()));

                            StringBuilder builder = new StringBuilder();

                            builder.append(function.equals("lcase") ? "lower(" : "upper(");
                            builder.append(cols.get(0));
                            builder.append(")::varchar");

                            result = List.of(new ExpressionColumn(builder.toString(), VARCHAR), cols.get(1));
                        }
                        else if(e.getKey() != null)
                        {
                            List<Column> cols = argument.get(relations, unionize(resClasses, box));

                            StringBuilder builder = new StringBuilder();

                            builder.append("sparql.");
                            builder.append(function);
                            builder.append("_rdfbox(");
                            builder.append(cols.get(0));
                            builder.append(")");

                            result = List.of(new ExpressionColumn(builder.toString(), RDFBOX));
                        }
                    }

                    mappings.put(e.getKey(), result);
                }

                return new SqlBuiltinCall(function, distinct, arguments, mappings, canBeNull);
            }

            case "strstarts":
            case "strends":
            case "contains":
            {
                SqlExpressionIntercode left = arguments.get(0);
                SqlExpressionIntercode right = arguments.get(1);

                if(!areStringLiteralCompatible(left, right))
                    return SqlNull.get();

                boolean canBeNull = left.canBeNull() || right.canBeNull() || areStringLiteralIncompatible(left, right);

                Map<ResourceClass, Set<List<ResourceClass>>> map = new HashMap<>();

                for(ResourceClass l : left.getResourceClasses())
                    for(ResourceClass r : right.getResourceClasses())
                        if(areStringLiteralsCompatible(l, r))
                            map.computeIfAbsent(xsdBoolean, _ -> new HashSet<>()).add(List.of(l, r));

                Set<List<Set<ResourceClass>>> variants = processResultMap(relations, arguments, map, restriction)
                        .get(xsdBoolean);

                if(variants == null)
                    return new SqlBuiltinCall(function, distinct, arguments, singletonMap(xsdBoolean, null), canBeNull);

                Set<Column> cols = new HashSet<>();

                for(List<Set<ResourceClass>> variant : variants)
                {
                    StringBuilder builder = new StringBuilder();

                    Set<ResourceClass> leftClasses = variant.get(0);
                    Set<ResourceClass> rightClasses = variant.get(1);

                    PrimitiveResourceClass leftUnionClass = unionize(leftClasses).getEffectiveClass();
                    PrimitiveResourceClass rightUnionClass = unionize(rightClasses).getEffectiveClass();

                    PrimitiveResourceClass leftTaggedClass = getLanguageTaggedClass(leftClasses);
                    PrimitiveResourceClass rightTaggedClass = getLanguageTaggedClass(rightClasses);


                    if(leftUnionClass.equals(box) && rightUnionClass.isSubclassOf(xsdString))
                    {
                        builder.append("sparql.");
                        builder.append(function);
                        builder.append("_rdfbox_string(");
                        builder.append(left.get(relations, unionize(leftClasses, box)).get(0));
                        builder.append(", ");
                        builder.append(right.get(relations, unionize(rightClasses, xsdString)).get(0));
                        builder.append(")");
                    }
                    else if(rightUnionClass.isSubclassOf(xsdString)
                            || isFixedTagClass(leftUnionClass) && isFixedTagClass(rightUnionClass))
                    {
                        builder.append("sparql.");
                        builder.append(function);
                        builder.append("_string_string(");
                        builder.append(left.getStringLiteral(relations, leftClasses));
                        builder.append(", ");
                        builder.append(right.getStringLiteral(relations, rightClasses));
                        builder.append(")");
                    }
                    else if(leftTaggedClass != null && leftTaggedClass.equals(rightTaggedClass))
                    {
                        List<Column> leftCols = left.get(relations, unionize(leftClasses, leftTaggedClass));
                        List<Column> rightCols = right.get(relations, unionize(rightClasses, rightTaggedClass));

                        builder.append("CASE WHEN ");
                        builder.append(leftCols.get(1));
                        builder.append(" = ");
                        builder.append(rightCols.get(1));
                        builder.append(" THEN sparql.");
                        builder.append(function);
                        builder.append("_string_string(");
                        builder.append(leftCols.get(0));
                        builder.append(", ");
                        builder.append(rightCols.get(0));
                        builder.append(") END");
                    }
                    else if(rightTaggedClass != null && getFixedTag(leftUnionClass, rightTaggedClass) != null)
                    {
                        List<Column> leftCols = left.get(relations, unionize(leftClasses, leftUnionClass));
                        List<Column> rightCols = right.get(relations, unionize(rightClasses, rightTaggedClass));

                        builder.append("CASE WHEN '");
                        builder.append(getFixedTag(leftUnionClass, rightTaggedClass));
                        builder.append("'::varchar = ");
                        builder.append(rightCols.get(1));
                        builder.append(" THEN sparql.");
                        builder.append(function);
                        builder.append("_string_string(");
                        builder.append(leftCols.get(0));
                        builder.append(", ");
                        builder.append(rightCols.get(0));
                        builder.append(") END");
                    }
                    else if(leftTaggedClass != null && getFixedTag(rightUnionClass, leftTaggedClass) != null)
                    {
                        List<Column> leftCols = left.get(relations, unionize(leftClasses, leftTaggedClass));
                        List<Column> rightCols = right.get(relations, unionize(rightClasses, rightUnionClass));

                        builder.append("CASE WHEN ");
                        builder.append(leftCols.get(1));
                        builder.append(" = '");
                        builder.append(getFixedTag(rightUnionClass, leftTaggedClass));
                        builder.append("'::varchar THEN sparql.");
                        builder.append(function);
                        builder.append("_string_string(");
                        builder.append(leftCols.get(0));
                        builder.append(", ");
                        builder.append(rightCols.get(0));
                        builder.append(") END");
                    }
                    else
                    {
                        builder.append("sparql.");
                        builder.append(function);
                        builder.append("_rdfbox_rdfbox(");
                        builder.append(left.get(relations, unionize(leftClasses, box)).get(0));
                        builder.append(", ");
                        builder.append(right.get(relations, unionize(rightClasses, box)).get(0));
                        builder.append(")");
                    }

                    cols.add(new ExpressionColumn(builder.toString(), BOOL));
                }

                List<Column> result = List.of(coalesce(cols));

                return new SqlBuiltinCall(function, distinct, arguments, singletonMap(xsdBoolean, result), canBeNull);
            }

            case "strbefore":
            case "strafter":
            {
                SqlExpressionIntercode left = arguments.get(0);
                SqlExpressionIntercode right = arguments.get(1);

                if(!areStringLiteralCompatible(left, right))
                    return SqlNull.get();

                boolean canBeNull = left.canBeNull() || right.canBeNull() || areStringLiteralIncompatible(left, right);

                Map<ResourceClass, Set<List<ResourceClass>>> map = new HashMap<>();

                for(ResourceClass l : left.getResourceClasses())
                    for(ResourceClass r : right.getResourceClasses())
                        if(areStringLiteralsCompatible(l, r))
                            map.computeIfAbsent(getStringLiteralResultClass2(l), _ -> new HashSet<>())
                                    .add(List.of(l, r));

                Map<ResourceClass, Set<List<Set<ResourceClass>>>> resMap = processResultMap(relations, arguments, map,
                        restriction);

                Map<ResourceClass, List<Column>> mappings = new HashMap<>();

                for(Entry<ResourceClass, Set<List<Set<ResourceClass>>>> e : resMap.entrySet())
                {
                    ResourceClass resultClass = e.getKey();
                    Set<List<Set<ResourceClass>>> variants = e.getValue();

                    if(variants == null)
                    {
                        mappings.put(e.getKey(), null);
                        continue;
                    }

                    Set<Column> cols = new HashSet<>();

                    for(List<Set<ResourceClass>> variant : variants)
                    {
                        StringBuilder builder = new StringBuilder();

                        Set<ResourceClass> leftClasses = variant.get(0);
                        Set<ResourceClass> rightClasses = variant.get(1);

                        if(resultClass.equals(xsdString))
                        {
                            builder.append("sparql.");
                            builder.append(function);
                            builder.append("_string_string(");
                            builder.append(left.get(relations, unionize(leftClasses, xsdString)).get(0));
                            builder.append(", ");
                            builder.append(right.get(relations, unionize(rightClasses, xsdString)).get(0));
                            builder.append(")");
                        }
                        else if(rightClasses.stream().allMatch(r -> isString(r)))
                        {
                            builder.append("sparql.");
                            builder.append(function);
                            builder.append("_rdfbox_string(");
                            builder.append(left.get(relations, unionize(leftClasses, box)).get(0));
                            builder.append(", ");
                            builder.append(right.get(relations, unionize(rightClasses, xsdString)).get(0));
                            builder.append(")");
                        }
                        else
                        {
                            builder.append("sparql.");
                            builder.append(function);
                            builder.append("_rdfbox_rdfbox(");
                            builder.append(left.get(relations, unionize(leftClasses, box)).get(0));
                            builder.append(", ");
                            builder.append(right.get(relations, unionize(rightClasses, box)).get(0));
                            builder.append(")");
                        }

                        cols.add(new ExpressionColumn(builder.toString(), resultClass.getSqlTypes().get(0)));
                    }

                    mappings.put(e.getKey(), List.of(coalesce(cols)));
                }

                return new SqlBuiltinCall(function, distinct, arguments, mappings, canBeNull);
            }

            case "encode_for_uri":
            {
                SqlExpressionIntercode argument = arguments.get(0);

                if(argument.getResourceClasses().stream().noneMatch(r -> hasStringLiteral(r)))
                    return SqlNull.get();

                boolean canBeNull = argument.canBeNull()
                        || argument.getResourceClasses().stream().anyMatch(r -> !isStringLiteral(r));


                if(!restriction.contains(relations, xsdString))
                    return new SqlBuiltinCall(function, distinct, arguments, singletonMap(xsdString, null), canBeNull);


                Set<Column> variants = new HashSet<>();

                for(ResourceClass argumentClass : argument.getResourceClasses())
                {
                    StringBuilder builder = new StringBuilder();

                    ResourceClass ec = argumentClass.getEffectiveClass();

                    if(ec.equals(xsdString) || ec.equals(rdfLangString) || ec.equals(rdfLtrLangString)
                            || ec.equals(rdfRtlLangString) || isFixedTagClass(ec))
                    {
                        List<Column> cols = argument.get(relations, argumentClass);

                        builder.append("sparql.encode_for_uri_string(");
                        builder.append(cols.get(0));
                        builder.append(")");
                    }
                    else if(hasStringLiteral(argumentClass))
                    {
                        List<Column> cols = argument.get(relations, unionize(Set.of(argumentClass), box));

                        builder.append("sparql.encode_for_uri_rdfbox(");
                        builder.append(cols.get(0));
                        builder.append(")");
                    }

                    if(!builder.isEmpty())
                        variants.add(new ExpressionColumn(builder.toString(), VARCHAR));
                }

                List<Column> result = List.of(coalesce(variants));

                return new SqlBuiltinCall(function, distinct, arguments, singletonMap(xsdString, result), canBeNull);
            }


            case "concat":
            {
                if(arguments.size() == 0)
                {
                    if(restriction.contains(relations, xsdString))
                        return SqlLiteral.create(request, new TypedLiteral("", xsdStringIri));
                    else
                        return new SqlBuiltinCall(function, distinct, arguments, singletonMap(xsdString, null), false);
                }

                boolean canBeNull = arguments.stream().anyMatch(
                        a -> a.canBeNull() || a.getResourceClasses().stream().anyMatch(r -> !isStringLiteral(r)));

                if(arguments.stream()
                        .anyMatch(a -> a.getResourceClasses().stream().noneMatch(r -> hasStringLiteral(r))))
                    return SqlNull.get();


                Set<ResourceClass> resClasses = arguments.get(0).getResourceClasses();

                for(int i = 0; i < arguments.size(); i++)
                {
                    Set<ResourceClass> newClasses = new HashSet<>();

                    for(ResourceClass rx : arguments.get(i).getResourceClasses())
                    {
                        ResourceClass x = rx.getEffectiveClass();

                        for(ResourceClass ry : resClasses)
                        {
                            ResourceClass y = ry.getEffectiveClass();

                            if(isString(x) || isString(y))
                                newClasses.add(xsdString);
                            else if(isFixedTagClass(x) && x.equals(y))
                                newClasses.add(x);
                            else if(isFixedTagClass(x) && isFixedTagClass(y))
                                newClasses.add(xsdString);
                            else if(hasStringLiteral(x) && hasStringLiteral(y))
                                newClasses.addAll(Set.of(rdfLangString, rdfLtrLangString, rdfRtlLangString, xsdString));
                        }
                    }

                    resClasses = newClasses;
                }

                ResourceClass resultClass = resClasses.size() == 1 ? resClasses.iterator().next() : boxedStringLiteral;

                if(!restriction.contains(relations, resultClass))
                    return new SqlBuiltinCall(function, distinct, arguments, singletonMap(resultClass, null),
                            canBeNull);

                if(arguments.size() == 1)
                    return new SqlBuiltinCall(function, distinct, arguments,
                            singletonMap(resultClass, arguments.get(0).get(relations, resultClass)), canBeNull);


                StringBuilder builder = new StringBuilder();

                if(resultClass.equals(xsdString) || isFixedTagClass(resultClass))
                {
                    builder.append("concat(");
                    builder.append(arguments.get(0).getStringLiteral());

                    for(int i = 1; i < arguments.size(); i++)
                        builder.append(", ").append(arguments.get(i).getStringLiteral());

                    builder.append(")");
                }
                else
                {
                    for(int i = 1; i < arguments.size(); i++)
                        builder.append("sparql.concat_rdfbox_rdfbox(");

                    builder.append(arguments.get(0).get(relations, boxedStringLiteral).get(0));

                    for(int i = 1; i < arguments.size(); i++)
                    {
                        builder.append(", ");
                        builder.append(arguments.get(i).get(relations, boxedStringLiteral).get(0));
                        builder.append(")");
                    }
                }

                List<Column> result = List
                        .of(new ExpressionColumn(builder.toString(), resultClass.getSqlTypes().get(0), canBeNull));

                return new SqlBuiltinCall(function, distinct, arguments, singletonMap(resultClass, result), canBeNull);
            }

            case "langmatches":
            {
                SqlExpressionIntercode lang = arguments.get(0);
                SqlExpressionIntercode pattern = arguments.get(1);

                if(lang.getResourceClasses().stream().noneMatch(r -> hasString(r)))
                    return SqlNull.get();

                if(pattern.getResourceClasses().stream().noneMatch(r -> hasString(r)))
                    return SqlNull.get();

                boolean canBeNull = lang.canBeNull() || pattern.canBeNull()
                        || lang.getResourceClasses().stream().anyMatch(r -> !isString(r))
                        || pattern.getResourceClasses().stream().anyMatch(r -> !isString(r));

                if(!restriction.contains(relations, xsdBoolean))
                    return new SqlBuiltinCall(function, distinct, arguments, singletonMap(xsdBoolean, null), canBeNull);

                if(lang.getResourceClasses().stream().anyMatch(r -> hasString(r) && !isString(r)))
                {
                    Column lcol = lang.get(relations, unionize(Set.of(xsdString), box)).get(0);
                    Column pcol = pattern.get(relations, unionize(Set.of(xsdString), box)).get(0);

                    List<Column> result = List.of(new ExpressionColumn(
                            "sparql.langmatches_rdfbox_rdfbox(" + lcol + ", " + pcol + ")", BOOL, canBeNull));

                    return new SqlBuiltinCall(function, distinct, arguments, singletonMap(xsdBoolean, result),
                            canBeNull);
                }
                else
                {
                    Column lcol = lang.get(relations, xsdString).get(0);
                    Column pcol = pattern.get(relations, xsdString).get(0);

                    List<Column> result = List.of(new ExpressionColumn(
                            "sparql.langmatches_string_string(" + lcol + ", " + pcol + ")", BOOL, canBeNull));

                    return new SqlBuiltinCall(function, distinct, arguments, singletonMap(xsdBoolean, result),
                            canBeNull);
                }
            }

            case "regex":
            {
                SqlExpressionIntercode argument = arguments.get(0);
                SqlExpressionIntercode pattern = arguments.get(1);
                SqlExpressionIntercode flags = arguments.size() > 2 ? arguments.get(2) : null;

                if(argument.getResourceClasses().stream().noneMatch(r -> hasStringLiteral(r)))
                    return SqlNull.get();

                if(pattern.getResourceClasses().stream().noneMatch(r -> hasString(r)))
                    return SqlNull.get();

                if(flags != null && flags.getResourceClasses().stream().noneMatch(r -> hasString(r)))
                    return SqlNull.get();

                boolean canBeNull = argument.canBeNull() || pattern.canBeNull() || flags != null && flags.canBeNull()
                        || argument.getResourceClasses().stream().anyMatch(r -> !isStringLiteral(r))
                        || pattern.getResourceClasses().stream().anyMatch(r -> !isString(r))
                        || flags != null && flags.getResourceClasses().stream().anyMatch(r -> !isString(r));

                //TODO: group based on string vs rdfbox mode

                Set<List<ResourceClass>> rawVariants = new HashSet<>();

                for(ResourceClass resClass : argument.getResourceClasses())
                {
                    if(!hasStringLiteral(resClass))
                        continue;

                    List<ResourceClass> list = new ArrayList<>();

                    list.add(resClass);
                    list.add(xsdString);

                    if(flags != null)
                        list.add(xsdString);

                    rawVariants.add(list);
                }


                Set<List<Set<ResourceClass>>> variants = processResultMap(relations, arguments,
                        singletonMap(xsdBoolean, rawVariants), restriction, box).get(xsdBoolean);


                Set<Column> cols = new HashSet<>();

                for(List<Set<ResourceClass>> variant : variants)
                {
                    StringBuilder builder = new StringBuilder();

                    if(variant.get(0).stream().anyMatch(r -> r.getEffectiveClass().equals(box)))
                    {
                        Column col = argument.get(relations, unionize(variant.get(0), box)).get(0);

                        builder.append("sparql.regex_rdfbox(");
                        builder.append(col);
                    }
                    else
                    {
                        Column col = argument.getStringLiteral(relations, variant.get(0));

                        builder.append("sparql.regex_string(");
                        builder.append(col);
                    }

                    builder.append(", ");
                    builder.append(pattern.get(relations, xsdString).get(0));

                    if(flags != null)
                        builder.append(", ").append(flags.get(relations, xsdString).get(0));

                    builder.append(")");

                    cols.add(new ExpressionColumn(builder.toString(), BOOL));
                }

                Map<ResourceClass, List<Column>> mappings = singletonMap(xsdBoolean, List.of(coalesce(cols)));

                return new SqlBuiltinCall(function, distinct, arguments, mappings, canBeNull);
            }

            case "replace":
            {
                SqlExpressionIntercode argument = arguments.get(0);
                SqlExpressionIntercode pattern = arguments.get(1);
                SqlExpressionIntercode replacement = arguments.get(2);
                SqlExpressionIntercode flags = arguments.size() > 3 ? arguments.get(3) : null;

                if(argument.getResourceClasses().stream().noneMatch(r -> hasStringLiteral(r)))
                    return SqlNull.get();

                if(pattern.getResourceClasses().stream().noneMatch(r -> hasString(r)))
                    return SqlNull.get();

                if(replacement.getResourceClasses().stream().noneMatch(r -> hasString(r)))
                    return SqlNull.get();

                if(flags != null && flags.getResourceClasses().stream().noneMatch(r -> hasString(r)))
                    return SqlNull.get();

                boolean additionalCanBeNull = pattern.canBeNull() || replacement.canBeNull()
                        || flags != null && flags.canBeNull()
                        || pattern.getResourceClasses().stream().anyMatch(r -> !isString(r))
                        || replacement.getResourceClasses().stream().anyMatch(r -> !isString(r))
                        || flags != null && flags.getResourceClasses().stream().anyMatch(r -> !isString(r));

                boolean canBeNull = additionalCanBeNull || argument.canBeNull()
                        || argument.getResourceClasses().stream().anyMatch(r -> !isStringLiteral(r));


                Map<ResourceClass, Set<List<ResourceClass>>> map = new HashMap<>();

                for(ResourceClass resClass : argument.getResourceClasses())
                {
                    if(!hasStringLiteral(resClass))
                        continue;

                    ResourceClass resultClass = getStringLiteralResultClass(resClass);

                    if((resultClass.equals(rdfLangString) || resultClass.equals(rdfLtrLangString)
                            || resultClass.equals(rdfRtlLangString)) && additionalCanBeNull)
                        resultClass = unionize(Set.of(resultClass), box);

                    List<ResourceClass> list = new ArrayList<>();

                    list.add(resClass);
                    list.add(xsdString);
                    list.add(xsdString);

                    if(flags != null)
                        list.add(xsdString);

                    map.computeIfAbsent(resultClass, _ -> new HashSet<>()).add(list);
                }

                Map<ResourceClass, Set<List<Set<ResourceClass>>>> resMap = processResultMap(relations, arguments, map,
                        restriction, box);

                Map<ResourceClass, List<Column>> mappings = new HashMap<>();

                for(Entry<ResourceClass, Set<List<Set<ResourceClass>>>> e : resMap.entrySet())
                {
                    List<Column> result = null;

                    if(e.getValue() != null)
                    {
                        Set<ResourceClass> resClasses = e.getValue().stream().flatMap(a -> a.get(0).stream())
                                .collect(toSet());

                        if(xsdString.equals(e.getKey()) || isFixedTagClass(e.getKey()))
                        {
                            Column col = argument.getStringLiteral(relations, resClasses);

                            StringBuilder builder = new StringBuilder();

                            builder.append("sparql.replace_string(");
                            builder.append(col);
                            builder.append(", ");
                            builder.append(pattern.get(relations, xsdString).get(0));
                            builder.append(", ");
                            builder.append(replacement.get(relations, xsdString).get(0));

                            if(flags != null)
                                builder.append(", ").append(flags.get(relations, xsdString).get(0));

                            builder.append(")");

                            result = List.of(new ExpressionColumn(builder.toString(), VARCHAR));
                        }
                        else if(rdfLangString.equals(e.getKey()) || rdfLtrLangString.equals(e.getKey())
                                || rdfRtlLangString.equals(e.getKey()))
                        {
                            List<Column> cols = argument.get(relations,
                                    unionize(resClasses, (PrimitiveResourceClass) e.getKey()));

                            StringBuilder builder = new StringBuilder();

                            builder.append("sparql.replace_string(");
                            builder.append(cols.get(0));
                            builder.append(", ");
                            builder.append(pattern.get(relations, xsdString).get(0));
                            builder.append(", ");
                            builder.append(replacement.get(relations, xsdString).get(0));

                            if(flags != null)
                                builder.append(", ").append(flags.get(relations, xsdString).get(0));

                            builder.append(")");

                            result = List.of(new ExpressionColumn(builder.toString(), VARCHAR));
                        }
                        else if(e.getKey() != null)
                        {
                            List<Column> cols = argument.get(relations, unionize(resClasses, box));

                            StringBuilder builder = new StringBuilder();

                            builder.append("sparql.replace_rdfbox(");
                            builder.append(cols.get(0));
                            builder.append(", ");
                            builder.append(pattern.get(relations, xsdString).get(0));
                            builder.append(", ");
                            builder.append(replacement.get(relations, xsdString).get(0));

                            if(flags != null)
                                builder.append(", ").append(flags.get(relations, xsdString).get(0));

                            builder.append(")");

                            result = List.of(new ExpressionColumn(builder.toString(), RDFBOX));
                        }
                    }

                    mappings.put(e.getKey(), result);
                }

                return new SqlBuiltinCall(function, distinct, arguments, mappings, canBeNull);
            }


            // functions on numerics:

            case "rand":
            {
                if(!restriction.contains(relations, xsdDouble))
                    return new SqlBuiltinCall(function, distinct, arguments, singletonMap(xsdDouble, null), false);

                List<Column> result = List.of(new ExpressionColumn("random()", FLOAT8, false));

                return new SqlBuiltinCall(function, distinct, arguments, singletonMap(xsdDouble, result), false);
            }

            case "abs":
            case "round":
            case "ceil":
            case "floor":
            {
                SqlExpressionIntercode argument = arguments.get(0);

                if(argument.getResourceClasses().stream().noneMatch(r -> hasNumeric(r)))
                    return SqlNull.get();

                if(!function.equals("abs") && argument.getResourceClasses().stream().allMatch(r -> isInteger(r)))
                    return argument;


                boolean canBeNull = argument.canBeNull()
                        || argument.getResourceClasses().stream().anyMatch(r -> !isNumeric(r));

                Map<ResourceClass, Set<List<ResourceClass>>> map = new HashMap<>();

                for(ResourceClass le : argument.getBinding().getMappings().keySet())
                    for(ResourceClass l : getNumericClasses(le))
                        map.computeIfAbsent(determineResultClass(l), _ -> new HashSet<>()).add(List.of(l));


                Map<ResourceClass, Set<List<Set<ResourceClass>>>> resMap = processResultMap(relations, arguments, map,
                        restriction, box);

                Map<ResourceClass, List<Column>> mappings = new HashMap<>();

                for(Entry<ResourceClass, Set<List<Set<ResourceClass>>>> e : resMap.entrySet())
                {
                    if(e.getValue() == null)
                    {
                        mappings.put(e.getKey(), null);
                    }
                    else
                    {
                        Set<Column> cols = new HashSet<>();

                        if(e.getKey().equals(xsdInteger))
                        {
                            for(List<Set<ResourceClass>> variant : e.getValue())
                            {
                                Column op = argument.promoteNumericAs(variant.get(0), xsdInteger);
                                cols.add(function.equals("abs") ?
                                        new ExpressionColumn(function + "(" + op + ")", op.getType()) : op);
                            }
                        }
                        else if(Stream.of(xsdDouble, xsdFloat, xsdDecimal).anyMatch(r -> r.equals(e.getKey())))
                        {
                            //NOTE: ignoring variants should be safe here
                            Column op = argument.get(relations, e.getKey()).get(0);
                            cols.add(new ExpressionColumn(function + "(" + op + ")", op.getType()));
                        }
                        else
                        {
                            for(List<Set<ResourceClass>> variant : e.getValue())
                            {
                                Column op = argument.get(relations, unionize(variant.get(0), box)).get(0);
                                cols.add(new ExpressionColumn("sparql." + function + "_rdfbox(" + op + ")",
                                        op.getType()));
                            }
                        }

                        mappings.put(e.getKey(), List.of(Column.coalesce(cols)));
                    }
                }

                return new SqlBuiltinCall(function, distinct, arguments, mappings, canBeNull);
            }


            // functions on dates and times:

            case "now":
            {
                DateTimeInZoneClass resultClass = DateTimeInZoneClass.get(0);

                if(!restriction.contains(relations, resultClass))
                    return new SqlBuiltinCall(function, distinct, arguments, singletonMap(resultClass, null), false);

                List<Column> result = List.of(new ExpressionColumn("now()", resultClass.getSqlTypes().get(0), false));

                return new SqlBuiltinCall(function, distinct, arguments, singletonMap(resultClass, result), false);
            }

            case "year":
            case "month":
            case "day":
            case "timezone":
            case "tz":
            case "hours":
            case "minutes":
            case "seconds":
            {
                SqlExpressionIntercode argument = arguments.get(0);

                boolean timeFunc = function.equals("hours") || function.equals("minutes") || function.equals("seconds");

                if(timeFunc && argument.getResourceClasses().stream().noneMatch(r -> hasDateTime(r)))
                    return SqlNull.get();

                if(!timeFunc && argument.getResourceClasses().stream().noneMatch(r -> hasDate(r) || hasDateTime(r)))
                    return SqlNull.get();

                if(function.equals("timezone") && argument.getResourceClasses().stream().noneMatch(r -> hasDate(r)
                        && !(r instanceof DateInZoneClass d && d.getZone() == Integer.MIN_VALUE)
                        || hasDateTime(r) && !(r instanceof DateTimeInZoneClass t && t.getZone() == Integer.MIN_VALUE)))
                    return SqlNull.get();

                boolean canBeNull = argument.canBeNull()
                        || timeFunc && argument.getResourceClasses().stream().anyMatch(r -> !isDateTime(r))
                        || !timeFunc && argument.getResourceClasses().stream().anyMatch(r -> !isDateOrDateTime(r));

                ResourceClass resultClass = switch(function)
                {
                    case "year" -> xsdInteger;
                    case "month" -> xsdInteger;
                    case "day" -> xsdInteger;
                    case "timezone" -> xsdDayTimeDuration;
                    case "tz" -> xsdString;
                    case "hours" -> xsdInteger;
                    case "minutes" -> xsdInteger;
                    case "seconds" -> xsdDecimal;
                    default -> throw new IllegalArgumentException();
                };

                if(!restriction.contains(relations, resultClass))
                    return new SqlBuiltinCall(function, distinct, arguments, singletonMap(resultClass, null),
                            canBeNull);

                boolean partCanBeBull = argument.canBeNull() || argument.getResourceClasses().size() > 1;

                Set<Column> variants = new HashSet<>();

                for(ResourceClass argumentClass : argument.getResourceClasses())
                {
                    StringBuilder builder = new StringBuilder();

                    if(!timeFunc && argumentClass instanceof DateInZone dateClass)
                    {
                        // as date

                        if(!function.equals("timezone") || dateClass.getZone() != Integer.MIN_VALUE)
                        {
                            builder.append("sparql.");
                            builder.append(function);
                            builder.append("_date(");
                            builder.append(argument.get(relations, argumentClass).get(0));
                            builder.append(", '");
                            builder.append(dateClass.getZone());
                            builder.append("'::int4)");
                        }
                    }
                    else if(!timeFunc && (argument.canSafelyGeneralize(relations, argumentClass, xsdDate)
                            || argument.canSafelyGeneralize(relations, argumentClass, genDate)))
                    {
                        // as date+int4

                        List<Column> columns = argumentClass.toGeneralClass(genDate,
                                argument.get(relations, argumentClass), partCanBeBull);

                        builder.append("sparql.");
                        builder.append(function);
                        builder.append("_date(");
                        builder.append(columns.get(0));
                        builder.append(", ");
                        builder.append(columns.get(1));
                        builder.append(")");
                    }
                    else if(!timeFunc && isDate(argumentClass) && isDate(argumentClass.getEffectiveClass()))
                    {
                        // as zoneddate

                        List<Column> columns = argumentClass.toGeneralClass(genScalarDate,
                                argument.get(relations, argumentClass), partCanBeBull);

                        builder.append("sparql.");
                        builder.append(function);
                        builder.append("_date");
                        builder.append("(");
                        builder.append(columns.get(0));
                        builder.append(")");
                    }
                    else if(argumentClass instanceof DateTimeInZone dateTimeClass)
                    {
                        // as timestamptz

                        if(!function.equals("timezone") || dateTimeClass.getZone() != Integer.MIN_VALUE)
                        {
                            builder.append("sparql.");
                            builder.append(function);
                            builder.append("_datetime(");
                            builder.append(argument.get(relations, argumentClass).get(0));
                            builder.append(", '");
                            builder.append(dateTimeClass.getZone());
                            builder.append("'::int4)");
                        }
                    }
                    else if(argument.canSafelyGeneralize(relations, argumentClass, xsdDateTime)
                            || argument.canSafelyGeneralize(relations, argumentClass, genDateTime))
                    {
                        // as timestamptz+int4

                        List<Column> columns = argumentClass.toGeneralClass(genDateTime,
                                argument.get(relations, argumentClass), partCanBeBull);

                        builder.append("sparql.");
                        builder.append(function);
                        builder.append("_datetime(");
                        builder.append(columns.get(0));
                        builder.append(", ");
                        builder.append(columns.get(1));
                        builder.append(")");
                    }
                    else if(isDateTime(argumentClass) && isDateTime(argumentClass.getEffectiveClass()))
                    {
                        // as zoneddatetime

                        List<Column> columns = argumentClass.toGeneralClass(genScalarDateTime,
                                argument.get(relations, argumentClass), partCanBeBull);

                        builder.append("sparql.");
                        builder.append(function);
                        builder.append("_datetime");
                        builder.append("(");
                        builder.append(columns.get(0));
                        builder.append(")");
                    }
                    else if(hasDateTime(argumentClass) || !timeFunc && hasDate(argumentClass))
                    {
                        // as rdfbox

                        List<Column> columns = argument.get(relations, unionize(Set.of(argumentClass), box));

                        builder.append("sparql.");
                        builder.append(function);
                        builder.append("_rdfbox");
                        builder.append("(");
                        builder.append(columns.get(0));
                        builder.append(")");
                    }


                    if(!builder.isEmpty())
                        variants.add(new ExpressionColumn(builder.toString(), resultClass.getSqlTypes().get(0)));
                }

                List<Column> result = List.of(coalesce(variants));

                return new SqlBuiltinCall(function, distinct, arguments, singletonMap(resultClass, result), canBeNull);
            }


            // hash functions:

            case "md5":
            case "sha1":
            case "sha256":
            case "sha384":
            case "sha512":
            {
                SqlExpressionIntercode argument = arguments.get(0);

                if(argument.getResourceClasses().stream().noneMatch(r -> hasString(r)))
                    return SqlNull.get();

                boolean canBeNull = argument.canBeNull()
                        || argument.getResourceClasses().stream().anyMatch(r -> !isString(r));

                if(!restriction.contains(relations, xsdString))
                    return new SqlBuiltinCall(function, distinct, arguments, singletonMap(xsdString, null), canBeNull);

                Column op = argument.get(relations, xsdString).get(0);
                List<Column> result = List.of(new ExpressionColumn(
                        "substring(pgcrypto.digest(" + op + ",'" + function + "')::varchar from 3)", VARCHAR,
                        canBeNull));

                return new SqlBuiltinCall(function, distinct, arguments, singletonMap(xsdString, result), canBeNull);
            }

            case "_strhash":
            {
                SqlExpressionIntercode argument = arguments.get(0);

                if(argument.getResourceClasses().stream().noneMatch(r -> hasStringLiteral(r)))
                    return SqlNull.get();

                boolean canBeNull = argument.canBeNull()
                        || argument.getResourceClasses().stream().anyMatch(r -> !isStringLiteral(r));


                if(!restriction.contains(relations, xsdLong))
                    return new SqlBuiltinCall(function, distinct, arguments, singletonMap(xsdLong, null), canBeNull);

                boolean partCanBeBull = argument.canBeNull() || argument.getResourceClasses().size() > 1;

                Set<Column> variants = new HashSet<>();

                for(ResourceClass argumentClass : argument.getResourceClasses())
                {
                    StringBuilder builder = new StringBuilder();

                    if(isFixedTagClass(argumentClass))
                    {
                        builder.append("hashtextextended(");
                        builder.append(argument.getMapping(argumentClass).get(0));
                        builder.append(",0)::int8");
                    }
                    else if(isString(argumentClass))
                    {
                        builder.append("hashtextextended(");
                        builder.append(argumentClass
                                .toGeneralClass(xsdString, argument.get(relations, argumentClass), partCanBeBull)
                                .get(0));
                        builder.append(",0)::int8");
                    }
                    else if(argument.canSafelyGeneralize(relations, argumentClass, rdfLangString))
                    {
                        builder.append("hashtextextended(");
                        builder.append(argumentClass
                                .toGeneralClass(rdfLangString, argument.get(relations, argumentClass), partCanBeBull)
                                .get(0));
                        builder.append(",0)::int8");
                    }
                    else if(argument.canSafelyGeneralize(relations, argumentClass, rdfLtrLangString))
                    {
                        builder.append("hashtextextended(");
                        builder.append(argumentClass
                                .toGeneralClass(rdfLtrLangString, argument.get(relations, argumentClass), partCanBeBull)
                                .get(0));
                        builder.append(",0)::int8");
                    }
                    else if(argument.canSafelyGeneralize(relations, argumentClass, rdfRtlLangString))
                    {
                        builder.append("hashtextextended(");
                        builder.append(argumentClass
                                .toGeneralClass(rdfRtlLangString, argument.get(relations, argumentClass), partCanBeBull)
                                .get(0));
                        builder.append(",0)::int8");
                    }
                    else if(hasStringLiteral(argumentClass))
                    {
                        builder.append("hashtextextended(sparql.rdfbox_get_string_literal(");
                        builder.append(argument.get(relations, unionize(Set.of(argumentClass), box)).get(0));
                        builder.append("), 0)::int8");
                    }

                    if(!builder.isEmpty())
                        variants.add(new ExpressionColumn(builder.toString(), INT8));
                }

                List<Column> result = List.of(coalesce(variants));

                return new SqlBuiltinCall(function, distinct, arguments, singletonMap(xsdLong, result), canBeNull);
            }

            default:
                //TODO: SPARQL 1.2
                throw new IllegalArgumentException("unsupported function: " + function); // unexpected
        }

    }


    /**
     * Result class of a string function preserving the string type of its argument (plain string, fixed-tag or general
     * language string, with or without a base direction).
     *
     * @param resClass the resource class
     * @return result class of a string function preserving the string type of its argument (plain string, fixed-tag or
     *         general language string, with or without a base direction)
     */
    private static ResourceClass getStringLiteralResultClass(ResourceClass resClass)
    {
        if(isString(resClass))
            return xsdString;

        if(isFixedTagClass(resClass.getEffectiveClass()))
            return resClass.getEffectiveClass();

        if(isLangString(resClass))
            return rdfLangString;

        if(isLtrLangString(resClass))
            return rdfLtrLangString;

        if(isRtlLangString(resClass))
            return rdfRtlLangString;

        return boxedStringLiteral;
    }


    /**
     * Result class of a string function over an argument of possibly several string types.
     *
     * @param left class of the left operand
     * @return result class of a string function over an argument of possibly several string types
     */
    private static ResourceClass getStringLiteralResultClass2(ResourceClass left)
    {
        Set<ResourceClass> resClasses = new HashSet<>();
        resClasses.add(xsdString);

        for(ResourceClass r : estimateAsUnion(left))
        {
            if(isFixedTagClass(r.getEffectiveClass()))
            {
                resClasses.add(r.getEffectiveClass());
            }
            else
            {
                if(hasLangString(r))
                    resClasses.add(rdfLangString);

                if(hasLtrLangString(r))
                    resClasses.add(rdfLtrLangString);

                if(hasRtlLangString(r))
                    resClasses.add(rdfRtlLangString);
            }
        }

        return unionize(resClasses);
    }


    /**
     * True if the class holds language-tagged strings (with or without a base direction) of one fixed tag.
     *
     * @param resClass the resource class
     * @return true if the class holds language-tagged strings (with or without a base direction) of one fixed tag,
     *         false otherwise
     */
    private static boolean isFixedTagClass(ResourceClass resClass)
    {
        return resClass instanceof LangStringWithTagClass || resClass instanceof DirLangStringWithTagClass;
    }


    /**
     * Fixed tag of a class holding language-tagged strings of one fixed tag, provided that they belong to the given
     * class of language-tagged strings; null otherwise.
     *
     * @param resClass the resource class
     * @param taggedClass the class of language-tagged strings ({@code rdfLangString}, {@code rdfLtrLangString} or
     *            {@code rdfRtlLangString})
     * @return fixed tag of a class holding language-tagged strings of one fixed tag, provided that they belong to the
     *         given class of language-tagged strings; null otherwise
     */
    private static String getFixedTag(ResourceClass resClass, PrimitiveResourceClass taggedClass)
    {
        if(resClass instanceof LangStringWithTagClass langClass && rdfLangString.equals(taggedClass))
            return langClass.getTag();

        if(resClass instanceof DirLangStringWithTagClass langClass && langClass.getDirectionClass().equals(taggedClass))
            return langClass.getTag();

        return null;
    }


    /**
     * The class of language-tagged strings ({@code rdfLangString}, {@code rdfLtrLangString} or
     * {@code rdfRtlLangString}) in whose columns every class of the set is stored; null when the classes are not all
     * stored in the same one.
     *
     * @param resClasses the resource classes
     * @return the class of language-tagged strings in whose columns every class of the set is stored; null when the
     *         classes are not all stored in the same one
     */
    private static PrimitiveResourceClass getLanguageTaggedClass(Set<ResourceClass> resClasses)
    {
        for(PrimitiveResourceClass taggedClass : List.<PrimitiveResourceClass> of(rdfLangString, rdfLtrLangString,
                rdfRtlLangString))
            if(resClasses.stream().allMatch(r -> r.getEffectiveClass().equals(taggedClass)))
                return taggedClass;

        return null;
    }


    @Override
    public Restrictions getRequirements(ClassRelations relations)
    {
        Restrictions restrictions = new Restrictions();

        for(SqlExpressionIntercode argument : arguments)
            restrictions.add(argument.getRequirements(relations));

        return restrictions;
    }


    /**
     * Lower-case function name.
     *
     * @return lower-case function name
     */
    public String getFunction()
    {
        return function;
    }


    /**
     * Arguments in order.
     *
     * @return arguments in order
     */
    public List<SqlExpressionIntercode> getArguments()
    {
        return arguments;
    }


    /**
     * The single argument; fails for other arities.
     *
     * @return the single argument; fails for other arities
     */
    public SqlExpressionIntercode getArgument()
    {
        if(arguments.size() != 1)
            throw new IllegalArgumentException();

        return arguments.get(0);
    }


    /**
     * True if the aggregate has the DISTINCT modifier.
     *
     * @return true if the aggregate has the DISTINCT modifier, false otherwise
     */
    public boolean isDistinct()
    {
        return distinct;
    }


    /**
     * Predicate telling whether a class is entirely of the kind tested by the {@code isIRI}-like function.
     *
     * @param function lower-case function name
     * @return predicate telling whether a class is entirely of the kind tested by the {@code isIRI}-like function
     */
    private static Function<ResourceClass, Boolean> getIsFunction(String function)
    {
        if(function.equals("isiri") || function.equals("isuri"))
            return BuiltinClasses::isIri;
        else if(function.equals("isblank"))
            return BuiltinClasses::isBlankNode;
        else if(function.equals("isliteral"))
            return BuiltinClasses::isLiteral;
        else if(function.equals("isnumeric"))
            return BuiltinClasses::isNumeric;
        else if(function.equals("istriple"))
            return BuiltinClasses::isTripleTerm;
        else if(function.equals("haslang"))
            return BuiltinClasses::isLanguageTaggedString;
        else if(function.equals("haslangdir"))
            return BuiltinClasses::isDirLanguageTaggedString;
        return null;
    }


    /**
     * Predicate telling whether a class may contain terms of the kind tested by the {@code isIRI}-like function.
     *
     * @param function lower-case function name
     * @return predicate telling whether a class may contain terms of the kind tested by the {@code isIRI}-like function
     */
    private static Function<ResourceClass, Boolean> getHasFunction(String function)
    {
        if(function.equals("isiri") || function.equals("isuri"))
            return BuiltinClasses::hasIri;
        else if(function.equals("isblank"))
            return BuiltinClasses::hasBlankNode;
        else if(function.equals("isliteral"))
            return BuiltinClasses::hasLiteral;
        else if(function.equals("isnumeric"))
            return BuiltinClasses::hasNumeric;
        else if(function.equals("istriple"))
            return BuiltinClasses::hasTripleTerm;
        else if(function.equals("haslang"))
            return BuiltinClasses::hasLanguageTaggedString;
        else if(function.equals("haslangdir"))
            return BuiltinClasses::hasDirLanguageTaggedString;
        return null;
    }


    /**
     * True if the function is an aggregate (including the internal {@code card}).
     *
     * @return true if the function is an aggregate (including the internal {@code card}), false otherwise
     */
    public boolean isAggregateFunction()
    {
        switch(function)
        {
            case "card":
            case "count":
            case "sum":
            case "min":
            case "max":
            case "avg":
            case "group_concat":
            case "sample":
                return true;
        }

        return false;
    }


    @Override
    public void generateExplanation(StringBuilder builder, String indent, int priority)
    {
        builder.append(function);
        builder.append("(");

        if(distinct)
            builder.append("distinct ");

        for(int i = 0; i < arguments.size(); i++)
        {
            if(i > 0)
                builder.append(", ");

            arguments.get(i).generateExplanation(builder, indent, 10);
        }

        builder.append(")");
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(!(object instanceof SqlBuiltinCall imcode))
            return false;

        if(!super.equals(imcode))
            return false;

        if(!Objects.equals(distinct, imcode.distinct) || !Objects.equals(function, imcode.function))
            return false;

        if(!Objects.equals(arguments, imcode.arguments))
            return false;

        return true;
    }


    @Override
    public Set<VirtualTable> getVirtualTables()
    {
        return getVirtualTables(arguments);
    }


    @Override
    protected int getHashCode()
    {
        return Objects.hash(distinct, function, arguments);
    }


    @Override
    public SqlExpressionIntercode optimize(Request request, VariableBindings bindings, Restriction restriction,
            boolean evalServices)
    {
        ClassRelations relations = request.getConfiguration();

        List<SqlExpressionIntercode> optimized = !restriction.contains(relations, unionize(getResourceClasses())) ?
                arguments.stream().map(a -> a.optimize(request, bindings, NONE, evalServices)).toList() :
                switch(function)
                {
                    case "card", "count", "sum", "avg", "min", "max", "sample", "group_concat" ->
                    {
                        yield arguments.stream().map(a -> a.optimize(request, bindings, ALL, evalServices)).toList();
                    }

                    case "bound", "isiri", "isuri", "isblank", "isliteral", "isnumeric", "istriple", "haslang", "haslangdir" ->
                    {
                        yield arguments.stream().map(a -> a.optimize(request, bindings, ALL, evalServices)).toList();
                    }

                    case "uuid", "struuid", "rand", "now" ->
                    {
                        yield List.of();
                    }

                    case "if" ->
                    {
                        //TODO: can be improved
                        yield List.of(
                                arguments.get(0).optimize(request, bindings, new Restriction(xsdBoolean), evalServices),
                                arguments.get(1).optimize(request, bindings, restriction, evalServices),
                                arguments.get(2).optimize(request, bindings, restriction, evalServices));
                    }

                    case "coalesce" ->
                    {
                        yield arguments.stream().map(a -> a.optimize(request, bindings, restriction, evalServices))
                                .toList();
                    }

                    case "sameterm" ->
                    {
                        //TODO: can be improved
                        yield arguments.stream().map(a -> a.optimize(request, bindings, ALL, evalServices)).toList();
                    }

                    case "str" ->
                    {
                        yield List.of(arguments.get(0).optimize(request, bindings, new Restriction(iri, literal),
                                evalServices));
                    }

                    case "lang", "langdir", "datatype" ->
                    {
                        yield List.of(
                                arguments.get(0).optimize(request, bindings, new Restriction(literal), evalServices));
                    }

                    case "iri", "uri" ->
                    {
                        yield List.of(
                                arguments.get(0).optimize(request, bindings, new Restriction(iri, xsdString),
                                        evalServices),
                                arguments.get(1).optimize(request, bindings, new Restriction(iri), evalServices));
                    }

                    case "bnode" ->
                    {
                        yield arguments.stream()
                                .map(a -> a.optimize(request, bindings, new Restriction(xsdString), evalServices))
                                .toList();
                    }

                    case "strdt" ->
                    {
                        yield List.of(
                                arguments.get(0).optimize(request, bindings, new Restriction(xsdString), evalServices),
                                arguments.get(1).optimize(request, bindings, new Restriction(iri), evalServices));
                    }

                    case "strlang", "strlangdir" ->
                    {
                        yield arguments.stream()
                                .map(a -> a.optimize(request, bindings, new Restriction(xsdString), evalServices))
                                .toList();
                    }

                    case "triple" ->
                    {
                        yield List.of(
                                arguments.get(0).optimize(request, bindings, new Restriction(reference), evalServices),
                                arguments.get(1).optimize(request, bindings, new Restriction(iri), evalServices),
                                arguments.get(2).optimize(request, bindings, ALL, evalServices));
                    }

                    case "subject", "predicate", "object" ->
                    {
                        yield List.of(arguments.get(0).optimize(request, bindings, new Restriction(tripleTerm),
                                evalServices));
                    }

                    case "strlen" ->
                    {
                        yield List.of(arguments.get(0).optimize(request, bindings,
                                new Restriction(xsdString, rdfLangString, rdfLtrLangString, rdfRtlLangString),
                                evalServices));
                    }

                    case "substr" ->
                    {
                        List<SqlExpressionIntercode> args = new ArrayList<>(arguments.size());

                        Set<ResourceClass> set = arguments.get(0).getResourceClasses().stream()
                                .filter(r -> hasStringLiteral(r)).map(r -> getStringLiteralResultClass(r))
                                .collect(toSet());

                        args.add(arguments.get(0).optimize(request, bindings, new Restriction(set), evalServices));
                        args.add(arguments.get(1).optimize(request, bindings, new Restriction(integerNumeric),
                                evalServices));

                        if(arguments.size() > 2)
                            args.add(arguments.get(2).optimize(request, bindings, new Restriction(integerNumeric),
                                    evalServices));

                        yield args;
                    }

                    case "ucase", "lcase" ->
                    {
                        Set<ResourceClass> set = arguments.get(0).getResourceClasses().stream()
                                .filter(r -> hasStringLiteral(r)).map(r -> getStringLiteralResultClass(r))
                                .collect(toSet());

                        yield List.of(arguments.get(0).optimize(request, bindings, new Restriction(set), evalServices));
                    }

                    case "strstarts", "strends", "contains" ->
                    {
                        //TODO can be improved
                        yield arguments.stream()
                                .map(a -> a.optimize(request, bindings,
                                        new Restriction(xsdString, rdfLangString, rdfLtrLangString, rdfRtlLangString),
                                        evalServices))
                                .toList();
                    }

                    case "strbefore", "strafter" ->
                    {
                        //TODO can be improved
                        yield arguments.stream()
                                .map(a -> a.optimize(request, bindings,
                                        new Restriction(xsdString, rdfLangString, rdfLtrLangString, rdfRtlLangString),
                                        evalServices))
                                .toList();
                    }

                    case "encode_for_uri" ->
                    {
                        yield List.of(arguments.get(0).optimize(request, bindings,
                                new Restriction(xsdString, rdfLangString, rdfLtrLangString, rdfRtlLangString),
                                evalServices));
                    }

                    case "concat" ->
                    {
                        //TODO can be improved
                        yield arguments.stream()
                                .map(a -> a.optimize(request, bindings,
                                        new Restriction(xsdString, rdfLangString, rdfLtrLangString, rdfRtlLangString),
                                        evalServices))
                                .toList();
                    }

                    case "langmatches" ->
                    {
                        yield arguments.stream()
                                .map(a -> a.optimize(request, bindings, new Restriction(xsdString), evalServices))
                                .toList();
                    }

                    case "regex" ->
                    {
                        List<SqlExpressionIntercode> args = new ArrayList<>(arguments.size());

                        args.add(arguments.get(0).optimize(request, bindings,
                                new Restriction(xsdString, rdfLangString, rdfLtrLangString, rdfRtlLangString),
                                evalServices));

                        for(int i = 1; i < arguments.size(); i++)
                            args.add(arguments.get(i).optimize(request, bindings, new Restriction(xsdString),
                                    evalServices));

                        yield args;
                    }

                    case "replace" ->
                    {
                        List<SqlExpressionIntercode> args = new ArrayList<>(arguments.size());

                        Set<ResourceClass> set = arguments.get(0).getResourceClasses().stream()
                                .filter(r -> hasStringLiteral(r)).map(r -> getStringLiteralResultClass(r))
                                .collect(toSet());

                        args.add(arguments.get(0).optimize(request, bindings, new Restriction(set), evalServices));

                        for(int i = 1; i < arguments.size(); i++)
                            args.add(arguments.get(i).optimize(request, bindings, new Restriction(xsdString),
                                    evalServices));

                        yield args;
                    }

                    case "abs", "round", "ceil", "floor" ->
                    {
                        Set<ResourceClass> set = new HashSet<>();

                        if(restriction.contains(relations, xsdDouble))
                            set.add(genDouble);

                        if(restriction.contains(relations, xsdFloat))
                            set.add(genFloat);

                        if(restriction.contains(relations, xsdDecimal))
                            set.add(genDecimal);

                        if(restriction.contains(relations, xsdInteger))
                            set.add(integerNumeric);

                        yield List.of(arguments.get(0).optimize(request, bindings, new Restriction(set), evalServices));
                    }

                    case "year", "month", "day", "tz" ->
                    {
                        yield List.of(arguments.get(0).optimize(request, bindings,
                                new Restriction(genScalarDateTime, genScalarDate), evalServices));
                    }

                    case "timezone" ->
                    {
                        Set<ResourceClass> set = arguments.get(0).getResourceClasses().stream()
                                .filter(r -> isDateOrDateTime(r)
                                        && (!(r instanceof DateTimeInZoneClass z) || z.getZone() != Integer.MIN_VALUE)
                                        && (!(r instanceof DateInZoneClass z) || z.getZone() != Integer.MIN_VALUE))
                                .collect(toSet());

                        yield List.of(arguments.get(0).optimize(request, bindings, new Restriction(set), evalServices));
                    }

                    case "hours", "minutes", "seconds" ->
                    {
                        yield List.of(arguments.get(0).optimize(request, bindings, new Restriction(genScalarDateTime),
                                evalServices));
                    }

                    case "md5", "sha1", "sha256", "sha384", "sha512" ->
                    {
                        yield List.of(
                                arguments.get(0).optimize(request, bindings, new Restriction(xsdString), evalServices));
                    }

                    case "_strhash" ->
                    {
                        yield List.of(arguments.get(0).optimize(request, bindings,
                                new Restriction(xsdString, rdfLangString, rdfLtrLangString, rdfRtlLangString),
                                evalServices));
                    }

                    default -> throw new IllegalArgumentException(); // unexpected
                };


        if(optimized.equals(arguments))
            return this;

        return create(request, function, distinct, optimized);
    }


    /**
     * True if some pair of argument classes has compatible string types (argument compatibility of string functions).
     *
     * @param left the left operand
     * @param right the right operand
     * @return true if some pair of argument classes has compatible string types (argument compatibility of string
     *         functions), false otherwise
     */
    private static boolean areStringLiteralCompatible(SqlExpressionIntercode left, SqlExpressionIntercode right)
    {
        for(ResourceClass l : left.getResourceClasses())
            for(ResourceClass r : right.getResourceClasses())
                if(areStringLiteralsCompatible(l, r))
                    return true;

        return false;
    }


    /**
     * True if some pair of primitive parts of the classes has compatible string types.
     *
     * @param left class of the left operand
     * @param right class of the right operand
     * @return true if some pair of primitive parts of the classes has compatible string types, false otherwise
     */
    private static boolean areStringLiteralsCompatible(ResourceClass left, ResourceClass right)
    {
        for(ResourceClass l : estimateAsUnion(left))
            for(ResourceClass r : estimateAsUnion(right))
                if(areStringLiteralsCompatibleBase(l, r))
                    return true;

        return false;
    }


    /**
     * True if the primitive classes are argument compatible: equal fixed tags (and base directions), a plain string
     * second argument, or both language-tagged strings of the same kind.
     *
     * @param left class of the left operand
     * @param right class of the right operand
     * @return true if the primitive classes are argument compatible, false otherwise
     */
    private static boolean areStringLiteralsCompatibleBase(ResourceClass left, ResourceClass right)
    {
        if(isFixedTagClass(left.getEffectiveClass()) && isFixedTagClass(right.getEffectiveClass()))
            return left.getEffectiveClass().equals(right.getEffectiveClass());

        if(hasString(right))
            return hasStringLiteral(left);

        if(hasLangString(left))
            return hasLangString(right);

        if(hasLtrLangString(left))
            return hasLtrLangString(right);

        if(hasRtlLangString(left))
            return hasRtlLangString(right);

        return false;
    }





    /**
     * True if some pair of argument classes has incompatible string types.
     *
     * @param left the left operand
     * @param right the right operand
     * @return true if some pair of argument classes has incompatible string types, false otherwise
     */
    private static boolean areStringLiteralIncompatible(SqlExpressionIntercode left, SqlExpressionIntercode right)
    {
        for(ResourceClass l : left.getResourceClasses())
            for(ResourceClass r : right.getResourceClasses())
                if(areStringLiteralsIncompatible(l, r))
                    return true;

        return false;
    }


    /**
     * True if some pair of primitive parts of the classes has incompatible string types.
     *
     * @param left class of the left operand
     * @param right class of the right operand
     * @return true if some pair of primitive parts of the classes has incompatible string types, false otherwise
     */
    private static boolean areStringLiteralsIncompatible(ResourceClass left, ResourceClass right)
    {
        for(ResourceClass l : estimateAsUnion(left))
            for(ResourceClass r : estimateAsUnion(right))
                if(areStringLiteralsIncompatibleBase(l, r))
                    return true;

        return false;
    }


    /**
     * True unless the primitive classes are compatible by construction: a language-tagged string (with or without a
     * base direction) with a plain string, or equal fixed tags (and base directions).
     *
     * @param left class of the left operand
     * @param right class of the right operand
     * @return true unless the primitive classes are compatible by construction, false otherwise
     */
    private static boolean areStringLiteralsIncompatibleBase(ResourceClass left, ResourceClass right)
    {
        ResourceClass l = left.getEffectiveClass();
        ResourceClass r = right.getEffectiveClass();

        return !(isLanguageTaggedString(l) && isString(r) || isFixedTagClass(l) && l.equals(r));
    }



}
