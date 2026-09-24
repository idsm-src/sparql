package cz.iocb.sparql.engine.rdf;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.rdfLangStringIri;
import java.util.Objects;



/**
 * Language-tagged literal ({@code rdf:langString}); the tag is normalised to lower case.
 */
public class LangStringLiteral extends Literal
{
    /**
     * Lower-cased language tag.
     */
    protected final String tag;


    /**
     * Creates the literal; the tag is lower-cased.
     *
     * @param value the lexical form
     * @param tag the language tag
     */
    public LangStringLiteral(String value, String tag)
    {
        super(value);

        this.tag = tag.toLowerCase();
    }


    /**
     * Lower-cased language tag.
     *
     * @return lower-cased language tag
     */
    public String getTag()
    {
        return tag;
    }


    @Override
    public Iri getType()
    {
        return rdfLangStringIri;
    }


    @Override
    public String toString()
    {
        return super.toString() + "@" + tag;
    }


    @Override
    public int hashCode()
    {
        return Objects.hash(value, tag);
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(object == null || getClass() != object.getClass() || !super.equals(object))
            return false;

        LangStringLiteral other = (LangStringLiteral) object;

        return Objects.equals(tag, other.tag);
    }
}
