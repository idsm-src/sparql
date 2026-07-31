package cz.iocb.sparql.engine.mapping.datatypes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.unsupportedLiteral;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdScalarDateTime;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDateTimeIri;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.time.temporal.ChronoField;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import cz.iocb.sparql.engine.mapping.classes.DateTimeCompositeClass;
import cz.iocb.sparql.engine.mapping.classes.DateTimeConstantZoneClass;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;
import cz.iocb.sparql.engine.rdf.Literal;



public class DateTimeDatatype extends TemporalDatatype
{
    private static final Pattern validFormPattern = Pattern.compile(XSD_DATETIME_PATTERN);

    private static final DateTimeFormatter dateTimeFormatter = new DateTimeFormatterBuilder()//
            .appendText(ChronoField.ERA, Map.of(0L, "-", 1L, ""))//
            .appendValue(ChronoField.YEAR_OF_ERA, 4, 19, SignStyle.NEVER).appendLiteral("-")//
            .appendValue(ChronoField.MONTH_OF_YEAR, 2).appendLiteral("-")//
            .appendValue(ChronoField.DAY_OF_MONTH, 2).appendLiteral("T")//
            .appendValue(ChronoField.HOUR_OF_DAY, 2).appendLiteral(":")//
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2).appendLiteral(":")//
            .appendValue(ChronoField.SECOND_OF_MINUTE, 2)//
            .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)//
            .appendOffset("+HH:MM", "Z")//
            .toFormatter(Locale.ENGLISH);

    //NOTE: limitations given by using the timestamp type in postgres
    private static final OffsetDateTime MIN_VALUE = OffsetDateTime.parse("-4714-11-24T00:00:00Z", dateTimeFormatter);
    private static final OffsetDateTime MAX_VALUE = OffsetDateTime.parse("294277-01-01T00:00:00Z", dateTimeFormatter);


    protected DateTimeDatatype()
    {
        super(xsdDateTimeIri);
    }


    @Override
    public LiteralClass getGeneralLiteralClass()
    {
        return xsdScalarDateTime;
    }


    @Override
    public LiteralClass getResourceClass(Literal literal)
    {
        assert typeIri.equals(literal.getType());

        if(!isValidForm(literal.getValue()))
            return unsupportedLiteral;

        return DateTimeConstantZoneClass.get(DateTimeCompositeClass.getZone(literal));
    }


    @Override
    public boolean isValidForm(String value)
    {
        if(!validFormPattern.matcher(value).matches())
            return false;

        value = getCollapsedForm(value).replaceFirst("(\\.[0-9]{6})[0-9]*", "$1");

        if(!value.matches(".*" + ZONE))
            value = value + "Z";

        OffsetDateTime date = OffsetDateTime.parse(value, dateTimeFormatter);

        return date.compareTo(MAX_VALUE) < 0 && date.compareTo(MIN_VALUE) >= 0;
    }


    @Override
    public String getCanonicalLexicalForm(String value)
    {
        return getCollapsedForm(value).replaceFirst("((\\.[0-9]{0,5}[1-9])[0-9]*|\\.[0-9]*)([^0-9]|$)", "$2")
                .replaceFirst("[-+]00:00", "Z");
    }
}
