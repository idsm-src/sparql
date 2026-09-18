package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genUnsignedInt;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdUnsignedIntType;



public final class UnsignedIntClass extends SimpleLiteralClass
{
    public UnsignedIntClass()
    {
        super("unsignedint", xsdUnsignedIntType, "int8", genUnsignedInt);
    }
}
