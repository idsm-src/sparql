package cz.iocb.chemweb.server.sparql.mapping;

import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;



public abstract class IriMapping extends NodeMapping
{
    protected IriMapping(UserIriClass iriClass)
    {
        super(iriClass);
    }


    public final UserIriClass getIriClass()
    {
        return (UserIriClass) resourceClass;
    }
}
