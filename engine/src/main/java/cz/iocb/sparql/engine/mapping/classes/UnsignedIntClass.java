package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genUnsignedInt;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdUnsignedIntType;



/**
 * Canonical {@code xsd:unsignedInt} literals stored in one {@code int8} column.
 */
public final class UnsignedIntClass extends SimpleLiteralClass
{
    /**
     * Creates the singleton instance, see {@link BuiltinClasses}.
     */
    public UnsignedIntClass()
    {
        super("unsignedint", xsdUnsignedIntType, "int8", genUnsignedInt);
    }
}
