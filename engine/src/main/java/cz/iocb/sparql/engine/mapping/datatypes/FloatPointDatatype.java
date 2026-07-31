package cz.iocb.sparql.engine.mapping.datatypes;

import java.util.regex.Pattern;
import cz.iocb.sparql.engine.rdf.Iri;



public abstract class FloatPointDatatype extends Datatype
{
    private static final Pattern validFormPattern = Pattern
            .compile("[-+]?([0-9]+(\\.[0-9]*)?|\\.[0-9]+)([Ee][-+]?[0-9]+)?|[-+]?INF|NaN");


    protected FloatPointDatatype(Iri typeIri)
    {
        super(typeIri);
    }


    @Override
    public boolean isValidForm(String value)
    {
        return validFormPattern.matcher(value).matches();
    }
}
