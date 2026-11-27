package cz.iocb.sparql.engine.database;

import java.util.Set;
import cz.iocb.sparql.engine.request.Request;



/**
 * Estimates of the rows a table access selects and of the distinct values of its columns, kept by the engine on its own
 * (see {@link cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration#getTableStatistics}) because the estimates of
 * the database planner are not enough where the table holds data of many kinds at once, such as all the triples of a
 * dataset: the planner estimates the selectivity of every column independently of the others, while the statistics of
 * the engine may be kept per value of a column (see {@link GroupedStatistics}). The statistics are only read, from the
 * catalog of the database or from tables kept for the engine, and never counted from the rows of the table, so the
 * statistics the catalog does not keep are computed together with the data and stored in such tables. The estimates are
 * consulted when the engine chooses the order and the methods of the joins it generates.
 */
public interface TableStatistics
{
    /**
     * Estimated number of rows of the table satisfying the conditions.
     *
     * @param request the current request, whose database connection may be used for lookups
     * @param conditions conditions on the columns of the table
     * @return estimated number of rows of the table satisfying the conditions
     */
    double estimateRows(Request request, Conditions conditions);


    /**
     * Estimated number of distinct combinations of the values of the columns among the rows satisfying the conditions,
     * like the distinct values of a variable held in several columns; at most the estimated number of the rows, and 1
     * for columns all bound to values by the conditions.
     *
     * @param request the current request, whose database connection may be used for lookups
     * @param columns the columns
     * @param conditions conditions on the columns of the table
     * @return estimated number of distinct combinations of the values of the columns among the rows satisfying the
     *         conditions
     */
    double estimateDistinct(Request request, Set<Column> columns, Conditions conditions);


    /**
     * Estimated fraction of the rows of the table whose value of the column is null, as the database planner takes it
     * for the table as a whole; 0 when nothing is known about the column.
     *
     * @param request the current request, whose database connection may be used for lookups
     * @param column the column
     * @return estimated fraction of the rows of the table whose value of the column is null
     */
    double estimateNullFraction(Request request, Column column);
}
