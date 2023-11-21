package cz.iocb.chemweb.server.sparql.translator.imcode;

import static java.util.stream.Collectors.joining;
import java.util.HashSet;
import java.util.Set;
import cz.iocb.chemweb.server.sparql.database.Column;
import cz.iocb.chemweb.server.sparql.translator.UsedVariable;
import cz.iocb.chemweb.server.sparql.translator.UsedVariables;



public class SqlDistinct extends SqlIntercode
{
    private final SqlIntercode child;
    private final Set<String> distinctVariables;


    protected SqlDistinct(UsedVariables variables, SqlIntercode child, Set<String> distinctVariables)
    {
        super(variables, child.isDeterministic());

        this.child = child;
        this.distinctVariables = distinctVariables;
    }


    public static SqlIntercode create(SqlIntercode child, Set<String> distinct)
    {
        return create(child, distinct, null);
    }


    protected static SqlIntercode create(SqlIntercode child, Set<String> distinctVariables, Set<String> restrictions)
    {
        /* special cases */

        if(child == SqlNoSolution.get())
            return SqlNoSolution.get();

        if(child == SqlEmptySolution.get())
            return SqlEmptySolution.get();

        if(child instanceof SqlTableAccess && ((SqlTableAccess) child).isDistinct(distinctVariables))
            return child.optimize(restrictions, true);


        /* standard distinct */

        UsedVariables variables = new UsedVariables();

        for(UsedVariable var : child.getVariables().getValues())
            if(distinctVariables.contains(var.getName())
                    && (restrictions == null || restrictions.contains(var.getName())))
                variables.add(var);

        return new SqlDistinct(variables, child, distinctVariables);
    }


    @Override
    public SqlIntercode optimize(Set<String> restrictions, boolean reduced)
    {
        if(restrictions == null)
            return this;

        HashSet<String> childRestriction = new HashSet<String>(restrictions);
        childRestriction.addAll(distinctVariables);

        return create(child.optimize(childRestriction, true), distinctVariables, restrictions);
    }


    @Override
    public String translate()
    {
        StringBuilder builder = new StringBuilder();

        builder.append("SELECT ");

        Set<Column> columns = getVariables().getNonConstantColumns();

        if(!columns.isEmpty())
            builder.append(columns.stream().map(Object::toString).collect(joining(", ")));
        else
            builder.append("1");

        builder.append(" FROM (");
        builder.append(child.translate());
        builder.append(" ) AS tab");

        builder.append(" GROUP BY ");

        Set<Column> groupColumns = child.getVariables().restrict(distinctVariables).getNonConstantColumns();

        if(!groupColumns.isEmpty())
            builder.append(groupColumns.stream().map(Object::toString).collect(joining(", ")));
        else
            builder.append("1");

        return builder.toString();
    }
}
