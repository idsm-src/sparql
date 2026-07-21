package cz.iocb.sparql.engine.translator.imcode.expression;

import static cz.iocb.sparql.engine.database.Column.coalesce;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasBoolean;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasDate;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasDateTime;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasDayTimeDuration;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasIri;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasNumeric;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasString;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.hasTemporalClass;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.iri;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isBoolean;
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
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isNumeric;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isShort;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.isString;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.numeric;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.temporal;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdBoolean;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdCompositeDate;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdCompositeDateTime;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDayTimeDuration;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDecimal;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDouble;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdFloat;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdLong;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdScalarDate;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdScalarDateTime;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdShort;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.engine.mapping.classes.ResourceClass.getUnionClass;
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
import cz.iocb.sparql.engine.mapping.classes.DateConstantZoneClass;
import cz.iocb.sparql.engine.mapping.classes.DateTimeConstantZoneClass;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.mapping.classes.UnsupportedLiteralClass;
import cz.iocb.sparql.engine.mapping.classes.UserLiteralClass;
import cz.iocb.sparql.engine.parser.model.IRI;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.UsedVariables;



public final class SqlCast extends SqlUnary
{
    private static final Set<LiteralClass> supportedClasses = Set.of(xsdBoolean, xsdShort, xsdInt, xsdLong, xsdInteger,
            xsdDecimal, xsdFloat, xsdDouble, xsdString, xsdDayTimeDuration, xsdScalarDateTime, xsdScalarDate);

    private final ResourceClass resourceClass;


    protected SqlCast(ResourceClass resourceClass, SqlExpressionIntercode operand,
            Map<ResourceClass, List<Column>> mappings, boolean canBeNull)
    {
        super(operand, mappings, canBeNull);

        this.resourceClass = resourceClass;
    }


    public static SqlExpressionIntercode create(ResourceClass resourceClass, SqlExpressionIntercode operand)
    {
        return create(resourceClass, operand, Restriction.ALL);
    }


