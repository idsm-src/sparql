package cz.iocb.chemweb.server.sparql.config.sachem;

import java.sql.SQLException;
import javax.sql.DataSource;



public class PubChemConfiguration extends SachemConfiguration
{
    public PubChemConfiguration(DataSource connectionPool) throws SQLException
    {
        super(connectionPool, "http://rdf.ncbi.nlm.nih.gov/pubchem/compound/", "CID[0-9]+");
    }
}
