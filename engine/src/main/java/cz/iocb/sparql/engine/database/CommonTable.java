package cz.iocb.sparql.engine.database;



/**
 * Name of a common table expression the engine defines for a query that a statement is to evaluate once, however many
 * places of the statement read its result, together with the query. The tables are numbered within a request in the
 * order they are asked for (see {@code Request#getCommonTableExpression}), and the name follows from the number. A
 * statement whose SQL reads the table declares it in its {@code WITH} clause, along with the declared tables the query
 * reads itself; since a query is complete before a table is asked for it, such a table always has a lower number.
 */
public final class CommonTable extends Table implements DeclaredTable
{
    /**
     * Name of the tables, which the number of the table follows.
     */
    private static final String namePrefix = "cte";

    /**
     * Number of the table among the common table expressions of the request.
     */
    private final int index;

    /**
     * The query the common table expression evaluates.
     */
    private final SqlCode query;


    /**
     * Creates the table.
     *
     * @param index number of the table among the common table expressions of the request
     * @param query the query the common table expression evaluates
     */
    public CommonTable(int index, SqlCode query)
    {
        super(namePrefix + index);

        this.index = index;
        this.query = query;
    }


    /**
     * Number of the table among the common table expressions of the request.
     *
     * @return number of the table among the common table expressions of the request
     */
    public int getIndex()
    {
        return index;
    }


    /**
     * The query the common table expression evaluates.
     *
     * @return the query the common table expression evaluates
     */
    public SqlCode getQuery()
    {
        return query;
    }
}
