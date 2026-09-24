package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdBooleanType;



/**
 * Any valid {@code xsd:boolean} literal stored as a {@code bool} value plus its lexical form.
 */
public final class BooleanBaseClass extends SimpleLiteralBaseClass
{
    /**
     * Creates the singleton instance, see {@link BuiltinClasses}.
     */
    protected BooleanBaseClass()
    {
        super("boolean", xsdBooleanType, "bool");
    }
}
