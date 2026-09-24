package cz.iocb.sparql.engine.model.triple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import cz.iocb.sparql.engine.model.base.BaseElement;
import cz.iocb.sparql.engine.model.base.ComplexElement;
import cz.iocb.sparql.engine.model.visitor.ComplexElementVisitor;
import cz.iocb.sparql.engine.model.visitor.ElementVisitor;
import cz.iocb.sparql.engine.parser.Parser;



/**
 * Triple that can contain syntax sugar.
 *
 * <p>
 * Only {@link Triple}s can appear in the output of {@link Parser}, never {@link ComplexTriple}s.
 *
 * <p>
 * Corresponds to the rule [81] TriplesSameSubjectPath in the SPARQL grammar.
 */
public class ComplexTriple extends BaseElement implements ComplexElement
{
    /**
     * The subject, possibly with syntax sugar.
     */
    private ComplexNode node;

    /**
     * Properties of the subject.
     */
    private List<Property> properties;


    /**
     * Creates the triple.
     *
     * @param node the subject node
     * @param properties the properties
     */
    public ComplexTriple(ComplexNode node, Collection<Property> properties)
    {
        this.node = node;
        this.properties = Collections.unmodifiableList(new ArrayList<>(properties));
    }


    /**
     * The subject, possibly with syntax sugar.
     *
     * @return the subject, possibly with syntax sugar
     */
    public ComplexNode getNode()
    {
        return node;
    }


    /**
     * Sets the subject.
     *
     * @param node the subject node
     */
    public void setNode(ComplexNode node)
    {
        this.node = node;
    }


    /**
     * Properties of the subject.
     *
     * @return properties of the subject
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
