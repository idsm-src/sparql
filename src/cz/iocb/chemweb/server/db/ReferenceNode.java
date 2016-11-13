package cz.iocb.chemweb.server.db;





public abstract class ReferenceNode extends RdfNode
{
    public ReferenceNode(String value)
    {
        super(value);
    }


    @Override
    public String toString()
    {
        return "<" + value + ">";
    }


    @Override
    public boolean isLiteral()
    {
        return false;
    }
}
