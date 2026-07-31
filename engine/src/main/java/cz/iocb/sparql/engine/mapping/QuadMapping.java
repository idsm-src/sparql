package cz.iocb.sparql.engine.mapping;

import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.Request;



public abstract class QuadMapping
{
    private final ConstantIriMapping graph;
    private final TermMapping subject;
    private final TermMapping predicate;
    private final TermMapping object;


    public QuadMapping(ConstantIriMapping graph, TermMapping subject, TermMapping predicate, TermMapping object)
    {
        //TODO: add support for parameterized graph mapping

        this.graph = graph;
        this.subject = subject;
        this.predicate = predicate;
        this.object = object;
    }


    public abstract QuadMapping asDefaultGraphMapping();


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


    private boolean match(Request request, TermMapping mapping, RdfTerm term)
    {
        if(term == null && mapping == null)
            return true;

        if(term == null || mapping == null)
            return false;

        return mapping.match(request, term);
    }


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


    public final ConstantIriMapping getGraph()
    {
        return graph;
    }


    public final TermMapping getSubject()
    {
        return subject;
    }


    public final TermMapping getPredicate()
    {
        return predicate;
    }


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
