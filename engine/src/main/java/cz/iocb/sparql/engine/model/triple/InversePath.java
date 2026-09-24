package cz.iocb.sparql.engine.model.triple;

import cz.iocb.sparql.engine.model.base.BaseElement;
import cz.iocb.sparql.engine.model.visitor.ElementVisitor;



/**
 * Path that has to be traversed in the opposite direction.
 *
 * <p>
 * Corresponds to the case with {@code ^} of the rule [92] PathEltOrInverse in the SPARQL grammar.
 */
public class InversePath extends BaseElement implements Path
{
    /**
     * The inverted path.
     */
    private Path child;


    /**
     * Creates the inverse of the child.
     *
     * @param child the child path
     */
    public InversePath(Path child)
    {
        setChild(child);
    }


    /**
     * The inverted path.
     *
     * @return the inverted path
     */
    public Path getChild()
    {
        return child;
    }


    /**
     * Sets the inverted path.
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
