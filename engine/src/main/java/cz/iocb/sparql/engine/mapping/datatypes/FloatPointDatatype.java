package cz.iocb.sparql.engine.mapping.datatypes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.unsupportedType;
import java.util.regex.Pattern;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.Literal;



public abstract sealed class FloatPointDatatype extends Datatype permits FloatDatatype, DoubleDatatype
{
    private static final Pattern validFloatPointPattern = Pattern
            .compile(WS + "([-+]?([0-9]+(\\.[0-9]*)?|\\.[0-9]+)([Ee][-+]?[0-9]+)?|[-+]?INF|NaN)" + WS);


    protected FloatPointDatatype(Iri typeIri)
    {
        super(typeIri);
    }


    protected abstract ResourceClass getNonCanonicalLiteralClass();


    @Override
    public ResourceClass getResourceClass(Literal literal)
    {
        assert typeIri.equals(literal.getType());

        if(!isValidForm(literal.getValue()))
            return unsupportedType;

        if(!isCanonicalForm(literal.getValue()))
            return getNonCanonicalLiteralClass();

        return getCanonicalLiteralClass();
    }


    @Override
    public boolean isValidForm(String value)
    {
        return validFloatPointPattern.matcher(value).matches();
    }
}
