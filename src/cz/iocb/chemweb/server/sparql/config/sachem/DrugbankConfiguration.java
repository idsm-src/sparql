package cz.iocb.chemweb.server.sparql.config.sachem;

import java.sql.SQLException;
import java.util.Properties;



public class DrugbankConfiguration extends SachemConfiguration
{
    public DrugbankConfiguration(Properties properties) throws SQLException
    {
        super(properties, "http://wifo5-04.informatik.uni-mannheim.de/drugbank/resource/drugs/", "DB[0-9]+");
    }
}
