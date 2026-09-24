package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.database.SqlType.INT2;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genByte;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdByteType;



/**
 * Canonical {@code xsd:byte} literals stored in one {@code int2} column.
 */
public final class ByteClass extends SimpleLiteralClass
{
    /**
     * Creates the singleton instance, see {@link BuiltinClasses}.
     */
    public ByteClass()
    {
        super("byte", xsdByteType, INT2, genByte);
    }
}
