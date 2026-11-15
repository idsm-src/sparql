package cz.iocb.sparql.engine.database;



/**
 * Name of a common table expression the engine defines for a query that a statement is to evaluate once, however many
 * places of the statement read its result, together with the query. The tables are numbered within a request in the
 * order they are asked for (see {@code Request#getCommonTableExpression}), and the name follows from the number. A
 * statement whose SQL reads the table declares it in its {@code WITH} clause, along with the declared tables the query
 * reads itself; since a query is complete before a table is asked for it, such a table always has a lower number. The
 * database evaluates a query that a single place of the statement reads in that place, as if it were written there, so
 * a table whose result has to be kept all the same (the place being evaluated again and again) is declared as
 * materialized (see {@link #isMaterialized}).
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
     * Whether the database is told to evaluate the query once and keep its result, wherever the table is read.
     */
    private final boolean materialized;


    /**
     * Creates the table.
     *
     * @param index number of the table among the common table expressions of the request
     * @param query the query the common table expression evaluates
     * @param materialized whether the database is told to evaluate the query once and keep its result
     */
    public CommonTable(int index, SqlCode query, boolean materialized)
    {
        super(namePrefix + index);

        this.index = index;
        this.query = query;
        this.materialized = materialized;
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


    /**
     * Whether the database is told to evaluate the query once and keep its result, wherever the table is read, also
     * when a single place of the statement reads it. A statement declares the table so (see
     * {@code SqlSelect#translateWithClause}).
     *
     * @return true if the table is declared as materialized, false otherwise
     */
    public boolean isMaterialized()
    {
        return materialized;
    }
}
