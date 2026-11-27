package cz.iocb.sparql.engine.database;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import cz.iocb.sparql.engine.database.Condition.ColumnComparison;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Request.ResultReader;



/**
 * Estimates from the statistics of groups of rows much the way the database planner does: the rows of a disjunct of the
 * conditions are the rows of the group the disjunct selects, multiplied by the selectivity of every column bound to a
 * value (the frequency of the value when it is among the most common ones, otherwise the frequency of an average of the
 * remaining values, which share the rows holding neither a most common value nor a null) and by the fraction of null or
 * not-null values of every column tested for them; the rows of a disjunction are the sum over its disjuncts. Unlike the
 * planner, which takes all those conditions for independent, the columns a disjunct requires not to be null are taken
 * to be null together (see {@link #estimateRows(Request, Condition)}). The distinct values of several columns are the
 * combinations the statistics keep for the columns together, or those of the column that has the most of them (see
 * {@link #estimateDistinct(Request, Set, Condition, double)}). Subclasses supply the groups, the frequencies and the
 * combinations, and keep what they read in caches of a bounded size (see {@link #cached}).
 */
public abstract class TableStatisticsBase implements TableStatistics
{
    /**
     * Selectivity of a column bound to a value when nothing is known about the column (the default of the planner).
     */
    protected static final double DEFAULT_SELECTIVITY = 0.005;

    /**
     * Rows of a group when nothing is known about it.
     */
    protected static final double DEFAULT_ROWS = 1000;

    /**
     * Most values a cache of the statistics keeps (see {@link #cached}): far more than the values the queries of a
     * deployment keep asking about, so that only a stream of ever new constants fills a cache, which is then emptied
     * instead of growing for the life of the statistics; a frequency takes up some 140 to 280 bytes of a cache, the
     * statistics of a group more.
     */
    protected static final int CACHE_LIMIT = 100_000;


    /**
     * Statistics of a group of rows: their number and the statistics of the columns over them.
     *
     * @param key the value of the group column selecting the rows, or null for all the rows of the table
     * @param rows number of rows
     * @param columns statistics of the columns by column name, missing for a column nothing is known about
     */
    protected record Group(String key, double rows, Map<String, ColumnStatistics> columns)
    {
    }


    /**
     * Creates the statistics.
     */
    protected TableStatisticsBase()
    {
    }


    /**
     * Statistics of the group of rows the disjunct selects, or null if nothing is known.
     *
     * @param request the current request
     * @param condition the disjunct
     * @return statistics of the group of rows the disjunct selects, or null if nothing is known
     */
    protected abstract Group lookupGroup(Request request, Condition condition);


    /**
     * Frequency of the value in the column among the rows of the group when the value is among the most common ones
     * kept, {@code NaN} otherwise.
     *
     * @param request the current request
     * @param group the group of rows
     * @param column the column
     * @param value SQL expression of the value
     * @return frequency of the value in the column among the rows of the group, or {@code NaN}
     */
    protected abstract double lookupFrequency(Request request, Group group, TableColumn column, Column value);


    /**
     * Number of the distinct combinations of the values of the columns among the rows of the group when the statistics
     * keep it for those columns together, {@code NaN} otherwise; none are kept by default.
     *
     * @param request the current request
     * @param group the group of rows
     * @param columns the columns, at least two
     * @return number of the distinct combinations of the values of the columns among the rows of the group, or
     *         {@code NaN}
     */
    protected double lookupDistinct(Request request, Group group, Set<TableColumn> columns)
    {
        return Double.NaN;
    }


    /**
     * True if the column selects the groups, so that a value bound to it is accounted for by the group itself.
     *
     * @param column the column
     * @return true if the column selects the groups, false otherwise
     */
    protected boolean isGroupColumn(TableColumn column)
    {
        return false;
    }


    @Override
    public double estimateRows(Request request, Conditions conditions)
    {
        double rows = 0;

        for(Condition condition : conditions.getConditions())
            if(!condition.isFalse())
                rows += estimateRows(request, condition);

        return rows;
    }


    @Override
    public double estimateDistinct(Request request, Set<Column> columns, Conditions conditions)
    {
        double total = 0;

        for(Condition condition : conditions.getConditions())
        {
            if(condition.isFalse())
                continue;

            double rows = estimateRows(request, condition);

            if(rows > 0)
                total += estimateDistinct(request, columns, condition, rows);
        }

        return total;
    }


