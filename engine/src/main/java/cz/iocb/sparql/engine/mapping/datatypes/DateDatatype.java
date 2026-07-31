package cz.iocb.sparql.engine.mapping.datatypes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.unsupportedLiteral;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdScalarDate;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDateIri;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.time.temporal.ChronoField;
import java.util.Locale;
import java.util.regex.Pattern;
import cz.iocb.sparql.engine.mapping.classes.DateCompositeClass;
import cz.iocb.sparql.engine.mapping.classes.DateConstantZoneClass;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;
import cz.iocb.sparql.engine.rdf.Literal;



public class DateDatatype extends TemporalDatatype
{
    private static final Pattern validFormPattern = Pattern.compile(XSD_DATE_PATTERN);

    private static final DateTimeFormatter dateFormatter = new DateTimeFormatterBuilder()//
            .appendValue(ChronoField.YEAR, 4, 19, SignStyle.NORMAL).appendLiteral("-")//
            .appendValue(ChronoField.MONTH_OF_YEAR, 2).appendLiteral("-")//
            .appendValue(ChronoField.DAY_OF_MONTH, 2)//
            .appendOffset("+HH:MM", "Z")//
            .toFormatter(Locale.ENGLISH);

    //NOTE: limitations given by using the date type in postgres
    private static final LocalDate MIN_VALUE = LocalDate.parse("-4714-11-24Z", dateFormatter);
    private static final LocalDate MAX_VALUE = LocalDate.parse("5874898-01-01Z", dateFormatter);


    protected DateDatatype()
    {
        super(xsdDateIri);
    }


    @Override
    public LiteralClass getGeneralLiteralClass()
    {
        return xsdScalarDate;
    }


    @Override
    public LiteralClass getResourceClass(Literal literal)
    {
        assert typeIri.equals(literal.getType());

        if(!isValidForm(literal.getValue()))
            return unsupportedLiteral;

        return DateConstantZoneClass.get(DateCompositeClass.getZone(literal));
    }


    @Override
    public boolean isValidForm(String value)
    {
        if(!validFormPattern.matcher(value).matches())
            return false;

        value = getCollapsedForm(value).replaceFirst("(\\.[0-9]{6})[0-9]*", "$1");

        if(!value.matches(".*" + ZONE))
            value = value + "Z";

        LocalDate date = LocalDate.parse(value, dateFormatter);

        return date.compareTo(MAX_VALUE) < 0 && date.compareTo(MIN_VALUE) >= 0;
    }


    @Override
    public String getCanonicalLexicalForm(String value)
    {
        return getCollapsedForm(value).replaceFirst("[-+]00:00", "Z");
    }
}
