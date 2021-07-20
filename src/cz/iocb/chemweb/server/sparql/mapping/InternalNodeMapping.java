package cz.iocb.chemweb.server.sparql.mapping;

import java.util.ArrayList;
import java.util.List;
import cz.iocb.chemweb.server.sparql.database.Column;
import cz.iocb.chemweb.server.sparql.database.DatabaseSchema.ColumnPair;
import cz.iocb.chemweb.server.sparql.engine.Request;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;



public class InternalNodeMapping extends NodeMapping implements ParametrisedMapping
{
    private final List<Column> columns;


    public InternalNodeMapping(ResourceClass resourceClass, List<Column> columns)
    {
        super(resourceClass);
        this.columns = columns;
    }


    @Override
    public boolean match(Node node, Request request)
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public String getSqlValueAccess(int part)
    {
        return columns.get(part).getCode();
    }


    @Override
    public Column getSqlColumn(int part)
    {
        return columns.get(part);
    }


    @Override
    public NodeMapping remapColumns(List<ColumnPair> columnMap)
    {
        ArrayList<Column> remappedColumns = new ArrayList<Column>();

        for(Column col : columns)
        {
            Column remapped = columnMap.stream().filter(s -> s.getLeft().equals(col)).findAny().get().getRight();
            assert remapped != null;
            remappedColumns.add(remapped);
        }

        return new InternalNodeMapping(getResourceClass(), remappedColumns);
    }


    @Override
    public boolean equals(Object obj)
    {
        if(this == obj)
            return true;

        if(obj == null || !(obj instanceof InternalNodeMapping))
            return false;

        if(!super.equals(obj))
            return false;

        InternalNodeMapping other = (InternalNodeMapping) obj;

        return columns.equals(other.columns);
    }
}
