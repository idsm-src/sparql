package cz.iocb.sparql.engine.imcode.expression;

import static cz.iocb.sparql.engine.database.Column.coalesce;
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
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasBoolean;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasDate;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasDateTime;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasDayTimeDuration;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasIri;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasNumeric;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasString;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasTemporalClass;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.intNumeric;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.integerNumeric;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.iri;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isBoolean;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isByte;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isDate;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isDateTime;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isDayTimeDuration;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isDecimal;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isDouble;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isFloat;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isInt;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isIri;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isLong;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isNegativeInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isNonNegativeInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isNonPositiveInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isNumeric;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isPositiveInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isShort;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isString;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isUnsignedByte;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isUnsignedInt;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isUnsignedLong;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isUnsignedShort;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.longNumeric;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.numeric;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.shortNumeric;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.temporal;
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
import static cz.iocb.sparql.engine.mapping.classes.DerivedClass.unionize;
import static java.util.Collections.singletonMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ExpressionColumn;
import cz.iocb.sparql.engine.mapping.classes.DateInZone;
import cz.iocb.sparql.engine.mapping.classes.DateInZoneClass;
import cz.iocb.sparql.engine.mapping.classes.DateTimeInZone;
import cz.iocb.sparql.engine.mapping.classes.DateTimeInZoneClass;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.VariableBindings;



public final class SqlCast extends SqlUnary
{
    private static final Set<LiteralClass> supportedClasses = Set.of(genBoolean, genByte, genUnsignedByte, genShort,
            genUnsignedShort, genInt, genUnsignedInt, genLong, genUnsignedLong, genInteger, genNonPositiveInteger,
            genNegativeInteger, genNonNegativeInteger, genPositiveInteger, genDecimal, genFloat, genDouble,
            genScalarDateTime, genScalarDate, genDayTimeDuration, xsdBoolean, xsdByte, xsdUnsignedByte, xsdShort,
            xsdUnsignedShort, xsdInt, xsdUnsignedInt, xsdLong, xsdUnsignedLong, xsdInteger, xsdNonPositiveInteger,
            xsdNegativeInteger, xsdNonNegativeInteger, xsdPositiveInteger, xsdDecimal, xsdFloat, xsdDouble,
            xsdScalarDateTime, xsdScalarDate, xsdDayTimeDuration, xsdString);

    private final LiteralClass resourceClass;


    protected SqlCast(LiteralClass resourceClass, SqlExpressionIntercode operand,
            Map<ResourceClass, List<Column>> mappings, boolean canBeNull)
    {
        super(operand, mappings, canBeNull);

        this.resourceClass = resourceClass;
    }


    public static SqlExpressionIntercode create(LiteralClass resourceClass, SqlExpressionIntercode operand)
    {
        return create(resourceClass, operand, Restriction.ALL);
    }


    public static SqlExpressionIntercode create(LiteralClass castClass, SqlExpressionIntercode operand,
            Restriction restriction)
    {
        //TODO: add support for casting to user literals

        if(operand.equals(SqlNull.get()))
            return SqlNull.get();

        if(operand.getResourceClasses().stream().noneMatch(r -> isCastable(r, castClass)))
            return SqlNull.get();

        boolean canBeNull = operand.canBeNull()
                || operand.getResourceClasses().stream().anyMatch(r -> canBeNull(r, castClass));

        boolean partCanBeNull = operand.canBeNull() || operand.getResourceClasses().size() > 1;


        /* special casts for dates and times having constant zones */

        boolean onlyConstantTags = (castClass.equals(xsdScalarDate) || castClass.equals(xsdScalarDateTime))
                && operand.getResourceClasses().stream().map(r -> r.getEffectiveClass())
                        .allMatch(r -> r instanceof DateInZone || r instanceof DateTimeInZone);

        if(onlyConstantTags)
        {
            Map<ResourceClass, Set<Column>> variants = new HashMap<>();

            for(Entry<ResourceClass, List<Column>> e : operand.getMappings().entrySet())
            {
                if(isCastable(e.getKey(), castClass))
                {
                    Integer zone = extractConstantZone(e.getKey());
                    LiteralClass resultClass = createConstantZoneResultClass(castClass, zone);
                    Column column = translate(e.getValue(), e.getKey(), resultClass, partCanBeNull);
                    variants.computeIfAbsent(resultClass, _ -> new HashSet<>()).add(column);
                }
            }

            Map<ResourceClass, List<Column>> mappings = new HashMap<>();

            for(Entry<ResourceClass, Set<Column>> e : variants.entrySet())
            {
                if(!restriction.contains(e.getKey()))
                    mappings.put(e.getKey(), null);
                else
                    mappings.put(e.getKey(), List.of(coalesce(e.getValue())));
            }

            return new SqlCast(castClass, operand, mappings, canBeNull);
        }


        if(!restriction.contains(castClass))
            return new SqlCast(castClass, operand, singletonMap(castClass, null), canBeNull);


        Set<Column> variants = new HashSet<>();

        for(Entry<ResourceClass, List<Column>> e : operand.getMappings().entrySet())
            if(isCastable(e.getKey(), castClass))
                variants.add(translate(e.getValue(), e.getKey(), castClass, partCanBeNull));

        Column result = coalesce(variants);

        return new SqlCast(castClass, operand, singletonMap(castClass, List.of(result)), canBeNull);
    }


