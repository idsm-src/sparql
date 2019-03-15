package cz.iocb.chemweb.server.sparql.mapping;

import java.util.List;
import cz.iocb.chemweb.server.db.schema.Column;
import cz.iocb.chemweb.server.db.schema.DatabaseSchema.ColumnPair;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;



public interface ParametrisedMapping
{
    ResourceClass getResourceClass();

    String getSqlValueAccess(int part);

    Column getSqlColumn(int part);

    NodeMapping remapColumns(List<ColumnPair> columnMap);
}
