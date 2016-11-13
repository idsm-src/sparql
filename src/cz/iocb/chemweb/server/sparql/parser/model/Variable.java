package cz.iocb.chemweb.server.sparql.parser.model;

import cz.iocb.chemweb.server.sparql.parser.BaseComplexNode;
import cz.iocb.chemweb.server.sparql.parser.ElementVisitor;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Verb;



/**
 * Represents a variable.
 */
public final class Variable extends BaseComplexNode implements VarOrIri, Verb
{
    private final String name;

    public Variable(String name)
    {
        if(name == null || name.isEmpty())
            throw new IllegalArgumentException();

        if(name.startsWith("?") || name.startsWith("$"))
            name = name.substring(1);

        this.name = name;
    }

    /**
     * The variable name, without the '?' (or '$') prefix.
     */
    public String getName()
    {
        return name;
    }

    @Override
    public boolean equals(Object o)
    {
        if(this == o)
            return true;
        if(o == null || getClass() != o.getClass())
            return false;

        Variable variable = (Variable) o;

        return name.equals(variable.name);
    }

    @Override
    public int hashCode()
    {
        return name.hashCode();
    }

    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
