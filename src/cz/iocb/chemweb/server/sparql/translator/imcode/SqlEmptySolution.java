package cz.iocb.chemweb.server.sparql.translator.imcode;

import java.util.HashSet;
import cz.iocb.chemweb.server.db.DatabaseSchema;
import cz.iocb.chemweb.server.sparql.translator.UsedVariables;



public class SqlEmptySolution extends SqlIntercode
{
    public SqlEmptySolution()
    {
        super(new UsedVariables(), true);
    }


    @Override
    public SqlIntercode optimize(DatabaseSchema schema, HashSet<String> restrictions, boolean reduced)
    {
        return this;
    }


    @Override
    public String translate()
    {
        return "SELECT 1";
    }
}
