package cz.iocb.sparql.engine.mapping.datatypes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.unsupportedLiteral;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntIri;
import java.util.regex.Pattern;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;
import cz.iocb.sparql.engine.rdf.Literal;



public class IntDatatype extends NumericDatatype
{
    private static final Pattern validFormPattern = generateFixedSizePattern(Integer.toString(Integer.MAX_VALUE), false,
            false);


    public IntDatatype()
    {
        super(xsdIntIri);
    }


    @Override
    public LiteralClass getGeneralLiteralClass()
    {
        return xsdInt;
    }


    @Override
    public LiteralClass getResourceClass(Literal literal)
    {
        assert typeIri.equals(literal.getType());

        if(!isValidForm(literal.getValue()))
            return unsupportedLiteral;

        return xsdInt;
    }


    @Override
    public boolean isValidForm(String value)
    {
        return validFormPattern.matcher(value).matches();
    }
}
