package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntType;



/**
 * Any valid {@code xsd:int} literal stored as a {@code int4} value plus its lexical form.
 */
public final class IntBaseClass extends SimpleLiteralBaseClass
{
    /**
     * Creates the singleton instance, see {@link BuiltinClasses}.
     */
    public IntBaseClass()
    {
        super("int", xsdIntType, "int4");
    }
}
