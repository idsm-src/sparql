package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdNonPositiveIntegerType;



public final class NonPositiveIntegerBaseClass extends SimpleLiteralBaseClass
{
    public NonPositiveIntegerBaseClass()
    {
        super("nonpositiveinteger", xsdNonPositiveIntegerType, "numeric");
    }
}
