package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerType;



public final class IntegerClass extends SimpleLiteralClass
{
    public IntegerClass()
    {
        super("integer", xsdIntegerType, "numeric");
    }
}
