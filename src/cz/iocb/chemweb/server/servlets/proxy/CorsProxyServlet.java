package cz.iocb.chemweb.server.servlets.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;



@SuppressWarnings("serial")
public class CorsProxyServlet extends HttpServlet
{
    private static final List<String> specialParameters = new ArrayList<>(
            Arrays.asList("endpoint", "requestMethod", "method"));


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException
    {
        res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }


    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException
    {
        if(!req.getContentType().matches("application/x-www-form-urlencoded.*"))
        {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        String accept = req.getHeader("accept");
        String endpoint = req.getParameter("endpoint");
        String method = req.getParameter("method");

        if(method == null)
            method = req.getParameter("requestMethod");

        if(endpoint == null || method == null || !method.matches("GET|POST"))
        {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }


        String query = "";

        for(Entry<String, String[]> item : req.getParameterMap().entrySet())
            for(String value : item.getValue())
                if(!specialParameters.contains(item.getKey()))
                    query += (query.isEmpty() ? "" : "&") + item.getKey() + "=" + URLEncoder.encode(value, "UTF-8");


        String url = method.equals("GET") ? endpoint + "?" + query : endpoint;
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod(method);
        connection.setRequestProperty("accept", accept);

        if(method.equals("POST"))
        {
            connection.setRequestProperty("content-type", "application/x-www-form-urlencoded; charset=UTF-8");
            connection.setDoOutput(true);

            try(OutputStream out = connection.getOutputStream())
            {
                out.write(query.getBytes());
            }
        }

        if(connection.getResponseCode() == HttpURLConnection.HTTP_OK)
        {
            try(InputStream input = connection.getInputStream())
            {
                try(OutputStream output = res.getOutputStream())
                {
                    IOUtils.copy(input, output);
                }
            }
        }
        else
        {
            res.sendError(connection.getResponseCode(), connection.getResponseMessage());
        }
    }
}
