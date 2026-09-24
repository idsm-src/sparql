package cz.iocb.sparql.engine.mapping;

import cz.iocb.sparql.engine.database.Conditions;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.Request;



/**
 * Describes how quads are obtained from the database: one {@link TermMapping} per position. A null graph mapping means
 * the quads belong to the default graph.
 */
public abstract class QuadMapping
{
    /**
     * Graph mapping; null for the default graph.
     */
    private final TermMapping graph;

    /**
     * Subject mapping.
     */
    private final TermMapping subject;

    /**
     * Predicate mapping.
     */
    private final TermMapping predicate;

    /**
     * Object mapping.
     */
    private final TermMapping object;


    /**
     * Creates the mapping; a null graph means the default graph.
     *
     * @param graph the graph mapping, or null for the default graph
     * @param subject the subject mapping
     * @param predicate the predicate mapping
     * @param object the object mapping
     */
    public QuadMapping(TermMapping graph, TermMapping subject, TermMapping predicate, TermMapping object)
    {
        //TODO: add support for parameterized graph mapping

        this.graph = graph;
        this.subject = subject;
        this.predicate = predicate;
        this.object = object;
    }


    /**
     * The same quads with the graph dropped, i.e. contributed to the default graph.
     *
     * @return the same quads with the graph dropped, i.e. contributed to the default graph
     */
    public abstract QuadMapping asDefaultGraphMapping();


    /**
     * The quads whose graph satisfies {@code graphConditions}, contributed to the default graph (for {@code FROM} on a
     * parametrised graph).
     *
     * @param graphConditions the conditions
     * @return the quads whose graph satisfies {@code graphConditions}, contributed to the default graph (for {@code
     *         FROM} on a parametrised graph)
     */
    public abstract QuadMapping asDefaultGraphMapping(Conditions graphConditions);


    /**
     * The quads whose graph satisfies {@code graphConditions}, kept in their named graph (for {@code FROM NAMED} on a
     * parametrised graph).
     *
     * @param graphConditions the conditions
     * @return the quads whose graph satisfies {@code graphConditions}, kept in their named graph (for {@code FROM
     *         NAMED} on a parametrised graph)
     */
    public abstract QuadMapping asNamedGraphMapping(Conditions graphConditions);


    /**
     * True if the mapping can produce a quad matching the pattern: each term matches its position (a null term matches
     * only a null graph mapping), and a variable used at two positions has compatible mappings there (equal constants,
     * or classes that are not disjoint).
     *
     * @param request the current request
     * @param graph the graph term
     * @param subject the subject term
     * @param predicate the predicate term
     * @param object the object term
     * @return true if the mapping can produce a quad matching the pattern, false otherwise
     */
    public boolean match(Request request, RdfTerm graph, RdfTerm subject, RdfTerm predicate, RdfTerm object)
    {
        if(!match(request, this.graph, graph))
            return false;

        if(!match(request, this.subject, subject))
            return false;

        if(!match(request, this.predicate, predicate))
            return false;

        if(!match(request, this.object, object))
            return false;

        if(!checkNodeCondition(request, graph, subject, getGraph(), getSubject()))
            return false;

        if(!checkNodeCondition(request, graph, predicate, getGraph(), getPredicate()))
            return false;

        if(!checkNodeCondition(request, graph, object, getGraph(), getObject()))
            return false;

        if(!checkNodeCondition(request, subject, predicate, getSubject(), getPredicate()))
            return false;

        if(!checkNodeCondition(request, subject, object, getSubject(), getObject()))
            return false;

        if(!checkNodeCondition(request, predicate, object, getPredicate(), getObject()))
            return false;

        return true;
    }


    /**
     * True if the term matches the mapping, a null term matching only a null mapping.
     *
     * @param request the current request
     * @param mapping the term mapping
     * @param term the RDF term
     * @return true if the term matches the mapping, a null term matching only a null mapping, false otherwise
     */
    private boolean match(Request request, TermMapping mapping, RdfTerm term)
    {
        if(term == null && mapping == null)
            return true;

        if(term == null || mapping == null)
            return false;

        return mapping.match(request, term);
    }


    /**
     * True unless the same variable is used at both positions and the two mappings cannot produce equal values
     * (different constants, or disjoint classes).
     *
     * @param request the current request
     * @param term1 the first term
     * @param term2 the second term
     * @param map1 mapping of the first term
     * @param map2 mapping of the second term
     * @return true unless the same variable is used at both positions and the two mappings cannot produce equal values
     *         (different constants, or disjoint classes), false otherwise
     */
    private boolean checkNodeCondition(Request request, RdfTerm term1, RdfTerm term2, TermMapping map1,
            TermMapping map2)
    {
        if(!(term1 instanceof Variable && term2 instanceof Variable))
            return true;

        if(!term1.equals(term2))
            return true;

        if(map1 instanceof ConstantMapping cmap1 && map2 instanceof ConstantMapping cmap2)
            return cmap1.getValue().equals(cmap2.getValue());

        return !ResourceClass.areDisjunct(map1.getResourceClass(request), map2.getResourceClass(request));
    }


    /**
     * Graph mapping; null for the default graph.
     *
     * @return graph mapping; null for the default graph
     */
    public final TermMapping getGraph()
    {
        return graph;
    }


    /**
     * Subject mapping.
     *
     * @return subject mapping
     */
    public final TermMapping getSubject()
    {
        return subject;
    }


    /**
     * Predicate mapping.
     *
     * @return predicate mapping
     */
    public final TermMapping getPredicate()
    {
        return predicate;
    }


    /**
     * Object mapping.
     *
     * @return object mapping
     */
    public final TermMapping getObject()
    {
        return object;
    }


    @Override
    public int hashCode()
    {
        return (subject != null ? subject.hashCode() : 0) ^ (predicate != null ? predicate.hashCode() : 0);
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(object == null || getClass() != object.getClass())
            return false;

        QuadMapping mapping = (QuadMapping) object;

        if(this.graph == null ? mapping.graph != null : !this.graph.equals(mapping.graph))
            return false;

        if(!this.predicate.equals(mapping.predicate))
            return false;

        if(!this.subject.equals(mapping.subject))
            return false;

        if(!this.object.equals(mapping.object))
            return false;

        return true;
    }
}
