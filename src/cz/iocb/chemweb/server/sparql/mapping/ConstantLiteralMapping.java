package cz.iocb.chemweb.server.sparql.mapping;

import cz.iocb.chemweb.server.sparql.mapping.classes.LiteralClass;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.Variable;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;



public class ConstantLiteralMapping extends LiteralMapping
{
    private final Literal value;


    public ConstantLiteralMapping(LiteralClass literalClass, Literal value)
    {
        super(literalClass);
        this.value = value;

        if(value == null)
            throw new RuntimeException();
    }


    @Override
    public boolean match(Node node)
    {
        if(node instanceof Variable)
            return true;

        if(!(node instanceof Literal))
            return false;

        Literal literal = (Literal) node;


        //TODO: fix Literal.getTypeIri() to not return null
        IRI iri = literal.getTypeIri();
        String type = iri != null ? iri.toString() : "http://www.w3.org/2001/XMLSchema#string";

        if(!getLiteralClass().getTypeIri().equals(type))
            return false;


        String literalTag = literal.getLanguageTag();
        String valueTag = value.getLanguageTag();

        if(literalTag != valueTag && (literalTag == null || !literalTag.equals(valueTag)))
            return false;


        return literal.getValue().equals(value.getValue());
    }


    @Override
    public String getSqlValueAccess(int i)
    {
        IRI iri = value.getTypeIri();
        String type = iri != null ? iri.toString() : "http://www.w3.org/2001/XMLSchema#string";

        if(type.equals("http://www.w3.org/2001/XMLSchema#string"))
            return "'" + ((String) value.getValue()).replaceAll("'", "\\'") + "'";
        else
            return value.toString();
    }


    @Override
    public boolean equals(Object obj)
    {
        if(this == obj)
            return true;

        if(obj == null || !(obj instanceof ConstantLiteralMapping))
            return false;

        if(!super.equals(obj))
            return false;

        ConstantLiteralMapping other = (ConstantLiteralMapping) obj;

        return value.equals(other.value);
    }
}
