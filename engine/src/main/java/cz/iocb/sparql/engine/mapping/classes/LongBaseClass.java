package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdLongType;



public final class LongBaseClass extends SimpleLiteralBaseClass
{
    public LongBaseClass()
    {
        super("long", xsdLongType, "int8");
    }
}
