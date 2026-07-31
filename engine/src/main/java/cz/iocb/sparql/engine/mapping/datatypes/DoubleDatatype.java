package cz.iocb.sparql.engine.mapping.datatypes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.unsupportedLiteral;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDouble;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDoubleIri;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;
import cz.iocb.sparql.engine.rdf.Literal;



public class DoubleDatatype extends FloatPointDatatype
{
    protected DoubleDatatype()
    {
        super(xsdDoubleIri);
    }


    @Override
    public LiteralClass getGeneralLiteralClass()
    {
        return xsdDouble;
    }


    @Override
    public LiteralClass getResourceClass(Literal literal)
    {
        assert typeIri.equals(literal.getType());

        if(!isValidForm(literal.getValue()))
            return unsupportedLiteral;

        return xsdDouble;
    }


    @Override
    public String getCanonicalLexicalForm(String value)
    {
        assert isValidForm(value);

        //FIXME: use a proper implementation
        return Double.toString(Double.parseDouble(value));
    }
}
