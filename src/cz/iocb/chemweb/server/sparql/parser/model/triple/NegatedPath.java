package cz.iocb.chemweb.server.sparql.parser.model.triple;

import cz.iocb.chemweb.server.sparql.parser.BaseElement;
import cz.iocb.chemweb.server.sparql.parser.ElementVisitor;



/**
 * Path that cannot be traversed.
 * 
 * <p>
 * Corresponds to the case with {@code !} of the rule [94] PathPrimary in the SPARQL grammar.
 */
public class NegatedPath extends BaseElement implements Path
{
    private Path child;

    public NegatedPath(Path child)
    {
        setChild(child);
    }

    public Path getChild()
    {
        return child;
    }

    public void setChild(Path child)
    {
        if(child == null)
            throw new IllegalArgumentException();

        this.child = child;
    }

    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
