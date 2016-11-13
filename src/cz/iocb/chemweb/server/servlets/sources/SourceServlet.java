package cz.iocb.chemweb.server.servlets.sources;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import cz.iocb.chemweb.server.Utils;



public class SourceServlet extends HttpServlet
{
    private static final long serialVersionUID = 1L;
    private final String dataDirectory;


    public SourceServlet() throws FileNotFoundException, IOException
    {
        Properties properties = new Properties();
        properties.load(new FileInputStream(Utils.getConfigDirectory() + "/sources.properties"));

        dataDirectory = properties.getProperty("directory");
    }


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException
    {
        processRequest(req, res);
    }


    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException
    {
        processRequest(req, res);
    }


    protected void processRequest(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException
    {
        String id = req.getParameter("id");

        if(id == null || !id.matches("^[0-9]+$"))
            throw new ServletException("an invalid value of the 'id' argument");


        String size = req.getParameter("w");

        if(size == null || !size.matches("^(80)|(200)$"))
            throw new ServletException("an invalid value of the 'w' argument");


        res.setContentType("image/png");

        FileInputStream in = new FileInputStream(dataDirectory + "/chebi/img/w" + size + "/CHEBI:" + id + ".png");
        ServletOutputStream out = res.getOutputStream();
        IOUtils.copy(in, out);

        in.close();
        out.close();
    }


    @Override
    public String getServletInfo()
    {
        return "Source Servlet";
    }
}
