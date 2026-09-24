package cz.iocb.sparql.engine.mapping.datatypes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genByte;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.lexByte;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdByte;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdByteIri;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;



/**
 * The xsd:byte datatype.
 */
public final class ByteDatatype extends FixedSizeIntegerDatatype
{
    /**
     * Creates the datatype.
     */
    public ByteDatatype()
    {
        super(xsdByteIri, Byte.toString(Byte.MIN_VALUE), Byte.toString(Byte.MAX_VALUE));
    }


    @Override
    public LiteralClass getBaseLiteralClass()
    {
        return genByte;
    }


    @Override
    public LiteralClass getCanonicalLiteralClass()
    {
        return xsdByte;
    }


    @Override
    public ResourceClass getNonCanonicalLiteralClass()
    {
        return lexByte;
    }
}
