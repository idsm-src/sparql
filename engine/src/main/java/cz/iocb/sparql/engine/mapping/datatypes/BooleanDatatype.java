package cz.iocb.sparql.engine.mapping.datatypes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.unsupportedLiteral;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdBoolean;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdBooleanIri;
import java.util.regex.Pattern;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;
import cz.iocb.sparql.engine.rdf.Literal;



public class BooleanDatatype extends Datatype
{
    static final Pattern validFormPattern = Pattern.compile(" *(true|false|1|0) *", Pattern.CASE_INSENSITIVE);


    public BooleanDatatype()
    {
        super(xsdBooleanIri);
    }


    @Override
    public LiteralClass getGeneralLiteralClass()
    {
        return xsdBoolean;
    }


    @Override
    public LiteralClass getResourceClass(Literal literal)
    {
        assert typeIri.equals(literal.getType());

        if(!isValidForm(literal.getValue()))
            return unsupportedLiteral;

        return xsdBoolean;
    }


    @Override
    public boolean isValidForm(String value)
    {
        return validFormPattern.matcher(value).matches();
    }


    @Override
    public String getCanonicalLexicalForm(String value)
    {
        assert isValidForm(value);

        value = getCollapsedForm(value);

        if(value.equalsIgnoreCase("true") || value.equals("1"))
            return "true";

        if(value.equalsIgnoreCase("false") || value.equals("0"))
            return "false";

        throw new IllegalArgumentException();
    }
}
