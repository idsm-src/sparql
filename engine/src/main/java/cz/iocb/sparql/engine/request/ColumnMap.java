package cz.iocb.sparql.engine.request;

import java.util.HashMap;
import java.util.Map;



/**
 * Shortens generated column names that exceed the PostgreSQL identifier limit, keeping the mapping stable within one
 * request so the same long name always gets the same short one.
 */
public class ColumnMap
{
    /**
     * Maximum identifier length accepted without shortening (PostgreSQL allows 63).
     */
    private static final int limit = 63;

    /**
     * Number of hexadecimal digits of the uniqueness suffix.
     */
    private static final int numpart = 8;

    /**
     * Shortened names by original name.
     */
    private Map<String, String> map = new HashMap<>();


    /**
     * Creates an empty map.
     */
    public ColumnMap()
    {
    }


    /**
     * The name itself if short enough, otherwise its unique shortened form.
     *
     * @param name the name
     * @return the name itself if short enough, otherwise its unique shortened form
     */
    public String getSafeName(String name)
    {
        if(name.length() < limit)
            return name;

        if(map.containsKey(name))
            return map.get(name);

        String shortName = name.substring(0, limit - numpart);

        for(int i = 0;; i++)
        {
            String safe = shortName + String.format("%0" + numpart + "X", i);

            if(!map.values().contains(safe))
            {
                map.put(name, safe);
                return safe;
            }
        }
    }
}
