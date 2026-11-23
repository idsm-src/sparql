package cz.iocb.sparql.engine.request;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
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
import cz.iocb.sparql.engine.database.CommonTable;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.database.LateralTable;
import cz.iocb.sparql.engine.database.SQLRuntimeException;
import cz.iocb.sparql.engine.database.SqlCode;
import cz.iocb.sparql.engine.error.MessageCategory;
import cz.iocb.sparql.engine.error.MessageType;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.error.TranslateMessage;
import cz.iocb.sparql.engine.imcode.SqlIntercode;
import cz.iocb.sparql.engine.imcode.SqlSelect;
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
     * What a node joining the tables of a join mapping by a key of a term stands for: the values of the join columns in
     * the row of a table that holds the term of a variable. A solution binding the node binds the variable to that term
     * wherever it binds it at all.
     *
     * @param variable the variable whose term gives the row
     * @param determining whether the node gives the term as well, the join columns being a key of the table too: two
     *            solutions binding the node then agree in it exactly when they agree in the variable, so the node can
     *            be compared in place of the term
     * @param reader the access to the table that binds the node to its join columns and the variable to the term the
     *            row holds, which gives the terms of the nodes of solutions deduplicated or grouped by the node
     */
    public record NodeTerm(Variable variable, boolean determining, SqlIntercode reader)
    {
    }


    /**
     * Reader of the result of a lookup query (see {@link #lookup}).
     *
     * @param <T> type of what is read
     */
    @FunctionalInterface
    public interface ResultReader<T>
    {
        /**
         * Reads the result of the query.
         *
         * @param result the result set, positioned before its first row
         * @return what has been read
         * @throws SQLException on database errors
         */
        T read(ResultSet result) throws SQLException;
    }


    /**
     * What tells the common table expressions of a request apart (see {@link #getCommonTableExpression}).
     *
     * @param text SQL text of the query the common table expression evaluates
     * @param materialized whether the database is told to keep the result of the query
     */
    private record CommonTableKey(String text, boolean materialized)
    {
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
     * Common table expressions of the query being evaluated, by the SQL texts of their queries and by whether the
     * database keeps their results (see {@link #getCommonTableExpression} and {@link #getMaterializedTableExpression}).
     */
    private final Map<CommonTableKey, CommonTable> commonTableExpressions = new HashMap<>();

    /**
     * Row counts the planner of the database has estimated for statements during the request, by the texts of the
     * statements (see {@link #getRowEstimates}).
     */
    private final Map<String, Long> rowEstimates = new HashMap<>();

    /**
     * Rows of the dictionary tables read during the request, by the texts of the queries reading them (see
     * {@link #getDictionaryRows}).
     */
    private final Map<String, List<List<String>>> dictionaryRows = new HashMap<>();

    /**
     * Whether the probed tables asked for a row during the request have one, by the texts of the queries asking (see
     * {@link #getProbedRows}).
     */
    private final Map<String, Boolean> probedRows = new HashMap<>();

    /**
     * What the nodes joining the tables of join mappings by the keys of terms stand for, by the node (see
     * {@link #getNodeTerms}).
     */
    private final Map<Variable, NodeTerm> nodeTerms = new HashMap<>();

    /**
     * Start of the execution in {@link System#nanoTime} units.
     */
    private long begin;

    /**
     * Time limit of the execution in nanoseconds; 0 for none.
     */
    private long timeout;

    /**
     * Whether the database may evaluate the statement of the request by parallel workers (see {@link #isParallel}).
     */
    private boolean parallel;

    /**
     * Whether the request was cancelled; set by another thread than the one evaluating the request.
     */
    private volatile boolean canceled;


    /**
     * Creates a request; with {@code serviceReorder}, SERVICE calls may be evaluated independently of their context and
     * joined afterwards, each asked about as many of its shared variables as the context limit of its endpoint takes by
     * the estimates.
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


            // the solutions whose triples fill one fetch (a template of no triple constructs none)
            int border = syntaxTree instanceof ConstructQuery cnst ?
                    fetchSize / Math.max(1, cnst.getTemplates().size()) : fetchSize;
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

            // the database evaluates a statement by parallel workers only when it is read whole, as it evaluates one
            // read by fetches without them, and when the session allows them; the joins the optimizer has chosen are
            // then written so that the workers can evaluate them (see SqlJoin.translateTree)
            parallel = config.isJoinOptimizerEnabled() && fetchSize == 0 && allowsParallelWorkers();

            TranslateVisitor translateVisitor = new TranslateVisitor(this);
            SqlSelect imcode = translateVisitor.translate(syntaxTree, newOffset, newLimit, order);
            String code;

            try
            {
                // optimize
                imcode = imcode.optimize(this, false);

                // the optimisation runs in Java, which neither a statement timeout nor a cancelled statement ends
                checkTimeout();
                checkCanceled();

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

            try
            {
                if(config.isJoinOptimizerEnabled())
                {
                    // the joins are written in the order and with the methods the engine has chosen: the planner must
                    // keep the order of the explicit joins and hash them (a merge join would sort the values, which is
                    // slow for boxes), the index lookups being forced by lateral subqueries (see JoinOptimizer); the
                    // settings last until the end of the transaction of the request
                    getStatement().execute("SET LOCAL join_collapse_limit = 1; SET LOCAL enable_nestloop = off; "
                            + "SET LOCAL enable_mergejoin = off");
                }

                return new Result(type, imcode.getResultDescription(), getStatement(fetchSize).executeQuery(code),
                        begin, timeout);
            }
            catch(SQLRuntimeException e)
            {
                // a statement of an elapsed or cancelled request
                throw(SQLException) e.getCause();
            }
        }
        catch(SQLException e)
        {
            if(canceled)
                logger.warn("query canceled: " + e.getMessage());
            else if(e.getErrorCode() == 0 && "57014".equals(e.getSQLState()))
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
     * Cancels the request: the running database statement is cancelled, and a SERVICE call waiting for the answer of an
     * endpoint gives the answer up within a fraction of a second (see
     * {@link cz.iocb.sparql.engine.imcode.SqlServiceStub}), closing the connection; the request cannot be used
     * afterwards. It may be called by another thread than the one evaluating the request, also once the request has
     * been closed.
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
     * Remaining time of the time limit of the request, which has one.
     *
     * @return remaining time of the time limit in nanoseconds, a positive number
     * @throws SQLTimeoutException with the SQL state 57014 of a cancelled statement if the time limit has elapsed
     */
    private long getRemainingTime() throws SQLTimeoutException
    {
        long rest = timeout - (System.nanoTime() - begin);

        if(rest <= 0)
            throw new SQLTimeoutException("query timeout", "57014", 0);

        return rest;
    }


    /**
     * Checks that the time limit of the request has not elapsed.
     *
     * @throws SQLTimeoutException with the SQL state 57014 of a cancelled statement if the time limit has elapsed
     */
    public void checkTimeout() throws SQLTimeoutException
    {
        if(timeout != 0)
            getRemainingTime();
    }


    /**
     * Remaining time of the timeout for a statement, in whole seconds rounded up, so that a statement is never
     * cancelled before the time limit, though it may run less than a second past it; 0 for no timeout.
     *
     * @return remaining time of the timeout for a statement in whole seconds rounded up; 0 for no timeout
     * @throws SQLTimeoutException with the SQL state 57014 of a cancelled statement if the time limit has elapsed
     */
    private int getStatementTimeout() throws SQLTimeoutException
    {
        if(timeout == 0)
            return 0;

        return (int) Math.min(Integer.MAX_VALUE, Math.ceilDiv(getRemainingTime(), 1_000_000_000L));
    }


    /**
     * Remaining time of the timeout for a connection to a remote endpoint, in whole milliseconds rounded up, as the
     * connect and read timeouts of a {@link java.net.URLConnection} take it, so that a connection never times out
     * before the time limit; 0 for no timeout.
     *
     * @return remaining time of the timeout for a connection in whole milliseconds rounded up; 0 for no timeout
     * @throws SQLTimeoutException with the SQL state 57014 of a cancelled statement if the time limit has elapsed
     */
    public int getConnectionTimeout() throws SQLTimeoutException
    {
        if(timeout == 0)
            return 0;

        return (int) Math.min(Integer.MAX_VALUE, Math.ceilDiv(getRemainingTime(), 1_000_000L));
    }


    /**
     * True if the request has been cancelled (see {@link #cancel}).
     *
     * @return true if the request has been cancelled, false otherwise
     */
    public boolean isCanceled()
    {
        return canceled;
    }


    /**
     * Checks that the request has not been cancelled.
     *
     * @throws SQLException if the request has been cancelled (see {@link #cancel})
     */
    public void checkCanceled() throws SQLException
    {
        if(canceled)
            throw new SQLException("query was canceled");
    }


    /**
     * The statement of the request (created on first use) with the given fetch size and the remaining part of the
     * timeout set.
     *
     * @param fetchSize the JDBC fetch size
     * @return the statement of the request (created on first use) with the given fetch size and the remaining part of
     *         the timeout set
     * @throws SQLRuntimeException wrapping a {@link SQLTimeoutException} with the SQL state 57014 if the time limit of
     *             the request has elapsed, or another {@link SQLException} if the request has been cancelled or the
     *             statement cannot be created
     */
    public synchronized Statement getStatement(int fetchSize)
    {
        try
        {
            checkCanceled();

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
     * @throws SQLRuntimeException as {@link #getStatement(int)} does
     */
    public synchronized Statement getStatement()
    {
        return getStatement(0);
    }


    /**
     * Runs queries whose failure must not abort the transaction of the request: the lookups of statistics and the
     * requests for plans, which the translation of a query is only improved by. The queries are sent between a
     * savepoint and its release in a single call, so they cost one round trip to the database, however many they are;
     * when one of them fails, the transaction is rolled back to the savepoint and none of them has a result.
     *
     * @param <T> type of what is read from the result of a query
     * @param queries SQL of the queries, each a single statement
     * @param reader reader of the result of each query
     * @return what the reader has read from the results of the queries, in their order
     * @throws SQLException when a query fails
     */
    public <T> List<T> lookup(List<String> queries, ResultReader<T> reader) throws SQLException
    {
        return lookup(List.of(), queries, reader);
    }


    /**
     * Runs queries as {@link #lookup(List, ResultReader)} does, under settings of the database that hold for them only:
     * the settings are made after the savepoint for the current transaction, and the transaction is rolled back to the
     * savepoint after the queries, which takes the settings back, so the queries may only read.
     *
     * @param <T> type of what is read from the result of a query
     * @param settings the settings the queries run under, each as {@code name = value}
     * @param queries SQL of the queries, each a single statement that only reads
     * @param reader reader of the result of each query
     * @return what the reader has read from the results of the queries, in their order
     * @throws SQLException when a query fails
     */
    public <T> List<T> lookup(List<String> settings, List<String> queries, ResultReader<T> reader) throws SQLException
    {
        List<T> values = new ArrayList<>(queries.size());

        if(queries.isEmpty())
            return values;

        Statement statement = getStatement();

        StringBuilder builder = new StringBuilder("SAVEPOINT lookup; ");

        for(String setting : settings)
            builder.append("SET LOCAL ").append(setting).append("; ");

        builder.append(String.join("; ", queries));

        if(!settings.isEmpty())
            builder.append("; ROLLBACK TO SAVEPOINT lookup");

        builder.append("; RELEASE SAVEPOINT lookup");

        try
        {
            statement.execute(builder.toString());
        }
        catch(SQLException e)
        {
            try
            {
                statement.execute("ROLLBACK TO SAVEPOINT lookup");
            }
            catch(SQLException rollback)
            {
                e.addSuppressed(rollback);
            }

            throw e;
        }

        // the results follow those of the savepoint and of the settings, in the order of the queries
        for(int i = 0; i < settings.size(); i++)
            statement.getMoreResults();

        for(int i = 0; i < queries.size(); i++)
        {
            if(!statement.getMoreResults())
                throw new SQLException("a lookup query has no result");

            try(ResultSet result = statement.getResultSet())
            {
                values.add(reader.read(result));
            }
        }

        return values;
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
     * Fresh alias for the left side of a lateral join.
     *
     * @return fresh alias for the left side of a lateral join
     */
    public LateralTable createLateralTable()
    {
        return new LateralTable(lateralId++);
    }


    /**
     * Row counts the planner of the database has estimated for statements during the request, by the texts of the
     * statements: an estimate asked for again is taken from here (see
     * {@link cz.iocb.sparql.engine.translator.Estimator}).
     *
     * @return row counts the planner has estimated during the request, by the texts of the statements
     */
    public Map<String, Long> getRowEstimates()
    {
        return rowEstimates;
    }


    /**
     * Rows of the dictionary tables read during the request, by the texts of the queries reading them, each row as the
     * text forms of its values: a row asked for again is taken from here (see
     * {@link cz.iocb.sparql.engine.imcode.SqlTableAccess}). Nothing read so outlives the request.
     *
     * @return rows of the dictionary tables read during the request, by the texts of the queries reading them
     */
    public Map<String, List<List<String>>> getDictionaryRows()
    {
        return dictionaryRows;
    }


    /**
     * Whether the probed tables asked for a row during the request have one, by the texts of the queries asking: a
     * table asked again is answered from here (see {@link cz.iocb.sparql.engine.imcode.SqlTableAccess}). Nothing found
     * so outlives the request.
     *
     * @return whether the probed tables asked for a row during the request have one, by the texts of the queries
     */
    public Map<String, Boolean> getProbedRows()
    {
        return probedRows;
    }


    /**
     * What the nodes joining the tables of join mappings by the keys of terms stand for, by the node, for the query
     * being translated (see {@link cz.iocb.sparql.engine.translator.TranslateVisitor}).
     *
     * @return what the nodes stand for, by the node
     */
    public Map<Variable, NodeTerm> getNodeTerms()
    {
        return nodeTerms;
    }


    /**
     * The common table expression that evaluates the query, created when the query is first asked for, so that a query
     * asked for several times within the request has one common table expression, which a statement evaluates once. The
     * table is numbered by the order it is asked for (see {@link CommonTable}). SQL code written with the table reads
     * it (see {@link SqlCode}), so every statement made of such code declares it in its {@code WITH} clause (see
     * {@link SqlSelect#translateWithClause}), also when the code has gone through the SQL of an expression, which is
     * rendered at once during the optimisation, before any statement is translated. The query is declared at the top of
     * the statement, where no alias given inside the statement is visible, so it must refer to no outer table (see
     * {@link SqlCode#getOuterTables}).
     *
     * @param query SQL code of the query
     * @return the common table expression evaluating the query
     */
    public CommonTable getCommonTableExpression(SqlCode query)
    {
        return getTableExpression(query, false);
    }


    /**
     * The common table expression that evaluates the query like {@link #getCommonTableExpression}, whose result the
     * database is told to keep wherever it is read: a query that a single place of the statement reads is evaluated in
     * that place otherwise, which is anew for every row when the place is a subquery evaluated for the rows of another
     * table. The result is still computed only as far as it is read, so a table no row reads costs nothing. The table
     * is another one than the table the query has without that.
     *
     * @param query SQL code of the query
     * @return the common table expression evaluating the query, declared as materialized
     */
    public CommonTable getMaterializedTableExpression(SqlCode query)
    {
        return getTableExpression(query, true);
    }


    /**
     * The common table expression of the given kind that evaluates the query, created when it is first asked for.
     *
     * @param query SQL code of the query
     * @param materialized whether the database is told to keep the result of the query
     * @return the common table expression evaluating the query
     */
    private CommonTable getTableExpression(SqlCode query, boolean materialized)
    {
        assert query.getOuterTables().isEmpty();

        CommonTableKey key = new CommonTableKey(query.getText(), materialized);
        CommonTable table = commonTableExpressions.get(key);

        if(table == null)
        {
            table = new CommonTable(commonTableExpressions.size(), query, materialized);
            commonTableExpressions.put(key, table);
        }

        return table;
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
     * Class of the blank node: the in-segment class of its segment. Every call gives a new instance, which is the same
     * class as any other one of the segment, because the classes are compared by their values.
     *
     * @param bnode the blank node
     * @return class of the blank node: the in-segment class of its segment
     */
    public ResourceClass getBlankNodeClass(BlankNode bnode)
    {
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


    /**
     * True if the database may evaluate the statement of the request by parallel workers, which is decided before the
     * query is translated (see {@link #execute(PreparedQuery, List, int, int, int, long, int)}): the join optimizer is
     * on, the statement is read whole, as the database evaluates a statement read by fetches without workers, and the
     * session allows workers ({@code max_parallel_workers_per_gather}). The lookups the optimizer has chosen are then
     * written so that the workers can do them (see {@link cz.iocb.sparql.engine.imcode.SqlTableAccess}), whereas
     * otherwise they stay subqueries of their own, which the database evaluates only in the process reading the rows
     * they are looked up for, but whose comparisons cost less and whose results it may keep for the values looked up.
     * False for a query translated without being executed.
     *
     * @return true if the database may evaluate the statement of the request by parallel workers, false otherwise
     */
    public boolean isParallel()
    {
        return parallel;
    }


    /**
     * True if the session of the request allows the database to evaluate a statement by parallel workers.
     *
     * @return true if the session allows parallel workers, false otherwise
     * @throws SQLException when the setting cannot be read, or with the SQL state 57014 when the time limit of the
     *             request has elapsed, or when the request has been cancelled (see {@link #getStatement(int)})
     */
    private boolean allowsParallelWorkers() throws SQLException
    {
        try
        {
            return lookup(List.of("SELECT current_setting('max_parallel_workers_per_gather')::integer > 0"),
                    result -> result.next() && result.getBoolean(1)).get(0);
        }
        catch(SQLRuntimeException e)
        {
            // the statement of an elapsed or cancelled request
            throw(SQLException) e.getCause();
        }
    }
}
