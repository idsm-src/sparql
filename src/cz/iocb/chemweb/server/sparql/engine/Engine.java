package cz.iocb.chemweb.server.sparql.engine;

import cz.iocb.chemweb.server.sparql.config.SparqlDatabaseConfiguration;



public class Engine
{
    private final SparqlDatabaseConfiguration config;


    public Engine(SparqlDatabaseConfiguration config)
    {
        this.config = config;
    }


    public Request getRequest()
    {
        return new Request(config);
    }


    public SparqlDatabaseConfiguration getConfig()
    {
        return config;
    }
}
