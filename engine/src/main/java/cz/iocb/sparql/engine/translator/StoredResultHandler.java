package cz.iocb.sparql.engine.translator;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static java.util.Collections.nCopies;
import static java.util.stream.Collectors.joining;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.database.NullColumn;
import cz.iocb.sparql.engine.database.SqlType;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.imcode.SqlIntercode;
import cz.iocb.sparql.engine.imcode.SqlIntercode.Restrictions;
import cz.iocb.sparql.engine.imcode.SqlNoSolution;
import cz.iocb.sparql.engine.imcode.SqlTableAccess;
import cz.iocb.sparql.engine.imcode.SqlValues;
import cz.iocb.sparql.engine.mapping.classes.ClassRelations;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.Request;



/**
 * Result handler that keeps small results as a VALUES list and stores large ones in a temporary table (created and
 * filled in batches while the solutions arrive), which is then accessed as an ordinary table.
 */
public class StoredResultHandler extends ResultHandler
{
    /**
     * Counter making temporary table names unique.
     */
    private static AtomicInteger tableIdx = new AtomicInteger(0);

    /**
     * Rows inserted per batch.
     */
    private static int batchSize = 1000;

    /**
     * Results with fewer rows stay a VALUES list instead of a table; must not exceed the batch size.
     */
    private static int minTableSize = 1000; // has to be less than or equal to batchSize

    /**
     * Pending asynchronous work.
     */
    private final List<Future<Boolean>> futures = new ArrayList<>();

    /**
     * Temporary table, created on the first flush.
     */
    private DatabaseTable table;

    /**
     * Table columns with their SQL types.
     */
    private LinkedHashMap<Column, SqlType> columns = new LinkedHashMap<>();

    /**
     * Columns of each class each received variable is stored in. The terms are classified one by one, so the classes of
     * a variable may overlap until {@link #mergeOverlappingClasses()} merges them, which is why they are not kept as a
     * {@link VariableBinding}.
     */
    private final Map<Variable, Map<ResourceClass, List<Column>>> classColumns = new LinkedHashMap<>();

    /**
     * Number of rows in which each variable is bound.
     */
    private final Map<Variable, Integer> counts = new HashMap<>();

    /**
     * Number of rows in which each variable is stored in each class.
     */
    private final Map<Variable, Map<ResourceClass, Integer>> classCounts = new HashMap<>();

    /**
     * Columns constant over all rows so far, with their value.
     */
    private final Map<Column, Column> constants = new HashMap<>();

    /**
     * Class of each variable in each row of the current batch.
     */
    Map<Variable, List<ResourceClass>> resourceClasses = new HashMap<>();

    /**
     * Term of each variable in each row of the current batch, from which a result kept as a VALUES list stores the
     * terms of overlapping classes in their union (see {@link #mergeOverlappingClasses()}).
     */
    private final Map<Variable, List<RdfTerm>> terms = new HashMap<>();

    /**
     * Column values of the current batch.
     */
    private final LinkedHashMap<Column, List<Column>> data = new LinkedHashMap<>();

    /**
     * Total number of rows received.
     */
    int rowCount;

    /**
     * Number of rows in the current batch.
     */
    int batchCount;


    /**
     * Creates the handler.
     *
     * @param request the current request
     * @param restrictions what the parent needs of the variables
     */
    public StoredResultHandler(Request request, Restrictions restrictions)
    {
        super(request, restrictions);

        columns.put(new TableColumn("__", INT4), INT4);
    }


