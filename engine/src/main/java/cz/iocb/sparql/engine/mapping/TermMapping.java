package cz.iocb.sparql.engine.mapping;

import java.util.List;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.DatabaseSchema.ColumnPair;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.request.Request;



/**
 * Describes how one position of a quad (graph, subject, predicate or object) is obtained from the database: the
 * resource class of the term and the columns holding its representation.
 */
public abstract class TermMapping
{
    /**
     * Resource class of the term; null when resolved lazily.
     */
    protected final ResourceClass resourceClass;

    /**
     * Columns representing the term; null when resolved lazily.
     */
    protected final List<Column> columns;


    /**
     * Creates the mapping; the column count must match the class.
     *
     * @param resourceClass the resource class
     * @param columns the columns
     */
    protected TermMapping(ResourceClass resourceClass, List<Column> columns)
    {
        this.resourceClass = resourceClass;
        this.columns = columns;

        if(resourceClass != null && columns != null && resourceClass.getColumnCount() != columns.size())
            throw new IllegalArgumentException("wrong number of columns");
    }


    /**
     * Resource class of the mapped terms. The request is needed only by constant IRI mappings that resolve their class
     * lazily.
     *
     * @param request the current request
     * @return resource class of the mapped terms
     */
    public ResourceClass getResourceClass(Request request)
    {
        return resourceClass;
    }


    /**
     * Columns (table columns or constants) representing the term, one per column of the resource class.
     *
     * @param request the current request
     * @return columns (table columns or constants) representing the term, one per column of the resource class
     */
    public List<Column> getColumns(Request request)
    {
        return columns;
    }


    /**
     * True if the mapping can produce the given term; a variable matches any mapping.
     *
     * @param request the current request
     * @param term the RDF term
     * @return true if the mapping can produce the given term, false otherwise
     */
    public abstract boolean match(Request request, RdfTerm term);


    /**
     * Returns this mapping with each column replaced according to {@code columnMap} (left to right).
     *
     * @param columnMap the column map
     * @return this mapping with each column replaced according to {@code columnMap} (left to right)
     */
    public abstract TermMapping remap(List<ColumnPair> columnMap);
}
