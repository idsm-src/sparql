package cz.iocb.sparql.engine.rdf;

import java.util.Objects;



/**
 * Query variable as seen by the translator, identified by its name (scope-qualified by
 * {@link cz.iocb.sparql.engine.translator.TermGenerator}).
 */
public class Variable extends RdfTerm
{
    /**
     * Name of the variable.
     */
    private final String name;


    /**
     * Creates the variable.
     *
     * @param name the variable name
     */
    public Variable(String name)
    {
        this.name = name;
    }


    /**
     * Name of the variable (scope-qualified when it comes from a named scope).
     *
     * @return name of the variable (scope-qualified when it comes from a named scope)
     */
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
