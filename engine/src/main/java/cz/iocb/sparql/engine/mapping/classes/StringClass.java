package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringType;



public final class StringClass extends SimpleLiteralClass
{
    public StringClass()
    {
        super("string", xsdStringType, "varchar");
    }
}
