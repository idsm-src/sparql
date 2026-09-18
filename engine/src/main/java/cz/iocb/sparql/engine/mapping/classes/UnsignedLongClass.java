package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genUnsignedLong;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdUnsignedLongType;



public final class UnsignedLongClass extends SimpleLiteralClass
{
    public UnsignedLongClass()
    {
        super("unsignedlong", xsdUnsignedLongType, "numeric", genUnsignedLong);
    }
}
