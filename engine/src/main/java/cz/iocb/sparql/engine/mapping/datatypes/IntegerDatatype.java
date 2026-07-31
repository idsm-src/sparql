package cz.iocb.sparql.engine.mapping.datatypes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.unsupportedLiteral;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInteger;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerIri;
import java.util.regex.Pattern;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;
import cz.iocb.sparql.engine.rdf.Literal;



public class IntegerDatatype extends NumericDatatype
{
    private static final Pattern validFormPattern = generateIntegerPattern(true, true, true, false);


    public IntegerDatatype()
    {
        super(xsdIntegerIri);
    }


    @Override
    public LiteralClass getGeneralLiteralClass()
    {
        return xsdInteger;
    }


    @Override
    public LiteralClass getResourceClass(Literal literal)
    {
        assert typeIri.equals(literal.getType());

        if(!isValidForm(literal.getValue()))
            return unsupportedLiteral;

        return xsdInteger;
    }


    @Override
    public boolean isValidForm(String value)
    {
        return validFormPattern.matcher(value).matches();
    }
}
