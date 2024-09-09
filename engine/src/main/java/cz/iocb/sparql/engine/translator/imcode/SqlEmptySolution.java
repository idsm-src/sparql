package cz.iocb.sparql.engine.translator.imcode;

import java.util.Set;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.UsedVariables;



public class SqlEmptySolution extends SqlIntercode
{
    static private final SqlEmptySolution singleton = new SqlEmptySolution();


    private SqlEmptySolution()
    {
        super(new UsedVariables(), true);
    }


    public static SqlEmptySolution get()
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
        return "SELECT 1";
    }
}
