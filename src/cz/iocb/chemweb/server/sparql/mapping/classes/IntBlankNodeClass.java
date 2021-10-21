package cz.iocb.chemweb.server.sparql.mapping.classes;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.intBlankNode;
import static cz.iocb.chemweb.server.sparql.mapping.classes.ResultTag.BLANKNODEINT;
import static java.util.Arrays.asList;
import java.util.List;



public abstract class IntBlankNodeClass extends BlankNodeClass
{
    protected IntBlankNodeClass(String name, List<String> sqlTypes)
    {
        super(name, sqlTypes, asList(BLANKNODEINT));
    }


    @Override
    public ResourceClass getGeneralClass()
    {
        return intBlankNode;
    }
}
