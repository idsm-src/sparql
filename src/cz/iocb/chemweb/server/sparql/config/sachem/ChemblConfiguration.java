package cz.iocb.chemweb.server.sparql.config.sachem;

import java.sql.SQLException;
import javax.sql.DataSource;



public class ChemblConfiguration extends SachemConfiguration
{
    private static final String schema = "chembl";
    private static final String table = "compounds";


    public ChemblConfiguration(DataSource connectionPool) throws SQLException
    {
        super(connectionPool, schema, table, "http://rdf.ebi.ac.uk/resource/chembl/molecule/", "CHEMBL[1-9][0-9]*");
    }
}
