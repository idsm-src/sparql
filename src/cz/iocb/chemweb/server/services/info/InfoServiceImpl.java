package cz.iocb.chemweb.server.services.info;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.sql.DataSource;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import cz.iocb.chemweb.shared.services.DatabaseException;
import cz.iocb.chemweb.shared.services.info.CountItem;
import cz.iocb.chemweb.shared.services.info.InfoService;
import cz.iocb.chemweb.shared.services.info.SourceItem;



public class InfoServiceImpl extends RemoteServiceServlet implements InfoService
{
    private static final long serialVersionUID = 1L;

    private DataSource connectionPool;
    List<CountItem> counts;
    List<SourceItem> sources;


    @Override
    public void init(ServletConfig config) throws ServletException
    {
        String resourceName = config.getInitParameter("resource");

        if(resourceName == null || resourceName.isEmpty())
            throw new ServletException("Resource name is not set");

        try
        {
            Context context = (Context) (new InitialContext()).lookup("java:comp/env");
            connectionPool = (DataSource) context.lookup(resourceName);
        }
        catch(NamingException e)
        {
            throw new ServletException(e);
        }

        super.init(config);
    }


    public List<CountItem> getCounts() throws DatabaseException
    {
        if(counts != null)
            return counts;

        synchronized(this)
        {
            if(counts != null)
                return counts;

            List<CountItem> list = new ArrayList<CountItem>();

            try(Connection connection = connectionPool.getConnection())
            {
                try(Statement statement = connection.createStatement())
                {
                    try(ResultSet result = statement.executeQuery("select count(*) from pubchem.substance_bases"))
                    {
                        if(result.next())
                            list.add(new CountItem("PubChem Substances", result.getLong(1)));
                    }

                    try(ResultSet result = statement
                            .executeQuery("select count(*) from pubchem.compound_bases where keep"))
                    {
                        if(result.next())
                            list.add(new CountItem("PubChem Compounds", result.getLong(1)));
                    }

                    try(ResultSet result = statement.executeQuery("select count(*) from pubchem.bioassay_bases"))
                    {
                        if(result.next())
                            list.add(new CountItem("PubChem BioAssays", result.getLong(1)));
                    }

                    try(ResultSet result = statement.executeQuery("select count(*) from chembl.molecule_dictionary"))
                    {
                        if(result.next())
                            list.add(new CountItem("ChEMBL Substances", result.getLong(1)));
                    }

                    try(ResultSet result = statement.executeQuery("select count(*) from chembl.assays"))
                    {
                        if(result.next())
                            list.add(new CountItem("ChEMBL Assays", result.getLong(1)));
                    }

                    try(ResultSet result = statement.executeQuery("select count(*) from chebi.classes"))
                    {
                        if(result.next())
                            list.add(new CountItem("ChEBI Entities", result.getLong(1)));
                    }
                }
            }
            catch(SQLException e)
            {
                throw new DatabaseException(e);
            }

            counts = list;

            return counts;
        }
    }


    public List<SourceItem> getSources() throws DatabaseException
    {
        if(sources != null)
            return sources;

        synchronized(this)
        {
            if(sources != null)
                return sources;

            List<SourceItem> list = new ArrayList<SourceItem>();

            try(Connection connection = connectionPool.getConnection())
            {
                try(Statement statement = connection.createStatement())
                {
                    try(ResultSet result = statement
                            .executeQuery("select url, name, version from info.sources order by id"))
                    {
                        while(result.next())
                        {
                            list.add(new SourceItem(result.getString(1), result.getString(2), result.getString(3)));
                        }
                    }
                }
            }
            catch(SQLException e)
            {
                throw new DatabaseException(e);
            }

            sources = list;

            return sources;
        }
    }
}
