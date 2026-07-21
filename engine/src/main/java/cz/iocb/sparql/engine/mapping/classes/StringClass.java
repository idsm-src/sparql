package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinDataTypeIRIs.xsdStringIri;



public final class StringClass extends SimpleLiteralClass
{
    public StringClass()
    {
        super("string", xsdStringIri, "varchar");
    }
}
