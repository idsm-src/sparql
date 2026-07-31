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
    private ComplexNode node;
    private List<Property> properties;


    public ComplexTriple(ComplexNode node, Collection<Property> properties)
    {
        this.node = node;
        this.properties = Collections.unmodifiableList(new ArrayList<>(properties));
    }


    public ComplexNode getNode()
    {
        return node;
    }


    public void setNode(ComplexNode node)
    {
        this.node = node;
    }


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
