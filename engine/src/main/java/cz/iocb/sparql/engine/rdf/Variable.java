package cz.iocb.sparql.engine.rdf;

import java.util.Objects;



public class Variable extends RdfTerm
{
    private final String name;


    public Variable(String name)
    {
        this.name = name;
    }


    public String getName()
    {
        return name;
    }


    @Override
    public String toString()
    {
        return '$' + name;
    }


    @Override
    public int hashCode()
    {
        return Objects.hash(name);
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(object == null || getClass() != object.getClass())
            return false;

        Variable other = (Variable) object;

        return Objects.equals(name, other.name);
    }
}
