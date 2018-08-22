package cz.iocb.chemweb.server.sparql.mapping.classes.bases;

import java.util.Arrays;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.ExpressionResourceClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.PatternResourceClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.ResultTag;
import cz.iocb.chemweb.server.sparql.parser.model.VariableOrBlankNode;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;



public abstract class BlankNodeBaseClass extends PatternResourceBaseClass implements ExpressionResourceClass
{
    protected BlankNodeBaseClass(String name, String sqlType, ResultTag resultTag)
    {
        super(name, Arrays.asList(sqlType), Arrays.asList(resultTag));
    }


    @Override
    public String getSqlValue(Node node, int part)
    {
        return getSqlColumn(((VariableOrBlankNode) node).getName(), part);
    }


    @Override
    public String getResultValue(String variable, int part)
    {
        return getSqlColumn(variable, part);
    }


    @Override
    public boolean match(Node node)
    {
        return node instanceof VariableOrBlankNode;
    }


    @Override
    public PatternResourceClass getPatternResourceClass()
    {
        return this;
    }
}
