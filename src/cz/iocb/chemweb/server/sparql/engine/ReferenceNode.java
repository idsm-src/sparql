package cz.iocb.chemweb.server.sparql.engine;



public abstract class ReferenceNode extends RdfNode
{
    public ReferenceNode(String value)
    {
        super(value);
    }


    @Override
    public boolean isLiteral()
    {
        return false;
    }
}
