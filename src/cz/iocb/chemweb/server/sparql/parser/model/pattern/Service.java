package cz.iocb.chemweb.server.sparql.parser.model.pattern;

import cz.iocb.chemweb.server.sparql.parser.ElementVisitor;
import cz.iocb.chemweb.server.sparql.parser.model.VarOrIri;



/**
 * Pattern used to perform a sub-query ({@link #getPattern}) on a specific named service ({@link #getName}).
 *
 * <p>
 * Corresponds to the rule [59] ServiceGraphPattern in the SPARQL grammar.
 */
public class Service extends Graph
{
    private boolean silent;

    public Service()
    {
        silent = false;
    }

    public Service(VarOrIri name, GraphPattern pattern, boolean silent)
    {
        super(name, pattern);
        this.silent = silent;
    }

    public boolean isSilent()
    {
        return silent;
    }

    public void setSilent(boolean silent)
    {
        this.silent = silent;
    }

    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
