package cz.iocb.chemweb.server.sparql.mapping.classes;

import java.util.List;
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


    public abstract String getSqlLiteralValue(Literal node, int part);


    @Override
    public final String getSqlValue(Node node, int part)
    {
        if(node instanceof VariableOrBlankNode)
            return getSqlColumn(((VariableOrBlankNode) node).getName(), part);
        else
            return getSqlLiteralValue((Literal) node, part);
    }


    @Override
    public boolean match(Node node)
    {
        if(node instanceof VariableOrBlankNode)
            return true;

        if(!(node instanceof Literal))
            return false;

        Literal literal = (Literal) node;

        if(!typeIri.equals(literal.getTypeIri()))
            return false;

        if(literal.getValue() == null)
            return false;

        if(typeIri.equals(BuiltinTypes.rdfLangStringIri) && literal.getLanguageTag() == null)
            return false;

        return true;
    }


    public final IRI getTypeIri()
    {
        return typeIri;
    }
}
