package cz.iocb.chemweb.server.sparql.config.sachem;

import java.sql.SQLException;
import javax.sql.DataSource;



public class PubChemConfiguration extends SachemConfiguration
{
    private static final String schema = "pubchem";
    private static final String table = "compounds";


    public PubChemConfiguration(DataSource connectionPool) throws SQLException
    {
        super(connectionPool, schema, table, "http://rdf.ncbi.nlm.nih.gov/pubchem/compound/", "CID[1-9][0-9]*");
    }
}