    @Override
    public void add(Map<Variable, RdfTerm> row) throws SQLException
    {
        ClassRelations relations = request.getConfiguration();
        Map<Variable, ResourceClass> classes = new HashMap<>();

        // a solution binding a variable to a value of a class the parent does not accept never reaches the result, so
        // it is neither stored nor counted
        for(Entry<Variable, RdfTerm> entry : row.entrySet())
        {
            if(!restrictions.containsVar(entry.getKey()))
                continue;

            ResourceClass resClass = getResourceClass(request, entry.getValue(), entry.getKey());
            Set<ResourceClass> accepted = restrictions.getAcceptedClasses(entry.getKey());

            if(resClass != null && accepted != null && ResourceClass.areDisjunct(relations, resClass, accepted))
                return;

            classes.put(entry.getKey(), resClass);
        }

        for(Entry<Variable, RdfTerm> entry : row.entrySet())
        {
            if(!restrictions.containsVar(entry.getKey()))
                continue;

            counts.merge(entry.getKey(), 1, Integer::sum);

            Map<ResourceClass, List<Column>> stored = classColumns.computeIfAbsent(entry.getKey(),
                    _ -> new HashMap<>());

            ResourceClass resClass = classes.get(entry.getKey());

            if(!restrictions.contains(relations, entry.getKey(), resClass))
                continue;

            classCounts.computeIfAbsent(entry.getKey(), _ -> new HashMap<>()).merge(resClass, 1, Integer::sum);

            resourceClasses.computeIfAbsent(entry.getKey(), _ -> new ArrayList<>(nCopies(batchSize, null)))
                    .set(batchCount, resClass);

            terms.computeIfAbsent(entry.getKey(), _ -> new ArrayList<>(nCopies(batchSize, null))).set(batchCount,
                    entry.getValue());

            List<Column> vals = getColumns(request, resClass, entry.getValue());
            List<Column> cols = stored.get(resClass);
            List<SqlType> types = resClass.getSqlTypes();

            if(cols == null)
            {
                cols = resClass.createColumns(request.getColumnMap(), entry.getKey());
                stored.put(resClass, cols);

                for(int i = 0; i < resClass.getColumnCount(); i++)
                    columns.put(cols.get(i), types.get(i));

                if(table != null)
                {
                    for(int i = 0; i < resClass.getColumnCount(); i++)
                    {
                        String sql = "alter table " + table + " add " + cols.get(i) + " " + types.get(i);
                        request.getStatement().execute(sql);
                    }
                }
            }

            for(int i = 0; i < vals.size(); i++)
                data.computeIfAbsent(cols.get(i), _ -> new ArrayList<>(nCopies(batchSize, null))).set(batchCount,
                        vals.get(i));

            if(rowCount == 0)
            {
                for(int i = 0; i < vals.size(); i++)
                    constants.put(cols.get(i), vals.get(i));
            }
            else
            {
                for(int i = 0; i < vals.size(); i++)
                    if(!vals.get(i).equals(constants.get(cols.get(i))))
                        constants.remove(cols.get(i));
            }
        }

        rowCount++;

        if(++batchCount == batchSize)
            flushValues();
    }


    @Override
    public SqlIntercode get() throws SQLException
    {
        if(rowCount == 0)
            return SqlNoSolution.get();

        // a result stored in the table is stored completely before its columns are merged
        if(rowCount >= minTableSize && batchCount > 0)
            flushValues();

        mergeOverlappingClasses();


        VariableBindings varBindings = new VariableBindings();

        for(Entry<Variable, Map<ResourceClass, List<Column>>> entry : classColumns.entrySet())
        {
            Variable variable = entry.getKey();
            VariableBinding v = new VariableBinding(variable, counts.getOrDefault(variable, 0) < rowCount);

            for(Entry<ResourceClass, List<Column>> e : entry.getValue().entrySet())
            {
                // a column is constant only when the class is stored in every row: a value shared by the rows of the
                // class alone would hide in which rows the variable takes the class
                boolean complete = classCounts.getOrDefault(variable, Map.of()).getOrDefault(e.getKey(), 0) == rowCount;

                v.addMapping(e.getKey(),
                        e.getValue().stream().map(c -> complete ? constants.getOrDefault(c, c) : c).toList());
            }

            varBindings.add(v);
        }


        if(rowCount < minTableSize)
        {
            Map<Column, SqlType> sqlTypes = new HashMap<>();

            for(VariableBinding v : varBindings.getValues())
                for(Entry<ResourceClass, List<Column>> e : v.getMappings().entrySet())
                    for(int i = 0; i < e.getKey().getColumnCount(); i++)
                        sqlTypes.put(e.getValue().get(i), e.getKey().getSqlTypes().get(i));


            Map<Variable, List<ResourceClass>> types = new HashMap<>();

            for(Entry<Variable, List<ResourceClass>> entry : resourceClasses.entrySet())
                types.put(entry.getKey(), entry.getValue().subList(0, rowCount));


            Set<Column> columns = varBindings.getNonConstantColumns();

            LinkedHashMap<Column, List<Column>> values = new LinkedHashMap<>();

            for(Entry<Column, List<Column>> entry : data.entrySet())
            {
                SqlType type = sqlTypes.get(entry.getKey());

                if(columns.contains(entry.getKey()))
                    values.put(entry.getKey(), entry.getValue().stream().limit(rowCount)
                            .map(c -> c != null ? c : new NullColumn(type)).toList());
            }


            return SqlValues.create(varBindings, types, values, rowCount).optimize(request, restrictions, false, false);
        }


        if(batchCount > 0)
            flushValues();

        try
        {
            for(Future<Boolean> f : futures)
                f.get();
        }
        catch(InterruptedException e)
        {
            throw new RuntimeException(e);
        }
        catch(ExecutionException e)
        {
            e.fillInStackTrace();

            if(e.getCause() instanceof SQLException ex)
                throw ex;

            throw new RuntimeException(e);
        }


        return SqlTableAccess.create(request, table, varBindings).optimize(request, restrictions, false, false);
    }


    @Override
    public int size()
    {
        return rowCount;
    }


    @Override
    public void close()
    {
    }


