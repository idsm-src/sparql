package cz.iocb.chemweb.server.sparql.translator.imcode;

import cz.iocb.chemweb.server.sparql.translator.UsedVariables;



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
