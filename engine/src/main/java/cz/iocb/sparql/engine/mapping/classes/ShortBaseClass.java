package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.database.SqlType.INT2;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdShortType;



/**
 * Any valid {@code xsd:short} literal stored as a {@code int2} value plus its lexical form.
 */
public final class ShortBaseClass extends SimpleLiteralBaseClass
{
    /**
     * Creates the singleton instance, see {@link BuiltinClasses}.
     */
    public ShortBaseClass()
    {
        super("short", xsdShortType, INT2);
    }
}
