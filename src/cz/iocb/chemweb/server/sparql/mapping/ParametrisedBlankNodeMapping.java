package cz.iocb.chemweb.server.sparql.mapping;

import java.util.List;
import cz.iocb.chemweb.server.sparql.database.Column;
import cz.iocb.chemweb.server.sparql.database.DatabaseSchema.ColumnPair;
import cz.iocb.chemweb.server.sparql.mapping.classes.BlankNodeClass;



public class ParametrisedBlankNodeMapping extends ParametrisedMapping
{
    public ParametrisedBlankNodeMapping(BlankNodeClass blankNodeClass, List<Column> columns)
    {
        super(blankNodeClass, columns);
    }


    @Override
    public NodeMapping remap(List<ColumnPair> columnMap)
    {
        return new ParametrisedBlankNodeMapping((BlankNodeClass) resourceClass, remapColumns(columnMap));
    }
}
