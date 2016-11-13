package cz.iocb.chemweb.server.sparql.parser;

import cz.iocb.chemweb.server.sparql.parser.model.triple.ComplexNode;
import cz.iocb.chemweb.server.sparql.parser.model.triple.NodeOptions;



public abstract class BaseComplexNode extends BaseElement implements ComplexNode
{
    private NodeOptions nodeOptions;

    @Override
    public NodeOptions getNodeOptions()
    {
        return nodeOptions;
    }

    @Override
    public void setNodeOptions(NodeOptions options)
    {
        nodeOptions = options;
    }
}
