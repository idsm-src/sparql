package cz.iocb.sparql.engine.translator;

import static java.util.Collections.nCopies;
import static java.util.stream.Collectors.joining;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.database.SQLRuntimeException;
import cz.iocb.sparql.engine.database.Table;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.imcode.SqlIntercode;
import cz.iocb.sparql.engine.imcode.SqlIntercode.Restrictions;
import cz.iocb.sparql.engine.imcode.SqlNoSolution;
import cz.iocb.sparql.engine.imcode.SqlTableAccess;
import cz.iocb.sparql.engine.imcode.SqlValues;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.Request;



public class StoredResultHandler extends ResultHandler
{
    private static AtomicInteger tableIdx = new AtomicInteger(0);
    private static int batchSize = 1000;
    private static int minTableSize = 1000; // has to be less than or equal to batchSize

    private final List<Future<Boolean>> futures = new ArrayList<>();

    private Table table;
    private LinkedHashMap<Column, String> columns = new LinkedHashMap<>();

    private final VariableBindings bindings = new VariableBindings();
    private final Map<Variable, Integer> counts = new HashMap<>();
    private final Map<Column, Column> constants = new HashMap<>();

    Map<Variable, List<ResourceClass>> resourceClasses = new HashMap<>();
    private final LinkedHashMap<Column, List<Column>> data = new LinkedHashMap<>();
    int rowCount;
    int batchCount;


    public StoredResultHandler(Request request, Restrictions restrictions)
    {
        super(request, restrictions);

        columns.put(new TableColumn("__"), "int4");
    }


    @Override
    public void add(Map<Variable, RdfTerm> row) throws SQLException
    {
        for(Entry<Variable, RdfTerm> entry : row.entrySet())
        {
            if(!restrictions.containsVar(entry.getKey()))
                continue;

            counts.merge(entry.getKey(), 1, Integer::sum);

            VariableBinding binding = bindings.get(entry.getKey());

            if(binding == null)
            {
                binding = new VariableBinding(entry.getKey(), true);
                bindings.add(binding);
            }

            ResourceClass resClass = getResourceClass(request, entry.getValue(), entry.getKey());

            if(!restrictions.contains(entry.getKey(), resClass))
                continue;

            resourceClasses.computeIfAbsent(entry.getKey(), _ -> new ArrayList<>(nCopies(batchSize, null)))
                    .set(batchCount, resClass);

            List<Column> vals = getColumns(request, resClass, entry.getValue());
            List<Column> cols = binding.getMapping(resClass);
            List<String> types = resClass.getSqlTypes();

            if(cols == null)
            {
                cols = resClass.createColumns(request.getColumnMap(), entry.getKey());
                binding.addMapping(resClass, cols);

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


        VariableBindings varBindings = new VariableBindings();

        for(VariableBinding binding : bindings.getValues())
        {
            VariableBinding v = new VariableBinding(binding.getVariable(),
                    counts.getOrDefault(binding.getVariable(), 0) < rowCount);

            for(Entry<ResourceClass, List<Column>> e : binding.getMappings().entrySet())
                v.addMapping(e.getKey(), e.getValue().stream().map(c -> constants.getOrDefault(c, c)).toList());

            varBindings.add(v);
        }


        if(rowCount < minTableSize)
        {
            Map<Column, String> sqlTypes = new HashMap<>();

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
                String type = sqlTypes.get(entry.getKey());

                if(columns.contains(entry.getKey()))
                    values.put(entry.getKey(), entry.getValue().stream().limit(rowCount)
                            .map(c -> c != null ? c : new ConstantColumn(null, type)).toList());
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
                throw new SQLRuntimeException(ex);

            throw new RuntimeException(e);
        }


        return SqlTableAccess.create(table, varBindings).optimize(request, restrictions, false, false);
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


    private final static String getColumnAsString(Column column)
    {
        if(column == null)
            return "NULL";

        return column.toString();
    }


    private void flushValues() throws SQLException
    {
        if(table == null)
        {
            table = new Table(null, "tmp_table_" + tableIdx.getAndIncrement());

            String sql = "create temporary table " + table + columns.entrySet().stream()
                    .map(e -> e.getKey() + " " + e.getValue()).collect(joining(", ", "(", ")"));

            request.getStatement().execute(sql);
        }

        if(data.isEmpty())
            data.put(new TableColumn("__"), new ArrayList<>(Collections.nCopies(batchCount, null)));

        String insert = "insert into " + table
                + data.keySet().stream().map(c -> c.toString()).collect(joining(", ", "(", ") values "))
                + IntStream.range(0, batchCount).mapToObj(i -> data.values().stream()
                        .map(v -> getColumnAsString(v.get(i))).collect(joining(", ", "(", ")"))).collect(joining(", "));

        request.getStatement().execute(insert);

        data.clear();
        resourceClasses.clear();
        batchCount = 0;
    }
}
