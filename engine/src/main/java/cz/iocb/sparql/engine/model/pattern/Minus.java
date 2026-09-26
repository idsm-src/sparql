package cz.iocb.sparql.engine.model.pattern;

import cz.iocb.sparql.engine.model.visitor.ElementVisitor;



/**
 * Pattern used to compute set difference with the given pattern ( {@link #getPattern}).
 *
 * <p>
 * Corresponds to the rule [72] MinusGraphPattern in the SPARQL grammar.
 */
public class Minus extends PatternElement implements Pattern
{
    /**
     * Pattern whose solutions are removed.
     */
    private final GraphPattern pattern;


    /**
     * Creates the pattern; MINUS brings no variable into scope.
     *
     * @param pattern the WHERE clause
     */
    public Minus(GraphPattern pattern)
    {
        this.pattern = pattern;
    }


    /**
     * Pattern whose solutions are removed.
     *
     * @return pattern whose solutions are removed
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
