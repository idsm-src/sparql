package cz.iocb.chemweb.server.sparql.translator.imcode;

import cz.iocb.chemweb.server.sparql.translator.UsedVariables;



public abstract class SqlIntercode extends SqlBaseClass
{
    protected final UsedVariables variables;


    protected SqlIntercode(UsedVariables variables)
    {
        this.variables = variables;
    }


    public abstract String translate();


    public final UsedVariables getVariables()
    {
        return variables;
    }
}
