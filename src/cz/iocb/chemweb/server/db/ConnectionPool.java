package cz.iocb.chemweb.server.db;

import java.beans.PropertyVetoException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import cz.iocb.chemweb.server.Utils;
import virtuoso.jdbc4.VirtuosoConnectionPoolDataSource;



public class ConnectionPool
{
    private static VirtuosoConnectionPoolDataSource connectionPool = null;


    synchronized static private void initConnectionPool()
            throws FileNotFoundException, IOException, PropertyVetoException, NumberFormatException, SQLException
    {
        if(connectionPool != null)
            return;

        Properties properties = new Properties();
        properties.load(new FileInputStream(Utils.getConfigDirectory() + "/datasource.properties"));

        connectionPool = new VirtuosoConnectionPoolDataSource();

        //connectionPool.setDriverClass(properties.getProperty("driverClass"));
        //connectionPool.setJdbcUrl(properties.getProperty("jdbcUrl"));
        connectionPool.setUser(properties.getProperty("username"));
        connectionPool.setPassword(properties.getProperty("password"));
        connectionPool.setMaxPoolSize(new Integer(properties.getProperty("maxPoolSize")));

        //connectionPool.setInitialPoolSize(new Integer(properties.getProperty("initialPoolSize")));
        //connectionPool.setAcquireIncrement(new Integer(properties.getProperty("acquireIncrement")));
        //connectionPool.setMaxPoolSize(new Integer(properties.getProperty("maxPoolSize")));
        //connectionPool.setMinPoolSize(new Integer(properties.getProperty("minPoolSize")));
        //connectionPool.setMaxStatements(new Integer(properties.getProperty("maxStatements")));
    }


    public static Connection getConnection()
            throws FileNotFoundException, IOException, PropertyVetoException, SQLException
    {
        if(connectionPool == null)
            initConnectionPool();

        Connection connection = connectionPool.getConnection();
        connection.setAutoCommit(true);
        connection.setTransactionIsolation(java.sql.Connection.TRANSACTION_READ_COMMITTED);

        return connection;
    }
}
