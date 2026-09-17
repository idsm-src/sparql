package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdBooleanType;



public final class BooleanBaseClass extends SimpleLiteralBaseClass
{
    protected BooleanBaseClass()
    {
        super("boolean", xsdBooleanType, "bool");
    }
}
