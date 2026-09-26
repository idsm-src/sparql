package cz.iocb.sparql.engine.model.pattern;

import cz.iocb.sparql.engine.model.VarOrIri;
import cz.iocb.sparql.engine.model.VariableNode;
import cz.iocb.sparql.engine.model.visitor.ElementVisitor;



/**
 * Pattern used to perform a sub-query ({@link #getPattern}) on a specific named graph ({@link #getName}).
 *
 * <p>
 * Corresponds to the rule [62] GraphGraphPattern in the SPARQL grammar.
 */
public class Graph extends PatternElement implements Pattern
{
    /**
     * Graph name: a variable or an IRI.
     */
    private final VarOrIri name;

    /**
     * Pattern evaluated in the graph.
     */
    private final GraphPattern pattern;


    /**
     * Creates the pattern; a variable graph name and the pattern's variables become in scope.
     *
     * @param name the graph name
     * @param pattern the WHERE clause
     */
    public Graph(VarOrIri name, GraphPattern pattern)
    {
        this.name = name;
        this.pattern = pattern;

        if(name instanceof VariableNode variable)
            variablesInScope.add(variable);

        variablesInScope.addAll(pattern.getVariablesInScope());
    }


    /**
     * Graph name: a variable or an IRI.
     *
     * @return graph name: a variable or an IRI
     */
    public VarOrIri getName()
    {
        return name;
    }


    /**
     * Pattern evaluated in the graph.
     *
     * @return pattern evaluated in the graph
     */
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
