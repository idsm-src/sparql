package cz.iocb.chemweb.server.sparql.mapping;

import cz.iocb.chemweb.server.sparql.mapping.classes.BlankNodeClass;



public abstract class BlankNodeMapping extends NodeMapping
{
    protected BlankNodeMapping(BlankNodeClass blankNodeClass)
    {
        super(blankNodeClass);
    }


    public final BlankNodeClass getBlankNodeClass()
    {
        return (BlankNodeClass) resourceClass;
    }
}
