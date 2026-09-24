package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdUnsignedShortType;



/**
 * Any valid {@code xsd:unsignedShort} literal stored as a {@code int4} value plus its lexical form.
 */
public final class UnsignedShortBaseClass extends SimpleLiteralBaseClass
{
    /**
     * Creates the singleton instance, see {@link BuiltinClasses}.
     */
    public UnsignedShortBaseClass()
    {
        super("unsignedshort", xsdUnsignedShortType, "int4");
    }
}
