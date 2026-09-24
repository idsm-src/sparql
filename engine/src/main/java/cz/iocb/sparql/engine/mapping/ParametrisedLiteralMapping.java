package cz.iocb.sparql.engine.mapping;

import java.util.List;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ColumnPair;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;



/**
 * Column-based mapping of literals of a {@link LiteralClass}.
 */
public class ParametrisedLiteralMapping extends ParametrisedMapping
{
    /**
     * Creates the mapping.
     *
     * @param literalClass the literal class
     * @param columns the columns
     */
    public ParametrisedLiteralMapping(LiteralClass literalClass, List<Column> columns)
    {
        super(literalClass, columns);
    }


    @Override
    public TermMapping remap(List<ColumnPair> columnMap)
    {
        return new ParametrisedLiteralMapping((LiteralClass) resourceClass, remapColumns(columnMap));
    }
}
