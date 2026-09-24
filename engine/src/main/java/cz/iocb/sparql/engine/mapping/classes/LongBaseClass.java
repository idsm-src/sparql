package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdLongType;



/**
 * Any valid {@code xsd:long} literal stored as a {@code int8} value plus its lexical form.
 */
public final class LongBaseClass extends SimpleLiteralBaseClass
{
    /**
     * Creates the singleton instance, see {@link BuiltinClasses}.
     */
    public LongBaseClass()
    {
        super("long", xsdLongType, "int8");
    }
}
