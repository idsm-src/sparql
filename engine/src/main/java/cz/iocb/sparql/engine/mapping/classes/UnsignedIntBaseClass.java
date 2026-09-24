package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.database.SqlType.INT8;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdUnsignedIntType;



/**
 * Any valid {@code xsd:unsignedInt} literal stored as a {@code int8} value plus its lexical form.
 */
public final class UnsignedIntBaseClass extends SimpleLiteralBaseClass
{
    /**
     * Creates the singleton instance, see {@link BuiltinClasses}.
     */
    public UnsignedIntBaseClass()
    {
        super("unsignedint", xsdUnsignedIntType, INT8);
    }
}
