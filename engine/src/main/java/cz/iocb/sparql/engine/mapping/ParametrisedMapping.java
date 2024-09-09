package cz.iocb.sparql.engine.mapping;

import java.util.ArrayList;
import java.util.List;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.DatabaseSchema.ColumnPair;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.parser.model.triple.Node;
import cz.iocb.sparql.engine.request.Request;



public abstract class ParametrisedMapping extends NodeMapping
{
    protected ParametrisedMapping(ResourceClass resourceClass, List<Column> columns)
    {
        super(resourceClass, columns);
    }


    @Override
    public boolean match(Request request, Node node)
    {
        return resourceClass.match(request.getStatement(), node);
    }


    public ArrayList<Column> remapColumns(List<ColumnPair> columnMap)
    {
        ArrayList<Column> remappedColumns = new ArrayList<Column>();

        for(Column col : columns)
            remappedColumns.add(columnMap.stream().filter(s -> s.getLeft().equals(col)).findAny().get().getRight());

        return remappedColumns;
    }


    @Override
    public int hashCode()
    {
        return columns.hashCode();
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(object == null || getClass() != object.getClass())
            return false;

        ParametrisedMapping other = (ParametrisedMapping) object;

        return resourceClass == other.resourceClass && columns.equals(other.columns);
    }
}
