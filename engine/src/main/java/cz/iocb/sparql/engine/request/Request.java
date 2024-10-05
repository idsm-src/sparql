package cz.iocb.sparql.engine.request;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.unsupportedLiteral;
import static java.util.stream.Collectors.toList;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.antlr.v4.runtime.ParserRuleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.SQLRuntimeException;
import cz.iocb.sparql.engine.database.Table;
import cz.iocb.sparql.engine.error.MessageCategory;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.error.TranslateMessage;
import cz.iocb.sparql.engine.mapping.BlankNodeLiteral;
import cz.iocb.sparql.engine.mapping.classes.BuiltinClasses;
import cz.iocb.sparql.engine.mapping.classes.DataType;
import cz.iocb.sparql.engine.mapping.classes.IriClass;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.mapping.classes.UserIriClass;
import cz.iocb.sparql.engine.parser.Parser;
import cz.iocb.sparql.engine.parser.model.AskQuery;
import cz.iocb.sparql.engine.parser.model.ConstructQuery;
import cz.iocb.sparql.engine.parser.model.DataSet;
import cz.iocb.sparql.engine.parser.model.DescribeQuery;
import cz.iocb.sparql.engine.parser.model.IRI;
import cz.iocb.sparql.engine.parser.model.Query;
import cz.iocb.sparql.engine.parser.model.Select;
import cz.iocb.sparql.engine.parser.model.SelectQuery;
import cz.iocb.sparql.engine.parser.model.expression.Literal;
import cz.iocb.sparql.engine.parser.model.triple.Node;
import cz.iocb.sparql.engine.parser.visitor.QueryVisitor;
import cz.iocb.sparql.engine.request.Result.ResultType;
import cz.iocb.sparql.engine.translator.TranslateVisitor;
import cz.iocb.sparql.engine.translator.imcode.SqlQuery;



public class Request implements AutoCloseable
{
    private static final Logger logger = LoggerFactory.getLogger(Request.class);

    private final SparqlDatabaseConfiguration config;

    private final IriCache iriCache = new IriCache(10000);
    private final Map<IRI, Set<IriClass>> missmatches = new HashMap<IRI, Set<IriClass>>();

    private Connection connection;
    private Statement statement;
    private List<Table> tables = new ArrayList<Table>();

    private long begin;
    private long timeout;
    private int fetchSize;
    private boolean canceled;


    public Request(SparqlDatabaseConfiguration config)
    {
        this.config = config;
    }


    public List<TranslateMessage> check(String query, List<DataSet> dataSets, int timeout) throws SQLException
    {
        List<TranslateMessage> messages = new LinkedList<TranslateMessage>();

        try
        {
            MDC.put("sparql", query);

            Parser parser = new Parser(messages);
            ParserRuleContext context = parser.parse(query);

            if(hasErrors(messages))
                return messages;

            QueryVisitor queryVisitor = new QueryVisitor(this, messages);
            Query syntaxTree = queryVisitor.visit(context);

            if(hasErrors(messages))
                return messages;

            if(dataSets != null && !dataSets.isEmpty())
                syntaxTree.getSelect().setDataSets(dataSets);

            TranslateVisitor translateVisitor = new TranslateVisitor(this, messages, false);
            translateVisitor.translate(syntaxTree, null, null, false);

            logger.trace("query check");
        }
        catch(Throwable e)
        {
            logger.error("sparql check error: " + e.getMessage(), e);

            if(statement != null)
                statement.close();
        }
        finally
        {
            MDC.remove("sparql");
        }

        return messages;
    }


    public List<TranslateMessage> check(String query) throws SQLException
    {
        return check(query, null, 0);
    }


