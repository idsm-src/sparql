package cz.iocb.chemweb.server.sparql.mapping;

import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;



public interface ParametrisedMapping
{
    ResourceClass getResourceClass();

    String getSqlValueAccess(int i);
}