    public static SqlExpressionIntercode create(ResourceClass castClass, SqlExpressionIntercode operand,
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
                        .allMatch(r -> r instanceof DateConstantZoneClass || r instanceof DateTimeConstantZoneClass);

        if(onlyConstantTags)
        {
            Map<ResourceClass, Set<Column>> variants = new HashMap<ResourceClass, Set<Column>>();

            for(Entry<ResourceClass, List<Column>> e : operand.getMappings().entrySet())
            {
                if(isCastable(e.getKey(), castClass))
                {
                    Integer zone = extractConstantZone(e.getKey());
                    ResourceClass resultClass = createConstantZoneResultClass(castClass, zone);
                    Column column = translate(e.getValue(), e.getKey(), resultClass, partCanBeNull);
                    variants.computeIfAbsent(resultClass, _ -> new HashSet<>()).add(column);
                }
            }

            Map<ResourceClass, List<Column>> mappings = new HashMap<ResourceClass, List<Column>>();

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


        Set<Column> variants = new HashSet<Column>();

        for(Entry<ResourceClass, List<Column>> e : operand.getMappings().entrySet())
            if(isCastable(e.getKey(), castClass))
                variants.add(translate(e.getValue(), e.getKey(), castClass, partCanBeNull));

        Column result = coalesce(variants);

        return new SqlCast(castClass, operand, singletonMap(castClass, List.of(result)), canBeNull);
    }


    private static ResourceClass createConstantZoneResultClass(ResourceClass castClass, int zone)
    {
        if(castClass.equals(xsdScalarDate))
            return DateConstantZoneClass.get(zone);

        if(castClass.equals(xsdScalarDateTime))
            return DateTimeConstantZoneClass.get(zone);

        throw new IllegalArgumentException();
    }


    private static int extractConstantZone(ResourceClass resClass)
    {
        if(resClass.getEffectiveClass() instanceof DateConstantZoneClass dateClass)
            return dateClass.getZone();

        if(resClass.getEffectiveClass() instanceof DateTimeConstantZoneClass dateTimeClass)
            return dateTimeClass.getZone();

        throw new IllegalArgumentException();
    }


    @Override
    public SqlExpressionIntercode optimize(Request request, UsedVariables variables, Restriction restriction,
            boolean evalServices)
    {
        Restriction operandRestriction = new Restriction();

        if(restriction.contains(resourceClass))
            for(ResourceClass resClass : operand.getResourceClasses())
                if(isCastable(resClass, resourceClass))
                    operandRestriction.add(resClass);

        SqlExpressionIntercode optOperand = operand.optimize(request, variables, operandRestriction, evalServices);

        if(optOperand == operand && restriction.isOptimized(variable))
            return this;

        return create(resourceClass, optOperand, restriction);
    }


    @Override
    public void generateExplanation(StringBuilder builder, String indent, int priority)
    {
        IRI type = ((LiteralClass) resourceClass).getTypeIri();

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
            return !from.isSubclassOf(getUnionClass(xsdString, xsdBoolean, temporal, numeric, iri));

        else if(isBoolean(to))
            return !from.isSubclassOf(getUnionClass(xsdBoolean, numeric));

        else if(isShort(to))
            return !from.isSubclassOf(getUnionClass(xsdBoolean, xsdShort));

        else if(isInt(to))
            return !from.isSubclassOf(getUnionClass(xsdBoolean, xsdShort, xsdInt));

        else if(isLong(to))
            return !from.isSubclassOf(getUnionClass(xsdBoolean, xsdShort, xsdInt, xsdLong));

        else if(isInteger(to))
            return !from.isSubclassOf(getUnionClass(xsdBoolean, xsdShort, xsdInt, xsdLong, xsdInteger));

        else if(isDecimal(to))
            return !from.isSubclassOf(getUnionClass(xsdBoolean, xsdShort, xsdInt, xsdLong, xsdInteger, xsdDecimal));

        else if(isFloat(to) || isDouble(to))
            return !from.isSubclassOf(getUnionClass(xsdBoolean, numeric));

        else if(isDate(to) || isDateTime(to))
            return !from.isSubclassOf(getUnionClass(xsdScalarDate, xsdScalarDateTime));

        else if(isDayTimeDuration(to))
            return !from.isSubclassOf(getUnionClass(xsdDayTimeDuration));

        else
            throw new IllegalArgumentException();
    }


    public static Column translate(List<Column> columns, ResourceClass resClass, ResourceClass castClass,
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

        else if(castClass instanceof DateTimeConstantZoneClass resultClass && effClass instanceof DateConstantZoneClass)
        {
            builder.append("sparql.cast_as_plain_datetime_from_date(");
            builder.append(columns.get(0));
            builder.append(", '");
            builder.append(resultClass.getZone());
            builder.append("'::int4)");
        }
        else if(castClass instanceof DateConstantZoneClass resultClass && resClass instanceof DateTimeConstantZoneClass)
        {
            builder.append("sparql.cast_as_plain_date_from_datetime(");
            builder.append(columns.get(0));
            builder.append(", '");
            builder.append(resultClass.getZone());
            builder.append("'::int4)");
        }


        /* special casts to xsd:date */

        else if(castClass.equals(xsdScalarDate) && effClass.equals(xsdCompositeDate))
        {
            builder.append("sparql.zoneddate_create(");
            builder.append(columns.get(0));
            builder.append(", '");
            builder.append(columns.get(1));
            builder.append("'::int4)");
        }
        else if(castClass.equals(xsdScalarDate) && resClass instanceof DateConstantZoneClass dateClass)
        {
            builder.append("sparql.zoneddate_create(");
            builder.append(columns.get(0));
            builder.append(", '");
            builder.append(dateClass.getZone());
            builder.append("'::int4)");
        }
        else if(castClass.equals(xsdScalarDate) && effClass.equals(xsdCompositeDateTime))
        {
            builder.append("sparql.cast_as_date_from_datetime(");
            builder.append(columns.get(0));
            builder.append(", ");
            builder.append(columns.get(1));
            builder.append(")");
        }
        else if(castClass.equals(xsdScalarDate) && effClass instanceof DateTimeConstantZoneClass dateTimeClass)
        {
            builder.append("sparql.cast_as_date_from_datetime(");
            builder.append(columns.get(0));
            builder.append(", '");
            builder.append(dateTimeClass.getZone());
            builder.append("'::int4)");
        }


        /* special casts to xsd:dateTime */

        else if(castClass.equals(xsdScalarDateTime) && effClass.equals(xsdCompositeDateTime))
        {
            builder.append("sparql.zoneddatetime_create(");
            builder.append(columns.get(0));
            builder.append(", '");
            builder.append(columns.get(1));
            builder.append("'::int4)");
        }
        else if(castClass.equals(xsdScalarDateTime) && resClass instanceof DateTimeConstantZoneClass dateTimeClass)
        {
            builder.append("sparql.zoneddatetime_create(");
            builder.append(columns.get(0));
            builder.append(", '");
            builder.append(dateTimeClass.getZone());
            builder.append("'::int4)");
        }
        else if(castClass.equals(xsdScalarDateTime) && effClass.equals(xsdCompositeDate))
        {
            builder.append("sparql.cast_as_datetime_from_date(");
            builder.append(columns.get(0));
            builder.append(", ");
            builder.append(columns.get(1));
            builder.append(")");
        }
        else if(castClass.equals(xsdScalarDateTime) && effClass instanceof DateConstantZoneClass dateClass)
        {
            builder.append("sparql.cast_as_datetime_from_date(");
            builder.append(columns.get(0));
            builder.append(", '");
            builder.append(dateClass.getZone());
            builder.append("'::int4)");
        }


        /* special casts to string */

        else if(castClass.equals(xsdString) && effClass.equals(xsdCompositeDate))
        {
            builder.append("sparql.cast_as_string_from_date(");
            builder.append(columns.get(0));
            builder.append(", ");
            builder.append(columns.get(1));
            builder.append(")");
        }
        else if(castClass.equals(xsdString) && effClass instanceof DateConstantZoneClass dateClass)
        {
            builder.append("sparql.cast_as_string_from_date(");
            builder.append(columns.get(0));
            builder.append(", '");
            builder.append(dateClass.getZone());
            builder.append("'::int4)");
        }
        else if(castClass.equals(xsdString) && effClass.equals(xsdCompositeDateTime))
        {
            builder.append("sparql.cast_as_string_from_datetime(");
            builder.append(columns.get(0));
            builder.append(", ");
            builder.append(columns.get(1));
            builder.append(")");
        }
        else if(castClass.equals(xsdString) && effClass instanceof DateTimeConstantZoneClass dateTimeClass)
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
        //FIXME: it is necessary to distinguish between unsupported type and invalid lexical form
        else if(castClass.equals(xsdString) && effClass instanceof UnsupportedLiteralClass)
        {
            builder.append(columns.get(0));
        }
        else if(castClass.equals(xsdString) && effClass instanceof UserLiteralClass)
        {
            builder.append(columns.get(0));
            builder.append("::varchar");
        }


        /* general casts */

        else if(supportedClasses.contains(effClass))
        {
            builder.append("sparql.cast_as_");
            builder.append(castClass.getName());
            builder.append("_from_");
            builder.append(effClass.getName());
            builder.append("(");
            builder.append(columns.get(0));
            builder.append(")");
        }
        else
        {
            builder.append("sparql.cast_as_");
            builder.append(castClass.getName());
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
