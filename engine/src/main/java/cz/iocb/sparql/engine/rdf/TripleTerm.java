package cz.iocb.sparql.engine.rdf;

import java.util.Objects;



/**
 * Triple term ({@code <<( subject predicate object )>>}): an RDF term denoting a triple. In data, the subject is an
 * IRI, a blank node or a triple term, the predicate an IRI and the object any term; in patterns, the components may
 * also be variables.
 */
public final class TripleTerm extends RdfTerm
{
    /**
     * The subject.
     */
    private final RdfTerm subject;

    /**
     * The predicate.
     */
    private final RdfTerm predicate;

    /**
     * The object.
     */
    private final RdfTerm object;


    /**
     * Creates the triple term.
     *
     * @param subject the subject
     * @param predicate the predicate
     * @param object the object
     */
    public TripleTerm(RdfTerm subject, RdfTerm predicate, RdfTerm object)
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
    public RdfTerm getSubject()
    {
        return subject;
    }


    /**
     * The predicate.
     *
     * @return the predicate
     */
    public RdfTerm getPredicate()
    {
        return predicate;
    }


    /**
     * The object.
     *
     * @return the object
     */
    public RdfTerm getObject()
    {
        return object;
    }


    /**
     * True if some component, those of nested triple terms included, is a variable, i.e. the term is a pattern rather
     * than a constant.
     *
     * @return true if some component, those of nested triple terms included, is a variable, false otherwise
     */
    public boolean hasVariable()
    {
        return hasVariable(subject) || hasVariable(predicate) || hasVariable(object);
    }


    /**
     * True if the component is a variable or a triple term with a variable.
     *
     * @param component the component
     * @return true if the component is a variable or a triple term with a variable, false otherwise
     */
    private static boolean hasVariable(RdfTerm component)
    {
        return component instanceof Variable || component instanceof TripleTerm triple && triple.hasVariable();
    }


    /**
     * SPARQL rendering {@code <<( subject predicate object )>>} of the components' renderings.
     */
    @Override
    public String toString()
    {
        return "<<( " + subject + " " + predicate + " " + object + " )>>";
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

        TripleTerm other = (TripleTerm) object;

        return Objects.equals(subject, other.subject) && Objects.equals(predicate, other.predicate)
                && Objects.equals(this.object, other.object);
    }
}
