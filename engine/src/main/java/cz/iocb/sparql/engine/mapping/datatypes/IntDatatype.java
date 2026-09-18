package cz.iocb.sparql.engine.mapping.datatypes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genInt;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.lexInt;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntIri;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;



public final class IntDatatype extends FixedSizeIntegerDatatype
{
    public IntDatatype()
    {
        super(xsdIntIri, Integer.toString(Integer.MIN_VALUE), Integer.toString(Integer.MAX_VALUE));
    }


    @Override
    public LiteralClass getBaseLiteralClass()
    {
        return genInt;
    }


    @Override
    public LiteralClass getCanonicalLiteralClass()
    {
        return xsdInt;
    }


    @Override
    public ResourceClass getNonCanonicalLiteralClass()
    {
        return lexInt;
    }
}