    private static LiteralClass createConstantZoneResultClass(ResourceClass castClass, int zone)
    {
        if(castClass.equals(xsdScalarDate))
            return DateInZoneClass.get(zone);

        if(castClass.equals(xsdScalarDateTime))
            return DateTimeInZoneClass.get(zone);

        throw new IllegalArgumentException();
    }


    private static int extractConstantZone(ResourceClass resClass)
    {
        if(resClass.getEffectiveClass() instanceof DateInZone dateClass)
            return dateClass.getZone();

        if(resClass.getEffectiveClass() instanceof DateTimeInZone dateTimeClass)
            return dateTimeClass.getZone();

        throw new IllegalArgumentException();
    }


    @Override
    public SqlExpressionIntercode optimize(Request request, VariableBindings bindings, Restriction restriction,
            boolean evalServices)
    {
        Restriction operandRestriction = new Restriction();

        if(restriction.contains(resourceClass))
            for(ResourceClass resClass : operand.getResourceClasses())
                if(isCastable(resClass, resourceClass))
                    operandRestriction.add(resClass);

        SqlExpressionIntercode optOperand = operand.optimize(request, bindings, operandRestriction, evalServices);

        if(optOperand == operand && restriction.isOptimized(variableBinding))
            return this;

        return create(resourceClass, optOperand, restriction);
    }


    @Override
    public void generateExplanation(StringBuilder builder, String indent, int priority)
    {
        Iri type = resourceClass.getTypeIri();

        if(type.getValue().startsWith("http://www.w3.org/2001/XMLSchema#"))
            builder.append("xsd:").append(type.getValue().substring(33));
        else
            builder.append(type);

        builder.append("(");
        operand.generateExplanation(builder, indent, 10);
        builder.append(")");
    }


    private static boolean isCastable(ResourceClass from, ResourceClass to)
    {
        if(isString(to))
            return hasIri(from) || hasString(from) || hasNumeric(from) || hasBoolean(from) || hasTemporalClass(from);
        else if(isNumeric(to) || isBoolean(to))
            return hasString(from) || hasNumeric(from) || hasBoolean(from);
        else if(isDate(to) || isDateTime(to))
            return hasString(from) || hasDate(from) || hasDateTime(from);
        else if(isDayTimeDuration(to))
            return hasString(from) || hasDayTimeDuration(from);
        else
            throw new IllegalArgumentException();
    }



