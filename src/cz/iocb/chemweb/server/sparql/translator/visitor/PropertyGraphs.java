package cz.iocb.chemweb.server.sparql.translator.visitor;

import java.beans.PropertyVetoException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import cz.iocb.chemweb.shared.services.DatabaseException;



public class PropertyGraphs
{
    private static Map<String, String> propertyGraphs;

    public static Map<String, String> get()
            throws FileNotFoundException, IOException, PropertyVetoException, DatabaseException
    {
        if(propertyGraphs != null)
            return propertyGraphs;


        synchronized(PropertyGraphs.class)
        {
            if(propertyGraphs != null)
                return propertyGraphs;

            Map<String, String> propertyGraphsLoaded = new HashMap<String, String>();

            /* TODO: use other way
            String query = "SELECT property, max(graph) as graph FROM property_graphs GROUP BY property HAVING (count(*) = 1)";

            VirtuosoDatabase database = new VirtuosoDatabase();
            Result result = database.query(query);

            for(Row row : result)
            {
                RdfNode property = row.get("property");
                RdfNode graph = row.get("graph");

                System.out.println(property.toString() + " -> " + graph.toString());
                propertyGraphsLoaded.put(property.toString(), graph.toString());
            }
            */

            propertyGraphs = propertyGraphsLoaded;
            return propertyGraphs;
        }
    }
}
