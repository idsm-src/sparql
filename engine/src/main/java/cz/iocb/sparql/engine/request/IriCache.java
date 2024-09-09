package cz.iocb.sparql.engine.request;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.mapping.classes.IriClass;
import cz.iocb.sparql.engine.parser.model.IRI;



public class IriCache
{
    private static record CacheItem(IriClass iriClass, List<Column> columns)
    {
    }


    public static final List<Column> mismatch = new ArrayList<Column>(0);

    private final Map<IRI, CacheItem> cache;
    private final Map<CacheItem, IRI> revCache;


    public IriCache(int majorSize)
    {
        this.cache = new HashMap<IRI, CacheItem>(majorSize);
        this.revCache = new HashMap<CacheItem, IRI>();
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


    public IRI getFromCache(IriClass iriClass, List<Column> columns)
    {
        CacheItem item = new CacheItem(iriClass, columns);
        return revCache.get(item);
    }


    public void storeToCache(IRI iri, IriClass iriClass, List<Column> columns)
    {
        CacheItem item = new CacheItem(iriClass, columns);
        cache.put(iri, item);
        revCache.put(item, iri);
    }
}
