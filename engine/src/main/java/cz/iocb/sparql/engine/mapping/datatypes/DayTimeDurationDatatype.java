package cz.iocb.sparql.engine.mapping.datatypes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genDayTimeDuration;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.lexDayTimeDuration;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.unsupportedType;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDayTimeDuration;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDayTimeDurationIri;
import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.Literal;



/**
 * The xsd:dayTimeDuration datatype with microsecond precision, limited to the range of a signed 64-bit count of
 * microseconds.
 */
public final class DayTimeDurationDatatype extends Datatype
{
    /**
     * Microseconds in a day.
     */
    static private final long DAY_USEC = 24 * 60 * 60 * 1000000l;

    /**
     * Microseconds in an hour.
     */
    static private final long HOUR_USEC = 60 * 60 * 1000000l;

    /**
     * Microseconds in a minute.
     */
    static private final long MIN_USEC = 60 * 1000000l;

    /**
     * Microseconds in a second.
     */
    static private final long SEC_USEC = 1000000l;

    /**
     * Lexical form of xsd:dayTimeDuration with at most microsecond precision.
     */
    private static final Pattern validFormPattern = Pattern.compile(WS + "-?P((([0-9]+D)(T(([0-9]+H)([0-9]+M)?([0-9]+"
            + "(\\.[0-9]{1,6}0*)?S)?|([0-9]+M)([0-9]+(\\.[0-9]{1,6}0*)?S)?|([0-9]+(\\.[0-9]{1,6}0*)?S)))?)|(T(([0-9]+H)([0-9]+M)?"
            + "([0-9]+(\\.[0-9]{1,6}0*)?S)?|([0-9]+M)([0-9]+(\\.[0-9]{1,6}0*)?S)?|([0-9]+(\\.[0-9]{1,6}0*)?S))))" + WS);

    /**
     * Canonical lexical form: no zero components, normalised ranges, no trailing zeros in seconds.
     */
    private static final Pattern canonicalFormPattern = Pattern.compile("""
            (?:PT0S|-?P(?:[1-9][0-9]*D|(?:[1-9][0-9]*D)?\
            T(?:(?:[1-9]|1[0-9]|2[0-3])H(?:(?:[1-9]|[1-5][0-9])M)?(?:(?:0\\.[0-9]*[1-9]|(?:[1-9]|[1-5][0-9])\
            (?:\\.[0-9]*[1-9])?)S)?|(?:[1-9]|[1-5][0-9])M(?:(?:0\\.[0-9]*[1-9]|(?:[1-9]|[1-5][0-9])\
            (?:\\.[0-9]*[1-9])?)S)?|(?:0\\.[0-9]*[1-9]|(?:[1-9]|[1-5][0-9])(?:\\.[0-9]*[1-9])?)S)))""");

    /**
     * Pattern with named groups for the sign and the components of the duration.
     */
    private static final Pattern splitPattern = Pattern.compile("^" + WS + "(?<sign>-)?P((?<days>[0-9]+)D)?"
            + "(T((?<hours>[0-9]+)H)?((?<mins>[0-9]+)M)?((?<secs>[0-9]+(\\.[0-9]{1,6})?)[0-9]*S)?)?" + WS + "$");

    /**
     * Lowest representable duration in microseconds.
     */
    static private final BigDecimal MIN_VALUE = new BigDecimal(Long.MIN_VALUE);

    /**
     * Highest representable duration in microseconds.
     */
    static private final BigDecimal MAX_VALUE = new BigDecimal(Long.MAX_VALUE);


    /**
     * Creates the datatype.
     */
    public DayTimeDurationDatatype()
    {
        super(xsdDayTimeDurationIri);
    }


    @Override
    public LiteralClass getBaseLiteralClass()
    {
        return genDayTimeDuration;
    }


    @Override
    public LiteralClass getCanonicalLiteralClass()
    {
        return xsdDayTimeDuration;
    }


