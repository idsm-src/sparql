package cz.iocb.sparql.engine.model.expression;

import cz.iocb.sparql.engine.model.base.BaseElement;
import cz.iocb.sparql.engine.model.pattern.GraphPattern;
import cz.iocb.sparql.engine.model.visitor.ElementVisitor;



/**
 * Represents an expression, that checks whether some pattern ( {@link #getPattern}) exists, or not
 * ({@link #isNegated}).
 *
 * <p>
 * Corresponds to the rules [125] ExistsFunc and [126] NotExistsFunc in the SPARQL grammar.
 */
public class ExistsExpression extends BaseElement implements Expression
{
    /**
     * Pattern whose existence is tested.
     */
    private GraphPattern pattern;

    /**
     * True for NOT EXISTS.
     */
    private boolean negated;


    /**
     * Creates the expression.
     *
     * @param pattern the WHERE clause
     * @param negated whether the test is negated
     */
    public ExistsExpression(GraphPattern pattern, boolean negated)
    {
        this.pattern = pattern;
        this.negated = negated;
    }


    /**
     * Pattern whose existence is tested.
     *
     * @return pattern whose existence is tested
     */
    public GraphPattern getPattern()
    {
        return pattern;
    }


    /**
     * Sets the tested pattern.
     *
     * @param pattern the WHERE clause
     */
    public void setPattern(GraphPattern pattern)
    {
        this.pattern = pattern;
    }


    /**
     * True for NOT EXISTS.
     *
     * @return true for NOT EXISTS, false otherwise
     */
    public boolean isNegated()
    {
        return negated;
    }


    /**
     * Sets whether the test is negated.
     *
     * @param negated whether the test is negated
     */
    public void setNegated(boolean negated)
    {
        this.negated = negated;
    }


    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
