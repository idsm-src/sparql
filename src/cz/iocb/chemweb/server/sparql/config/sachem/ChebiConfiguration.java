package cz.iocb.chemweb.server.sparql.config.sachem;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;



public class ChebiConfiguration extends SachemConfiguration
{
    private static ChebiConfiguration singleton;


    public ChebiConfiguration() throws FileNotFoundException, IOException, SQLException
    {
        super("chebi", "http://purl.obolibrary.org/obo/CHEBI_", "[0-9]+");
    }


    public static ChebiConfiguration get() throws FileNotFoundException, IOException, SQLException
    {
        if(singleton != null)
            return singleton;

        synchronized(ChebiConfiguration.class)
        {
            if(singleton != null)
                return singleton;

            singleton = new ChebiConfiguration();
        }

        return singleton;
    }
}
