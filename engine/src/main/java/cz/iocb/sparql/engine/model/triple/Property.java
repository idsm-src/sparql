package cz.iocb.sparql.engine.model.triple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import cz.iocb.sparql.engine.model.base.BaseElement;
import cz.iocb.sparql.engine.model.base.ComplexElement;
import cz.iocb.sparql.engine.model.visitor.ComplexElementVisitor;
import cz.iocb.sparql.engine.model.visitor.ElementVisitor;
import cz.iocb.sparql.engine.parser.Parser;



/**
 * Property of the subject, that is, a single predicate ({@link #getVerb}) with one or more objects
 * ({@link #getObjects}).
 *
 * <p>
 * This is a {@link ComplexElement}, so it won't appear in the output of the {@link Parser}.
 */
public class Property extends BaseElement implements ComplexElement
{
    /**
     * The predicate.
     */
    private Verb verb;

    /**
     * Objects of the predicate.
     */
    private List<ComplexNode> objects;


    /**
     * Creates an empty property to be filled later.
     */
    public Property()
    {
        this.objects = new ArrayList<>();
    }


    /**
     * Creates the property with a single object.
     *
     * @param verb the predicate
     * @param object the object node
     */
    public Property(Verb verb, ComplexNode object)
    {
        this();
        this.verb = verb;
        this.objects.add(object);
    }


    /**
     * Creates the property with the given objects.
     *
     * @param verb the predicate
     * @param objects the objects
     */
    public Property(Verb verb, Collection<ComplexNode> objects)
    {
        this.verb = verb;
        this.objects = new ArrayList<>(objects);
    }


    /**
     * The predicate.
     *
     * @return the predicate
     */
    public Verb getVerb()
    {
        return verb;
    }


    /**
     * Sets the predicate.
     *
     * @param verb the predicate
     */
    public void setVerb(Verb verb)
    {
        this.verb = verb;
    }


    /**
     * Objects of the predicate (modifiable).
     *
     * @return objects of the predicate (modifiable)
     */
    public List<ComplexNode> getObjects()
    {
        return objects;
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
