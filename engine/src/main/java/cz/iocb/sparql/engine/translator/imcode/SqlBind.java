package cz.iocb.sparql.engine.translator.imcode;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ExpressionColumn;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.mapping.classes.IriClass;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.mapping.classes.UserIriClass;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.UsedVariable;
import cz.iocb.sparql.engine.translator.UsedVariables;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlExpressionIntercode;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlIri;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlLiteral;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlNodeValue;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlNull;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlVariable;



public class SqlBind extends SqlIntercode
{
    private static final Column expressionColumn = new TableColumn("#expression");

    private final SqlIntercode child;
    private final String variableName;
    private final SqlExpressionIntercode expression;


    protected SqlBind(UsedVariables variables, String variableName, SqlExpressionIntercode expression,
            SqlIntercode child)
    {
        super(variables, child.isDeterministic() && expression.isDeterministic());

        this.child = child;
        this.variableName = variableName;
        this.expression = expression;
    }


    public static SqlIntercode bind(Request request, String varName, SqlExpressionIntercode expression,
            SqlIntercode child)
    {
        return bind(request, varName, expression, child, null);
    }


    protected static SqlIntercode bind(Request request, String varName, SqlExpressionIntercode expression,
            SqlIntercode child, Restrictions restrictions)
    {
        UsedVariable variable = null;

        if(expression instanceof SqlIri iri)
        {
            IriClass resClass = iri.getIriClass();
            List<Column> columns = request.getColumns(resClass, iri.getIri());
            variable = new UsedVariable(varName, resClass, columns, expression.canBeNull());
        }
        else if(expression instanceof SqlLiteral literal)
        {
            LiteralClass resClass = literal.getLiteralClass();
            List<Column> columns = request.getColumns(resClass, literal.getLiteral());
            variable = new UsedVariable(varName, resClass, columns, expression.canBeNull());
        }
        else if(expression instanceof SqlVariable var)
        {
            UsedVariable source = child.getVariables().get(var.getName());
            variable = new UsedVariable(varName, source.getMappings(), expression.canBeNull());
        }
        else
        {
            Set<ResourceClass> resClasses = expression.getResourceClasses();

            for(ResourceClass resClass : resClasses)
            {
                if(!resClass.canBeDerivatedFromGeneral())
                {
                    ResourceClass genClass = resClass.getGeneralClass();

                    if(resClasses.stream().filter(c -> c.getGeneralClass() == genClass).count() > 1)
                    {
                        resClasses = resClasses.stream().filter(c -> c.getGeneralClass() != genClass).collect(toSet());
                        resClasses.add(genClass);
                    }
                }
            }

            UsedVariable bindVar = new UsedVariable(varName, expression.canBeNull());
            resClasses.stream().forEach(r -> bindVar.addMapping(r, r.createColumns(request.getColumnMap(), varName)));
            variable = bindVar;
        }

        UsedVariables variables = child.getVariables().restrict(restrictions);
        variables.add(variable);

        return new SqlBind(variables, varName, expression, child);
    }


    @Override
    public SqlIntercode optimize(Request request, Restrictions restrictions, boolean reduced, boolean evalServices)
    {
        SqlExpressionIntercode optExpression = expression;
        SqlIntercode optChild = child;

        while(true)
        {
            boolean optReduced = reduced && optExpression.isDeterministic();
            Restrictions expressionRequirements = optExpression.getRequirements(null);
            Restrictions childRestrictions = new Restrictions(restrictions, expressionRequirements);

            optChild = optChild.optimize(request, childRestrictions, optReduced, evalServices);
            optExpression = optExpression.optimize(request, optChild.getVariables(), evalServices);

            if(optExpression.getRequirements(null).equals(expressionRequirements))
                break;
        }


        if(optChild == SqlNoSolution.get())
            return SqlNoSolution.get();

        if(optExpression == SqlNull.get() || !restrictions.contains(variableName, optExpression.getResourceClasses()))
            return optChild.optimize(request, restrictions, reduced, false);

        if(optChild instanceof SqlUnion union)
        {
            List<SqlIntercode> childs = new ArrayList<SqlIntercode>();

            for(SqlIntercode child : union.getChilds())
            {
                SqlExpressionIntercode expr = optExpression.optimize(request, child.getVariables(), evalServices);

                childs.add(bind(request, variableName, expr, child, restrictions));
            }

            return SqlUnion.union(request, childs).optimize(request, restrictions, reduced, evalServices);
        }

        if(optChild instanceof SqlTableAccess access && optExpression instanceof SqlVariable variable)
        {
            UsedVariable var = access.getInternalVariable(variable.getName());
            UsedVariables internal = new UsedVariables(access.getInternalVariables());
            internal.add(new UsedVariable(variableName, var.getMappings(), var.canBeNull()));

            return SqlTableAccess.create(access.getTable(), access.getConditions(), internal, access.getReduced())
                    .optimize(request, restrictions, reduced, evalServices);
        }


        if(optExpression == expression && optChild == child && restrictions.isOptimized(variables))
            return this;

        return bind(request, variableName, optExpression, optChild, restrictions);
    }


