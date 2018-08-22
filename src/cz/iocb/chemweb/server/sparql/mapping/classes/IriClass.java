package cz.iocb.chemweb.server.sparql.mapping.classes;

import java.util.List;
import cz.iocb.chemweb.server.sparql.mapping.classes.bases.PatternResourceBaseClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.PatternResourceClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.ResultTag;



public abstract class IriClass extends PatternResourceBaseClass implements PatternResourceClass
{
    protected IriClass(String name, List<String> sqlTypes, List<ResultTag> resultTags)
    {
        super(name, sqlTypes, resultTags);
    }
}
