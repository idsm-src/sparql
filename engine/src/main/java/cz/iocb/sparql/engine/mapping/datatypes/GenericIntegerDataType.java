package cz.iocb.sparql.engine.mapping.datatypes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.unsupportedType;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.Literal;



public abstract sealed class GenericIntegerDataType extends Datatype
        permits FixedSizeIntegerDatatype, VariableSizeIntegerDataType
{
    private static final Pattern canonicalIntegerPattern = Pattern.compile("0|-?[1-9][0-9]*");

    private static final Pattern canonizerPattern = Pattern.compile(WS + "([+-]?)0*([0-9]+)" + WS);

    private final Pattern validIntegerPattern;


    protected GenericIntegerDataType(Iri typeIri, Pattern validIntegerPattern)
    {
        super(typeIri);

        this.validIntegerPattern = validIntegerPattern;
    }


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
