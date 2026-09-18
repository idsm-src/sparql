package cz.iocb.sparql.engine.mapping.datatypes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genNegativeInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.lexNegativeInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdNegativeInteger;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdNegativeIntegerIri;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;



public final class NegativeIntegerDatatype extends VariableSizeIntegerDataType
{
    public NegativeIntegerDatatype()
    {
        super(xsdNegativeIntegerIri, Variant.NEGATIVE);
    }


    @Override
    public LiteralClass getBaseLiteralClass()
    {
        return genNegativeInteger;
    }


    @Override
    public LiteralClass getCanonicalLiteralClass()
    {
        return xsdNegativeInteger;
    }


    @Override
    public ResourceClass getNonCanonicalLiteralClass()
    {
        return lexNegativeInteger;
    }
}
