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
import cz.iocb.sparql.engine.database.AliasTable;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.database.SQLRuntimeException;
import cz.iocb.sparql.engine.error.MessageCategory;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.error.TranslateMessage;
import cz.iocb.sparql.engine.imcode.SqlSelect;
import cz.iocb.sparql.engine.mapping.classes.BuiltinClasses;
import cz.iocb.sparql.engine.mapping.classes.IntBlankNodeInSegmentClass;
import cz.iocb.sparql.engine.mapping.classes.IriClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.mapping.classes.StrBlankNodeInSegmentClass;
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



/**
 * Processing of one SPARQL request: parsing and checking the query ({@link #prepareQuery}), translating it to SQL and
 * running it ({@link #execute}), on a single database connection with a per-request IRI cache. The request is closed
 * after the {@link Result} has been consumed.
 */
public class Request implements AutoCloseable
{
    /**
     * Parsed and checked query together with its dataset clauses, messages and result type.
     */
    public static class PreparedQuery
    {
        /**
         * Query text.
         */
        private final String query;

        /**
         * Dataset clauses given by the protocol, or null.
         */
        private final List<DataSet> dataSets;

        /**
         * Parsed query.
         */
        private final Query syntaxTree;

        /**
         * Messages collected during parsing (warnings only, since errors are thrown).
         */
        private final List<TranslateMessage> messages;

        /**
         * Form of the query.
         */
        private final ResultType type;

        /**
         * Creates the prepared query.
         *
         * @param query the query text
         * @param dataSets dataset clauses given by the protocol, or null
         * @param syntaxTree the parsed query
         * @param messages messages collected during parsing
         * @throws TranslateExceptions if the query has errors
         */
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


        /**
         * Query text.
         *
         * @return query text
         */
        public final String getQuery()
        {
            return query;
        }


        /**
         * Dataset clauses given by the protocol, or null.
         *
         * @return dataset clauses given by the protocol, or null
         */
        public final List<DataSet> getDataSets()
        {
            return dataSets;
        }


        /**
         * Parsed query.
         *
         * @return parsed query
         */
        public final Query getSyntaxTree()
        {
            return syntaxTree;
        }


        /**
         * Messages collected during parsing.
         *
         * @return messages collected during parsing
         */
        public final List<TranslateMessage> getMessages()
        {
            return messages;
        }


        /**
         * Form of the query.
         *
         * @return form of the query
         */
        public final ResultType getResultType()
        {
            return type;
        }
    }


    /**
     * Logger of the request processing.
     */
    private static final Logger logger = LoggerFactory.getLogger(Request.class);

    /**
     * Configuration of the endpoint.
     */
    private final SparqlDatabaseConfiguration config;

    /**
     * Whether SERVICE calls may be evaluated independently of their context.
     */
    private final boolean serviceReorder;

    /**
     * Cache of IRI class detections private to this request.
     */
    private final IriCache iriCache = new IriCache(10000);

    /**
     * IRI classes already found not to match an IRI, to avoid repeating database lookups.
     */
    private final Map<Iri, Set<IriClass>> missmatches = new HashMap<>();

    /**
     * Database connection, opened on first use.
     */
    private Connection connection;

    /**
     * Statement of the request, created on first use.
     */
    private Statement statement;

    /**
     * Map shortening the generated column names.
     */
    private ColumnMap columnMap = new ColumnMap();

    /**
     * Temporary tables to drop when closing.
     */
    private List<DatabaseTable> tables = new ArrayList<>();

    /**
     * Counter of LATERAL aliases.
     */
    private int lateralId = 0;

    /**
     * Start of the execution in {@link System#nanoTime} units.
     */
    private long begin;

    /**
     * Time limit of the execution in nanoseconds; 0 for none.
     */
    private long timeout;

    /**
     * Whether the request was cancelled.
     */
    private boolean canceled;


    /**
     * Creates a request; with {@code serviceReorder}, SERVICE calls may be evaluated independently of their context and
     * joined afterwards.
     *
     * @param config the endpoint configuration
     * @param serviceReorder whether SERVICE calls may be evaluated independently of their context
     */
    public Request(SparqlDatabaseConfiguration config, boolean serviceReorder)
    {
        this.config = config;
        this.serviceReorder = serviceReorder;
    }


