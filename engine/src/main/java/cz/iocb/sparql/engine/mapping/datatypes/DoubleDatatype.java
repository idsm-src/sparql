package cz.iocb.sparql.engine.mapping.datatypes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genDouble;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.lexDouble;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDouble;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDoubleIri;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import info.adams.ryu.RyuDouble;



public final class DoubleDatatype extends FloatPointDatatype
{
    protected DoubleDatatype()
    {
        super(xsdDoubleIri);
    }


    @Override
    public LiteralClass getBaseLiteralClass()
    {
        return genDouble;
    }


    @Override
    public LiteralClass getCanonicalLiteralClass()
    {
        return xsdDouble;
    }


    @Override
    public ResourceClass getNonCanonicalLiteralClass()
    {
        return lexDouble;
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

        return RyuDouble.doubleToString(Double.parseDouble(value));
    }


    @Override
    public boolean isCanonicalForm(String value)
    {
        return value.equals(getCanonicalLexicalForm(value));
    }
}
