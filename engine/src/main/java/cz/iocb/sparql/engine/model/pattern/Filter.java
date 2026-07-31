package cz.iocb.sparql.engine.model.pattern;

import cz.iocb.sparql.engine.model.expression.Expression;
import cz.iocb.sparql.engine.model.visitor.ElementVisitor;



/**
 * Pattern that filters results to those that satisfy a constraint ( {@link #getConstraint}).
 *
 * <p>
 * Corresponds to the rule [68] Filter in the SPARQL grammar.
 */
public class Filter extends PatternElement implements Pattern
{
    private final Expression constraint;


    public Filter(Expression constraint)
    {
        this.constraint = constraint;
    }


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
