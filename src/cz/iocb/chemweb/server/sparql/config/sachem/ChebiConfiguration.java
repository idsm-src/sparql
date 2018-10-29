package cz.iocb.chemweb.server.sparql.config.sachem;

import java.sql.SQLException;
import java.util.Properties;



public class ChebiConfiguration extends SachemConfiguration
{
    public ChebiConfiguration(Properties properties) throws SQLException
    {
        super(properties, "http://purl.obolibrary.org/obo/CHEBI_", "[0-9]+");
    }
}
