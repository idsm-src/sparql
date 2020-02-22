package cz.iocb.chemweb.server.sparql.config.sachem;

import java.sql.SQLException;
import javax.sql.DataSource;



public class DrugbankConfiguration extends SachemConfiguration
{
    public DrugbankConfiguration(DataSource connectionPool) throws SQLException
    {
        super(connectionPool, "drugbank", "drugbank", "compounds",
                "http://wifo5-04.informatik.uni-mannheim.de/drugbank/resource/drugs/", "DB[0-9]{5}");
    }
}
