package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genNonNegativeInteger;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdNonNegativeIntegerType;



/**
 * Canonical {@code xsd:nonNegativeInteger} literals stored in one {@code numeric} column.
 */
public final class NonNegativeIntegerClass extends SimpleLiteralClass
{
    /**
     * Creates the singleton instance, see {@link BuiltinClasses}.
     */
    public NonNegativeIntegerClass()
    {
        super("nonnegativeinteger", xsdNonNegativeIntegerType, "numeric", genNonNegativeInteger);
    }
}
