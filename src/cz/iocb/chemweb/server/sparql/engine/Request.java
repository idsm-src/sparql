package cz.iocb.chemweb.server.sparql.engine;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import javax.net.ssl.SSLContext;
import org.antlr.v4.runtime.ParserRuleContext;
import cz.iocb.chemweb.server.sparql.config.SparqlDatabaseConfiguration;
import cz.iocb.chemweb.server.sparql.error.MessageCategory;
import cz.iocb.chemweb.server.sparql.error.TranslateExceptions;
import cz.iocb.chemweb.server.sparql.error.TranslateMessage;
import cz.iocb.chemweb.server.sparql.parser.Parser;
import cz.iocb.chemweb.server.sparql.parser.model.DataSet;
import cz.iocb.chemweb.server.sparql.parser.model.SelectQuery;
import cz.iocb.chemweb.server.sparql.parser.visitor.QueryVisitor;
import cz.iocb.chemweb.server.sparql.translator.TranslateVisitor;



public class Request implements AutoCloseable
{
    private final SparqlDatabaseConfiguration config;
    private final SSLContext sslContext;

    private Connection connection;
    private Statement statement;

    private long begin;
    private long timeout;
    private boolean canceled;


    Request(SparqlDatabaseConfiguration config, SSLContext sslContext)
    {
        this.config = config;
        this.sslContext = sslContext;
    }


    public List<TranslateMessage> check(String query, List<DataSet> dataSets, int timeout)
            throws TranslateExceptions, SQLException
    {
        List<TranslateMessage> messages = new LinkedList<TranslateMessage>();

        Parser parser = new Parser(messages);
        ParserRuleContext context = parser.parse(query);

        QueryVisitor queryVisitor = new QueryVisitor(config, messages);
        SelectQuery syntaxTree = (SelectQuery) queryVisitor.visit(context);

        if(dataSets != null && !dataSets.isEmpty())
            syntaxTree.getSelect().setDataSets(dataSets);

        TranslateVisitor translateVisitor = new TranslateVisitor(this, messages, false);
        translateVisitor.translate(syntaxTree);

        return messages;
    }


    public List<TranslateMessage> check(String query) throws TranslateExceptions, SQLException
    {
        return check(query, null, 0);
    }


    public Result execute(String query, List<DataSet> dataSets, int offset, int limit, int timeout)
            throws TranslateExceptions, SQLException
    {
        List<TranslateMessage> messages = new LinkedList<TranslateMessage>();

        Parser parser = new Parser(messages);
        ParserRuleContext context = parser.parse(query);

        checkForErrors(messages);

        QueryVisitor queryVisitor = new QueryVisitor(config, messages);
        SelectQuery syntaxTree = (SelectQuery) queryVisitor.visit(context);

        checkForErrors(messages);

        if(dataSets != null && !dataSets.isEmpty())
            syntaxTree.getSelect().setDataSets(dataSets);


        if(offset > 0)
        {
            BigInteger oldOffset = syntaxTree.getSelect().getOffset();
            BigInteger newOffset = BigInteger.valueOf(offset);

            if(oldOffset != null)
                newOffset = newOffset.add(oldOffset);

            syntaxTree.getSelect().setOffset(newOffset);
        }

        if(limit >= 0)
        {
            BigInteger oldLimit = syntaxTree.getSelect().getLimit();
            BigInteger newLimit = BigInteger.valueOf(limit + 1);

            if(oldLimit != null && offset > 0)
                oldLimit = oldLimit.subtract(BigInteger.valueOf(offset));

            if(oldLimit != null && oldLimit.signum() == -1)
                oldLimit = BigInteger.ZERO;

            if(oldLimit == null || oldLimit.compareTo(newLimit) == 1)
                syntaxTree.getSelect().setLimit(newLimit);
            else
                syntaxTree.getSelect().setLimit(oldLimit);
        }

        setTimeout(timeout);

        TranslateVisitor translateVisitor = new TranslateVisitor(this, messages, true);
        String code = translateVisitor.translate(syntaxTree);

        checkForErrors(messages);

        Statement statement = getStatement();

        try
        {
            return new Result(statement.executeQuery(code));
        }
        catch(Throwable e)
        {
            statement.close();
            throw e;
        }
    }


    public Result execute(String query) throws TranslateExceptions, SQLException
    {
        return execute(query, null, -1, -1, 0);
    }


    public Result execute(String query, List<DataSet> dataSets) throws TranslateExceptions, SQLException
    {
        return execute(query, dataSets, -1, -1, 0);
    }


    public Result execute(String query, int offset, int limit, int timeout) throws TranslateExceptions, SQLException
    {
        return execute(query, null, offset, limit, timeout);
    }


    private void checkForErrors(List<TranslateMessage> messages) throws TranslateExceptions
    {
        List<TranslateMessage> errors = messages.stream().filter(m -> m.getCategory() == MessageCategory.ERROR)
                .collect(Collectors.toList());

        if(!errors.isEmpty())
            throw new TranslateExceptions(errors);
    }


    public synchronized void cancel() throws SQLException
    {
        canceled = true;

        if(statement != null && !statement.isClosed())
        {
            statement.cancel();
            statement.close();
        }
    }


    @Override
    public synchronized void close() throws SQLException
    {
        try
        {
            if(statement != null && !statement.isClosed())
                statement.close();
        }
        finally
        {
            if(connection != null)
                connection.close();
        }
    }


    private synchronized Connection getConnection() throws SQLException
    {
        if(connection == null)
        {
            connection = config.getConnectionPool().getConnection();
            connection.setAutoCommit(false);
        }

        return connection;
    }


    private int getTimeout()
    {
        if(timeout == 0)
            return 0;

        int restTime = (int) (timeout - ((System.nanoTime() - begin) / 1000000));

        if(restTime <= 0)
            return 1; //FIXME: throw exception?

        return restTime;
    }


    private void setTimeout(long timeout) throws SQLException
    {
        getConnection(); // time is measured after a connection is established

        this.timeout = timeout;
        this.begin = System.nanoTime();
    }


    public synchronized Statement getStatement() throws SQLException
    {
        if(statement != null && !statement.isClosed())
            throw new IllegalStateException();

        if(canceled)
            throw new SQLException("query was canceled");

        Statement statement = getConnection().createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        statement.setQueryTimeout(getTimeout());
        statement.setFetchSize(1000);

        this.statement = statement;
        return statement;
    }


    public PreparedStatement getStatement(String query) throws SQLException
    {
        if(statement != null && !statement.isClosed())
            throw new IllegalStateException();

        if(canceled)
            throw new SQLException("query was canceled");

        PreparedStatement statement = getConnection().prepareStatement(query);
        statement.setQueryTimeout(getTimeout());
        statement.setFetchSize(1000);

        this.statement = statement;
        return statement;
    }


    public SparqlDatabaseConfiguration getConfiguration()
    {
        return config;
    }


    public SSLContext getSslContext()
    {
        return sslContext;
    }
}
