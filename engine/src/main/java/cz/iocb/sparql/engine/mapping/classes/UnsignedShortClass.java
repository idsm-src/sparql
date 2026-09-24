package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genUnsignedShort;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdUnsignedShortType;



/**
 * Canonical {@code xsd:unsignedShort} literals stored in one {@code int4} column.
 */
public final class UnsignedShortClass extends SimpleLiteralClass
{
    /**
     * Creates the singleton instance, see {@link BuiltinClasses}.
     */
    public UnsignedShortClass()
    {
        super("unsignedshort", xsdUnsignedShortType, "int4", genUnsignedShort);
    }
}
