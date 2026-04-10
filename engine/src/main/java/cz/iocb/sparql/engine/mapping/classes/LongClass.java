package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinDataTypes.xsdLongIri;



public final class LongClass extends SimpleLiteralClass
{
    public LongClass()
    {
        super("long", "int8", xsdLongIri);
    }
}
