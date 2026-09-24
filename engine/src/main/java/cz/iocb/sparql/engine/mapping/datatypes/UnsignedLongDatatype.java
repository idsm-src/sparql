package cz.iocb.sparql.engine.mapping.datatypes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genUnsignedLong;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.lexUnsignedLong;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdUnsignedLong;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdUnsignedLongIri;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;



/**
 * The xsd:unsignedLong datatype.
 */
public final class UnsignedLongDatatype extends FixedSizeIntegerDatatype
{
    /**
     * Creates the datatype.
     */
    public UnsignedLongDatatype()
    {
        super(xsdUnsignedLongIri, "0", "18446744073709551615");
    }


    @Override
    public LiteralClass getBaseLiteralClass()
    {
        return genUnsignedLong;
    }


    @Override
    public LiteralClass getCanonicalLiteralClass()
    {
        return xsdUnsignedLong;
    }


    @Override
    public ResourceClass getNonCanonicalLiteralClass()
    {
        return lexUnsignedLong;
    }
}
