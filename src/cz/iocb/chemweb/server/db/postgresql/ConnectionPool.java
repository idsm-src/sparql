package cz.iocb.chemweb.server.db.postgresql;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import org.postgresql.ds.PGPoolingDataSource;
import cz.iocb.chemweb.server.Utils;



public class ConnectionPool
{
    private static PGPoolingDataSource connectionPool = null;


    synchronized static private PGPoolingDataSource getConnectionPool() throws FileNotFoundException, IOException
    {
        if(connectionPool != null)
            return connectionPool;

        Properties properties = new Properties();
        properties.load(new FileInputStream(Utils.getConfigDirectory() + "/datasource-postgresql.properties"));

        PGPoolingDataSource pool = new PGPoolingDataSource();
        pool.setServerName("localhost");
        pool.setPortNumber(Integer.parseInt(properties.getProperty("port")));
        pool.setDatabaseName(properties.getProperty("database"));
        pool.setUser(properties.getProperty("username"));
        pool.setPassword(properties.getProperty("password"));
        pool.setSocketTimeout(Integer.parseInt(properties.getProperty("socketTimeout")));
        pool.setTcpKeepAlive(properties.getProperty("tcpKeepAlive").equals("true"));
        pool.setCompatible(properties.getProperty("assumeMinServerVersion"));
        pool.setMaxConnections(Integer.parseInt(properties.getProperty("maxConnections")));

        connectionPool = pool;
        return connectionPool;
    }


    public static Connection getConnection() throws FileNotFoundException, SQLException, IOException
    {
        return getConnectionPool().getConnection();
    }
}
