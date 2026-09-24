package cz.iocb.sparql.engine.database;

import cz.iocb.sparql.engine.common.Pair;



/**
 * Pair of a parent (referenced) table and a foreign (referencing) table.
 */
public class TablePair extends Pair<SourceTable, SourceTable>
{
    /**
     * Creates the pair of a referenced and a referencing table.
     *
     * @param parent the referenced table
     * @param foreign the referencing table
     */
    public TablePair(SourceTable parent, SourceTable foreign)
    {
        super(parent, foreign);
    }
}
