package cz.iocb.chemweb.server;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;



public class Utils implements ServletContextListener
{
    private static String path;


    @Override
    public void contextInitialized(ServletContextEvent sce)
    {
        path = sce.getServletContext().getRealPath("/");
    }


    @Override
    public void contextDestroyed(ServletContextEvent sce)
    {
    }


    public static String getConfigDirectory()
    {
        return path;
    }
}
