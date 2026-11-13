package cz.iocb.sparql.engine.database;



/**
 * Alias of a subquery inside the generated SQL statement ({@code (...) AS "tab0"}), used to qualify the columns of the
 * subquery when several are joined; only the query that gives the alias refers to it (an alias nested queries refer to
 * is an {@link OuterTable}). Aliases never appear in mappings.
 */
public final class AliasTable extends Table
{
    /**
     * Creates the alias.
     *
     * @param name the alias
     */
    public AliasTable(String name)
    {
        super(name);
    }
}
