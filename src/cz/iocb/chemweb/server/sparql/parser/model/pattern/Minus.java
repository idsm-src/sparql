package cz.iocb.chemweb.server.sparql.parser.model.pattern;

import cz.iocb.chemweb.server.sparql.parser.BaseElement;
import cz.iocb.chemweb.server.sparql.parser.ElementVisitor;



/**
 * Pattern used to compute set difference with the given pattern ( {@link #getPattern}).
 *
 * <p>
 * Corresponds to the rule [66] MinusGraphPattern in the SPARQL grammar.
 */
public class Minus extends BaseElement implements Pattern
{
    private GraphPattern pattern;

    public Minus()
    {
        this(null);
    }

    public Minus(GraphPattern pattern)
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
