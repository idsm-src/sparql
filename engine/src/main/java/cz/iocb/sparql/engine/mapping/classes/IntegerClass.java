package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinDataTypeIRIs.xsdIntegerIri;



public final class IntegerClass extends SimpleLiteralClass
{
    public IntegerClass()
    {
        super("integer", xsdIntegerIri, "numeric");
    }
}
