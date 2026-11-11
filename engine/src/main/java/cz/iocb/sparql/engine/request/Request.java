package cz.iocb.sparql.engine.request;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.antlr.v4.runtime.ParserRuleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.AliasTable;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.database.SQLRuntimeException;
import cz.iocb.sparql.engine.database.VirtualTable;
import cz.iocb.sparql.engine.error.MessageCategory;
import cz.iocb.sparql.engine.error.MessageType;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.error.TranslateMessage;
import cz.iocb.sparql.engine.imcode.SqlSelect;
import cz.iocb.sparql.engine.imcode.SqlShared;
import cz.iocb.sparql.engine.imcode.expression.SqlExists;
import cz.iocb.sparql.engine.imcode.expression.SqlOuterVariable;
import cz.iocb.sparql.engine.mapping.classes.BuiltinClasses;
import cz.iocb.sparql.engine.mapping.classes.DerivedClass;
import cz.iocb.sparql.engine.mapping.classes.IntBlankNodeInSegmentClass;
import cz.iocb.sparql.engine.mapping.classes.IriClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.mapping.classes.StrBlankNodeInSegmentClass;
import cz.iocb.sparql.engine.mapping.classes.TripleTermClass;
import cz.iocb.sparql.engine.mapping.classes.UserIriClass;
import cz.iocb.sparql.engine.mapping.datatypes.Datatype;
import cz.iocb.sparql.engine.model.AskQuery;
import cz.iocb.sparql.engine.model.ConstructQuery;
import cz.iocb.sparql.engine.model.DataSet;
import cz.iocb.sparql.engine.model.DescribeQuery;
import cz.iocb.sparql.engine.model.Prologue;
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
import cz.iocb.sparql.engine.rdf.TripleTerm;
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.Result.ResultType;
import cz.iocb.sparql.engine.translator.ServiceException;
import cz.iocb.sparql.engine.translator.ServiceRuntimeException;
import cz.iocb.sparql.engine.translator.TranslateVisitor;
import cz.iocb.sparql.engine.translator.VariableBindings;



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
         * Version label in effect: the one given by the protocol, else the one declared by the query; null when none.
         */
        private final String version;

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
         * @param version version label in effect, or null
         * @throws TranslateExceptions if the query has errors
         */
        public PreparedQuery(String query, List<DataSet> dataSets, Query syntaxTree, List<TranslateMessage> messages,
                String version) throws TranslateExceptions
        {
            this.query = query;
            this.dataSets = dataSets;
            this.syntaxTree = syntaxTree;
            this.messages = messages;
            this.version = version;

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


        /**
         * Version label in effect: the one given by the protocol, else the one declared by the query; null when none.
         *
         * @return version label in effect, or null
         */
        public final String getVersion()
        {
            return version;
        }
    }


    /**
     * Logger of the request processing.
     */
    private static final Logger logger = LoggerFactory.getLogger(Request.class);

    /**
     * Pattern of an alias of a common table expression (see {@link #getCommonTableExpression}) in SQL.
     */
    private static final Pattern commonTableExpressionName = Pattern.compile("\"cte[0-9]+\"");

    /**
     * Name of the alias of the input of a node holding expressions, which the depth of the patterns of expressions
     * follows (see {@link #getInputTable}).
     */
    private static final String inputTableName = "outer";

    /**
     * Name of the alias of the left side of a left join (see {@link #getLeftInputTable}).
     */
    private static final String leftInputTableName = "left";

    /**
     * Name of the alias of the right side of a left join (see {@link #getRightInputTable}).
     */
    private static final String rightInputTableName = "right";

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
     * Common table expressions of the query being evaluated, by their queries, in the order they were asked for (see
     * {@link #getCommonTableExpression}).
     */
    private final LinkedHashMap<String, AliasTable> commonTableExpressions = new LinkedHashMap<>();

    /**
     * Virtual tables the queries of the common table expressions read, by the aliases of the expressions.
     */
    private final Map<String, Set<VirtualTable>> commonTableExpressionTables = new HashMap<>();

    /**
     * Depth of the patterns of expressions being optimised (see {@link #isWithinExpression}).
     */
    private int expressionDepth = 0;

    /**
     * Bindings of the current solutions of the patterns of expressions being translated or optimised, innermost last,
     * null for a pattern that has none of its own (see {@link #beginPattern}); their number is the depth of the
     * patterns (see {@link #getInputTable}).
     */
    private final List<VariableBindings> currentSolutions = new ArrayList<>();

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
        return prepareQuery(query, dataSets, null);
    }


    /**
     * Parses and checks the query; {@code dataSets} (protocol default-graph-uri and named-graph-uri parameters)
     * override the FROM clauses of the query, and {@code version} (protocol version parameter) takes precedence over
     * the VERSION declaration of the query, a difference being a warning; a version that is not a SPARQL version label
     * is an error.
     *
     * @param query the query text
     * @param dataSets the dataset clauses, or null
     * @param version the version label given by the protocol, or null
     * @return the prepared query
     * @throws TranslateExceptions if the query has errors
     */
    public PreparedQuery prepareQuery(String query, List<DataSet> dataSets, String version) throws TranslateExceptions
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

            String declared = syntaxTree.getPrologue().getVersion();
            String effective = version != null ? version : declared;

            if(version != null && !Prologue.versionLabels.contains(version))
                messages.add(new TranslateMessage(MessageType.unsupportedVersion, null, version));
            else if(version != null && declared != null && !declared.equals(version))
                messages.add(new TranslateMessage(MessageType.versionMismatch, null, version, declared));

            checkForErrors(messages);

            if(dataSets != null && !dataSets.isEmpty())
                syntaxTree.getSelect().setDataSets(dataSets);

            return new PreparedQuery(query, dataSets, syntaxTree, messages, effective);
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

            commonTableExpressions.clear();
            commonTableExpressionTables.clear();

            TranslateVisitor translateVisitor = new TranslateVisitor(this);
            SqlSelect imcode = translateVisitor.translate(syntaxTree, newOffset, newLimit, order);
            String code;

            try
            {
                // optimize
                imcode = imcode.optimize(this, false);

                // evaluate service calls
                imcode = imcode.optimize(this, true);

                code = imcode.translate(this);
            }
            catch(ServiceRuntimeException e)
            {
                // the unchecked wrappers of the intermediate code are unwrapped at the border of the request
                throw(ServiceException) e.getCause();
            }
            catch(SQLRuntimeException e)
            {
                throw(SQLException) e.getCause();
            }

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

            if(config.isJoinOptimizerEnabled())
            {
                // the joins are written in the order and with the methods the engine has chosen: the planner must keep
                // the order of the explicit joins and hash them (a merge join would sort the values, which is slow for
                // boxes), the index lookups being forced by lateral subqueries (see JoinOptimizer); the settings last
                // until the end of the transaction of the request
                Statement statement = getStatement();
                statement.execute("SET LOCAL join_collapse_limit = 1");
                statement.execute("SET LOCAL enable_nestloop = off");
                statement.execute("SET LOCAL enable_mergejoin = off");
            }

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
     * Begins the optimisation of a pattern of an expression (see {@link #isWithinExpression}).
     */
    public void beginExpression()
    {
        expressionDepth++;
    }


    /**
     * Ends the optimisation of a pattern of an expression (see {@link #beginExpression}).
     */
    public void endExpression()
    {
        expressionDepth--;
    }


    /**
     * True while a pattern of an expression (EXISTS) is being optimised. Only the existence of a solution of such a
     * pattern matters, and the database stops evaluating it at its first solution, so a walk in it is not seeded from
     * shared solutions (see {@link SqlShared#canShare}): the seeds would be computed from the whole shared side before
     * the walk yields anything. The SQL of the pattern is rendered into the expression at once, and the common table
     * expressions it refers to are declared by the statement holding it (see {@link #getCommonTableExpressions}).
     *
     * @return true while a pattern of an expression is being optimised, false otherwise
     */
    public boolean isWithinExpression()
    {
        return expressionDepth > 0;
    }


    /**
     * Begins the translation or the optimisation of a pattern of an expression (EXISTS): the nodes of the pattern lie
     * one level deeper than the node holding the expression (see {@link #getInputTable}). A pattern evaluated for the
     * current solution of the solutions its expression is evaluated for gets the bindings of that solution, from which
     * the expressions of the pattern read its values (see {@link SqlOuterVariable}): the bindings the node holding the
     * expression provides at the moment, joined with the current solution of the pattern the node itself lies in (see
     * {@link SqlExists#getCurrentSolution}). A pattern independent of those solutions has no current solution of its
     * own (null); the current solution of the pattern its expression lies in stays in effect for its nodes, because the
     * optimisation builds such a pattern from nodes of that pattern (the existence test of a leading OPTIONAL).
     *
     * @param solution bindings of the current solution of the pattern, its columns addressed through the aliases of the
     *            inputs of the nodes providing them, or null for a pattern without a current solution of its own
     */
    public void beginPattern(VariableBindings solution)
    {
        currentSolutions.add(solution);
    }


    /**
     * Ends the translation or the optimisation of a pattern of an expression (see {@link #beginPattern}).
     */
    public void endPattern()
    {
        currentSolutions.removeLast();
    }


    /**
     * Bindings of the current solution in effect: that of the innermost pattern of an expression being translated or
     * optimised which has one of its own (see {@link #beginPattern}), or null outside any such pattern.
     *
     * @return bindings of the current solution in effect, or null outside any pattern evaluated for a current solution
     */
    public VariableBindings getCurrentSolution()
    {
        for(int i = currentSolutions.size() - 1; i >= 0; i--)
            if(currentSolutions.get(i) != null)
                return currentSolutions.get(i);

        return null;
    }


    /**
     * True if the SQL refers to the input of a node holding an expression whose pattern is being translated or
     * optimised (see {@link #getInputTable}), so to a value of a current solution: such SQL has a meaning only at its
     * place inside the pattern, it can neither be moved into a common table expression nor explained on its own. The
     * aliases are found in the text, so one spelled in a string constant counts as well, which only keeps the SQL at
     * its place.
     *
     * @param sql the SQL
     * @return true if the SQL refers to the input of a node holding an expression whose pattern is being translated or
     *         optimised, false otherwise
     */
    public boolean refersToEnclosingInput(String sql)
    {
        for(int depth = 0; depth < currentSolutions.size(); depth++)
            for(String name : List.of(inputTableName, leftInputTableName, rightInputTableName))
                if(sql.contains("\"" + name + depth + "\"."))
                    return true;

        return false;
    }


    /**
     * Alias of the input of a node holding expressions (a filter, a bind, an aggregation or a procedure call): a name
     * followed by the depth of the patterns of expressions the node lies in. A pattern of an expression held by the
     * node can refer to the columns of the input through the alias from any depth, because no subquery between such a
     * reference and the node declares the alias again: the subqueries belong to the pattern, whose nodes lie deeper.
     *
     * @return alias of the input of a node holding expressions at the current depth
     */
    public AliasTable getInputTable()
    {
        return new AliasTable(inputTableName + currentSolutions.size());
    }


    /**
     * Alias of the left side of a left join, whose conditions are expressions over the solutions of both sides (see
     * {@link #getInputTable}).
     *
     * @return alias of the left side of a left join at the current depth
     */
    public AliasTable getLeftInputTable()
    {
        return new AliasTable(leftInputTableName + currentSolutions.size());
    }


    /**
     * Alias of the right side of a left join (see {@link #getLeftInputTable}).
     *
     * @return alias of the right side of a left join at the current depth
     */
    public AliasTable getRightInputTable()
    {
        return new AliasTable(rightInputTableName + currentSolutions.size());
    }


    /**
     * Alias of the common table expression that evaluates the query, declared when first asked for, so that a query
     * asked for several times is evaluated once by a statement. Every statement declares the common table expressions
     * its SQL refers to (see {@link #getCommonTableExpressions}), also those referred to by the SQL of an expression,
     * which is rendered at once during the optimisation, before any statement is translated. SQL using the alias has to
     * run under the {@code WITH} clause of {@link SqlSelect#translateWithClause}; code rendering a node outside
     * {@link SqlSelect#translate} prepends the clause and wraps the SQL of the node, which may begin with a
     * {@code WITH RECURSIVE} clause of its own.
     *
     * @param query the query
     * @return alias of the common table expression evaluating the query
     */
    public AliasTable getCommonTableExpression(String query)
    {
        return getCommonTableExpression(query, Set.of());
    }


    /**
     * Alias of the common table expression that evaluates the query reading the virtual tables (see
     * {@link #getCommonTableExpression(String)}); a statement declaring the expression declares the tables too.
     *
     * @param query the query
     * @param tables the virtual tables the query reads
     * @return alias of the common table expression evaluating the query
     */
    public AliasTable getCommonTableExpression(String query, Set<VirtualTable> tables)
    {
        AliasTable table = commonTableExpressions.get(query);

        if(table == null)
        {
            table = new AliasTable("cte" + commonTableExpressions.size());
            commonTableExpressions.put(query, table);
            commonTableExpressionTables.put(table.toString(), Set.copyOf(tables));
        }

        return table;
    }


    /**
     * Virtual tables the queries of the common table expressions the SQL refers to read (see
     * {@link #getCommonTableExpressions}).
     *
     * @param sql the SQL
     * @return virtual tables the common table expressions the SQL refers to read
     */
    public Set<VirtualTable> getCommonTableExpressionTables(String sql)
    {
        Set<VirtualTable> tables = new HashSet<>();

        for(String name : getReferredCommonTableExpressions(sql))
            tables.addAll(commonTableExpressionTables.getOrDefault(name, Set.of()));

        return tables;
    }


    /**
     * Definitions of the common table expressions the SQL refers to, directly or through the definitions of others, as
     * {@code alias AS (query)}, in the order they were asked for: the query of a common table expression is rendered
     * before it is asked for, together with those it refers to, so each one follows the ones it refers to.
     *
     * @param sql the SQL
     * @return definitions of the common table expressions the SQL refers to, in the order they were asked for
     */
    public List<String> getCommonTableExpressions(String sql)
    {
        Set<String> referred = getReferredCommonTableExpressions(sql);

        List<String> definitions = new ArrayList<>(referred.size());

        for(Entry<String, AliasTable> entry : commonTableExpressions.entrySet())
            if(referred.contains(entry.getValue().toString()))
                definitions.add(entry.getValue() + " AS (" + entry.getKey() + ")");

        return definitions;
    }


    /**
     * Aliases of the common table expressions the SQL refers to, directly or through the definitions of others. The
     * aliases are found in the text, so one spelled in a string constant counts as well; its definition is then
     * declared without being read, which is harmless, since its query reads nothing the statement does not declare.
     *
     * @param sql the SQL
     * @return aliases of the common table expressions the SQL refers to
     */
    private Set<String> getReferredCommonTableExpressions(String sql)
    {
        Map<String, Entry<String, AliasTable>> byName = new HashMap<>();

        for(Entry<String, AliasTable> entry : commonTableExpressions.entrySet())
            byName.put(entry.getValue().toString(), entry);

        Set<String> referred = new HashSet<>();
        Deque<String> pending = new ArrayDeque<>(List.of(sql));

        while(!pending.isEmpty())
        {
            Matcher matcher = commonTableExpressionName.matcher(pending.pop());

            while(matcher.find())
            {
                Entry<String, AliasTable> entry = byName.get(matcher.group());

                if(entry != null && referred.add(matcher.group()))
                    pending.push(entry.getKey());
            }
        }

        return referred;
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
     * Most specific resource class of a constant term (null for a variable, or for a triple term with a variable).
     *
     * @param term the RDF term
     * @return most specific resource class of a constant term (null for a variable, or for a triple term with a
     *         variable)
     */
    public ResourceClass getResourceClass(RdfTerm term)
    {
        return switch(term)
        {
            case Literal lit -> getLiteralClass(lit);
            case Iri iri -> getIriClass(iri);
            case BlankNode bn -> getBlankNodeClass(bn);
            case TripleTerm triple -> getTripleTermClass(triple);
            default -> null;
        };
    }


    /**
     * Class of a constant triple term: the triple term class of the most specific classes of its components; null when
     * some component (of a nested triple term too) is a variable.
     *
     * @param triple the triple term
     * @return class of a constant triple term; null when some component is a variable
     */
    public TripleTermClass getTripleTermClass(TripleTerm triple)
    {
        ResourceClass subject = getResourceClass(triple.getSubject());
        ResourceClass predicate = getResourceClass(triple.getPredicate());
        ResourceClass object = getResourceClass(triple.getObject());

        if(subject == null || predicate == null || object == null)
            return null;

        return new TripleTermClass(subject, predicate, object);
    }


    /**
     * Class of the IRI: the intersection of the user IRI classes of the configuration the IRI belongs to, or the
     * unsupported IRI class when it belongs to none (see {@link #detectIriClass(Iri, List)}); detections are cached.
     *
     * @param value the value
     * @return class of the IRI: the intersection of the user IRI classes of the configuration the IRI belongs to, or
     *         the unsupported IRI class when it belongs to none
     */
    public ResourceClass getIriClass(Iri value)
    {
        ResourceClass iriClass = iriCache.getIriClass(value);

        if(iriClass != null)
            return iriClass;

        iriClass = config.getIriCache().getIriClass(value);

        if(iriClass != null)
            return iriClass;

        iriClass = detectIriClass(value, config.getIriClasses());
        List<Column> columns = iriClass.toColumns(this, value);
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
     * Class of the IRI among the given user IRI classes: the intersection of those the IRI belongs to, or the
     * unsupported IRI class when it belongs to none. The classes are tried in the given order (the configuration lists
     * the cheap checks first), skipping every class that a class already found makes needless: a superclass of a found
     * class contains the IRI too and adds nothing to the intersection, and a class disjoint with a found class cannot
     * contain the IRI. So once a class is found, only its subclasses and the classes declared to share IRIs with it
     * ({@link SparqlDatabaseConfiguration#addIriClassOverlap}) are still tried, and two classes not declared so are
     * never both found. The result is not cached.
     *
     * @param value the value
     * @param candidates the user IRI classes to try, in the order to try them
     * @return class of the IRI among the given user IRI classes
     */
    public ResourceClass detectIriClass(Iri value, List<UserIriClass> candidates)
    {
        Set<ResourceClass> found = new HashSet<>();

        for(UserIriClass candidate : candidates)
            if(found.stream()
                    .noneMatch(f -> f.isSubclassOf(candidate) || ResourceClass.areDisjunct(config, f, candidate)))
                if(candidate.match(this, value))
                    found.add(candidate);

        if(found.isEmpty())
            return BuiltinClasses.unsupportedIri;

        return DerivedClass.intersect(config, found);
    }


    /**
     * True if the term is representable in the class. An IRI belongs to a class of IRIs exactly when its detected class
     * ({@link #getIriClass}) is a subclass of it, so the test is served from the caches instead of matching the class,
     * which may query the database.
     *
     * @param resClass the resource class
     * @param term the RDF term
     * @return true if the term is representable in the class, false otherwise
     */
    public boolean match(ResourceClass resClass, RdfTerm term)
    {
        if(resClass instanceof IriClass iriClass && term instanceof Iri iri)
            return getIriClass(iri).isSubclassOf(iriClass);

        return resClass.match(this, term);
    }


    /**
     * Constant columns representing the term in the class, served from the IRI caches for IRIs.
     *
     * @param resClass the resource class
     * @param term the RDF term
     * @return constant columns representing the term in the class, served from the IRI caches for IRIs
     */
    public List<Column> getColumns(ResourceClass resClass, RdfTerm term)
    {
        if(term instanceof Iri iri)
            return getColumns(resClass, iri);

        return resClass.toColumns(this, term);
    }


    /**
     * Constant columns representing the IRI in the class, which has to be one of the classes the IRI belongs to; served
     * from the IRI caches when possible.
     *
     * @param resClass the resource class
     * @param iri the IRI
     * @return constant columns representing the IRI in the class, served from the IRI caches when possible
     */
    public List<Column> getColumns(ResourceClass resClass, Iri iri)
    {
        List<Column> columns = iriCache.getIriColumns(iri, resClass);

        if(columns != null)
            return columns;

        columns = config.getIriCache().getIriColumns(iri, resClass);

        if(columns != null)
            return columns;

        columns = resClass.toColumns(this, iri);
        iriCache.storeColumns(iri, resClass, columns);

        return columns;
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
