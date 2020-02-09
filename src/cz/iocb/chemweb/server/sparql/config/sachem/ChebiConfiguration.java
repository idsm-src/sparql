package cz.iocb.chemweb.server.sparql.config.sachem;

import java.sql.SQLException;
import javax.sql.DataSource;



public class ChebiConfiguration extends SachemConfiguration
{
    private static final String schema = "chebi";
    private static final String table = "compounds";


    public ChebiConfiguration(DataSource connectionPool) throws SQLException
    {
        super(connectionPool, schema, table, "http://purl.obolibrary.org/obo/CHEBI_", "[1-9][0-9]*");
    }
}
