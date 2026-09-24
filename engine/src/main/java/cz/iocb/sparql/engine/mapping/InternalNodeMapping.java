package cz.iocb.sparql.engine.mapping;

import java.util.List;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.DatabaseSchema.ColumnPair;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;



/**
 * Column-based mapping of an engine-internal value that is not an RDF term, used to expose the join columns between the
 * tables of a join mapping as hidden variables.
 */
public final class InternalNodeMapping extends ParametrisedMapping
{
    /**
     * Creates the mapping over the given internal class and columns.
     *
     * @param resourceClass the resource class
     * @param columns the columns
     */
    public InternalNodeMapping(ResourceClass resourceClass, List<Column> columns)
    {
        super(resourceClass, columns);
    }


    @Override
    public TermMapping remap(List<ColumnPair> columnMap)
    {
        return new InternalNodeMapping(resourceClass, remapColumns(columnMap));
    }
}
