package cz.iocb.chemweb.server.sparql.config.sachem;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;



public class PubChemConfiguration extends SachemConfiguration
{
    private static PubChemConfiguration singleton;


    public PubChemConfiguration() throws FileNotFoundException, IOException, SQLException
    {
        super("pubchem", "http://rdf.ncbi.nlm.nih.gov/pubchem/compound/", "CID[0-9]+");
    }


    public static PubChemConfiguration get() throws FileNotFoundException, IOException, SQLException
    {
        if(singleton != null)
            return singleton;

        synchronized(PubChemConfiguration.class)
        {
            if(singleton != null)
                return singleton;

            singleton = new PubChemConfiguration();
        }

        return singleton;
    }
}
