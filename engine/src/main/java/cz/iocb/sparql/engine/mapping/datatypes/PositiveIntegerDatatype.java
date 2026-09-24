package cz.iocb.sparql.engine.mapping.datatypes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genPositiveInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.lexPositiveInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdPositiveInteger;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdPositiveIntegerIri;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;



/**
 * The xsd:positiveInteger datatype.
 */
public final class PositiveIntegerDatatype extends VariableSizeIntegerDataType
{
    /**
     * Creates the datatype.
     */
    public PositiveIntegerDatatype()
    {
        super(xsdPositiveIntegerIri, Variant.POSITIVE);
    }


    @Override
    public LiteralClass getBaseLiteralClass()
    {
        return genPositiveInteger;
    }


    @Override
    public LiteralClass getCanonicalLiteralClass()
    {
        return xsdPositiveInteger;
    }


    @Override
    public ResourceClass getNonCanonicalLiteralClass()
    {
        return lexPositiveInteger;
    }
}
