package cz.iocb.chemweb.server;



public class Utils
{
    public static String getConfigDirectory()
    {
        String path = System.getProperty("catalina.base");

        if(path == null)
            path = "/home/galgonek/workspace/chemweb/war";
        else
            path = path + "/webapps/chemweb";

        return path;
    }
}
