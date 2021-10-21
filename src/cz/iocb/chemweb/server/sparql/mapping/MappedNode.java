package cz.iocb.chemweb.server.sparql.mapping;

import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;



public class MappedNode
{
    private final Node node;
    private final NodeMapping mapping;


    public MappedNode(Node node, NodeMapping mapping)
    {
        this.node = node;
        this.mapping = mapping;
    }


    public Node getNode()
    {
        return node;
    }


    public NodeMapping getMapping()
    {
        return mapping;
    }
}
