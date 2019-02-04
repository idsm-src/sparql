package cz.iocb.chemweb.server.db;

import cz.iocb.chemweb.server.sparql.parser.model.IRI;



public class TypedLiteral extends Literal
{
    private final String datatype;


    public TypedLiteral(String value, String datatype)
    {
        super(value);
        this.datatype = datatype;
    }


    public TypedLiteral(String value, IRI datatype)
    {
        super(value);
        this.datatype = datatype.getValue();
    }


    public IriNode getDatatype()
    {
        return new IriNode(datatype);
    }


    @Override
    public String toString()
    {
        return "\"" + value.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\"^^<" + datatype + ">";
    }


    @Override
    public boolean equals(Object obj)
    {
        if(this == obj)
            return true;

        if(obj == null || !(obj instanceof TypedLiteral))
            return false;

        TypedLiteral literal = (TypedLiteral) obj;

        return value.equals(literal.value) && datatype.equals(literal.datatype);
    }
}
