package cz.iocb.sparql.engine.model.triple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import cz.iocb.sparql.engine.model.base.BaseComplexNode;
import cz.iocb.sparql.engine.model.base.ComplexElement;
import cz.iocb.sparql.engine.model.visitor.ComplexElementVisitor;
import cz.iocb.sparql.engine.model.visitor.ElementVisitor;



/**
 * Describes an anonymous blank node with zero or more properties ( {@link #getProperties}).
 *
 * <p>
 * Corresponds to rules [106] BlankNodePropertyListPath and [184] ANON in the SPARQL grammar.
 */
public class BlankNodePropertyList extends BaseComplexNode implements ComplexNode, ComplexElement
{
    /**
     * Properties of the anonymous node.
     */
    private final List<Property> properties;


    /**
     * Creates an anonymous node without properties ({@code []}).
     */
    public BlankNodePropertyList()
    {
        this.properties = new ArrayList<>();
    }


    /**
     * Creates an anonymous node with the given properties.
     *
     * @param properties the properties
     */
    public BlankNodePropertyList(Collection<Property> properties)
    {
        this.properties = new ArrayList<>(properties);
    }


    /**
     * Properties of the anonymous node (modifiable).
     *
     * @return properties of the anonymous node (modifiable)
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
