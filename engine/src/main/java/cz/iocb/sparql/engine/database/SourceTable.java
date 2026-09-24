package cz.iocb.sparql.engine.database;



/**
 * Relation that a quad mapping can read from: a {@link DatabaseTable} stored in the database or a {@link VirtualTable}
 * defined by the configuration and attached to the generated statement as a common table expression. Both kinds are
 * described to the optimiser by a {@link DatabaseSchema}.
 */
public abstract class SourceTable extends Table
{
    /**
     * Creates the reference.
     *
     * @param name the table name
     */
    protected SourceTable(String name)
    {
        super(name);
    }
}
