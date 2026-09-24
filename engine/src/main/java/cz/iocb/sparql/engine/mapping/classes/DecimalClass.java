package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genDecimal;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDecimalType;



/**
 * Canonical {@code xsd:decimal} literals stored in one {@code numeric} column.
 */
public final class DecimalClass extends SimpleLiteralClass
{
    /**
     * Creates the singleton instance, see {@link BuiltinClasses}.
     */
    public DecimalClass()
    {
        super("decimal", xsdDecimalType, "numeric", genDecimal);
    }
}
