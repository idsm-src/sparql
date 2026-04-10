package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.strBlankNode;
import java.util.List;
import java.util.Set;



public abstract class StrBlankNodeClass extends BlankNodeClass
{
    protected StrBlankNodeClass(String name, List<String> sqlTypes)
    {
        super(name, sqlTypes);
    }


    @Override
    public ResourceClass getGeneralClass()
    {
        return strBlankNode;
    }


    @Override
    public Set<ResultResourceClass> getResultResourceClasses()
    {
        return Set.of(strBlankNode);
    }
}
