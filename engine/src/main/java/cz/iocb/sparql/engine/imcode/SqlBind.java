package cz.iocb.sparql.engine.imcode;

import static java.util.stream.Collectors.joining;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ExpressionColumn;
import cz.iocb.sparql.engine.imcode.expression.SqlExpressionIntercode;
import cz.iocb.sparql.engine.imcode.expression.SqlExpressionIntercode.Restriction;
import cz.iocb.sparql.engine.imcode.expression.SqlNull;
import cz.iocb.sparql.engine.imcode.expression.SqlVariable;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.VariableBinding;
import cz.iocb.sparql.engine.translator.VariableBindings;



public final class SqlBind extends SqlIntercode
{
    private final SqlIntercode child;
    private final Variable variable;
    private final SqlExpressionIntercode expression;


    protected SqlBind(VariableBindings bindings, Variable variable, SqlExpressionIntercode expression,
            SqlIntercode child)
    {
        super(bindings, child.isDeterministic() && expression.isDeterministic());

        this.child = child;
        this.variable = variable;
        this.expression = expression;
    }


    public static SqlIntercode bind(Request request, Variable variable, SqlExpressionIntercode expression,
            SqlIntercode child)
    {
        return bind(request, variable, expression, child, null);
    }


    protected static SqlIntercode bind(Request request, Variable variable, SqlExpressionIntercode expression,
            SqlIntercode child, Restrictions restrictions)
    {
        Map<ResourceClass, List<Column>> columns = new HashMap<>();

        for(Entry<ResourceClass, List<Column>> e : expression.getMappings().entrySet())
        {
            List<Column> names = e.getKey().createColumns(request.getColumnMap(), variable);

            List<Column> list = new ArrayList<>(e.getKey().getColumnCount());

            for(int i = 0; i < e.getKey().getColumnCount(); i++)
            {
                if(e.getValue().get(i) instanceof ExpressionColumn)
                    list.add(names.get(i));
                else
                    list.add(e.getValue().get(i));
            }

            columns.put(e.getKey(), list);
        }


        VariableBindings bindings = child.getVariableBindings().restrict(restrictions);
        bindings.add(new VariableBinding(variable, columns, expression.canBeNull()));

        return new SqlBind(bindings, variable, expression, child);
    }


    @Override
    public SqlIntercode optimize(Request request, Restrictions restrictions, boolean reduced, boolean evalServices)
    {
        SqlExpressionIntercode optExpression = expression;
        SqlIntercode optChild = child;

        Restriction varRestriction = restrictions.getRestriction(variable);

        if(varRestriction == null)
            return optChild.optimize(request, restrictions, reduced, false);

        while(true)
        {
            boolean optReduced = reduced && optExpression.isDeterministic();
            Restrictions expressionRequirements = optExpression.getRequirements();
            Restrictions childRestrictions = new Restrictions(restrictions, expressionRequirements);

            optChild = optChild.optimize(request, childRestrictions, optReduced, evalServices);
            optExpression = optExpression.optimize(request, optChild.getVariableBindings(), varRestriction,
                    evalServices);

            if(optExpression.getRequirements().equals(expressionRequirements))
                break;
        }


        if(optChild.equals(SqlNoSolution.get()))
            return SqlNoSolution.get();

        if(optExpression.equals(SqlNull.get()) || !restrictions.contains(variable, optExpression.getResourceClasses()))
            return optChild.optimize(request, restrictions, reduced, false);

        if(optChild instanceof SqlUnion union)
        {
            List<SqlIntercode> childs = new ArrayList<>();

            for(SqlIntercode child : union.getChilds())
            {
                SqlExpressionIntercode expr = optExpression.optimize(request, child.getVariableBindings(),
                        varRestriction, evalServices);

                childs.add(bind(request, variable, expr, child, restrictions));
            }

            return SqlUnion.union(request, childs).optimize(request, restrictions, reduced, evalServices);
        }

        if(optChild instanceof SqlTableAccess access && optExpression instanceof SqlVariable var)
        {
            VariableBinding binding = access.getInternalVariableBinding(var.getVariable());
            VariableBindings internal = new VariableBindings(access.getInternalVariableBindings());
            internal.add(new VariableBinding(variable, binding.getMappings(), binding.canBeNull()));

            return SqlTableAccess.create(access.getTable(), access.getConditions(), internal, access.getReduced())
                    .optimize(request, restrictions, reduced, evalServices);
        }


        if(optExpression == expression && optChild == child && restrictions.isOptimized(bindings))
            return this;

        return bind(request, variable, optExpression, optChild, restrictions);
    }


    @Override
    public String translate(Request request)
    {
        if(!expression.getBinding().hasExpressionColumn())
            return child.translate(request);


        StringBuilder builder = new StringBuilder();

        builder.append("SELECT ");

        VariableBinding binding = getVariableBindings().get(variable);
        boolean hasSelect = false;

        for(Entry<ResourceClass, List<Column>> e : expression.getBinding().getMappings().entrySet())
        {
            List<Column> cols = e.getValue();
            ResourceClass resClass = e.getKey();
            List<Column> names = binding.getMapping(resClass);

            for(int i = 0; i < resClass.getColumnCount(); i++)
            {
                if(cols.get(i) instanceof ExpressionColumn)
                {
                    appendComma(builder, hasSelect);
                    hasSelect = true;

                    builder.append(cols.get(i));
                    builder.append(" AS ");
                    builder.append(names.get(i));
                }
            }
        }


        VariableBindings tmp = new VariableBindings(getVariableBindings());
        tmp.remove(variable);
        Set<Column> columns = tmp.getNonConstantColumns();

        if(!columns.isEmpty())
        {
            builder.append(", ");
            builder.append(columns.stream().map(Object::toString).collect(joining(", ")));
        }


        if(!child.equals(SqlEmptySolution.get()))
        {
            builder.append(" FROM (");
            builder.append(child.translate(request));
            builder.append(" ) AS tab");
        }


        return builder.toString();
    }


    @Override
    public boolean isDistinct(Request request, Collection<Variable> selected)
    {
        return child.isDistinct(request, selected);
    }


    @Override
    public boolean hasServiceSubpattern()
    {
        return child.hasServiceSubpattern();
    }


    @Override
    public void generateExplanation(StringBuilder builder, String indent)
    {
        builder.append("bind");

        indentInfo(builder, indent, true);
        expression.generateExplanation(builder, getIndent(indent, false) + "  ");
        builder.append(" as ");
        builder.append(variable);

        indentChild(builder, indent, true);
        child.generateExplanation(builder, getIndent(indent, true));
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(!(object instanceof SqlBind imcode))
            return false;

        if(!super.equals(imcode))
            return false;

        if(!Objects.equals(variable, imcode.variable))
            return false;

        if(!Objects.equals(expression, imcode.expression))
            return false;

        if(!Objects.equals(child, imcode.child))
            return false;

        return true;
    }


    @Override
    protected int getHashCode()
    {
        return Objects.hash(variable, expression, child);
    }
}
