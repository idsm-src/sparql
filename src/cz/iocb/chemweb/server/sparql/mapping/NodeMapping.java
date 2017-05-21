package cz.iocb.chemweb.server.sparql.mapping;

import java.util.List;
import cz.iocb.chemweb.server.db.DatabaseSchema.KeyPair;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;



public abstract class NodeMapping
{
    protected final ResourceClass resourceClass;


    protected NodeMapping(ResourceClass resourceClass)
    {
        this.resourceClass = resourceClass;
    }


    public abstract boolean match(Node node);

    public abstract String getSqlValueAccess(int i);

    public abstract NodeMapping remapColumns(List<KeyPair> columnMap);


    public final ResourceClass getResourceClass()
    {
        return resourceClass;
    }


    @Override
    public boolean equals(Object obj)
    {
        if(this == obj)
            return true;

        if(obj == null || !(obj instanceof NodeMapping))
            return false;

        NodeMapping other = (NodeMapping) obj;

        return resourceClass.equals(other.resourceClass);
    }
}
