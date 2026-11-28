package cz.iocb.sparql.endpoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.sql.SQLException;
import java.time.Duration;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.config.EngineSettings;
import cz.iocb.sparql.engine.config.EngineSettings.JoinOptimizerMode;
import cz.iocb.sparql.engine.config.ServiceSettings;
import cz.iocb.sparql.engine.config.ServiceSettings.ContextOverflow;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.Function;



/**
 * The settings of a configuration set by the attributes of its resource in {@code context.xml}: an attribute named by a
 * setting of the engine sets that setting, an attribute named by a setting of the calls of remote SERVICE endpoints
 * prefixed by {@code service-} sets the default of that setting, both over what the configuration sets itself, the
 * attributes that are no settings set nothing, and a text giving no value of its setting is refused, which leaves the
 * settings of the configuration as they were.
 */
public class SparqlDatabaseConfigurationFactoryTest
{
    /**
     * Creates the reference of a resource with the given attributes, as Tomcat creates it.
     *
     * @param attributes the names and the values of the attributes, in turn
     * @return the reference
     */
    private static Reference reference(String... attributes)
    {
        Reference reference = new Reference("org.example.ExampleConfiguration",
                SparqlDatabaseConfigurationFactory.class.getName(), null);

        for(int i = 0; i < attributes.length; i += 2)
            reference.add(new StringRefAddr(attributes[i], attributes[i + 1]));

        return reference;
    }


    @Test
    public void settings() throws SQLException
    {
        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, null, null, false);

        // what the configuration sets itself
        config.setEngineSettings(EngineSettings.DEFAULT.withLookupCost(4).withHashCost(3));
        config.setServiceSettings(ServiceSettings.DEFAULT.withResultLimit(5).withContextLimit(10));

        SparqlDatabaseConfigurationFactory.configure(config,
                reference("factory", SparqlDatabaseConfigurationFactory.class.getName(), "auth", "Container",
                        "singleton", "true", "description", "an example", "datasource", "jdbc/example", "service",
                        "http://example.org/sparql", "join-optimizer", "advice", "statistics-function",
                        "idsm.extended_statistics", "statistics-check-interval", "5min", "hash-cost", "2",
                        "service-reorder", "true", "service-context-limit", "5000", "service-context-overflow",
                        "independent"));

        EngineSettings settings = config.getEngineSettings();

        assertEquals(JoinOptimizerMode.ADVICE, settings.joinOptimizer());
        assertEquals(new Function("idsm", "extended_statistics"), settings.statisticsFunction());
        assertEquals(Duration.ofMinutes(5), settings.statisticsCheckInterval());
        assertEquals(2, settings.hashCost());
        assertTrue(settings.serviceReorder());

        // the settings no attribute sets stay as the configuration sets them
        assertEquals(4, settings.lookupCost());
        assertEquals(EngineSettings.DEFAULT.disjunctLimit(), settings.disjunctLimit());

        assertEquals(ServiceSettings.DEFAULT.withResultLimit(5).withContextLimit(5000)
                .withContextOverflow(ContextOverflow.INDEPENDENT), config.getServiceSettings());
    }


    @Test
    public void names()
    {
        // no setting of the engine is named as a setting of the calls with the prefix
        for(String name : ServiceSettings.NAMES)
            assertFalse(EngineSettings.NAMES.contains(SparqlDatabaseConfigurationFactory.SERVICE_PREFIX + name), name);
    }


    @Test
    public void ignored() throws SQLException
    {
        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, null, null, false);

        // an attribute that is no setting sets nothing, a setting of the calls without the prefix neither
        SparqlDatabaseConfigurationFactory.configure(config,
                reference("lookup-costs", "4", "service-", "1", "service-lookup-cost", "4", "context-limit", "5"));

        assertSame(EngineSettings.DEFAULT, config.getEngineSettings());
        assertEquals(ServiceSettings.DEFAULT, config.getServiceSettings());
    }


    @Test
    public void refused() throws SQLException
    {
        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, null, null, false);

        // a text giving no value of its setting is refused by a message naming the setting
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> SparqlDatabaseConfigurationFactory.configure(config,
                        reference("hash-cost", "2", "lookup-cost", "cheap")));

        assertTrue(e.getMessage().startsWith("lookup-cost "), e.getMessage());

        e = assertThrows(IllegalArgumentException.class, () -> SparqlDatabaseConfigurationFactory.configure(config,
                reference("service-result-limit", "5", "service-context-limit", "0")));

        assertTrue(e.getMessage().startsWith("context-limit "), e.getMessage());

        // and the settings stay as they were
        assertSame(EngineSettings.DEFAULT, config.getEngineSettings());
        assertEquals(ServiceSettings.DEFAULT, config.getServiceSettings());
    }
}
