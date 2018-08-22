package cz.iocb.chemweb.server.sparql.mapping.classes.bases;

import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.ResourceClass;



public class ResourceBaseClass implements ResourceClass
{
    protected final String name;


    protected ResourceBaseClass(String name)
    {
        this.name = name;
    }


    @Override
    public final String getName()
    {
        return name;
    }
}
