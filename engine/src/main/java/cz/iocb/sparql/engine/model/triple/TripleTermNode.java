package cz.iocb.sparql.engine.model.triple;

import java.util.Objects;
import cz.iocb.sparql.engine.model.VarOrIri;
import cz.iocb.sparql.engine.model.base.BaseComplexNode;
import cz.iocb.sparql.engine.model.expression.Expression;
import cz.iocb.sparql.engine.model.visitor.ElementVisitor;



/**
 * Triple term ({@code <<( subject predicate object )>>}): an RDF term denoting a triple. It appears as a node of a
 * triple pattern (typically as the object of {@code rdf:reifies}), as a constant of a VALUES clause and as an
 * expression.
 *
 * <p>
 * Corresponds to the following rules in the SPARQL grammar:
 * <ul>
 * <li>[119] TripleTerm, after the expansion of its {@link ComplexTripleTerm}
 * <li>[122] TripleTermData
 * <li>[137] ExprTripleTerm
 * </ul>
 */
public class TripleTermNode extends BaseComplexNode implements Node, Expression
{
    /**
     * The subject.
     */
    private final Node subject;

    /**
     * The predicate: an IRI or a variable.
     */
    private final VarOrIri predicate;

    /**
     * The object.
     */
    private final Node object;


    /**
     * Creates the triple term.
     *
     * @param subject the subject node
     * @param predicate the predicate
     * @param object the object node
     */
    public TripleTermNode(Node subject, VarOrIri predicate, Node object)
    {
        this.subject = subject;
        this.predicate = predicate;
        this.object = object;
    }


    /**
     * The subject.
     *
     * @return the subject
     */
    public Node getSubject()
    {
        return subject;
    }


    /**
     * The predicate: an IRI or a variable.
     *
     * @return the predicate: an IRI or a variable
     */
    public VarOrIri getPredicate()
    {
        return predicate;
    }


    /**
     * The object.
     *
     * @return the object
     */
    public Node getObject()
    {
        return object;
    }


    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }


    @Override
    public int hashCode()
    {
        return Objects.hash(subject, predicate, object);
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(object == null || getClass() != object.getClass())
            return false;

        TripleTermNode other = (TripleTermNode) object;

        return Objects.equals(subject, other.subject) && Objects.equals(predicate, other.predicate)
                && Objects.equals(this.object, other.object);
    }
}
