package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntType;



public final class IntBaseClass extends SimpleLiteralBaseClass
{
    public IntBaseClass()
    {
        super("int", xsdIntType, "int4");
    }
}
