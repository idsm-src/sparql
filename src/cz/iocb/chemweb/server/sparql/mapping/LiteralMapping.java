package cz.iocb.chemweb.server.sparql.mapping;

import cz.iocb.chemweb.server.sparql.mapping.classes.LiteralClass;



public abstract class LiteralMapping extends NodeMapping
{
    protected LiteralMapping(LiteralClass literalClass)
    {
        super(literalClass);
    }


    public final LiteralClass getLiteralClass()
    {
        return (LiteralClass) resourceClass;
    }
}
