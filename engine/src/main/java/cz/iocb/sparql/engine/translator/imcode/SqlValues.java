package cz.iocb.sparql.engine.translator.imcode;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.Condition;
import cz.iocb.sparql.engine.database.Conditions;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.UsedVariable;
import cz.iocb.sparql.engine.translator.UsedVariables;



public final class SqlValues extends SqlIntercode
{
    private final LinkedHashMap<Column, List<Column>> data;
    private final Map<String, List<ResourceClass>> resourceClasses;
    private final int size;


    protected SqlValues(UsedVariables usedVariables, Map<String, List<ResourceClass>> resourceClasses,
            LinkedHashMap<Column, List<Column>> data, int size)
    {
        super(usedVariables, true);

        this.data = data;
        this.resourceClasses = resourceClasses;
        this.size = size;
    }


    public static SqlIntercode create(UsedVariables usedVariables, Map<String, List<ResourceClass>> resourceClasses,
            LinkedHashMap<Column, List<Column>> data, int size)
    {
        return new SqlValues(usedVariables, resourceClasses, data, size);
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

        return create(optimizedVariables, resourceClasses, optimizedData, size);
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


    @Override
    public void generateExplanation(StringBuilder builder, String indent)
    {
        builder.append("inline data");

        if(!variables.getNames().isEmpty())
            builder.append(variables.getNames().stream().collect(joining(" ", " ", "")));
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(!(object instanceof SqlValues imcode))
            return false;

        if(!super.equals(imcode))
            return false;

        if(!Objects.equals(size, imcode.size))
            return false;

        if(!Objects.equals(data, imcode.data))
            return false;

        return true;
    }


    @Override
    protected int getHashCode()
    {
        return Objects.hash(size, data);
    }


    public SqlIntercode getSlice(int j)
    {
        UsedVariables subvars = new UsedVariables();

        for(UsedVariable var : variables.getValues())
        {
            ResourceClass resClass = resourceClasses.get(var.getName()).get(j);

            if(resClass != null)
            {
                List<Column> cols = var.getMapping(resClass);

                if(cols == null)
                {
                    subvars.add(new UsedVariable(var.getName(), resClass, null, false));
                }
                else
                {
                    List<Column> constCols = cols.stream()
                            .map(c -> c instanceof ConstantColumn ? c : data.get(c).get(j)).toList();
                    subvars.add(new UsedVariable(var.getName(), resClass, constCols, false));
                }
            }
        }

        return SqlTableAccess.create(null, subvars);
    }

    static <T> List<T> filterByMask(List<T> list, boolean[] mask)
    {
        List<T> out = new java.util.ArrayList<>(list.size());

        for(int i = 0; i < list.size(); i++)
            if(mask[i])
                out.add(list.get(i));

        return out;
    }


    public SqlIntercode strip(boolean[] mask)
    {
        int newSize = 0;

        for(int i = 0; i < mask.length; i++)
            if(mask[i])
                newSize++;

        if(newSize == 0)
            return SqlNoSolution.get();

        if(newSize == 1)
        {
            //TODO: return SqlTableAccess
            /*
            for(int i = 0; i < mask.length; i++)
                if(mask[i])
                    return getSlice(i);
            */
        }


        Map<String, List<ResourceClass>> filteredResourceClasses = resourceClasses.entrySet().stream()
                .collect(toMap(Entry::getKey, e -> filterByMask(e.getValue(), mask), (a, _) -> a, HashMap::new));

        LinkedHashMap<Column, List<Column>> filteredData = data.entrySet().stream()
                .collect(toMap(Entry::getKey, e -> filterByMask(e.getValue(), mask), (a, _) -> a, LinkedHashMap::new));

        UsedVariables newVars = new UsedVariables();

        for(UsedVariable var : variables.getValues())
        {
            Set<ResourceClass> classes = new HashSet<ResourceClass>();
            boolean canBeNull = false;

            for(int i = 0; i < size; i++)
            {
                if(mask[i])
                {
                    ResourceClass resClass = resourceClasses.get(var.getName()).get(i);

                    if(resClass != null)
                        classes.add(resClass);
                    else
                        canBeNull = true;
                }
            }

            if(!classes.isEmpty())
            {
                UsedVariable newVar = new UsedVariable(var.getName(), canBeNull);

                for(ResourceClass rc : classes)
                {
                    List<Column> cols = var.getMapping(rc);

                    if(cols != null)
                        cols = cols.stream()
                                .map(c -> c instanceof ConstantColumn ? c :
                                        filteredData.get(c).stream().distinct().limit(2).count() == 1 ?
                                                filteredData.get(c).get(0) : c)
                                .toList();

                    newVar.addMapping(rc, cols);
                }

                newVars.add(newVar);
            }
        }

        Set<Column> nonConstCols = newVars.getNonConstantColumns();

        LinkedHashMap<Column, List<Column>> refilteredData = filteredData.entrySet().stream()
                .filter(e -> nonConstCols.contains(e.getKey()))
                .collect(toMap(Entry::getKey, Entry::getValue, (a, _) -> a, LinkedHashMap::new));

        return create(newVars, filteredResourceClasses, refilteredData, newSize);
    }
}
