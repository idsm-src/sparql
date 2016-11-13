package cz.iocb.chemweb.server.db;

import java.beans.PropertyVetoException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import cz.iocb.chemweb.server.sparql.parser.model.Prefix;



public class Prefixes
{
    private static Collection<Prefix> prefixes = null;


    private static synchronized void initializePrefixes()
            throws FileNotFoundException, SQLException, IOException, PropertyVetoException
    {
        if(prefixes != null)
            return;

        Collection<Prefix> loadPrefixes = new ArrayList<Prefix>();

        try (Connection connection = ConnectionPool.getConnection())
        {
            try (Statement statement = connection.createStatement())
            {
                try (ResultSet result = statement.executeQuery("DB.DBA.XML_SELECT_ALL_NS_DECLS()"))
                {
                    while(result.next())
                    {
                        String name = result.getString(1);
                        String uri = result.getString(2);

                        Prefix prefix = new Prefix(name, uri);
                        loadPrefixes.add(prefix);
                    }
                }
            }
        }

        prefixes = loadPrefixes;
    }


    public static Collection<Prefix> getPrefixes()
            throws FileNotFoundException, SQLException, IOException, PropertyVetoException
    {
        if(prefixes == null)
            initializePrefixes();

        return prefixes;
    }
}
