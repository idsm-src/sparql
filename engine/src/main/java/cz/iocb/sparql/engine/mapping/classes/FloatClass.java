package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinDataTypeIRIs.xsdFloatIri;



public final class FloatClass extends SimpleLiteralClass
{
    public FloatClass()
    {
        super("float", xsdFloatIri, "float4");
    }
}
