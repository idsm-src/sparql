package cz.iocb.sparql.engine.database;

import static java.util.stream.Collectors.toSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.request.Request;



/**
 * Statistics of a database table as the planner keeps them in the catalog: the row count of {@code pg_class} and, per
 * column, the number of distinct values, the fraction of null values and the most common values with their frequencies
 * of {@code pg_stats}, and the numbers of the distinct combinations of the values of several columns that the extended
 * statistics of the table keep ({@code CREATE STATISTICS} of the kind {@code ndistinct}), read from
 * {@code pg_stats_ext} or through the function the configuration names (see
 * {@link SparqlDatabaseConfiguration#setExtendedStatisticsFunction}). They are read on first use through the connection
 * of the request and kept for the life of the object, the frequency of a value being looked up when it is first asked
 * for and kept with those of other values in a cache of a bounded size (see {@link TableStatisticsBase#cached}). The
 * configuration replaces the object by a new one once the table changes or a lookup fails (see {@link #getSources} and
 * {@link #isComplete}).
 */
public final class CatalogStatistics extends TableStatisticsBase
{
    /**
     * Logger of the lookups of the statistics.
     */
    private static final Logger logger = LoggerFactory.getLogger(CatalogStatistics.class);

    /**
     * Reader of the JSON forms of the extended statistics.
     */
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * The table.
     */
    private final DatabaseTable table;

    /**
     * Statistics of the whole table, once read.
     */
    private volatile Group group;

    /**
     * Frequencies of the values asked for, by column name and SQL expression of the value; a cache of a bounded size
     * (see {@link TableStatisticsBase#cached}).
     */
    private final Map<String, Double> frequencies = new ConcurrentHashMap<>();

    /**
     * Numbers of the distinct combinations of the values of several columns that the extended statistics of the table
     * keep, by the set of the names of the columns, once read.
     */
    private volatile Map<Set<String>, Double> combinations;

