package cz.iocb.sparql.engine.mapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.DatabaseSchema.ColumnPair;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.request.Request;



/**
 * Mapping of a position to table columns; matches every term representable in its resource class.
 */
public abstract class ParametrisedMapping extends TermMapping
{
    /**
     * Creates the mapping.
     *
     * @param resourceClass the resource class
     * @param columns the columns
     */
    protected ParametrisedMapping(ResourceClass resourceClass, List<Column> columns)
    {
        super(resourceClass, columns);
    }


    @Override
    public boolean match(Request request, RdfTerm term)
    {
        return request.match(resourceClass, term);
    }


    /**
     * Replaces each column by its counterpart in {@code columnMap} (left to right).
     *
     * @param columnMap the column map
     * @return the remapped columns
     */
    public List<Column> remapColumns(List<ColumnPair> columnMap)
    {
        List<Column> remappedColumns = new ArrayList<>();

        for(Column col : columns)
            remappedColumns.add(columnMap.stream().filter(s -> s.getLeft().equals(col)).findAny().get().getRight());

        return remappedColumns;
    }


    @Override
    public int hashCode()
    {
        return columns.hashCode();
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(object == null || getClass() != object.getClass())
            return false;

        ParametrisedMapping other = (ParametrisedMapping) object;

        return Objects.equals(resourceClass, other.resourceClass) && columns.equals(other.columns);
    }
}
