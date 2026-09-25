package cz.iocb.sparql.engine.mapping.datatypes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genScalarDateTime;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.unsupportedType;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdScalarDateTime;
import static cz.iocb.sparql.engine.mapping.classes.DerivedClass.subtract;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDateTimeIri;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.time.temporal.ChronoField;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Pattern;
import cz.iocb.sparql.engine.mapping.classes.DateTimeInZoneBaseClass;
import cz.iocb.sparql.engine.mapping.classes.DateTimeInZoneClass;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.Literal;



/**
 * The xsd:dateTime datatype, limited to the range of PostgreSQL {@code timestamptz} and microsecond precision. Literals
 * are classified by their timezone offset (see {@link DateTimeInZoneClass}).
 */
public final class DateTimeDatatype extends TemporalDatatype
{
    /**
     * Lexical form of xsd:dateTime with optional timezone.
     */
    private static final Pattern validFormPattern = Pattern.compile(XSD_DATETIME_PATTERN);

    /**
     * Parser of the lexical form with microsecond precision.
     */
    private static final DateTimeFormatter inputFormatter = new DateTimeFormatterBuilder()//
            .appendValue(ChronoField.YEAR, 4, 19, SignStyle.NORMAL).appendLiteral("-")//
            .appendValue(ChronoField.MONTH_OF_YEAR, 2).appendLiteral("-")//
            .appendValue(ChronoField.DAY_OF_MONTH, 2).appendLiteral("T")//
            .appendValue(ChronoField.HOUR_OF_DAY, 2).appendLiteral(":")//
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2).appendLiteral(":")//
            .appendValue(ChronoField.SECOND_OF_MINUTE, 2)//
            .appendFraction(ChronoField.MICRO_OF_SECOND, 0, 6, true)//
            .appendOffset("+HH:MM", "Z")//
            .toFormatter(Locale.ENGLISH);

    /**
     * Formatter of PostgreSQL {@code timestamptz} constants, with the {@code BC} suffix for negative years.
     */
    private static final DateTimeFormatter outputFormatter = new DateTimeFormatterBuilder()//
            .appendValue(ChronoField.YEAR_OF_ERA, 4, 19, SignStyle.NORMAL).appendLiteral("-")//
            .appendValue(ChronoField.MONTH_OF_YEAR, 2).appendLiteral("-")//
            .appendValue(ChronoField.DAY_OF_MONTH, 2).appendLiteral("T")//
            .appendValue(ChronoField.HOUR_OF_DAY, 2).appendLiteral(":")//
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2).appendLiteral(":")//
            .appendValue(ChronoField.SECOND_OF_MINUTE, 2)//
            .appendFraction(ChronoField.MICRO_OF_SECOND, 0, 6, true)//
            .appendOffset("+HH:MM", "Z")//
            .appendText(ChronoField.ERA, Map.of(0l, " BC", 1l, ""))//
            .toFormatter(Locale.ENGLISH);


    /**
     * Lowest instant PostgreSQL can store.
     */
    private static final OffsetDateTime MIN_VALUE = OffsetDateTime.parse("-4713-11-24T00:00:00Z", inputFormatter);

    /**
     * Highest instant PostgreSQL can store.
     */
    private static final OffsetDateTime MAX_VALUE = OffsetDateTime.parse("294276-12-31T23:59:59.999999Z",
            inputFormatter);


    /**
     * Creates the datatype.
     */
    protected DateTimeDatatype()
    {
        super(xsdDateTimeIri);
    }


    @Override
    public LiteralClass getBaseLiteralClass()
    {
        return genScalarDateTime;
    }


    @Override
    public LiteralClass getCanonicalLiteralClass()
    {
        return xsdScalarDateTime;
    }


    @Override
    public ResourceClass getResourceClass(Literal literal)
    {
        assert typeIri.equals(literal.getType());

        if(!isValidForm(literal.getValue()))
            return unsupportedType;

        if(!isCanonicalForm(literal.getValue()))
            return subtract(DateTimeInZoneBaseClass.get(getZone(literal)), DateTimeInZoneClass.get(getZone(literal))); //TODO use cache

        return DateTimeInZoneClass.get(getZone(literal));
    }


    @Override
    public boolean isValidForm(String value)
    {
        if(!validFormPattern.matcher(value).matches())
            return false;

        value = getCollapsedForm(value).replaceFirst("(\\.[0-9]{6})0*", "$1");

        if(!value.matches(".*" + ZONE))
            value = value + "Z";

        OffsetDateTime date = OffsetDateTime.parse(value, inputFormatter);

        return !date.isBefore(MIN_VALUE) && !date.isAfter(MAX_VALUE);
    }


    @Override
    public boolean isCanonicalForm(String value)
    {
        return value.equals(getCanonicalLexicalForm(value));
    }


    @Override
    public String getCanonicalLexicalForm(String value)
    {
        assert isValidForm(value);

        value = Datatype.getCollapsedForm(value).replaceFirst("(\\.[0-9]{6})0*", "$1");

        if(!value.matches(".*" + TemporalDatatype.ZONE))
            return OffsetDateTime.parse(value + "Z", inputFormatter).format(inputFormatter).replaceFirst("Z$", "");

        return OffsetDateTime.parse(value, inputFormatter).format(inputFormatter);
    }


    /**
     * The instant of the literal converted to UTC, formatted as a PostgreSQL {@code timestamptz} constant (a missing
     * timezone is taken as UTC).
     *
     * @param literal the literal
     * @return the instant of the literal converted to UTC, formatted as a PostgreSQL {@code timestamptz} constant (a
     *         missing timezone is taken as UTC)
     */
    public static String getDateTime(Literal literal)
    {
        String value = Datatype.getCollapsedForm(literal.getValue()).replaceFirst("(\\.[0-9]{6})0*", "$1");

        if(!value.matches(".*" + TemporalDatatype.ZONE))
            value = value + "Z";

        return OffsetDateTime.parse(value, inputFormatter).atZoneSameInstant(ZoneOffset.UTC).format(outputFormatter);
    }


    /**
     * Timezone offset of the literal in seconds east of UTC; {@link Integer#MIN_VALUE} when it has none.
     *
     * @param literal the literal
     * @return timezone offset of the literal in seconds east of UTC; {@link Integer#MIN_VALUE} when it has none
     */
    public static int getZone(Literal literal)
    {
        String zone = Datatype.getCollapsedForm(literal.getValue()).replaceFirst(TemporalDatatype.DATETIME, "");

        if(zone.equals("Z"))
            return 0;

        if(zone.isEmpty())
            return Integer.MIN_VALUE;

        return TimeZone.getTimeZone("GMT" + zone).getRawOffset() / 1000;
    }
}
