package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genNonPositiveInteger;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdNonPositiveIntegerType;



public final class NonPositiveIntegerClass extends SimpleLiteralClass
{
    public NonPositiveIntegerClass()
    {
        super("nonpositiveinteger", xsdNonPositiveIntegerType, "numeric", genNonPositiveInteger);
    }
}
