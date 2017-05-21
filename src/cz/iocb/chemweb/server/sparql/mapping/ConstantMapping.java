package cz.iocb.chemweb.server.sparql.mapping;

import java.util.List;
import cz.iocb.chemweb.server.db.DatabaseSchema.KeyPair;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;



public interface ConstantMapping
{
    ResourceClass getResourceClass();

    String getSqlValueAccess(int i);

    NodeMapping remapColumns(List<KeyPair> columnMap);
}
