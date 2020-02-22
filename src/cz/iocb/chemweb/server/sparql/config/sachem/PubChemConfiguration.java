package cz.iocb.chemweb.server.sparql.config.sachem;

import java.sql.SQLException;
import javax.sql.DataSource;



public class PubChemConfiguration extends SachemConfiguration
{
    public PubChemConfiguration(DataSource connectionPool) throws SQLException
    {
        super(connectionPool, "pubchem", "pubchem", "compounds", "http://rdf.ncbi.nlm.nih.gov/pubchem/compound/",
                "CID[1-9][0-9]*");
    }
}
