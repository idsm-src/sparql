package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genPositiveInteger;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdPositiveIntegerType;



/**
 * Canonical {@code xsd:positiveInteger} literals stored in one {@code numeric} column.
 */
public final class PositiveIntegerClass extends SimpleLiteralClass
{
    /**
     * Creates the singleton instance, see {@link BuiltinClasses}.
     */
    public PositiveIntegerClass()
    {
        super("positiveinteger", xsdPositiveIntegerType, "numeric", genPositiveInteger);
    }
}
