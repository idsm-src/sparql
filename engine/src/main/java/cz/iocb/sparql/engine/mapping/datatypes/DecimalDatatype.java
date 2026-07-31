package cz.iocb.sparql.engine.mapping.datatypes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.unsupportedLiteral;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDecimal;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDecimalIri;
import java.util.regex.Pattern;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;
import cz.iocb.sparql.engine.rdf.Literal;



public class DecimalDatatype extends NumericDatatype
{
    private static final Pattern validFormPattern = generateDecimalPattern(false);


    public DecimalDatatype()
    {
        super(xsdDecimalIri);
    }


    @Override
    public LiteralClass getGeneralLiteralClass()
    {
        return xsdDecimal;
    }


    @Override
    public LiteralClass getResourceClass(Literal literal)
    {
        assert typeIri.equals(literal.getType());

        if(!isValidForm(literal.getValue()))
            return unsupportedLiteral;

        return xsdDecimal;
    }


    @Override
    public boolean isValidForm(String value)
    {
        return validFormPattern.matcher(value).matches();
    }
}
