package cz.iocb.chemweb.server.sparql.translator.sql.imcode;

import cz.iocb.chemweb.server.sparql.translator.sql.UsedVariables;



public abstract class SqlIntercode
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


    protected static void appendComma(StringBuilder builder, boolean condition)
    {
        if(condition)
            builder.append(", ");
    }


    protected static void appendAnd(StringBuilder builder, boolean condition)
    {
        if(condition)
            builder.append(" AND ");
    }


    protected static void appendOr(StringBuilder builder, boolean condition)
    {
        if(condition)
            builder.append(" OR ");
    }
}
