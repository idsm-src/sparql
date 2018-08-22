package cz.iocb.chemweb.server.sparql.mapping;

import cz.iocb.chemweb.server.sparql.mapping.classes.bases.BlankNodeBaseClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.PatternResourceClass;



public abstract class BlankNodeMapping extends NodeMapping
{
    protected BlankNodeMapping(BlankNodeBaseClass blankNodeClass)
    {
        super((PatternResourceClass) blankNodeClass);
    }


    public final BlankNodeBaseClass getBlankNodeClass()
    {
        return (BlankNodeBaseClass) resourceClass;
    }
}
