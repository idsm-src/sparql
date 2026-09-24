package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdNegativeIntegerType;



/**
 * Any valid {@code xsd:negativeInteger} literal stored as a {@code numeric} value plus its lexical form.
 */
public final class NegativeIntegerBaseClass extends SimpleLiteralBaseClass
{
    /**
     * Creates the singleton instance, see {@link BuiltinClasses}.
     */
    public NegativeIntegerBaseClass()
    {
        super("negativeinteger", xsdNegativeIntegerType, "numeric");
    }
}
