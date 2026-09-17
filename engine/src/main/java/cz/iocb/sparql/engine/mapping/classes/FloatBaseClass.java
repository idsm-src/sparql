package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdFloatType;



public final class FloatBaseClass extends SimpleLiteralBaseClass
{
    public FloatBaseClass()
    {
        super("float", xsdFloatType, "float4");
    }
}
