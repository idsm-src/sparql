package cz.iocb.chemweb.server.sparql.mapping;

import java.util.List;
import cz.iocb.chemweb.server.db.DatabaseSchema.KeyPair;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.ResourceClass;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;



public interface ConstantMapping
{
    ResourceClass getResourceClass();

    String getSqlValueAccess(int i);

    NodeMapping remapColumns(List<KeyPair> columnMap);

    Node getValue();
}
