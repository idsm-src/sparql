package cz.iocb.chemweb.server.sparql.mapping;

import java.util.ArrayList;
import java.util.List;
import cz.iocb.chemweb.server.sparql.database.Column;
import cz.iocb.chemweb.server.sparql.database.DatabaseSchema.ColumnPair;
import cz.iocb.chemweb.server.sparql.engine.Request;
import cz.iocb.chemweb.server.sparql.mapping.classes.IriClass;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;



public class ParametrisedIriMapping extends IriMapping implements ParametrisedMapping
{
    private final List<Column> columns;


    public ParametrisedIriMapping(IriClass iriClass, List<Column> columns)
    {
        super(iriClass);
        this.columns = columns;
    }


    @Override
    public boolean match(Node node, Request request)
    {
        return getIriClass().match(node, request);
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

        return new ParametrisedIriMapping(getIriClass(), remappedColumns);
    }


    @Override
    public boolean equals(Object obj)
    {
        if(this == obj)
            return true;

        if(obj == null || !(obj instanceof ParametrisedIriMapping))
            return false;

        if(!super.equals(obj))
            return false;

        ParametrisedIriMapping other = (ParametrisedIriMapping) obj;

        return columns.equals(other.columns);
    }


    @Override
    public String getSqlIriValueAccess()
    {
        return getIriClass().getIriValueCode(columns);
    }


    public List<Column> getColumns()
    {
        return columns;
    }
}
