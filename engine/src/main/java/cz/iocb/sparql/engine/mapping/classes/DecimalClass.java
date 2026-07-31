package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDecimalType;



public final class DecimalClass extends SimpleLiteralClass
{
    public DecimalClass()
    {
        super("decimal", xsdDecimalType, "numeric");
    }
}
