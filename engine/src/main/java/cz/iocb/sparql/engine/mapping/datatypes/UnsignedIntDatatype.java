package cz.iocb.sparql.engine.mapping.datatypes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genUnsignedInt;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.lexUnsignedInt;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdUnsignedInt;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdUnsignedIntIri;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;



public final class UnsignedIntDatatype extends FixedSizeIntegerDatatype
{
    public UnsignedIntDatatype()
    {
        super(xsdUnsignedIntIri, "0", "4294967295");
    }


    @Override
    public LiteralClass getBaseLiteralClass()
    {
        return genUnsignedInt;
    }


    @Override
    public LiteralClass getCanonicalLiteralClass()
    {
        return xsdUnsignedInt;
    }


    @Override
    public ResourceClass getNonCanonicalLiteralClass()
    {
        return lexUnsignedInt;
    }
}
