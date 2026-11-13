package cz.iocb.sparql.engine.database;



/**
 * Alias of the left side of a lateral join, through which the right side refers to the solution of the left side it is
 * evaluated for. The aliases are numbered within a request (see {@code Request#createLateralTable}), because a
 * reference crosses the boundaries of nested subqueries and a nested lateral join would otherwise shadow the alias of
 * the outer one.
 */
public final class LateralTable extends Table implements OuterTable
{
    /**
     * Name of the aliases, which the number of the alias follows.
     */
    private static final String namePrefix = "lateral";


    /**
     * Creates the alias.
     *
     * @param index number of the alias among the lateral aliases of the request
     */
    public LateralTable(int index)
    {
        super(namePrefix + index);
    }
}
