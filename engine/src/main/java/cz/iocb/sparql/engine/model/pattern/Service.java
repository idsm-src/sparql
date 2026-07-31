package cz.iocb.sparql.engine.model.pattern;

import cz.iocb.sparql.engine.model.VarOrIri;
import cz.iocb.sparql.engine.model.VariableNode;
import cz.iocb.sparql.engine.model.visitor.ElementVisitor;



/**
 * Pattern used to perform a sub-query ({@link #getPattern}) on a specific named service ({@link #getName}).
 *
 * <p>
 * Corresponds to the rule [59] ServiceGraphPattern in the SPARQL grammar.
 */
public class Service extends PatternElement implements Pattern
{
    private final VarOrIri name;
    private final GraphPattern pattern;
    private final boolean silent;


    public Service(VarOrIri name, GraphPattern pattern, boolean silent)
    {
        this.name = name;
        this.pattern = pattern;
        this.silent = silent;

        if(name instanceof VariableNode variable)
            variablesInScope.add(variable);

        variablesInScope.addAll(pattern.getVariablesInScope());
    }


    public VarOrIri getName()
    {
        return name;
    }


    public GraphPattern getPattern()
    {
        return pattern;
    }


    public boolean isSilent()
    {
        return silent;
    }


    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
