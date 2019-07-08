package cz.iocb.chemweb.server.sparql.mapping;

import java.util.List;
import cz.iocb.chemweb.server.sparql.database.DatabaseSchema.ColumnPair;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;



public interface ConstantMapping
{
    ResourceClass getResourceClass();

    String getSqlValueAccess(int part);

    NodeMapping remapColumns(List<ColumnPair> columnMap);

    Node getValue();
}