    /**
     * Merges the overlapping classes of each variable into their union (see
     * {@link SqlIntercode#mergeOverlappingClasses}), as the received terms are classified one by one, while the classes
     * of a variable have to be disjoint: a result kept as a VALUES list stores the terms of the merged classes in the
     * union, a result stored in the table gets the columns of the union filled with their values converted to it.
     *
     * @throws SQLException on database errors
     */
    private void mergeOverlappingClasses() throws SQLException
    {
        ClassRelations relations = request.getConfiguration();

        for(Entry<Variable, Map<ResourceClass, List<Column>>> entry : classColumns.entrySet())
        {
            Variable variable = entry.getKey();
            Map<ResourceClass, List<Column>> stored = entry.getValue();
            Map<ResourceClass, ResourceClass> merged = SqlIntercode.mergeOverlappingClasses(relations, stored.keySet());

            if(merged.entrySet().stream().allMatch(e -> e.getKey().equals(e.getValue())))
                continue;

            Map<ResourceClass, List<Column>> result = new HashMap<>();
            Map<ResourceClass, Integer> rowsPerClass = classCounts.get(variable);

            for(ResourceClass union : new LinkedHashSet<>(merged.values()))
            {
                List<ResourceClass> group = merged.keySet().stream().filter(c -> merged.get(c).equals(union)).toList();

                if(group.equals(List.of(union)))
                {
                    result.put(union, stored.get(union));
                    continue;
                }

                List<Column> cols = union.createColumns(request.getColumnMap(), variable);
                result.put(union, cols);

                rowsPerClass.put(union, group.stream().mapToInt(c -> rowsPerClass.remove(c)).sum());

                for(ResourceClass resClass : group)
                    for(Column column : stored.get(resClass))
                        constants.remove(column);

                if(table == null)
                {
                    // all the rows are in the current batch
                    List<ResourceClass> rowClasses = resourceClasses.get(variable);
                    List<RdfTerm> rowTerms = terms.get(variable);
                    List<List<Column>> values = new ArrayList<>();

                    for(int i = 0; i < union.getColumnCount(); i++)
                        values.add(new ArrayList<>(nCopies(batchSize, null)));

                    for(int k = 0; k < batchCount; k++)
                    {
                        if(!group.contains(rowClasses.get(k)))
                            continue;

                        List<Column> vals = getColumns(request, union, rowTerms.get(k));

                        for(int i = 0; i < union.getColumnCount(); i++)
                            values.get(i).set(k, vals.get(i));

                        rowClasses.set(k, union);
                    }

                    for(ResourceClass resClass : group)
                        stored.get(resClass).forEach(data::remove);

                    for(int i = 0; i < union.getColumnCount(); i++)
                    {
                        List<Column> column = values.get(i).subList(0, batchCount);
                        data.put(cols.get(i), values.get(i));

                        // the column is constant when every row stores the same value in it
                        if(column.stream().allMatch(v -> v != null && v.equals(column.getFirst())))
                            constants.put(cols.get(i), column.getFirst());
                    }
                }
                else
                {
                    // the columns of a merged class that is the union itself are filled in place
                    if(!group.contains(union))
                        for(int i = 0; i < union.getColumnCount(); i++)
                            request.getStatement().execute(
                                    "alter table " + table + " add " + cols.get(i) + " " + union.getSqlTypes().get(i));

                    List<List<Column>> variants = group.stream()
                            .map(c -> c.equals(union) ? stored.get(c) : c.toGeneralClass(union, stored.get(c), true))
                            .toList();

                    request.getStatement().execute("update " + table + " set " + IntStream
                            .range(0, union.getColumnCount())
                            .mapToObj(i -> cols.get(i) + " = COALESCE("
                                    + variants.stream().map(v -> v.get(i).toString()).collect(joining(", ")) + ")")
                            .collect(joining(", ")));
                }
            }

            entry.setValue(result);
        }
    }


    /**
     * SQL text of a value column, {@code NULL} for a missing one.
     *
     * @param column the value column, or null
     * @return SQL text of a value column, {@code NULL} for a missing one
     */
    private final static String getColumnAsString(Column column)
    {
        if(column == null)
            return "NULL";

        return column.toString();
    }


    /**
     * Writes the current batch into the temporary table, creating the table first if needed.
     *
     * @throws SQLException on database errors
     */
    private void flushValues() throws SQLException
    {
        if(table == null)
        {
            table = new DatabaseTable("pg_temp", "tmp_table_" + tableIdx.getAndIncrement());

            String sql = "create temporary table " + table + columns.entrySet().stream()
                    .map(e -> e.getKey() + " " + e.getValue()).collect(joining(", ", "(", ")"));

            request.getStatement().execute(sql);
        }

        if(data.isEmpty())
            data.put(new TableColumn("__", INT4), new ArrayList<>(Collections.nCopies(batchCount, null)));

        String insert = "insert into " + table
                + data.keySet().stream().map(c -> c.toString()).collect(joining(", ", "(", ") values "))
                + IntStream.range(0, batchCount).mapToObj(i -> data.values().stream()
                        .map(v -> getColumnAsString(v.get(i))).collect(joining(", ", "(", ")"))).collect(joining(", "));

        request.getStatement().execute(insert);

        data.clear();
        resourceClasses.clear();
        terms.clear();
        batchCount = 0;
    }
}
