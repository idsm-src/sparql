package cz.iocb.sparql.engine.rdf;

import java.util.Objects;



/**
 * Literal with an explicit datatype IRI.
 */
public class TypedLiteral extends Literal
{
    /**
     * Datatype IRI.
     */
    private final Iri type;


    /**
     * Creates the literal.
     *
     * @param value the lexical form
     * @param typeIri the datatype IRI
     */
    public TypedLiteral(String value, Iri typeIri)
    {
        super(value);

        this.type = typeIri;
    }


    @Override
    public Iri getType()
    {
        return type;
    }


    @Override
    public String toString()
    {
        return super.toString() + "@" + "^^" + type;
    }


    @Override
    public int hashCode()
    {
        return Objects.hash(value, type);
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(object == null || getClass() != object.getClass() || !super.equals(object))
            return false;

        TypedLiteral other = (TypedLiteral) object;

        return Objects.equals(type, other.type);
    }
}
