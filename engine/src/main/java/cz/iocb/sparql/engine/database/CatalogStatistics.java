package cz.iocb.sparql.engine.database;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import cz.iocb.sparql.engine.request.Request;



/**
 * Statistics of a database table as the planner keeps them in the catalog: the row count of {@code pg_class} and, per
 * column, the number of distinct values, the fraction of null values and the most common values with their frequencies
 * of {@code pg_stats}. They are read on first use through the connection of the request and kept for the life of the
 * object, the frequency of a value being looked up when it is first asked for.
 */
public final class CatalogStatistics extends TableStatisticsBase
{
    /**
     * The table.
     */
    private final DatabaseTable table;

    /**
     * Statistics of the whole table, once read.
     */
    private volatile Group group;

    /**
     * Frequencies of the values asked for, by column name and SQL expression of the value.
     */
    private final Map<String, Double> frequencies = new ConcurrentHashMap<>();


    /**
     * Creates the statistics of the table.
     *
     * @param table the table
     */
    public CatalogStatistics(DatabaseTable table)
    {
        this.table = table;
    }


    @Override
    protected Group lookupGroup(Request request, Condition condition)
    {
        Group result = group;

        if(result == null)
        {
            synchronized(this)
            {
                if(group == null)
                    group = readGroup(request);

                result = group;
            }
        }

        return result;
    }


    @Override
    protected double lookupFrequency(Request request, Group group, TableColumn column, Column value)
    {
        return frequencies.computeIfAbsent(column.getName() + "\0" + value, _ -> readFrequency(request, column, value));
    }


    /**
     * Reads the statistics of the table from the catalog; the row count is unknown when the table has not been
     * analysed.
     *
     * @param request the current request
     * @return the statistics of the table
     */
    private Group readGroup(Request request)
    {
        String sql = "SELECT c.reltuples, s.attname, s.n_distinct, s.null_frac, "
                + "coalesce(array_length(s.most_common_vals, 1), 0), "
                + "coalesce((SELECT sum(f) FROM unnest(s.most_common_freqs) AS f), 0), "
                + "coalesce((SELECT sum(f * f) FROM unnest(s.most_common_freqs) AS f), 0) "
                + "FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace "
                + "LEFT JOIN pg_stats s ON s.schemaname = n.nspname AND s.tablename = c.relname " + "WHERE n.nspname = "
                + literal(table.getSchema()) + " AND c.relname = " + literal(table.getName());

        return query(request, sql, result -> {
            double rows = Double.NaN;
            Map<String, ColumnStatistics> columns = new HashMap<>();

            while(result.next())
            {
                rows = result.getDouble(1);

                String column = result.getString(2);

                if(column != null)
                    columns.put(column, ColumnStatistics.of(Math.max(rows, 0), result.getDouble(3), result.getDouble(4),
                            result.getInt(5), result.getDouble(6), result.getDouble(7)));
            }

            if(Double.isNaN(rows))
                return null;

            return new Group(null, rows < 0 ? DEFAULT_ROWS : Math.max(rows, 1), columns);
        });
    }


    /**
     * Reads the frequency of the value in the column from the list of the most common values of the catalog, the value
     * and the values of the list being compared by their text forms (so that a value whose text form the input of the
     * type does not take back cannot fail the lookup); a failing lookup counts as the value not being among the most
     * common ones.
     *
     * @param request the current request
     * @param column the column
     * @param value SQL expression of the value
     * @return frequency of the value, or {@code NaN} if it is not among the most common values
     */
    private double readFrequency(Request request, TableColumn column, Column value)
    {
        String sql = "SELECT s.most_common_freqs[array_position(s.most_common_vals::text::text[], (" + value
                + ")::text)] FROM pg_stats s WHERE s.schemaname = " + literal(table.getSchema()) + " AND s.tablename = "
                + literal(table.getName()) + " AND s.attname = " + literal(column.getName());

        try
        {
            return query(request, sql, result -> {
                if(!result.next())
                    return Double.NaN;

                double frequency = result.getDouble(1);

                return result.wasNull() ? Double.NaN : frequency;
            });
        }
        catch(SQLRuntimeException e)
        {
            return Double.NaN;
        }
    }
}
