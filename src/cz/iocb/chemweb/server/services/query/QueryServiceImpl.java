package cz.iocb.chemweb.server.services.query;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;
import org.apache.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import cz.iocb.chemweb.server.db.Literal;
import cz.iocb.chemweb.server.db.RdfNode;
import cz.iocb.chemweb.server.db.Result;
import cz.iocb.chemweb.server.db.Row;
import cz.iocb.chemweb.server.db.postgresql.PostgresHandler;
import cz.iocb.chemweb.server.services.SessionData;
import cz.iocb.chemweb.server.sparql.SparqlEngine;
import cz.iocb.chemweb.server.sparql.config.SparqlDatabaseConfiguration;
import cz.iocb.chemweb.server.sparql.error.TranslateExceptions;
import cz.iocb.chemweb.server.velocity.NodeUtils;
import cz.iocb.chemweb.server.velocity.SparqlDirective;
import cz.iocb.chemweb.server.velocity.UrlDirective;
import cz.iocb.chemweb.shared.services.DatabaseException;
import cz.iocb.chemweb.shared.services.SessionException;
import cz.iocb.chemweb.shared.services.query.DataGridNode;
import cz.iocb.chemweb.shared.services.query.QueryException;
import cz.iocb.chemweb.shared.services.query.QueryResult;
import cz.iocb.chemweb.shared.services.query.QueryService;



public class QueryServiceImpl extends RemoteServiceServlet implements QueryService
{
    private static class QueryState
    {
        Thread thread;
        PostgresHandler handler;
        QueryResult result;
        Throwable exception;
    }


    private static final long serialVersionUID = 1L;
    private static final int timeout = 20 * 60 * 1000; // 20 minutes
    private static final SessionData<QueryState> sessionData = new SessionData<QueryState>("QuerySessionStorage");
    private static final Logger logger = Logger.getLogger(QueryServiceImpl.class);
    private static final int cacheSize = 1000000;
    private static final Map<String, Map<RdfNode, String>> nodeHashMaps = new HashMap<String, Map<RdfNode, String>>();

    private Map<RdfNode, String> nodeHashMap;
    private SparqlDatabaseConfiguration sparqlConfig;
    private SparqlEngine engine;
    private VelocityEngine ve;


    @Override
    public void init(ServletConfig config) throws ServletException
    {
        String resourceName = config.getInitParameter("resource");

        if(resourceName == null || resourceName.isEmpty())
            throw new ServletException("Resource name is not set");


        try
        {
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(config.getServletContext().getResourceAsStream("cacerts"), "changeit".toCharArray());

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keyStore);

            X509TrustManager trustManager = null;

            for(TrustManager tm : tmf.getTrustManagers())
                if(tm instanceof X509TrustManager)
                    trustManager = (X509TrustManager) tm;

            if(trustManager == null)
                throw new NoSuchAlgorithmException("No X509TrustManager in TrustManagerFactory");

            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, new TrustManager[] { trustManager }, null);

            Context context = (Context) (new InitialContext()).lookup("java:comp/env");
            sparqlConfig = (SparqlDatabaseConfiguration) context.lookup(resourceName);

            engine = new SparqlEngine(sparqlConfig, sslContext);


            Properties properties = new Properties();
            properties.put("runtime.log.logsystem.class", "org.apache.velocity.runtime.log.NullLogChute");
            properties.put("file.resource.loader.path", config.getServletContext().getRealPath("/templates"));
            properties.put("userdirective",
                    "cz.iocb.chemweb.server.velocity.SparqlDirective,cz.iocb.chemweb.server.velocity.UrlDirective");

