package cz.iocb.sparql.engine.request;

import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;



public class Engine
{
    private final SparqlDatabaseConfiguration config;


    public Engine(SparqlDatabaseConfiguration config)
    {
        this.config = config;
    }


    public Request getRequest(boolean serviceReorder)
    {
        return new Request(config, serviceReorder);
    }


    public Request getRequest()
    {
        return new Request(config, false);
    }


    public SparqlDatabaseConfiguration getConfig()
    {
        return config;
    }
}
