package cz.iocb.sparql.engine.request;

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
import cz.iocb.sparql.engine.imcode.SqlSelect;
import cz.iocb.sparql.engine.mapping.classes.BlankNodeClass;
import cz.iocb.sparql.engine.mapping.classes.BuiltinClasses;
import cz.iocb.sparql.engine.mapping.classes.IntBlankNodeConstantSegmentClass;
import cz.iocb.sparql.engine.mapping.classes.IriClass;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.mapping.classes.StrBlankNodeConstantSegmentClass;
import cz.iocb.sparql.engine.mapping.classes.UserIriClass;
import cz.iocb.sparql.engine.mapping.datatypes.Datatype;
import cz.iocb.sparql.engine.model.AskQuery;
import cz.iocb.sparql.engine.model.ConstructQuery;
import cz.iocb.sparql.engine.model.DataSet;
import cz.iocb.sparql.engine.model.DescribeQuery;
import cz.iocb.sparql.engine.model.Query;
import cz.iocb.sparql.engine.model.Select;
import cz.iocb.sparql.engine.model.SelectQuery;
import cz.iocb.sparql.engine.parser.Parser;
import cz.iocb.sparql.engine.parser.QueryVisitor;
import cz.iocb.sparql.engine.rdf.BlankNode;
import cz.iocb.sparql.engine.rdf.IntBlankNode;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.Literal;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.StrBlankNode;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.Result.ResultType;
import cz.iocb.sparql.engine.translator.ServiceException;
import cz.iocb.sparql.engine.translator.TranslateVisitor;



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
                case SelectQuery _ -> ResultType.SELECT;
                case AskQuery _ -> ResultType.ASK;
                case DescribeQuery _ -> ResultType.DESCRIBE;
                case ConstructQuery _ -> ResultType.CONSTRUCT;
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
    private final boolean serviceReorder;

    private final IriCache iriCache = new IriCache(10000);
    private final Map<Iri, Set<IriClass>> missmatches = new HashMap<>();

    private Connection connection;
    private Statement statement;
    private ColumnMap columnMap = new ColumnMap();
    private List<Table> tables = new ArrayList<>();

    private long begin;
    private long timeout;
    private boolean canceled;


    public Request(SparqlDatabaseConfiguration config, boolean serviceReorder)
    {
        this.config = config;
        this.serviceReorder = serviceReorder;
    }


    public Request(SparqlDatabaseConfiguration config)
    {
        this(config, false);
    }


    public List<TranslateMessage> check(String query, List<DataSet> dataSets, long timeout)
    {
        List<TranslateMessage> messages = new LinkedList<>();

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

            List<TranslateMessage> messages = new LinkedList<>();

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



    public Result execute(PreparedQuery query, List<Variable> order, int offset, int limit, int fetchSize, long timeout,
            int sqlSizeLimit) throws LimitExceedException, SQLException, ServiceException
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
            SqlSelect imcode = translateVisitor.translate(syntaxTree, newOffset, newLimit, order);

            // optimize
            imcode = imcode.optimize(this, false);

            // evaluate service calls
            imcode = imcode.optimize(this, true);

            String code = imcode.translate(this);

            /*
            System.err.println();
            System.err.println();
            System.err.println();
            System.err.println(query.getQuery());
            System.err.println();
            System.err.println(imcode.getExplanation());
            System.err.println();
            System.err.println(code);
            System.err.println();
            */

            if(sqlSizeLimit > 0 && code.length() > sqlSizeLimit)
                throw new LimitExceedException("generated SQL query exceeded the maximum allowed limit");

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


    public Result execute(String query, List<DataSet> dataSets, List<Variable> order, int offset, int limit,
            int fetchSize, long timeout)
            throws TranslateExceptions, LimitExceedException, SQLException, ServiceException
    {
        return execute(prepareQuery(query, dataSets), order, offset, limit, fetchSize, timeout, 0);
    }


    public Result execute(String query, List<DataSet> dataSets, int offset, int limit, int fetchSize, long timeout)
            throws TranslateExceptions, LimitExceedException, SQLException, ServiceException
    {
        return execute(query, dataSets, List.of(), offset, limit, fetchSize, timeout);
    }


    public Result execute(String query) throws TranslateExceptions, LimitExceedException, SQLException, ServiceException
    {
        return execute(query, null, 0, -1, 0, 0);
    }


    public Result execute(String query, List<DataSet> dataSets)
            throws TranslateExceptions, LimitExceedException, SQLException, ServiceException
    {
        return execute(query, dataSets, 0, -1, 0, 0);
    }


    public Result execute(String query, int offset, int limit, long timeout)
            throws TranslateExceptions, LimitExceedException, SQLException, ServiceException
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
                .toList();

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
            return 1; //FIXME: throw exception

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


    public ResourceClass getResourceClass(RdfTerm term)
    {
        return switch(term)
        {
            case Literal lit -> getLiteralClass(lit);
            case Iri iri -> getIriClass(iri);
            case BlankNode bn -> getBlankNodeClass(bn);
            default -> null;
        };
    }


    public IriClass getIriClass(Iri value)
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


    public LiteralClass getLiteralClass(Literal literal)
    {
        Datatype datatype = getConfiguration().getDatatype(literal.getType());

        //FIXME: language tags?

        if(datatype == null || !datatype.isValidForm(literal.getValue()))
            return BuiltinClasses.unsupportedLiteral;

        return datatype.getResourceClass(literal);
    }


    public BlankNodeClass getBlankNodeClass(BlankNode bnode)
    {
        //TODO use cache

        return switch(bnode)
        {
            case StrBlankNode s -> new StrBlankNodeConstantSegmentClass(s.getSegment());
            case IntBlankNode i -> new IntBlankNodeConstantSegmentClass(i.getSegment());
            default -> throw new IllegalArgumentException();
        };
    }


    private IriClass detectIriClass(Iri value)
    {
        for(UserIriClass iriClass : getConfiguration().getIriClasses())
            if(iriClass.match(getStatement(), value))
                return iriClass;

        return BuiltinClasses.unsupportedIri;
    }


    public boolean match(ResourceClass resClass, RdfTerm term)
    {
        if(resClass instanceof IriClass iriClass && term instanceof Iri iri)
        {
            IriClass cachedClass = iriCache.getIriClass(iri);

            if(cachedClass != null)
                return iriClass.equals(cachedClass);

            cachedClass = config.getIriCache().getIriClass(iri);

            if(cachedClass != null)
                return iriClass.equals(cachedClass);

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
                    set = new HashSet<>();
                    missmatches.put(iri, set);
                }

                set.add(iriClass);

                return false;
            }
        }

        return resClass.match(getStatement(), term);
    }


    public List<Column> getColumns(ResourceClass resClass, RdfTerm term)
    {
        if(resClass instanceof IriClass iriClass && term instanceof Iri iri)
            return getColumns(iriClass, iri);

        return resClass.toColumns(getStatement(), term);
    }


    public List<Column> getColumns(IriClass iriClass, Iri iri)
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
        Iri iri = iriCache.getIri(iriClass, columns);

        if(iri != null)
            return iri.getValue();

        iri = config.getIriCache().getIri(iriClass, columns);

        if(iri != null)
            return iri.getValue();

        return iriClass.getPrefix(columns);
    }


    public boolean isServiceReorderEnabled()
    {
        return serviceReorder;
    }
}
