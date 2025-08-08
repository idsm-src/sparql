package cz.iocb.sparql.engine.translator.imcode;

import static java.util.stream.Collectors.joining;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.Condition;
import cz.iocb.sparql.engine.database.Conditions;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.UsedVariable;
import cz.iocb.sparql.engine.translator.UsedVariables;



public class SqlValues extends SqlIntercode
{
    private final LinkedHashMap<Column, List<Column>> data;
    private final int size;


    protected SqlValues(UsedVariables usedVariables, LinkedHashMap<Column, List<Column>> data, int size)
    {
        super(usedVariables, true);

        this.data = data;
        this.size = size;
    }


    public static SqlIntercode create(UsedVariables usedVariables, LinkedHashMap<Column, List<Column>> data, int size)
    {
        return new SqlValues(usedVariables, data, size);
    }


    @Override
    public SqlIntercode optimize(Request request, Restrictions restrictions, boolean reduced, boolean evalServices)
    {
        UsedVariables optimizedVariables = variables.restrict(restrictions);

        if(optimizedVariables.equals(variables))
            return this;

        LinkedHashMap<Column, List<Column>> optimizedData = new LinkedHashMap<Column, List<Column>>();

        for(Column column : optimizedVariables.getNonConstantColumns())
            optimizedData.put(column, data.get(column));

        return create(optimizedVariables, optimizedData, size);
    }


    public boolean isDistinct()
    {
        for(int i = 0; i < size; i++)
        {
            check:
            for(int j = i + 1; j < size; j++)
            {
                for(List<Column> values : data.values())
                {
                    Column coli = values.get(i);
                    Column colj = values.get(j);

                    if(coli == null ? colj != null : !coli.equals(colj))
                        continue check;
                }

                return false;
            }
        }

        return true;
    }


    public Conditions asConditions(UsedVariables outerVariables)
    {
        Conditions conditions = new Conditions(false);

        for(int i = 0; i < size; i++)
        {
            Condition condition = new Condition();

            for(UsedVariable variable : variables.getValues())
            {
                UsedVariable outerVariable = outerVariables.get(variable.getName());

                //TODO: support multiple resource class ...
                assert variable.getMappings().size() == 1;

                for(Entry<ResourceClass, List<Column>> mapping : variable.getMappings().entrySet())
                {
                    List<Column> outerCollumns = outerVariable.getMapping(mapping.getKey());

                    for(int j = 0; j < outerCollumns.size(); j++)
                    {
                        if(mapping.getValue().get(j) instanceof ConstantColumn)
                            condition.addAreEqual(outerCollumns.get(j), mapping.getValue().get(j));
                        else
                            condition.addAreEqual(outerCollumns.get(j), data.get(mapping.getValue().get(j)).get(i));
                    }
                }
            }

            conditions.add(condition);
        }

        return conditions;
    }


    @Override
    public String translate(Request request)
    {
        StringBuilder builder = new StringBuilder();

        builder.append("SELECT * FROM (VALUES ");

        for(int i = 0; i < size; i++)
        {
            appendComma(builder, i > 0);
            builder.append("(");

            boolean hasValue = false;

            for(List<Column> column : data.values())
            {
                appendComma(builder, hasValue);
                hasValue = true;

                builder.append(column.get(i));
            }

            if(!hasValue)
                builder.append("1");

            builder.append(")");
        }

        builder.append(") AS tab (");

        if(data.size() > 0)
            builder.append(data.keySet().stream().map(Object::toString).collect(joining(",")));
        else
            builder.append("\"#null\"");

        builder.append(")");

        return builder.toString();
    }


    public int getSize()
    {
        return size;
    }


    @Override
    public boolean hasServiceSubpattern()
    {
        return false;
    }
}
