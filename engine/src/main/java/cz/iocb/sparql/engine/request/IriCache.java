package cz.iocb.sparql.engine.request;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.mapping.classes.IriClass;
import cz.iocb.sparql.engine.parser.model.IRI;



public class IriCache
{
    private static record CacheItem(IriClass iriClass, List<Column> columns)
    {
    }


    private final Map<IRI, CacheItem> cache;


    public IriCache(int majorSize)
    {
        cache = new HashMap<IRI, CacheItem>(majorSize);
    }


    public IriClass getIriClass(IRI iri)
    {
        CacheItem items = cache.get(iri);

        if(items == null)
            return null;

        return items.iriClass();
    }


    public List<Column> getIriColumns(IRI iri)
    {
        CacheItem items = cache.get(iri);

        if(items == null)
            return null;

        return items.columns();
    }


    public IRI getIri(IriClass iriClass, List<Column> columns)
    {
        CacheItem item = new CacheItem(iriClass, columns);

        for(Entry<IRI, CacheItem> entry : cache.entrySet())
            if(entry.getValue().equals(item))
                return entry.getKey();

        return null;
    }


    public void storeToCache(IRI iri, IriClass iriClass, List<Column> columns)
    {
        CacheItem item = new CacheItem(iriClass, columns);
        cache.put(iri, item);
    }
}
