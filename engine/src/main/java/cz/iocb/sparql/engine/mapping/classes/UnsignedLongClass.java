package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genUnsignedLong;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdUnsignedLongType;



/**
 * Canonical {@code xsd:unsignedLong} literals stored in one {@code numeric} column.
 */
public final class UnsignedLongClass extends SimpleLiteralClass
{
    /**
     * Creates the singleton instance, see {@link BuiltinClasses}.
     */
    public UnsignedLongClass()
    {
        super("unsignedlong", xsdUnsignedLongType, "numeric", genUnsignedLong);
    }
}
