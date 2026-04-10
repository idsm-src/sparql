package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinDataTypes.xsdIntIri;



public final class IntClass extends SimpleLiteralClass
{
    public IntClass()
    {
        super("int", "int4", xsdIntIri);
    }
}
