package cz.iocb.sparql.engine.mapping.datatypes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genLong;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.lexLong;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdLong;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdLongIri;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;



/**
 * The xsd:long datatype.
 */
public final class LongDatatype extends FixedSizeIntegerDatatype
{
    /**
     * Creates the datatype.
     */
    public LongDatatype()
    {
        super(xsdLongIri, Long.toString(Long.MIN_VALUE), Long.toString(Long.MAX_VALUE));
    }


    @Override
    public LiteralClass getBaseLiteralClass()
    {
        return genLong;
    }


    @Override
    public LiteralClass getCanonicalLiteralClass()
    {
        return xsdLong;
    }


    @Override
    public ResourceClass getNonCanonicalLiteralClass()
    {
        return lexLong;
    }
}
