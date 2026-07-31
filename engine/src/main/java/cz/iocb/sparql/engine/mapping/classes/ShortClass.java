package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdShortType;



public final class ShortClass extends SimpleLiteralClass
{
    public ShortClass()
    {
        super("short", xsdShortType, "int2");
    }
}
