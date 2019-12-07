package cz.iocb.chemweb.server.sparql.translator.imcode;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.rdfLangString;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDate;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDateTime;
import java.util.HashSet;
import cz.iocb.chemweb.server.sparql.engine.Request;
import cz.iocb.chemweb.server.sparql.mapping.classes.DateConstantZoneClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.DateTimeConstantZoneClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.LangStringConstantTagClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIntBlankNodeClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserStrBlankNodeClass;
import cz.iocb.chemweb.server.sparql.translator.UsedVariable;
import cz.iocb.chemweb.server.sparql.translator.UsedVariables;
import cz.iocb.chemweb.server.sparql.translator.expression.SimpleVariableAccessor;
import cz.iocb.chemweb.server.sparql.translator.expression.VariableAccessor;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlExpressionIntercode;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlNodeValue;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlNull;



public class SqlBind extends SqlIntercode
{
    String variableName;
    SqlExpressionIntercode expression;
    SqlIntercode context;


    protected SqlBind(UsedVariables variables, String variableName, SqlExpressionIntercode expression,
            SqlIntercode context)
    {
        super(variables, context.isDeterministic() && expression.isDeterministic());
        this.variableName = variableName;
        this.expression = expression;
        this.context = context;
    }


    public static SqlIntercode bind(String variable, SqlExpressionIntercode expression, SqlIntercode context)
    {
        if(context == SqlNoSolution.get())
            return SqlNoSolution.get();

        if(expression == SqlNull.get())
            return context;

        if(context instanceof SqlUnion)
        {
            SqlIntercode union = SqlNoSolution.get();

            for(SqlIntercode child : ((SqlUnion) context).getChilds())
            {
                VariableAccessor variableAccessor = new SimpleVariableAccessor(child.getVariables());
                union = SqlUnion.union(union, bind(variable, expression.optimize(variableAccessor), child));
            }

            return union;
        }


        UsedVariables variables = new UsedVariables();
        variables.add(new UsedVariable(variable, expression.getResourceClasses(), expression.canBeNull()));

        for(UsedVariable var : context.getVariables().getValues())
            variables.add(var);

        return new SqlBind(variables, variable, expression, context);
    }


    @Override
    public SqlIntercode optimize(Request request, HashSet<String> restrictions, boolean reduced)
    {
        if(!restrictions.contains(variableName))
            return context.optimize(request, restrictions, reduced);

        HashSet<String> contextRestrictions = new HashSet<String>(restrictions);
        contextRestrictions.addAll(expression.getVariables());

        return bind(variableName, expression,
                context.optimize(request, contextRestrictions, reduced && expression.isDeterministic()));
    }


