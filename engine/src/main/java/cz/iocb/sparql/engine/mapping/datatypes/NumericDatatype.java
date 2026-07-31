package cz.iocb.sparql.engine.mapping.datatypes;

import static java.lang.String.format;
import java.math.BigInteger;
import java.util.regex.Pattern;
import cz.iocb.sparql.engine.rdf.Iri;



public abstract class NumericDatatype extends Datatype
{
    protected NumericDatatype(Iri typeIri)
    {
        super(typeIri);
    }


    @Override
    public String getCanonicalLexicalForm(String value)
    {
        assert isValidForm(value);

        return getCollapsedForm(value).replaceFirst("^(?<prefix>[+-]?)\\.", "${prefix}0.")
                .replaceFirst(
                        "^(?:(?<minus>-)(?=.*[1-9])|[+-])?0*(?<integer>[1-9][0-9]*|0)"
                                + "(?:(?<dot>\\.)(?<fraction>[0-9]*[1-9])0*|\\.0*)?$",
                        "${minus}${integer}${dot}${fraction}");
    }


    protected static Pattern generateDecimalPattern(boolean canonical)
    {
        StringBuilder builder = new StringBuilder();

        if(canonical)
            return Pattern.compile("-?([1-9][0-9]{0,131071}(\\.[0-9]{0,16382}[1-9])?|0\\.[0-9]{0,16382}[1-9])|0");

        builder.append("[\\t\\n\\r ]*");
        builder.append("[-+]?((0*[1-9][0-9]{0,131071}|0+)(\\.([0-9]{0,16382}[1-9])?0*)?|\\.([0-9]{0,16382}[1-9]|0)0*)");
        builder.append("[\\t\\n\\r ]*");

        return Pattern.compile(builder.toString());
    }


    protected static Pattern generateIntegerPattern(boolean canonical)
    {
        StringBuilder builder = new StringBuilder();

        if(!canonical)
            builder.append("[\\t\\n\\r ]*");

        builder.append("(");

        if(canonical)
            builder.append("-?");
        else
            builder.append("[-+]?0*");

        //NOTE: postgres is able to express integers up to 131072 digits
        builder.append("[1-9][0-9]{0,131071}");

        if(!canonical)
            builder.append("|[-+]?0+)");
        else
            builder.append("|0)");

        if(!canonical)
            builder.append("[\\t\\n\\r ]*");

        return Pattern.compile(builder.toString());
    }





    protected static Pattern generateIntegerPattern(boolean zero, boolean signed, boolean unsigned, boolean canonical)
    {
        if(!signed && !unsigned || !zero && signed && unsigned)
            throw new IllegalArgumentException();

        StringBuilder builder = new StringBuilder();

        if(!canonical)
            builder.append("[\\t\\n\\r ]*");

        builder.append("(");

        if(signed && unsigned && canonical)
            builder.append("-?");
        else if(signed && unsigned && !canonical)
            builder.append("[-+]?");
        else if(signed && !unsigned && !canonical)
            builder.append("\\+?");
        else if(!signed && unsigned)
            builder.append("-");

        if(!canonical)
            builder.append("0*");

        //NOTE: postgres is able to express integers up to 131072 digits
        builder.append("[1-9][0-9]{0,131071}");

        if(!zero)
            builder.append(")");
        else if(!canonical)
            builder.append("|[-+]?0+)");
        else
            builder.append("|0)");

        if(!canonical)
            builder.append("[\\t\\n\\r ]*");

        return Pattern.compile(builder.toString());
    }


    protected static Pattern generateFixedSizePattern(String max, boolean unsigned, boolean canonical)
    {
        String whites = canonical ? "" : "[\\t\\n\\r ]*";
        String zeros = canonical ? "" : "0*";
        String signs = canonical ? (unsigned ? "" : "-") : (unsigned ? "-" : "[-+]");

        StringBuilder builder = new StringBuilder();

        builder.append(whites).append("(");

        if(!unsigned)
            builder.append(format("-%s%s|", zeros, new BigInteger(max).add(BigInteger.ONE).toString()));

        builder.append(format("%s?%s(0|[1-9][0-9]{0,%d}|", signs, zeros, max.length() - 2));

        builder.append(format("[1-%d][0-9]{%d}|", max.charAt(0) - '0' - 1, max.length() - 1));

        for(int i = 1; i < max.length() - 1; i++)
            if(max.charAt(i) > '0')
                builder.append(format("%s[0-%d][0-9]{%d}|", max.substring(0, i), max.charAt(i) - '0' - 1,
                        max.length() - i - 1));

        builder.append(format("%s[0-%d])", max.substring(0, max.length() - 1), max.charAt(max.length() - 1) - '0'));

        builder.append(")").append(whites);

        return Pattern.compile(builder.toString());
    }
}
