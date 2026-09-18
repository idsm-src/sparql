package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genPositiveInteger;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdPositiveIntegerType;



public final class PositiveIntegerClass extends SimpleLiteralClass
{
    public PositiveIntegerClass()
    {
        super("positiveinteger", xsdPositiveIntegerType, "numeric", genPositiveInteger);
    }
}
