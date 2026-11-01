package cz.iocb.sparql.engine.database;

import cz.iocb.sparql.engine.request.Request;



/**
 * Estimates of the rows a table access selects and of the distinct values of its columns, kept by the engine on its own
 * (see {@link cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration#getTableStatistics}) because the estimates of
 * the database planner are not enough where the table holds data of many kinds at once, such as all the triples of a
 * dataset: the planner estimates the selectivity of every column independently of the others, while the statistics of
 * the engine may be kept per value of a column (see {@link GroupedStatistics}). The estimates are consulted when the
 * engine chooses the order and the methods of the joins it generates.
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
     * Estimated number of distinct values of the column among the rows satisfying the conditions; at most the estimated
     * number of the rows, and 1 for a column bound to a value by the conditions.
     *
     * @param request the current request, whose database connection may be used for lookups
     * @param column the column
     * @param conditions conditions on the columns of the table
     * @return estimated number of distinct values of the column among the rows satisfying the conditions
     */
    double estimateDistinct(Request request, Column column, Conditions conditions);
}
