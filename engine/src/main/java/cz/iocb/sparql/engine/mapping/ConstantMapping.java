package cz.iocb.sparql.engine.mapping;

import java.util.List;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.DatabaseSchema.ColumnPair;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.parser.model.VariableOrBlankNode;
import cz.iocb.sparql.engine.parser.model.triple.Node;
import cz.iocb.sparql.engine.request.Request;



public abstract class ConstantMapping extends NodeMapping
{
    protected final Node value;


    protected ConstantMapping(Node value, ResourceClass resourceClass, List<Column> columns)
    {
        super(resourceClass, columns);
        this.value = value;
    }


    public Node getValue()
    {
        return value;
    }


    @Override
    public boolean match(Request request, Node node)
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
