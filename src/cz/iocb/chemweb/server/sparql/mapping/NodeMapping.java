package cz.iocb.chemweb.server.sparql.mapping;

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


    public final ResourceClass getResourceClass()
    {
        return resourceClass;
    }
}
