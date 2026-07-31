package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdFloatType;



public final class FloatClass extends SimpleLiteralClass
{
    public FloatClass()
    {
        super("float", xsdFloatType, "float4");
    }
}
