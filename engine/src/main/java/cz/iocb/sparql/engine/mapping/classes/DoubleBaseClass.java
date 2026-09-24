package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.database.SqlType.FLOAT8;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDoubleType;



/**
 * Any valid {@code xsd:double} literal stored as a {@code float8} value plus its lexical form.
 */
public final class DoubleBaseClass extends SimpleLiteralBaseClass
{
    /**
     * Creates the singleton instance, see {@link BuiltinClasses}.
     */
    public DoubleBaseClass()
    {
        super("double", xsdDoubleType, FLOAT8);
    }
}
