package cz.iocb.chemweb.server.sparql.engine;



public class IriNode extends ReferenceNode
{
    public IriNode(String value)
    {
        super(value);
    }


    @Override
    public String toString()
    {
        return "<" + value + ">";
    }


    @Override
    public boolean equals(Object obj)
    {
        if(this == obj)
            return true;

        if(obj == null || !(obj instanceof IriNode))
            return false;

        return value.equals(((IriNode) obj).value);
    }
}