    @Override
    public String translate()
    {
        boolean useTwoPhases = true;
        StringBuilder builder = new StringBuilder();

        if(expression instanceof SqlNodeValue)
        {
            useTwoPhases = false;
            builder.append("SELECT ");

            SqlNodeValue node = (SqlNodeValue) expression;
            boolean hasSelect = false;

            for(ResourceClass resourceClass : node.getResourceClasses())
            {
                for(int i = 0; i < resourceClass.getPatternPartsCount(); i++)
                {
                    appendComma(builder, hasSelect);
                    hasSelect = true;

                    builder.append(node.getNodeAccess(resourceClass, i));
                    builder.append(" AS ");
                    builder.append(resourceClass.getSqlColumn(variableName, i));
                }
            }
        }
        else
        {
            String columnName = '"' + variableName + "#expression\"";

            if(expression.isBoxed() == false && expression.getResourceClasses().size() == 1)
            {
                ResourceClass resourceClass = expression.getResourceClasses().iterator().next();

                if(!(resourceClass instanceof UserIriClass) && resourceClass.getPatternPartsCount() == 1)
                {
                    columnName = resourceClass.getSqlColumn(variableName, 0);
                    useTwoPhases = false;
                }
            }


            if(useTwoPhases)
            {
                boolean splitDateTimeClasses = expression.getResourceClasses().stream()
                        .filter(r -> r instanceof DateTimeConstantZoneClass).count() > 1;

                boolean splitDateClasses = expression.getResourceClasses().stream()
                        .filter(r -> r instanceof DateConstantZoneClass).count() > 1;

                boolean splitLangClasses = expression.getResourceClasses().stream()
                        .filter(r -> r instanceof LangStringConstantTagClass).count() > 1;

                boolean splitIntBlankNodeClasses = expression.getResourceClasses().stream()
                        .filter(r -> r instanceof UserIntBlankNodeClass).count() > 1;

                boolean splitStrBlankNodeClasses = expression.getResourceClasses().stream()
                        .filter(r -> r instanceof UserStrBlankNodeClass).count() > 1;


                builder.append("SELECT ");

                boolean hasSelect = false;

                for(ResourceClass resourceClass : expression.getResourceClasses())
                {
                    if(resourceClass instanceof DateTimeConstantZoneClass && splitDateTimeClasses)
                    {
                        appendComma(builder, hasSelect);
                        hasSelect = true;

                        builder.append("CASE ");
                        builder.append(xsdDateTime.getPatternCode(columnName, 1, expression.isBoxed()));
                        builder.append(" WHEN '");
                        builder.append(((DateTimeConstantZoneClass) resourceClass).getZone());
                        builder.append("'::int4 THEN ");
                        builder.append(resourceClass.getPatternCode(columnName, 0, expression.isBoxed()));
                        builder.append(" END AS ");
                        builder.append(resourceClass.getSqlColumn(variableName, 0));
                    }
                    else if(resourceClass instanceof DateConstantZoneClass && splitDateClasses)
                    {
                        appendComma(builder, hasSelect);
                        hasSelect = true;

                        builder.append("CASE ");
                        builder.append(xsdDate.getPatternCode(columnName, 1, expression.isBoxed()));
                        builder.append(" WHEN '");
                        builder.append(((DateConstantZoneClass) resourceClass).getZone());
                        builder.append("'::int4 THEN ");
                        builder.append(resourceClass.getPatternCode(columnName, 0, expression.isBoxed()));
                        builder.append(" END AS ");
                        builder.append(resourceClass.getSqlColumn(variableName, 0));
                    }
                    else if(resourceClass instanceof LangStringConstantTagClass && splitLangClasses)
                    {
                        appendComma(builder, hasSelect);
                        hasSelect = true;

                        builder.append("CASE ");
                        builder.append(rdfLangString.getPatternCode(columnName, 1, expression.isBoxed()));
                        builder.append(" WHEN '");
                        builder.append(((LangStringConstantTagClass) resourceClass).getTag());
                        builder.append("'::varchar THEN ");
                        builder.append(resourceClass.getPatternCode(columnName, 0, expression.isBoxed()));
                        builder.append(" END AS ");
                        builder.append(resourceClass.getSqlColumn(variableName, 0));
                    }
                    else if(resourceClass instanceof UserIntBlankNodeClass && splitIntBlankNodeClasses)
                    {
                        appendComma(builder, hasSelect);
                        hasSelect = true;

                        builder.append("CASE sparql.");

                        if(expression.isBoxed())
                            builder.append("rdfbox_extract_");

                        builder.append("int_blanknode_segment(");
                        builder.append(columnName);
                        builder.append(") WHEN '");
                        builder.append(((UserIntBlankNodeClass) resourceClass).getSegment());
                        builder.append("'::int4 THEN ");
                        builder.append(resourceClass.getPatternCode(columnName, 0, expression.isBoxed()));
                        builder.append(" END AS ");
                        builder.append(resourceClass.getSqlColumn(variableName, 0));
                    }
                    else if(resourceClass instanceof UserStrBlankNodeClass && splitStrBlankNodeClasses)
                    {
                        appendComma(builder, hasSelect);
                        hasSelect = true;

                        builder.append("CASE sparql.");

                        if(expression.isBoxed())
                            builder.append("rdfbox_extract_");

                        builder.append("str_blanknode_segment(");
                        builder.append(columnName);
                        builder.append(") WHEN '");
                        builder.append(((UserIntBlankNodeClass) resourceClass).getSegment());
                        builder.append("'::int4 THEN ");
                        builder.append(resourceClass.getPatternCode(columnName, 0, expression.isBoxed()));
                        builder.append(" END AS ");
                        builder.append(resourceClass.getSqlColumn(variableName, 0));
                    }
                    else
                    {
                        for(int i = 0; i < resourceClass.getPatternPartsCount(); i++)
                        {
                            appendComma(builder, hasSelect);
                            hasSelect = true;

                            builder.append(resourceClass.getPatternCode(columnName, i, expression.isBoxed()));
                            builder.append(" AS ");
                            builder.append(resourceClass.getSqlColumn(variableName, i));
                        }
                    }
                }

                for(UsedVariable variable : variables.getValues())
                {
                    if(variable.getName().equals(variableName))
                        continue;

                    for(ResourceClass resClass : variable.getClasses())
                    {
                        for(int i = 0; i < resClass.getPatternPartsCount(); i++)
                        {
                            appendComma(builder, true);
                            builder.append(resClass.getSqlColumn(variable.getName(), i));
                        }
                    }
                }

                builder.append(" FROM (");
            }

            builder.append("SELECT ");
            builder.append(expression.translate());
            builder.append(" AS ");
            builder.append(columnName);
        }


        for(UsedVariable variable : variables.getValues())
        {
            if(variable.getName().equals(variableName))
                continue;

            for(ResourceClass resClass : variable.getClasses())
            {
                for(int i = 0; i < resClass.getPatternPartsCount(); i++)
                {
                    appendComma(builder, true);
                    builder.append(resClass.getSqlColumn(variable.getName(), i));
                }
            }
        }

        if(context != SqlEmptySolution.get())
        {
            builder.append(" FROM (");
            builder.append(context.translate());
            builder.append(" ) AS tab");
        }

        if(useTwoPhases)
            builder.append(" ) AS tab");

        return builder.toString();
    }
}
