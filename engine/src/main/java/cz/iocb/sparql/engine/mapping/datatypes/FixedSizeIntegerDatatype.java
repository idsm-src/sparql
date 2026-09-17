package cz.iocb.sparql.engine.mapping.datatypes;

import static java.lang.String.format;
import java.math.BigInteger;
import java.util.regex.Pattern;
import cz.iocb.sparql.engine.rdf.Iri;



public abstract sealed class FixedSizeIntegerDatatype extends GenericIntegerDataType
        permits ShortDatatype, IntDatatype, LongDatatype
{
    protected FixedSizeIntegerDatatype(Iri typeIri, String max)
    {
        super(typeIri, generatePattern(max));
    }


    private static Pattern generatePattern(String max)
    {
        int limit = max.length() - 1;

        StringBuilder builder = new StringBuilder();

        builder.append(WS).append("(");
        builder.append(format("-0*%s|", new BigInteger(max).add(BigInteger.ONE).toString()));
        builder.append(format("[-+]?0*(0|[1-9][0-9]{0,%d}|", limit - 1));
        builder.append(format("[1-%d][0-9]{%d}|", max.charAt(0) - '0' - 1, limit));

        for(int i = 1; i < limit; i++)
            if(max.charAt(i) > '0')
                builder.append(format("%s[0-%d][0-9]{%d}|", max.substring(0, i), max.charAt(i) - '0' - 1, limit - i));

        builder.append(format("%s[0-%d])", max.substring(0, limit), max.charAt(limit) - '0'));
        builder.append(")").append(WS);

        return Pattern.compile(builder.toString());
    }
}
