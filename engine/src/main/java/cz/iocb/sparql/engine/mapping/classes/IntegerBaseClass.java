package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.database.SqlType.NUMERIC;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerType;



/**
 * Any valid {@code xsd:integer} literal stored as a {@code numeric} value plus its lexical form.
 */
public final class IntegerBaseClass extends SimpleLiteralBaseClass
{
    /**
     * Creates the singleton instance, see {@link BuiltinClasses}.
     */
    public IntegerBaseClass()
    {
        super("integer", xsdIntegerType, NUMERIC);
    }
}
