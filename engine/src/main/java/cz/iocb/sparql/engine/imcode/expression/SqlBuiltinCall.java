package cz.iocb.sparql.engine.imcode.expression;

import static cz.iocb.sparql.engine.database.Column.coalesce;
import static cz.iocb.sparql.engine.imcode.expression.SqlExpressionIntercode.Restriction.ALL;
import static cz.iocb.sparql.engine.imcode.expression.SqlExpressionIntercode.Restriction.NONE;
import static cz.iocb.sparql.engine.imcode.expression.SqlLiteral.falseValue;
import static cz.iocb.sparql.engine.imcode.expression.SqlLiteral.trueValue;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.bnodeIntBlankNode;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.bnodeStrBlankNode;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
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
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasIri;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasLangString;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasLiteral;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasNumeric;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasString;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasStringLiteral;
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
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isLiteral;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isNumeric;
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
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.database.ExpressionColumn;
import cz.iocb.sparql.engine.imcode.SqlIntercode.Restrictions;
import cz.iocb.sparql.engine.mapping.classes.BuiltinClasses;
import cz.iocb.sparql.engine.mapping.classes.DateInZone;
import cz.iocb.sparql.engine.mapping.classes.DateInZoneBaseClass;
import cz.iocb.sparql.engine.mapping.classes.DateInZoneClass;
import cz.iocb.sparql.engine.mapping.classes.DateTimeInZone;
import cz.iocb.sparql.engine.mapping.classes.DateTimeInZoneBaseClass;
import cz.iocb.sparql.engine.mapping.classes.DateTimeInZoneClass;
import cz.iocb.sparql.engine.mapping.classes.LangStringWithTagClass;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;
import cz.iocb.sparql.engine.mapping.classes.PrimitiveResourceClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.mapping.classes.UserLiteralBaseClass;
import cz.iocb.sparql.engine.mapping.classes.UserLiteralClass;
import cz.iocb.sparql.engine.mapping.classes.UserLiteralCompositeBaseClass;
import cz.iocb.sparql.engine.mapping.classes.UserLiteralCompositeClass;
import cz.iocb.sparql.engine.mapping.datatypes.Datatype;
import cz.iocb.sparql.engine.rdf.TypedLiteral;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.VariableBindings;



public final class SqlBuiltinCall extends SqlExpressionIntercode
{
    private final String function;
    private final boolean distinct;
    private final List<SqlExpressionIntercode> arguments;


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


    protected SqlBuiltinCall(String function, List<SqlExpressionIntercode> arguments,
            Map<ResourceClass, List<Column>> mappings, boolean canBeNull)
    {
        this(function, false, arguments, mappings, canBeNull);
    }


    public static SqlExpressionIntercode create(Request request, String function, boolean distinct,
            List<SqlExpressionIntercode> arguments)
    {
        return create(request, function, distinct, arguments, Restriction.ALL);
    }


