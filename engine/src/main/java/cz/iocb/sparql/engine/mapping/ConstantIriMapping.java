package cz.iocb.sparql.engine.mapping;

import java.util.List;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.mapping.classes.IriClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.parser.model.IRI;
import cz.iocb.sparql.engine.request.Request;



public class ConstantIriMapping extends ConstantMapping
{
    public ConstantIriMapping(IRI iri, IriClass iriClass, List<Column> columns)
    {
        super(iri, iriClass, columns);
    }


    public ConstantIriMapping(IRI iri)
    {
        this(iri, null, null);
    }


    @Override
    public ResourceClass getResourceClass(Request request)
    {
        if(resourceClass != null)
            return resourceClass;

        return request.getIriClass((IRI) value);
    }


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


    public List<Column> getColumns()
    {
        return columns;
    }


    public IRI getIRI()
    {
        return (IRI) value;
    }
}
