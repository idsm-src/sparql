package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.database.SqlType.BOOL;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genBoolean;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdBooleanType;



/**
 * Canonical {@code xsd:boolean} literals stored in one {@code bool} column.
 */
public final class BooleanClass extends SimpleLiteralClass
{
    /**
     * Creates the singleton instance, see {@link BuiltinClasses}.
     */
    protected BooleanClass()
    {
        super("boolean", xsdBooleanType, BOOL, genBoolean);
    }
}
