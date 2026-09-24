package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genLong;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdLongType;



/**
 * Canonical {@code xsd:long} literals stored in one {@code int8} column.
 */
public final class LongClass extends SimpleLiteralClass
{
    /**
     * Creates the singleton instance, see {@link BuiltinClasses}.
     */
    public LongClass()
    {
        super("long", xsdLongType, "int8", genLong);
    }
}
