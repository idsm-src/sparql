package cz.iocb.chemweb.server.sparql.parser.model;

import cz.iocb.chemweb.server.sparql.parser.BaseComplexNode;
import cz.iocb.chemweb.server.sparql.parser.ElementVisitor;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Verb;



/**
 * Represents a variable.
 */
public final class Variable extends BaseComplexNode implements VarOrIri, Verb, VariableOrBlankNode
{
    private final String name;


    public Variable(String name)
    {
        if(name.startsWith("$") || name.startsWith("?"))
            name = name.substring(1);

        this.name = name;
    }


    public String getName()
    {
        return name;
    }


    @Override
    public String getSqlName()
    {
        return name;
    }


    @Override
    public int hashCode()
    {
        return name.hashCode();
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(object == null || getClass() != object.getClass())
            return false;

        Variable variable = (Variable) object;

        return name.equals(variable.name);
    }


    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
