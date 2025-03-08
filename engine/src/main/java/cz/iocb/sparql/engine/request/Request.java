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
import cz.iocb.sparql.engine.mapping.classes.IriClass;
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
import cz.iocb.sparql.engine.translator.ServiceException;
import cz.iocb.sparql.engine.translator.TranslateVisitor;
import cz.iocb.sparql.engine.translator.imcode.SqlSelect;



public class Request implements AutoCloseable
{
    public static class PreparedQuery
    {
        private final String query;
        private final List<DataSet> dataSets;
        private final Query syntaxTree;
        private final List<TranslateMessage> messages;
        private final ResultType type;

        public PreparedQuery(String query, List<DataSet> dataSets, Query syntaxTree, List<TranslateMessage> messages)
                throws TranslateExceptions
        {
            this.query = query;
            this.dataSets = dataSets;
            this.syntaxTree = syntaxTree;
            this.messages = messages;

            this.type = switch(syntaxTree)
            {
                case SelectQuery s -> ResultType.SELECT;
                case AskQuery a -> ResultType.ASK;
                case DescribeQuery d -> ResultType.DESCRIBE;
                case ConstructQuery c -> ResultType.CONSTRUCT;
                default -> null;
            };
        }

        public final String getQuery()
        {
            return query;
        }

        public final List<DataSet> getDataSets()
        {
            return dataSets;
        }

        public final Query getSyntaxTree()
        {
            return syntaxTree;
        }

        public final List<TranslateMessage> getMessages()
        {
            return messages;
        }

        public final ResultType getResultType()
        {
            return type;
        }
    }


    private static final Logger logger = LoggerFactory.getLogger(Request.class);

    private final SparqlDatabaseConfiguration config;

    private final IriCache iriCache = new IriCache(10000);
    private final Map<IRI, Set<IriClass>> missmatches = new HashMap<IRI, Set<IriClass>>();

    private Connection connection;
    private Statement statement;
    private ColumnMap columnMap = new ColumnMap();
    private List<Table> tables = new ArrayList<Table>();

    private long begin;
    private long timeout;
    private boolean canceled;


    public Request(SparqlDatabaseConfiguration config)
    {
        this.config = config;
    }


    public List<TranslateMessage> check(String query, List<DataSet> dataSets, long timeout)
    {
        List<TranslateMessage> messages = new LinkedList<TranslateMessage>();

        try
        {
            MDC.put("sparql", query);

            Parser parser = new Parser(messages);
            ParserRuleContext context = parser.parse(query);

            if(hasErrors(messages))
                return messages;

            QueryVisitor queryVisitor = new QueryVisitor(getConfiguration(), messages);
            Query syntaxTree = queryVisitor.visit(context);

            if(hasErrors(messages))
                return messages;

            if(dataSets != null && !dataSets.isEmpty())
                syntaxTree.getSelect().setDataSets(dataSets);

            logger.trace("query check");
        }
        catch(Throwable e)
        {
            logger.error("sparql check error: " + e.getMessage(), e);
        }
        finally
        {
            MDC.remove("sparql");
        }

        return messages;
    }


    public List<TranslateMessage> check(String query)
    {
        return check(query, null, 0);
    }


    public PreparedQuery prepareQuery(String query, List<DataSet> dataSets) throws TranslateExceptions
    {
        try
        {
            MDC.put("sparql", query);

            List<TranslateMessage> messages = new LinkedList<TranslateMessage>();

            Parser parser = new Parser(messages);
            ParserRuleContext context = parser.parse(query);

            checkForErrors(messages);

            QueryVisitor queryVisitor = new QueryVisitor(getConfiguration(), messages);
            Query syntaxTree = queryVisitor.visit(context);

            checkForErrors(messages);

            if(dataSets != null && !dataSets.isEmpty())
                syntaxTree.getSelect().setDataSets(dataSets);

            return new PreparedQuery(query, dataSets, syntaxTree, messages);
        }
        catch(TranslateExceptions e)
        {
            logger.info("query translation error: " + e.getMessage());
            throw e;
        }
        catch(Throwable e)
        {
            logger.error("unexpected error: " + e.getMessage(), e);
            throw e;
        }
        finally
        {
            MDC.remove("sparql");
        }
    }



