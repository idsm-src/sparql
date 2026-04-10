package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinDataTypes.xsdStringIri;



public final class StringClass extends SimpleLiteralClass
{
    public StringClass()
    {
        super("string", "varchar", xsdStringIri);
    }
}
