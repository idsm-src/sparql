package cz.iocb.chemweb.server.sparql.mapping.classes;

import java.util.List;
import cz.iocb.chemweb.server.sparql.engine.Request;
import cz.iocb.chemweb.server.sparql.parser.BuiltinTypes;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.VariableOrBlankNode;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;



public abstract class LiteralClass extends ResourceClass
{
    private final IRI typeIri;


    protected LiteralClass(String name, List<String> sqlTypes, List<ResultTag> resultTags, IRI typeIri)
    {
        super(name, sqlTypes, resultTags);
        this.typeIri = typeIri;
    }


    @Override
    public final String getPatternCode(Node node, int part)
    {
        if(node instanceof VariableOrBlankNode)
            return getSqlColumn(((VariableOrBlankNode) node).getSqlName(), part);
        else
            return getLiteralPatternCode((Literal) node, part);
    }


    /**
     * Gets SQL code to obtain the given part of the pattern representation for the given SPARQL literal.
     *
     * @param node SPARQL literal
     * @param part part index
     * @return SQL code
     */
    public abstract String getLiteralPatternCode(Literal literal, int part);


    /**
     * Gets SQL code to obtain the expression representation for the given literal node.
     *
     * @param literal SPARQL literal node
     * @return SQL code
     */
    public abstract String getExpressionCode(Literal literal);


    @Override
    public boolean match(Node node, Request request)
    {
        if(node instanceof VariableOrBlankNode)
            return true;

        if(!(node instanceof Literal))
            return false;

        Literal literal = (Literal) node;
        IRI literalTypeIri = literal.getTypeIri();

        if(typeIri == null)
        {
            if(literal.isTypeSupported() && literal.getValue() != null)
                return false;
        }
        else
        {
            if(literal.getValue() == null)
                return false;

            if(!typeIri.equals(literalTypeIri))
                return false;

            if(typeIri.equals(BuiltinTypes.rdfLangStringIri) && literal.getLanguageTag() == null)
                return false;
        }

        return true;
    }


    public final IRI getTypeIri()
    {
        return typeIri;
    }


    @Override
    public boolean equals(Object object)
    {
        if(object == this)
            return true;

        if(!super.equals(object))
            return false;

        LiteralClass other = (LiteralClass) object;

        if(!typeIri.equals(other.typeIri))
            return false;

        return true;
    }
}
