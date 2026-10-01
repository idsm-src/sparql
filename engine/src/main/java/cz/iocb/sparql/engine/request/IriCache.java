package cz.iocb.sparql.engine.request;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.rdf.Iri;



/**
 * Cache of detected IRI classes and of the constant columns representing the IRIs, keyed by IRI. An IRI has one
 * detected class (the intersection of the user IRI classes it belongs to, see {@link Request#getIriClass}) but may be
 * represented in the columns of every class it belongs to, so the columns are kept per class. One instance is shared by
 * the configuration, another is private to each request.
 */
public class IriCache
{
    /**
     * Cached class of an IRI and its columns per class.
     */
    private static final class CacheItem
    {
        /**
         * Detected class of the IRI, or null when only columns are cached.
         */
        private ResourceClass iriClass;

        /**
         * Columns representing the IRI, by class.
         */
        private final Map<ResourceClass, List<Column>> columns = new HashMap<>();


        /**
         * Creates an entry without a detected class and without columns.
         */
        private CacheItem()
        {
        }
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
        CacheItem item = cache.get(iri);

        if(item == null)
            return null;

        return item.iriClass;
    }


    /**
     * Cached columns representing the IRI in the given class, or null.
     *
     * @param iri the IRI
     * @param resClass the class
     * @return cached columns representing the IRI in the given class, or null
     */
    public List<Column> getIriColumns(Iri iri, ResourceClass resClass)
    {
        CacheItem item = cache.get(iri);

        if(item == null)
            return null;

        return item.columns.get(resClass);
    }


    /**
     * Reverse lookup: the cached IRI represented by the given columns in the given class, or null.
     *
     * @param resClass the class
     * @param columns the columns
     * @return reverse lookup: the cached IRI represented by the given columns in the given class, or null
     */
    public Iri getIri(ResourceClass resClass, List<Column> columns)
    {
        for(Entry<Iri, CacheItem> entry : cache.entrySet())
            if(columns.equals(entry.getValue().columns.get(resClass)))
                return entry.getKey();

        return null;
    }


    /**
     * Stores the detected class of the IRI.
     *
     * @param iri the IRI
     * @param iriClass the detected class
     */
    public void storeClass(Iri iri, ResourceClass iriClass)
    {
        cache.computeIfAbsent(iri, _ -> new CacheItem()).iriClass = iriClass;
    }


    /**
     * Stores the detected class of the IRI together with the columns representing the IRI in it.
     *
     * @param iri the IRI
     * @param iriClass the detected class
     * @param columns the columns representing the IRI in the class
     */
    public void storeToCache(Iri iri, ResourceClass iriClass, List<Column> columns)
    {
        storeClass(iri, iriClass);
        storeColumns(iri, iriClass, columns);
    }


    /**
     * Stores the columns representing the IRI in the given class, one of the classes the IRI belongs to.
     *
     * @param iri the IRI
     * @param resClass the class
     * @param columns the columns representing the IRI in the class
     */
    public void storeColumns(Iri iri, ResourceClass resClass, List<Column> columns)
    {
        cache.computeIfAbsent(iri, _ -> new CacheItem()).columns.put(resClass, columns);
    }
}
