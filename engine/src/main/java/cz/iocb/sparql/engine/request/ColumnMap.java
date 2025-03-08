package cz.iocb.sparql.engine.request;

import java.util.HashMap;
import java.util.Map;



public class ColumnMap
{
    private static final int limit = 63;
    private static final int numpart = 8;

    private Map<String, String> map = new HashMap<String, String>();


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
