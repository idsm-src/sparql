package cz.iocb.chemweb.server.filters.piwik;

import java.io.IOException;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.DatatypeConverter;
import org.apache.commons.io.IOUtils;
import org.piwik.java.tracking.PiwikRequest;
import org.piwik.java.tracking.PiwikTracker;



public class PiwikFilter implements Filter
{
    private int siteId = -1;
    private String hostUrl = null;
    private String authToken = null;
    private String actionName = null;
    String[] replaceUrl = null;
    private boolean rpc = false;

    private String cookieName = null;


    static
    {
        /* memory leak protection*/
        ClassLoader active = Thread.currentThread().getContextClassLoader();
        ClassLoader root = active;

        while(root.getParent() != null)
            root = root.getParent();

        Thread.currentThread().setContextClassLoader(root);
        DatatypeConverter.printHexBinary(new byte[0]);
        Thread.currentThread().setContextClassLoader(active);
    }


    @Override
    public void init(FilterConfig config) throws ServletException
    {
        /* obtain the value of parameter siteId */
        String siteIdString = config.getInitParameter("siteId");

        if(siteIdString == null || siteIdString.isEmpty())
            throw new ServletException("Site ID not set, please set init-param 'siteId' in web.xml");

        siteId = Integer.parseInt(siteIdString);


        /* obtain the value of parameter hostUrl */
        hostUrl = config.getInitParameter("hostUrl");

        if(hostUrl == null || hostUrl.isEmpty())
            throw new ServletException("Host URL not set, please set init-param 'hostUrl' in web.xml");


        /* obtain the value of parameter filterName */
        actionName = config.getInitParameter("actionName");


        /* obtain the value of parameter replaceUrl */
        String replace = config.getInitParameter("replaceUrl");

        if(replace != null)
        {
            if(replace.length() < 4)
                throw new ServletException("Invalid value of init-param 'replaceUrl' in web.xml");

            if(replace.charAt(0) != replace.charAt(replace.length() - 1))
                throw new ServletException("Invalid value of init-param 'replaceUrl' in web.xml");

            int delimiter = replace.indexOf(replace.charAt(0), 1);

            if(delimiter == -1 || replace.indexOf(replace.charAt(0), delimiter + 1) != replace.length() - 1)
                throw new ServletException("Invalid value of init-param 'replaceUrl' in web.xml");

            replaceUrl = new String[] { replace.substring(1, delimiter),
                    replace.substring(delimiter + 1, replace.length() - 1) };
        }


        /* obtain the value of parameter rpc */
        String rpcString = config.getInitParameter("rpc");

        if(rpcString != null && !rpcString.isEmpty())
            rpc = Boolean.parseBoolean(rpcString);


        /* obtain the value of parameter authToken */
        String authTokenParameter = config.getInitParameter("authToken");

        if(authTokenParameter == null || authTokenParameter.isEmpty())
            throw new ServletException("AuthToken property not set, please set init-param 'authToken' in web.xml");

        authToken = config.getServletContext().getInitParameter(authTokenParameter);

        if(authToken == null || authToken.isEmpty())
            throw new ServletException("AuthToken property " + authTokenParameter + " is not valid");


        cookieName = "_pk_id." + siteId + ".";
    }


    @Override
    public void destroy()
    {

    }


    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
            throws IOException, ServletException
    {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String actionUrl = httpRequest.getRequestURL().toString();
        String actionName = null;


        if(rpc)
        {
            httpRequest = new MultiReadHttpServletRequest(httpRequest);

            StringWriter writer = new StringWriter();
            IOUtils.copy(httpRequest.getReader(), writer);
            String body = writer.toString();
            String[] args = body.split("\\|");

            if(args.length > 6)
            {
                actionName = "GWT RPC: " + args[5] + "." + args[6] + "()";
                actionUrl = actionUrl + "/" + args[6];
            }
        }

        if(replaceUrl != null)
            actionUrl = actionUrl.replaceFirst(replaceUrl[0], replaceUrl[1]);

        if(this.actionName != null)
            actionName = this.actionName;


        /* create piwik request */
        PiwikRequest piwikRequest = new PiwikRequest(siteId, new URL(actionUrl));
        piwikRequest.setActionName(actionName);
        piwikRequest.setAuthToken(authToken);


        /* set visitor id */
        String visitorId = null;

        if(httpRequest.getCookies() != null)
            for(Cookie cookie : httpRequest.getCookies())
                if(cookie.getName().startsWith(cookieName) && cookie.getValue().length() >= 16)
                    visitorId = cookie.getValue().substring(0, 16);

        if(visitorId != null)
            piwikRequest.setVisitorId(visitorId);


        /* set visitor ip */
        String forwarded = httpRequest.getHeader("X-Forwarded-For");
        String address = null;

        if(forwarded != null)
        {
            for(String forward : forwarded.split(","))
            {
                String trimmed = forward.trim();

                try
                {
                    if(!InetAddress.getByName(trimmed).isSiteLocalAddress())
                    {
                        address = trimmed;
                        break;
                    }
                }
                catch(UnknownHostException e)
                {

                    e.printStackTrace();
                }
            }
        }

        piwikRequest.setVisitorIp(address != null ? address : httpRequest.getRemoteAddr());


        /* set user agent */
        String agent = httpRequest.getHeader("User-Agent");

        if(agent != null)
            piwikRequest.setHeaderUserAgent(agent);


        /* set action time */
        long actionTime = System.currentTimeMillis();
        filterChain.doFilter(httpRequest, response);
        actionTime = System.currentTimeMillis() - actionTime;
        piwikRequest.setActionTime(actionTime);


        PiwikTracker tracker = new PiwikTracker(hostUrl);
        /*HttpResponse piwikResponse =*/ tracker.sendRequest(piwikRequest);
    }
}
