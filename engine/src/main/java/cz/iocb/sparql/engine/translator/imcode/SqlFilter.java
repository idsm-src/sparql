package cz.iocb.sparql.engine.translator.imcode;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdBoolean;
import static cz.iocb.sparql.engine.translator.imcode.expression.SqlLiteral.falseValue;
import static cz.iocb.sparql.engine.translator.imcode.expression.SqlLiteral.trueValue;
import static java.util.stream.Collectors.joining;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.Multiset;
import cz.iocb.sparql.engine.translator.UsedVariables;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlBinaryComparison;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlExpressionIntercode;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlNull;



public final class SqlFilter extends SqlIntercode
{
    private final SqlIntercode child;
    private final List<SqlExpressionIntercode> conditions;


    protected SqlFilter(UsedVariables variables, SqlIntercode child, List<SqlExpressionIntercode> conditions)
    {
        super(variables, child.isDeterministic() && conditions.stream().allMatch(c -> c.isDeterministic()));

        this.child = child;
        this.conditions = conditions;
    }


    public static SqlIntercode filter(Request request, List<SqlExpressionIntercode> conditions, SqlIntercode child)
    {
        return filter(request, conditions, child, null);
    }


    protected static SqlIntercode filter(Request request, List<SqlExpressionIntercode> conditions, SqlIntercode child,
            Restrictions restrictions)
    {
        UsedVariables variables = child.getVariables().restrict(restrictions);

        return new SqlFilter(variables, child, conditions);
    }


    @Override
    public SqlIntercode optimize(Request request, Restrictions restrictions, boolean reduced, boolean evalServices)
    {
        SqlIntercode optChild = child;
        List<SqlExpressionIntercode> optCnds = conditions;

        boolean childReduced = reduced && optCnds.stream().allMatch(r -> r.isDeterministic());

        Restrictions childRestrictions = new Restrictions(restrictions);

        for(SqlExpressionIntercode cnd : optCnds)
            childRestrictions.add(cnd.getRequirements(Set.of(xsdBoolean)));

        while(true)
        {
            SqlIntercode newOptChild = optChild.optimize(request, childRestrictions, childReduced, evalServices);

            List<SqlExpressionIntercode> newOptCnds = new LinkedList<SqlExpressionIntercode>();

            for(SqlExpressionIntercode cnd : optCnds)
            {
                SqlExpressionIntercode expression = cnd.optimize(request, newOptChild.getVariables(), evalServices);

                if(expression == SqlNull.get() || expression == falseValue)
                    return SqlNoSolution.get();
                else if(expression instanceof SqlBinaryComparison binary && binary.isAlwaysFalseOrNull())
                    return SqlNoSolution.get();
                else if(expression != trueValue)
                    newOptCnds.add(expression);
            }

            optChild = newOptChild;
            optCnds = newOptCnds;

            boolean newChildReduced = reduced && optCnds.stream().allMatch(r -> r.isDeterministic());
            Restrictions newChildRestrictions = new Restrictions(restrictions);
            newOptCnds.forEach(c -> newChildRestrictions.add(c.getRequirements(Set.of(xsdBoolean))));

            if(newChildReduced == childReduced && newChildRestrictions.equals(childRestrictions))
                break;

            childRestrictions = newChildRestrictions;
            childReduced = newChildReduced;
        }


        if(optChild == SqlNoSolution.get())
            return SqlNoSolution.get();

        if(optCnds.isEmpty())
            return optChild.optimize(request, restrictions, reduced, evalServices);

        if(optChild instanceof SqlFilter filter)
        {
            ArrayList<SqlExpressionIntercode> merged = new ArrayList<SqlExpressionIntercode>();
            merged.addAll(filter.conditions);
            merged.addAll(optCnds);

            List<SqlExpressionIntercode> cnds = merged.stream()
                    .map(c -> c.optimize(request, filter.child.getVariables(), evalServices)).toList();

            return filter(request, cnds, filter.child).optimize(request, restrictions, reduced, evalServices);
        }

        if(optChild instanceof SqlUnion union)
        {
            List<SqlIntercode> childs = new ArrayList<SqlIntercode>();

            for(SqlIntercode child : union.getChilds())
            {
                List<SqlExpressionIntercode> cnds = optCnds.stream()
                        .map(c -> c.optimize(request, child.getVariables(), evalServices)).toList();

                childs.add(filter(request, cnds, child, restrictions));
            }

            return SqlUnion.union(request, childs).optimize(request, restrictions, reduced, evalServices);
        }


        if(optCnds.equals(conditions) && optChild == child && restrictions.isOptimized(variables))
            return this;

        return filter(request, optCnds, optChild, restrictions);
    }


    @Override
    public String translate(Request request)
    {
        StringBuilder builder = new StringBuilder();

        builder.append("SELECT ");

        Set<Column> columns = getVariables().getNonConstantColumns();

        if(!columns.isEmpty())
            builder.append(columns.stream().map(Object::toString).collect(joining(", ")));
        else
            builder.append("1");

        builder.append(" FROM (");
        builder.append(child.translate(request));
        builder.append(" ) AS tab");

        builder.append(" WHERE ");

        builder.append(conditions.stream().map(cnd -> cnd.translate(request)).collect(joining(" AND ")));

        return builder.toString();
    }


    @Override
    public boolean isDistinct(Request request, Collection<String> selected)
    {
        return child.isDistinct(request, selected);
    }


    public final SqlIntercode getChild()
    {
        return child;
    }


    public final List<SqlExpressionIntercode> getConditions()
    {
        return conditions;
    }


    @Override
    public boolean hasServiceSubpattern()
    {
        return child.hasServiceSubpattern();
    }


    @Override
    public void generateExplanation(StringBuilder builder, String indent)
    {
        builder.append("filter");

        for(SqlExpressionIntercode cnd : conditions)
        {
            indentInfo(builder, indent, true);
            cnd.generateExplanation(builder, getIndent(indent, false));
        }

        indentChild(builder, indent, true);
        child.generateExplanation(builder, getIndent(indent, true));
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(!(object instanceof SqlFilter imcode))
            return false;

        if(!super.equals(imcode))
            return false;

        if(!Objects.equals(new Multiset<>(conditions), new Multiset<>(imcode.conditions)))
            return false;

        if(!Objects.equals(child, imcode.child))
            return false;

        return true;
    }


    @Override
    protected int getHashCode()
    {
        return Objects.hash(new Multiset<>(conditions), child);
    }
}
