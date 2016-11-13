package cz.iocb.chemweb.server.sparql.semanticcheck.visitor.structures.type;


public class NegOperator extends TypeElement implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    public TypeElement son;


    public NegOperator(String value)
    {
        this.value = value;
    }


    @Override
    public <T> T accept(Visitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
