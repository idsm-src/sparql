package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinDataTypes.xsdShortIri;



public final class ShortClass extends SimpleLiteralClass
{
    public ShortClass()
    {
        super("short", "int2", xsdShortIri);
    }
}
