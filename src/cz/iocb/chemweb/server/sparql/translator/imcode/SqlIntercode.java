package cz.iocb.chemweb.server.sparql.translator.imcode;

import java.util.HashSet;
import cz.iocb.chemweb.server.sparql.engine.Request;
import cz.iocb.chemweb.server.sparql.translator.UsedVariables;



public abstract class SqlIntercode extends SqlBaseClass
{
    protected final UsedVariables variables;
    protected final boolean isDeterministic;


    protected SqlIntercode(UsedVariables variables, boolean isDeterministic)
    {
        this.variables = variables;
        this.isDeterministic = isDeterministic;
    }


    public abstract SqlIntercode optimize(Request request, HashSet<String> restrictions, boolean reduced);


    public abstract String translate();


    public final UsedVariables getVariables()
    {
        return variables;
    }


    public boolean isDeterministic()
    {
        return isDeterministic;
    }
}
