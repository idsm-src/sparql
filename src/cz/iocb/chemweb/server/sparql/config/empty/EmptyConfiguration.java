package cz.iocb.chemweb.server.sparql.config.empty;

import java.sql.SQLException;
import javax.sql.DataSource;
import cz.iocb.chemweb.server.sparql.translator.SparqlDatabaseConfiguration;



public class EmptyConfiguration extends SparqlDatabaseConfiguration
{
    public EmptyConfiguration(DataSource connectionPool) throws SQLException
    {
        super(connectionPool);
    }
}