    private static boolean canBeNull(ResourceClass from, ResourceClass to)
    {
        if(isString(to))
            return !from.isSubclassOf(unionize(xsdString, genBoolean, temporal, numeric, iri));

        if(isBoolean(to))
            return !from.isSubclassOf(unionize(genBoolean, numeric));

        if(isByte(to))
            return !from.isSubclassOf(unionize(genBoolean, genByte));

        if(isUnsignedByte(to))
            return !from.isSubclassOf(unionize(genBoolean, genUnsignedByte));

        if(isShort(to))
            return !from.isSubclassOf(unionize(genBoolean, shortNumeric));

        if(isUnsignedShort(to))
            return !from.isSubclassOf(unionize(genBoolean, genUnsignedByte, genUnsignedShort));

        if(isInt(to))
            return !from.isSubclassOf(unionize(genBoolean, intNumeric));

        if(isUnsignedInt(to))
            return !from.isSubclassOf(unionize(genBoolean, genUnsignedByte, genUnsignedShort, genUnsignedInt));

        if(isLong(to))
            return !from.isSubclassOf(unionize(genBoolean, longNumeric));

        if(isUnsignedLong(to))
            return !from.isSubclassOf(
                    unionize(genBoolean, genUnsignedByte, genUnsignedShort, genUnsignedInt, genUnsignedLong));

        if(isInteger(to))
            return !from.isSubclassOf(unionize(genBoolean, integerNumeric));

        if(isNonPositiveInteger(to))
            return !from.isSubclassOf(unionize(genNonPositiveInteger, genNegativeInteger));

        if(isNegativeInteger(to))
            return !from.isSubclassOf(genNegativeInteger);

        if(isNonNegativeInteger(to))
            return !from.isSubclassOf(unionize(genBoolean, genUnsignedByte, genUnsignedShort, genUnsignedInt,
                    genUnsignedLong, genNonNegativeInteger, genPositiveInteger));

        if(isPositiveInteger(to))
            return !from.isSubclassOf(genPositiveInteger);

        if(isDecimal(to))
            return !from.isSubclassOf(unionize(genBoolean, integerNumeric, genDecimal));

        if(isFloat(to) || isDouble(to))
            return !from.isSubclassOf(unionize(genBoolean, numeric));

        if(isDate(to) || isDateTime(to))
            return !from.isSubclassOf(unionize(genScalarDateTime, genScalarDate));

        if(isDayTimeDuration(to))
            return !from.isSubclassOf(unionize(genDayTimeDuration));

        throw new IllegalArgumentException();
    }


