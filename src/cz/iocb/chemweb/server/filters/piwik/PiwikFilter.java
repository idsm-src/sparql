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
import org.apache.commons.io.IOUtils;
import org.piwik.java.tracking.PiwikRequest;
import org.piwik.java.tracking.PiwikTracker;



public class PiwikFilter implements Filter
{
    private int siteId = -1;
    private String hostUrl = null;
    private String authToken = null;
    private boolean rpc = false;

    private String cookieName = null;


    @Override
    public void init(FilterConfig config) throws ServletException
    {
        String siteIdString = config.getInitParameter("siteId");

        if(siteIdString == null || siteIdString.isEmpty())
            throw new IllegalArgumentException("Site ID not set, please set init-param 'siteId' in web.xml");

        siteId = Integer.parseInt(siteIdString);

        hostUrl = config.getInitParameter("hostUrl");
        if(hostUrl == null && hostUrl.isEmpty())
            throw new IllegalArgumentException("Host URL not set, please set init-param 'hostUrl' in web.xml");

        authToken = config.getInitParameter("authToken");
        if(authToken == null && authToken.isEmpty())
            throw new IllegalArgumentException("AuthToken not set, please set init-param 'authToken' in web.xml");

        String rpcString = config.getInitParameter("rpc");
        if(rpcString != null && !rpcString.isEmpty())
            rpc = Boolean.parseBoolean(rpcString);


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

        URL actionUrl = new URL(httpRequest.getRequestURL().toString());
        PiwikRequest piwikRequest = new PiwikRequest(siteId, actionUrl);


        String visitorId = null;

        if(httpRequest.getCookies() != null)
            for(Cookie cookie : httpRequest.getCookies())
                if(cookie.getName().startsWith(cookieName) && cookie.getValue().length() >= 16)
                    visitorId = cookie.getValue().substring(0, 16);

        if(visitorId != null)
            piwikRequest.setVisitorId(visitorId);

        piwikRequest.setAuthToken(authToken);

        
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
                } catch (UnknownHostException e) {

                    e.printStackTrace();
                }
            }
        }
        
        piwikRequest.setVisitorIp(address != null ? address : httpRequest.getRemoteAddr());


        String agent = httpRequest.getHeader("User-Agent");

        if(agent != null)
            piwikRequest.setHeaderUserAgent(agent);


        if(rpc)
        {
            httpRequest = new MultiReadHttpServletRequest(httpRequest);

            StringWriter writer = new StringWriter();
            IOUtils.copy(httpRequest.getReader(), writer);
            String body = writer.toString();
            String[] args = body.split("\\|");

            if(args.length > 6)
            {
                piwikRequest.setActionName("GWT RPC: " + args[5] + "." + args[6] + "()");
                piwikRequest.setActionUrl(new URL(actionUrl.toString() + "/" + args[6]));
            }
        }

        long actionTime = System.currentTimeMillis();
        filterChain.doFilter(httpRequest, response);
        actionTime = System.currentTimeMillis() - actionTime;
        piwikRequest.setActionTime(actionTime);


        PiwikTracker tracker = new PiwikTracker(hostUrl);
        /*HttpResponse piwikResponse =*/ tracker.sendRequest(piwikRequest);
    }
}
