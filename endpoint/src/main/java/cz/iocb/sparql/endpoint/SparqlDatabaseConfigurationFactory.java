package cz.iocb.sparql.endpoint;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Set;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import cz.iocb.sparql.engine.config.EngineSettings;
import cz.iocb.sparql.engine.config.ServiceSettings;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.DatabaseSchema;



/**
 * JNDI object factory creating the deployment's {@link SparqlDatabaseConfiguration}: the class named by the reference
 * is instantiated through its {@code (String service, DataSource, DatabaseSchema)} constructor, with the data source
 * looked up by the {@code datasource} address and the optional {@code service} IRI address of the reference. The other
 * addresses of the reference, the attributes of the resource in {@code context.xml}, set the settings of the
 * configuration over what the configuration sets itself (see {@link #configure}): an address named by a setting of the
 * engine sets that setting (see {@link EngineSettings#with(String, String)}), like {@code join-optimizer},
 * {@code statistics-check-interval} or {@code lookup-cost}, and an address named by a setting of the calls of remote
 * SERVICE endpoints prefixed by {@code service-} sets the default of that setting (see
 * {@link ServiceSettings#with(String, String)}), like {@code service-context-limit}, as in
 *
 * <pre>
 * &lt;Resource name="sparql:example" auth="Container" type="org.example.ExampleConfiguration"
 *     factory="cz.iocb.sparql.endpoint.SparqlDatabaseConfigurationFactory" datasource="jdbc:example"
 *     join-optimizer="advice" statistics-check-interval="5min" service-context-limit="5000" /&gt;
 * </pre>
 */
public class SparqlDatabaseConfigurationFactory implements ObjectFactory
{
    /**
     * The prefix of the names of the addresses that set the defaults of the settings of the calls of remote SERVICE
     * endpoints.
     */
    static final String SERVICE_PREFIX = "service-";

    /**
     * Addresses of the reference that are no settings: those this factory reads and those Tomcat gives every resource.
     */
    private static final Set<String> addresses = Set.of("datasource", "service", "factory", "auth", "scope",
            "singleton", "description");

    /**
     * Logger of the addresses that set nothing.
     */
    private static final Logger logger = LoggerFactory.getLogger(SparqlDatabaseConfigurationFactory.class);


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

            SparqlDatabaseConfiguration config = (SparqlDatabaseConfiguration) constructor.newInstance(
                    service == null ? null : (String) service.getContent(), connectionPool,
                    new DatabaseSchema(connectionPool));

            configure(config, ref);

            return config;
        }
        catch(SQLException e)
        {
            throw new InvocationTargetException(e);
        }
    }


    /**
     * Sets the settings that the addresses of the reference give to the configuration, over what the configuration sets
     * itself: an address named by a setting of the engine sets that setting (see
     * {@link EngineSettings#with(String, String)}), and an address named by a setting of the calls of remote SERVICE
     * endpoints prefixed by {@link #SERVICE_PREFIX} sets the default of that setting (see
     * {@link ServiceSettings#with(String, String)}). An address that is neither, nor one that is no setting (see
     * {@link #addresses}), sets nothing, which is logged as a warning, as its name is likely misspelt.
     *
     * @param config the configuration
     * @param reference the reference
     * @throws IllegalArgumentException if an address gives no value of its setting
     */
    static void configure(SparqlDatabaseConfiguration config, Reference reference)
    {
        EngineSettings settings = config.getEngineSettings();
        ServiceSettings serviceSettings = config.getServiceSettings();

        for(Enumeration<RefAddr> all = reference.getAll(); all.hasMoreElements();)
        {
            RefAddr address = all.nextElement();
            String name = address.getType();

            if(!(address.getContent() instanceof String value))
                continue;

            String serviceName = name.startsWith(SERVICE_PREFIX) ? name.substring(SERVICE_PREFIX.length()) : null;

            if(EngineSettings.NAMES.contains(name))
                settings = settings.with(name, value);
            else if(serviceName != null && ServiceSettings.NAMES.contains(serviceName))
                serviceSettings = serviceSettings.with(serviceName, value);
            else if(!addresses.contains(name))
                logger.warn("the attribute {} of the configuration {} is no setting and sets nothing", name,
                        reference.getClassName());
        }

        config.setEngineSettings(settings);
        config.setServiceSettings(serviceSettings);
    }
}