    public static Column translate(List<Column> columns, ResourceClass resClass, LiteralClass castClass,
            boolean partCanBeNull)
    {
        ResourceClass effClass = resClass.getEffectiveClass();
        StringBuilder builder = new StringBuilder();


        /* identity cats */

        if(effClass.isSubclassOf(castClass))
        {
            builder.append(effClass.toGeneralClass(castClass, columns, partCanBeNull).get(0));
        }


        /* special constant zone casts */

        else if(castClass instanceof DateTimeInZoneClass resultClass && effClass instanceof DateInZone)
        {
            builder.append("sparql.cast_as_plain_datetime_from_date(");
            builder.append(columns.get(0));
            builder.append(", '");
            builder.append(resultClass.getZone());
            builder.append("'::int4)");
        }
        else if(castClass instanceof DateInZoneClass resultClass && resClass instanceof DateTimeInZone)
        {
            builder.append("sparql.cast_as_plain_date_from_datetime(");
            builder.append(columns.get(0));
            builder.append(", '");
            builder.append(resultClass.getZone());
            builder.append("'::int4)");
        }


        /* special casts to xsd:date */

        else if(castClass.equals(xsdScalarDate) && (effClass.equals(xsdDate) || effClass.equals(genDate)))
        {
            builder.append("sparql.zoneddate_create(");
            builder.append(columns.get(0));
            builder.append(", '");
            builder.append(columns.get(1));
            builder.append("'::int4)");
        }
        else if(castClass.equals(xsdScalarDate) && resClass instanceof DateInZone dateClass)
        {
            builder.append("sparql.zoneddate_create(");
            builder.append(columns.get(0));
            builder.append(", '");
            builder.append(dateClass.getZone());
            builder.append("'::int4)");
        }
        else if(castClass.equals(xsdScalarDate) && (effClass.equals(xsdDateTime) || effClass.equals(genDateTime)))
        {
            builder.append("sparql.cast_as_date_from_datetime(");
            builder.append(columns.get(0));
            builder.append(", ");
            builder.append(columns.get(1));
            builder.append(")");
        }
        else if(castClass.equals(xsdScalarDate) && effClass instanceof DateTimeInZone dateTimeClass)
        {
            builder.append("sparql.cast_as_date_from_datetime(");
            builder.append(columns.get(0));
            builder.append(", '");
            builder.append(dateTimeClass.getZone());
            builder.append("'::int4)");
        }


        /* special casts to xsd:dateTime */

        else if(castClass.equals(xsdScalarDateTime) && (effClass.equals(xsdDateTime) || effClass.equals(genDateTime)))
        {
            builder.append("sparql.zoneddatetime_create(");
            builder.append(columns.get(0));
            builder.append(", '");
            builder.append(columns.get(1));
            builder.append("'::int4)");
        }
        else if(castClass.equals(xsdScalarDateTime) && resClass instanceof DateTimeInZone dateTimeClass)
        {
            builder.append("sparql.zoneddatetime_create(");
            builder.append(columns.get(0));
            builder.append(", '");
            builder.append(dateTimeClass.getZone());
            builder.append("'::int4)");
        }
        else if(castClass.equals(xsdScalarDateTime) && (effClass.equals(xsdDate) || effClass.equals(genDate)))
        {
            builder.append("sparql.cast_as_datetime_from_date(");
            builder.append(columns.get(0));
            builder.append(", ");
            builder.append(columns.get(1));
            builder.append(")");
        }
        else if(castClass.equals(xsdScalarDateTime) && effClass instanceof DateInZone dateClass)
        {
            builder.append("sparql.cast_as_datetime_from_date(");
            builder.append(columns.get(0));
            builder.append(", '");
            builder.append(dateClass.getZone());
            builder.append("'::int4)");
        }


        /* special casts to string */

        else if(castClass.equals(xsdString) && (effClass.equals(xsdDate) || effClass.equals(genDate)))
        {
            builder.append("sparql.cast_as_string_from_date(");
            builder.append(columns.get(0));
            builder.append(", ");
            builder.append(columns.get(1));
            builder.append(")");
        }
        else if(castClass.equals(xsdString) && effClass instanceof DateInZone dateClass)
        {
            builder.append("sparql.cast_as_string_from_date(");
            builder.append(columns.get(0));
            builder.append(", '");
            builder.append(dateClass.getZone());
            builder.append("'::int4)");
        }
        else if(castClass.equals(xsdString) && (effClass.equals(xsdDateTime) || effClass.equals(genDateTime)))
        {
            builder.append("sparql.cast_as_string_from_datetime(");
            builder.append(columns.get(0));
            builder.append(", ");
            builder.append(columns.get(1));
            builder.append(")");
        }
        else if(castClass.equals(xsdString) && effClass instanceof DateTimeInZone dateTimeClass)
        {
            builder.append("sparql.cast_as_string_from_datetime(");
            builder.append(columns.get(0));
            builder.append(", '");
            builder.append(dateTimeClass.getZone());
            builder.append("'::int4)");
        }
        else if(castClass.equals(xsdString) && isIri(effClass))
        {
            builder.append(effClass.toGeneralClass(iri, columns, partCanBeNull).get(0));
        }


        /* general casts */

        else if(supportedClasses.contains(effClass))
        {
            if(effClass instanceof LiteralClass litClass && castClass.getDatatype().equals(litClass.getDatatype()))
            {
                builder.append(columns.get(0));
            }
            else
            {
                builder.append("sparql.cast_as_");
                builder.append(getLiteralClassName(castClass));
                builder.append("_from_");
                builder.append(getLiteralClassName(effClass));
                builder.append("(");
                builder.append(columns.get(0));
                builder.append(")");
            }
        }
        else
        {
            builder.append("sparql.cast_as_");
            builder.append(getLiteralClassName(castClass));
            builder.append("_from_rdfbox");
            builder.append("(");
            builder.append(effClass.toGeneralClass(box, columns, partCanBeNull).get(0));
            builder.append(")");
        }

        return new ExpressionColumn(builder.toString());
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(!(object instanceof SqlCast imcode))
            return false;

        if(!super.equals(imcode))
            return false;

        if(!Objects.equals(resourceClass, imcode.resourceClass))
            return false;

        if(!Objects.equals(operand, imcode.operand))
            return false;

        return true;
    }


    @Override
    protected int getHashCode()
    {
        return Objects.hash(resourceClass, operand);
    }
}
