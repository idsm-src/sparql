package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinDataTypes.xsdFloatIri;



public final class FloatClass extends SimpleLiteralClass
{
    public FloatClass()
    {
        super("float", "float4", xsdFloatIri);
    }
}
