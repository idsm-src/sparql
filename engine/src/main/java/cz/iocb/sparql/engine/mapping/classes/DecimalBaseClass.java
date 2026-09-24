package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDecimalType;



/**
 * Any valid {@code xsd:decimal} literal stored as a {@code numeric} value plus its lexical form.
 */
public final class DecimalBaseClass extends SimpleLiteralBaseClass
{
    /**
     * Creates the singleton instance, see {@link BuiltinClasses}.
     */
    public DecimalBaseClass()
    {
        super("decimal", xsdDecimalType, "numeric");
    }
}
