package cz.iocb.chemweb.server.sparql.parser.model.expression;

import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdBooleanIri;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdDateIri;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdDateTimeIri;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdDayTimeDurationIri;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdDecimalIri;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdDoubleIri;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdFloatIri;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdIntIri;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdIntegerIri;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdLongIri;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdShortIri;
import static cz.iocb.chemweb.server.sparql.parser.BuiltinTypes.xsdStringIri;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import cz.iocb.chemweb.server.sparql.parser.BaseComplexNode;
import cz.iocb.chemweb.server.sparql.parser.BuiltinTypes;
import cz.iocb.chemweb.server.sparql.parser.ElementVisitor;
import cz.iocb.chemweb.server.sparql.parser.StreamUtils;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;



/**
 * Represents a literal value ({@link #getStringValue}) that has a type ({@link #getTypeIri}) and can have a language
 * tag ({@link #getLanguageTag}). This includes the shorthand forms for numeric and boolean literals.
 *
 * <p>
 * For supported literal types, a converted value ({@link #getValue}), along with its Java type ({@link #getJavaClass})
 * is also provided.
 *
 * <p>
 * Corresponds to the following rules in the SPARQL grammar:
 * <ul>
 * <li>[129] RDFLiteral
 * <li>[130] NumericLiteral
 * <li>[134] BooleanLiteral
 * </ul>
 */
public class Literal extends BaseComplexNode implements Expression, Node
{
    private final String stringValue;
    private final Object value;
    private final String languageTag;
    private final IRI type;
    private final boolean isTypeSupported;
    private final boolean isSimple;


    public Literal(String value)
    {
        this.stringValue = value;
        this.value = value;
        this.languageTag = null;
        this.type = xsdStringIri;
        this.isTypeSupported = true;
        this.isSimple = true;
    }


    public Literal(String value, String languageTag)
    {
        this.stringValue = value;
        this.value = Type.unescape(value);
        this.languageTag = languageTag.toLowerCase();
        this.type = BuiltinTypes.rdfLangStringIri;
        this.isTypeSupported = true;
        this.isSimple = false;
    }


    public Literal(String value, IRI typeIri)
    {
        this.stringValue = value;
        this.languageTag = null;
        this.type = typeIri;
        this.isSimple = false;

        Type<?> typeClass = Type.getType(typeIri);

        if(typeClass == null)
        {
            this.value = null;
            this.isTypeSupported = false;
        }
        else
        {
            this.value = typeClass.getParser().apply(value);
            this.isTypeSupported = true;
        }
    }


    public String getStringValue()
    {
        return stringValue;
    }


    public Object getValue()
    {
        return value;
    }


    public String getLanguageTag()
    {
        return languageTag;
    }


    public IRI getTypeIri()
    {
        return type;
    }


    public boolean isTypeSupported()
    {
        return isTypeSupported;
    }


    public boolean isSimple()
    {
        return isSimple;
    }


    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }


    @Override
    public boolean equals(Object obj)
    {
        if(!(obj instanceof Literal))
            return false;

        Literal literal = (Literal) obj;

        if(!type.equals(literal.type))
            return false;

        if(value != null && !value.equals(literal.value) || value == null && literal.value != null)
            return false;

        if(value == null && !stringValue.equals(literal.stringValue))
            return false;

        if(languageTag != null && !languageTag.equals(literal.languageTag)
                || languageTag == null && literal.languageTag != null)
            return false;

        return true;
    }
}


/**
 * Helper type for mapping between XSD types ({@link #getIri}) and Java types ( {@link #getJavaClass}). Also provides
 * parser function from string to the Java type ({@link #getParser}).
 */
class Type<T>
{
    private static List<Type<?>> types = new ArrayList<Type<?>>();


    private static final DateTimeFormatter dateFormatter = new DateTimeFormatterBuilder()//
            .appendValue(ChronoField.YEAR, 4, 19, SignStyle.NORMAL).appendLiteral("-")//
            .appendValue(ChronoField.MONTH_OF_YEAR, 2).appendLiteral("-")//
            .appendValue(ChronoField.DAY_OF_MONTH, 2)//
            .appendOffset("+HH:MM", "Z")//
            .toFormatter();

    private static final LocalDate maxDate = LocalDate.parse("5874898-01-01Z", dateFormatter);
    private static final LocalDate minDate = LocalDate.parse("-4714-11-24Z", dateFormatter);

