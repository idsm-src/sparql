package cz.iocb.sparql.engine.database;

import cz.iocb.sparql.engine.common.Pair;



/**
 * Pair of a parent table column and the foreign table column referencing it.
 */
public class ColumnPair extends Pair<Column, Column>
{
    /**
     * Creates the pair of a referenced column and the column referencing it.
     *
     * @param parent the referenced column
     * @param foreign the referencing column
     */
    public ColumnPair(Column parent, Column foreign)
    {
        super(parent, foreign);
    }
}
