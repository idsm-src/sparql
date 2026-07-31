package cz.iocb.sparql.engine.mapping.datatypes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.unsupportedLiteral;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdFloat;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdFloatIri;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;
import cz.iocb.sparql.engine.rdf.Literal;



public class FloatDatatype extends FloatPointDatatype
{
    protected FloatDatatype()
    {
        super(xsdFloatIri);
    }


    @Override
    public LiteralClass getGeneralLiteralClass()
    {
        return xsdFloat;
    }


    @Override
    public LiteralClass getResourceClass(Literal literal)
    {
        assert typeIri.equals(literal.getType());

        if(!isValidForm(literal.getValue()))
            return unsupportedLiteral;

        return xsdFloat;
    }


    @Override
    public String getCanonicalLexicalForm(String value)
    {
        assert isValidForm(value);

        //FIXME: use a proper implementation
        return Float.toString(Float.parseFloat(value));
    }
}
