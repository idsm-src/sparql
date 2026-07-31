package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdBooleanType;



public final class BooleanClass extends SimpleLiteralClass
{
    protected BooleanClass()
    {
        super("boolean", xsdBooleanType, "bool");
    }
}
