package cz.iocb.sparql.engine.mapping.datatypes;

import cz.iocb.sparql.engine.rdf.Iri;



public abstract class TemporalDatatype extends Datatype
{
    protected static final String YEAR = "([1-9][0-9]{3,}|0[0-9]{3})";
    protected static final String LEAP_YEAR_DIV100 = "(([1-9][0-9]*)?([02468][048]|[13579][26])00)";
    protected static final String LEAP_YEAR_DIV4 = "(([0-9]{2}|[1-9][0-9]{2,})(0[48]|[2468][048]|[13579][26]))";
    protected static final String LEAP_YEAR = "(" + LEAP_YEAR_DIV4 + "|" + LEAP_YEAR_DIV100 + ")";

    protected static final String SHORT_MONTH = "(04|06|09|11)-(0[1-9]|[12][0-9]|30)";
    protected static final String LONG_MONTH = "(01|03|05|07|08|10|12)-(0[1-9]|[12][0-9]|3[01])";
    protected static final String FEBRUARY = "02-(0[1-9]|1[0-9]|2[0-8])";
    protected static final String MONTH_DAY = "(" + LONG_MONTH + "|" + SHORT_MONTH + "|" + FEBRUARY + ")";

    public static final String TIME = "(([01][0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9](\\.[0-9]+)?|(24:00:00(\\.0+)?))";
    public static final String DATE = "((-?" + YEAR + "-" + MONTH_DAY + ")|(-?" + LEAP_YEAR + "-02-29))";
    public static final String ZONE = "(Z|(\\+|-)((0[0-9]|1[0-3]):[0-5][0-9]|14:00))";

    public static final String XSD_DATE_PATTERN = WS + DATE + ZONE + "?" + WS;
    public static final String XSD_DATETIME_PATTERN = WS + DATE + "T" + TIME + ZONE + "?" + WS;


    protected TemporalDatatype(Iri typeIri)
    {
        super(typeIri);
    }
}
