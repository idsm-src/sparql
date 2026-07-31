package cz.iocb.sparql.engine.rdf;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.rdfLangStringIri;
import java.util.Objects;



public class LangStringLiteral extends Literal
{
    protected final String tag;


    public LangStringLiteral(String value, String tag)
    {
        super(value);

        this.tag = tag.toLowerCase();
    }


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
