package cz.iocb.chemweb.server.sparql.mapping;

import java.util.List;
import cz.iocb.chemweb.server.db.DatabaseSchema.KeyPair;
import cz.iocb.chemweb.server.sparql.mapping.classes.LiteralClass;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.VariableOrBlankNode;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;



public class ParametrisedLiteralMapping extends LiteralMapping implements ParametrisedMapping
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
        if(node instanceof VariableOrBlankNode)
            return true;

        if(!(node instanceof Literal))
            return false;

        Literal literal = (Literal) node;


        //TODO: fix Literal.getTypeIri() to not return null
        IRI iri = literal.getTypeIri();
        String type = iri != null ? iri.getUri().toString() : "http://www.w3.org/2001/XMLSchema#string";

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


    @Override
    public NodeMapping remapColumns(List<KeyPair> columnMap)
    {
        String remapped = columnMap.stream().filter(s -> s.getParent().equals(column)).findAny().get().getForeign();
        assert remapped != null;
        return new ParametrisedLiteralMapping(getLiteralClass(), remapped);
    }


    @Override
    public boolean equals(Object obj)
    {
        if(this == obj)
            return true;

        if(obj == null || !(obj instanceof ParametrisedLiteralMapping))
            return false;

        if(!super.equals(obj))
            return false;

        ParametrisedLiteralMapping other = (ParametrisedLiteralMapping) obj;

        return column.equals(other.column);
    }
}
