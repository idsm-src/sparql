package cz.iocb.sparql.engine.mapping.datatypes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genBoolean;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.lexBoolean;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.unsupportedType;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdBoolean;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdBooleanIri;
import java.util.regex.Pattern;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.Literal;



/**
 * The xsd:boolean datatype; {@code 1} and {@code 0} are valid but not canonical.
 */
public final class BooleanDatatype extends Datatype
{
    /**
     * Valid lexical forms: {@code true}, {@code false}, {@code 1}, {@code 0} with surrounding whitespace.
     */
    static final Pattern validFormPattern = Pattern.compile(WS + "(true|false|1|0)" + WS);


    /**
     * Creates the datatype.
     */
    public BooleanDatatype()
    {
        super(xsdBooleanIri);
    }


    @Override
    public LiteralClass getBaseLiteralClass()
    {
        return genBoolean;
    }


    @Override
    public LiteralClass getCanonicalLiteralClass()
    {
        return xsdBoolean;
    }


    @Override
    public ResourceClass getResourceClass(Literal literal)
    {
        assert typeIri.equals(literal.getType());

        if(!isValidForm(literal.getValue()))
            return unsupportedType;

        if(!isCanonicalForm(literal.getValue()))
            return lexBoolean;

        return xsdBoolean;
    }


    @Override
    public boolean isValidForm(String value)
    {
        return validFormPattern.matcher(value).matches();
    }


    @Override
    public boolean isCanonicalForm(String value)
    {
        return value.equals("true") || value.equals("false");
    }


    @Override
    public String getCanonicalLexicalForm(String value)
    {
        assert isValidForm(value);

        if(value.contains("true") || value.contains("1"))
            return "true";

        if(value.contains("false") || value.contains("0"))
            return "false";

        throw new IllegalArgumentException();
    }
}
