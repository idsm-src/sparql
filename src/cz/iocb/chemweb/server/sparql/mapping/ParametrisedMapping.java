package cz.iocb.chemweb.server.sparql.mapping;

import java.util.List;
import cz.iocb.chemweb.server.db.DatabaseSchema.KeyPair;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.PatternResourceClass;



public interface ParametrisedMapping
{
    PatternResourceClass getResourceClass();

    String getSqlValueAccess(int i);

    String getSqlColumn(int i);

    NodeMapping remapColumns(List<KeyPair> columnMap);
}
