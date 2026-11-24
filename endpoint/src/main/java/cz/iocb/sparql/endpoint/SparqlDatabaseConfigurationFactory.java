package cz.iocb.sparql.endpoint;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;
import javax.sql.DataSource;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.DatabaseSchema;



/**
 * JNDI object factory creating the deployment's {@link SparqlDatabaseConfiguration}: the class named by the reference
 * is instantiated through its {@code (String service, DataSource, DatabaseSchema)} constructor, with the data source
 * looked up by the {@code datasource} address and the optional {@code service} IRI address of the reference. The
 * optional {@code join-optimizer} address overrides what the configuration says about the join optimizer of the engine:
 * {@code off} turns it off, {@code settings} turns it on with its joins kept by the settings of the planner, and
 * {@code advice} turns it on with its joins kept by plan advice (see
 * {@link SparqlDatabaseConfiguration#setPlanAdvice}).
 */
public class SparqlDatabaseConfigurationFactory implements ObjectFactory
{
    /**
     * Creates the factory; instantiated by the JNDI provider.
     */
    public SparqlDatabaseConfigurationFactory()
    {
    }

    @Override
    public Object getObjectInstance(Object object, Name name, Context context, Hashtable<?, ?> environment)
            throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException, NamingException
    {
        if(!(object instanceof Reference))
            return null;

        Reference ref = (Reference) object;
        RefAddr datasource = ref.get("datasource");
        RefAddr service = ref.get("service");
        RefAddr optimizer = ref.get("join-optimizer");

        if(datasource == null)
            return null;

        Context contextX = (Context) (new InitialContext()).lookup("java:comp/env");
        DataSource connectionPool = (DataSource) contextX.lookup((String) datasource.getContent());

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String className = ref.getClassName();

        try
        {
            Class<?> objectClass = classLoader != null ? classLoader.loadClass(className) : Class.forName(className);
            Constructor<?> constructor = objectClass.getConstructor(String.class, DataSource.class,
                    DatabaseSchema.class);

            Object config = constructor.newInstance(service == null ? null : (String) service.getContent(),
                    connectionPool, new DatabaseSchema(connectionPool));

            if(optimizer != null)
                setJoinOptimizer((SparqlDatabaseConfiguration) config, (String) optimizer.getContent());

            return config;
        }
        catch(SQLException e)
        {
            throw new InvocationTargetException(e);
        }
    }


    /**
     * Sets the join optimizer of the configuration as the {@code join-optimizer} address of the reference says.
     *
     * @param config the configuration
     * @param value the content of the address: {@code off}, {@code settings} or {@code advice}
     * @throws IllegalArgumentException if the value is none of them
     */
    private static void setJoinOptimizer(SparqlDatabaseConfiguration config, String value)
    {
        switch(value)
        {
            case "off" -> config.setJoinOptimizer(false);
            case "settings", "advice" ->
            {
                config.setJoinOptimizer(true);
                config.setPlanAdvice(value.equals("advice"));
            }
            default -> throw new IllegalArgumentException("unknown join-optimizer value: " + value);
        }
    }
}
