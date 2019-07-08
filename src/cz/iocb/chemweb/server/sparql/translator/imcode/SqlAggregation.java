package cz.iocb.chemweb.server.sparql.translator.imcode;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.rdfLangString;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDate;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import cz.iocb.chemweb.server.sparql.engine.Request;
import cz.iocb.chemweb.server.sparql.mapping.classes.DateConstantZoneClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.DateTimeConstantZoneClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.LangStringConstantTagClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;
import cz.iocb.chemweb.server.sparql.translator.UsedVariable;
import cz.iocb.chemweb.server.sparql.translator.UsedVariables;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlExpressionIntercode;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlNull;



public class SqlAggregation extends SqlIntercode
{
    private final HashSet<String> groupVariables;
    private final LinkedHashMap<String, SqlExpressionIntercode> aggregations;
    private final SqlIntercode child;


    protected SqlAggregation(UsedVariables variables, boolean isDeterministic, HashSet<String> groupVariables,
            LinkedHashMap<String, SqlExpressionIntercode> aggregations, SqlIntercode child)
    {
        super(variables, isDeterministic);
        this.groupVariables = groupVariables;
        this.aggregations = aggregations;
        this.child = child;
    }


    public static SqlIntercode aggregate(HashSet<String> groupVariables,
            LinkedHashMap<String, SqlExpressionIntercode> aggregations, SqlIntercode child,
            HashSet<String> restrictions)
    {
        UsedVariables variables = new UsedVariables();

        for(String variable : groupVariables)
            if(child.getVariables().get(variable) != null && (restrictions == null || restrictions.contains(variable)))
                variables.add(child.getVariables().get(variable));

        for(Entry<String, SqlExpressionIntercode> entry : aggregations.entrySet())
            if(entry.getValue() != SqlNull.get() && (restrictions == null || restrictions.contains(entry.getKey())))
                variables.add(new UsedVariable(entry.getKey(), entry.getValue().getResourceClasses(),
                        entry.getValue().canBeNull()));


        boolean isDeterministic = child.isDeterministic();

        for(Entry<String, SqlExpressionIntercode> entry : aggregations.entrySet())
            if(restrictions == null || restrictions.contains(entry.getKey()))
                isDeterministic &= entry.getValue().isDeterministic();

        return new SqlAggregation(variables, isDeterministic, groupVariables, aggregations, child);
    }


    @Override
    public SqlIntercode optimize(Request request, HashSet<String> restrictions, boolean reduced)
    {
        HashSet<String> childRestrictions = new HashSet<String>(groupVariables);

        for(Entry<String, SqlExpressionIntercode> entry : aggregations.entrySet())
            if(restrictions == null || restrictions.contains(entry.getKey()))
                childRestrictions.addAll(entry.getValue().getVariables());

        return aggregate(groupVariables, aggregations, child.optimize(request, childRestrictions, false), restrictions);
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

            for(Entry<String, SqlExpressionIntercode> entry : aggregations.entrySet())
            {
                if(variables.get(entry.getKey()) == null)
                    continue;

                String variable = entry.getKey();
                SqlExpressionIntercode expression = entry.getValue();


                if(!isExpansionNeeded(expression))
                {
                    for(ResourceClass resClass : expression.getResourceClasses())
                    {
                        for(int i = 0; i < resClass.getPatternPartsCount(); i++)
                        {
                            appendComma(builder, hasSelect);
                            hasSelect = true;

                            builder.append(resClass.getSqlColumn(variable, i));
                        }
                    }
                }
                else
                {
                    String columnName = '"' + variable + "#expression\"";


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
                            builder.append(resourceClass.getSqlColumn(variable, 0));
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
                            builder.append(resourceClass.getSqlColumn(variable, 0));
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
                            builder.append(resourceClass.getSqlColumn(variable, 0));
                        }
                        else
                        {
                            for(int i = 0; i < resourceClass.getPatternPartsCount(); i++)
                            {
                                appendComma(builder, hasSelect);
                                hasSelect = true;

                                builder.append(resourceClass.getPatternCode(columnName, i, expression.isBoxed()));
                                builder.append(" AS ");
                                builder.append(resourceClass.getSqlColumn(variable, i));
                            }
                        }
                    }
                }
            }


            for(String variable : groupVariables)
            {
                if(variables.get(variable) == null)
                    continue;

                for(ResourceClass resClass : child.getVariables().get(variable).getClasses())
                {
                    for(int i = 0; i < resClass.getPatternPartsCount(); i++)
                    {
                        appendComma(builder, hasSelect);
                        hasSelect = true;

                        builder.append(resClass.getSqlColumn(variable, i));
                    }
                }
            }

            if(!hasSelect)
                builder.append("1");

            builder.append(" FROM (");
        }


        builder.append("SELECT ");
        boolean hasSelect = false;

        for(Entry<String, SqlExpressionIntercode> entry : aggregations.entrySet())
        {
            if(variables.get(entry.getKey()) == null)
                continue;

            appendComma(builder, hasSelect);
            hasSelect = true;

            String variable = entry.getKey();
            SqlExpressionIntercode expression = entry.getValue();

            String columnName = '"' + variable + "#expression\"";

            if(expression.isBoxed() == false && expression.getResourceClasses().size() == 1)
            {
                ResourceClass resourceClass = expression.getResourceClasses().iterator().next();

                if(!(resourceClass instanceof UserIriClass) && resourceClass.getPatternPartsCount() == 1)
                    columnName = resourceClass.getSqlColumn(variable, 0);
            }

            builder.append(expression.translate());
            builder.append(" AS ");
            builder.append(columnName);
        }

        for(String variable : groupVariables)
        {
            if(variables.get(variable) == null)
                continue;

            for(ResourceClass resClass : child.getVariables().get(variable).getClasses())
            {
                for(int i = 0; i < resClass.getPatternPartsCount(); i++)
                {
                    appendComma(builder, hasSelect);
                    hasSelect = true;

                    builder.append(resClass.getSqlColumn(variable, i));
                }
            }
        }

        builder.append(" FROM (");
        builder.append(child.translate());
        builder.append(" ) AS tab");

        if(groupVariables.stream().anyMatch(v -> child.getVariables().get(v) != null))
        {
            builder.append(" GROUP BY ");
            boolean hasGroupBy = false;

            for(String variable : groupVariables)
            {
                if(child.getVariables().get(variable) == null)
                    continue;

                for(ResourceClass resClass : child.getVariables().get(variable).getClasses())
                {
                    for(int i = 0; i < resClass.getPatternPartsCount(); i++)
                    {
                        appendComma(builder, hasGroupBy);
                        hasGroupBy = true;

                        builder.append(resClass.getSqlColumn(variable, i));
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
