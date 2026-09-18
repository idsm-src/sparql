package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdNonNegativeIntegerType;



public final class NonNegativeIntegerBaseClass extends SimpleLiteralBaseClass
{
    public NonNegativeIntegerBaseClass()
    {
        super("nonnegativeinteger", xsdNonNegativeIntegerType, "numeric");
    }
}
