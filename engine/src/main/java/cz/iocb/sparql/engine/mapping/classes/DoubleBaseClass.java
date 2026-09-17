package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDoubleType;



public final class DoubleBaseClass extends SimpleLiteralBaseClass
{
    public DoubleBaseClass()
    {
        super("double", xsdDoubleType, "float8");
    }
}
