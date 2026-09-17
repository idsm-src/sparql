package cz.iocb.sparql.engine.mapping.datatypes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genScalarDate;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.unsupportedType;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdScalarDate;
import static cz.iocb.sparql.engine.mapping.classes.DerivedClass.subtract;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDateIri;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.time.temporal.ChronoField;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Pattern;
import cz.iocb.sparql.engine.mapping.classes.DateInZoneBaseClass;
import cz.iocb.sparql.engine.mapping.classes.DateInZoneClass;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.Literal;



public final class DateDatatype extends TemporalDatatype
{
    private static final Pattern validFormPattern = Pattern.compile(XSD_DATE_PATTERN);

    private static final DateTimeFormatter inputFormatter = new DateTimeFormatterBuilder()//
            .appendValue(ChronoField.YEAR, 4, 19, SignStyle.NORMAL).appendLiteral("-")//
            .appendValue(ChronoField.MONTH_OF_YEAR, 2).appendLiteral("-")//
            .appendValue(ChronoField.DAY_OF_MONTH, 2)//
            .appendOffset("+HH:MM", "Z")//
            .toFormatter(Locale.ENGLISH);

    private static final DateTimeFormatter outputFormatter = new DateTimeFormatterBuilder()//
            .appendValue(ChronoField.YEAR_OF_ERA, 4, 19, SignStyle.NORMAL).appendLiteral("-")//
            .appendValue(ChronoField.MONTH_OF_YEAR, 2).appendLiteral("-")//
            .appendValue(ChronoField.DAY_OF_MONTH, 2)//
            .appendText(ChronoField.ERA, Map.of(0l, " BC", 1l, ""))//
            .toFormatter(Locale.ENGLISH);

    //NOTE: limitations given by using the date type in postgres
    private static final LocalDate MIN_VALUE = LocalDate.parse("-4714-11-24Z", inputFormatter);
    private static final LocalDate MAX_VALUE = LocalDate.parse("5874897-12-31Z", inputFormatter);


    protected DateDatatype()
    {
        super(xsdDateIri);
    }


    @Override
    public LiteralClass getBaseLiteralClass()
    {
        return genScalarDate;
    }


    @Override
    public LiteralClass getCanonicalLiteralClass()
    {
        return xsdScalarDate;
    }


    @Override
    public ResourceClass getResourceClass(Literal literal)
    {
        assert typeIri.equals(literal.getType());

        if(!isValidForm(literal.getValue()))
            return unsupportedType;

        if(!isCanonicalForm(literal.getValue()))
            return subtract(DateInZoneBaseClass.get(getZone(literal)), DateInZoneClass.get(getZone(literal))); //TODO use cache

        return DateInZoneClass.get(getZone(literal));
    }


    @Override
    public boolean isValidForm(String value)
    {
        if(!validFormPattern.matcher(value).matches())
            return false;

        value = getCollapsedForm(value).replaceFirst("(\\.[0-9]{6})[0-9]*", "$1");

        if(!value.matches(".*" + ZONE))
            value = value + "Z";

        LocalDate date = LocalDate.parse(value, inputFormatter);

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
        return getCollapsedForm(value).replaceFirst("[-+]00:00", "Z");
    }


    public static String getDate(Literal literal)
    {
        String value = Datatype.getCollapsedForm(literal.getValue()).replace("(\\.[0-9]{6})[0-9]*", "$1");

        if(!value.matches(".*" + TemporalDatatype.ZONE))
            value = value + "Z";

        LocalDate date = LocalDate.parse(value, inputFormatter);
        return date.format(outputFormatter);
    }


    public static int getZone(Literal literal)
    {
        String zone = Datatype.getCollapsedForm(literal.getValue()).replaceFirst(TemporalDatatype.DATE, "");

        if(zone.equals("Z"))
            return 0;

        if(zone.isEmpty())
            return Integer.MIN_VALUE;

        return TimeZone.getTimeZone("GMT" + zone).getRawOffset() / 1000;
    }
}