    /**
     * Creates a request without service reordering.
     *
     * @param config the endpoint configuration
     */
    public Request(SparqlDatabaseConfiguration config)
    {
        this(config, false);
    }


    /**
     * Parses and checks the query without executing it, returning all errors and warnings.
     *
     * @param query the query text
     * @param dataSets the dataset clauses
     * @param timeout time limit in nanoseconds, 0 for none
     * @return all errors and warnings
     */
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


    /**
     * Parses and checks the query without executing it, returning all errors and warnings.
     *
     * @param query the query text
     * @return all errors and warnings
     */
    public List<TranslateMessage> check(String query)
    {
        return check(query, null, 0);
    }


    /**
     * Parses and checks the query; {@code dataSets} (protocol default-graph-uri and named-graph-uri parameters)
     * override the FROM clauses of the query.
     *
     * @param query the query text
     * @param dataSets the dataset clauses
     * @return the prepared query
     * @throws TranslateExceptions if the query has errors
     */
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



    /**
     * Translates, optimises and runs the query.
     *
     * @param query the query text
     * @param order variables to order the results by, applied on top of the query's own ORDER BY
     * @param offset number of results to skip on top of the query's own OFFSET (ignored if not positive)
     * @param limit maximum number of results on top of the query's own LIMIT (ignored if not positive)
     * @param fetchSize JDBC fetch size; 0 fetches everything at once (forced for small or aggregated results)
     * @param timeout time limit in nanoseconds for the whole execution including fetching; 0 for none
     * @param sqlSizeLimit maximum length of the generated SQL; 0 for none
     * @return the result cursor
     * @throws LimitExceedException if the generated SQL is longer than {@code sqlSizeLimit}
     * @throws SQLException on database errors, including query timeout (SQL state 57014)
     * @throws ServiceException if a federated SERVICE call fails
     */
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


    /**
     * Prepares and runs the query, see {@link #execute(PreparedQuery, List, int, int, int, long, int)}.
     *
     * @param query the query text
     * @param dataSets the dataset clauses
     * @param order variables to order the results by, on top of the query's own ORDER BY
     * @param offset the offset, or null
     * @param limit the limit, or null
     * @param fetchSize the JDBC fetch size
     * @param timeout time limit in nanoseconds, 0 for none
     * @return the result cursor
     * @throws TranslateExceptions if the query has errors
     * @throws LimitExceedException if a configured limit is exceeded
     * @throws SQLException on database errors
     * @throws ServiceException if a federated SERVICE call fails
     */
    public Result execute(String query, List<DataSet> dataSets, List<Variable> order, int offset, int limit,
            int fetchSize, long timeout)
            throws TranslateExceptions, LimitExceedException, SQLException, ServiceException
    {
        return execute(prepareQuery(query, dataSets), order, offset, limit, fetchSize, timeout, 0);
    }


    /**
     * Prepares and runs the query, see {@link #execute(PreparedQuery, List, int, int, int, long, int)}.
     *
     * @param query the query text
     * @param dataSets the dataset clauses
     * @param offset the offset, or null
     * @param limit the limit, or null
     * @param fetchSize the JDBC fetch size
     * @param timeout time limit in nanoseconds, 0 for none
     * @return the result cursor
     * @throws TranslateExceptions if the query has errors
     * @throws LimitExceedException if a configured limit is exceeded
     * @throws SQLException on database errors
     * @throws ServiceException if a federated SERVICE call fails
     */
    public Result execute(String query, List<DataSet> dataSets, int offset, int limit, int fetchSize, long timeout)
            throws TranslateExceptions, LimitExceedException, SQLException, ServiceException
    {
        return execute(query, dataSets, List.of(), offset, limit, fetchSize, timeout);
    }


    /**
     * Prepares and runs the query with no limits and no timeout.
     *
     * @param query the query text
     * @return the result cursor
     * @throws TranslateExceptions if the query has errors
     * @throws LimitExceedException if a configured limit is exceeded
     * @throws SQLException on database errors
     * @throws ServiceException if a federated SERVICE call fails
     */
    public Result execute(String query) throws TranslateExceptions, LimitExceedException, SQLException, ServiceException
    {
        return execute(query, null, 0, -1, 0, 0);
    }


