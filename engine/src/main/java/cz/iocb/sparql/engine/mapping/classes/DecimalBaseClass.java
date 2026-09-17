package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDecimalType;



public final class DecimalBaseClass extends SimpleLiteralBaseClass
{
    public DecimalBaseClass()
    {
        super("decimal", xsdDecimalType, "numeric");
    }
}
