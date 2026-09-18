package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdByteType;



public final class ByteBaseClass extends SimpleLiteralBaseClass
{
    public ByteBaseClass()
    {
        super("byte", xsdByteType, "int2");
    }
}
