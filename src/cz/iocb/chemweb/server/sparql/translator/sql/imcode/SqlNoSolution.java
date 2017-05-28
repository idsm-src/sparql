package cz.iocb.chemweb.server.sparql.translator.sql.imcode;

import cz.iocb.chemweb.server.sparql.translator.sql.UsedVariables;



public class SqlNoSolution extends SqlIntercode
{
    public SqlNoSolution()
    {
        super(new UsedVariables());
    }


    @Override
    public String translate()
    {
        return "SELECT 1 WHERE false";
    }
}
