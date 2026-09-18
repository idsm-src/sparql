package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdUnsignedIntType;



public final class UnsignedIntBaseClass extends SimpleLiteralBaseClass
{
    public UnsignedIntBaseClass()
    {
        super("unsignedint", xsdUnsignedIntType, "int8");
    }
}
