package cz.iocb.sparql.engine.mapping.datatypes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genNonPositiveInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.lexNonPositiveInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdNonPositiveInteger;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdNonPositiveIntegerIri;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;



/**
 * The xsd:nonPositiveInteger datatype.
 */
public final class NonPositiveIntegerDatatype extends VariableSizeIntegerDataType
{
    /**
     * Creates the datatype.
     */
    public NonPositiveIntegerDatatype()
    {
        super(xsdNonPositiveIntegerIri, Variant.NONPOSITIVE);
    }


    @Override
    public LiteralClass getBaseLiteralClass()
    {
        return genNonPositiveInteger;
    }


    @Override
    public LiteralClass getCanonicalLiteralClass()
    {
        return xsdNonPositiveInteger;
    }


    @Override
    public ResourceClass getNonCanonicalLiteralClass()
    {
        return lexNonPositiveInteger;
    }
}
