package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdLongType;



public final class LongClass extends SimpleLiteralClass
{
    public LongClass()
    {
        super("long", xsdLongType, "int8");
    }
}
