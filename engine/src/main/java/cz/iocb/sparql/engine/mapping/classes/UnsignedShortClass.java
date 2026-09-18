package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genUnsignedShort;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdUnsignedShortType;



public final class UnsignedShortClass extends SimpleLiteralClass
{
    public UnsignedShortClass()
    {
        super("unsignedshort", xsdUnsignedShortType, "int4", genUnsignedShort);
    }
}
