package cz.iocb.chemweb.server.sparql.engine;



public class Literal extends RdfNode
{
    public Literal(String value)
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
    public boolean equals(Object obj)
    {
        if(this == obj)
            return true;

        if(obj == null || !(obj instanceof Literal))
            return false;

        return value.equals(((Literal) obj).value);
    }
}
