package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genDouble;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDoubleType;



public final class DoubleClass extends SimpleLiteralClass
{
    public DoubleClass()
    {
        super("double", xsdDoubleType, "float8", genDouble);
    }
}
