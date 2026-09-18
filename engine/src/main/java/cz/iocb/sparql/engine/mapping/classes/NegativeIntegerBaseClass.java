package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdNegativeIntegerType;



public final class NegativeIntegerBaseClass extends SimpleLiteralBaseClass
{
    public NegativeIntegerBaseClass()
    {
        super("negativeinteger", xsdNegativeIntegerType, "numeric");
    }
}
