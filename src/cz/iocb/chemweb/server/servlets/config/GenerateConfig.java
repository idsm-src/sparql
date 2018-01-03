package cz.iocb.chemweb.server.servlets.config;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map.Entry;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import cz.iocb.chemweb.server.sparql.pubchem.PubChemConfiguration;



public class GenerateConfig extends HttpServlet
{
    private static final long serialVersionUID = 1L;



    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException
    {
        try
        {
            processRequest(req, res);
        }
        catch(SQLException e)
        {
            throw new IOException(e);
        }
    }


    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException
    {
        try
        {
            processRequest(req, res);
        }
        catch(SQLException e)
        {
            throw new IOException(e);
        }
    }


    protected void processRequest(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException, SQLException
    {
        ServletOutputStream out = res.getOutputStream();


        out.println("var prefixes = [");

        boolean first = true;

        for(Entry<String, String> prefix : PubChemConfiguration.get().getPrefixes().entrySet())
        {
            if(!first)
                out.println(",");
            else
                first = false;

            out.print("{ \"name\": \"");
            out.print(prefix.getKey());
            out.print(":\", \"iri\":\"");
            out.print(prefix.getValue());
            out.print("\"}");
        }

        out.println("];");
        out.println();
        out.println("var startIri = \"http://www.w3.org/2002/07/owl#Class\";");

        out.close();
    }
}
