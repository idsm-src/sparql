package cz.iocb.sparql.engine.model;

import cz.iocb.sparql.engine.model.base.BaseComplexNode;
import cz.iocb.sparql.engine.model.triple.Verb;
import cz.iocb.sparql.engine.model.visitor.ElementVisitor;



/**
 * Represents a variable.
 */
public final class VariableNode extends BaseComplexNode implements VarOrIri, Verb, VariableOrBlankNode
{
    /**
     * Name without the leading {@code ?} or {@code $}.
     */
    private final String name;

    /**
     * Scope the variable is bound in, or null.
     */
    private final String scope;


    /**
     * Creates a variable without a scope; a leading {@code ?} or {@code $} is stripped from the name.
     *
     * @param name the variable name
     */
    public VariableNode(String name)
    {
        if(name.startsWith("$") || name.startsWith("?"))
            name = name.substring(1);

        this.scope = null;
        this.name = name;
    }


    /**
     * Creates a variable bound in the given scope; a leading {@code ?} or {@code $} is stripped from the name.
     *
     * @param scope name of the scope
     * @param name the variable name
     */
    public VariableNode(String scope, String name)
    {
        if(name.startsWith("$") || name.startsWith("?"))
            name = name.substring(1);

        this.scope = scope;
        this.name = name;
    }


    /**
     * Name without the leading {@code ?} or {@code $}.
     *
     * @return name without the leading {@code ?} or {@code $}
     */
    public String getName()
    {
        return name;
    }


    /**
     * Name of the scope the variable is bound in (see {@link cz.iocb.sparql.engine.parser.VariableScopes}), or null.
     * Variables with the same name but different scopes are distinct variables.
     *
     * @return name of the scope the variable is bound in (see {@link cz.iocb.sparql.engine.parser.VariableScopes}), or
     *         null
     */
    public String getScope()
    {
        return scope;
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

        VariableNode variable = (VariableNode) object;

        if(!name.equals(variable.name))
            return false;

        if(scope == null && variable.scope != null || !scope.equals(variable.scope))
            return false;

        return true;
    }


    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
