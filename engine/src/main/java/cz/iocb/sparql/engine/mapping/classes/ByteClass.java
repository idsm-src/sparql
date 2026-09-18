package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genByte;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdByteType;



public final class ByteClass extends SimpleLiteralClass
{
    public ByteClass()
    {
        super("byte", xsdByteType, "int2", genByte);
    }
}
