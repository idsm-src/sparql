package cz.iocb.chemweb.server.sparql.mapping;

import java.util.List;
import cz.iocb.chemweb.server.sparql.database.DatabaseSchema.ColumnPair;
import cz.iocb.chemweb.server.sparql.engine.Request;
import cz.iocb.chemweb.server.sparql.mapping.classes.BlankNodeClass;
import cz.iocb.chemweb.server.sparql.parser.model.VariableOrBlankNode;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;



public class ConstantBlankNodeMapping extends BlankNodeMapping implements ConstantMapping
{
    private final BlankNodeLiteral value;


    public ConstantBlankNodeMapping(BlankNodeClass blanknodeClass, BlankNodeLiteral node)
    {
        super(blanknodeClass);
        value = node;
    }


    @Override
    public boolean match(Node node, Request request)
    {
        if(node instanceof VariableOrBlankNode)
            return true;

        return false;
    }


    @Override
    public String getSqlValueAccess(int part)
    {
        return getBlankNodeClass().getPatternCode(value, part);
    }


    @Override
    public NodeMapping remapColumns(List<ColumnPair> columnMap)
    {
        return this;
    }


    @Override
    public boolean equals(Object obj)
    {
        if(this == obj)
            return true;

        if(obj == null || !(obj instanceof ConstantBlankNodeMapping))
            return false;

        if(!super.equals(obj))
            return false;

        ConstantBlankNodeMapping other = (ConstantBlankNodeMapping) obj;

        return resourceClass == other.resourceClass && value.equals(other.value);
    }


    @Override
    public Node getValue()
    {
        return value;
    }
}
