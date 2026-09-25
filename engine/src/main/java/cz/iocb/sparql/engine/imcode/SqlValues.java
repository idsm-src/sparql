package cz.iocb.sparql.engine.imcode;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;
import java.util.ArrayList;
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
import cz.iocb.sparql.engine.database.VirtualTable;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.VariableBinding;
import cz.iocb.sparql.engine.translator.VariableBindings;



/**
 * Inline table of constant solutions (VALUES, or small SERVICE results). Every variable has fixed columns per resource
 * class; {@code data} holds the column values row by row and {@code resourceClasses} the class of each variable in each
 * row (null when unbound).
 */
public final class SqlValues extends SqlIntercode
{
    /**
     * Values of each non-constant column, row by row.
     */
    private final LinkedHashMap<Column, List<Column>> data;

    /**
     * Class of each variable in each row; null when unbound.
     */
    private final Map<Variable, List<ResourceClass>> resourceClasses;

    /**
     * Number of rows.
     */
    private final int size;


    /**
     * Creates the node.
     *
     * @param bindings the variable bindings
     * @param resourceClasses class of each variable in each row
     * @param data the column values row by row
     * @param size the number of rows
     */
    protected SqlValues(VariableBindings bindings, Map<Variable, List<ResourceClass>> resourceClasses,
            LinkedHashMap<Column, List<Column>> data, int size)
    {
        super(bindings, true);

        this.data = data;
        this.resourceClasses = resourceClasses;
        this.size = size;
    }


    /**
     * Values node over the given bindings and data.
     *
     * @param bindings the variable bindings
     * @param resourceClasses class of each variable in each row
     * @param data the column values row by row
     * @param size the number of rows
     * @return values node over the given bindings and data
     */
    public static SqlIntercode create(VariableBindings bindings, Map<Variable, List<ResourceClass>> resourceClasses,
            LinkedHashMap<Column, List<Column>> data, int size)
    {
        return new SqlValues(bindings, resourceClasses, data, size);
    }


    @Override
    public SqlIntercode optimize(Request request, Restrictions restrictions, boolean reduced, boolean evalServices)
    {
        VariableBindings optimizedBindings = bindings.restrict(restrictions);

        if(optimizedBindings.equals(bindings))
            return this;

        LinkedHashMap<Column, List<Column>> optimizedData = new LinkedHashMap<>();

        for(Column column : optimizedBindings.getNonConstantColumns())
            optimizedData.put(column, data.get(column));

        return create(optimizedBindings, resourceClasses, optimizedData, size);
    }


    /**
     * True if no two rows are equal.
     *
     * @return true if no two rows are equal, false otherwise
     */
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


