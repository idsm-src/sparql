package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinDataTypeIRIs.xsdBooleanIri;



public final class BooleanClass extends SimpleLiteralClass
{
    protected BooleanClass()
    {
        super("boolean", xsdBooleanIri, "bool");
    }
}
