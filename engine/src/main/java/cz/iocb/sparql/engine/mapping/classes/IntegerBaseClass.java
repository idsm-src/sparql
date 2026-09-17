package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerType;



public final class IntegerBaseClass extends SimpleLiteralBaseClass
{
    public IntegerBaseClass()
    {
        super("integer", xsdIntegerType, "numeric");
    }
}
