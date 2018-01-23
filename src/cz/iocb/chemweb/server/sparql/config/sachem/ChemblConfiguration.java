package cz.iocb.chemweb.server.sparql.config.sachem;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;



public class ChemblConfiguration extends SachemConfiguration
{
    private static ChemblConfiguration singleton;


    public ChemblConfiguration() throws FileNotFoundException, IOException, SQLException
    {
        super("chembl", "http://rdf.ebi.ac.uk/resource/chembl/molecule/", "CHEMBL[0-9]+");
    }


    public static ChemblConfiguration get() throws FileNotFoundException, IOException, SQLException
    {
        if(singleton != null)
            return singleton;

        synchronized(ChemblConfiguration.class)
        {
            if(singleton != null)
                return singleton;

            singleton = new ChemblConfiguration();
        }

        return singleton;
    }
}
