package cz.iocb.chemweb.server.sparql.mapping;

import java.util.ArrayList;
import java.util.List;
import cz.iocb.chemweb.server.db.DatabaseSchema.ColumnPair;
import cz.iocb.chemweb.server.sparql.mapping.classes.IriClass;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;



public class ParametrisedIriMapping extends IriMapping implements ParametrisedMapping
{
    private final List<String> columns;


    public ParametrisedIriMapping(IriClass iriClass, List<String> columns)
    {
        super(iriClass);
        this.columns = columns;
    }


    @Override
    public boolean match(Node node)
    {
        return getIriClass().match(node);
    }


    @Override
    public String getSqlValueAccess(int part)
    {
        return columns.get(part);
    }


    @Override
    public String getSqlColumn(int part)
    {
        return columns.get(part);
    }


    @Override
    public NodeMapping remapColumns(List<ColumnPair> columnMap)
    {
        ArrayList<String> remappedColumns = new ArrayList<String>();

        for(String col : columns)
        {
            String remapped = columnMap.stream().filter(s -> s.getLeft().equals(col)).findAny().get().getRight();
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


    public String getSqlIriValueAccess()
    {
        return getIriClass().getIriValueCode(columns);
    }
}
