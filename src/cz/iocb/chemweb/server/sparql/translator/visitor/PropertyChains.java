package cz.iocb.chemweb.server.sparql.translator.visitor;

import java.beans.PropertyVetoException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import cz.iocb.chemweb.shared.services.DatabaseException;



// FIXME: remake
public class PropertyChains
{
    private static Map<String, List<String>> propertyChains;


    public static Map<String, List<String>> get()
            throws FileNotFoundException, IOException, PropertyVetoException, DatabaseException
    {
        if(propertyChains != null)
            return propertyChains;

        synchronized(PropertyChains.class)
        {
            //System.out.println("load property chains ...");

            if(propertyChains != null)
                return propertyChains;

            Map<String, List<String>> propertyChainsLoaded = new HashMap<String, List<String>>();

            /*
            String query = "sparql SELECT ?CHEBIX ?LIST WHERE { ?CHEBIX owl:propertyChainAxiom ?LIST. }";
            
            VirtuosoDatabase database = new VirtuosoDatabase();
            Result result = database.query(query);
            
            for(Row row : result)
            {
                RdfNode chebix = row.get("CHEBIX");
                RdfNode list = row.get("LIST");
            
                List<String> chain = new LinkedList<String>();
            
                while(!list.getValue().equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#nil"))
                {
                    String subquery = "sparql SELECT ?FIRST ?REST WHERE { " + list.toString()
                            + " rdf:first ?FIRST; rdf:rest ?REST. }";
                    Result subresult = database.query(subquery);
            
                    RdfNode first = subresult.get("FIRST");
                    RdfNode rest = subresult.get("REST");
            
                    chain.add(first.toString());
                    list = rest;
                }
            
                propertyChainsLoaded.put(chebix.toString(), chain);
            }
            */

            propertyChains = propertyChainsLoaded;
            return propertyChains;
        }
    }
}
