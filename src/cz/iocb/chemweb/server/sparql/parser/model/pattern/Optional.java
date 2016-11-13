package cz.iocb.chemweb.server.sparql.parser.model.pattern;

import cz.iocb.chemweb.server.sparql.parser.BaseElement;
import cz.iocb.chemweb.server.sparql.parser.ElementVisitor;



/**
 * Pattern used to contain a pattern that may not succeed ({@link #getPattern}).
 * 
 * <p>
 * Corresponds to the rule [57] OptionalGraphPattern in the SPARQL grammar.
 */
public class Optional extends BaseElement implements Pattern
{
    private GraphPattern pattern;

    public Optional()
    {
        this(null);
    }

    public Optional(GraphPattern pattern)
    {
        this.pattern = pattern;
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
