package cz.iocb.chemweb.server.sparql.translator.imcode;

import java.util.HashSet;
import cz.iocb.chemweb.server.sparql.engine.Request;
import cz.iocb.chemweb.server.sparql.translator.UsedVariables;



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
    public SqlIntercode optimize(Request request, HashSet<String> restrictions, boolean reduced)
    {
        return this;
    }


    @Override
    public String translate()
    {
        return "SELECT 1 WHERE false";
    }
}
