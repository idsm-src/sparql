package cz.iocb.sparql.engine.model.pattern;

import cz.iocb.sparql.engine.model.expression.Expression;
import cz.iocb.sparql.engine.model.visitor.ElementVisitor;



/**
 * Pattern that filters results to those that satisfy a constraint ( {@link #getConstraint}).
 *
 * <p>
 * Corresponds to the rule [74] Filter in the SPARQL grammar.
 */
public class Filter extends PatternElement implements Pattern
{
    /**
     * The filter expression.
     */
    private final Expression constraint;


    /**
     * Creates the pattern; a filter brings no variable into scope.
     *
     * @param constraint the filter expression
     */
    public Filter(Expression constraint)
    {
        this.constraint = constraint;
    }


    /**
     * The filter expression.
     *
     * @return the filter expression
     */
    public Expression getConstraint()
    {
        return constraint;
    }


    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
