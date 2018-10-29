package cz.iocb.chemweb.server.sparql.config.sachem;

import java.sql.SQLException;
import java.util.Properties;



public class ChemblConfiguration extends SachemConfiguration
{
    public ChemblConfiguration(Properties properties) throws SQLException
    {
        super(properties, "http://rdf.ebi.ac.uk/resource/chembl/molecule/", "CHEMBL[0-9]+");
    }
}
