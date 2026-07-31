package cz.iocb.sparql.engine.model.pattern;

import cz.iocb.sparql.engine.model.VarOrIri;
import cz.iocb.sparql.engine.model.VariableNode;
import cz.iocb.sparql.engine.model.visitor.ElementVisitor;



/**
 * Pattern used to perform a sub-query ({@link #getPattern}) on a specific named graph ({@link #getName}).
 *
 * <p>
 * Corresponds to the rule [58] GraphGraphPattern in the SPARQL grammar.
 */
public class Graph extends PatternElement implements Pattern
{
    private final VarOrIri name;
    private final GraphPattern pattern;


    public Graph(VarOrIri name, GraphPattern pattern)
    {
        this.name = name;
        this.pattern = pattern;

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


    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
