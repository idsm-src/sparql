package cz.iocb.sparql.engine.model.triple;

import cz.iocb.sparql.engine.model.base.BaseElement;
import cz.iocb.sparql.engine.model.visitor.ElementVisitor;



/**
 * Path that cannot be traversed.
 *
 * <p>
 * Corresponds to the case with {@code !} of the rule [100] PathPrimary in the SPARQL grammar.
 */
public class NegatedPath extends BaseElement implements Path
{
    /**
     * The negated property set: an IRI, an inverse IRI, or an alternative of those.
     */
    private Path child;


    /**
     * Creates the negation of the child.
     *
     * @param child the child path
     */
    public NegatedPath(Path child)
    {
        setChild(child);
    }


    /**
     * The negated property set.
     *
     * @return the negated property set
     */
    public Path getChild()
    {
        return child;
    }


    /**
     * Sets the negated property set.
     *
     * @param child the child path
     */
    public void setChild(Path child)
    {
        this.child = child;
    }


    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
