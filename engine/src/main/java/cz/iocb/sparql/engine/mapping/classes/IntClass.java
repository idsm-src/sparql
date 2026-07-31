package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntType;



public final class IntClass extends SimpleLiteralClass
{
    public IntClass()
    {
        super("int", xsdIntType, "int4");
    }
}
