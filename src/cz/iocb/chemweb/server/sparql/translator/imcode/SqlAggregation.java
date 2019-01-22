package cz.iocb.chemweb.server.sparql.translator.imcode;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.rdfLangString;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDate;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import cz.iocb.chemweb.server.sparql.mapping.classes.DateConstantZoneClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.DateTimeConstantZoneClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.LangStringConstantTagClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;
import cz.iocb.chemweb.server.sparql.parser.model.Variable;
import cz.iocb.chemweb.server.sparql.parser.model.VariableOrBlankNode;
import cz.iocb.chemweb.server.sparql.translator.UsedVariable;
import cz.iocb.chemweb.server.sparql.translator.UsedVariables;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlExpressionIntercode;



public class SqlAggregation extends SqlIntercode
{
    private final List<Variable> groupVariables;
    private final LinkedHashMap<Variable, SqlExpressionIntercode> aggregations;
    private final SqlIntercode child;


    protected SqlAggregation(UsedVariables variables, List<Variable> groupVariables,
            LinkedHashMap<Variable, SqlExpressionIntercode> aggregations, SqlIntercode child)
    {
        super(variables);
        this.groupVariables = groupVariables;
        this.aggregations = aggregations;
        this.child = child;
    }


    public static SqlIntercode aggregate(List<Variable> groupVariables,
            LinkedHashMap<Variable, SqlExpressionIntercode> aggregations, SqlIntercode child)
    {
        UsedVariables variables = new UsedVariables();
        groupVariables.stream().map(v -> child.getVariables().get(v.getName())).forEach(v -> variables.add(v));
        aggregations.entrySet().stream().forEach(v -> variables.add(
                new UsedVariable(v.getKey().getName(), v.getValue().getResourceClasses(), v.getValue().canBeNull())));

        return new SqlAggregation(variables, groupVariables, aggregations, child);
    }


    @Override
    public String translate()
    {
        boolean useTwoPhases = false;

        for(SqlExpressionIntercode expression : aggregations.values())
            useTwoPhases |= isExpansionNeeded(expression);


        StringBuilder builder = new StringBuilder();

        if(useTwoPhases)
        {
            builder.append("SELECT ");
            boolean hasSelect = false;

            for(Entry<Variable, SqlExpressionIntercode> entry : aggregations.entrySet())
            {
                Variable variable = entry.getKey();
                SqlExpressionIntercode expression = entry.getValue();


                if(!isExpansionNeeded(expression))
                {
                    for(ResourceClass resClass : expression.getResourceClasses())
                    {
                        for(int i = 0; i < resClass.getPatternPartsCount(); i++)
                        {
                            appendComma(builder, hasSelect);
                            hasSelect = true;

                            builder.append(resClass.getSqlColumn(variable.getName(), i));
                        }
                    }
                }
                else
                {
                    String variableName = variable.getName();
                    String columnName = '"' + variableName + "#expression\"";


                    boolean splitDateTimeClasses = expression.getResourceClasses().stream()
                            .filter(r -> r instanceof DateTimeConstantZoneClass).count() > 1;

                    boolean splitDateClasses = expression.getResourceClasses().stream()
                            .filter(r -> r instanceof DateConstantZoneClass).count() > 1;

                    boolean splitLangClasses = expression.getResourceClasses().stream()
                            .filter(r -> r instanceof LangStringConstantTagClass).count() > 1;


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
                }
            }


            for(VariableOrBlankNode variable : groupVariables)
            {
                for(ResourceClass resClass : child.getVariables().get(variable.getName()).getClasses())
                {
                    for(int i = 0; i < resClass.getPatternPartsCount(); i++)
                    {
                        appendComma(builder, hasSelect);
                        hasSelect = true;

                        builder.append(resClass.getSqlColumn(variable.getName(), i));
                    }
                }
            }


            builder.append(" FROM (");
        }


        builder.append("SELECT ");
        boolean hasSelect = false;

        for(Entry<Variable, SqlExpressionIntercode> entry : aggregations.entrySet())
        {
            appendComma(builder, hasSelect);
            hasSelect = true;

            Variable variable = entry.getKey();
            SqlExpressionIntercode expression = entry.getValue();

            String variableName = variable.getName();
            String columnName = '"' + variableName + "#expression\"";

            if(expression.isBoxed() == false && expression.getResourceClasses().size() == 1)
            {
                ResourceClass resourceClass = expression.getResourceClasses().iterator().next();

                if(!(resourceClass instanceof UserIriClass) && resourceClass.getPatternPartsCount() == 1)
                    columnName = resourceClass.getSqlColumn(variableName, 0);
            }

            builder.append(expression.translate());
            builder.append(" AS ");
            builder.append(columnName);
        }

        for(Variable variable : groupVariables)
        {
            for(ResourceClass resClass : child.getVariables().get(variable.getName()).getClasses())
            {
                for(int i = 0; i < resClass.getPatternPartsCount(); i++)
                {
                    appendComma(builder, hasSelect);
                    hasSelect = true;

                    builder.append(resClass.getSqlColumn(variable.getName(), i));
                }
            }
        }

        builder.append(" FROM (");
        builder.append(child.translate());
        builder.append(" ) AS tab");

        if(!groupVariables.isEmpty())
        {
            builder.append(" GROUP BY ");
            boolean hasGroupBy = false;

            for(Variable variable : groupVariables)
            {
                for(ResourceClass resClass : child.getVariables().get(variable.getName()).getClasses())
                {
                    for(int i = 0; i < resClass.getPatternPartsCount(); i++)
                    {
                        appendComma(builder, hasGroupBy);
                        hasGroupBy = true;

                        builder.append(resClass.getSqlColumn(variable.getName(), i));
                    }
                }
            }
        }

        if(useTwoPhases)
            builder.append(" ) AS tab");


        return builder.toString();
    }


    private static boolean isExpansionNeeded(SqlExpressionIntercode expression)
    {
        if(expression.isBoxed() || expression.getResourceClasses().size() != 1)
            return true;

        ResourceClass resourceClass = expression.getResourceClasses().iterator().next();

        if(resourceClass instanceof UserIriClass || resourceClass.getPatternPartsCount() != 1)
            return true;

        return false;
    }
}
