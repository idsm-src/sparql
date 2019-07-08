package cz.iocb.chemweb.server.sparql.engine;

import javax.net.ssl.SSLContext;
import cz.iocb.chemweb.server.sparql.config.SparqlDatabaseConfiguration;



public class Engine
{
    private final SparqlDatabaseConfiguration config;
    private final SSLContext sslContext;


    public Engine(SparqlDatabaseConfiguration config, SSLContext sslContext)
    {
        this.config = config;
        this.sslContext = sslContext;
    }


    public Request getRequest()
    {
        return new Request(config, sslContext);
    }
}