    @Override
    public String translate(Request request)
    {
        if(expression instanceof SqlNodeValue)
            return child.translate(request);


        UsedVariable variable = getVariables().get(variableName);
        boolean expand = isExpressionExpansionNeeded(expression);

        Column column = expand ? expressionColumn : variable.getMapping(variable.getClasses().iterator().next()).get(0);

        UsedVariables tmp = new UsedVariables(getVariables());
        tmp.remove(variableName);
        Set<Column> columns = tmp.getNonConstantColumns();

        StringBuilder builder = new StringBuilder();

        if(expand)
        {
            builder.append("SELECT ");

            if(variable.hasMapping())
                builder.append(translateExpressionExpansion(column, variable, expression.isBoxed()));

            if(variable.hasMapping() && !columns.isEmpty())
                builder.append(", ");

            if(!columns.isEmpty())
                builder.append(columns.stream().map(Object::toString).collect(joining(", ")));

            builder.append(" FROM (");
        }

        builder.append("SELECT ");
        builder.append(expression.translate(request));
        builder.append(" AS ");
        builder.append(column);

        if(!columns.isEmpty())
        {
            builder.append(", ");
            builder.append(columns.stream().map(Object::toString).collect(joining(", ")));
        }

        if(child != SqlEmptySolution.get())
        {
            builder.append(" FROM (");
            builder.append(child.translate(request));
            builder.append(" ) AS tab");
        }

        if(expand)
            builder.append(" ) AS tab");

        return builder.toString();
    }


    protected static boolean isExpressionExpansionNeeded(SqlExpressionIntercode expression)
    {
        if(expression.isBoxed() || expression.getResourceClasses().size() != 1)
            return true;

        ResourceClass resourceClass = expression.getResourceClasses().iterator().next();

        if(resourceClass instanceof UserIriClass || resourceClass.getColumnCount() != 1)
            return true;

        return false;
    }


    protected static String translateExpressionExpansion(Column column, UsedVariable variable, boolean isBoxed)
    {
        Set<ResourceClass> resClasses = variable.getClasses();

        StringBuilder builder = new StringBuilder();
        boolean hasSelect = false;

        for(ResourceClass resClass : resClasses)
        {
            if(variable.getMapping(resClass) == null)
                continue;

            List<Column> columns = null;

            if(isBoxed)
            {
                ResourceClass generalClass = resClass.getGeneralClass();
                boolean check = resClasses.stream().filter(r -> r.getGeneralClass() == generalClass).count() > 1;
                columns = resClass.fromBoxedExpression(column, check);
            }
            else
            {
                Column expression = new ExpressionColumn(resClass.fromGeneralExpression(column.toString()));
                columns = resClass.fromExpression(expression);
            }

            for(int i = 0; i < resClass.getColumnCount(); i++)
            {
                appendComma(builder, hasSelect);
                hasSelect = true;

                builder.append(columns.get(i));
                builder.append(" AS ");
                builder.append(variable.getMapping(resClass).get(i));
            }
        }

        return builder.toString();
    }


    @Override
    public boolean isDistinct(Request request, Collection<String> selected)
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
        builder.append(variableName);

        indentChild(builder, indent, true);
        child.generateExplanation(builder, getIndent(indent, true));
    }
}
