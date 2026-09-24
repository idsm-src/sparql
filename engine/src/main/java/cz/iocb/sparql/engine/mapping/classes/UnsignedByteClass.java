package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genUnsignedByte;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdUnsignedByteType;



/**
 * Canonical {@code xsd:unsignedByte} literals stored in one {@code int2} column.
 */
public final class UnsignedByteClass extends SimpleLiteralClass
{
    /**
     * Creates the singleton instance, see {@link BuiltinClasses}.
     */
    public UnsignedByteClass()
    {
        super("unsignedbyte", xsdUnsignedByteType, "int2", genUnsignedByte);
    }
}
