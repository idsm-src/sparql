package cz.iocb.sparql.engine.mapping.datatypes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genNonNegativeInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.lexNonNegativeInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdNonNegativeInteger;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdNonNegativeIntegerIri;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;



/**
 * The xsd:nonNegativeInteger datatype.
 */
public final class NonNegativeIntegerDatatype extends VariableSizeIntegerDataType
{
    /**
     * Creates the datatype.
     */
    public NonNegativeIntegerDatatype()
    {
        super(xsdNonNegativeIntegerIri, Variant.NONNEGATIVE);
    }


    @Override
    public LiteralClass getBaseLiteralClass()
    {
        return genNonNegativeInteger;
    }


    @Override
    public LiteralClass getCanonicalLiteralClass()
    {
        return xsdNonNegativeInteger;
    }


    @Override
    public ResourceClass getNonCanonicalLiteralClass()
    {
        return lexNonNegativeInteger;
    }
}