    public static SqlExpressionIntercode create(Request request, String function, boolean distinct,
            List<SqlExpressionIntercode> arguments, Restriction restriction)
    {
        switch(function)
        {
            // aggregate functions:

            case "card":
            {
                if(!restriction.contains(xsdInteger))
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

                List<Column> result = List.of(new ExpressionColumn(builder.toString(), false));

                return new SqlBuiltinCall(function, distinct, arguments, Map.of(xsdInteger, result), false);
            }

            case "count":
            {
                if(!restriction.contains(xsdInteger))
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
                        && columns.stream().anyMatch(c -> c instanceof ExpressionColumn))
                {
                    ResourceClass resClass = getExpressionClass(argument.getResourceClasses());
                    builder.append(argument.get(resClass).get(0));
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

                List<Column> result = List.of(new ExpressionColumn(builder.toString(), false));

                return new SqlBuiltinCall(function, distinct, arguments, Map.of(xsdInteger, result), false);
            }

            case "sum":
            case "avg":
            {
                SqlExpressionIntercode argument = arguments.get(0);

                boolean canBeNull = argument.getResourceClasses().stream().anyMatch(r -> !isNumeric(r));

                if(argument.equals(SqlNull.get()))
                {
                    if(restriction.contains(xsdInteger))
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


                if(!restriction.contains(resultClass))
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
                    Column op = argument.get(box).get(0);

                    builder.append("sparql.");
                    builder.append(function);
                    builder.append("_rdfbox(");

                    if(distinct)
                        builder.append("DISTINCT ");

                    builder.append(op);
                    builder.append(")");

                    builder.toString();
                }

                List<Column> result = List.of(new ExpressionColumn(builder.toString(), canBeNull));

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

                builder.append(argument.get(resultClass).get(0));
                builder.append(")");


                List<Column> result = List.of(new ExpressionColumn(builder.toString()));

                return new SqlBuiltinCall(function, distinct, arguments, Map.of(resultClass, result), true);
            }

            case "group_concat":
            {
                SqlExpressionIntercode argument = arguments.get(0);

                boolean canBeNull = argument.getResourceClasses().stream().anyMatch(r -> !isStringLiteral(r));

                if(!restriction.contains(xsdString))
                    return new SqlBuiltinCall(function, distinct, arguments, singletonMap(xsdString, null), canBeNull);

                if(argument.equals(SqlNull.get()))
                    return SqlLiteral.create(request, new TypedLiteral("", xsdStringIri));

                boolean simple = argument.getResourceClasses().stream().allMatch(r -> isStringLiteral(r))
                        && !argument.getResourceClasses().stream().allMatch(r -> r.getEffectiveClass().equals(box));

                Column op = simple ? argument.getStringLiteral() : argument.get(box).get(0);

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
                    builder.append(arguments.get(1).get(xsdString).get(0));

                builder.append(")");


                List<Column> result = List.of(new ExpressionColumn(builder.toString()));

                return new SqlBuiltinCall(function, distinct, arguments, Map.of(xsdString, result), canBeNull);
            }


            // functional forms:

            case "bound":
            {
                SqlExpressionIntercode argument = arguments.get(0);

                if(!restriction.contains(xsdBoolean))
                    return new SqlBuiltinCall(function, distinct, arguments, singletonMap(xsdBoolean, null), false);

                if(argument.equals(SqlNull.get()))
                    return falseValue;

                if(!argument.canBeNull())
                    return trueValue;

                List<Column> result = List.of(new ExpressionColumn(argument.getIsNotNull(), false));

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
                        .collect(partitioningBy(c -> restriction.contains(c), toSet()));

                boolean canBeNull = condition.canBeNull() || left.canBeNull() || right.canBeNull();

                Map<ResourceClass, List<Column>> mappings = new HashMap<>();

                if(!classes.get(false).isEmpty())
                    mappings.put(unionize(classes.get(false)), null);

                if(!classes.get(true).isEmpty())
                {
                    ResourceClass unionClass = getExpressionClass(classes.get(true));

                    StringBuilder builder = new StringBuilder();
                    builder.append("CASE ");
                    builder.append(condition.get(xsdBoolean).get(0));
                    builder.append(" WHEN true THEN ").append(left.get(unionClass).get(0));
                    builder.append(" WHEN false THEN ").append(right.get(unionClass).get(0));
                    builder.append(" END");

                    Column col = new ExpressionColumn(builder.toString(), canBeNull || !classes.get(false).isEmpty());
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
                        .collect(partitioningBy(c -> restriction.contains(c), toSet()));

                Map<ResourceClass, List<Column>> mappings = new HashMap<>();

                if(!classes.get(false).isEmpty())
                    mappings.put(unionize(classes.get(false)), null);

                if(!classes.get(true).isEmpty())
                {
                    ResourceClass unionClass = getExpressionClass(classes.get(true));

                    List<Column> cols = realArguments.stream().map(a -> a.get(unionClass).get(0)).collect(toList());

                    Column col = new ExpressionColumn(
                            cols.stream().map(Object::toString).collect(joining(",", "COALESCE(", ")")));

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

                if(!restriction.contains(xsdBoolean))
                    return new SqlBuiltinCall(function, distinct, arguments, singletonMap(xsdBoolean, null), canBeNull);

                if(left.equals(right) && !canBeNull)
                    return trueValue;

                Set<List<ResourceClass>> set = new HashSet<>();
                Map<ResourceClass, Set<List<ResourceClass>>> map = Map.of(xsdBoolean, set);

                for(ResourceClass leftClass : left.getMappings().keySet())
                    for(ResourceClass rightClass : right.getMappings().keySet())
                        if(!areDisjunct(leftClass, rightClass))
                            set.add(List.of(leftClass, rightClass));

                if(set.isEmpty() && !canBeNull)
                    return falseValue;

                List<SqlExpressionIntercode> operands = List.of(left, right);
                Map<ResourceClass, Set<List<Set<ResourceClass>>>> resMap = processResultMap(operands, map, restriction);

                Set<String> variants = new HashSet<>();

                for(List<Set<ResourceClass>> v : resMap.get(xsdBoolean))
                {
                    Set<ResourceClass> classes = new HashSet<>();
                    classes.addAll(v.get(0));
                    classes.addAll(v.get(1));

                    ResourceClass unionClass = unionize(classes);

                    //TODO: optimize for special combinations

                    //NOTE: to ensure that the expression column is not used more than once during conversion
                    if(left.hasExpressionColumn(v.get(0)) || right.hasExpressionColumn(v.get(1)))
                        unionClass = getExpressionClass(classes);

                    List<Column> lcols = left.get(unionClass);
                    List<Column> rcols = right.get(unionClass);

                    //FIXME: use correct compare operator, when unionClass is rdfbox

                    variants.add(IntStream.range(0, unionClass.getColumnCount())
                            .mapToObj(i -> lcols.get(i) + " = " + rcols.get(i)).collect(joining(" AND ", "(", ")")));
                }

                List<Column> result = List
                        .of(new ExpressionColumn(variants.stream().collect(joining(" OR ", "(", ")")), canBeNull));

                return new SqlBuiltinCall(function, distinct, arguments, singletonMap(xsdBoolean, result), canBeNull);
            }


            // functions on RDF terms:

            case "isiri":
            case "isuri":
            case "isblank":
            case "isliteral":
            case "isnumeric":
            {
                SqlExpressionIntercode argument = arguments.get(0);

                if(argument instanceof SqlNull)
                    return SqlNull.get();

                boolean canBeNull = argument.canBeNull();

                if(!restriction.contains(xsdBoolean))
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
                        variants.add(new ExpressionColumn("NULLIF(" + argument.getIsNotNull(r) + ", false)"));
                    else if(!has.apply(r))
                        variants.add(new ExpressionColumn("NULLIF(" + argument.getIsNull(r) + ", true)"));
                    else
                        variants.add(
                                new ExpressionColumn("sparql.is_" + function.substring(2).replaceFirst("uri", "iri")
                                        + "_rdfbox(" + argument.get(unionize(Set.of(r), box)).get(0) + ")"));
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


                if(!restriction.contains(xsdString))
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
                    else if(argument.canSafelyGeneralize(argumentClass, xsdDateTime))
                    {
                        List<Column> columns = argumentClass.toGeneralClass(xsdDateTime, argument.get(argumentClass),
                                true);

                        builder.append("sparql.str_datetime(");
                        builder.append(columns.get(0));
                        builder.append(", ");
                        builder.append(columns.get(1));
                        builder.append(")");
                    }
                    else if(argument.canSafelyGeneralize(argumentClass, genDateTime))
                    {
                        List<Column> columns = argumentClass.toGeneralClass(xsdDateTime, argument.get(argumentClass),
                                true);

                        builder.append("COALESCE(NULLIF(");
                        builder.append(columns.get(2));
                        builder.append(", ''::varchar), sparql.str_datetime");
                        builder.append("(");
                        builder.append(columns.get(0));
                        builder.append(", ");
                        builder.append(columns.get(1));
                        builder.append("))");
                    }
                    else if(argument.canSafelyGeneralize(argumentClass, xsdDate))
                    {
                        List<Column> columns = argumentClass.toGeneralClass(xsdDate, argument.get(argumentClass), true);

                        builder.append("sparql.str_date(");
                        builder.append(columns.get(0));
                        builder.append(", ");
                        builder.append(columns.get(1));
                        builder.append(")");
                    }
                    else if(argument.canSafelyGeneralize(argumentClass, genDate))
                    {
                        List<Column> columns = argumentClass.toGeneralClass(xsdDate, argument.get(argumentClass), true);

                        builder.append("COALESCE(NULLIF(");
                        builder.append(columns.get(2));
                        builder.append(", ''::varchar), sparql.str_date");
                        builder.append("(");
                        builder.append(columns.get(0));
                        builder.append(", ");
                        builder.append(columns.get(1));
                        builder.append("))");
                    }
                    else if(argument.canSafelyGeneralize(argumentClass, rdfLangString))
                    {
                        List<Column> columns = argumentClass.toGeneralClass(rdfLangString, argument.get(argumentClass),
                                true);

                        builder.append(columns.get(0));
                    }
                    else if(argumentClass instanceof UserLiteralClass)
                    {
                        builder.append("(" + argument.getMapping(argumentClass).get(0) + ")::varchar");
                    }
                    else if(argumentClass instanceof UserLiteralBaseClass)
                    {
                        List<Column> columns = argumentClass.toGeneralClass(argumentClass, argument.get(argumentClass),
                                true);

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
                        List<Column> columns = argumentClass.toGeneralClass(argumentClass, argument.get(argumentClass),
                                true);

                        builder.append("COALESCE(NULLIF(");
                        builder.append(columns.get(2));
                        builder.append(", ''::varchar), ");
                        builder.append(columns.get(0));
                        builder.append(")");
                    }
                    else if(isString(argumentClass))
                    {
                        builder.append(
                                argumentClass.toGeneralClass(xsdString, argument.get(argumentClass), true).get(0));
                    }
                    else if(isIri(argumentClass))
                    {
                        builder.append(argumentClass.toGeneralClass(iri, argument.get(argumentClass), true).get(0));
                    }
                    else if(argument.canSafelyGeneralize(argumentClass, unsupportedType))
                    {
                        List<Column> columns = argumentClass.toGeneralClass(unsupportedType,
                                argument.get(argumentClass), true);

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
                                    argument.get(argumentClass), true);

                            builder.append("sparql.str_").append(getLiteralClassName(canonicalClass));
                            builder.append("(");
                            builder.append(columns.get(0));
                            builder.append(")");
                        }
                        else if(nonCanonicalClass != null)
                        {
                            List<Column> columns = argumentClass.toGeneralClass(nonCanonicalClass,
                                    argument.get(argumentClass), true);
                            builder.append(columns.get(1));
                        }
                        else if(baseClass != null)
                        {
                            List<Column> columns = argumentClass.toGeneralClass(baseClass, argument.get(argumentClass),
                                    true);

                            builder.append("COALESCE(NULLIF(");
                            builder.append(columns.get(1));
                            builder.append(", ''::varchar), sparql.str_").append(getLiteralClassName(baseClass));
                            builder.append("(");
                            builder.append(columns.get(0));
                            builder.append("))");
                        }
                        else
                        {
                            List<Column> columns = argumentClass.toGeneralClass(box, argument.get(argumentClass), true);

                            builder.append("sparql.str_rdfbox");
                            builder.append("(");
                            builder.append(columns.get(0));
                            builder.append(")");
                        }
                    }

                    if(!builder.isEmpty())
                        variants.add(new ExpressionColumn(builder.toString()));
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


                if(!restriction.contains(xsdString))
                    return new SqlBuiltinCall(function, distinct, arguments, singletonMap(xsdString, null), canBeNull);

                boolean partCanBeBull = argument.canBeNull() || argument.getResourceClasses().size() > 1;

                Set<Column> variants = new HashSet<>();

                for(ResourceClass argumentClass : argument.getResourceClasses())
                {
                    StringBuilder builder = new StringBuilder();

                    if(argumentClass instanceof LangStringWithTagClass langClass)
                    {
                        if(partCanBeBull)
                            builder.append("CASE WHEN " + argument.getIsNotNull(argumentClass) + " THEN '"
                                    + langClass.getTag() + "' END");
                        else
                            builder.append("'" + langClass.getTag() + "'");
                    }
                    else if(argument.canSafelyGeneralize(argumentClass, rdfLangString))
                    {
                        builder.append(argumentClass
                                .toGeneralClass(rdfLangString, argument.get(argumentClass), partCanBeBull).get(1));
                    }
                    else if(isLiteral(argumentClass) && !hasLangString(argumentClass))
                    {
                        if(partCanBeBull)
                            builder.append("CASE WHEN " + argument.getIsNotNull(argumentClass) + " THEN '' END");
                        else
                            builder.append("''");
                    }
                    else if(hasLiteral(argumentClass))
                    {
                        Column col = argument.get(unionize(Set.of(argumentClass), box)).get(0);
                        builder.append("sparql.lang_rdfbox(" + col + ")");
                    }

                    if(!builder.isEmpty())
                        variants.add(new ExpressionColumn(builder.toString()));
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


                if(!restriction.contains(iri))
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
                            builder.append("CASE WHEN " + argument.getIsNotNull(argumentClass) + " THEN '" + iriValue
                                    + "' END");
                        else
                            builder.append("'" + iriValue + "'");
                    }
                    else if(argument.canSafelyGeneralize(argumentClass, unsupportedType))
                    {
                        builder.append(argumentClass
                                .toGeneralClass(unsupportedType, argument.get(argumentClass), partCanBeBull).get(1));
                    }
                    else if(hasLiteral(argumentClass))
                    {
                        Column col = argument.get(unionize(Set.of(argumentClass), box)).get(0);
                        builder.append("sparql.datatype_rdfbox(" + col + ")");
                    }

                    if(!builder.isEmpty())
                        variants.add(new ExpressionColumn(builder.toString()));
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

                if(!restriction.contains(iri))
                    return new SqlBuiltinCall(function, distinct, arguments, singletonMap(iri, null), canBeNull);

                boolean partCanBeBull = argument.canBeNull() || argument.getResourceClasses().size() > 1;
                Column base = arguments.get(1).get(iri).get(0);

                Set<Column> variants = new HashSet<>();

                for(ResourceClass argClass : argument.getResourceClasses())
                {
                    StringBuilder builder = new StringBuilder();

                    if(isIri(argClass))
                    {
                        builder.append(argClass.toGeneralClass(iri, argument.get(argClass), partCanBeBull).get(0));
                    }
                    else if(isString(argClass))
                    {
                        Column col = argClass.toGeneralClass(xsdString, argument.get(argClass), partCanBeBull).get(0);

                        builder.append("sparql.iri_string(" + base + ", " + col + ")");
                    }
                    else if(hasIri(argClass) && !hasString(argClass))
                    {
                        ResourceClass boxClass = unionize(Set.of(argClass), box);
                        Column col = argClass.toGeneralClass(boxClass, argument.get(argClass), partCanBeBull).get(0);

                        builder.append("sparql.rdfbox_get_iri(" + col + ")");
                    }
                    else if(hasIri(argClass) || hasString(argClass))
                    {
                        ResourceClass boxClass = unionize(Set.of(argClass), box);
                        Column col = argClass.toGeneralClass(boxClass, argument.get(argClass), partCanBeBull).get(0);

                        builder.append("sparql.iri_rdfbox(" + base + ", " + col + ")");
                    }

                    if(!builder.isEmpty())
                        variants.add(new ExpressionColumn(builder.toString()));
                }

                List<Column> result = List.of(coalesce(variants));

                return new SqlBuiltinCall(function, distinct, arguments, singletonMap(iri, result), canBeNull);
            }

            case "bnode":
            {
                if(arguments.isEmpty())
                {
                    if(!restriction.contains(bnodeIntBlankNode))
                        return new SqlBuiltinCall(function, distinct, arguments, singletonMap(bnodeIntBlankNode, null),
                                false);

                    List<Column> result = List.of(new ExpressionColumn("sparql.bnode()", false));

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

                    if(!restriction.contains(bnodeStrBlankNode))
                        return new SqlBuiltinCall(function, distinct, arguments, singletonMap(bnodeStrBlankNode, null),
                                canBeNull);

                    List<Column> result = argument.get(xsdString);

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

                    if(xsdString.equals(resourceClass) && argumentIsString && restriction.contains(xsdString))
                    {
                        return argument;
                    }
                    else if(xsdString.equals(resourceClass))
                    {
                        List<Column> result = !restriction.contains(xsdString) ? null : argument.get(xsdString);
                        return new SqlBuiltinCall(function, arguments, singletonMap(xsdString, result), canBeNull);
                    }
                    else if(resourceClass == null && argumentIsString && !argument.canBeNull())
                    {
                        List<Column> result = !restriction.contains(unsupportedType) ? null : List.of(
                                argument.get(xsdString).get(0), new ConstantColumn(iri.getIri().getValue(), "varchar"));

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

                if(!restriction.contains(resultClass))
                    return new SqlBuiltinCall(function, distinct, arguments, singletonMap(resultClass, null),
                            canBeNull);

                List<Column> result = List.of(new ExpressionColumn("sparql.rdfbox_create_from_typedliteral("
                        + argument.get(xsdString).get(0) + ", " + type.get(iri).get(0) + ")", canBeNull));

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

                    if(!tag.matches("""
                            ([A-Za-z]{2,3}(-[A-Za-z]{3}){0,3}|[A-Za-z]{4,8})\
                            (-[A-Za-z]{4})?(-([A-Za-z]{2}|[0-9]{3}))?(-([A-Za-z0-9]{5,8}|[0-9][A-Za-z0-9]{3}))*\
                            (-[0-9A-WY-Za-wy-z](-[A-Za-z0-9]{2,8})+)*(-x(-[A-Za-z0-9]{1,8})+)?|x(-[A-Za-z0-9]{1,8})+\
                            |i-ami|i-bnn|i-default|i-enochian|i-hak|i-klingon|i-lux|i-mingo|i-navajo|i-pwn\
                            |i-tao|i-tay|i-tsu|sgn-BE-FR|sgn-BE-NL|sgn-CH-DE"""))
                        return SqlNull.get();

                    // TODO: add a variant for case the argument is a constant

                    boolean canBeNull = argument.canBeNull()
                            || argument.getResourceClasses().stream().anyMatch(r -> !isString(r));
                    LangStringWithTagClass resultClass = LangStringWithTagClass.get(tag);

                    if(!restriction.contains(resultClass))
                        return new SqlBuiltinCall(function, arguments, singletonMap(resultClass, null), canBeNull);

                    List<Column> result = argument.get(xsdString);

                    return new SqlBuiltinCall(function, arguments, singletonMap(resultClass, result), canBeNull);
                }

                if(!restriction.contains(rdfLangString))
                    return new SqlBuiltinCall(function, arguments, singletonMap(rdfLangString, null), true);

                ResourceClass resultClass = unionize(Set.of(rdfLangString), box);

                List<Column> result = List.of(new ExpressionColumn("sparql.rdfbox_create_from_langstring("
                        + argument.get(xsdString).get(0) + ", " + lang.get(xsdString).get(0) + ")"));

                return new SqlBuiltinCall(function, arguments, singletonMap(resultClass, result), true);
            }

            case "uuid":
            {
                if(!restriction.contains(iri))
                    return new SqlBuiltinCall(function, distinct, arguments, singletonMap(iri, null), false);

                List<Column> result = List
                        .of(new ExpressionColumn("('urn:uuid:' || uuid.uuid_generate_v4())::varchar", false));

                return new SqlBuiltinCall(function, distinct, arguments, singletonMap(iri, result), false);
            }

            case "struuid":
            {
                if(!restriction.contains(xsdString))
                    return new SqlBuiltinCall(function, distinct, arguments, singletonMap(xsdString, null), false);

                List<Column> result = List.of(new ExpressionColumn("uuid.uuid_generate_v4()::varchar", false));

                return new SqlBuiltinCall(function, distinct, arguments, singletonMap(xsdString, result), false);
            }


            // functions on strings:

            case "strlen":
            {
                SqlExpressionIntercode argument = arguments.get(0);

                if(argument.getResourceClasses().stream().noneMatch(r -> hasStringLiteral(r)))
                    return SqlNull.get();

                boolean canBeNull = argument.canBeNull()
                        || argument.getResourceClasses().stream().anyMatch(r -> !isStringLiteral(r));


                if(!restriction.contains(xsdInteger))
                    return new SqlBuiltinCall(function, distinct, arguments, singletonMap(xsdInteger, null), canBeNull);

                boolean partCanBeBull = argument.canBeNull() || argument.getResourceClasses().size() > 1;

                Set<Column> variants = new HashSet<>();

                for(ResourceClass argumentClass : argument.getResourceClasses())
                {
                    StringBuilder builder = new StringBuilder();

                    if(argumentClass instanceof LangStringWithTagClass)
                    {
                        builder.append("length(");
                        builder.append(argument.getMapping(argumentClass).get(0));
                        builder.append(")::decimal");
                    }
                    else if(isString(argumentClass))
                    {
                        builder.append("length(");
                        builder.append(argumentClass
                                .toGeneralClass(xsdString, argument.get(argumentClass), partCanBeBull).get(0));
                        builder.append(")::decimal");
                    }
                    else if(argument.canSafelyGeneralize(argumentClass, rdfLangString))
                    {
                        builder.append("length(");
                        builder.append(argumentClass
                                .toGeneralClass(rdfLangString, argument.get(argumentClass), partCanBeBull).get(1));
                        builder.append(")::decimal");
                    }
                    else if(hasStringLiteral(argumentClass))
                    {
                        builder.append("sparql.strlen_rdfbox(");
                        builder.append(argument.get(unionize(Set.of(argumentClass), box)).get(0));
                        builder.append(")");
                    }

                    if(!builder.isEmpty())
                        variants.add(new ExpressionColumn(builder.toString()));
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

                    if(resultClass.equals(rdfLangString) && additionalCanBeNull)
                        resultClass = unionize(Set.of(rdfLangString), box);

                    List<ResourceClass> list = new ArrayList<>();

                    list.add(resClass);
                    list.add(integerNumeric);

                    if(length != null)
                        list.add(integerNumeric);

                    map.computeIfAbsent(resultClass, _ -> new HashSet<>()).add(list);
                }

                Map<ResourceClass, Set<List<Set<ResourceClass>>>> resMap = processResultMap(arguments, map, restriction,
                        box);

                Map<ResourceClass, List<Column>> mappings = new HashMap<>();

                for(Entry<ResourceClass, Set<List<Set<ResourceClass>>>> e : resMap.entrySet())
                {
                    List<Column> result = null;

                    if(e.getValue() != null)
                    {
                        Set<ResourceClass> resClasses = e.getValue().stream().flatMap(a -> a.get(0).stream())
                                .collect(toSet());

                        if(xsdString.equals(e.getKey()) || e.getKey() instanceof LangStringWithTagClass)
                        {
                            Column col = argument.getStringLiteral(resClasses);

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

                            result = List.of(new ExpressionColumn(builder.toString()));
                        }
                        else if(rdfLangString.equals(e.getKey()))
                        {
                            List<Column> cols = argument.get(unionize(resClasses, (PrimitiveResourceClass) e.getKey()));

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

                            result = List.of(new ExpressionColumn(builder.toString()), cols.get(1));
                        }
                        else if(e.getKey() != null)
                        {
                            List<Column> cols = argument.get(unionize(resClasses, box));

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

                            result = List.of(new ExpressionColumn(builder.toString()));
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

                Map<ResourceClass, Set<List<Set<ResourceClass>>>> resMap = processResultMap(arguments, map, restriction,
                        box);

                Map<ResourceClass, List<Column>> mappings = new HashMap<>();

                for(Entry<ResourceClass, Set<List<Set<ResourceClass>>>> e : resMap.entrySet())
                {
                    List<Column> result = null;

                    if(e.getValue() != null)
                    {
                        Set<ResourceClass> resClasses = e.getValue().stream().flatMap(a -> a.get(0).stream())
                                .collect(toSet());

                        if(xsdString.equals(e.getKey()) || e.getKey() instanceof LangStringWithTagClass)
                        {
                            List<Column> cols = argument.get(unionize(resClasses, (PrimitiveResourceClass) e.getKey()));

                            StringBuilder builder = new StringBuilder();

                            builder.append(function.equals("lcase") ? "lower(" : "upper(");
                            builder.append(cols.get(0));
                            builder.append(")::varchar");

                            result = List.of(new ExpressionColumn(builder.toString()));
                        }
                        else if(rdfLangString.equals(e.getKey()))
                        {
                            List<Column> cols = argument.get(unionize(resClasses, (PrimitiveResourceClass) e.getKey()));

                            StringBuilder builder = new StringBuilder();

                            builder.append(function.equals("lcase") ? "lower(" : "upper(");
                            builder.append(cols.get(0));
                            builder.append(")::varchar");

                            result = List.of(new ExpressionColumn(builder.toString()), cols.get(1));
                        }
                        else if(e.getKey() != null)
                        {
                            List<Column> cols = argument.get(unionize(resClasses, box));

                            StringBuilder builder = new StringBuilder();

                            builder.append("sparql.");
                            builder.append(function);
                            builder.append("_rdfbox(");
                            builder.append(cols.get(0));
                            builder.append(")");

                            result = List.of(new ExpressionColumn(builder.toString()));
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

                Set<List<Set<ResourceClass>>> variants = processResultMap(arguments, map, restriction).get(xsdBoolean);

                if(variants == null)
                    return new SqlBuiltinCall(function, distinct, arguments, singletonMap(xsdBoolean, null), canBeNull);

                Set<Column> cols = new HashSet<>();

                for(List<Set<ResourceClass>> variant : variants)
                {
                    StringBuilder builder = new StringBuilder();

                    Set<ResourceClass> leftClasses = variant.get(0);
                    Set<ResourceClass> rightClasses = variant.get(1);

                    ResourceClass leftUnionClass = unionize(leftClasses).getEffectiveClass();
                    ResourceClass rightUnionClass = unionize(rightClasses).getEffectiveClass();


                    if(leftUnionClass.equals(box) && rightUnionClass.isSubclassOf(xsdString))
                    {
                        builder.append("sparql.");
                        builder.append(function);
                        builder.append("_rdfbox_string(");
                        builder.append(left.get(unionize(leftClasses, box)).get(0));
                        builder.append(", ");
                        builder.append(right.get(unionize(rightClasses, xsdString)).get(0));
                        builder.append(")");
                    }
                    else if(rightUnionClass.isSubclassOf(xsdString) || leftUnionClass instanceof LangStringWithTagClass
                            && rightUnionClass instanceof LangStringWithTagClass)
                    {
                        builder.append("sparql.");
                        builder.append(function);
                        builder.append("_string_string(");
                        builder.append(left.getStringLiteral(leftClasses));
                        builder.append(", ");
                        builder.append(right.getStringLiteral(rightClasses));
                        builder.append(")");
                    }
                    else if(leftClasses.stream().allMatch(r -> r.getEffectiveClass().equals(rdfLangString))
                            && rightClasses.stream().allMatch(r -> r.getEffectiveClass().equals(rdfLangString)))
                    {
                        List<Column> leftCols = left.get(unionize(leftClasses, rdfLangString));
                        List<Column> rightCols = right.get(unionize(rightClasses, rdfLangString));

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
                    else if(leftUnionClass instanceof LangStringWithTagClass leftConstantTagClass
                            && rightClasses.stream().allMatch(r -> r.getEffectiveClass().equals(rdfLangString)))
                    {
                        List<Column> leftCols = left.get(unionize(leftClasses, leftConstantTagClass));
                        List<Column> rightCols = right.get(unionize(rightClasses, rdfLangString));

                        builder.append("CASE WHEN '");
                        builder.append(leftConstantTagClass.getTag());
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
                    else if(leftClasses.stream().allMatch(r -> r.getEffectiveClass().equals(rdfLangString))
                            && rightUnionClass instanceof LangStringWithTagClass rightConstantTagClass)
                    {
                        List<Column> leftCols = left.get(unionize(leftClasses, rightConstantTagClass));
                        List<Column> rightCols = right.get(unionize(rightClasses, rdfLangString));

                        builder.append("CASE WHEN ");
                        builder.append(leftCols.get(1));
                        builder.append(" = '");
                        builder.append(rightConstantTagClass.getTag());
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
                        builder.append(left.get(unionize(leftClasses, box)).get(0));
                        builder.append(", ");
                        builder.append(right.get(unionize(rightClasses, box)).get(0));
                        builder.append(")");
                    }

                    cols.add(new ExpressionColumn(builder.toString()));
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

                Map<ResourceClass, Set<List<Set<ResourceClass>>>> resMap = processResultMap(arguments, map,
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
                            builder.append(left.get(unionize(leftClasses, xsdString)).get(0));
                            builder.append(", ");
                            builder.append(right.get(unionize(rightClasses, xsdString)).get(0));
                            builder.append(")");
                        }
                        else if(rightClasses.stream().allMatch(r -> isString(r)))
                        {
                            builder.append("sparql.");
                            builder.append(function);
                            builder.append("_rdfbox_string(");
                            builder.append(left.get(unionize(leftClasses, box)).get(0));
                            builder.append(", ");
                            builder.append(right.get(unionize(rightClasses, xsdString)).get(0));
                            builder.append(")");
                        }
                        else
                        {
                            builder.append("sparql.");
                            builder.append(function);
                            builder.append("_rdfbox_rdfbox(");
                            builder.append(left.get(unionize(leftClasses, box)).get(0));
                            builder.append(", ");
                            builder.append(right.get(unionize(rightClasses, box)).get(0));
                            builder.append(")");
                        }

                        cols.add(new ExpressionColumn(builder.toString()));
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


                if(!restriction.contains(xsdString))
                    return new SqlBuiltinCall(function, distinct, arguments, singletonMap(xsdString, null), canBeNull);


                Set<Column> variants = new HashSet<>();

                for(ResourceClass argumentClass : argument.getResourceClasses())
                {
                    StringBuilder builder = new StringBuilder();

                    ResourceClass ec = argumentClass.getEffectiveClass();

                    if(ec.equals(xsdString) || ec.equals(rdfLangString) || ec instanceof LangStringWithTagClass)
                    {
                        List<Column> cols = argument.get(argumentClass);

                        builder.append("sparql.encode_for_uri_string(");
                        builder.append(cols.get(0));
                        builder.append(")");
                    }
                    else if(hasStringLiteral(argumentClass))
                    {
                        List<Column> cols = argument.get(unionize(Set.of(argumentClass), box));

                        builder.append("sparql.encode_for_uri_rdfbox(");
                        builder.append(cols.get(0));
                        builder.append(")");
                    }

                    if(!builder.isEmpty())
                        variants.add(new ExpressionColumn(builder.toString()));
                }

                List<Column> result = List.of(coalesce(variants));

                return new SqlBuiltinCall(function, distinct, arguments, singletonMap(xsdString, result), canBeNull);
            }


            case "concat":
            {
                if(arguments.size() == 0)
                {
                    if(restriction.contains(xsdString))
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
                            else if(x instanceof LangStringWithTagClass && x.equals(y))
                                newClasses.add(x);
                            else if(x instanceof LangStringWithTagClass && y instanceof LangStringWithTagClass)
                                newClasses.add(xsdString);
                            else if(hasStringLiteral(x) && hasStringLiteral(y))
                                newClasses.addAll(Set.of(rdfLangString, xsdString));
                        }
                    }

                    resClasses = newClasses;
                }

                ResourceClass resultClass = resClasses.size() == 1 ? resClasses.iterator().next() :
                        unionize(Set.of(rdfLangString, xsdString), box);

                if(!restriction.contains(resultClass))
                    return new SqlBuiltinCall(function, distinct, arguments, singletonMap(resultClass, null),
                            canBeNull);

                if(arguments.size() == 1)
                    return new SqlBuiltinCall(function, distinct, arguments,
                            singletonMap(resultClass, arguments.get(0).get(resultClass)), canBeNull);


                StringBuilder builder = new StringBuilder();

                if(resultClass.equals(xsdString) || resultClass instanceof LangStringWithTagClass)
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

                    builder.append(arguments.get(0).get(unionize(Set.of(rdfLangString, xsdString), box)).get(0));

                    for(int i = 1; i < arguments.size(); i++)
                    {
                        builder.append(", ");
                        builder.append(arguments.get(i).get(unionize(Set.of(rdfLangString, xsdString), box)).get(0));
                        builder.append(")");
                    }
                }

                List<Column> result = List.of(new ExpressionColumn(builder.toString(), canBeNull));

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

                if(!restriction.contains(xsdBoolean))
                    return new SqlBuiltinCall(function, distinct, arguments, singletonMap(xsdBoolean, null), canBeNull);

                if(lang.getResourceClasses().stream().anyMatch(r -> hasString(r) && !isString(r)))
                {
                    Column lcol = lang.get(unionize(Set.of(xsdString), box)).get(0);
                    Column pcol = pattern.get(unionize(Set.of(xsdString), box)).get(0);

                    List<Column> result = List.of(new ExpressionColumn(
                            "sparql.langmatches_rdfbox_rdfbox(" + lcol + ", " + pcol + ")", canBeNull));

                    return new SqlBuiltinCall(function, distinct, arguments, singletonMap(xsdBoolean, result),
                            canBeNull);
                }
                else
                {
                    Column lcol = lang.get(xsdString).get(0);
                    Column pcol = pattern.get(xsdString).get(0);

                    List<Column> result = List.of(new ExpressionColumn(
                            "sparql.langmatches_string_string(" + lcol + ", " + pcol + ")", canBeNull));

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


                Set<List<Set<ResourceClass>>> variants = processResultMap(arguments,
                        singletonMap(xsdBoolean, rawVariants), restriction, box).get(xsdBoolean);


                Set<Column> cols = new HashSet<>();

                for(List<Set<ResourceClass>> variant : variants)
                {
                    StringBuilder builder = new StringBuilder();

                    if(variant.get(0).stream().anyMatch(r -> r.getEffectiveClass().equals(box)))
                    {
                        Column col = argument.get(unionize(variant.get(0), box)).get(0);

                        builder.append("sparql.regex_rdfbox(");
                        builder.append(col);
                    }
                    else
                    {
                        Column col = argument.getStringLiteral(variant.get(0));

                        builder.append("sparql.regex_string(");
                        builder.append(col);
                    }

                    builder.append(", ");
                    builder.append(pattern.get(xsdString).get(0));

                    if(flags != null)
                        builder.append(", ").append(flags.get(xsdString).get(0));

                    builder.append(")");

                    cols.add(new ExpressionColumn(builder.toString()));
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

                    if(resultClass.equals(rdfLangString) && additionalCanBeNull)
                        resultClass = unionize(Set.of(rdfLangString), box);

                    List<ResourceClass> list = new ArrayList<>();

                    list.add(resClass);
                    list.add(xsdString);
                    list.add(xsdString);

                    if(flags != null)
                        list.add(xsdString);

                    map.computeIfAbsent(resultClass, _ -> new HashSet<>()).add(list);
                }

                Map<ResourceClass, Set<List<Set<ResourceClass>>>> resMap = processResultMap(arguments, map, restriction,
                        box);

                Map<ResourceClass, List<Column>> mappings = new HashMap<>();

                for(Entry<ResourceClass, Set<List<Set<ResourceClass>>>> e : resMap.entrySet())
                {
                    List<Column> result = null;

                    if(e.getValue() != null)
                    {
                        Set<ResourceClass> resClasses = e.getValue().stream().flatMap(a -> a.get(0).stream())
                                .collect(toSet());

                        if(xsdString.equals(e.getKey()) || e.getKey() instanceof LangStringWithTagClass)
                        {
                            Column col = argument.getStringLiteral(resClasses);

                            StringBuilder builder = new StringBuilder();

                            builder.append("sparql.replace_string(");
                            builder.append(col);
                            builder.append(", ");
                            builder.append(pattern.get(xsdString).get(0));
                            builder.append(", ");
                            builder.append(replacement.get(xsdString).get(0));

                            if(flags != null)
                                builder.append(", ").append(flags.get(xsdString).get(0));

                            builder.append(")");

                            result = List.of(new ExpressionColumn(builder.toString()));
                        }
                        else if(rdfLangString.equals(e.getKey()))
                        {
                            List<Column> cols = argument.get(unionize(resClasses, (PrimitiveResourceClass) e.getKey()));

                            StringBuilder builder = new StringBuilder();

                            builder.append("sparql.replace_string(");
                            builder.append(cols.get(0));
                            builder.append(", ");
                            builder.append(pattern.get(xsdString).get(0));
                            builder.append(", ");
                            builder.append(replacement.get(xsdString).get(0));

                            if(flags != null)
                                builder.append(", ").append(flags.get(xsdString).get(0));

                            builder.append(")");

                            result = List.of(new ExpressionColumn(builder.toString()));
                        }
                        else if(e.getKey() != null)
                        {
                            List<Column> cols = argument.get(unionize(resClasses, box));

                            StringBuilder builder = new StringBuilder();

                            builder.append("sparql.replace_rdfbox(");
                            builder.append(cols.get(0));
                            builder.append(", ");
                            builder.append(pattern.get(xsdString).get(0));
                            builder.append(", ");
                            builder.append(replacement.get(xsdString).get(0));

                            if(flags != null)
                                builder.append(", ").append(flags.get(xsdString).get(0));

                            builder.append(")");

                            result = List.of(new ExpressionColumn(builder.toString()));
                        }
                    }

                    mappings.put(e.getKey(), result);
                }

                return new SqlBuiltinCall(function, distinct, arguments, mappings, canBeNull);
            }


            // functions on numerics:

            case "rand":
            {
                if(!restriction.contains(xsdDouble))
                    return new SqlBuiltinCall(function, distinct, arguments, singletonMap(xsdDouble, null), false);

                List<Column> result = List.of(new ExpressionColumn("random()", false));

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


                Map<ResourceClass, Set<List<Set<ResourceClass>>>> resMap = processResultMap(arguments, map, restriction,
                        box);

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
                                cols.add(function.equals("abs") ? new ExpressionColumn(function + "(" + op + ")") : op);
                            }
                        }
                        else if(Stream.of(xsdDouble, xsdFloat, xsdDecimal).anyMatch(r -> r.equals(e.getKey())))
                        {
                            //NOTE: ignoring variants should be safe here
                            Column op = argument.get(e.getKey()).get(0);
                            cols.add(new ExpressionColumn(function + "(" + op + ")"));
                        }
                        else
                        {
                            for(List<Set<ResourceClass>> variant : e.getValue())
                            {
                                Column op = argument.get(unionize(variant.get(0), box)).get(0);
                                cols.add(new ExpressionColumn("sparql." + function + "_rdfbox(" + op + ")"));
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

                if(!restriction.contains(resultClass))
                    return new SqlBuiltinCall(function, distinct, arguments, singletonMap(resultClass, null), false);

                List<Column> result = List.of(new ExpressionColumn("now()", false));

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

                if(!restriction.contains(resultClass))
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
                            builder.append(argument.get(argumentClass).get(0));
                            builder.append(", '");
                            builder.append(dateClass.getZone());
                            builder.append("'::int4)");
                        }
                    }
                    else if(!timeFunc && (argument.canSafelyGeneralize(argumentClass, xsdDate)
                            || argument.canSafelyGeneralize(argumentClass, genDate)))
                    {
                        // as date+int4

                        List<Column> columns = argumentClass.toGeneralClass(genDate, argument.get(argumentClass),
                                partCanBeBull);

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

                        List<Column> columns = argumentClass.toGeneralClass(genScalarDate, argument.get(argumentClass),
                                partCanBeBull);

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
                            builder.append(argument.get(argumentClass).get(0));
                            builder.append(", '");
                            builder.append(dateTimeClass.getZone());
                            builder.append("'::int4)");
                        }
                    }
                    else if(argument.canSafelyGeneralize(argumentClass, xsdDateTime)
                            || argument.canSafelyGeneralize(argumentClass, genDateTime))
                    {
                        // as timestamptz+int4

                        List<Column> columns = argumentClass.toGeneralClass(genDateTime, argument.get(argumentClass),
                                partCanBeBull);

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
                                argument.get(argumentClass), partCanBeBull);

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

                        List<Column> columns = argument.get(unionize(Set.of(argumentClass), box));

                        builder.append("sparql.");
                        builder.append(function);
                        builder.append("_rdfbox");
                        builder.append("(");
                        builder.append(columns.get(0));
                        builder.append(")");
                    }


                    if(!builder.isEmpty())
                        variants.add(new ExpressionColumn(builder.toString()));
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

                if(!restriction.contains(xsdString))
                    return new SqlBuiltinCall(function, distinct, arguments, singletonMap(xsdString, null), canBeNull);

                Column op = argument.get(xsdString).get(0);
                List<Column> result = List.of(new ExpressionColumn(
                        "substring(pgcrypto.digest(" + op + ",'" + function + "')::varchar from 3)", canBeNull));

                return new SqlBuiltinCall(function, distinct, arguments, singletonMap(xsdString, result), canBeNull);
            }

            case "_strhash":
            {
                SqlExpressionIntercode argument = arguments.get(0);

                if(argument.getResourceClasses().stream().noneMatch(r -> hasStringLiteral(r)))
                    return SqlNull.get();

                boolean canBeNull = argument.canBeNull()
                        || argument.getResourceClasses().stream().anyMatch(r -> !isStringLiteral(r));


                if(!restriction.contains(xsdLong))
                    return new SqlBuiltinCall(function, distinct, arguments, singletonMap(xsdLong, null), canBeNull);

                boolean partCanBeBull = argument.canBeNull() || argument.getResourceClasses().size() > 1;

                Set<Column> variants = new HashSet<>();

                for(ResourceClass argumentClass : argument.getResourceClasses())
                {
                    StringBuilder builder = new StringBuilder();

                    if(argumentClass instanceof LangStringWithTagClass)
                    {
                        builder.append("hashtextextended(");
                        builder.append(argument.getMapping(argumentClass).get(0));
                        builder.append(",0)::int8");
                    }
                    else if(isString(argumentClass))
                    {
                        builder.append("hashtextextended(");
                        builder.append(argumentClass
                                .toGeneralClass(xsdString, argument.get(argumentClass), partCanBeBull).get(0));
                        builder.append(",0)::int8");
                    }
                    else if(argument.canSafelyGeneralize(argumentClass, rdfLangString))
                    {
                        builder.append("hashtextextended(");
                        builder.append(argumentClass
                                .toGeneralClass(rdfLangString, argument.get(argumentClass), partCanBeBull).get(1));
                        builder.append(",0)::int8");
                    }
                    else if(hasStringLiteral(argumentClass))
                    {
                        builder.append("hashtextextended(sparql.rdfbox_get_string_literal(");
                        builder.append(argument.get(unionize(Set.of(argumentClass), box)).get(0));
                        builder.append("), 0)::int8");
                    }

                    if(!builder.isEmpty())
                        variants.add(new ExpressionColumn(builder.toString()));
                }

                List<Column> result = List.of(coalesce(variants));

                return new SqlBuiltinCall(function, distinct, arguments, singletonMap(xsdLong, result), canBeNull);
            }

            default:
                throw new IllegalArgumentException(); // unexpected
        }

    }


    private static ResourceClass getStringLiteralResultClass(ResourceClass resClass)
    {
        if(isString(resClass))
            return xsdString;

        if(resClass.getEffectiveClass() instanceof LangStringWithTagClass)
            return resClass.getEffectiveClass();

        if(isLangString(resClass))
            return rdfLangString;

        return unionize(Set.of(xsdString, rdfLangString), box);
    }


    private static ResourceClass getStringLiteralResultClass2(ResourceClass left)
    {
        Set<ResourceClass> resClasses = new HashSet<>();
        resClasses.add(xsdString);

        for(ResourceClass r : estimateAsUnion(left))
        {
            if(r.getEffectiveClass() instanceof LangStringWithTagClass)
                resClasses.add(r.getEffectiveClass());
            else if(hasLangString(r))
                resClasses.add(rdfLangString);
        }

        return unionize(resClasses);
    }


    @Override
    public Restrictions getRequirements()
    {
        Restrictions restrictions = new Restrictions();

        for(SqlExpressionIntercode argument : arguments)
            restrictions.add(argument.getRequirements());

        return restrictions;
    }


    public String getFunction()
    {
        return function;
    }


    public List<SqlExpressionIntercode> getArguments()
    {
        return arguments;
    }


    public SqlExpressionIntercode getArgument()
    {
        if(arguments.size() != 1)
            throw new IllegalArgumentException();

        return arguments.get(0);
    }


    public boolean isDistinct()
    {
        return distinct;
    }


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
        return null;
    }


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
        return null;
    }


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
    protected int getHashCode()
    {
        return Objects.hash(distinct, function, arguments);
    }


    @Override
    public SqlExpressionIntercode optimize(Request request, VariableBindings bindings, Restriction restriction,
            boolean evalServices)
    {
        List<SqlExpressionIntercode> optimized = !restriction.contains(unionize(getResourceClasses())) ?
                arguments.stream().map(a -> a.optimize(request, bindings, NONE, evalServices)).toList() :
                switch(function)
                {
                    case "card", "count", "sum", "avg", "min", "max", "sample", "group_concat" ->
                    {
                        yield arguments.stream().map(a -> a.optimize(request, bindings, ALL, evalServices)).toList();
                    }

                    case "bound", "isiri", "isuri", "isblank", "isliteral", "isnumeric" ->
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

                    case "lang", "datatype" ->
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

                    case "strlang" ->
                    {
                        yield arguments.stream()
                                .map(a -> a.optimize(request, bindings, new Restriction(xsdString), evalServices))
                                .toList();
                    }

                    case "strlen" ->
                    {
                        yield List.of(arguments.get(0).optimize(request, bindings,
                                new Restriction(xsdString, rdfLangString), evalServices));
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
                        yield arguments.stream().map(a -> a.optimize(request, bindings,
                                new Restriction(xsdString, rdfLangString), evalServices)).toList();
                    }

                    case "strbefore", "strafter" ->
                    {
                        //TODO can be improved
                        yield arguments.stream().map(a -> a.optimize(request, bindings,
                                new Restriction(xsdString, rdfLangString), evalServices)).toList();
                    }

                    case "encode_for_uri" ->
                    {
                        yield List.of(arguments.get(0).optimize(request, bindings,
                                new Restriction(xsdString, rdfLangString), evalServices));
                    }

                    case "concat" ->
                    {
                        //TODO can be improved
                        yield arguments.stream().map(a -> a.optimize(request, bindings,
                                new Restriction(xsdString, rdfLangString), evalServices)).toList();
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

                        args.add(arguments.get(0).optimize(request, bindings, new Restriction(xsdString, rdfLangString),
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

                        if(restriction.contains(xsdDouble))
                            set.add(genDouble);

                        if(restriction.contains(xsdFloat))
                            set.add(genFloat);

                        if(restriction.contains(xsdDecimal))
                            set.add(genDecimal);

                        if(restriction.contains(xsdInteger))
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
                                new Restriction(xsdString, rdfLangString), evalServices));
                    }

                    default -> throw new IllegalArgumentException(); // unexpected
                };


        if(optimized.equals(arguments))
            return this;

        return create(request, function, distinct, optimized);
    }


    private static boolean areStringLiteralCompatible(SqlExpressionIntercode left, SqlExpressionIntercode right)
    {
        for(ResourceClass l : left.getResourceClasses())
            for(ResourceClass r : right.getResourceClasses())
                if(areStringLiteralsCompatible(l, r))
                    return true;

        return false;
    }


    private static boolean areStringLiteralsCompatible(ResourceClass left, ResourceClass right)
    {
        for(ResourceClass l : estimateAsUnion(left))
            for(ResourceClass r : estimateAsUnion(right))
                if(areStringLiteralsCompatibleBase(l, r))
                    return true;

        return false;
    }


    private static boolean areStringLiteralsCompatibleBase(ResourceClass left, ResourceClass right)
    {
        if(left.getEffectiveClass() instanceof LangStringWithTagClass l
                && right.getEffectiveClass() instanceof LangStringWithTagClass r)
            return l.equals(r);

        if(hasString(right))
            return hasStringLiteral(left);

        if(hasLangString(left))
            return hasLangString(right);

        return false;
    }





    private static boolean areStringLiteralIncompatible(SqlExpressionIntercode left, SqlExpressionIntercode right)
    {
        for(ResourceClass l : left.getResourceClasses())
            for(ResourceClass r : right.getResourceClasses())
                if(areStringLiteralsIncompatible(l, r))
                    return true;

        return false;
    }


    private static boolean areStringLiteralsIncompatible(ResourceClass left, ResourceClass right)
    {
        for(ResourceClass l : estimateAsUnion(left))
            for(ResourceClass r : estimateAsUnion(right))
                if(areStringLiteralsIncompatibleBase(l, r))
                    return true;

        return false;
    }


    private static boolean areStringLiteralsIncompatibleBase(ResourceClass left, ResourceClass right)
    {
        ResourceClass l = left.getEffectiveClass();
        ResourceClass r = right.getEffectiveClass();

        return !(isLangString(l) && isString(r) || l instanceof LangStringWithTagClass && l.equals(r));
    }



}
