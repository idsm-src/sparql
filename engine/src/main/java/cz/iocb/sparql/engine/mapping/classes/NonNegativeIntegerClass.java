package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genNonNegativeInteger;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdNonNegativeIntegerType;



public final class NonNegativeIntegerClass extends SimpleLiteralClass
{
    public NonNegativeIntegerClass()
    {
        super("nonnegativeinteger", xsdNonNegativeIntegerType, "numeric", genNonNegativeInteger);
    }
}
