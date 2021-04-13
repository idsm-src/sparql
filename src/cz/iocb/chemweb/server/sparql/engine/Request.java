package cz.iocb.chemweb.server.sparql.engine;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import org.antlr.v4.runtime.ParserRuleContext;
import cz.iocb.chemweb.server.sparql.config.SparqlDatabaseConfiguration;
import cz.iocb.chemweb.server.sparql.engine.Result.ResultType;
import cz.iocb.chemweb.server.sparql.error.MessageCategory;
import cz.iocb.chemweb.server.sparql.error.TranslateExceptions;
import cz.iocb.chemweb.server.sparql.error.TranslateMessage;
import cz.iocb.chemweb.server.sparql.parser.Parser;
import cz.iocb.chemweb.server.sparql.parser.model.AskQuery;
import cz.iocb.chemweb.server.sparql.parser.model.ConstructQuery;
import cz.iocb.chemweb.server.sparql.parser.model.DataSet;
import cz.iocb.chemweb.server.sparql.parser.model.DescribeQuery;
import cz.iocb.chemweb.server.sparql.parser.model.IRI;
import cz.iocb.chemweb.server.sparql.parser.model.Query;
import cz.iocb.chemweb.server.sparql.parser.model.SelectQuery;
import cz.iocb.chemweb.server.sparql.parser.model.Variable;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;
import cz.iocb.chemweb.server.sparql.parser.model.pattern.Pattern;
import cz.iocb.chemweb.server.sparql.parser.model.triple.BlankNode;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Node;
import cz.iocb.chemweb.server.sparql.parser.model.triple.Triple;
import cz.iocb.chemweb.server.sparql.parser.visitor.QueryVisitor;
import cz.iocb.chemweb.server.sparql.translator.TranslateVisitor;
import cz.iocb.chemweb.server.sparql.translator.imcode.SqlQuery;



public class Request implements AutoCloseable
{
    private static int fetchSize = 1000;

    private final SparqlDatabaseConfiguration config;

    private Connection connection;
    private Statement statement;

    private long begin;
    private long timeout;
    private boolean canceled;


    Request(SparqlDatabaseConfiguration config)
    {
        this.config = config;
    }


    public List<TranslateMessage> check(String query, List<DataSet> dataSets, int timeout) throws SQLException
    {
        List<TranslateMessage> messages = new LinkedList<TranslateMessage>();

        try
        {
            Parser parser = new Parser(messages);
            ParserRuleContext context = parser.parse(query);

            QueryVisitor queryVisitor = new QueryVisitor(config, messages);
            Query syntaxTree = queryVisitor.visit(context);

            if(dataSets != null && !dataSets.isEmpty())
                syntaxTree.getSelect().setDataSets(dataSets);

            TranslateVisitor translateVisitor = new TranslateVisitor(this, messages, false);
            translateVisitor.translate(syntaxTree);
        }
        catch(SQLException e)
        {
            throw e;
        }
        catch(Throwable e)
        {
            System.err.println("RequestCheck: log begin");
            System.err.println(query);
            e.printStackTrace(System.err);
            System.err.println("RequestCheck: log end");
        }

        return messages;
    }


    public List<TranslateMessage> check(String query) throws SQLException
    {
        return check(query, null, 0);
    }


