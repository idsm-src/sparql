package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinDataTypes.xsdDoubleIri;



public final class DoubleClass extends SimpleLiteralClass
{
    public DoubleClass()
    {
        super("double", "float8", xsdDoubleIri);
    }
}
