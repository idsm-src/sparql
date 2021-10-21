package cz.iocb.chemweb.server.sparql.mapping;

import java.util.List;
import cz.iocb.chemweb.server.sparql.database.DatabaseSchema.ColumnPair;
import cz.iocb.chemweb.server.sparql.parser.model.VariableOrBlankNode;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;



public abstract class ConstantMapping extends NodeMapping
{
    protected final Node value;


    protected ConstantMapping(Node node)
    {
        this.value = node;
    }


    public Node getValue()
    {
        return value;
    }


    @Override
    public boolean match(Node node)
    {
        if(node instanceof VariableOrBlankNode)
            return true;

        return value.equals(node);
    }


    @Override
    public NodeMapping remap(List<ColumnPair> columnMap)
    {
        return this;
    }


    @Override
    public int hashCode()
    {
        return value.hashCode();
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(object == null || getClass() != object.getClass())
            return false;

        ConstantMapping other = (ConstantMapping) object;

        return value.equals(other.value);
    }
}
