package cz.iocb.sparql.engine.mapping.datatypes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genDecimal;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.lexDecimal;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.unsupportedType;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDecimal;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDecimalIri;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.Literal;



/**
 * The xsd:decimal datatype; canonical form always has a fractional part ({@code 1.0}) and no redundant zeros.
 */
public final class DecimalDatatype extends Datatype
{
    /**
     * Valid lexical form within the precision of PostgreSQL {@code numeric}, capturing sign, integer and fractional
     * digits.
     */
    private static final Pattern validDecimalPattern = Pattern
            .compile(WS + "([+-]?)(?=\\.?[0-9])0*([0-9]{0,131072})(?:\\.([0-9]{0,16383}?)0*)?" + WS);

    /**
     * Canonical lexical form: no leading or trailing zeros, always a fractional part, no negative zero.
     */
    private static final Pattern canonicalDecimalPattern = Pattern
            .compile("(?!-0\\.0$)-?(?:0|[1-9][0-9]*)\\.(?:0|[0-9]*[1-9])");


    /**
     * Creates the datatype.
     */
    public DecimalDatatype()
    {
        super(xsdDecimalIri);
    }


    @Override
    public LiteralClass getBaseLiteralClass()
    {
        return genDecimal;
    }


    @Override
    public LiteralClass getCanonicalLiteralClass()
    {
        return xsdDecimal;
    }


    @Override
    public ResourceClass getResourceClass(Literal literal)
    {
        assert typeIri.equals(literal.getType());

        if(!isValidForm(literal.getValue()))
            return unsupportedType;

        if(!isCanonicalForm(literal.getValue()))
            return lexDecimal;

        return xsdDecimal;
    }


    @Override
    public boolean isValidForm(String value)
    {
        return validDecimalPattern.matcher(value).matches();
    }


    @Override
    public boolean isCanonicalForm(String value)
    {
        assert isValidForm(value);

        return canonicalDecimalPattern.matcher(value).matches();
    }


    @Override
    public String getCanonicalLexicalForm(String value)
    {
        assert isValidForm(value);

        Matcher m = validDecimalPattern.matcher(value);

        if(!m.matches())
            throw new IllegalArgumentException();

        String intPart = m.group(2).isEmpty() ? "0" : m.group(2);
        String fracPart = m.group(3) == null || m.group(3).isEmpty() ? "0" : m.group(3);
        String sign = "-".equals(m.group(1)) && !(intPart.equals("0") && fracPart.equals("0")) ? "-" : "";

        return sign + intPart + "." + fracPart;
    }
}
