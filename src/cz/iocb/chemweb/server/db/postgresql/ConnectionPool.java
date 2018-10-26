package cz.iocb.chemweb.server.db.postgresql;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;



public class ConnectionPool
{
    public DataSource pool = null;


    public ConnectionPool(Properties properties)
    {
        PoolProperties p = new PoolProperties();

        p.setDriverClassName("org.postgresql.Driver");
        p.setUrl("jdbc:postgresql://" + properties.getProperty("database"));
        p.setUsername(properties.getProperty("username"));
        p.setPassword(properties.getProperty("password"));

        int connections = Integer.parseInt(properties.getProperty("connections"));
        p.setInitialSize(connections);
        p.setMaxActive(connections);

        p.setMaxWait(-1);
        p.setTestOnBorrow(true);
        p.setValidationQuery("SELECT 1");

        p.setTestWhileIdle(false);
        p.setRemoveAbandoned(false);
        p.setTimeBetweenEvictionRunsMillis(0);

        DataSource datasource = new DataSource();
        datasource.setPoolProperties(p);

        pool = datasource;
    }


    public Connection getConnection() throws SQLException
    {
        return pool.getConnection();
    }
}
