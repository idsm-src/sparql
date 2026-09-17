package cz.iocb.sparql.engine.mapping.datatypes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.lexInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInteger;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerIri;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;



public final class IntegerDatatype extends VariableSizeIntegerDataType
{
    public IntegerDatatype()
    {
        super(xsdIntegerIri, Variant.FULL);
    }


    @Override
    public LiteralClass getBaseLiteralClass()
    {
        return genInteger;
    }


    @Override
    public LiteralClass getCanonicalLiteralClass()
    {
        return xsdInteger;
    }


    @Override
    public ResourceClass getNonCanonicalLiteralClass()
    {
        return lexInteger;
    }
}
