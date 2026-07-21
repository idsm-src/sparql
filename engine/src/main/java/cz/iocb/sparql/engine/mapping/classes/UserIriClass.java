package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.expression;
import static cz.iocb.sparql.engine.mapping.classes.CodeHelper.string;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;



public abstract class UserIriClass extends IriClass
{
    protected UserIriClass(String name, List<String> sqlTypes, Set<ResourceClass> superClasses)
    {
        super(name, sqlTypes, superClasses);
    }


    public abstract int getCheckCost();


    protected static Column addPrefixAndSuffix(String prefix, Column value, String suffix)
    {
        if(prefix != null && suffix != null)
            return expression("(%s || %s || %s)::varchar", string(prefix), value, string(suffix));
        else if(prefix != null)
            return expression("(%s || %s)::varchar", string(prefix), value);
        else if(suffix != null)
            return expression("(%s || %s)::varchar", value, string(suffix));
        else
            return value;
    }
}
