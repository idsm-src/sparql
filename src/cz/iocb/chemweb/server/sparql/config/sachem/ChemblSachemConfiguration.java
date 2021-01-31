package cz.iocb.chemweb.server.sparql.config.sachem;

import java.sql.SQLException;
import javax.sql.DataSource;
import cz.iocb.chemweb.server.sparql.database.DatabaseSchema;
import cz.iocb.chemweb.server.sparql.database.Table;



public class ChemblSachemConfiguration extends SachemConfiguration
{
    public ChemblSachemConfiguration(String service, DataSource connectionPool, DatabaseSchema schema)
            throws SQLException
    {
        super(service, connectionPool, schema, "chembl", new Table("molecules", "chembl"),
                "http://rdf.ebi.ac.uk/resource/chembl/molecule/", "CHEMBL", 0);
    }
}
