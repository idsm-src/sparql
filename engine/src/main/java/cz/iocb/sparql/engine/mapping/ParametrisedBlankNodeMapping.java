package cz.iocb.sparql.engine.mapping;

import java.util.List;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ColumnPair;
import cz.iocb.sparql.engine.mapping.classes.BlankNodeClass;



/**
 * Column-based mapping of blank nodes of a {@link BlankNodeClass}.
 */
public class ParametrisedBlankNodeMapping extends ParametrisedMapping
{
    /**
     * Creates the mapping.
     *
     * @param blankNodeClass the blank node class
     * @param columns the columns
     */
    public ParametrisedBlankNodeMapping(BlankNodeClass blankNodeClass, List<Column> columns)
    {
        super(blankNodeClass, columns);
    }


    @Override
    public TermMapping remap(List<ColumnPair> columnMap)
    {
        return new ParametrisedBlankNodeMapping((BlankNodeClass) resourceClass, remapColumns(columnMap));
    }
}
