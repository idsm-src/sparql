package cz.iocb.sparql.engine.model.pattern;

import cz.iocb.sparql.engine.model.visitor.ElementVisitor;



/**
 * Pattern used to contain a pattern that may not succeed ({@link #getPattern}).
 *
 * <p>
 * Corresponds to the rule [61] OptionalGraphPattern in the SPARQL grammar.
 */
public class Optional extends PatternElement implements Pattern
{
    /**
     * The optional pattern.
     */
    private final GraphPattern pattern;


    /**
     * Creates the pattern; the variables of the optional pattern become in scope.
     *
     * @param pattern the WHERE clause
     */
    public Optional(GraphPattern pattern)
    {
        this.pattern = pattern;

        variablesInScope.addAll(pattern.getVariablesInScope());
    }


    /**
     * The optional pattern.
     *
     * @return the optional pattern
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