    public Result execute(String query, List<DataSet> dataSets, int offset, int limit, int fetchSize, long timeout)
            throws TranslateExceptions, SQLException
    {
        try
        {
            MDC.put("sparql", query);

            getConnection(); // time is measured after a connection is established

            List<TranslateMessage> messages = new LinkedList<TranslateMessage>();

            Parser parser = new Parser(messages);
            ParserRuleContext context = parser.parse(query);

            checkForErrors(messages);

            QueryVisitor queryVisitor = new QueryVisitor(this, messages);
            Query syntaxTree = queryVisitor.visit(context);

            checkForErrors(messages);

            if(dataSets != null && !dataSets.isEmpty())
                syntaxTree.getSelect().setDataSets(dataSets);


            this.fetchSize = fetchSize;
            this.timeout = timeout;
            this.begin = System.nanoTime();


            ResultType type = null;

            if(syntaxTree instanceof SelectQuery)
            {
                type = ResultType.SELECT;

                Select select = syntaxTree.getSelect();

                if(limit > 0 && limit <= fetchSize)
                    this.fetchSize = 0;

                if(select.getLimit() != null && select.getLimit().compareTo(BigInteger.valueOf(fetchSize)) <= 0)
                    this.fetchSize = 0;

                if(select.getGroupByConditions().isEmpty() && select.isInAggregateMode())
                    this.fetchSize = 0;
            }
            else if(syntaxTree instanceof AskQuery)
            {
                type = ResultType.ASK;
                this.fetchSize = 0;
            }
            else if(syntaxTree instanceof DescribeQuery)
            {
                type = ResultType.DESCRIBE;
                this.fetchSize = 0;
            }
            else if(syntaxTree instanceof ConstructQuery)
            {
                type = ResultType.CONSTRUCT;
            }

            BigInteger newOffset = syntaxTree instanceof AskQuery || offset <= 0 ? null : BigInteger.valueOf(offset);
            BigInteger newLimit = syntaxTree instanceof AskQuery || limit <= 0 ? null : BigInteger.valueOf(limit);

            TranslateVisitor translateVisitor = new TranslateVisitor(this, messages, true);
            SqlQuery imcode = translateVisitor.translate(syntaxTree, newOffset, newLimit, true);

            String code = imcode.translate(this);

            MDC.put("sql", code);

            logger.trace("query evaluation");

            checkForErrors(messages);

            return new Result(type, getStatement().executeQuery(code), begin, timeout);
        }
        catch(Throwable e)
        {
            if(e instanceof TranslateExceptions)
                logger.info("query translation error: " + e.getMessage());
            else
                logger.error("query evaluation error: " + e.getMessage(), e);

            if(statement != null)
                statement.close();

            throw e;
        }
        finally
        {
            MDC.remove("sparql");
            MDC.remove("sql");
        }
    }


    public Result execute(String query) throws TranslateExceptions, SQLException
    {
        return execute(query, null, 0, -1, 0, 0);
    }


    public Result execute(String query, List<DataSet> dataSets) throws TranslateExceptions, SQLException
    {
        return execute(query, dataSets, 0, -1, 0, 0);
    }


    public Result execute(String query, int offset, int limit, long timeout) throws TranslateExceptions, SQLException
    {
        return execute(query, null, offset, limit, 0, timeout);
    }


    private boolean hasErrors(List<TranslateMessage> messages)
    {
        return messages.stream().anyMatch(m -> m.getCategory() == MessageCategory.ERROR);
    }


