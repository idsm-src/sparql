package cz.iocb.sparql.engine.mapping.datatypes;

import java.util.regex.Pattern;
import cz.iocb.sparql.engine.rdf.Iri;



public abstract sealed class VariableSizeIntegerDataType extends GenericIntegerDataType permits IntegerDatatype
{
    protected enum Variant
    {
        FULL(true, true, true),
        POSITIVE(false, true, false),
        NEGATIVE(false, false, true),
        NONPOSITIVE(true, false, true),
        NONNEGATIVE(true, true, false);

        boolean zero;
        boolean plus;
        boolean minus;

        Variant(boolean zero, boolean plus, boolean minus)
        {
            this.zero = zero;
            this.plus = plus;
            this.minus = minus;
        }
    }


    protected VariableSizeIntegerDataType(Iri typeIri, Variant variant)
    {
        super(typeIri, generatePattern(variant));
    }


    private static Pattern generatePattern(Variant variant)
    {
        StringBuilder builder = new StringBuilder();

        builder.append(WS);
        builder.append("(");

        if(variant.plus && variant.minus)
            builder.append("[-+]?");
        else if(variant.plus && !variant.minus)
            builder.append("\\+?");
        else if(!variant.plus && variant.minus)
            builder.append("-");

        builder.append("0*");

        //NOTE: postgres is able to express integers up to 131072 digits
        builder.append("[0-9]{1,131072}");

        if(!variant.zero)
            builder.append(")");
        else
            builder.append("|[-+]?0+)");

        builder.append(WS);

        return Pattern.compile(builder.toString());
    }
}
