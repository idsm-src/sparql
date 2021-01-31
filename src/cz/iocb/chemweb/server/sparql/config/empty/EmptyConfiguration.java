package cz.iocb.chemweb.server.sparql.config.empty;

import java.sql.SQLException;
import javax.sql.DataSource;
import cz.iocb.chemweb.server.sparql.config.SparqlDatabaseConfiguration;
import cz.iocb.chemweb.server.sparql.database.DatabaseSchema;



public class EmptyConfiguration extends SparqlDatabaseConfiguration
{
    public EmptyConfiguration(String service, DataSource connectionPool, DatabaseSchema schema) throws SQLException
    {
        super(service, connectionPool, schema);
    }
}