    @Override
    public ResourceClass getResourceClass(Literal literal)
    {
        assert typeIri.equals(literal.getType());

        if(!isValidForm(literal.getValue()))
            return unsupportedType;

        if(!isCanonicalForm(literal.getValue()))
            return lexDayTimeDuration;

        return xsdDayTimeDuration;
    }


    @Override
    public boolean isValidForm(String value)
    {
        if(!validFormPattern.matcher(value).matches())
            return false;

        BigDecimal num = parseValue(value);

        return num.compareTo(MIN_VALUE) >= 0 && num.compareTo(MAX_VALUE) <= 0;
    }


    @Override
    public boolean isCanonicalForm(String value)
    {
        assert isValidForm(value);

        return canonicalFormPattern.matcher(value).matches();
    }


    @Override
    public String getCanonicalLexicalForm(String value)
    {
        assert isValidForm(value);

        return getLexicalForm(parseValue(value).longValueExact());
    }


    /**
     * The canonical lexical form of the duration of the given length, as the extension prints it: the parts that are
     * not zero, the seconds with their fraction without trailing zeros, as in {@code -P1DT2H0.5S}, and {@code PT0S} for
     * the zero duration.
     *
     * @param value length of the duration in microseconds (negative for a negative duration)
     * @return the canonical lexical form of the duration
     */
    public static String getLexicalForm(long value)
    {
        if(value == 0)
            return "PT0S";

        StringBuilder builder = new StringBuilder();

        if(value < 0)
            builder.append('-');

        builder.append('P');

        // the parts are taken from the negative length, because the least duration has no positive counterpart
        long length = value > 0 ? -value : value;

        long days = -(length / DAY_USEC);
        long hours = -(length % DAY_USEC / HOUR_USEC);
        long mins = -(length % HOUR_USEC / MIN_USEC);
        long secs = -(length % MIN_USEC / SEC_USEC);
        long usecs = -(length % SEC_USEC);

        if(days > 0)
            builder.append(days).append('D');

        if(hours > 0 || mins > 0 || secs > 0 || usecs > 0)
            builder.append('T');

        if(hours > 0)
            builder.append(hours).append('H');

        if(mins > 0)
            builder.append(mins).append('M');

        if(secs > 0 || usecs > 0)
        {
            builder.append(secs);

            if(usecs > 0)
            {
                // the six digits of the fraction, with the leading zeros and without the trailing ones
                String digits = Long.toString(SEC_USEC + usecs).substring(1);
                int end = digits.length();

                while(digits.charAt(end - 1) == '0')
                    end--;

                builder.append('.').append(digits, 0, end);
            }

            builder.append('S');
        }

        return builder.toString();
    }


    /**
     * Length of the duration in microseconds (negative for negative durations).
     *
     * @param value the value
     * @return length of the duration in microseconds (negative for negative durations)
     */
    public static BigDecimal parseValue(String value)
    {
        Matcher match = splitPattern.matcher(value);

        if(!match.matches())
            throw new IllegalArgumentException();

        BigDecimal result = BigDecimal.ZERO;

        if(match.group("days") != null && !match.group("days").isEmpty())
            result = result.add(new BigDecimal(match.group("days")).multiply(BigDecimal.valueOf(DAY_USEC)));

        if(match.group("hours") != null && !match.group("hours").isEmpty())
            result = result.add(new BigDecimal(match.group("hours")).multiply(BigDecimal.valueOf(HOUR_USEC)));

        if(match.group("mins") != null && !match.group("mins").isEmpty())
            result = result.add(new BigDecimal(match.group("mins")).multiply(BigDecimal.valueOf(MIN_USEC)));

        if(match.group("secs") != null && !match.group("secs").isEmpty())
            result = result.add(new BigDecimal(match.group("secs")).multiply(BigDecimal.valueOf(SEC_USEC)));

        if(match.group("sign") != null && !match.group("sign").isEmpty())
            result = result.negate();

        return result;
    }
}
