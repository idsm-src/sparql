package cz.iocb.chemweb.server.sparql.config.empty;

import java.sql.SQLException;
import java.util.Properties;
import cz.iocb.chemweb.server.sparql.translator.SparqlDatabaseConfiguration;



public class EmptyConfiguration extends SparqlDatabaseConfiguration
{
    public EmptyConfiguration(Properties properties) throws SQLException
    {
        super(properties);
    }
}
