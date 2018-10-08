package cz.iocb.chemweb.server.sparql.mapping.classes;

import java.util.Arrays;
import cz.iocb.chemweb.server.sparql.parser.model.VariableOrBlankNode;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;



public abstract class BlankNodeClass extends ResourceClass
{
    protected BlankNodeClass(String name, String sqlType, ResultTag resultTag)
    {
        super(name, Arrays.asList(sqlType), Arrays.asList(resultTag));
    }


    @Override
    public String getPatternCode(Node node, int part)
    {
        return getSqlColumn(((VariableOrBlankNode) node).getName(), part);
    }


    @Override
    public String getResultCode(String variable, int part)
    {
        return getSqlColumn(variable, part);
    }


    @Override
    public boolean match(Node node)
    {
        return node instanceof VariableOrBlankNode;
    }
}
