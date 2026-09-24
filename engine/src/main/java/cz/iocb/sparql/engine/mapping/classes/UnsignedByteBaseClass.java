package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.database.SqlType.INT2;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdUnsignedByteType;



/**
 * Any valid {@code xsd:unsignedByte} literal stored as a {@code int2} value plus its lexical form.
 */
public final class UnsignedByteBaseClass extends SimpleLiteralBaseClass
{
    /**
     * Creates the singleton instance, see {@link BuiltinClasses}.
     */
    public UnsignedByteBaseClass()
    {
        super("unsignedbyte", xsdUnsignedByteType, INT2);
    }
}
