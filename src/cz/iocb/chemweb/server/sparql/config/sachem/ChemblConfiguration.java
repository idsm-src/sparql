package cz.iocb.chemweb.server.sparql.config.sachem;

import java.sql.SQLException;
import javax.sql.DataSource;



public class ChemblConfiguration extends SachemConfiguration
{
    public ChemblConfiguration(DataSource connectionPool) throws SQLException
    {
        super(connectionPool, "chembl", "molecules", "chembl", "http://rdf.ebi.ac.uk/resource/chembl/molecule/",
                "CHEMBL", 0);
    }
}
