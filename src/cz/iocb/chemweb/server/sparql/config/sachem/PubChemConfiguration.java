package cz.iocb.chemweb.server.sparql.config.sachem;

import java.sql.SQLException;
import java.util.Properties;



public class PubChemConfiguration extends SachemConfiguration
{
    public PubChemConfiguration(Properties properties) throws SQLException
    {
        super(properties, "http://rdf.ncbi.nlm.nih.gov/pubchem/compound/", "CID[0-9]+");
    }
}
