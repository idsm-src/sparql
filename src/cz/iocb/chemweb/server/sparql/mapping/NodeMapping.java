package cz.iocb.chemweb.server.sparql.mapping;

import java.util.List;
import cz.iocb.chemweb.server.sparql.database.Column;
import cz.iocb.chemweb.server.sparql.database.DatabaseSchema.ColumnPair;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;



public abstract class NodeMapping
{
    public abstract ResourceClass getResourceClass();

    public abstract List<Column> getColumns();

    public abstract boolean match(Node node);

    public abstract NodeMapping remap(List<ColumnPair> columnMap);
}
