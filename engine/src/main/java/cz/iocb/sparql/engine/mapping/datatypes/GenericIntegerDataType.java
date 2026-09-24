package cz.iocb.sparql.engine.mapping.datatypes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.unsupportedType;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.Literal;



/**
 * Common base of the integer datatypes. Validity is given by a range-specific pattern; the canonical form has no plus
 * sign, no leading zeros and no sign for zero.
 */
public abstract sealed class GenericIntegerDataType extends Datatype
        permits FixedSizeIntegerDatatype, VariableSizeIntegerDataType
{
    /**
     * Canonical lexical form of an integer.
     */
    private static final Pattern canonicalIntegerPattern = Pattern.compile("0|-?[1-9][0-9]*");

    /**
     * Pattern capturing the sign and the digits without leading zeros.
     */
    private static final Pattern canonizerPattern = Pattern.compile(WS + "([+-]?)0*([0-9]+)" + WS);

    /**
     * Valid lexical forms of this datatype.
     */
    private final Pattern validIntegerPattern;


    /**
     * Creates the datatype with the pattern of its valid lexical forms.
     *
     * @param typeIri the datatype IRI
     * @param validIntegerPattern pattern of the valid lexical forms
     */
    protected GenericIntegerDataType(Iri typeIri, Pattern validIntegerPattern)
    {
        super(typeIri);

        this.validIntegerPattern = validIntegerPattern;
    }


    /**
     * Class of the valid but non-canonical literals.
     *
     * @return class of the valid but non-canonical literals
     */
    protected abstract ResourceClass getNonCanonicalLiteralClass();


    @Override
    public ResourceClass getResourceClass(Literal literal)
    {
        assert typeIri.equals(literal.getType());

        if(!isValidForm(literal.getValue()))
            return unsupportedType;

        if(!isCanonicalForm(literal.getValue()))
            return getNonCanonicalLiteralClass();

        return getCanonicalLiteralClass();
    }


    @Override
    public boolean isValidForm(String value)
    {
        return validIntegerPattern.matcher(value).matches();
    }


    @Override
    public boolean isCanonicalForm(String value)
    {
        assert isValidForm(value);

        return canonicalIntegerPattern.matcher(value).matches();
    }


    @Override
    public String getCanonicalLexicalForm(String value)
    {
        assert isValidForm(value);

        Matcher m = canonizerPattern.matcher(value);

        if(!m.matches())
            throw new IllegalArgumentException();

        String digits = m.group(2);
        String sign = "-".equals(m.group(1)) && !digits.equals("0") ? "-" : "";

        return sign + digits;
    }


    @Override
    public boolean equals(Object object)
    {
        if(object == this)
            return true;

        if(!super.equals(object))
            return false;

        GenericIntegerDataType other = (GenericIntegerDataType) object;

        return Objects.equals(validIntegerPattern, other.validIntegerPattern);
    }
}
