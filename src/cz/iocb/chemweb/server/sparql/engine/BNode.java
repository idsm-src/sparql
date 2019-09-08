package cz.iocb.chemweb.server.sparql.engine;



public class BNode extends ReferenceNode
{
    public BNode(String value)
    {
        super(value);
    }


    @Override
    public String toString()
    {
        return "_:" + value;
    }


    @Override
    public boolean equals(Object obj)
    {
        if(this == obj)
            return true;

        if(obj == null || !(obj instanceof BNode))
            return false;

        return value.equals(((BNode) obj).value);
    }
}
