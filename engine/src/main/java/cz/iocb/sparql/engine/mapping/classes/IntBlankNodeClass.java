package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.intCompositeBlankNode;
import java.util.List;
import java.util.Set;



public abstract class IntBlankNodeClass extends BlankNodeClass
{
    protected IntBlankNodeClass(String name, List<String> sqlTypes, Set<ResourceClass> superClasses)
    {
        super(name, sqlTypes, superClasses);
    }


    @Override
    public Set<ResultResourceClass> getResultResourceClasses()
    {
        return Set.of(intCompositeBlankNode);
    }
}
