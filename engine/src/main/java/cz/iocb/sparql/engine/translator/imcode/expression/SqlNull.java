package cz.iocb.sparql.engine.translator.imcode.expression;

import java.util.HashSet;
import java.util.Set;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.UsedVariables;
import cz.iocb.sparql.engine.translator.imcode.SqlIntercode.Restrictions;



public class SqlNull extends SqlExpressionIntercode
{
    static private final SqlNull singleton = new SqlNull();


    private SqlNull()
    {
        super(new HashSet<ResourceClass>(), true, true);
    }


    public static SqlExpressionIntercode get()
    {
        return singleton;
    }


    @Override
    public Restrictions getRequirements(Set<ResourceClass> expected)
    {
        return new Restrictions();
    }


    @Override
    public SqlExpressionIntercode optimize(Request request, UsedVariables variables, boolean evalServices)
    {
        return singleton;
    }


    @Override
    public String translate(Request request)
    {
        return "NULL";
    }
}