    /**
     * Whether no lookup of the statistics has failed (see {@link #isComplete}).
     */
    private volatile boolean complete = true;


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
        return cached(request, frequencies, column.getName() + "\0" + value,
                _ -> readFrequency(request, column, value));
    }


    @Override
    protected double lookupDistinct(Request request, Group group, Set<TableColumn> columns)
    {
        Map<Set<String>, Double> result = combinations;

        if(result == null)
        {
            synchronized(this)
            {
                if(combinations == null)
                    combinations = readCombinations(request);

                result = combinations;
            }
        }

        return result.getOrDefault(columns.stream().map(TableColumn::getName).collect(toSet()), Double.NaN);
    }


    @Override
    public Set<DatabaseTable> getSources()
    {
        return Set.of(table);
    }


    @Override
    public CatalogStatistics renew()
    {
        return new CatalogStatistics(table);
    }


    @Override
    public boolean isComplete()
    {
        return complete;
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

            return new Group(null, rows < 0 ? request.getEngineSettings().defaultRows() : Math.max(rows, 1), columns);
        });
    }


    /**
     * Reads the frequency of the value in the column from the list of the most common values of the catalog, the value
     * and the values of the list being compared by the text forms the output of their type gives them (so that a value
     * whose text form the input of the type does not take back cannot fail the lookup). The catalog gives the list as
     * an array, whose text holds such forms, and the value is written the same way, as the element of an array, because
     * a cast to text gives another form for some types: a boolean is cast to {@code true} and written as {@code t}. A
     * failing lookup counts as the value not being among the most common ones until the statistics are renewed (see
     * {@link #isComplete}), but for the lookup of a request that is out of time or cancelled, which is no answer at
     * all.
     *
     * @param request the current request
     * @param column the column
     * @param value SQL expression of the value
     * @return frequency of the value, or {@code NaN} if it is not among the most common values
     * @throws SQLRuntimeException if the request is out of time (SQL state 57014) or cancelled
     */
    private double readFrequency(Request request, TableColumn column, Column value)
    {
        String sql = "SELECT s.most_common_freqs[array_position(s.most_common_vals::text::text[], ((ARRAY[" + value
                + "])::text::text[])[1])] FROM pg_stats s WHERE s.schemaname = " + literal(table.getSchema())
                + " AND s.tablename = " + literal(table.getName()) + " AND s.attname = " + literal(column.getName());

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
            // a request out of time or cancelled has not looked the value up, and the frequency, which the statistics
            // keep for the following requests, must not be taken for the value not being among the most common ones
            if(isUnanswered(request, e))
                throw e;

            complete = false;

            return Double.NaN;
        }
    }


    /**
     * Reads the numbers of the distinct combinations of the values of several columns that the extended statistics of
     * the table keep, with the names of the columns of the table by their numbers, by which the statistics name them:
     * from the view {@code pg_stats_ext}, which shows them to the owner of the table only, or through the function the
     * configuration names for a role that does not own it (see
     * {@link SparqlDatabaseConfiguration#setExtendedStatisticsFunction}). A failing lookup counts as no statistics
     * until the statistics are renewed (see {@link #isComplete}), but for the lookup of a request that is out of time
     * or cancelled, which is no answer at all.
     *
     * @param request the current request
     * @return numbers of the distinct combinations of the values of several columns by the set of the names of the
     *         columns
     * @throws SQLRuntimeException if the request is out of time (SQL state 57014) or cancelled
     */
    private Map<Set<String>, Double> readCombinations(Request request)
    {
        Function function = request.getConfiguration().getExtendedStatisticsFunction();

        String source = function != null ? "SELECT * FROM " + function + "(n.nspname, c.relname)" :
                "SELECT e.n_distinct::text FROM pg_stats_ext e WHERE e.schemaname = n.nspname "
                        + "AND e.tablename = c.relname";

        String sql = "SELECT d.n_distinct, (SELECT json_object_agg(a.attnum, a.attname) FROM pg_attribute a "
                + "WHERE a.attrelid = c.oid AND a.attnum > 0 AND NOT a.attisdropped)::text "
                + "FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace CROSS JOIN LATERAL (" + source
                + ") AS d(n_distinct) WHERE n.nspname = " + literal(table.getSchema()) + " AND c.relname = "
                + literal(table.getName());

        try
        {
            return query(request, sql, result -> {
                Map<Set<String>, Double> combinations = new HashMap<>();

                while(result.next())
                    addCombinations(combinations, result.getString(1), result.getString(2));

                return combinations;
            });
        }
        catch(SQLRuntimeException e)
        {
            // as for a frequency, the statistics keep what they have read for the following requests
            if(isUnanswered(request, e))
                throw e;

            complete = false;
            logger.warn("cannot read the extended statistics of {}", table, e);

            return Map.of();
        }
    }


    /**
     * Adds the numbers of the distinct combinations that the text form of an {@code n_distinct} of extended statistics
     * gives, in either of its forms: a JSON object whose keys list the numbers of the columns of a combination, like
     * <code>{"1, 2": 400}</code> (PostgreSQL 18 and older), or a JSON array of objects holding the numbers of the
     * columns in {@code attributes} and the number of the combinations in {@code ndistinct} (PostgreSQL 19). A
     * combination holding an expression, which the statistics number by a negative number, is left out.
     *
     * @param combinations the numbers of the distinct combinations by the set of the names of the columns
     * @param text the text form of the {@code n_distinct}, or null
     * @param names the names of the columns of the table by their numbers, as a JSON object, or null
     * @throws SQLException if the text cannot be parsed
     */
    private static void addCombinations(Map<Set<String>, Double> combinations, String text, String names)
            throws SQLException
    {
        if(text == null || names == null)
            return;

        try
        {
            JsonNode columns = mapper.readTree(names);
            JsonNode root = mapper.readTree(text);

            if(root.isObject())
            {
                for(Entry<String, JsonNode> item : root.properties())
                    addCombination(combinations, columns, List.of(item.getKey().split(",")), item.getValue());
            }
            else
            {
                for(JsonNode item : root)
                {
                    List<String> attributes = new ArrayList<>();

                    for(JsonNode attribute : item.path("attributes"))
                        attributes.add(attribute.asText());

                    addCombination(combinations, columns, attributes, item.path("ndistinct"));
                }
            }
        }
        catch(JsonProcessingException e)
        {
            throw new SQLException("cannot parse the extended statistics", e);
        }
    }


    /**
     * Adds the number of the distinct combinations of the values of the columns of the given numbers, unless a number
     * is none of a column of the table, like the negative number of an expression.
     *
     * @param combinations the numbers of the distinct combinations by the set of the names of the columns
     * @param names the names of the columns of the table by their numbers
     * @param attributes the numbers of the columns
     * @param count the number of the distinct combinations
     */
    private static void addCombination(Map<Set<String>, Double> combinations, JsonNode names, List<String> attributes,
            JsonNode count)
    {
        Set<String> columns = new HashSet<>();

        for(String attribute : attributes)
        {
            JsonNode name = names.get(attribute.trim());

            if(name == null || !name.isTextual())
                return;

            columns.add(name.asText());
        }

        if(columns.size() > 1 && count.isNumber())
            combinations.put(columns, count.asDouble());
    }


    /**
     * True if a lookup has failed because the request is out of time (SQL state 57014) or cancelled, which is no answer
     * about the statistics at all.
     *
     * @param request the current request
     * @param exception the failure of the lookup
     * @return true if the lookup has failed because the request is out of time or cancelled, false otherwise
     */
    private static boolean isUnanswered(Request request, SQLRuntimeException exception)
    {
        return request.isCanceled()
                || exception.getCause() instanceof SQLException cause && "57014".equals(cause.getSQLState());
    }
}
