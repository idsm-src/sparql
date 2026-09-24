package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genNonPositiveInteger;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdNonPositiveIntegerType;



/**
 * Canonical {@code xsd:nonPositiveInteger} literals stored in one {@code numeric} column.
 */
public final class NonPositiveIntegerClass extends SimpleLiteralClass
{
    /**
     * Creates the singleton instance, see {@link BuiltinClasses}.
     */
    public NonPositiveIntegerClass()
    {
        super("nonpositiveinteger", xsdNonPositiveIntegerType, "numeric", genNonPositiveInteger);
    }
}
