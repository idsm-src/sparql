package cz.iocb.sparql.engine.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import cz.iocb.sparql.engine.database.Condition.ColumnComparison;
import cz.iocb.sparql.engine.request.Request;



/**
 * Estimates from the statistics of groups of rows the way the database planner does: the rows of a disjunct of the
 * conditions are the rows of the group the disjunct selects, multiplied by the selectivity of every column bound to a
 * value (the frequency of the value when it is among the most common ones, otherwise the frequency of an average of the
 * remaining values) and by the fraction of null or not-null values of every column tested for them; the rows of a
 * disjunction are the sum over its disjuncts. Subclasses supply the groups and the frequencies.
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
     * Reads the result of a lookup query.
     *
     * @param <T> type of the result
     */
    @FunctionalInterface
    protected interface ResultReader<T>
    {
        /**
         * Reads the result from the result set.
         *
         * @param result the result set, positioned before its first row
         * @return the result
         * @throws SQLException on database errors
         */
        T read(ResultSet result) throws SQLException;
    }


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
    public double estimateDistinct(Request request, Column column, Conditions conditions)
    {
        double total = 0;

        for(Condition condition : conditions.getConditions())
        {
            if(condition.isFalse())
                continue;

            double rows = estimateRows(request, condition);

            if(rows <= 0)
                continue;

            double distinct = rows;

            if(column instanceof ConstantColumn)
            {
                distinct = 1;
            }
            else if(column instanceof TableColumn tableColumn)
            {
                Map<TableColumn, Column> values = getValues(condition);

                if(values.containsKey(tableColumn))
                {
                    distinct = 1;
                }
                else
                {
                    Group group = lookupGroup(request, condition);
                    ColumnStatistics statistics = group != null ? group.columns().get(tableColumn.getName()) : null;

                    if(statistics != null)
                        distinct = Math.min(statistics.joinDistinct(), rows);
                }
            }

            total += distinct;
        }

        return total;
    }


    /**
     * Estimated number of rows satisfying the disjunct.
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

        for(Entry<TableColumn, Column> entry : values.entrySet())
            if(!isGroupColumn(entry.getKey()))
                rows *= selectivity(request, group, entry.getKey(), entry.getValue());

        for(Column column : condition.getIsNull())
            if(column instanceof TableColumn tableColumn)
                rows *= nullFraction(group, tableColumn);

        for(Column column : condition.getIsNotNull())
            if(column instanceof TableColumn tableColumn && !values.containsKey(tableColumn))
                rows *= 1 - nullFraction(group, tableColumn);

        return rows;
    }


    /**
     * Selectivity of the column bound to the value among the rows of the group.
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

        double rest = Math.max(1 - statistics.mostCommonFraction(), 0);
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
     * Runs a lookup query on the connection of the request, under a savepoint so that a failure does not abort the
     * transaction of the request.
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
            Statement statement = request.getStatement();

            statement.execute("SAVEPOINT statistics");

            try(ResultSet result = statement.executeQuery(sql))
            {
                T value = reader.read(result);
                statement.execute("RELEASE SAVEPOINT statistics");
                return value;
            }
            catch(SQLException e)
            {
                statement.execute("ROLLBACK TO SAVEPOINT statistics");
                throw e;
            }
        }
        catch(SQLException e)
        {
            throw new SQLRuntimeException(e);
        }
    }


    /**
     * SQL string literal of the text.
     *
     * @param text the text
     * @return SQL string literal of the text
     */
    protected static String literal(String text)
    {
        return "'" + text.replace("'", "''") + "'";
    }
}
