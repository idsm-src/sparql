package cz.iocb.sparql.engine.request;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.mapping.classes.IriClass;
import cz.iocb.sparql.engine.rdf.Iri;



public class IriCache
{
    private static record CacheItem(IriClass iriClass, List<Column> columns)
    {
    }


    private final Map<Iri, CacheItem> cache;


    public IriCache(int majorSize)
    {
        cache = new HashMap<>(majorSize);
    }


    public IriClass getIriClass(Iri iri)
    {
        CacheItem items = cache.get(iri);

        if(items == null)
            return null;

        return items.iriClass();
    }


    public List<Column> getIriColumns(Iri iri)
    {
        CacheItem items = cache.get(iri);

        if(items == null)
            return null;

        return items.columns();
    }


    public Iri getIri(IriClass iriClass, List<Column> columns)
    {
        CacheItem item = new CacheItem(iriClass, columns);

        for(Entry<Iri, CacheItem> entry : cache.entrySet())
            if(entry.getValue().equals(item))
                return entry.getKey();

        return null;
    }


    public void storeToCache(Iri iri, IriClass iriClass, List<Column> columns)
    {
        CacheItem item = new CacheItem(iriClass, columns);
        cache.put(iri, item);
    }
}
