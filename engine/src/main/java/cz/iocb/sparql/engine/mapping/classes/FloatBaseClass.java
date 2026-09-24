package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.database.SqlType.FLOAT4;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdFloatType;



/**
 * Any valid {@code xsd:float} literal stored as a {@code float4} value plus its lexical form.
 */
public final class FloatBaseClass extends SimpleLiteralBaseClass
{
    /**
     * Creates the singleton instance, see {@link BuiltinClasses}.
     */
    public FloatBaseClass()
    {
        super("float", xsdFloatType, FLOAT4);
    }
}