    private static final String dateWithoutZonePattern = "-?([1-9][0-9]{3,}|0[0-9]{3})-(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01])";
    private static final String timezonePattern = "(Z|(\\+|-)((0[0-9]|1[0-3]):[0-5][0-9]|14:00))?";
    private static final String datePattern = dateWithoutZonePattern + timezonePattern;


    private static final DateTimeFormatter dateTimeFormatter = new DateTimeFormatterBuilder()//
            .appendValue(ChronoField.YEAR, 4, 19, SignStyle.NORMAL).appendLiteral("-")//
            .appendValue(ChronoField.MONTH_OF_YEAR, 2).appendLiteral("-")//
            .appendValue(ChronoField.DAY_OF_MONTH, 2).appendLiteral("T")//
            .appendValue(ChronoField.HOUR_OF_DAY, 2).appendLiteral(":")//
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2).appendLiteral(":")//
            .appendValue(ChronoField.SECOND_OF_MINUTE, 2)//
            .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)//
            .optionalStart()//
            .appendOffset("+HH:MM", "Z")//
            .optionalEnd()//
            .toFormatter();

    private static final OffsetDateTime maxDateTime = OffsetDateTime.parse("294277-01-01T00:00:00Z", dateTimeFormatter);
    private static final OffsetDateTime minDateTime = OffsetDateTime.parse("-4714-11-24T00:00:00Z", dateTimeFormatter);

    private static final String timeWithoutZonePattern = "(([01][0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9](\\.[0-9]+)?|(24:00:00(\\.0+)?))";
    private static final String dateTimeWithoutZonePattern = dateWithoutZonePattern + "T" + timeWithoutZonePattern;
    private static final String dateTimePattern = dateTimeWithoutZonePattern + timezonePattern;


    private final IRI iri;
    private final Class<T> javaClass;
    private final Function<String, T> parser;


    static
    {
        types.add(new Type<>(xsdBooleanIri, Boolean.class, s -> parseBool(s.trim())));
        types.add(new Type<>(xsdShortIri, Short.class, s -> new Short(s.trim())));
        types.add(new Type<>(xsdIntIri, Integer.class, s -> new Integer(s.trim())));
        types.add(new Type<>(xsdLongIri, Long.class, s -> new Long(s.trim())));
        types.add(new Type<>(xsdFloatIri, Float.class, s -> parseFloat(s.trim())));
        types.add(new Type<>(xsdDoubleIri, Double.class, s -> parseDouble(s.trim())));
        types.add(new Type<>(xsdIntegerIri, BigInteger.class, s -> new BigInteger(s.trim())));
        types.add(new Type<>(xsdDecimalIri, BigDecimal.class, s -> parseDecimal(s.trim())));
        types.add(new Type<>(xsdDateTimeIri, Object.class, s -> parseDateTime(s.trim())));
        types.add(new Type<>(xsdDateIri, String.class, s -> parseDate(s.trim())));
        types.add(new Type<>(xsdDayTimeDurationIri, Long.class, s -> parseDayTimeDuration(s.trim())));
        types.add(new Type<>(xsdStringIri, String.class, s -> unescape(s)));
    }


    private static Boolean parseBool(String s)
    {
        if(s.equalsIgnoreCase("true") || s.equals("1"))
            return true;
        if(s.equalsIgnoreCase("false") || s.equals("0"))
            return false;

        throw new IllegalArgumentException();
    }


    private static Float parseFloat(String s)
    {
        // the differences between the format of new Float(s) and xsd:float are  hexadecimal literals and the format
        // of infinity

        if(s.contains("0x") || s.contains("0X"))
            throw new IllegalArgumentException();

        s = s.replace("INF", "Infinity");

        return new Float(s);
    }


    private static Double parseDouble(String s)
    {
        // the differences between the format of new Double(s) and xsd:double are hexadecimal literals and the format
        // of infinity

        if(s.contains("0x") || s.contains("0X"))
            throw new IllegalArgumentException();

        s = s.replace("INF", "Infinity");

        return new Double(s);
    }


    private static BigDecimal parseDecimal(String s)
    {
        // the format of new BigDecimal(s) accepts exponents, which xsd:decimal doesn't

        if(s.contains("e") || s.contains("E"))
            throw new IllegalArgumentException();

        return new BigDecimal(s);
    }


    private static Object parseDateTime(String s)
    {
        String value = s;

        if(!value.matches(dateTimePattern))
            throw new IllegalArgumentException();

        // truncate to microseconds
        value = value.replace("(\\.[0-9]{6})[0-9]*", "$1");

        if(value.matches(dateTimeWithoutZonePattern))
        {
            LocalDateTime date = LocalDateTime.parse(value, dateTimeFormatter);

            if(date.compareTo(maxDateTime.toLocalDateTime()) < 0 && date.compareTo(minDateTime.toLocalDateTime()) >= 0)
                return date;
        }
        else
        {
            OffsetDateTime date = OffsetDateTime.parse(value, dateTimeFormatter);

            if(date.compareTo(maxDateTime) < 0 && date.compareTo(minDateTime) >= 0)
                return date;
        }

        return null;
    }


    private static String parseDate(String s)
    {
        String value = s;

        if(!value.matches(datePattern))
            throw new IllegalArgumentException();

        if(value.matches(dateWithoutZonePattern))
            value = value + "Z";

        LocalDate date = LocalDate.parse(value, dateFormatter);

        if(date.compareTo(maxDate) >= 0 || date.compareTo(minDate) < 0)
            return null;

        String canonical = s.replaceAll("[+-]00:00$", "Z");

        return canonical;
    }


    private static Long parseDayTimeDuration(String s)
    {
        String dayTimeDurationPattern = "-?P((([0-9]+D)(T(([0-9]+H)([0-9]+M)?([0-9]+(\\.[0-9]+)?S)?|([0-9]+M)([0-9]+(\\.[0-9]+)?S)?|([0-9]+(\\.[0-9]+)?S)))?)|(T(([0-9]+H)([0-9]+M)?([0-9]+(\\.[0-9]+)?S)?|([0-9]+M)([0-9]+(\\.[0-9]+)?S)?|([0-9]+(\\.[0-9]+)?S))))";
        String dayTimeDurationSplitPattern = "^-?P(([0-9]+)D)?(T(([0-9]+)H)?(([0-9]+)M)?(([0-9]+(\\.[0-9]{1,6})?)[0-9]*S)?)?$";

        if(!s.matches(dayTimeDurationPattern))
            throw new IllegalArgumentException();

        String[] parts = s.replaceFirst(dayTimeDurationSplitPattern, "0$2#0$5#0$7#0$9").split("#");

        BigDecimal daysUsec = new BigDecimal(parts[0]).multiply(new BigDecimal(24 * 60 * 60 * 1000000l));
        BigDecimal hoursUsec = new BigDecimal(parts[1]).multiply(new BigDecimal(60 * 60 * 1000000l));
        BigDecimal minutesUsec = new BigDecimal(parts[2]).multiply(new BigDecimal(60 * 1000000l));
        BigDecimal secondsUsec = new BigDecimal(parts[3]).multiply(new BigDecimal(1000000l));

        BigDecimal result = daysUsec.add(hoursUsec).add(minutesUsec).add(secondsUsec);

        if(s.charAt(0) == '-')
            result = result.multiply(new BigDecimal(-1));

        try
        {
            return result.longValueExact();
        }
        catch(ArithmeticException e)
        {
            return null;
        }
    }


    static String unescape(String s)
    {
        return s.replace("\\t", "\t").replace("\\n", "\n").replace("\\r", "\r").replace("\\b", "\b")
                .replace("\\f", "\f").replace("\\\"", "\"").replace("\\'", "\'").replace("\\\\", "\\");
    }


    /**
     * Finds {@link Type} for the given XSD type. If no corresponding type is found, null is returned.
     */
    public static Type<?> getType(IRI iri)
    {
        Optional<Type<?>> type = types.stream().filter(t -> Objects.equals(t.getIri(), iri))
                .collect(StreamUtils.singleCollector());

        return type.orElse(null);
    }


    private Type(IRI iri, Class<T> javaClass, Function<String, T> parser)
    {
        this.iri = iri;
        this.javaClass = javaClass;
        this.parser = parser;
    }


    public IRI getIri()
    {
        return iri;
    }


    public Class<T> getJavaClass()
    {
        return javaClass;
    }


    public Function<String, T> getParser()
    {
        return this::parseOrNull;
    }


    /**
     * Tries to parse the given input. If that fails, returns {@code null}.
     */
    private T parseOrNull(String input)
    {
        try
        {
            return parser.apply(input);
        }
        catch(Exception ex)
        {
            return null;
        }
    }
}
