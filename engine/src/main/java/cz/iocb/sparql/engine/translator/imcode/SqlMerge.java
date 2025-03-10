package cz.iocb.sparql.engine.translator.imcode;

import static java.util.stream.Collectors.joining;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.UsedVariable;
import cz.iocb.sparql.engine.translator.UsedVariables;



public class SqlMerge extends SqlIntercode
{
    private final SqlIntercode child;
    private final String variable1;
    private final String variable2;
    private final Map<Column, Column> columnMap;


    protected SqlMerge(UsedVariables variables, String variable1, String variable2, SqlIntercode child,
            Map<Column, Column> columnMap)
    {
        super(variables, child.isDeterministic());

        this.variable1 = variable1;
        this.variable2 = variable2;
        this.child = child;
        this.columnMap = columnMap;
    }


    public static SqlIntercode create(Request request, String variable1, String variable2, SqlIntercode child)
    {
        return create(request, variable1, variable2, child, null);
    }


    protected static SqlIntercode create(Request request, String variable1, String variable2, SqlIntercode child,
            Set<String> restrictions)
    {
        /* special cases */

        if(child == SqlNoSolution.get())
            return SqlNoSolution.get();

        if(child instanceof SqlUnion union)
            return SqlUnion.union(request, union.getChilds().stream()
                    .map(c -> create(request, variable1, variable2, c, restrictions)).toList());


        /* special merge */

        UsedVariables usedVars1 = new UsedVariables();
        UsedVariables usedVars2 = new UsedVariables();

        for(UsedVariable variable : child.getVariables().getValues())
        {
            if(!variable.getName().equals(variable2))
                usedVars1.add(variable);
            else
                usedVars2.add(new UsedVariable(variable1, variable.getMappings(), variable.canBeNull()));
        }

        Map<Column, Column> map = new HashMap<Column, Column>();
        UsedVariables variables = getJoinUsedVariables(request, usedVars1, usedVars2, null, null, restrictions, map);

        if(variables == null)
            return SqlNoSolution.get();

        return new SqlMerge(variables, variable1, variable2, child, map);
    }


    @Override
    public SqlIntercode optimize(Request request, Set<String> restrictions, boolean reduced)
    {
        if(restrictions == null)
            return this;

        HashSet<String> contextRestrictions = new HashSet<String>(restrictions);
        contextRestrictions.add(variable1);
        contextRestrictions.add(variable2);

        SqlIntercode optimizedContext = child.optimize(request, contextRestrictions, reduced);

        if(child.getVariables().get(variable1) == null && !restrictions.contains(variable1))
            return child.optimize(request, restrictions, reduced);

        if(optimizedContext.getVariables().get(variable2) == null)
            return child.optimize(request, restrictions, reduced);

        return create(request, variable1, variable2, optimizedContext, restrictions);
    }


    @Override
    public String translate(Request request)
    {
        UsedVariable usedVariable1 = child.getVariables().get(variable1);
        UsedVariable usedVariable2 = child.getVariables().get(variable2);

        Set<Column> columns = variables.getNonConstantColumns();

        StringBuilder builder = new StringBuilder();

        builder.append("SELECT ");

        if(!columns.isEmpty())
            builder.append(columns.stream().map(c -> (columnMap.get(c) != null ? columnMap.get(c) + " AS " : "") + c)
                    .collect(joining(", ")));
        else
            builder.append("1");

        builder.append(" FROM (");
        builder.append(child.translate(request));
        builder.append(" ) AS ");
        builder.append("tab");

        String condition = generateJoinCondition(usedVariable1, usedVariable2, null, null);

        if(condition != null)
        {
            builder.append(" WHERE ");
            builder.append(condition);
        }

        return builder.toString();
    }
}
