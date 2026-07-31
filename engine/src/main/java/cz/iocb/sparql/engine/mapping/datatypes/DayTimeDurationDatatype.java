package cz.iocb.sparql.engine.mapping.datatypes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.unsupportedLiteral;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDayTimeDuration;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDayTimeDurationIri;
import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;
import cz.iocb.sparql.engine.rdf.Literal;



public class DayTimeDurationDatatype extends Datatype
{
    static private final BigDecimal DAY_USEC = new BigDecimal(24 * 60 * 60 * 1000000l);
    static private final BigDecimal HOUR_USEC = new BigDecimal(60 * 60 * 1000000l);
    static private final BigDecimal MIN_USEC = new BigDecimal(60 * 1000000l);
    static private final BigDecimal SEC_USEC = new BigDecimal(1000000l);

    static final Pattern validFormPattern = Pattern.compile(WS + "-?P((([0-9]+D)(T(([0-9]+H)([0-9]+M)?([0-9]+"
            + "(\\.[0-9]+)?S)?|([0-9]+M)([0-9]+(\\.[0-9]+)?S)?|([0-9]+(\\.[0-9]+)?S)))?)|(T(([0-9]+H)([0-9]+M)?"
            + "([0-9]+(\\.[0-9]+)?S)?|([0-9]+M)([0-9]+(\\.[0-9]+)?S)?|([0-9]+(\\.[0-9]+)?S))))" + WS);

    //private static final Pattern canonicalFormPattern = Pattern.compile("""
    //        (?:PT0S|-?P(?:[1-9][0-9]*D|(?:[1-9][0-9]*D)?\
    //        T(?:(?:[1-9]|1[0-9]|2[0-3])H(?:(?:[1-9]|[1-5][0-9])M)?(?:(?:0\\.[0-9]*[1-9]|(?:[1-9]|[1-5][0-9])\
    //        (?:\\.[0-9]*[1-9])?)S)?|(?:[1-9]|[1-5][0-9])M(?:(?:0\\.[0-9]*[1-9]|(?:[1-9]|[1-5][0-9])\
    //        (?:\\.[0-9]*[1-9])?)S)?|(?:0\\.[0-9]*[1-9]|(?:[1-9]|[1-5][0-9])(?:\\.[0-9]*[1-9])?)S)))""");

    private static final Pattern splitPattern = Pattern.compile("^" + WS + "(?<sign>-)?P((?<days>[0-9]+)D)?"
            + "(T((?<hours>[0-9]+)H)?((?<mins>[0-9]+)M)?((?<secs>[0-9]+(\\.[0-9]{1,6})?)[0-9]*S)?)?" + WS + "$");

    static private final BigDecimal MIN_VALUE = new BigDecimal(Long.MIN_VALUE);
    static private final BigDecimal MAX_VALUE = new BigDecimal(Long.MAX_VALUE);


    public DayTimeDurationDatatype()
    {
        super(xsdDayTimeDurationIri);
    }


    @Override
    public LiteralClass getGeneralLiteralClass()
    {
        return xsdDayTimeDuration;
    }


    @Override
    public LiteralClass getResourceClass(Literal literal)
    {
        assert typeIri.equals(literal.getType());

        if(!isValidForm(literal.getValue()))
            return unsupportedLiteral;

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
    public String getCanonicalLexicalForm(String value)
    {
        assert isValidForm(value);

        BigDecimal num = parseValue(value);

        if(num.equals(BigDecimal.ZERO))
            return "PT0S";

        StringBuilder builder = new StringBuilder();

        if(num.compareTo(BigDecimal.ZERO) < 0)
            builder.append("-1");

        builder.append("P");

        num = num.abs();

        BigDecimal days = num.divideToIntegralValue(DAY_USEC);
        BigDecimal hours = num.remainder(DAY_USEC).divideToIntegralValue(HOUR_USEC);
        BigDecimal mins = num.remainder(HOUR_USEC).divideToIntegralValue(MIN_USEC);
        BigDecimal secs = num.remainder(MIN_USEC).divide(SEC_USEC).stripTrailingZeros();

        if(!days.equals(BigDecimal.ZERO))
            builder.append(days).append("D");

        if(!hours.equals(BigDecimal.ZERO) || !mins.equals(BigDecimal.ZERO) || !secs.equals(BigDecimal.ZERO))
            builder.append("T");

        if(!hours.equals(BigDecimal.ZERO))
            builder.append(hours).append("H");

        if(!mins.equals(BigDecimal.ZERO))
            builder.append(mins).append("M");

        if(!secs.equals(BigDecimal.ZERO))
            builder.append(secs).append("S");

        return builder.toString();
    }


    private static BigDecimal parseValue(String value)
    {
        Matcher match = splitPattern.matcher(value);

        if(!match.matches())
            throw new IllegalArgumentException();

        BigDecimal result = BigDecimal.ZERO;

        if(!match.group("days").isEmpty())
            result = result.add(new BigDecimal(match.group("days")).multiply(DAY_USEC));

        if(!match.group("hours").isEmpty())
            result = result.add(new BigDecimal(match.group("hours")).multiply(HOUR_USEC));

        if(!match.group("minutes").isEmpty())
            result = result.add(new BigDecimal(match.group("minutes")).multiply(MIN_USEC));

        if(!match.group("secs").isEmpty())
            result = result.add(new BigDecimal(match.group("secs")).multiply(SEC_USEC));

        if(!match.group("sign").isEmpty())
            result = result.negate();

        return result;
    }
}
