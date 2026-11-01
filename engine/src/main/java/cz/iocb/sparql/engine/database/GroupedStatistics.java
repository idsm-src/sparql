package cz.iocb.sparql.engine.database;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import cz.iocb.sparql.engine.request.Request;



/**
 * Statistics of a database table kept per value of one of its columns (the group column) in two side tables, for a
 * table whose rows are of many kinds told apart by that column, such as a table of all the triples of a dataset with
 * the predicate as the group column. The statistics table holds a row per group and column: {@code key_value} (of the
 * type of the group column), {@code column_name text}, {@code row_count bigint} (rows of the group),
 * {@code distinct_count float8} and {@code effective_count float8} (the distinct and the effective distinct values of
 * the column in the group, see {@link ColumnStatistics#effectiveDistinct}); the optional table of the most common
 * values holds a row per group, column and value: {@code key_value}, {@code column_name text}, {@code value text} (the
 * text form of the value) and {@code row_count bigint}. An access that does not bind the group column to a value is
 * estimated from the catalog statistics of the table (see {@link CatalogStatistics}). The side tables are read through
 * the connection of the request as the groups and the values are first asked for, and the results are kept for the life
 * of the object.
 */
public final class GroupedStatistics extends TableStatisticsBase
{
    /**
     * The group column.
     */
    private final TableColumn key;

    /**
     * The statistics table.
     */
    private final DatabaseTable statisticsTable;

    /**
     * The table of the most common values, or null.
     */
    private final DatabaseTable valuesTable;

    /**
     * Statistics of the table for the accesses that do not bind the group column.
     */
    private final CatalogStatistics catalog;

    /**
     * Statistics of the groups asked for, by SQL expression of the group value.
     */
    private final Map<String, Group> groups = new ConcurrentHashMap<>();

    /**
     * Frequencies of the values asked for, by SQL expression of the group value, column name and SQL expression of the
     * value.
     */
    private final Map<String, Double> frequencies = new ConcurrentHashMap<>();


    /**
     * Creates the statistics of the table.
     *
     * @param table the table
     * @param key the group column
     * @param statisticsTable the statistics table
     * @param valuesTable the table of the most common values, or null if there is none
     */
    public GroupedStatistics(DatabaseTable table, TableColumn key, DatabaseTable statisticsTable,
            DatabaseTable valuesTable)
    {
        this.key = key;
        this.statisticsTable = statisticsTable;
        this.valuesTable = valuesTable;
        this.catalog = new CatalogStatistics(table);
    }


    @Override
    protected boolean isGroupColumn(TableColumn column)
    {
        return key.equals(column);
    }


    @Override
    protected Group lookupGroup(Request request, Condition condition)
    {
        Column value = getValues(condition).get(key);

        if(value == null)
            return catalog.lookupGroup(request, condition);

        return groups.computeIfAbsent(value.toString(), _ -> readGroup(request, value));
    }


    @Override
    protected double lookupFrequency(Request request, Group group, TableColumn column, Column value)
    {
        if(group.key() == null)
            return catalog.lookupFrequency(request, group, column, value);

        if(valuesTable == null)
            return Double.NaN;

        return frequencies.computeIfAbsent(group.key() + "\0" + column.getName() + "\0" + value,
                _ -> readFrequency(request, group, column, value));
    }


    /**
     * Reads the statistics of the group of the value from the statistics table; a group absent from the table has no
     * rows.
     *
     * @param request the current request
     * @param value SQL expression of the group value
     * @return the statistics of the group
     */
    private Group readGroup(Request request, Column value)
    {
        String sql = "SELECT s.column_name, s.row_count, s.distinct_count, s.effective_count, "
                + (valuesTable != null ? "coalesce(v.number, 0), coalesce(v.total, 0)" : "0, 0") + " FROM "
                + statisticsTable + " AS s"
                + (valuesTable != null ? " LEFT JOIN LATERAL (SELECT count(*) AS number, sum(row_count) AS total "
                        + "FROM " + valuesTable + " AS v WHERE " + keyCondition(value, "v")
                        + " AND v.column_name = s.column_name) AS v ON true" : "")
                + " WHERE " + keyCondition(value, "s");

        return query(request, sql, result -> {
            double rows = 0;
            Map<String, ColumnStatistics> columns = new HashMap<>();

            while(result.next())
            {
                rows = result.getDouble(2);
                columns.put(result.getString(1), new ColumnStatistics(Math.max(result.getDouble(3), 1),
                        result.getDouble(4), 0, rows > 0 ? result.getDouble(6) / rows : 0, result.getInt(5)));
            }

            return new Group(value.toString(), rows, columns);
        });
    }


    /**
     * Reads the frequency of the value in the column of the group from the table of the most common values.
     *
     * @param request the current request
     * @param group the group
     * @param column the column
     * @param value SQL expression of the value
     * @return frequency of the value in the column of the group, or {@code NaN} if it is not among the most common
     *         values
     */
    private double readFrequency(Request request, Group group, TableColumn column, Column value)
    {
        String sql = "SELECT row_count FROM " + valuesTable + " AS v WHERE " + keyCondition(group.key(), "v")
                + " AND column_name = " + literal(column.getName()) + " AND value = (" + value + ")::text";

        return query(request, sql, result -> {
            if(!result.next() || group.rows() <= 0)
                return Double.NaN;

            return result.getDouble(1) / group.rows();
        });
    }


    /**
     * SQL condition selecting the rows of the group of the value in a side table.
     *
     * @param value SQL expression of the group value
     * @param alias alias of the side table
     * @return SQL condition selecting the rows of the group of the value in a side table
     */
    private String keyCondition(Object value, String alias)
    {
        return key.getType().equal(new TableColumn("key_value", key.getType()).fromTable(new AliasTable(alias)),
                new ExpressionColumn(value.toString(), key.getType()));
    }
}
