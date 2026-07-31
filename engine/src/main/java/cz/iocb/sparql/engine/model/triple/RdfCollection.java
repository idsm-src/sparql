package cz.iocb.sparql.engine.model.triple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import cz.iocb.sparql.engine.model.base.BaseComplexNode;
import cz.iocb.sparql.engine.model.base.ComplexElement;
import cz.iocb.sparql.engine.model.visitor.ComplexElementVisitor;
import cz.iocb.sparql.engine.model.visitor.ElementVisitor;
import cz.iocb.sparql.engine.parser.Parser;



/**
 * RDF collection, syntax sugar for a singly linked list.
 *
 * <p>
 * This is a {@link ComplexElement}, so it won't appear in the output of the {@link Parser}.
 *
 * <p>
 * Corresponds to rules [103] CollectionPath and [161] NIL in the SPARQL grammar.
 */
public class RdfCollection extends BaseComplexNode implements ComplexNode, ComplexElement
{
    private final List<ComplexNode> nodes;


    public RdfCollection()
    {
        this.nodes = new ArrayList<>();
    }


    public RdfCollection(Collection<ComplexNode> nodes)
    {
        this.nodes = new ArrayList<>(nodes);
    }


    public List<ComplexNode> getNodes()
    {
        return nodes;
    }


    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public <T> T accept(ComplexElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
