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
 * Object of a triple followed by an annotation ({@code object ~ reifier {| ... |}}), the shorthand for reifying the
 * triple whose object it is. The expansion emits the triple itself, the triple
 * {@code reifier rdf:reifies <<( subject predicate object )>>} for every {@link Reifier} and for every
 * {@link AnnotationBlock} that does not directly follow a reifier (using a fresh blank node then), and the properties
 * of every annotation block with its reifier as the subject.
 *
 * <p>
 * This is a {@link ComplexElement}, so it won't appear in the output of the {@link Parser}.
 *
 * <p>
 * Corresponds to the rules [86] Object and [93] ObjectPath with a non-empty annotation in the SPARQL grammar.
 */
public class AnnotatedNode extends BaseComplexNode implements ComplexNode, ComplexElement
{
    /**
     * The annotated object, possibly with syntax sugar.
     */
    private final ComplexNode node;

    /**
     * Elements of the annotation in order.
     */
    private final List<Annotation> annotations;


    /**
     * Creates the annotated object.
     *
     * @param node the object node
     * @param annotations elements of the annotation
     */
    public AnnotatedNode(ComplexNode node, Collection<Annotation> annotations)
    {
        this.node = node;
        this.annotations = new ArrayList<>(annotations);
    }


    /**
     * The annotated object, possibly with syntax sugar.
     *
     * @return the annotated object, possibly with syntax sugar
     */
    public ComplexNode getNode()
    {
        return node;
    }


    /**
     * Elements of the annotation in order (modifiable).
     *
     * @return elements of the annotation in order (modifiable)
     */
    public List<Annotation> getAnnotations()
    {
        return annotations;
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