    /**
     * Estimated number of distinct combinations of the values of the columns among the rows satisfying the disjunct:
     * the combinations of the columns the disjunct does not bind to values where the statistics keep them for those
     * columns together (see {@link #lookupDistinct}), and at least the most distinct values any of the columns has, as
     * a combination has at least as many values as each of its columns; without the statistics of the columns together
     * that is all, as the columns holding one term together, like the parts of an IRI, mostly have one of them
     * determine the others. A constant and a column the disjunct binds to a value have one value, a column with
     * statistics the skew-corrected number of its distinct values (see {@link ColumnStatistics#joinDistinct}) and a
     * column nothing is known about, like an expression, a distinct value in every row, all of them capped by the rows.
     *
     * @param request the current request
     * @param columns the columns
     * @param condition the disjunct
     * @param rows estimated number of rows satisfying the disjunct
     * @return estimated number of distinct combinations of the values of the columns among the rows satisfying the
     *         disjunct
     */
    private double estimateDistinct(Request request, Set<Column> columns, Condition condition, double rows)
    {
        Map<TableColumn, Column> values = getValues(condition);
        Group group = lookupGroup(request, condition);
        Set<TableColumn> free = new HashSet<>();
        double distinct = 1;

        for(Column column : columns)
        {
            if(column instanceof ConstantColumn || column instanceof TableColumn bound && values.containsKey(bound))
                continue;

            ColumnStatistics statistics = null;

            if(column instanceof TableColumn tableColumn)
            {
                free.add(tableColumn);
                statistics = group != null ? group.columns().get(tableColumn.getName()) : null;
            }

            distinct = Math.max(distinct, statistics != null ? Math.min(statistics.joinDistinct(), rows) : rows);
        }

        if(free.size() > 1 && group != null)
        {
            double combinations = lookupDistinct(request, group, free);

            if(!Double.isNaN(combinations))
                distinct = Math.max(distinct, Math.min(combinations, rows));
        }

        return distinct;
    }


    @Override
    public double estimateNullFraction(Request request, Column column)
    {
        return column instanceof TableColumn tableColumn ?
                nullFraction(lookupGroup(request, new Condition()), tableColumn) : 0;
    }


    /**
     * Estimated number of rows satisfying the disjunct. The columns the disjunct requires not to be null, by a test or
     * by a value, are taken to be null together: a table of rows of several kinds has the columns of a kind filled in
     * the rows of that kind and null in the others, so the rows in which a column is not null are mostly among those in
     * which a column of fewer nulls is not null either, far from being independent of them. The columns together thus
     * leave the rows of the one with the most nulls, and a value is taken to be as frequent among those rows as it is
     * among the rows in which its own column is not null. Taken for independent, each such column would take its nulls
     * away from the rows the others have left, and the rows of a kind, all of which have its columns filled, would come
     * out the fewer the more of its columns are asked for. A column required to be null is taken for independent of the
     * others.
     *
     * @param request the current request
     * @param condition the disjunct
     * @return estimated number of rows satisfying the disjunct
     */
    protected double estimateRows(Request request, Condition condition)
    {
        Group group = lookupGroup(request, condition);
        Map<TableColumn, Column> values = getValues(condition);

        double rows = group != null ? group.rows() : DEFAULT_ROWS;

        // the fraction of the rows in which all the columns required not to be null are not null
        double present = 1;

        for(Entry<TableColumn, Column> entry : values.entrySet())
        {
            if(isGroupColumn(entry.getKey()))
                continue;

            double filled = 1 - nullFraction(group, entry.getKey());

            // the frequency of the value among the rows in which its column is not null
            if(filled > 0)
                rows *= Math.min(selectivity(request, group, entry.getKey(), entry.getValue()) / filled, 1);

            present = Math.min(present, filled);
        }

        for(Column column : condition.getIsNull())
            if(column instanceof TableColumn tableColumn)
                rows *= nullFraction(group, tableColumn);

        for(Column column : condition.getIsNotNull())
            if(column instanceof TableColumn tableColumn && !values.containsKey(tableColumn))
                present = Math.min(present, 1 - nullFraction(group, tableColumn));

        return rows * Math.max(present, 0);
    }


