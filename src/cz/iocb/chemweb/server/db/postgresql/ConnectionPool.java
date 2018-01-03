package cz.iocb.chemweb.server.db.postgresql;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import org.postgresql.ds.PGPoolingDataSource;



@SuppressWarnings("deprecation")
public class ConnectionPool
{
    private PGPoolingDataSource pool = null;


    public ConnectionPool(Properties properties)
    {
        pool = new PGPoolingDataSource();
        pool.setServerName(properties.getProperty("host"));
        pool.setPortNumber(Integer.parseInt(properties.getProperty("port")));
        pool.setDatabaseName(properties.getProperty("database"));
        pool.setUser(properties.getProperty("username"));
        pool.setPassword(properties.getProperty("password"));
        pool.setSocketTimeout(Integer.parseInt(properties.getProperty("socketTimeout")));
        pool.setTcpKeepAlive(properties.getProperty("tcpKeepAlive").equals("true"));
        pool.setAssumeMinServerVersion(properties.getProperty("assumeMinServerVersion"));
        pool.setMaxConnections(Integer.parseInt(properties.getProperty("maxConnections")));
    }


    public Connection getConnection() throws SQLException
    {
        return pool.getConnection();
    }
}
