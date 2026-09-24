package cz.iocb.sparql.engine.mapping.datatypes;

import java.util.regex.Pattern;
import cz.iocb.sparql.engine.rdf.Iri;



/**
 * Unbounded integer datatypes (xsd:integer and its sign-restricted derivatives), limited only by the precision of
 * PostgreSQL {@code numeric}.
 */
public abstract sealed class VariableSizeIntegerDataType extends GenericIntegerDataType permits IntegerDatatype,
        NonPositiveIntegerDatatype, NegativeIntegerDatatype, NonNegativeIntegerDatatype, PositiveIntegerDatatype
{
    /**
     * Which signs, and whether zero, the datatype admits.
     */
    protected enum Variant
    {
        /**
         * Any integer.
         */
        FULL(true, true, true),

        /**
         * Positive integers.
         */
        POSITIVE(false, true, false),

        /**
         * Negative integers.
         */
        NEGATIVE(false, false, true),

        /**
         * Zero and negative integers.
         */
        NONPOSITIVE(true, false, true),

        /**
         * Zero and positive integers.
         */
        NONNEGATIVE(true, true, false);

        /**
         * Whether zero is admitted.
         */
        boolean zero;

        /**
         * Whether positive numbers are admitted.
         */
        boolean plus;

        /**
         * Whether negative numbers are admitted.
         */
        boolean minus;

        /**
         * Creates the variant.
         *
         * @param zero whether zero is admitted
         * @param plus whether positive numbers are admitted
         * @param minus whether negative numbers are admitted
         */
        Variant(boolean zero, boolean plus, boolean minus)
        {
            this.zero = zero;
            this.plus = plus;
            this.minus = minus;
        }
    }


    /**
     * Creates the datatype admitting the signs of the variant.
     *
     * @param typeIri the datatype IRI
     * @param variant the sign variant
     */
    protected VariableSizeIntegerDataType(Iri typeIri, Variant variant)
    {
        super(typeIri, generatePattern(variant));
    }


    /**
     * Pattern of the lexical forms admitted by the variant, up to the digit limit of PostgreSQL {@code numeric}.
     *
     * @param variant the sign variant
     * @return pattern of the lexical forms admitted by the variant, up to the digit limit of PostgreSQL {@code
     *         numeric}
     */
    private static Pattern generatePattern(Variant variant)
    {
        StringBuilder builder = new StringBuilder();

        builder.append(WS);
        builder.append("(");

        if(variant.plus && variant.minus)
            builder.append("[-+]?");
        else if(variant.plus)
            builder.append("\\+?");
        else if(variant.minus)
            builder.append("-");

        //NOTE: postgres is able to express integers up to 131072 digits
        builder.append("0*[1-9][0-9]{0,131071}");

        // zero can be written with any sign
        if(variant.zero)
            builder.append("|[-+]?0+");

        builder.append(")");
        builder.append(WS);

        return Pattern.compile(builder.toString());
    }
}
