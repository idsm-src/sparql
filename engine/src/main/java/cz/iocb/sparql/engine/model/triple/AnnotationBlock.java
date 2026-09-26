package cz.iocb.sparql.engine.model.triple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import cz.iocb.sparql.engine.model.base.BaseElement;
import cz.iocb.sparql.engine.model.visitor.ComplexElementVisitor;
import cz.iocb.sparql.engine.model.visitor.ElementVisitor;
import cz.iocb.sparql.engine.parser.Parser;



/**
 * Annotation block ({@code {| ... |}}) of an annotation: properties whose subject is the reifier of the annotated
 * triple. The reifier is the one of the directly preceding {@link Reifier}, or a fresh blank node when the block does
 * not directly follow a reifier.
 *
 * <p>
 * This is a {@link cz.iocb.sparql.engine.model.base.ComplexElement}, so it won't appear in the output of the
 * {@link Parser}.
 *
 * <p>
 * Corresponds to the rules [110] AnnotationBlockPath and [112] AnnotationBlock in the SPARQL grammar.
 */
public class AnnotationBlock extends BaseElement implements Annotation
{
    /**
     * Properties of the reifier.
     */
    private final List<Property> properties;


    /**
     * Creates the block with the given properties.
     *
     * @param properties the properties
     */
    public AnnotationBlock(Collection<Property> properties)
    {
        this.properties = new ArrayList<>(properties);
    }


    /**
     * Properties of the reifier (modifiable).
     *
     * @return properties of the reifier (modifiable)
     */
    public List<Property> getProperties()
    {
        return properties;
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
