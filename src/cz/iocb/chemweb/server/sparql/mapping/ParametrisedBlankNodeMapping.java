package cz.iocb.chemweb.server.sparql.mapping;

import java.util.ArrayList;
import java.util.List;
import cz.iocb.chemweb.server.db.DatabaseSchema.KeyPair;
import cz.iocb.chemweb.server.sparql.mapping.classes.BlankNodeClass;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;



public class ParametrisedBlankNodeMapping extends BlankNodeMapping implements ParametrisedMapping
{
    private final List<String> columns;


    public ParametrisedBlankNodeMapping(BlankNodeClass blankNodeClass, List<String> columns)
    {
        super(blankNodeClass);
        this.columns = columns;
    }


    @Override
    public boolean match(Node node)
    {
        return getBlankNodeClass().match(node);
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
    public NodeMapping remapColumns(List<KeyPair> columnMap)
    {
        ArrayList<String> remappedColumns = new ArrayList<String>();

        for(String col : columns)
        {
            String remapped = columnMap.stream().filter(s -> s.getParent().equals(col)).findAny().get().getForeign();
            assert remapped != null;
            remappedColumns.add(remapped);
        }

        return new ParametrisedBlankNodeMapping(getBlankNodeClass(), remappedColumns);
    }


    @Override
    public boolean equals(Object obj)
    {
        if(this == obj)
            return true;

        if(obj == null || !(obj instanceof ParametrisedBlankNodeMapping))
            return false;

        if(!super.equals(obj))
            return false;

        ParametrisedBlankNodeMapping other = (ParametrisedBlankNodeMapping) obj;

        return columns.equals(other.columns);
    }
}
