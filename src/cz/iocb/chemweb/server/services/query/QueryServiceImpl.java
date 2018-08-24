package cz.iocb.chemweb.server.services.query;

import java.beans.PropertyVetoException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import javax.servlet.http.HttpSession;
import org.apache.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import cz.iocb.chemweb.server.Utils;
import cz.iocb.chemweb.server.db.Literal;
import cz.iocb.chemweb.server.db.RdfNode;
import cz.iocb.chemweb.server.db.Result;
import cz.iocb.chemweb.server.db.Row;
import cz.iocb.chemweb.server.db.postgresql.PostgresDatabase;
import cz.iocb.chemweb.server.db.postgresql.PostgresHandler;
import cz.iocb.chemweb.server.services.SessionData;
import cz.iocb.chemweb.server.sparql.parser.Parser;
import cz.iocb.chemweb.server.sparql.parser.error.ParseExceptions;
import cz.iocb.chemweb.server.sparql.parser.model.Select;
import cz.iocb.chemweb.server.sparql.parser.model.SelectQuery;
import cz.iocb.chemweb.server.sparql.pubchem.PubChemConfiguration;
import cz.iocb.chemweb.server.sparql.translator.SparqlDatabaseConfiguration;
import cz.iocb.chemweb.server.sparql.translator.TranslateVisitor;
import cz.iocb.chemweb.server.sparql.translator.error.TranslateExceptions;
import cz.iocb.chemweb.server.velocity.NodeUtils;
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
    //private static final int timeout = 40 * 60 * 1000; // 40 minutes
    private static final int timeout = 24 * 60 * 60 * 1000;
    private static final SessionData<QueryState> sessionData = new SessionData<QueryState>("QuerySessionStorage");
    private static final Logger logger = Logger.getLogger(QueryServiceImpl.class);

    private final SparqlDatabaseConfiguration dbConfig;
    private final Parser parser;
    private final PostgresDatabase database;

    LinkedHashMap<RdfNode, String> nodeHashMap = new LinkedHashMap<RdfNode, String>(10000, 0.75f);



    public QueryServiceImpl()
            throws DatabaseException, FileNotFoundException, IOException, SQLException, PropertyVetoException
    {
        dbConfig = PubChemConfiguration.get();
        database = new PostgresDatabase(dbConfig.getConnectionPool());
        parser = new Parser(dbConfig.getProcedures(), dbConfig.getPrefixes());
    }


    @Override
    public long query(String query) throws SessionException, QueryException, DatabaseException
    {
        return query(query, 0, -1);
    }


    @Override
    public long query(String query, int offset, int limit) throws SessionException, QueryException, DatabaseException
    {
        final HttpSession httpSession = this.getThreadLocalRequest().getSession(true);
        final QueryState queryState = new QueryState();


        logger.info(query.replaceAll("\n", "\\\\n"));

        queryState.handler = database.getHandler();


        final long id = sessionData.insert(httpSession, queryState);

        try
        {
            final SelectQuery syntaxTree = parser.parse(query);

            if(limit >= 0)
            {
                Select originalSelect = syntaxTree.getSelect();
                Select limitedSelect = new Select();
                limitedSelect.setPattern(originalSelect);
                limitedSelect.setOffset(BigInteger.valueOf(offset));
                limitedSelect.setLimit(BigInteger.valueOf(limit + 1));
                syntaxTree.setSelect(limitedSelect);
            }


            final String translatedQuery = new TranslateVisitor(dbConfig).translate(syntaxTree);

            // TODO: log
            System.err.println(translatedQuery);


            queryState.thread = new Thread()
            {
                final Timer timer = new Timer();

                @Override
                public void run()
                {
                    try
                    {
                        Properties properties = new Properties();
                        properties.load(new FileInputStream(Utils.getConfigDirectory() + "/velocity.properties"));
                        properties.put("file.resource.loader.path", Utils.getConfigDirectory() + "/templates");

                        // TODO: use pool
                        final VelocityEngine ve = new VelocityEngine(properties);
                        Template template = ve.getTemplate("node.vm");


                        // query timeout
                        timer.schedule(new TimerTask()
                        {
                            @Override
                            public void run()
                            {
                                try
                                {
                                    queryState.handler.cancel();
                                }
                                catch(DatabaseException e)
                                {
                                    e.printStackTrace();
                                }
                            }
                        }, timeout);


                        Result result = database.rawQuery(translatedQuery, queryState.handler);


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
                                    context.put("utils", NodeUtils.class);
                                    context.put("entity", row.getRdfNodes()[i]);

                                    StringWriter writer = new StringWriter();
                                    template.merge(context, writer);

                                    stringRow[i].html = writer.toString();
                                    nodeHashMap.put(row.getRdfNodes()[i], stringRow[i].html);
                                }
                            }

                            items.add(stringRow);
                        }

                        timer.cancel();


                        boolean truncated = false;

                        if(limit >= 0 && items.size() > limit)
                        {
                            truncated = true;
                            items.remove(limit);
                        }


                        queryState.result = new QueryResult(result.getHeads(), items, truncated);
                    }
                    catch(Throwable e) // (SQLException | IOException e)
                    {
                        timer.cancel();

                        e.printStackTrace();
                        queryState.exception = e;
                    }
                }
            };
        }
        catch(ParseExceptions | TranslateExceptions e)
        {
            e.printStackTrace();
            throw new QueryException();
        }


        queryState.thread.start();
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
            e.printStackTrace();
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


        System.err.println("cancel query");
        queryState.handler.cancel();
    }


    @Override
    public int countOfProperties(String iri) throws DatabaseException
    {
        Result result;
        try
        {
            Parser parser = new Parser(dbConfig.getProcedures(), dbConfig.getPrefixes());

            final SelectQuery syntaxTree = parser
                    .parse("SELECT * WHERE { " + "<" + new URI(iri) + "> ?Property ?Value. }");

            final String translatedQuery = new TranslateVisitor(dbConfig).translate(syntaxTree);

            //result = database.query("sparql define input:storage virtrdf:PubchemQuadStorage "
            //        + "SELECT (count(*) as ?count) WHERE { " + "<" + new URI(iri) + "> ?Property ?Value. }");

            result = database.query("select count(*) as \"C#int\" from (" + translatedQuery + ") as tab");


            if(result.size() != 1)
                throw new DatabaseException();

            Row row = result.iterator().next();

            if(row.getRdfNodes().length != 1)
                throw new DatabaseException();

            return Integer.parseInt(((Literal) row.getRdfNodes()[0]).getValue());
        }
        catch(URISyntaxException | ParseExceptions | TranslateExceptions e)
        {
            e.printStackTrace();
            throw new DatabaseException(e);
        }
    }
}
