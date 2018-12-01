package cz.iocb.chemweb.server.sparql.mapping;

import java.util.ArrayList;
import java.util.List;
import cz.iocb.chemweb.server.db.DatabaseSchema.ColumnPair;
import cz.iocb.chemweb.server.sparql.mapping.classes.LiteralClass;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;



public class ParametrisedLiteralMapping extends LiteralMapping implements ParametrisedMapping
{
    private final List<String> columns;


    public ParametrisedLiteralMapping(LiteralClass literalClass, List<String> columns)
    {
        super(literalClass);
        this.columns = columns;
    }


    @Override
    public boolean match(Node node)
    {
        return getLiteralClass().match(node);
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

        return new ParametrisedLiteralMapping(getLiteralClass(), remappedColumns);
    }


    @Override
    public boolean equals(Object obj)
    {
        if(this == obj)
            return true;

        if(obj == null || !(obj instanceof ParametrisedLiteralMapping))
            return false;

        if(!super.equals(obj))
            return false;

        ParametrisedLiteralMapping other = (ParametrisedLiteralMapping) obj;

        return columns.equals(other.columns);
    }
}
