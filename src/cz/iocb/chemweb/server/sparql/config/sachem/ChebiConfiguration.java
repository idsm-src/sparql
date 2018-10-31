package cz.iocb.chemweb.server.sparql.config.sachem;

import java.sql.SQLException;
import javax.sql.DataSource;



public class ChebiConfiguration extends SachemConfiguration
{
    public ChebiConfiguration(DataSource connectionPool) throws SQLException
    {
        super(connectionPool, "http://purl.obolibrary.org/obo/CHEBI_", "[0-9]+");
    }
}
