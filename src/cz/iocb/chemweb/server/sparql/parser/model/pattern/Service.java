package cz.iocb.chemweb.server.sparql.parser.model.pattern;

import cz.iocb.chemweb.server.sparql.parser.BaseElement;
import cz.iocb.chemweb.server.sparql.parser.ElementVisitor;
import cz.iocb.chemweb.server.sparql.parser.model.VarOrIri;



/**
 * Pattern used to perform a sub-query ({@link #getPattern}) on a specific named service ({@link #getName}).
 *
 * <p>
 * Corresponds to the rule [59] ServiceGraphPattern in the SPARQL grammar.
 */
public class Service extends BaseElement implements Pattern
{
    private VarOrIri name;
    private GraphPattern pattern;
    private boolean silent;

    public Service()
    {
        silent = false;
    }

    public Service(VarOrIri name, GraphPattern pattern, boolean silent)
    {
        this.name = name;
        this.pattern = pattern;
        this.silent = silent;
    }

    public VarOrIri getName()
    {
        return name;
    }

    public void setName(VarOrIri name)
    {
        this.name = name;
    }

    public GraphPattern getPattern()
    {
        return pattern;
    }

    public void setPattern(GraphPattern pattern)
    {
        this.pattern = pattern;
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
