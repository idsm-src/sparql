package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdNonNegativeIntegerType;



/**
 * Any valid {@code xsd:nonNegativeInteger} literal stored as a {@code numeric} value plus its lexical form.
 */
public final class NonNegativeIntegerBaseClass extends SimpleLiteralBaseClass
{
    /**
     * Creates the singleton instance, see {@link BuiltinClasses}.
     */
    public NonNegativeIntegerBaseClass()
    {
        super("nonnegativeinteger", xsdNonNegativeIntegerType, "numeric");
    }
}
