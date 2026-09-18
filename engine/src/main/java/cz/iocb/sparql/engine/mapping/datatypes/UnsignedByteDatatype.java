package cz.iocb.sparql.engine.mapping.datatypes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genUnsignedByte;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.lexUnsignedByte;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdUnsignedByte;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdUnsignedByteIri;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;



public final class UnsignedByteDatatype extends FixedSizeIntegerDatatype
{
    public UnsignedByteDatatype()
    {
        super(xsdUnsignedByteIri, "0", "255");
    }


    @Override
    public LiteralClass getBaseLiteralClass()
    {
        return genUnsignedByte;
    }


    @Override
    public LiteralClass getCanonicalLiteralClass()
    {
        return xsdUnsignedByte;
    }


    @Override
    public ResourceClass getNonCanonicalLiteralClass()
    {
        return lexUnsignedByte;
    }
}