    private void checkForErrors(List<TranslateMessage> messages) throws TranslateExceptions
    {
        List<TranslateMessage> errors = messages.stream().filter(m -> m.getCategory() == MessageCategory.ERROR)
                .collect(toList());

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
            {
                for(Table table : tables)
                {
                    try(Statement stm = connection.createStatement())
                    {
                        stm.execute("drop table " + table);
                    }
                    catch(SQLException e)
                    {
                        e.printStackTrace();
                    }
                }

                try
                {
                    connection.rollback();
                }
                catch(SQLException e)
                {
                    e.printStackTrace();
                }
                finally
                {
                    connection.close();
                }
            }
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


    public synchronized Statement getStatement()
    {
        try
        {
            if(canceled)
                throw new SQLException("query was canceled");

            if(statement == null)
            {
                Statement statement = getConnection().createStatement(ResultSet.TYPE_FORWARD_ONLY,
                        ResultSet.CONCUR_READ_ONLY);
                statement.setFetchSize(fetchSize);

                this.statement = statement;
            }

            statement.setQueryTimeout(getStatementTimeout());

            return statement;
        }
        catch(SQLException e)
        {
            throw new SQLRuntimeException(e);
        }
    }


    public SparqlDatabaseConfiguration getConfiguration()
    {
        return config;
    }


    public long getBegin()
    {
        return begin;
    }


    public long getTimeout()
    {
        return timeout;
    }


    public ResourceClass getResourceClass(Node value)
    {
        if(value instanceof Literal)
        {
            Literal literal = (Literal) value;
            DataType datatype = getConfiguration().getDataType(literal.getTypeIri());
            LiteralClass resourceClass = datatype == null ? unsupportedLiteral : datatype.getResourceClass(literal);

            return resourceClass;
        }
        else if(value instanceof IRI iri)
        {
            return getIriClass(iri);
        }
        else if(value instanceof BlankNodeLiteral)
        {
            return ((BlankNodeLiteral) value).getResourceClass();
        }
        else
        {
            return null;
        }
    }


    public IriClass getIriClass(IRI value)
    {
        IriClass iriClass = iriCache.getIriClass(value);

        if(iriClass != null)
            return iriClass;

        iriClass = config.getIriCache().getIriClass(value);

        if(iriClass != null)
            return iriClass;

        iriClass = detectIriClass(value);
        List<Column> columns = iriClass.toColumns(statement, value);
        iriCache.storeToCache(value, iriClass, columns);

        return iriClass;
    }


    private IriClass detectIriClass(IRI value)
    {
        for(UserIriClass iriClass : getConfiguration().getIriClasses())
            if(iriClass.match(getStatement(), value))
                return iriClass;

        return BuiltinClasses.unsupportedIri;
    }


    public boolean match(ResourceClass resClass, Node node)
    {
        if(resClass instanceof IriClass iriClass && node instanceof IRI iri)
        {
            IriClass cachedClass = iriCache.getIriClass(iri);

            if(cachedClass != null)
                return iriClass == cachedClass;

            cachedClass = config.getIriCache().getIriClass(iri);

            if(cachedClass != null)
                return iriClass == cachedClass;

            Set<IriClass> set = missmatches.get(iri);

            if(set != null && set.contains(iriClass))
                return false;

            if(iriClass.match(getStatement(), iri))
            {
                if(set != null)
                    missmatches.remove(iri);

                List<Column> columns = iriClass.toColumns(statement, iri);
                iriCache.storeToCache(iri, iriClass, columns);

                return true;
            }
            else
            {
                if(set == null)
                {
                    set = new HashSet<IriClass>();
                    missmatches.put(iri, set);
                }

                set.add(iriClass);

                return false;
            }
        }

        return resClass.match(getStatement(), node);
    }


    public List<Column> getColumns(ResourceClass resClass, Node node)
    {
        if(resClass instanceof IriClass iriClass && node instanceof IRI iri)
            return getColumns(iriClass, iri);

        return resClass.toColumns(getStatement(), node);
    }


    public List<Column> getColumns(IriClass iriClass, IRI iri)
    {
        List<Column> columns = iriCache.getIriColumns(iri);

        if(columns != null)
            return columns;

        columns = config.getIriCache().getIriColumns(iri);

        if(columns != null)
            return columns;

        iriCache.storeToCache(iri, iriClass, columns);

        return iriClass.toColumns(getStatement(), iri);
    }


    public String getIriPrefix(IriClass iriClass, List<Column> columns)
    {
        IRI iri = iriCache.getIri(iriClass, columns);

        if(iri != null)
            return iri.getValue();

        iri = config.getIriCache().getIri(iriClass, columns);

        if(iri != null)
            return iri.getValue();

        return iriClass.getPrefix(columns);
    }
}