    /**
     * Prepares and runs the query with no limits and no timeout.
     *
     * @param query the query text
     * @param dataSets the dataset clauses
     * @return the result cursor
     * @throws TranslateExceptions if the query has errors
     * @throws LimitExceedException if a configured limit is exceeded
     * @throws SQLException on database errors
     * @throws ServiceException if a federated SERVICE call fails
     */
    public Result execute(String query, List<DataSet> dataSets)
            throws TranslateExceptions, LimitExceedException, SQLException, ServiceException
    {
        return execute(query, dataSets, 0, -1, 0, 0);
    }


    /**
     * Prepares and runs the query, see {@link #execute(PreparedQuery, List, int, int, int, long, int)}.
     *
     * @param query the query text
     * @param offset the offset, or null
     * @param limit the limit, or null
     * @param timeout time limit in nanoseconds, 0 for none
     * @return the result cursor
     * @throws TranslateExceptions if the query has errors
     * @throws LimitExceedException if a configured limit is exceeded
     * @throws SQLException on database errors
     * @throws ServiceException if a federated SERVICE call fails
     */
    public Result execute(String query, int offset, int limit, long timeout)
            throws TranslateExceptions, LimitExceedException, SQLException, ServiceException
    {
        return execute(query, null, offset, limit, 0, timeout);
    }


    /**
     * True if some message is an error.
     *
     * @param messages the messages to check
     * @return true if some message is an error, false otherwise
     */
    private static boolean hasErrors(List<TranslateMessage> messages)
    {
        return messages.stream().anyMatch(m -> m.getCategory() == MessageCategory.ERROR);
    }


    /**
     * Throws the error messages, if there are any.
     *
     * @param messages the messages to check
     * @throws TranslateExceptions if the query has errors
     */
    private static void checkForErrors(List<TranslateMessage> messages) throws TranslateExceptions
    {
        List<TranslateMessage> errors = messages.stream().filter(m -> m.getCategory() == MessageCategory.ERROR)
                .toList();

        if(!errors.isEmpty())
            throw new TranslateExceptions(errors);
    }


    /**
     * Cancels the running database statement; the request cannot be used afterwards.
     *
     * @throws SQLException on database errors
     */
    public synchronized void cancel() throws SQLException
    {
        canceled = true;

        if(statement != null && !statement.isClosed())
        {
            statement.cancel();
            statement.close();
        }
    }


    /**
     * Closes the statement, drops the temporary tables created for SERVICE results, rolls back and closes the
     * connection.
     */
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
                for(DatabaseTable table : tables)
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


    /**
     * The connection of the request, opened without auto-commit on first use.
     *
     * @return the connection of the request, opened without auto-commit on first use
     * @throws SQLException on database errors
     */
    private synchronized Connection getConnection() throws SQLException
    {
        if(connection == null)
        {
            connection = config.getConnectionPool().getConnection();
            connection.setAutoCommit(false);
        }

        return connection;
    }


    /**
     * Remaining time of the timeout in whole seconds for the statement; 0 for no timeout, 1 when already elapsed.
     *
     * @return remaining time of the timeout in whole seconds for the statement; 0 for no timeout, 1 when already
     *         elapsed
     */
    private int getStatementTimeout()
    {
        if(timeout == 0)
            return 0;

        int restTime = (int) ((timeout - (System.nanoTime() - begin)) / 1000000000);

        if(restTime <= 0)
            return 1; //FIXME: throw exception

        return restTime;
    }


    /**
     * The statement of the request (created on first use) with the given fetch size and the remaining part of the
     * timeout set.
     *
     * @param fetchSize the JDBC fetch size
     * @return the statement of the request (created on first use) with the given fetch size and the remaining part of
     *         the timeout set
     */
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


    /**
     * The statement of the request with no fetch size limit.
     *
     * @return the statement of the request with no fetch size limit
     */
    public synchronized Statement getStatement()
    {
        return getStatement(0);
    }


    /**
     * Configuration of the endpoint.
     *
     * @return configuration of the endpoint
     */
    public SparqlDatabaseConfiguration getConfiguration()
    {
        return config;
    }


    /**
     * Map shortening the generated column names of this request.
     *
     * @return map shortening the generated column names of this request
     */
    public ColumnMap getColumnMap()
    {
        return columnMap;
    }


    /**
     * Fresh alias for a LATERAL subquery.
     *
     * @return fresh alias for a LATERAL subquery
     */
    public AliasTable createLateralTable()
    {
        return new AliasTable("lateral" + lateralId++);
    }


