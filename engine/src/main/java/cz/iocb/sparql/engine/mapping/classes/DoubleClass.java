package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinDataTypeIRIs.xsdDoubleIri;



public final class DoubleClass extends SimpleLiteralClass
{
    public DoubleClass()
    {
        super("double", xsdDoubleIri, "float8");
    }
}
