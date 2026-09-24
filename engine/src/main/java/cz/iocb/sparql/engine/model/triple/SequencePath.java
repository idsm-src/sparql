package cz.iocb.sparql.engine.model.triple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import cz.iocb.sparql.engine.model.base.BaseElement;
import cz.iocb.sparql.engine.model.visitor.ElementVisitor;



/**
 * Path, that contains two or more paths ({@link #getChildren}) that have to be traversed in order.
 *
 * <p>
 * Corresponds to the rule [90] PathSequence with at least one {@code /} in the SPARQL grammar.
 */
public class SequencePath extends BaseElement implements Path
{
    /**
     * Paths in traversal order.
     */
    private List<Path> children;


    /**
     * Creates an empty sequence to be filled later.
     */
    public SequencePath()
    {
        this.children = new ArrayList<>();
    }


    /**
     * Creates the sequence of the given paths.
     *
     * @param children the child paths
     */
    public SequencePath(Collection<Path> children)
    {
        this.children = new ArrayList<>(children);
    }


    /**
     * Paths in traversal order (modifiable).
     *
     * @return paths in traversal order (modifiable)
     */
    public List<Path> getChildren()
    {
        return children;
    }


    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
