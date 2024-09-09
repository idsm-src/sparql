package cz.iocb.sparql.engine.translator.imcode;

import java.util.Set;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.UsedVariables;



public class SqlNoSolution extends SqlIntercode
{
    static private final SqlNoSolution singleton = new SqlNoSolution();


    private SqlNoSolution()
    {
        super(new UsedVariables(), true);
    }


    public static SqlNoSolution get()
    {
        return singleton;
    }


    @Override
    public SqlIntercode optimize(Request request, Set<String> restrictions, boolean reduced)
    {
        return this;
    }


    @Override
    public String translate(Request request)
    {
        return "SELECT 1 WHERE false";
    }
}
