package cz.iocb.chemweb.server.sparql.parser.model.triple;

import cz.iocb.chemweb.server.sparql.parser.BaseElement;
import cz.iocb.chemweb.server.sparql.parser.ElementVisitor;



/**
 * Path that has to be traversed in the opposite direction.
 *
 * <p>
 * Corresponds to the case with {@code ^} of the rule [92] PathEltOrInverse in the SPARQL grammar.
 */
public class InversePath extends BaseElement implements Path
{
    private Path child;

    public InversePath(Path child)
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
