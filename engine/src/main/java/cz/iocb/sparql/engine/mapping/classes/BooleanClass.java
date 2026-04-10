package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinDataTypes.xsdBooleanIri;



public final class BooleanClass extends SimpleLiteralClass
{
    public BooleanClass()
    {
        super("boolean", "bool", xsdBooleanIri);
    }
}
