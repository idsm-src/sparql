package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdShortType;



public final class ShortBaseClass extends SimpleLiteralBaseClass
{
    public ShortBaseClass()
    {
        super("short", xsdShortType, "int2");
    }
}
