package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genNegativeInteger;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdNegativeIntegerType;



/**
 * Canonical {@code xsd:negativeInteger} literals stored in one {@code numeric} column.
 */
public final class NegativeIntegerClass extends SimpleLiteralClass
{
    /**
     * Creates the singleton instance, see {@link BuiltinClasses}.
     */
    public NegativeIntegerClass()
    {
        super("negativeinteger", xsdNegativeIntegerType, "numeric", genNegativeInteger);
    }
}
