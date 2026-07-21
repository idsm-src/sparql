package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinDataTypeIRIs.xsdShortIri;



public final class ShortClass extends SimpleLiteralClass
{
    public ShortClass()
    {
        super("short", xsdShortIri, "int2");
    }
}
