package cz.iocb.chemweb.server.sparql.parser.model;

import java.util.concurrent.atomic.AtomicInteger;
import cz.iocb.chemweb.server.sparql.parser.BaseComplexNode;
import cz.iocb.chemweb.server.sparql.parser.ElementVisitor;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Verb;



/**
 * Represents a variable.
 */
public final class Variable extends BaseComplexNode implements VarOrIri, Verb, VariableOrBlankNode
{
    private static final String prefix = "@var";
    private static final AtomicInteger variableId = new AtomicInteger();
    private final String name;

    public Variable(String name)
    {
        if(name.startsWith("?") || name.startsWith("$"))
            name = name.substring(1);

        this.name = name;
    }

    public static Variable getNewVariable()
    {
        int id = variableId.incrementAndGet();
        return new Variable(prefix + id);
    }

    /**
     * The variable name, without the '?' (or '$') prefix.
     */
    @Override
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
