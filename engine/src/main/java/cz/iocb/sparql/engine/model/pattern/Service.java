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
    /**
     * Endpoint: a variable or an IRI.
     */
    private final VarOrIri name;

    /**
     * Pattern sent to the endpoint.
     */
    private final GraphPattern pattern;

    /**
     * SILENT modifier.
     */
    private final boolean silent;


    /**
     * Creates the pattern; a variable endpoint and the pattern's variables become in scope.
     *
     * @param name the endpoint
     * @param pattern the WHERE clause
     * @param silent the SILENT modifier
     */
    public Service(VarOrIri name, GraphPattern pattern, boolean silent)
    {
        this.name = name;
        this.pattern = pattern;
        this.silent = silent;

        if(name instanceof VariableNode variable)
            variablesInScope.add(variable);

        variablesInScope.addAll(pattern.getVariablesInScope());
    }


    /**
     * Endpoint: a variable or an IRI.
     *
     * @return endpoint: a variable or an IRI
     */
    public VarOrIri getName()
    {
        return name;
    }


    /**
     * Pattern sent to the endpoint.
     *
     * @return pattern sent to the endpoint
     */
    public GraphPattern getPattern()
    {
        return pattern;
    }


    /**
     * True for {@code SERVICE SILENT}: a failure of the remote endpoint is ignored and the pattern behaves as a single
     * empty solution instead of raising an error.
     *
     * @return true for {@code SERVICE SILENT}, false otherwise
     */
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
