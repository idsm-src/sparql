package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genShort;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdShortType;



/**
 * Canonical {@code xsd:short} literals stored in one {@code int2} column.
 */
public final class ShortClass extends SimpleLiteralClass
{
    /**
     * Creates the singleton instance, see {@link BuiltinClasses}.
     */
    public ShortClass()
    {
        super("short", xsdShortType, "int2", genShort);
    }
}
