package cz.iocb.sparql.engine.mapping.datatypes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genFloat;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.lexFloat;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdFloat;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdFloatIri;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import info.adams.ryu.RyuFloat;



/**
 * The xsd:float datatype.
 */
public final class FloatDatatype extends FloatPointDatatype
{
    /**
     * Creates the datatype.
     */
    protected FloatDatatype()
    {
        super(xsdFloatIri);
    }


    @Override
    public LiteralClass getBaseLiteralClass()
    {
        return genFloat;
    }


    @Override
    public LiteralClass getCanonicalLiteralClass()
    {
        return xsdFloat;
    }


    @Override
    public ResourceClass getNonCanonicalLiteralClass()
    {
        return lexFloat;
    }


    @Override
    public String getCanonicalLexicalForm(String value)
    {
        assert isValidForm(value);

        value = getCollapsedForm(value);

        if(value.equals("INF"))
            return "INF";
        else if(value.equals("-INF"))
            return "-INF";

        return RyuFloat.floatToString(Float.parseFloat(value));
    }


    @Override
    public boolean isCanonicalForm(String value)
    {
        return value.equals(getCanonicalLexicalForm(value));
    }
}
