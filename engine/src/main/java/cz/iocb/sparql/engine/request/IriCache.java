package cz.iocb.sparql.engine.request;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.Iri;



/**
 * Cache of detected IRI classes and the corresponding constant columns, keyed by IRI. One instance is shared by the
 * configuration, another is private to each request.
 */
public class IriCache
{
    /**
     * Cached class and columns of an IRI.
     *
     * @param iriClass the IRI class
     * @param columns the columns
     */
    private static record CacheItem(ResourceClass iriClass, List<Column> columns)
    {
    }


    /**
     * Cache entries by IRI.
     */
    private final Map<Iri, CacheItem> cache;


    /**
     * Creates the cache with the given initial capacity.
     *
     * @param majorSize the initial capacity
     */
    public IriCache(int majorSize)
    {
        cache = new HashMap<>(majorSize);
    }


    /**
     * Cached class of the IRI, or null.
     *
     * @param iri the IRI
     * @return cached class of the IRI, or null
     */
    public ResourceClass getIriClass(Iri iri)
    {
        CacheItem items = cache.get(iri);

        if(items == null)
            return null;

        return items.iriClass();
    }


    /**
     * Cached columns of the IRI, or null.
     *
     * @param iri the IRI
     * @return cached columns of the IRI, or null
     */
    public List<Column> getIriColumns(Iri iri)
    {
        CacheItem items = cache.get(iri);

        if(items == null)
            return null;

        return items.columns();
    }


    /**
     * Reverse lookup: the cached IRI represented by the given class and columns, or null.
     *
     * @param iriClass the IRI class
     * @param columns the columns
     * @return reverse lookup: the cached IRI represented by the given class and columns, or null
     */
    public Iri getIri(ResourceClass iriClass, List<Column> columns)
    {
        CacheItem item = new CacheItem(iriClass, columns);

        for(Entry<Iri, CacheItem> entry : cache.entrySet())
            if(entry.getValue().equals(item))
                return entry.getKey();

        return null;
    }


    /**
     * Stores the class and columns of the IRI.
     *
     * @param iri the IRI
     * @param iriClass the IRI class
     * @param columns the columns
     */
    public void storeToCache(Iri iri, ResourceClass iriClass, List<Column> columns)
    {
        CacheItem item = new CacheItem(iriClass, columns);
        cache.put(iri, item);
    }
}