    public Result execute(PreparedQuery query, List<String> order, int offset, int limit, int fetchSize, long timeout)
            throws SQLException, ServiceException
    {
        try
        {
            MDC.put("sparql", query.getQuery());

            Query syntaxTree = query.getSyntaxTree();
            ResultType type = query.getResultType();


            int border = syntaxTree instanceof ConstructQuery cnst ? fetchSize / cnst.getTemplates().size() : fetchSize;
            Select select = syntaxTree.getSelect();

            if(syntaxTree instanceof AskQuery)
                fetchSize = 0;
            else if(syntaxTree instanceof DescribeQuery)
                fetchSize = 0;

            if(limit >= 0 && limit <= fetchSize)
                fetchSize = 0;
            else if(select.getLimit() != null && select.getLimit().compareTo(BigInteger.valueOf(border)) <= 0)
                fetchSize = 0;
            else if(select.getGroupByConditions().isEmpty() && select.isInAggregateMode())
                fetchSize = 0;


            getConnection(); // time is measured after a connection is established

            this.timeout = timeout;
            this.begin = System.nanoTime();

            BigInteger newOffset = syntaxTree instanceof AskQuery || offset <= 0 ? null : BigInteger.valueOf(offset);
            BigInteger newLimit = syntaxTree instanceof AskQuery || limit <= 0 ? null : BigInteger.valueOf(limit);

            TranslateVisitor translateVisitor = new TranslateVisitor(this);
            SqlSelect imcode = translateVisitor.translate(syntaxTree, newOffset, newLimit, order, true);

            String code = imcode.translate(this);

            MDC.put("sql", code);

            logger.trace("query evaluation");

            return new Result(type, imcode.getResultDescription(), getStatement(fetchSize).executeQuery(code), begin,
                    timeout);
        }
        catch(SQLException e)
        {
            if(e.getErrorCode() == 0 && "57014".equals(e.getSQLState()))
                logger.warn("query timeout: " + e.getMessage());
            else
                logger.error("query evaluation error: " + e.getMessage());

            if(statement != null)
                statement.close();

            throw e;
        }
        catch(ServiceException e)
        {
            logger.error("query evaluation error: " + e.getMessage());

            if(statement != null)
                statement.close();

            throw e;
        }
        catch(Throwable e)
        {
            logger.error("unexpected error: " + e.getMessage(), e);

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


    public Result execute(String query, List<DataSet> dataSets, List<String> order, int offset, int limit,
            int fetchSize, long timeout) throws TranslateExceptions, SQLException, ServiceException
    {
        return execute(prepareQuery(query, dataSets), order, offset, limit, fetchSize, timeout);
    }


    public Result execute(String query, List<DataSet> dataSets, int offset, int limit, int fetchSize, long timeout)
            throws TranslateExceptions, SQLException, ServiceException
    {
        return execute(query, dataSets, List.of(), offset, limit, fetchSize, timeout);
    }


    public Result execute(String query) throws TranslateExceptions, SQLException, ServiceException
    {
        return execute(query, null, 0, -1, 0, 0);
    }


    public Result execute(String query, List<DataSet> dataSets)
            throws TranslateExceptions, SQLException, ServiceException
    {
        return execute(query, dataSets, 0, -1, 0, 0);
    }


    public Result execute(String query, int offset, int limit, long timeout)
            throws TranslateExceptions, SQLException, ServiceException
    {
        return execute(query, null, offset, limit, 0, timeout);
    }


    private static boolean hasErrors(List<TranslateMessage> messages)
    {
        return messages.stream().anyMatch(m -> m.getCategory() == MessageCategory.ERROR);
    }


    private static void checkForErrors(List<TranslateMessage> messages) throws TranslateExceptions
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


    public synchronized Statement getStatement(int fetchSize)
    {
        try
        {
            if(canceled)
                throw new SQLException("query was canceled");

            if(statement == null)
                statement = getConnection().createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);

            statement.setFetchSize(fetchSize);
            statement.setQueryTimeout(getStatementTimeout());

            return statement;
        }
        catch(SQLException e)
        {
            throw new SQLRuntimeException(e);
        }
    }


    public synchronized Statement getStatement()
    {
        return getStatement(0);
    }


    public SparqlDatabaseConfiguration getConfiguration()
    {
        return config;
    }


    public ColumnMap getColumnMap()
    {
        return columnMap;
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
        return switch(value)
        {
            case Literal lit -> lit.isTypeSupported() ? lit.getDataType().getResourceClass(lit) : unsupportedLiteral;
            case IRI iri -> getIriClass(iri);
            case BlankNodeLiteral bn -> bn.getResourceClass();
            default -> null;
        };
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
