package cz.iocb.sparql.engine.mapping.datatypes;

import static java.lang.String.format;
import java.math.BigInteger;
import java.util.regex.Pattern;
import cz.iocb.sparql.engine.rdf.Iri;



public abstract sealed class FixedSizeIntegerDatatype extends GenericIntegerDataType
        permits ByteDatatype, UnsignedByteDatatype, ShortDatatype, UnsignedShortDatatype, IntDatatype,
        UnsignedIntDatatype, LongDatatype, UnsignedLongDatatype
{
    protected FixedSizeIntegerDatatype(Iri typeIri, String min, String max)
    {
        super(typeIri, generatePattern(new BigInteger(min), new BigInteger(max)));
    }


    private static Pattern generatePattern(BigInteger min, BigInteger max)
    {
        assert min.signum() <= 0 && max.signum() >= 0;

        StringBuilder builder = new StringBuilder();

        builder.append(WS).append("(");
        builder.append("\\+?0*(").append(generateRange(max)).append(")");
        builder.append("|-0*(").append(generateRange(min.negate())).append(")");
        builder.append(")").append(WS);

        return Pattern.compile(builder.toString());
    }


    /**
     * Generates alternatives matching the canonical forms of all integers from zero up to the given limit.
     */
    private static String generateRange(BigInteger limit)
    {
        String digits = limit.toString();
        int last = digits.length() - 1;

        StringBuilder builder = new StringBuilder("0");

        if(limit.signum() == 0)
            return builder.toString();

        // numbers having fewer digits
        if(last == 1)
            builder.append("|[1-9]");
        else if(last > 1)
            builder.append(format("|[1-9][0-9]{0,%d}", last - 1));

        // numbers having the same number of digits but a smaller leading digit
        if(digits.charAt(0) > '1')
            builder.append(format("|[1-%c][0-9]{%d}", digits.charAt(0) - 1, last));

        // numbers sharing a prefix with the limit and having a smaller digit at the first differing position
        for(int i = 1; i < last; i++)
            if(digits.charAt(i) > '0')
                builder.append(format("|%s[0-%c][0-9]{%d}", digits.substring(0, i), digits.charAt(i) - 1, last - i));

        // numbers sharing all but the last digit with the limit
        builder.append(format("|%s[0-%c]", digits.substring(0, last), digits.charAt(last)));

        return builder.toString();
    }
}
