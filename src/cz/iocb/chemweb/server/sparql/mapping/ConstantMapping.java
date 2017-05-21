package cz.iocb.chemweb.server.sparql.mapping;

import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;



public interface ConstantMapping
{
    ResourceClass getResourceClass();

    String getSqlValueAccess(int i);
}
