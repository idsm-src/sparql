package cz.iocb.sparql.engine.mapping.datatypes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genUnsignedShort;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.lexUnsignedShort;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdUnsignedShort;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdUnsignedShortIri;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;



public final class UnsignedShortDatatype extends FixedSizeIntegerDatatype
{
    public UnsignedShortDatatype()
    {
        super(xsdUnsignedShortIri, "0", "65535");
    }


    @Override
    public LiteralClass getBaseLiteralClass()
    {
        return genUnsignedShort;
    }


    @Override
    public LiteralClass getCanonicalLiteralClass()
    {
        return xsdUnsignedShort;
    }


    @Override
    public ResourceClass getNonCanonicalLiteralClass()
    {
        return lexUnsignedShort;
    }
}
