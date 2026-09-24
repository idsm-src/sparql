package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.database.SqlType.FLOAT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genFloat;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdFloatType;



/**
 * Canonical {@code xsd:float} literals stored in one {@code float4} column.
 */
public final class FloatClass extends SimpleLiteralClass
{
    /**
     * Creates the singleton instance, see {@link BuiltinClasses}.
     */
    public FloatClass()
    {
        super("float", xsdFloatType, FLOAT4, genFloat);
    }
}
