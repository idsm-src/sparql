package cz.iocb.chemweb.server.sparql.mapping;

import java.util.List;
import cz.iocb.chemweb.server.sparql.database.Column;
import cz.iocb.chemweb.server.sparql.database.DatabaseSchema.ColumnPair;
import cz.iocb.chemweb.server.sparql.mapping.classes.LiteralClass;



public class ParametrisedLiteralMapping extends ParametrisedMapping
{
    public ParametrisedLiteralMapping(LiteralClass literalClass, List<Column> columns)
    {
        super(literalClass, columns);
    }


    @Override
    public NodeMapping remap(List<ColumnPair> columnMap)
    {
        return new ParametrisedLiteralMapping((LiteralClass) resourceClass, remapColumns(columnMap));
    }
}
