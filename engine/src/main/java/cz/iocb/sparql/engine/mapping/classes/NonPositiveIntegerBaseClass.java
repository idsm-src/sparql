package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.database.SqlType.NUMERIC;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdNonPositiveIntegerType;



/**
 * Any valid {@code xsd:nonPositiveInteger} literal stored as a {@code numeric} value plus its lexical form.
 */
public final class NonPositiveIntegerBaseClass extends SimpleLiteralBaseClass
{
    /**
     * Creates the singleton instance, see {@link BuiltinClasses}.
     */
    public NonPositiveIntegerBaseClass()
    {
        super("nonpositiveinteger", xsdNonPositiveIntegerType, NUMERIC);
    }
}
