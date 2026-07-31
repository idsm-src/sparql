package cz.iocb.sparql.engine.model.pattern;

import cz.iocb.sparql.engine.model.visitor.ElementVisitor;



/**
 * Pattern used to compute set difference with the given pattern ( {@link #getPattern}).
 *
 * <p>
 * Corresponds to the rule [66] MinusGraphPattern in the SPARQL grammar.
 */
public class Minus extends PatternElement implements Pattern
{
    private final GraphPattern pattern;


    public Minus(GraphPattern pattern)
    {
        this.pattern = pattern;
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
