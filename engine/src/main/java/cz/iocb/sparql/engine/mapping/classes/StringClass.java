package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.database.SqlType.VARCHAR;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringType;



/**
 * Canonical {@code xsd:string} literals stored in one {@code varchar} column.
 */
public final class StringClass extends SimpleLiteralClass
{
    /**
     * Creates the singleton instance, see {@link BuiltinClasses}.
     */
    public StringClass()
    {
        super("string", xsdStringType, VARCHAR);
    }
}
