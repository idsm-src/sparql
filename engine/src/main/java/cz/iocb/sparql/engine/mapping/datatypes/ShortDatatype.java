package cz.iocb.sparql.engine.mapping.datatypes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genShort;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.lexShort;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdShort;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdShortIri;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;



public final class ShortDatatype extends FixedSizeIntegerDatatype
{
    public ShortDatatype()
    {
        super(xsdShortIri, Short.toString(Short.MIN_VALUE), Short.toString(Short.MAX_VALUE));
    }


    @Override
    public LiteralClass getBaseLiteralClass()
    {
        return genShort;
    }


    @Override
    public LiteralClass getCanonicalLiteralClass()
    {
        return xsdShort;
    }


    @Override
    public ResourceClass getNonCanonicalLiteralClass()
    {
        return lexShort;
    }
}
