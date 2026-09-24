package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genDouble;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDoubleType;



/**
 * Canonical {@code xsd:double} literals stored in one {@code float8} column.
 */
public final class DoubleClass extends SimpleLiteralClass
{
    /**
     * Creates the singleton instance, see {@link BuiltinClasses}.
     */
    public DoubleClass()
    {
        super("double", xsdDoubleType, "float8", genDouble);
    }
}
