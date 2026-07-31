package cz.iocb.sparql.engine.mapping;

import java.util.List;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.DatabaseSchema.ColumnPair;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.request.Request;



public abstract class TermMapping
{
    protected final ResourceClass resourceClass;
    protected final List<Column> columns;


    protected TermMapping(ResourceClass resourceClass, List<Column> columns)
    {
        this.resourceClass = resourceClass;
        this.columns = columns;

        if(resourceClass != null && columns != null && resourceClass.getColumnCount() != columns.size())
            throw new IllegalArgumentException("wrong number of columns");
    }


    public ResourceClass getResourceClass(Request request)
    {
        return resourceClass;
    }


    public List<Column> getColumns(Request request)
    {
        return columns;
    }


    public abstract boolean match(Request request, RdfTerm term);


    public abstract TermMapping remap(List<ColumnPair> columnMap);
}
