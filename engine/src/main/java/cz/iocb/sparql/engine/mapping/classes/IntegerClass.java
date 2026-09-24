package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.database.SqlType.NUMERIC;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genInteger;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerType;



/**
 * Canonical {@code xsd:integer} literals stored in one {@code numeric} column.
 */
public final class IntegerClass extends SimpleLiteralClass
{
    /**
     * Creates the singleton instance, see {@link BuiltinClasses}.
     */
    public IntegerClass()
    {
        super("integer", xsdIntegerType, NUMERIC, genInteger);
    }
}
