package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genNegativeInteger;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdNegativeIntegerType;



public final class NegativeIntegerClass extends SimpleLiteralClass
{
    public NegativeIntegerClass()
    {
        super("negativeinteger", xsdNegativeIntegerType, "numeric", genNegativeInteger);
    }
}
