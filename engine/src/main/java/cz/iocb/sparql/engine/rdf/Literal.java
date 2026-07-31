package cz.iocb.sparql.engine.rdf;

import java.util.Objects;



public abstract class Literal extends RdfTerm
{
    protected final String value;


    public Literal(String value)
    {
        this.value = value;
    }


    public abstract Iri getType();


    public String getValue()
    {
        return value;
    }


    @Override
    public String toString()
    {
        return "'" + value.replaceAll("(['\\\\])", "\\\\$1").replaceAll("\n", "\\\\n").replaceAll("\r", "\\\\r") + "'";
    }


    @Override
    public int hashCode()
    {
        return value.hashCode();
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(object == null || getClass() != object.getClass())
            return false;

        Literal literal = (Literal) object;

        if(!Objects.equals(value, literal.value))
            return false;

        return true;
    }
}
