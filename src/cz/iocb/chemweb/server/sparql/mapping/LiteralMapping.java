package cz.iocb.chemweb.server.sparql.mapping;

import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.PatternLiteralClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.PatternResourceClass;



public abstract class LiteralMapping extends NodeMapping
{
    protected LiteralMapping(PatternLiteralClass literalClass)
    {
        super((PatternResourceClass) literalClass);
    }


    public final PatternLiteralClass getLiteralClass()
    {
        return (PatternLiteralClass) resourceClass;
    }
}
