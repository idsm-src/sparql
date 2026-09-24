package cz.iocb.sparql.engine.mapping.datatypes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.unsupportedType;
import java.util.regex.Pattern;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.Literal;



/**
 * Common base of xsd:float and xsd:double; the canonical form is the shortest round-trip representation produced by
 * Ryu, matching the output of the pgsparql extension.
 */
public abstract sealed class FloatPointDatatype extends Datatype permits FloatDatatype, DoubleDatatype
{
    /**
     * Valid lexical form: decimal notation with optional exponent, {@code INF}, {@code -INF} or {@code NaN}.
     */
    private static final Pattern validFloatPointPattern = Pattern
            .compile(WS + "([-+]?([0-9]+(\\.[0-9]*)?|\\.[0-9]+)([Ee][-+]?[0-9]+)?|[-+]?INF|NaN)" + WS);


    /**
     * Creates the datatype.
     *
     * @param typeIri the datatype IRI
     */
    protected FloatPointDatatype(Iri typeIri)
    {
        super(typeIri);
    }


    /**
     * Class of the valid but non-canonical literals.
     *
     * @return class of the valid but non-canonical literals
     */
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
