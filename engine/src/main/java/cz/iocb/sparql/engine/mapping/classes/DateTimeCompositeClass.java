package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdScalarDateTime;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinDataTypeIRIs.xsdDateTimeIri;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.constant;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.expression;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.parser.model.expression.Literal;



public final class DateTimeCompositeClass extends LiteralClass implements ResultResourceClass
{
    private static final Map<Long, String> era = Map.of(0l, " BC", 1l, "");

    private static final DateTimeFormatter dateTimeFormatter = new DateTimeFormatterBuilder()//
            .appendValue(ChronoField.YEAR_OF_ERA, 4, 19, SignStyle.NORMAL).appendLiteral("-")//
            .appendValue(ChronoField.MONTH_OF_YEAR, 2).appendLiteral("-")//
            .appendValue(ChronoField.DAY_OF_MONTH, 2).appendLiteral("T")//
            .appendValue(ChronoField.HOUR_OF_DAY, 2).appendLiteral(":")//
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2).appendLiteral(":")//
            .appendValue(ChronoField.SECOND_OF_MINUTE, 2)//
            .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)//
            .appendOffset("+HH:MM", "Z")//
            .appendText(ChronoField.ERA, era)//
            .toFormatter(Locale.ENGLISH);

    private static final DateTimeFormatter localDateTimeFormatter = new DateTimeFormatterBuilder()//
            .appendValue(ChronoField.YEAR_OF_ERA, 4, 19, SignStyle.NORMAL).appendLiteral("-")//
            .appendValue(ChronoField.MONTH_OF_YEAR, 2).appendLiteral("-")//
            .appendValue(ChronoField.DAY_OF_MONTH, 2).appendLiteral("T")//
            .appendValue(ChronoField.HOUR_OF_DAY, 2).appendLiteral(":")//
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2).appendLiteral(":")//
            .appendValue(ChronoField.SECOND_OF_MINUTE, 2)//
            .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)//
            .appendLiteral("Z")//
            .appendText(ChronoField.ERA, era)//
            .toFormatter(Locale.ENGLISH);


    protected DateTimeCompositeClass()
    {
        super("datetime", xsdDateTimeIri, List.of("timestamptz", "int4"), Set.of(box, xsdScalarDateTime));
    }


    @Override
    public Set<ResultResourceClass> getResultResourceClasses()
    {
        return Set.of(this);
    }


    @Override
    public List<Column> toColumns(Literal literal)
    {
        return List.of(constant(getDateTime(literal), "timestamptz"), constant(getZone(literal), "int4"));
    }


    @Override
    public List<Column> toGeneralClass(ResourceClass superClass, List<Column> columns, boolean canBeNull)
    {
        assert isSubclassOf(superClass);

        ResourceClass targetClass = superClass.getEffectiveClass();

        if(targetClass.equals(this))
            return columns;

        Column time = columns.get(0);
        Column zone = columns.get(1);

        if(targetClass.equals(box))
            return List.of(expression("sparql.rdfbox_create_from_datetime(%s, %s)", zone, time));

        if(targetClass.equals(xsdScalarDateTime))
            return List.of(expression("sparql.zoneddatetime_create(%s, %s)", zone, time));

        throw new IllegalArgumentException();
    }


    @Override
    public List<Column> fromGeneralClass(ResourceClass superClass, List<Column> columns)
    {
        if(superClass.equals(this))
            return columns;

        ResourceClass sourceClass = superClass.getEffectiveClass();

        assert isSubclassOf(sourceClass);

        if(sourceClass.equals(box))
            return List.of(expression("sparql.rdfbox_get_datetime_value(%s)", columns.get(0)),
                    expression("sparql.rdfbox_get_datetime_zone(%s)", columns.get(0)));

        if(sourceClass.equals(xsdScalarDateTime))
            return List.of(expression("sparql.zoneddatetime_get_value(%s)", columns.get(0)),
                    expression("sparql.zoneddatetime_get_zone(%s)", columns.get(0)));

        throw new IllegalArgumentException();
    }


    protected static String getDateTime(Literal literal)
    {
        return switch(literal.getValue())
        {
            case OffsetDateTime value -> value.atZoneSameInstant(ZoneOffset.UTC).format(dateTimeFormatter);
            case LocalDateTime value -> value.format(localDateTimeFormatter);
            default -> null;
        };
    }


    protected static int getZone(Literal literal)
    {
        if(literal.getValue() instanceof OffsetDateTime value)
            return value.getOffset().getTotalSeconds();

        return Integer.MIN_VALUE;
    }
}
