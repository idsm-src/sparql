package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdByteType;



/**
 * Any valid {@code xsd:byte} literal stored as a {@code int2} value plus its lexical form.
 */
public final class ByteBaseClass extends SimpleLiteralBaseClass
{
    /**
     * Creates the singleton instance, see {@link BuiltinClasses}.
     */
    public ByteBaseClass()
    {
        super("byte", xsdByteType, "int2");
    }
}
