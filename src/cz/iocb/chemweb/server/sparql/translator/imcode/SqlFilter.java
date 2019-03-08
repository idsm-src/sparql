package cz.iocb.chemweb.server.sparql.translator.imcode;

import java.util.HashSet;
import java.util.List;
import cz.iocb.chemweb.server.db.DatabaseSchema;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.translator.UsedVariable;
import cz.iocb.chemweb.server.sparql.translator.UsedVariables;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlExpressionIntercode;



public class SqlFilter extends SqlIntercode
{
    private final SqlIntercode child;
    private final List<SqlExpressionIntercode> conditions;


    public SqlFilter(UsedVariables variables, SqlIntercode child, List<SqlExpressionIntercode> conditions)
    {
        super(variables, child.isDeterministic() && conditions.stream().allMatch(c -> c.isDeterministic()));
        this.child = child;
        this.conditions = conditions;
    }


    @Override
    public SqlIntercode optimize(DatabaseSchema schema, HashSet<String> restrictions, boolean reduced)
    {
        reduced = reduced & conditions.stream().allMatch(r -> r.isDeterministic());

        HashSet<String> childRestrictions = new HashSet<String>(restrictions);

        for(SqlExpressionIntercode condition : conditions)
            childRestrictions.addAll(condition.getVariables());

        SqlIntercode optimized = child.optimize(schema, childRestrictions, reduced);

        return new SqlFilter(optimized.getVariables().restrict(restrictions), optimized, conditions);
    }


    @Override
    public String translate()
    {
        StringBuilder builder = new StringBuilder();

        builder.append("SELECT ");
        boolean hasSelect = false;

        for(UsedVariable variable : variables.getValues())
        {
            for(ResourceClass resClass : variable.getClasses())
            {
                for(int i = 0; i < resClass.getPatternPartsCount(); i++)
                {
                    appendComma(builder, hasSelect);
                    hasSelect = true;

                    builder.append(resClass.getSqlColumn(variable.getName(), i));
                }
            }
        }

        if(!hasSelect)
            builder.append("1");

        builder.append(" FROM (");
        builder.append(child.translate());
        builder.append(" ) AS tab");

        builder.append(" WHERE ");

        boolean hasCondition = false;

        for(SqlExpressionIntercode condition : conditions)
        {
            appendAnd(builder, hasCondition);
            hasCondition = true;

            builder.append(condition.translate());
        }

        return builder.toString();
    }
}
