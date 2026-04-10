package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinDataTypes.xsdIntegerIri;



public final class IntegerClass extends SimpleLiteralClass
{
    public IntegerClass()
    {
        super("integer", "numeric", xsdIntegerIri);
    }
}
