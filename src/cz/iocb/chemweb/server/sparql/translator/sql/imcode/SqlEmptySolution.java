package cz.iocb.chemweb.server.sparql.translator.sql.imcode;

import cz.iocb.chemweb.server.sparql.translator.sql.UsedVariables;



public class SqlEmptySolution extends SqlIntercode
{
    public SqlEmptySolution()
    {
        super(new UsedVariables());
    }


    @Override
    public String translate()
    {
        return "SELECT 1";
    }
}
