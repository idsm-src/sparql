package cz.iocb.sparql.engine.database;

import cz.iocb.sparql.engine.request.Request;



/**
 * Statistics of a table nothing is known about, such as a virtual table: the defaults of the planner.
 */
public final class DefaultStatistics extends TableStatisticsBase
{
    /**
     * Creates the statistics.
     */
    public DefaultStatistics()
    {
    }


    @Override
    protected Group lookupGroup(Request request, Condition condition)
    {
        return null;
    }


    @Override
    protected double lookupFrequency(Request request, Group group, TableColumn column, Column value)
    {
        return Double.NaN;
    }
}
