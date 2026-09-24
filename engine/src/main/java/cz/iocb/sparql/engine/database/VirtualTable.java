package cz.iocb.sparql.engine.database;



/**
 * Name of a table defined by the configuration rather than stored in the database. Its {@link VirtualTableDefinition}
 * is registered in the {@code SparqlDatabaseConfiguration}, and every generated statement that reads the table declares
 * it in a {@code WITH} clause (common table expression) under this name. The name therefore has to be unique among the
 * virtual tables of a configuration and must not collide with the aliases the engine generates ({@code tab0},
 * {@code lateral0}, {@code recursion} and the like).
 */
public final class VirtualTable extends SourceTable
{
    /**
     * Creates the reference.
     *
     * @param name the table name
     */
    public VirtualTable(String name)
    {
        super(name);
    }
}
