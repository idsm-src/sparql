package cz.iocb.chemweb.server.sparql.config.sachem;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;



public class DrugbankConfiguration extends SachemConfiguration
{
    private static DrugbankConfiguration singleton;


    public DrugbankConfiguration() throws FileNotFoundException, IOException, SQLException
    {
        super("drugbank", "http://wifo5-04.informatik.uni-mannheim.de/drugbank/resource/drugs/", "DB[0-9]+");
    }


    public static DrugbankConfiguration get() throws FileNotFoundException, IOException, SQLException
    {
        if(singleton != null)
            return singleton;

        synchronized(DrugbankConfiguration.class)
        {
            if(singleton != null)
                return singleton;

            singleton = new DrugbankConfiguration();
        }

        return singleton;
    }
}
