package cz.iocb.sparql.engine.mapping.datatypes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;
import cz.iocb.sparql.engine.rdf.Literal;



public class StringDatatype extends StringLiteralDatatype
{
    protected StringDatatype()
    {
        super(xsdStringIri);
    }


    @Override
    public LiteralClass getGeneralLiteralClass()
    {
        return xsdString;
    }


    @Override
    public LiteralClass getResourceClass(Literal literal)
    {
        assert typeIri.equals(literal.getType());

        return xsdString;
    }
}