    /**
     * The rows as a disjunction of equality conditions on the given outer bindings, used to merge the values into a
     * table access.
     *
     * @param outerBindings bindings of the access to merge into
     * @return the rows as a disjunction of equality conditions on the given outer bindings, used to merge the values
     *         into a table access
     */
    public Conditions asConditions(VariableBindings outerBindings)
    {
        Conditions conditions = new Conditions(false);

        for(int i = 0; i < size; i++)
        {
            Condition condition = new Condition();

            for(VariableBinding binding : bindings.getValues())
            {
                VariableBinding outerBinding = outerBindings.get(binding.getVariable());

                //TODO: support multiple resource class ...
                assert binding.getMappings().size() == 1;

                for(Entry<ResourceClass, List<Column>> mapping : binding.getMappings().entrySet())
                {
                    List<Column> outerCollumns = outerBinding.getMapping(mapping.getKey());

                    List<Column> values = new ArrayList<>(outerCollumns.size());

                    for(int j = 0; j < outerCollumns.size(); j++)
                    {
                        if(mapping.getValue().get(j) instanceof ConstantColumn)
                            values.add(mapping.getValue().get(j));
                        else
                            values.add(data.get(mapping.getValue().get(j)).get(i));
                    }

                    condition.addAreEqual(outerCollumns, values, mapping.getKey()::isOptionalColumn);
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


    /**
     * Number of rows.
     *
     * @return number of rows
     */
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
    public Set<VirtualTable> getVirtualTables()
    {
        return Set.of();
    }


    @Override
    public void generateExplanation(StringBuilder builder, String indent)
    {
        builder.append("inline data");

        if(!bindings.getVariables().isEmpty())
            builder.append(bindings.getVariables().stream().map(v -> v.toString()).collect(joining(" ", " ", "")));
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


    /**
     * The j-th row as a constant-only table access.
     *
     * @param j the row index
     * @return the j-th row as a constant-only table access
     */
    public SqlIntercode getSlice(int j)
    {
        VariableBindings subBindings = new VariableBindings();

        for(VariableBinding binding : bindings.getValues())
        {
            ResourceClass resClass = resourceClasses.get(binding.getVariable()).get(j);

            if(resClass != null)
            {
                List<Column> cols = binding.getMapping(resClass);

                if(cols == null)
                {
                    subBindings.add(new VariableBinding(binding.getVariable(), resClass, null, false));
                }
                else
                {
                    List<Column> constCols = cols.stream()
                            .map(c -> c instanceof ConstantColumn ? c : data.get(c).get(j)).toList();
                    subBindings.add(new VariableBinding(binding.getVariable(), resClass, constCols, false));
                }
            }
        }

        return SqlTableAccess.create(null, subBindings);
    }


    /**
     * Elements selected by the mask.
     *
     * @param <T> the element type
     * @param list the list to filter
     * @param mask selection mask of the rows
     * @return elements selected by the mask
     */
    static <T> List<T> filterByMask(List<T> list, boolean[] mask)
    {
        List<T> out = new java.util.ArrayList<>(list.size());

        for(int i = 0; i < list.size(); i++)
            if(mask[i])
                out.add(list.get(i));

        return out;
    }


    /**
     * The rows selected by the mask; no solution when none is selected.
     *
     * @param mask selection mask of the rows
     * @return the rows selected by the mask; no solution when none is selected
     */
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


        Map<Variable, List<ResourceClass>> filteredResourceClasses = resourceClasses.entrySet().stream()
                .collect(toMap(Entry::getKey, e -> filterByMask(e.getValue(), mask), (a, _) -> a, HashMap::new));

        LinkedHashMap<Column, List<Column>> filteredData = data.entrySet().stream()
                .collect(toMap(Entry::getKey, e -> filterByMask(e.getValue(), mask), (a, _) -> a, LinkedHashMap::new));

        VariableBindings newBindings = new VariableBindings();

        for(VariableBinding binding : bindings.getValues())
        {
            Set<ResourceClass> classes = new HashSet<>();
            boolean canBeNull = false;

            for(int i = 0; i < size; i++)
            {
                if(mask[i])
                {
                    ResourceClass resClass = resourceClasses.get(binding.getVariable()).get(i);

                    if(resClass != null)
                        classes.add(resClass);
                    else
                        canBeNull = true;
                }
            }

            if(!classes.isEmpty())
            {
                VariableBinding newBinding = new VariableBinding(binding.getVariable(), canBeNull);

                for(ResourceClass rc : classes)
                {
                    List<Column> cols = binding.getMapping(rc);

                    if(cols != null)
                        cols = cols.stream()
                                .map(c -> c instanceof ConstantColumn ? c :
                                        filteredData.get(c).stream().distinct().limit(2).count() == 1 ?
                                                filteredData.get(c).get(0) : c)
                                .toList();

                    newBinding.addMapping(rc, cols);
                }

                newBindings.add(newBinding);
            }
        }

        Set<Column> nonConstCols = newBindings.getNonConstantColumns();

        LinkedHashMap<Column, List<Column>> refilteredData = filteredData.entrySet().stream()
                .filter(e -> nonConstCols.contains(e.getKey()))
                .collect(toMap(Entry::getKey, Entry::getValue, (a, _) -> a, LinkedHashMap::new));

        return create(newBindings, filteredResourceClasses, refilteredData, newSize);
    }
}
