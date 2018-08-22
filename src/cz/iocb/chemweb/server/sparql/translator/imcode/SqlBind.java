package cz.iocb.chemweb.server.sparql.translator.imcode;

import java.util.Set;
import java.util.stream.Collectors;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.ExpressionResourceClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.interfaces.PatternResourceClass;
import cz.iocb.chemweb.server.sparql.translator.UsedVariable;
import cz.iocb.chemweb.server.sparql.translator.UsedVariables;
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
        super(variables);
        this.variableName = variableName;
        this.expression = expression;
        this.context = context;
    }


    public static SqlIntercode bind(String variable, SqlExpressionIntercode expression, SqlIntercode context)
    {
        if(context instanceof SqlNoSolution)
            return new SqlNoSolution();

        if(expression == SqlNull.get())
            return context;

        UsedVariables variables = new UsedVariables();

        for(UsedVariable var : context.variables.getValues())
            variables.add(var);

        Set<PatternResourceClass> classes = null;

        if(expression instanceof SqlNodeValue)
            classes = ((SqlNodeValue) expression).getPatternResourceClasses();
        else
            classes = expression.getResourceClasses().stream().map(r -> r.getPatternResourceClass())
                    .collect(Collectors.toSet());

        variables.add(new UsedVariable(variable, classes, expression.canBeNull()));

        return new SqlBind(variables, variable, expression, context);
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

            for(PatternResourceClass resourceClass : node.getPatternResourceClasses())
            {
                for(int i = 0; i < resourceClass.getPartsCount(); i++)
                {
                    appendComma(builder, hasSelect);
                    hasSelect = true;

                    builder.append(node.nodeAccess(resourceClass, i));
                    builder.append(" AS ");
                    builder.append(resourceClass.getSqlColumn(variableName, i));
                }
            }
        }
        else
        {
            String columnName = '"' + variableName + "#expression\"";

            if(!expression.isBoxed())
            {
                ExpressionResourceClass expressionClass = expression.getResourceClasses().iterator().next();
                PatternResourceClass patternClass = expressionClass.getPatternResourceClass();

                if(expressionClass == patternClass)
                {
                    columnName = patternClass.getSqlColumn(variableName, 0);
                    useTwoPhases = false;
                }
            }


            if(useTwoPhases)
            {
                builder.append("SELECT ");

                boolean hasSelect = false;

                for(ExpressionResourceClass expressionClass : expression.getResourceClasses())
                {
                    PatternResourceClass patternClass = expressionClass.getPatternResourceClass();

                    for(int i = 0; i < patternClass.getPartsCount(); i++)
                    {
                        appendComma(builder, hasSelect);
                        hasSelect = true;

                        builder.append(expressionClass.getSqlPatternValue(columnName, i, expression.isBoxed()));
                        builder.append(" AS ");
                        builder.append(patternClass.getSqlColumn(variableName, i));
                    }
                }

                for(UsedVariable variable : context.variables.getValues())
                {
                    for(PatternResourceClass resClass : variable.getClasses())
                    {
                        for(int i = 0; i < resClass.getPartsCount(); i++)
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


        for(UsedVariable variable : context.variables.getValues())
        {
            for(PatternResourceClass resClass : variable.getClasses())
            {
                for(int i = 0; i < resClass.getPartsCount(); i++)
                {
                    appendComma(builder, true);
                    builder.append(resClass.getSqlColumn(variable.getName(), i));
                }
            }
        }

        builder.append(" FROM (");
        builder.append(context.translate());
        builder.append(" ) AS tab");

        if(useTwoPhases)
            builder.append(" ) AS tab");

        return builder.toString();
    }
}
