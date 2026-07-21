package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinDataTypeIRIs.xsdDecimalIri;



public final class DecimalClass extends SimpleLiteralClass
{
    public DecimalClass()
    {
        super("decimal", xsdDecimalIri, "numeric");
    }
}
