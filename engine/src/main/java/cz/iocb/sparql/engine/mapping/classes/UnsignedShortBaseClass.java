package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdUnsignedShortType;



public final class UnsignedShortBaseClass extends SimpleLiteralBaseClass
{
    public UnsignedShortBaseClass()
    {
        super("unsignedshort", xsdUnsignedShortType, "int4");
    }
}
