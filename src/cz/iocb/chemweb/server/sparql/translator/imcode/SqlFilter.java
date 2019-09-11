package cz.iocb.chemweb.server.sparql.translator.imcode;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import cz.iocb.chemweb.server.sparql.engine.Request;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.translator.UsedVariable;
import cz.iocb.chemweb.server.sparql.translator.UsedVariables;
import cz.iocb.chemweb.server.sparql.translator.expression.SimpleVariableAccessor;
import cz.iocb.chemweb.server.sparql.translator.expression.VariableAccessor;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlBinaryComparison;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlEffectiveBooleanValue;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlExpressionIntercode;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlNull;



public class SqlFilter extends SqlIntercode
{
    private final SqlIntercode child;
    private final List<SqlExpressionIntercode> conditions;


    protected SqlFilter(UsedVariables variables, SqlIntercode child, List<SqlExpressionIntercode> conditions)
    {
        super(variables, child.isDeterministic() && conditions.stream().allMatch(c -> c.isDeterministic()));
        this.child = child;
        this.conditions = conditions;
    }


    public static SqlIntercode filter(List<SqlExpressionIntercode> filterExpressions, SqlIntercode child)
    {
        if(child == SqlNoSolution.get())
            return SqlNoSolution.get();


        if(child instanceof SqlUnion)
        {
            SqlIntercode union = SqlNoSolution.get();

            for(SqlIntercode subChild : ((SqlUnion) child).getChilds())
            {
                VariableAccessor accessor = new SimpleVariableAccessor(subChild.getVariables());

                List<SqlExpressionIntercode> optimizedExpressions = filterExpressions.stream()
                        .map(f -> SqlEffectiveBooleanValue.create(f.optimize(accessor))).collect(Collectors.toList());

                union = SqlUnion.union(union, filter(optimizedExpressions, subChild));
            }

            return union;
        }


        List<SqlExpressionIntercode> validExpressions = new LinkedList<SqlExpressionIntercode>();
        boolean isFalse = false;

        for(SqlExpressionIntercode expression : filterExpressions)
        {
            if(expression == SqlNull.get() || expression == SqlEffectiveBooleanValue.falseValue)
                isFalse = true;
            else if(expression instanceof SqlBinaryComparison
                    && ((SqlBinaryComparison) expression).isAlwaysFalseOrNull())
                isFalse = true;
            else if(expression != SqlEffectiveBooleanValue.trueValue)
                validExpressions.add(expression);
        }

        if(isFalse)
            return SqlNoSolution.get();

        if(validExpressions.isEmpty())
            return child;

        return new SqlFilter(child.getVariables(), child, validExpressions);
    }


    @Override
    public SqlIntercode optimize(Request request, HashSet<String> restrictions, boolean reduced)
    {
        reduced = reduced & conditions.stream().allMatch(r -> r.isDeterministic());

        HashSet<String> childRestrictions = new HashSet<String>(restrictions);

        for(SqlExpressionIntercode condition : conditions)
            childRestrictions.addAll(condition.getVariables());

        SqlIntercode optimized = child.optimize(request, childRestrictions, reduced);

        return filter(conditions, optimized);
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
