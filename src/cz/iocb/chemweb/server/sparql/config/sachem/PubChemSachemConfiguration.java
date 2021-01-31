package cz.iocb.chemweb.server.sparql.config.sachem;

import java.sql.SQLException;
import javax.sql.DataSource;
import cz.iocb.chemweb.server.sparql.database.DatabaseSchema;
import cz.iocb.chemweb.server.sparql.database.Table;



public class PubChemSachemConfiguration extends SachemConfiguration
{
    public PubChemSachemConfiguration(String service, DataSource connectionPool, DatabaseSchema schema)
            throws SQLException
    {
        super(service, connectionPool, schema, "pubchem", new Table("molecules", "pubchem"),
                "http://rdf.ncbi.nlm.nih.gov/pubchem/compound/", "CID", 0);
    }
}
