package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdUnsignedLongType;



public final class UnsignedLongBaseClass extends SimpleLiteralBaseClass
{
    public UnsignedLongBaseClass()
    {
        super("unsignedlong", xsdUnsignedLongType, "numeric");
    }
}
