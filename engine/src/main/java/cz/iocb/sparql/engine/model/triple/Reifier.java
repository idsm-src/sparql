package cz.iocb.sparql.engine.model.triple;

import cz.iocb.sparql.engine.model.base.BaseElement;
import cz.iocb.sparql.engine.model.visitor.ComplexElementVisitor;
import cz.iocb.sparql.engine.model.visitor.ElementVisitor;
import cz.iocb.sparql.engine.parser.Parser;



/**
 * Reifier ({@code ~ node}) of an annotation: the node that reifies the annotated triple, i.e. the subject of the triple
 * {@code node rdf:reifies <<( subject predicate object )>>} added by the expansion. When the node is omitted, a fresh
 * blank node is used.
 *
 * <p>
 * This is a {@link cz.iocb.sparql.engine.model.base.ComplexElement}, so it won't appear in the output of the
 * {@link Parser}.
 *
 * <p>
 * Corresponds to the rule [70] Reifier in the SPARQL grammar.
 */
public class Reifier extends BaseElement implements Annotation
{
    /**
     * The reifier node: a variable, an IRI or a blank node; null when omitted.
     */
    private final Node node;


    /**
     * Creates the reifier.
     *
     * @param node the reifier node, or null when omitted
     */
    public Reifier(Node node)
    {
        this.node = node;
    }


    /**
     * The reifier node: a variable, an IRI or a blank node; null when omitted, which stands for a fresh blank node.
     *
     * @return the reifier node: a variable, an IRI or a blank node; null when omitted, which stands for a fresh blank
     *         node
     */
    public Node getNode()
    {
        return node;
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
