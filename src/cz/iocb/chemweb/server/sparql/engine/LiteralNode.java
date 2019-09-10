package cz.iocb.chemweb.server.sparql.engine;



public class LiteralNode extends RdfNode
{
    public LiteralNode(String value)
    {
        super(value);
    }


    @Override
    public String toString()
    {
        //FIXME: multiline value
        return "\"" + value.replace("\"", "\\\"") + "\"";
    }


    @Override
    public boolean isLiteral()
    {
        return true;
    }


    @Override
    public boolean isIri()
    {
        return false;
    }


    @Override
    public boolean equals(Object obj)
    {
        if(this == obj)
            return true;

        if(obj == null || !(obj instanceof LiteralNode))
            return false;

        return value.equals(((LiteralNode) obj).value);
    }
}
