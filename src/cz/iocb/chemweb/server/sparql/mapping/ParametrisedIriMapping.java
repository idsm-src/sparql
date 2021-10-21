package cz.iocb.chemweb.server.sparql.mapping;

import java.util.List;
import cz.iocb.chemweb.server.sparql.database.Column;
import cz.iocb.chemweb.server.sparql.database.DatabaseSchema.ColumnPair;
import cz.iocb.chemweb.server.sparql.mapping.classes.IriClass;



public class ParametrisedIriMapping extends ParametrisedMapping
{
    public ParametrisedIriMapping(IriClass iriClass, List<Column> columns)
    {
        super(iriClass, columns);
    }


    @Override
    public NodeMapping remap(List<ColumnPair> columnMap)
    {
        return new ParametrisedIriMapping((IriClass) resourceClass, remapColumns(columnMap));
    }
}