    /**
     * Start of the execution in {@link System#nanoTime} units.
     *
     * @return start of the execution in {@link System#nanoTime} units
     */
    public long getBegin()
    {
        return begin;
    }


    /**
     * Time limit of the execution in nanoseconds; 0 for none.
     *
     * @return time limit of the execution in nanoseconds; 0 for none
     */
    public long getTimeout()
    {
        return timeout;
    }


    /**
     * Most specific resource class of a constant term (null for a variable).
     *
     * @param term the RDF term
     * @return most specific resource class of a constant term (null for a variable)
     */
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


    /**
     * Class of the IRI: the first user IRI class that matches it (in check-cost order), or the unsupported IRI class;
     * detections are cached.
     *
     * @param value the value
     * @return class of the IRI: the first user IRI class that matches it (in check-cost order), or the unsupported IRI
     *         class; detections are cached
     */
    public ResourceClass getIriClass(Iri value)
    {
        ResourceClass iriClass = iriCache.getIriClass(value);

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


    /**
     * Class of the literal according to its datatype; the unsupported literal class for unknown datatypes or invalid
     * lexical forms.
     *
     * @param literal the literal
     * @return class of the literal according to its datatype; the unsupported literal class for unknown datatypes or
     *         invalid lexical forms
     */
    public ResourceClass getLiteralClass(Literal literal)
    {
        Datatype datatype = getConfiguration().getDatatype(literal.getType());

        if(datatype == null || !datatype.isValidForm(literal.getValue()))
            return BuiltinClasses.unsupportedType;

        return datatype.getResourceClass(literal);
    }


    /**
     * Class of the blank node: the in-segment class of its segment.
     *
     * @param bnode the blank node
     * @return class of the blank node: the in-segment class of its segment
     */
    public ResourceClass getBlankNodeClass(BlankNode bnode)
    {
        //TODO use cache

        return switch(bnode)
        {
            case StrBlankNode s -> new StrBlankNodeInSegmentClass(s.getSegment());
            case IntBlankNode i -> new IntBlankNodeInSegmentClass(i.getSegment());
            default -> throw new IllegalArgumentException();
        };
    }


    /**
     * The first user IRI class matching the IRI, in check-cost order, or the unsupported IRI class.
     *
     * @param value the value
     * @return the first user IRI class matching the IRI, in check-cost order, or the unsupported IRI class
     */
    private ResourceClass detectIriClass(Iri value)
    {
        for(UserIriClass iriClass : getConfiguration().getIriClasses())
            if(iriClass.match(getStatement(), value))
                return iriClass;

        return BuiltinClasses.unsupportedIri;
    }


    /**
     * True if the term is representable in the class; for IRIs, the answer is served from the caches and negative
     * answers are remembered, since matching may query the database.
     *
     * @param resClass the resource class
     * @param term the RDF term
     * @return true if the term is representable in the class, false otherwise
     */
    public boolean match(ResourceClass resClass, RdfTerm term)
    {
        if(resClass instanceof IriClass iriClass && term instanceof Iri iri)
        {
            ResourceClass cachedClass = iriCache.getIriClass(iri);

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


    /**
     * Constant columns representing the term in the class, served from the IRI caches when possible.
     *
     * @param resClass the resource class
     * @param term the RDF term
     * @return constant columns representing the term in the class, served from the IRI caches when possible
     */
    public List<Column> getColumns(ResourceClass resClass, RdfTerm term)
    {
        if(resClass instanceof IriClass iriClass && term instanceof Iri iri)
            return getColumns(iriClass, iri);

        return resClass.toColumns(getStatement(), term);
    }


    /**
     * Constant columns representing the IRI in the class, served from the IRI caches when possible.
     *
     * @param iriClass the IRI class
     * @param iri the IRI
     * @return constant columns representing the IRI in the class, served from the IRI caches when possible
     */
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


    /**
     * The IRI represented by the columns if it is cached, otherwise the prefix the class can guarantee for them.
     *
     * @param iriClass the IRI class
     * @param columns the columns
     * @return the IRI represented by the columns if it is cached, otherwise the prefix the class can guarantee for them
     */
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


    /**
     * True if SERVICE calls may be evaluated independently of their context and joined afterwards.
     *
     * @return true if SERVICE calls may be evaluated independently of their context and joined afterwards, false
     *         otherwise
     */
    public boolean isServiceReorderEnabled()
    {
        return serviceReorder;
    }
}
