package cz.iocb.chemweb.server.sparql.engine;



public class VariableNode extends RdfNode
{
    protected VariableNode(String value)
    {
        super(value);
    }


    @Override
    public boolean isLiteral()
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public boolean isIri()
    {
        throw new UnsupportedOperationException();
    }
}
