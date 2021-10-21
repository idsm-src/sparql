package cz.iocb.chemweb.server.sparql.mapping;

import java.util.List;
import cz.iocb.chemweb.server.sparql.database.Column;
import cz.iocb.chemweb.server.sparql.engine.Request;
import cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.UserIriClass;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;



public class ConstantIriMapping extends ConstantMapping
{
    public ConstantIriMapping(IRI iri)
    {
        super(iri);
    }


    @SuppressWarnings("resource")
    @Override
    public ResourceClass getResourceClass()
    {
        //TODO: use cache

        for(UserIriClass iriClass : Request.currentRequest().getConfiguration().getIriClasses())
            if(iriClass.match(value))
                return iriClass;

        return BuiltinClasses.unsupportedIri;
    }


    @SuppressWarnings("resource")
    @Override
    public List<Column> getColumns()
    {
        //TODO: use cache

        for(UserIriClass iriClass : Request.currentRequest().getConfiguration().getIriClasses())
            if(iriClass.match(value))
                return iriClass.toColumns(value);

        return BuiltinClasses.unsupportedIri.toColumns(value);
    }
}
