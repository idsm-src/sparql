package cz.iocb.chemweb.server.sparql.mapping;

import cz.iocb.chemweb.server.sparql.engine.Request;
import cz.iocb.chemweb.server.sparql.parser.model.VariableOrBlankNode;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;



public abstract class QuadMapping
{
    private final ConstantIriMapping graph;
    private final NodeMapping subject;
    private final ConstantIriMapping predicate;
    private final NodeMapping object;


    public QuadMapping(ConstantIriMapping graph, NodeMapping subject, ConstantIriMapping predicate, NodeMapping object)
    {
        //TODO: add support for ParametrisedIriMapping graphs
        //TODO: add support for ParametrisedIriMapping predicates

        this.graph = graph;
        this.subject = subject;
        this.predicate = predicate;
        this.object = object;
    }


    public boolean match(Node graph, Node subject, Node predicate, Node object, Request request)
    {
        if(!match(this.graph, graph, request))
            return false;

        if(!match(this.subject, subject, request))
            return false;

        if(!match(this.predicate, predicate, request))
            return false;

        if(!match(this.object, object, request))
            return false;

        if(!checkNodeCondition(graph, subject, getGraph(), getSubject()))
            return false;

        if(!checkNodeCondition(graph, predicate, getGraph(), getPredicate()))
            return false;

        if(!checkNodeCondition(graph, object, getGraph(), getObject()))
            return false;

        if(!checkNodeCondition(subject, predicate, getSubject(), getPredicate()))
            return false;

        if(!checkNodeCondition(subject, object, getSubject(), getObject()))
            return false;

        if(!checkNodeCondition(predicate, object, getPredicate(), getObject()))
            return false;

        return true;
    }


    private boolean match(NodeMapping mapping, Node node, Request request)
    {
        if(node == null)
            return true;

        if(mapping == null)
            return false;

        return mapping.match(node, request);
    }


    private boolean checkNodeCondition(Node node1, Node node2, NodeMapping map1, NodeMapping map2)
    {
        if(!(node1 instanceof VariableOrBlankNode && node2 instanceof VariableOrBlankNode))
            return true;

        if(!node1.equals(node2))
            return true;

        if(map1.getResourceClass().getGeneralClass() != map2.getResourceClass().getGeneralClass())
            return false;

        if(map1 instanceof ConstantMapping && map2 instanceof ConstantMapping && !map1.equals(map2))
            return false;

        return true;
    }


    public final ConstantIriMapping getGraph()
    {
        return graph;
    }


    public final NodeMapping getSubject()
    {
        return subject;
    }


    public final ConstantIriMapping getPredicate()
    {
        return predicate;
    }


    public final NodeMapping getObject()
    {
        return object;
    }
}
