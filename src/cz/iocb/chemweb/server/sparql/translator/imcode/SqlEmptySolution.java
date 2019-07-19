package cz.iocb.chemweb.server.sparql.translator.imcode;

import java.util.HashSet;
import cz.iocb.chemweb.server.sparql.engine.Request;
import cz.iocb.chemweb.server.sparql.translator.UsedVariables;



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
    public SqlIntercode optimize(Request request, HashSet<String> restrictions, boolean reduced)
    {
        return this;
    }


    @Override
    public String translate()
    {
        return "SELECT 1";
    }
}
