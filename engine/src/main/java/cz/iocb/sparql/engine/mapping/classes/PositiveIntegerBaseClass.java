package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdPositiveIntegerType;



public final class PositiveIntegerBaseClass extends SimpleLiteralBaseClass
{
    public PositiveIntegerBaseClass()
    {
        super("positiveinteger", xsdPositiveIntegerType, "numeric");
    }
}
