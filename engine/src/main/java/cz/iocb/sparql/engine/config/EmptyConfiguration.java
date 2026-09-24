package cz.iocb.sparql.engine.config;

import java.sql.SQLException;
import javax.sql.DataSource;
import cz.iocb.sparql.engine.database.DatabaseSchema;



/**
 * Configuration with the built-in datatypes only and no mappings.
 */
public class EmptyConfiguration extends SparqlDatabaseConfiguration
{
    /**
     * Creates the configuration (JNDI-compatible constructor).
     *
     * @param service the service IRI
     * @param connectionPool the connection pool of the database
     * @param schema the database schema
     * @throws SQLException on database errors
     */
    public EmptyConfiguration(String service, DataSource connectionPool, DatabaseSchema schema) throws SQLException
    {
        super(service, connectionPool, schema);
    }
}
