package cz.iocb.sparql.engine.request;

import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;



/**
 * Entry point of the engine for one configuration: creates {@link Request}s.
 */
public class Engine
{
    /**
     * Configuration of the endpoint.
     */
    private final SparqlDatabaseConfiguration config;


    /**
     * Creates the engine for the configuration.
     *
     * @param config the endpoint configuration
     */
    public Engine(SparqlDatabaseConfiguration config)
    {
        this.config = config;
    }


    /**
     * Creates a request; with {@code serviceReorder}, SERVICE calls may be evaluated independently of their context and
     * joined afterwards.
     *
     * @param serviceReorder whether SERVICE calls may be evaluated independently of their context
     * @return the new request
     */
    public Request getRequest(boolean serviceReorder)
    {
        return new Request(config, serviceReorder);
    }


    /**
     * Creates a request without service reordering.
     *
     * @return the new request
     */
    public Request getRequest()
    {
        return new Request(config, false);
    }


    /**
     * Configuration of the endpoint.
     *
     * @return configuration of the endpoint
     */
    public SparqlDatabaseConfiguration getConfig()
    {
        return config;
    }
}
