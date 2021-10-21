package cz.iocb.chemweb.server.sparql.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import cz.iocb.chemweb.server.sparql.database.Column;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;



public class IriCache
{
    public static final List<Column> mismatch = new ArrayList<Column>(0);

    private final Map<IRI, Map<ResourceClass, List<Column>>> cache;
    private final int minorSize;


    public IriCache(int majorSize, int minorSize)
    {
        this.cache = new HashMap<IRI, Map<ResourceClass, List<Column>>>(majorSize);
        this.minorSize = minorSize;
    }


    public List<Column> getFromCache(IRI iri, ResourceClass resClass)
    {
        Map<ResourceClass, List<Column>> items = cache.get(iri);

        if(items == null)
            return null;

        return items.get(resClass);
    }


    public void storeToCache(IRI iri, ResourceClass resClass, List<Column> columns)
    {
        Map<ResourceClass, List<Column>> items = cache.get(iri);

        if(items == null)
        {
            items = new HashMap<ResourceClass, List<Column>>(minorSize);
            cache.put(iri, items);
        }

        items.put(resClass, columns);
    }
}
