package cz.iocb.sparql.engine.model.triple;

import cz.iocb.sparql.engine.model.base.BaseElement;
import cz.iocb.sparql.engine.model.visitor.ElementVisitor;



/**
 * Path that contains a path ({@link #getChild}) enclosed in parentheses.
 *
 * <p>
 * Corresponds to cases with parentheses in rules [100] PathPrimary and [101] PathNegatedPropertySet in the SPARQL
 * grammar.
 */
public class BracketedPath extends BaseElement implements Path
{
    /**
     * The enclosed path.
     */
    private Path child;


    /**
     * Creates the path around the child.
     *
     * @param child the child path
     */
    public BracketedPath(Path child)
    {
        setChild(child);
    }


    /**
     * The enclosed path.
     *
     * @return the enclosed path
     */
    public Path getChild()
    {
        return child;
    }


    /**
     * Sets the enclosed path.
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
