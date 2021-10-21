package cz.iocb.chemweb.server.sparql.mapping;

import java.util.List;
import cz.iocb.chemweb.server.sparql.database.Column;
import cz.iocb.chemweb.server.sparql.database.DatabaseSchema.ColumnPair;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;



public final class InternalNodeMapping extends ParametrisedMapping
{
    public InternalNodeMapping(ResourceClass resourceClass, List<Column> columns)
    {
        super(resourceClass, columns);
    }


    @Override
    public NodeMapping remap(List<ColumnPair> columnMap)
    {
        return new InternalNodeMapping(getResourceClass(), remapColumns(columnMap));
    }
}
