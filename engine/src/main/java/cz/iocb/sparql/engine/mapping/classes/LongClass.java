package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinDataTypeIRIs.xsdLongIri;



public final class LongClass extends SimpleLiteralClass
{
    public LongClass()
    {
        super("long", xsdLongIri, "int8");
    }
}
