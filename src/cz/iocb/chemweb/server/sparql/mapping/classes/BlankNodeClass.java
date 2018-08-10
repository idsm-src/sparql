package cz.iocb.chemweb.server.sparql.mapping.classes;

import java.util.List;
import cz.iocb.chemweb.server.sparql.parser.model.VariableOrBlankNode;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;



public class BlankNodeClass extends ResourceClass
{
    protected BlankNodeClass(String name, List<String> sqlTypes, List<ResultTag> resultTags)
    {
        super(name, sqlTypes, resultTags);
    }


    @Override
    public String getSqlValue(Node node, int part)
    {
        return getSqlColumn(((VariableOrBlankNode) node).getName(), part);
    }


    @Override
    public boolean match(Node node)
    {
        return node instanceof VariableOrBlankNode;
    }


    @Override
    public String getResultValue(String variable, int part)
    {
        return getSqlColumn(variable, part);
    }
}