    /**
     * Selectivity of the column bound to the value among the rows of the group: the frequency of the value when it is
     * among the most common ones, and otherwise an equal share of the rows left to the other values, which are the rows
     * that hold neither a most common value nor a null, as no value is equal to a null.
     *
     * @param request the current request
     * @param group the group of rows, or null if nothing is known about it
     * @param column the column
     * @param value SQL expression of the value
     * @return selectivity of the column bound to the value among the rows of the group
     */
    protected double selectivity(Request request, Group group, TableColumn column, Column value)
    {
        ColumnStatistics statistics = group != null ? group.columns().get(column.getName()) : null;

        if(statistics == null)
            return DEFAULT_SELECTIVITY;

        double frequency = lookupFrequency(request, group, column, value);

        if(!Double.isNaN(frequency))
            return frequency;

        double rest = Math.max(1 - statistics.nullFraction() - statistics.mostCommonFraction(), 0);
        double others = Math.max(statistics.distinct() - statistics.mostCommonCount(), 1);

        return rest / others;
    }


    /**
     * Fraction of the rows of the group whose value of the column is null; 0 when nothing is known about it.
     *
     * @param group the group of rows, or null if nothing is known about it
     * @param column the column
     * @return fraction of the rows of the group whose value of the column is null
     */
    protected static double nullFraction(Group group, TableColumn column)
    {
        ColumnStatistics statistics = group != null ? group.columns().get(column.getName()) : null;

        return statistics != null ? statistics.nullFraction() : 0;
    }


    /**
     * Table columns the disjunct binds to values (constants or expressions not taken from the table), with the values.
     *
     * @param condition the disjunct
     * @return table columns the disjunct binds to values, with the values
     */
    protected static Map<TableColumn, Column> getValues(Condition condition)
    {
        Map<TableColumn, Column> values = new HashMap<>();

        for(ColumnComparison comparison : condition.getAreEqual())
            addValue(values, comparison);

        for(ColumnComparison comparison : condition.getAreNotDistinct())
            addValue(values, comparison);

        return values;
    }


    /**
     * Records the value the comparison binds a table column to, if it binds one.
     *
     * @param values the values by column
     * @param comparison the comparison
     */
    private static void addValue(Map<TableColumn, Column> values, ColumnComparison comparison)
    {
        if(comparison.getLeft() instanceof TableColumn column && !(comparison.getRight() instanceof TableColumn))
            values.putIfAbsent(column, comparison.getRight());
        else if(comparison.getRight() instanceof TableColumn column && !(comparison.getLeft() instanceof TableColumn))
            values.putIfAbsent(column, comparison.getLeft());
    }


    /**
     * The value the cache keeps for the key, read and kept when the cache does not hold it yet; a cache holding
     * {@link #CACHE_LIMIT} values already is emptied first, so that the values asked for over the life of the
     * statistics do not take up ever more memory, a value asked for again being read anew once.
     *
     * @param <V> type of the values
     * @param cache the cache, a concurrent map
     * @param key the key
     * @param reader reader of the value of a key
     * @return the value of the key
     */
    protected static <V> V cached(Map<String, V> cache, String key, Function<String, V> reader)
    {
        if(cache.size() >= CACHE_LIMIT && !cache.containsKey(key))
            cache.clear();

        return cache.computeIfAbsent(key, reader);
    }


    /**
     * Runs a lookup query on the connection of the request, so that a failure does not abort the transaction of the
     * request (see {@link Request#lookup}).
     *
     * @param <T> type of the result
     * @param request the current request
     * @param sql the query
     * @param reader reader of the result
     * @return the result
     * @throws SQLRuntimeException on database errors
     */
    protected static <T> T query(Request request, String sql, ResultReader<T> reader)
    {
        try
        {
            return request.lookup(List.of(sql), reader).get(0);
        }
        catch(SQLException e)
        {
            throw new SQLRuntimeException(e);
        }
    }


    /**
     * SQL string literal of the text, which stands for the text whatever the setting
     * {@code standard_conforming_strings} of the server is (see {@link ValueColumn#quote}).
     *
     * @param text the text
     * @return SQL string literal of the text
     */
    protected static String literal(String text)
    {
        return ValueColumn.quote(text);
    }
}
