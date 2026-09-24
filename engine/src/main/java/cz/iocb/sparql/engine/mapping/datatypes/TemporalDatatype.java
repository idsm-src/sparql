package cz.iocb.sparql.engine.mapping.datatypes;

import cz.iocb.sparql.engine.rdf.Iri;



/**
 * Common base of xsd:date and xsd:dateTime holding the regular expressions of their lexical forms (with calendar
 * validation of month lengths and leap years).
 */
public abstract sealed class TemporalDatatype extends Datatype permits DateTimeDatatype, DateDatatype
{
    /**
     * Year: at least four digits, no leading zeros beyond four.
     */
    protected static final String YEAR = "([1-9][0-9]{3,}|0[0-9]{3})";

    /**
     * Leap year divisible by 400.
     */
    protected static final String LEAP_YEAR_DIV100 = "(([1-9][0-9]*)?([02468][048]|[13579][26])00)";

    /**
     * Leap year divisible by 4 but not by 100.
     */
    protected static final String LEAP_YEAR_DIV4 = "(([0-9]{2}|[1-9][0-9]{2,})(0[48]|[2468][048]|[13579][26]))";

    /**
     * Any leap year.
     */
    protected static final String LEAP_YEAR = "(" + LEAP_YEAR_DIV4 + "|" + LEAP_YEAR_DIV100 + ")";

    /**
     * Month-day of a 30-day month.
     */
    protected static final String SHORT_MONTH = "(04|06|09|11)-(0[1-9]|[12][0-9]|30)";

    /**
     * Month-day of a 31-day month.
     */
    protected static final String LONG_MONTH = "(01|03|05|07|08|10|12)-(0[1-9]|[12][0-9]|3[01])";

    /**
     * Month-day of February excluding the 29th.
     */
    protected static final String FEBRUARY = "02-(0[1-9]|1[0-9]|2[0-8])";

    /**
     * Any month-day valid in every year.
     */
    protected static final String MONTH_DAY = "(" + LONG_MONTH + "|" + SHORT_MONTH + "|" + FEBRUARY + ")";

    /**
     * Time of day with at most microsecond precision, allowing {@code 24:00:00}.
     */
    public static final String TIME = "(([01][0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9](\\.[0-9]{1,6}0*)?|(24:00:00(\\.0+)?))";

    /**
     * Date with calendar validation of the day, including 29 February in leap years.
     */
    public static final String DATE = "((-?" + YEAR + "-" + MONTH_DAY + ")|(-?" + LEAP_YEAR + "-02-29))";

    /**
     * Date and time separated by {@code T}.
     */
    public static final String DATETIME = DATE + "T" + TIME;

    /**
     * Timezone: {@code Z} or an offset up to 14 hours.
     */
    public static final String ZONE = "(Z|(\\+|-)((0[0-9]|1[0-3]):[0-5][0-9]|14:00))";

    /**
     * Full lexical form of xsd:date with optional timezone and surrounding whitespace.
     */
    public static final String XSD_DATE_PATTERN = WS + DATE + ZONE + "?" + WS;

    /**
     * Full lexical form of xsd:dateTime with optional timezone and surrounding whitespace.
     */
    public static final String XSD_DATETIME_PATTERN = WS + DATETIME + ZONE + "?" + WS;


    /**
     * Creates the datatype.
     *
     * @param typeIri the datatype IRI
     */
    protected TemporalDatatype(Iri typeIri)
    {
        super(typeIri);
    }
}
