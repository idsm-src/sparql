package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdUnsignedByteType;



public final class UnsignedByteBaseClass extends SimpleLiteralBaseClass
{
    public UnsignedByteBaseClass()
    {
        super("unsignedbyte", xsdUnsignedByteType, "int2");
    }
}
