package cz.iocb.chemweb.server.sparql.parser.model.expression;

import cz.iocb.chemweb.server.sparql.parser.BaseElement;
import cz.iocb.chemweb.server.sparql.parser.ElementVisitor;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.GraphPattern;



/**
 * Represents an expression, that checks whether some pattern ( {@link #getPattern}) exists, or not
 * ({@link #isNegated}).
 *
 * <p>
 * Corresponds to the rules [125] ExistsFunc and [126] NotExistsFunc in the SPARQL grammar.
 */
public class ExistsExpression extends BaseElement implements Expression
{
    private GraphPattern pattern;
    private boolean negated;


    public ExistsExpression(GraphPattern pattern, boolean negated)
    {
        this.pattern = pattern;
        this.negated = negated;
    }


    public GraphPattern getPattern()
    {
        return pattern;
    }


    public void setPattern(GraphPattern pattern)
    {
        this.pattern = pattern;
    }


    public boolean isNegated()
    {
        return negated;
    }


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
