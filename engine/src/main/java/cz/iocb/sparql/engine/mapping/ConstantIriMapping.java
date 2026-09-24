package cz.iocb.sparql.engine.mapping;

import java.util.List;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.mapping.classes.IriClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.request.Request;



/**
 * Mapping of a position to a fixed IRI. When created from the IRI alone, its IRI class and columns are detected on
 * first use through the request (and cached).
 */
public class ConstantIriMapping extends ConstantMapping
{
    /**
     * Creates the mapping with an explicit IRI class and columns.
     *
     * @param iri the IRI
     * @param iriClass the IRI class
     * @param columns the columns
     */
    public ConstantIriMapping(Iri iri, ResourceClass iriClass, List<Column> columns)
    {
        super(iri, iriClass, columns);
    }


    /**
     * Creates the mapping; the IRI class and columns are detected lazily.
     *
     * @param iri the IRI
     */
    public ConstantIriMapping(Iri iri)
    {
        this(iri, null, null);
    }


    @Override
    public ResourceClass getResourceClass(Request request)
    {
        if(resourceClass != null)
            return resourceClass;

        return request.getIriClass((Iri) value);
    }


    /**
     * The IRI class given at construction, or null when it is detected lazily.
     *
     * @return the IRI class given at construction, or null when it is detected lazily
     */
    public IriClass getResourceClass()
    {
        return (IriClass) resourceClass;
    }


    @Override
    public List<Column> getColumns(Request request)
    {
        if(columns != null)
            return columns;

        return request.getColumns(getResourceClass(request), value);
    }


    /**
     * The columns given at construction, or null when they are detected lazily.
     *
     * @return the columns given at construction, or null when they are detected lazily
     */
    public List<Column> getColumns()
    {
        return columns;
    }


    /**
     * The constant IRI.
     *
     * @return the constant IRI
     */
    public Iri getIri()
    {
        return (Iri) value;
    }
}
