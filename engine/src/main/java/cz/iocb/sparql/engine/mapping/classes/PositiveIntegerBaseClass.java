package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.database.SqlType.NUMERIC;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdPositiveIntegerType;



/**
 * Any valid {@code xsd:positiveInteger} literal stored as a {@code numeric} value plus its lexical form.
 */
public final class PositiveIntegerBaseClass extends SimpleLiteralBaseClass
{
    /**
     * Creates the singleton instance, see {@link BuiltinClasses}.
     */
    public PositiveIntegerBaseClass()
    {
        super("positiveinteger", xsdPositiveIntegerType, NUMERIC);
    }
}
