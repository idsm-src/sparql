package cz.iocb.chemweb.server.sparql.mapping;

import cz.iocb.chemweb.server.sparql.mapping.classes.IriClass;



public abstract class IriMapping extends NodeMapping
{
    protected IriMapping(IriClass iriClass)
    {
        super(iriClass);
    }


    public final IriClass getIriClass()
    {
        return (IriClass) resourceClass;
    }


    public abstract String getSqlIriValueAccess();
}