    public Result execute(String query, List<DataSet> dataSets, int offset, int limit, long timeout)
            throws TranslateExceptions, SQLException
    {
        List<TranslateMessage> messages = new LinkedList<TranslateMessage>();

        Parser parser = new Parser(messages);
        ParserRuleContext context = parser.parse(query);

        checkForErrors(messages);

        QueryVisitor queryVisitor = new QueryVisitor(config, messages);
        Query syntaxTree = queryVisitor.visit(context);

        checkForErrors(messages);

        if(dataSets != null && !dataSets.isEmpty())
            syntaxTree.getSelect().setDataSets(dataSets);


        getConnection(); // time is measured after a connection is established
        this.timeout = timeout;
        this.begin = System.nanoTime();


        ResultType type = null;

        if(syntaxTree instanceof SelectQuery)
            type = ResultType.SELECT;
        else if(syntaxTree instanceof AskQuery)
            type = ResultType.ASK;
        else if(syntaxTree instanceof DescribeQuery)
            type = ResultType.DESCRIBE;
        else if(syntaxTree instanceof ConstructQuery)
            type = ResultType.CONSTRUCT;


        TranslateVisitor translateVisitor = new TranslateVisitor(this, messages, true);
        SqlQuery imcode = translateVisitor.translate(syntaxTree);

        if(type != ResultType.CONSTRUCT)
        {
            imcode.setOffset(offset);

            if(limit >= 0)
                imcode.setLimit(limit);
        }

        String code = imcode.translate();

        checkForErrors(messages);

        Statement statement = getStatement();

        try
        {
            if(type == ResultType.CONSTRUCT)
            {
                ArrayList<RdfNode[]> templates = new ArrayList<RdfNode[]>();

                for(Pattern pattern : ((ConstructQuery) syntaxTree).getTemplates())
                {
                    Triple triple = (Triple) pattern;

                    RdfNode[] template = new RdfNode[3];
                    templates.add(template);

                    template[0] = convertNodeToTemplate(triple.getSubject());
                    template[1] = convertNodeToTemplate((Node) triple.getPredicate());
                    template[2] = convertNodeToTemplate(triple.getObject());
                }

                return new ConstructResult(templates, statement.executeQuery(code), limit, offset, begin, timeout);
            }
            else
            {
                return new SelectResult(type, statement.executeQuery(code), begin, timeout);
            }
        }
        catch(Throwable e)
        {
            statement.close();
            e.printStackTrace();
            throw e;
        }
    }


    public Result execute(String query) throws TranslateExceptions, SQLException
    {
        return execute(query, null, 0, -1, 0);
    }


    public Result execute(String query, List<DataSet> dataSets) throws TranslateExceptions, SQLException
    {
        return execute(query, dataSets, 0, -1, 0);
    }


    public Result execute(String query, int offset, int limit, long timeout) throws TranslateExceptions, SQLException
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


    private int getStatementTimeout()
    {
        if(timeout == 0)
            return 0;

        int restTime = (int) ((timeout - (System.nanoTime() - begin)) / 1000000000);

        if(restTime <= 0)
            return 1; //FIXME: throw exception?

        return restTime;
    }


    public synchronized Statement getStatement() throws SQLException
    {
        if(statement != null && !statement.isClosed())
            throw new IllegalStateException();

        if(canceled)
            throw new SQLException("query was canceled");

        Statement statement = getConnection().createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        statement.setQueryTimeout(getStatementTimeout());
        statement.setFetchSize(fetchSize);

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
        statement.setQueryTimeout(getStatementTimeout());
        statement.setFetchSize(fetchSize);

        this.statement = statement;
        return statement;
    }


    public SparqlDatabaseConfiguration getConfiguration()
    {
        return config;
    }


    private static RdfNode convertNodeToTemplate(Node node)
    {
        if(node instanceof IRI)
            return new IriNode(((IRI) node).getValue());
        else if(node instanceof BlankNode)
            return new BNode(((BlankNode) node).getName());
        else if(node instanceof Variable)
            return new VariableNode(((Variable) node).getName());
        else if(((Literal) node).getLanguageTag() != null)
            return new LanguageTaggedLiteral(((Literal) node).getStringValue(), ((Literal) node).getLanguageTag());
        else if(((Literal) node).getTypeIri() != null)
            return new TypedLiteral(((Literal) node).getStringValue(), ((Literal) node).getTypeIri());
        else
            return new LiteralNode(((Literal) node).getStringValue());
    }


    public long getBegin()
    {
        return begin;
    }


    public long getTimeout()
    {
        return timeout;
    }
}
