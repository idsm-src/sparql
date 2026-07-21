package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinDataTypeIRIs.xsdIntIri;



public final class IntClass extends SimpleLiteralClass
{
    public IntClass()
    {
        super("int", xsdIntIri, "int4");
    }
}
