package cz.iocb.chemweb.server.sparql.translator.imcode;

import static java.util.stream.Collectors.joining;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import cz.iocb.chemweb.server.sparql.database.Column;
import cz.iocb.chemweb.server.sparql.database.TableColumn;
import cz.iocb.chemweb.server.sparql.mapping.classes.DateConstantZoneClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.DateTimeConstantZoneClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.IriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.LangStringConstantTagClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.LiteralClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIntBlankNodeClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserStrBlankNodeClass;
import cz.iocb.chemweb.server.sparql.translator.UsedVariable;
import cz.iocb.chemweb.server.sparql.translator.UsedVariables;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlExpressionIntercode;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlIri;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlLiteral;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlNodeValue;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlNull;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlVariable;



public class SqlBind extends SqlIntercode
{
    private static final Column expressionColumn = new TableColumn("#expression");

    SqlIntercode child;
    String variableName;
    SqlExpressionIntercode expression;


    protected SqlBind(UsedVariables variables, String variableName, SqlExpressionIntercode expression,
            SqlIntercode child)
    {
        super(variables, child.isDeterministic() && expression.isDeterministic());

        this.child = child;
        this.variableName = variableName;
        this.expression = expression;
    }


    public static SqlIntercode bind(String variableName, SqlExpressionIntercode expression, SqlIntercode child)
    {
        return bind(variableName, expression, child, null, false);
    }


    protected static SqlIntercode bind(String variableName, SqlExpressionIntercode expression, SqlIntercode child,
            Set<String> restrictions, boolean reduced)
    {
        /* special cases */

        if(child == SqlNoSolution.get())
            return SqlNoSolution.get();

        if(expression == SqlNull.get() || restrictions != null && !restrictions.contains(variableName))
            return restrictions == null ? child : child.optimize(restrictions, reduced);

        if(child instanceof SqlUnion)
        {
            SqlIntercode union = SqlNoSolution.get();

            for(SqlIntercode intercode : ((SqlUnion) child).getChilds())
                union = SqlUnion.union(union, bind(variableName, expression, intercode, restrictions, reduced));

            return restrictions == null ? union : union.optimize(restrictions, reduced);
        }


        /* standard bind */

        UsedVariable variable = null;

        if(expression instanceof SqlIri)
        {
            IriClass resClass = ((SqlIri) expression).getIriClass();
            List<Column> columns = resClass.toColumns(((SqlIri) expression).getIri());
            variable = new UsedVariable(variableName, resClass, columns, expression.canBeNull());
        }
        else if(expression instanceof SqlLiteral)
        {
            LiteralClass resClass = ((SqlLiteral) expression).getLiteralClass();
            List<Column> columns = resClass.toColumns(((SqlLiteral) expression).getLiteral());
            variable = new UsedVariable(variableName, resClass, columns, expression.canBeNull());
        }
        else if(expression instanceof SqlVariable)
        {
            UsedVariable source = child.getVariables().get(((SqlVariable) expression).getName());
            variable = new UsedVariable(variableName, source.getMappings(), expression.canBeNull());
        }
        else
        {
            UsedVariable bindVariable = new UsedVariable(variableName, expression.canBeNull());
            expression.getResourceClasses().stream()
                    .forEach(res -> bindVariable.addMapping(res, res.createColumns(variableName)));
            variable = bindVariable;
        }

        UsedVariables variables = child.getVariables().restrict(restrictions);
        variables.add(variable);

        return new SqlBind(variables, variableName, expression, child);
    }


    @Override
    public SqlIntercode optimize(Set<String> restrictions, boolean reduced)
    {
        if(!restrictions.contains(variableName))
            return child.optimize(restrictions, reduced);

        SqlExpressionIntercode optExpression = expression;
        SqlIntercode optChild = child;

        while(true)
        {
            Set<String> expressionVariables = optExpression.getReferencedVariables();

            HashSet<String> childRestrictions = new HashSet<String>(restrictions);
            childRestrictions.addAll(expressionVariables);

            optChild = optChild.optimize(childRestrictions, reduced && optExpression.isDeterministic());
            optExpression = optExpression.optimize(optChild.getVariables());

            if(optExpression.getReferencedVariables().equals(expressionVariables))
                break;
        }

        return bind(variableName, optExpression, optChild, restrictions, reduced);
    }


    @Override
    public String translate()
    {
        if(expression instanceof SqlNodeValue)
            return child.translate();


        UsedVariable variable = getVariables().get(variableName);
        boolean expand = isExpressionExpansionNeeded(expression);

        Column column = expand ? expressionColumn : variable.getMapping(variable.getClasses().iterator().next()).get(0);
        Set<Column> columns = child.getVariables().getNonConstantColumns();

        StringBuilder builder = new StringBuilder();

        if(expand)
        {
            builder.append("SELECT ");

            builder.append(translateExpressionExpansion(column, variable));

            if(!columns.isEmpty())
            {
                builder.append(", ");
                builder.append(columns.stream().map(Object::toString).collect(joining(", ")));
            }

            builder.append(" FROM (");
        }

        builder.append("SELECT ");
        builder.append(expression.translate());
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
            builder.append(child.translate());
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


    protected static String translateExpressionExpansion(Column column, UsedVariable variable)
    {
        Set<ResourceClass> resClasses = variable.getClasses();

        boolean isBoxed = SqlExpressionIntercode.isBoxed(resClasses);
        boolean splitDateTime = resClasses.stream().filter(r -> r instanceof DateTimeConstantZoneClass).count() > 1;
        boolean splitDate = resClasses.stream().filter(r -> r instanceof DateConstantZoneClass).count() > 1;
        boolean splitLang = resClasses.stream().filter(r -> r instanceof LangStringConstantTagClass).count() > 1;
        boolean splitIntBlankNode = resClasses.stream().filter(r -> r instanceof UserIntBlankNodeClass).count() > 1;
        boolean splitStrBlankNode = resClasses.stream().filter(r -> r instanceof UserStrBlankNodeClass).count() > 1;

        StringBuilder builder = new StringBuilder();

        boolean hasSelect = false;

        for(ResourceClass resourceClass : resClasses)
        {
            boolean check = (resourceClass instanceof DateTimeConstantZoneClass && splitDateTime)
                    || (resourceClass instanceof DateConstantZoneClass && splitDate)
                    || (resourceClass instanceof LangStringConstantTagClass && splitLang)
                    || (resourceClass instanceof UserIntBlankNodeClass && splitIntBlankNode)
                    || (resourceClass instanceof UserStrBlankNodeClass && splitStrBlankNode);

            List<Column> columns = resourceClass.fromExpression(column, isBoxed, check);

            for(int i = 0; i < resourceClass.getColumnCount(); i++)
            {
                appendComma(builder, hasSelect);
                hasSelect = true;

                builder.append(columns.get(i));
                builder.append(" AS ");
                builder.append(variable.getMapping(resourceClass).get(i));
            }
        }

        return builder.toString();
    }
}
