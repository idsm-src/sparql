package cz.iocb.chemweb.server.sparql.config.sachem;

import java.sql.SQLException;
import javax.sql.DataSource;
import cz.iocb.chemweb.server.sparql.database.DatabaseSchema;
import cz.iocb.chemweb.server.sparql.database.Table;



public class ChebiSachemConfiguration extends SachemConfiguration
{
    public ChebiSachemConfiguration(String service, DataSource connectionPool, DatabaseSchema schema)
            throws SQLException
    {
        super(service, connectionPool, schema, "chebi", new Table("molecules", "chebi"),
                "http://purl.obolibrary.org/obo/CHEBI_", "", 0);
    }
}
