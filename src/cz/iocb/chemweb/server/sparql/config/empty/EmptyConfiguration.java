package cz.iocb.chemweb.server.sparql.config.empty;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;
import cz.iocb.chemweb.server.Utils;
import cz.iocb.chemweb.server.db.postgresql.ConnectionPool;
import cz.iocb.chemweb.server.db.postgresql.PostgresSchema;
import cz.iocb.chemweb.server.sparql.translator.SparqlDatabaseConfiguration;



public class EmptyConfiguration extends SparqlDatabaseConfiguration
{
    private static EmptyConfiguration singleton;


    public EmptyConfiguration() throws FileNotFoundException, IOException, SQLException
    {
        Properties properties = new Properties();
        properties.load(new FileInputStream(Utils.getConfigDirectory() + "/datasource-empty.properties"));

        connectionPool = new ConnectionPool(properties);
        schema = new PostgresSchema(connectionPool);
    }


    public static EmptyConfiguration get() throws FileNotFoundException, IOException, SQLException
    {
        if(singleton != null)
            return singleton;

        synchronized(EmptyConfiguration.class)
        {
            if(singleton != null)
                return singleton;

            singleton = new EmptyConfiguration();
        }

        return singleton;
    }
}
