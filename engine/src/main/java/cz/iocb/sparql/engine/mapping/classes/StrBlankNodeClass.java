package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.strCompositeBlankNode;
import java.util.List;
import java.util.Set;



public abstract class StrBlankNodeClass extends BlankNodeClass
{
    protected StrBlankNodeClass(String name, List<String> sqlTypes, Set<ResourceClass> superClasses)
    {
        super(name, sqlTypes, superClasses);
    }


    @Override
    public Set<ResultResourceClass> getResultResourceClasses()
    {
        return Set.of(strCompositeBlankNode);
    }
}
