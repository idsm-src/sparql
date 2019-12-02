package cz.iocb.chemweb.server.sparql.translator.imcode;

import java.util.HashSet;
import cz.iocb.chemweb.server.sparql.engine.Request;
import cz.iocb.chemweb.server.sparql.translator.UsedVariables;



public class SqlDistinct extends SqlIntercode
{
    private final SqlIntercode child;


    protected SqlDistinct(UsedVariables variables, SqlIntercode child)
    {
        super(variables, child.isDeterministic());
        this.child = child;
    }


    public static SqlIntercode create(SqlIntercode child)
    {
        if(child == SqlNoSolution.get())
            return SqlNoSolution.get();

        if(child instanceof SqlTableAccess && ((SqlTableAccess) child).isDistinct())
            return child;

        return new SqlDistinct(child.getVariables(), child);
    }


    @Override
    public SqlIntercode optimize(Request request, HashSet<String> restrictions, boolean reduced)
    {
        return create(child.optimize(request, restrictions, reduced));
    }


    @Override
    public String translate()
    {
        StringBuilder builder = new StringBuilder();

        builder.append("SELECT DISTINCT * ");

        builder.append(" FROM (");
        builder.append(child.translate());
        builder.append(" ) AS tab");

        return builder.toString();
    }
}
