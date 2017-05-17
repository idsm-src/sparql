package cz.iocb.chemweb.server.sparql.mapping;

import cz.iocb.chemweb.server.sparql.mapping.classes.LiteralClass;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.Variable;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;



public class ParametrisedLiteralMapping extends LiteralMapping
{
    private final String column;


    public ParametrisedLiteralMapping(LiteralClass literalClass, String column)
    {
        super(literalClass);
        this.column = column;
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


        //TODO: literals with language tags are not yet supported
        if(literal.getLanguageTag() != null)
            return false;


        return true;
    }


    @Override
    public String getSqlValueAccess(int i)
    {
        return column;
    }
}
