package cz.iocb.chemweb.server.sparql.config.sachem;

import java.sql.SQLException;
import javax.sql.DataSource;
import cz.iocb.chemweb.server.sparql.database.DatabaseSchema;
import cz.iocb.chemweb.server.sparql.database.Table;



public class DrugbankSachemConfiguration extends SachemConfiguration
{
    public DrugbankSachemConfiguration(String service, DataSource connectionPool, DatabaseSchema schema)
            throws SQLException
    {
        super(service, connectionPool, schema, "drugbank", new Table("molecules", "drugbank"),
                "http://wifo5-04.informatik.uni-mannheim.de/drugbank/resource/drugs/", "DB", 5);
    }
}
