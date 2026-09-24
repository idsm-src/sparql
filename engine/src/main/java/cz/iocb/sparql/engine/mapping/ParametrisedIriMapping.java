package cz.iocb.sparql.engine.mapping;

import java.util.List;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.DatabaseSchema.ColumnPair;
import cz.iocb.sparql.engine.mapping.classes.IriClass;



/**
 * Column-based mapping of IRIs of an {@link IriClass}.
 */
public class ParametrisedIriMapping extends ParametrisedMapping
{
    /**
     * Creates the mapping.
     *
     * @param iriClass the IRI class
     * @param columns the columns
     */
    public ParametrisedIriMapping(IriClass iriClass, List<Column> columns)
    {
        super(iriClass, columns);
    }


    @Override
    public TermMapping remap(List<ColumnPair> columnMap)
    {
        return new ParametrisedIriMapping((IriClass) resourceClass, remapColumns(columnMap));
    }


    /**
     * The IRI class.
     *
     * @return the IRI class
     */
    public IriClass getResourceClass()
    {
        return (IriClass) resourceClass;
    }


    /**
     * The columns.
     *
     * @return the columns
     */
    public List<Column> getColumns()
    {
        return columns;
    }
}
