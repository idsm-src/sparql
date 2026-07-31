package cz.iocb.sparql.engine.rdf;

import java.util.Objects;



public class TypedLiteral extends Literal
{
    private final Iri type;


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
