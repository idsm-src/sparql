package cz.iocb.sparql.engine.imcode;

import static cz.iocb.sparql.engine.imcode.expression.SqlLiteral.falseValue;
import static cz.iocb.sparql.engine.imcode.expression.SqlLiteral.trueValue;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdBoolean;
import static java.util.stream.Collectors.joining;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.VirtualTable;
import cz.iocb.sparql.engine.imcode.expression.SqlBooleanExpression;
import cz.iocb.sparql.engine.imcode.expression.SqlExpressionIntercode;
import cz.iocb.sparql.engine.imcode.expression.SqlExpressionIntercode.Restriction;
import cz.iocb.sparql.engine.imcode.expression.SqlNull;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.Multiset;
import cz.iocb.sparql.engine.translator.VariableBindings;



/**
 * Keeps the solutions of the child for which every condition evaluates to true (FILTER).
 */
public final class SqlFilter extends SqlIntercode
{
    /**
     * Filtered solutions.
     */
    private final SqlIntercode child;

    /**
     * Conditions, all of which must hold.
     */
    private final List<SqlExpressionIntercode> conditions;


    /**
     * Creates the node.
     *
     * @param bindings the variable bindings
     * @param child the child node
     * @param conditions the conditions
     */
    protected SqlFilter(VariableBindings bindings, SqlIntercode child, List<SqlExpressionIntercode> conditions)
    {
        super(bindings, child.isDeterministic() && conditions.stream().allMatch(c -> c.isDeterministic()));

        this.child = child;
        this.conditions = conditions;
    }


    /**
     * Filter of the child by the conjunction of the conditions.
     *
     * @param request the current request
     * @param conditions the conditions
     * @param child the child node
     * @return filter of the child by the conjunction of the conditions
     */
    public static SqlIntercode filter(Request request, List<SqlExpressionIntercode> conditions, SqlIntercode child)
    {
        return filter(request, conditions, child, null);
    }


    /**
     * Filter exposing only what the parent needs.
     *
     * @param request the current request
     * @param conditions the conditions
     * @param child the child node
     * @param restrictions what the parent needs of the variables
     * @return filter exposing only what the parent needs
     */
    protected static SqlIntercode filter(Request request, List<SqlExpressionIntercode> conditions, SqlIntercode child,
            Restrictions restrictions)
    {
        VariableBindings bindings = child.getVariableBindings().restrict(restrictions);

        return new SqlFilter(bindings, child, conditions);
    }


    @Override
    public SqlIntercode optimize(Request request, Restrictions restrictions, boolean reduced, boolean evalServices)
    {
        SqlIntercode optChild = child;
        List<SqlExpressionIntercode> optCnds = conditions;

        boolean childReduced = reduced && optCnds.stream().allMatch(r -> r.isDeterministic());

        Restrictions childRestrictions = new Restrictions(restrictions);

        for(SqlExpressionIntercode cnd : optCnds)
            childRestrictions.add(cnd.getRequirements());

        while(true)
        {
            SqlIntercode newOptChild = optChild.optimize(request, childRestrictions, childReduced, evalServices);

            List<SqlExpressionIntercode> newOptCnds = new LinkedList<>();

            for(SqlExpressionIntercode cnd : optCnds)
            {
                SqlExpressionIntercode expression = cnd.optimize(request, newOptChild.getVariableBindings(),
                        new Restriction(xsdBoolean), evalServices);

                if(expression.equals(SqlNull.get()) || expression.equals(falseValue))
                    return SqlNoSolution.get();
                else if(expression instanceof SqlBooleanExpression expr && expr.isFalseOrError())
                    return SqlNoSolution.get();
                else if(!expression.equals(trueValue))
                    newOptCnds.add(expression);
            }

            optChild = newOptChild;
            optCnds = newOptCnds;

            boolean newChildReduced = reduced && optCnds.stream().allMatch(r -> r.isDeterministic());
            Restrictions newChildRestrictions = new Restrictions(restrictions);
            newOptCnds.forEach(c -> newChildRestrictions.add(c.getRequirements()));

            if(newChildReduced == childReduced && newChildRestrictions.equals(childRestrictions))
                break;

            childRestrictions = newChildRestrictions;
            childReduced = newChildReduced;
        }


        if(optChild.equals(SqlNoSolution.get()))
            return SqlNoSolution.get();

        if(optCnds.isEmpty())
            return optChild.optimize(request, restrictions, reduced, evalServices);

        if(optChild instanceof SqlFilter filter)
        {
            List<SqlExpressionIntercode> merged = new ArrayList<>();
            merged.addAll(filter.conditions);
            merged.addAll(optCnds);

            List<SqlExpressionIntercode> cnds = merged.stream().map(c -> c.optimize(request,
                    filter.child.getVariableBindings(), new Restriction(xsdBoolean), evalServices)).toList();

            return filter(request, cnds, filter.child).optimize(request, restrictions, reduced, evalServices);
        }

        if(optChild instanceof SqlUnion union)
        {
            List<SqlIntercode> childs = new ArrayList<>();

            for(SqlIntercode child : union.getChilds())
            {
                List<SqlExpressionIntercode> cnds = optCnds.stream().map(c -> c.optimize(request,
                        child.getVariableBindings(), new Restriction(xsdBoolean), evalServices)).toList();

                childs.add(filter(request, cnds, child, restrictions));
            }

            return SqlUnion.union(request, childs).optimize(request, restrictions, reduced, evalServices);
        }


        if(optCnds.equals(conditions) && optChild == child && restrictions.isOptimized(bindings))
            return this;

        return filter(request, optCnds, optChild, restrictions);
    }


    @Override
    public String translate(Request request)
    {
        StringBuilder builder = new StringBuilder();

        builder.append("SELECT ");

        Set<Column> columns = getVariableBindings().getNonConstantColumns();

        if(!columns.isEmpty())
            builder.append(columns.stream().map(Object::toString).collect(joining(", ")));
        else
            builder.append("1");

        builder.append(" FROM (");
        builder.append(child.translate(request));
        builder.append(" ) AS tab");

        builder.append(" WHERE ");

        builder.append(conditions.stream().map(cnd -> cnd.get(xsdBoolean).get(0).toString()).collect(joining(" AND ")));

        return builder.toString();
    }


    @Override
    public boolean isDistinct(Request request, Collection<Variable> selected)
    {
        return child.isDistinct(request, selected);
    }


    /**
     * Filtered solutions.
     *
     * @return filtered solutions
     */
    public final SqlIntercode getChild()
    {
        return child;
    }


    /**
     * Conditions, all of which must hold.
     *
     * @return conditions, all of which must hold
     */
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
    public Set<VirtualTable> getVirtualTables()
    {
        Set<VirtualTable> tables = getVirtualTables(child);
        tables.addAll(getVirtualTables(conditions));
        return tables;
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
