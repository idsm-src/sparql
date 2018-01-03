package cz.iocb.chemweb.server.services.details;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import cz.iocb.chemweb.server.Utils;
import cz.iocb.chemweb.server.db.IriNode;
import cz.iocb.chemweb.server.velocity.NodeUtils;
import cz.iocb.chemweb.server.velocity.UrlDirective;
import cz.iocb.chemweb.shared.services.details.DetailsPageService;



public class DetailsPageServiceImpl extends RemoteServiceServlet implements DetailsPageService
{
    private static final long serialVersionUID = 1L;
    private static final Logger logger = Logger.getLogger(DetailsPageServiceImpl.class);

    //private final VelocityEngine ve;
    private final Properties properties;

    public DetailsPageServiceImpl() throws FileNotFoundException, IOException
    {
        /*Properties*/properties = new Properties();
        properties.load(new FileInputStream(Utils.getConfigDirectory() + "/velocity.properties"));
        properties.put("file.resource.loader.path", Utils.getConfigDirectory() + "/templates");

        //ve = new VelocityEngine(properties);
        //ve.init();
    }


    @Override
    public String details(String iriText)
    {
        try
        {
            long time = System.currentTimeMillis();
            URI uri = new URI(iriText);

            VelocityEngine ve = new VelocityEngine(properties);
            ve.init();

            StringWriter writer = new StringWriter();
            Template template = ve.getTemplate("detail.vm");

            VelocityContext context = new VelocityContext();
            context.put("urlContext", UrlDirective.Context.DETAILS);
            context.put("utils", NodeUtils.class);
            context.put("entity", new IriNode(uri.toString()));

            template.merge(context, writer);
            writer.close();

            time = System.currentTimeMillis() - time;
            logger.info(uri + " " + time / 1000.0 + "s");

            return writer.toString();
        }
        catch(URISyntaxException e)
        {
            // TODO:
            return "";
        }
        catch(IOException e)
        {
            // TODO:
            return "";
        }
    }
}