            ve = new VelocityEngine(properties);
            ve.setApplicationAttribute(SparqlDirective.SPARQL_CONFIG, sparqlConfig);
        }
        catch(NamingException | KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException
                | KeyManagementException e)
        {
            throw new ServletException(e);
        }


        synchronized(QueryServiceImpl.class)
        {
            nodeHashMap = nodeHashMaps.get(resourceName);

            if(nodeHashMap == null)
            {
                nodeHashMap = Collections.synchronizedMap(new LinkedHashMap<RdfNode, String>(cacheSize, 0.75f, true)
                {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected boolean removeEldestEntry(Map.Entry<RdfNode, String> eldest)
                    {
                        return size() > cacheSize;
                    }
                });

                nodeHashMaps.put(resourceName, nodeHashMap);
            }
        }


        super.init(config);
    }


    @Override
    public long query(String query) throws QueryException, DatabaseException
    {
        return query(query, 0, -1);
    }


    @Override
    public long query(String query, int offset, int limit) throws QueryException, DatabaseException
    {
        final HttpSession httpSession = this.getThreadLocalRequest().getSession(true);
        final QueryState queryState = new QueryState();


        logger.info(query.replaceAll("\n", "\\\\n"));

        queryState.handler = engine.getHandler();


        final long id = sessionData.insert(httpSession, queryState);

        queryState.thread = new Thread()
        {
            @Override
            public void run()
            {
                try
                {
                    Template template = ve.getTemplate("node.vm");

                    Result result = engine.execute(query, offset, limit, timeout, queryState.handler);

                    Vector<DataGridNode[]> items = new Vector<DataGridNode[]>();

                    for(Row row : result)
                    {
                        DataGridNode[] stringRow = new DataGridNode[row.getRdfNodes().length];

                        for(int i = 0; i < row.getRdfNodes().length; i++)
                        {
                            stringRow[i] = new DataGridNode();

                            if(row.getRdfNodes()[i] != null && !row.getRdfNodes()[i].isLiteral())
                                stringRow[i].ref = row.getRdfNodes()[i].getValue();


                            String html = nodeHashMap.get(row.getRdfNodes()[i]);

                            if(html != null)
                            {
                                stringRow[i].html = html;
                            }
                            else
                            {
                                VelocityContext context = new VelocityContext();
                                context.put("urlContext", UrlDirective.Context.NODE);
                                context.put("utils", new NodeUtils(sparqlConfig.getPrefixes()));
                                context.put("entity", row.getRdfNodes()[i]);

                                StringWriter writer = new StringWriter();
                                template.merge(context, writer);

                                stringRow[i].html = writer.toString();
                                nodeHashMap.put(row.getRdfNodes()[i], stringRow[i].html);
                            }
                        }

                        items.add(stringRow);
                    }


                    boolean truncated = false;

                    if(limit >= 0 && items.size() > limit)
                    {
                        truncated = true;
                        items.remove(limit);
                    }


                    queryState.result = new QueryResult(result.getHeads(), items, truncated);
                }
                catch(Throwable e)
                {
                    queryState.exception = e;
                }
            }
        };


        synchronized(queryState.handler)
        {
            queryState.thread.start();

            try
            {
                queryState.handler.wait();
            }
            catch(InterruptedException e)
            {
                new DatabaseException(e); //FIXME: use different exception
            }
        }

        return id;
    }


    @Override
    public QueryResult getResult(long queryID) throws SessionException, DatabaseException
    {
        final HttpSession httpSession = this.getThreadLocalRequest().getSession(false);

        if(httpSession == null)
            throw new SessionException("Your server session does not exist.");

        final QueryState queryState = sessionData.get(httpSession, queryID);

        if(queryState == null)
            throw new SessionException("Your server session does not contain the requested query ID.");


        try
        {
            queryState.thread.join();
        }
        catch(InterruptedException e)
        {
            throw new DatabaseException(e); //FIXME: use different exception
        }
        finally
        {
            sessionData.remove(httpSession, queryID);
        }

        if(queryState.exception != null)
        {
            if(queryState.exception instanceof DatabaseException)
                throw(DatabaseException) queryState.exception;
            else
                throw new DatabaseException(queryState.exception); //FIXME: use different exception
        }

        return queryState.result;
    }


    @Override
    public void cancel(long queryID) throws SessionException, DatabaseException
    {
        HttpSession httpSession = this.getThreadLocalRequest().getSession(false);

        if(httpSession == null)
            throw new SessionException("Your server session does not exist.");

        final QueryState queryState = sessionData.getAndRemove(httpSession, queryID);

        if(queryState == null)
            throw new SessionException("Your server session does not contain the requested query ID.");

        try
        {
            queryState.handler.cancel();
        }
        catch(SQLException e)
        {
            throw new DatabaseException(e);
        }
    }


    @Override
    public int countOfProperties(String iri) throws DatabaseException
    {
        try
        {
            Result result = engine
                    .execute("SELECT (count(*) as ?C) WHERE { " + "<" + new URI(iri) + "> ?Property ?Value. }");

            if(result.size() != 1)
                throw new DatabaseException();

            Row row = result.iterator().next();

            if(row.getRdfNodes().length != 1)
                throw new DatabaseException();

            return Integer.parseInt(((Literal) row.getRdfNodes()[0]).getValue());
        }
        catch(URISyntaxException | TranslateExceptions | SQLException e)
        {
            throw new DatabaseException(e);
        }
    }
}
