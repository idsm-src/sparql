package cz.iocb.chemweb.server.sparql.mapping;

import cz.iocb.chemweb.server.sparql.database.Table;
import cz.iocb.chemweb.server.sparql.engine.Request;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;



public class QuadMapping
{
    private final Table table;
    private final String condition;
    private final NodeMapping graph;
    private final NodeMapping subject;
    private final ConstantIriMapping predicate;
    private final NodeMapping object;


    public QuadMapping(Table table, NodeMapping graph, NodeMapping subject, ConstantIriMapping predicate,
            NodeMapping object)
    {
        this(table, graph, subject, predicate, object, null);
    }


    public QuadMapping(Table table, NodeMapping graph, NodeMapping subject, ConstantIriMapping predicate,
            NodeMapping object, String condition)
    {
        //TODO: add support for ParametrisedIriMapping graphs
        if(graph != null && !(graph instanceof ConstantIriMapping))
            throw new IllegalArgumentException();

        this.table = table;
        this.graph = graph;
        this.subject = subject;
        this.predicate = predicate;
        this.object = object;
        this.condition = condition;
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


    public final Table getTable()
    {
        return table;
    }


    public final String getCondition()
    {
        return condition;
    }


    public final NodeMapping getGraph()
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
