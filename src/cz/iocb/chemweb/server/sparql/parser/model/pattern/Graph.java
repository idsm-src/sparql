package cz.iocb.chemweb.server.sparql.parser.model.pattern;

import cz.iocb.chemweb.server.sparql.parser.BaseElement;
import cz.iocb.chemweb.server.sparql.parser.ElementVisitor;
import cz.iocb.chemweb.server.sparql.parser.model.VarOrIri;



/**
 * Pattern used to perform a sub-query ({@link #getPattern}) on a specific named graph ({@link #getName}).
 *
 * <p>
 * Corresponds to the rule [58] GraphGraphPattern in the SPARQL grammar.
 */
public class Graph extends BaseElement implements Pattern
{
    private VarOrIri name;
    private GraphPattern pattern;

    public Graph()
    {
    }

    public Graph(VarOrIri name, GraphPattern pattern)
    {
        this.name = name;
        this.pattern = pattern;
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

    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
