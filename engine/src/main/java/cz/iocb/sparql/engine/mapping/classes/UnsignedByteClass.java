package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genUnsignedByte;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdUnsignedByteType;



public final class UnsignedByteClass extends SimpleLiteralClass
{
    public UnsignedByteClass()
    {
        super("unsignedbyte", xsdUnsignedByteType, "int2", genUnsignedByte);
    }
}
