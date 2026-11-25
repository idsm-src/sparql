package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.postgresql.PGConnection;
import org.postgresql.copy.CopyManager;
import org.postgresql.copy.CopyOut;
import org.postgresql.core.BaseConnection;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.mapping.classes.IntegerUserIriClass;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.testing.Database;



/**
 * The lookups of the join optimizer where the database may evaluate the statement by parallel workers (see
 * {@link Request#isParallel}): the statement is read whole and the session allows workers. A lookup is then written in
 * place, without the fence of {@code OFFSET 0} and comparing by {@code = ANY(ARRAY[...])}, which the database can
 * neither hash nor merge, so it still looks the rows up by an index, now by a nested loop it hands to the workers; a
 * hash join above a lookup does not compare the variable the lookup compares, from which the database would derive an
 * equality to hash the lookup by; the right side of an OPTIONAL looked up for every solution is written so too. A
 * lookup in place compares a variable with one child only, and a lookup by the values of two children stays fenced. A
 * statement read by fetches, or a session allowing no workers, keeps the fenced lookups, and a statement read by
 * fetches is planned without workers; a statement read in parts whose rows the database copies to the client is
 * evaluated whole, as one read whole is. The solutions are those of the joins left to the planner.
 */
public class ParallelLookupTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> "
            + "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> ";

    /**
     * The settings the engine evaluates a statement under with the join optimizer on, together with those that make the
     * planner choose a parallel plan for the small test tables wherever one is possible.
     */
    private static final List<String> settings = List.of("join_collapse_limit = 1", "enable_nestloop = off",
            "enable_mergejoin = off", "max_parallel_workers_per_gather = 2", "parallel_setup_cost = 0",
            "parallel_tuple_cost = 0", "min_parallel_table_scan_size = 0", "min_parallel_index_scan_size = 0");

    /**
     * SQL code of the queries the statements of the optimized configurations have run, and of the statements whose rows
     * they have copied, in their order.
     */
    private static final List<String> queries = new ArrayList<>();

    /**
     * Configuration with the join optimizer on, whose queries are recorded.
     */
    private static SparqlDatabaseConfiguration optimized = null;

    /**
     * Configuration with the join optimizer on, whose queries are recorded and whose connections do not give the
     * connection of the driver, so that the rows of a statement read in parts are read by fetches.
     */
    private static SparqlDatabaseConfiguration fetching = null;

    /**
     * Configuration leaving the joins to the planner.
     */
    private static SparqlDatabaseConfiguration plain = null;


    /**
     * Creates the {@code parallel_lookup} schema: the nodes 1 to 2000 of ten kinds, two links from every node and the
     * links from the node 3 to 13 and from 13 to 23 (indexed by the source), ten marks of every node (not indexed), and
     * fifty pairs of every node, among them the pair of 23 and 3 (indexed), analysed; and the two configurations
     * mapping {@code ex:kind}, {@code ex:link}, {@code ex:mark} and {@code ex:pair} over them.
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema parallel_lookup");
            statement.execute("create table parallel_lookup.nodes (id integer primary key, kind integer not null)");
            statement.execute("insert into parallel_lookup.nodes select i, i % 10 from generate_series(1, 2000) i");
            statement.execute("create table parallel_lookup.links (src integer not null, dst integer not null)");
            statement.execute("insert into parallel_lookup.links select i % 2000 + 1, (i * 7) % 2000 + 1 "
                    + "from generate_series(1, 4000) as i");
            statement.execute("insert into parallel_lookup.links values (3, 13), (13, 23)");
            statement.execute("create index on parallel_lookup.links (src)");
            statement.execute("create table parallel_lookup.marks (id integer not null, mark integer not null)");
            statement.execute("insert into parallel_lookup.marks select i % 2000 + 1, i % 7 "
                    + "from generate_series(1, 20000) as i");
            statement.execute("create table parallel_lookup.pairs (a integer not null, b integer not null)");
            statement.execute("insert into parallel_lookup.pairs select i % 2000 + 1, (i * 13) % 2000 + 1 "
                    + "from generate_series(1, 100000) as i");
            statement.execute("insert into parallel_lookup.pairs values (23, 3)");
            statement.execute("create index on parallel_lookup.pairs (a, b)");
            statement.execute("""
                    analyze parallel_lookup.nodes, parallel_lookup.links, parallel_lookup.marks, \
                    parallel_lookup.pairs""");
        }

        DatabaseSchema schema = new DatabaseSchema(connectionPool);

        optimized = configure(record(DataSource.class, connectionPool, true), schema);
        optimized.setJoinOptimizer(true);

        fetching = configure(record(DataSource.class, connectionPool, false), schema);
        fetching.setJoinOptimizer(true);

        plain = configure(connectionPool, schema);
    }


    /**
     * Creates a configuration mapping the test tables.
     */
    private static SparqlDatabaseConfiguration configure(DataSource connectionPool, DatabaseSchema schema)
            throws SQLException
    {
        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);

        config.addPrefix("ex", "http://example.org/");
        config.addIriClass(new IntegerUserIriClass("node", INT4, "http://example.org/n/"));

        DatabaseTable nodes = new DatabaseTable("parallel_lookup", "nodes");
        DatabaseTable links = new DatabaseTable("parallel_lookup", "links");
        DatabaseTable marks = new DatabaseTable("parallel_lookup", "marks");
        DatabaseTable pairs = new DatabaseTable("parallel_lookup", "pairs");

        config.addQuadMapping(nodes, null, config.createIriMapping("node", "id"), config.createIriMapping("ex:kind"),
                config.createLiteralMapping(xsdInt, "kind"));
        config.addQuadMapping(links, null, config.createIriMapping("node", "src"), config.createIriMapping("ex:link"),
                config.createIriMapping("node", "dst"));
        config.addQuadMapping(marks, null, config.createIriMapping("node", "id"), config.createIriMapping("ex:mark"),
                config.createLiteralMapping(xsdInt, "mark"));
        config.addQuadMapping(pairs, null, config.createIriMapping("node", "a"), config.createIriMapping("ex:pair"),
                config.createIriMapping("node", "b"));

        return config;
    }


    /**
     * Wraps a connection pool, a connection, a statement or a connection of the driver so that the SQL code of every
     * query a statement runs, and of every statement whose rows the driver copies, is recorded; the connections and the
     * statements it gives are wrapped likewise. Unless it copies, a connection does not give the connection of the
     * driver.
     */
    private static <T> T record(Class<T> type, T target, boolean copies)
    {
        return type.cast(Proxy.newProxyInstance(ParallelLookupTest.class.getClassLoader(), new Class<?>[] { type },
                (_, method, arguments) -> call(target, method, arguments, copies)));
    }


    /**
     * Calls the method of the wrapped object for {@link #record}.
     */
    private static Object call(Object target, Method method, Object[] arguments, boolean copies) throws Throwable
    {
        if(target instanceof Statement && method.getName().equals("executeQuery"))
        {
            synchronized(queries)
            {
                queries.add((String) arguments[0]);
            }
        }

        if(target instanceof Connection && method.getName().equals("isWrapperFor") && !copies)
            return false;

        if(target instanceof PGConnection driver && method.getName().equals("getCopyAPI"))
            return copyManager(driver);

        try
        {
            Object result = method.invoke(target, arguments);

            if(result instanceof Connection connection && method.getName().equals("getConnection"))
                return record(Connection.class, connection, copies);

            if(result instanceof Statement statement && method.getName().equals("createStatement"))
                return record(Statement.class, statement, copies);

            if(result instanceof PGConnection driver && method.getName().equals("unwrap"))
                return record(PGConnection.class, driver, copies);

            return result;
        }
        catch(InvocationTargetException e)
        {
            throw e.getCause();
        }
    }


    /**
     * The copy API of the connection of the driver, which records the SQL code of every statement whose rows it copies.
     */
    private static CopyManager copyManager(PGConnection driver) throws SQLException
    {
        return new CopyManager((BaseConnection) driver)
        {
            @Override
            public CopyOut copyOut(String sql) throws SQLException
            {
                synchronized(queries)
                {
                    queries.add(sql.substring("COPY (".length(), sql.length() - ") TO STDOUT".length()));
                }

                return super.copyOut(sql);
            }
        };
    }


    /**
     * Reads all rows of a result.
     */
    private static List<List<RdfTerm>> read(Result result) throws SQLException
    {
        List<List<RdfTerm>> rows = new ArrayList<>();

        while(result.next())
            rows.add(Arrays.asList(result.getRow()));

        return rows;
    }


    /**
     * Runs the query over the plain configuration and returns all rows.
     */
    private static List<List<RdfTerm>> expected(String query) throws Exception
    {
        try(Request request = new Engine(plain).getRequest(); Result result = request.execute(prefix + query))
        {
            return read(result);
        }
    }


    /**
     * The plan the database chooses for the SQL code under the settings of the engine, or with nested loops allowed,
     * and with parallel plans made as cheap as possible. A database older than PostgreSQL 18 adds a cost to a nested
     * loop it is told not to use so large that it cannot tell a parallel plan of the loop from a serial one, whereas a
     * newer one counts such nodes apart from the costs; so with the nested loops allowed the plan shows whether the
     * loop can be done by the workers whatever the version of the database is.
     */
    private static String explain(String sql, boolean nestedLoops) throws SQLException
    {
        try(Connection connection = Database.getPool().getConnection();
                Statement statement = connection.createStatement())
        {
            connection.setAutoCommit(false);

            try
            {
                for(String setting : settings)
                    statement.execute("SET LOCAL " + setting);

                if(nestedLoops)
                    statement.execute("SET LOCAL enable_nestloop = on");

                StringBuilder plan = new StringBuilder();

                try(ResultSet result = statement.executeQuery("EXPLAIN (COSTS OFF) " + sql))
                {
                    while(result.next())
                        plan.append(result.getString(1)).append('\n');
                }

                return plan.toString();
            }
            finally
            {
                connection.rollback();
                connection.setAutoCommit(true);
            }
        }
    }


    /**
     * True if the test database counts the nested loops it is told not to use apart from the costs (see
     * {@link #explain}), as PostgreSQL 18 and newer do.
     */
    private static boolean countsDisabledNodes() throws SQLException
    {
        try(Connection connection = Database.getPool().getConnection();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("SHOW server_version_num"))
        {
            result.next();
            return Integer.parseInt(result.getString(1)) >= 180000;
        }
    }


    /**
     * Runs the query over the optimized configuration with the given fetch size, checks that its solutions are those of
     * the plain configuration and whether the request has been evaluated as one the database may evaluate by parallel
     * workers, and returns the SQL code of the query it has run.
     */
    private static String check(String query, int fetchSize, boolean parallel, boolean workers) throws Exception
    {
        return check(optimized, query, fetchSize, parallel, workers);
    }


    /**
     * Runs the query over the optimized configuration whose rows read in parts are read by fetches, reading them in
     * parts, checks that its solutions are those of the plain configuration and that the request has not been evaluated
     * as one the database may evaluate by parallel workers, and returns the SQL code of the query it has run.
     */
    private static String checkFetched(String query) throws Exception
    {
        return check(fetching, query, 10, false, true);
    }


    /**
     * Runs the query over the configuration with the join optimizer on with the given fetch size, checks that its
     * solutions are those of the plain configuration and whether the request has been evaluated as one the database may
     * evaluate by parallel workers, and returns the SQL code of the query it has run.
     */
    private static String check(SparqlDatabaseConfiguration config, String query, int fetchSize, boolean parallel,
            boolean workers) throws Exception
    {
        List<List<RdfTerm>> expected = expected(query);

        try(Request request = new Engine(config).getRequest())
        {
            if(!workers)
                request.getStatement().execute("SET LOCAL max_parallel_workers_per_gather = 0");

            int before = queries.size();

            try(Result result = request.execute(prefix + query, null, 0, -1, fetchSize, 0))
            {
                List<List<RdfTerm>> actual = read(result);

                assertThat(actual, containsInAnyOrder(expected.toArray()));
                assertThat(actual, hasSize(greaterThan(0)));
            }

            assertThat(request.isParallel(), is(parallel));
            assertThat(queries.size(), greaterThan(before));

            return queries.getLast();
        }
    }


    /**
     * Runs the query over the configuration with the join optimizer on with the given fetch size and with parallel
     * plans made as cheap as possible, and returns the plan the database chooses for the SQL code of the query in the
     * transaction of the request, under the settings the request has made for its statement.
     */
    private static String planInRequest(SparqlDatabaseConfiguration config, String query, int fetchSize)
            throws Exception
    {
        try(Request request = new Engine(config).getRequest())
        {
            request.getStatement().execute("SET LOCAL parallel_setup_cost = 0; SET LOCAL parallel_tuple_cost = 0; "
                    + "SET LOCAL min_parallel_table_scan_size = 0");

            try(Result result = request.execute(prefix + query, null, 0, -1, fetchSize, 0))
            {
                assertThat(read(result), hasSize(greaterThan(0)));
            }

            StringBuilder plan = new StringBuilder();

            try(ResultSet result = request.getStatement().executeQuery("EXPLAIN (COSTS OFF) " + queries.getLast()))
            {
                while(result.next())
                    plan.append(result.getString(1)).append('\n');
            }

            return plan.toString();
        }
    }


    @Test
    public void lookupInPlace() throws Exception
    {
        String query = "SELECT ?x ?y WHERE { ?x ex:kind \"3\"^^xsd:int . ?x ex:link ?y }";

        // the lookup of the links is a join the database merges and compares by an array
        String sql = check(query, 0, true, true);
        assertThat(sql, containsString(" CROSS JOIN LATERAL (SELECT "));
        assertThat(sql, containsString("ANY(ARRAY["));
        assertThat(sql, not(containsString(" OFFSET 0")));

        // so the nested loop looking the links up by the index can be done by the workers
        for(boolean nestedLoops : countsDisabledNodes() ? List.of(true, false) : List.of(true))
        {
            String plan = explain(sql, nestedLoops);
            assertThat(plan, containsString("Gather"));
            assertThat(plan, not(containsString("Seq Scan on links")));
            assertThat(plan.indexOf("Nested Loop"), greaterThan(plan.indexOf("Gather")));
            assertThat(plan, containsString("= ANY (ARRAY["));
        }

        // whereas a fenced lookup is done by the process reading the nodes
        String fenced = checkFetched(query);
        assertThat(explain(fenced, true).indexOf("Nested Loop"), lessThan(explain(fenced, true).indexOf("Gather")));
    }


    @Test
    public void lookupReadByFetches() throws Exception
    {
        String query = "SELECT ?x ?y WHERE { ?x ex:kind \"3\"^^xsd:int . ?x ex:link ?y }";

        // a statement read by fetches is evaluated without workers, so the lookup stays fenced
        String sql = checkFetched(query);
        assertThat(sql, containsString(" OFFSET 0) AS "));
        assertThat(sql, not(containsString("ANY(ARRAY[")));
    }


    @Test
    public void lookupCopied() throws Exception
    {
        String query = "SELECT ?x ?y WHERE { ?x ex:kind \"3\"^^xsd:int . ?x ex:link ?y }";

        // whereas a statement whose rows the database copies is evaluated whole, so the lookup is written in place
        String sql = check(query, 10, true, true);
        assertThat(sql, containsString("ANY(ARRAY["));
        assertThat(sql, not(containsString(" OFFSET 0")));
    }


    @Test
    public void lookupWithoutWorkers() throws Exception
    {
        String query = "SELECT ?x ?y WHERE { ?x ex:kind \"3\"^^xsd:int . ?x ex:link ?y }";

        // so does a statement of a session allowing no workers
        String sql = check(query, 0, false, false);
        assertThat(sql, containsString(" OFFSET 0) AS "));
        assertThat(sql, not(containsString("ANY(ARRAY[")));
    }


    @Test
    public void readByFetchesPlannedWithoutWorkers() throws Exception
    {
        String query = "SELECT ?x ?m WHERE { ?x ex:mark ?m }";

        // the scan of the marks of a statement read whole or copied is handed to the workers
        assertThat(planInRequest(optimized, query, 0), containsString("Gather"));
        assertThat(planInRequest(optimized, query, 10), containsString("Gather"));

        // whereas a statement read by fetches, which the database evaluates without workers, is planned without them
        assertThat(planInRequest(fetching, query, 10), not(containsString("Gather")));
    }


    @Test
    public void sharedVariableHashedAboveLookup() throws Exception
    {
        String query = "SELECT ?x ?y ?m WHERE { ?x ex:kind \"3\"^^xsd:int . ?x ex:link ?y . ?x ex:mark ?m }";

        // the marks, which no index serves, are hashed, the links are looked up, and no equality of the links with the
        // nodes the database could derive from the hash join lets it hash the links instead
        String sql = check(query, 0, true, true);
        assertThat(sql, containsString("ANY(ARRAY["));

        String plan = explain(sql, false);
        assertThat(plan, containsString("Hash Join"));
        assertThat(plan, containsString("Seq Scan on marks"));
        assertThat(plan, not(containsString("Seq Scan on links")));
        assertThat(plan, containsString("= ANY (ARRAY["));
    }


    @Test
    public void variableComparedOnce() throws Exception
    {
        String query = "SELECT ?x ?y ?z WHERE { ?x ex:kind \"3\"^^xsd:int . ?x ex:link ?y . ?x ex:link ?z }";

        // the second lookup compares the node with one of the two children giving it, whose values are equal, so that
        // the database does not take the two comparisons for independent conditions
        String sql = check(query, 0, true, true);
        assertThat(sql.split(Pattern.quote("\"src\" = ANY(ARRAY["), -1).length - 1, is(2));
        assertThat(sql, not(containsString(" OFFSET 0")));

        String plan = explain(sql, true);
        assertThat(plan, containsString("Gather"));
        assertThat(plan, not(containsString("Join Filter")));
        assertThat(plan, not(containsString("Seq Scan on links")));
    }


    @Test
    public void lookupByTwoChildrenFenced() throws Exception
    {
        String query = "SELECT ?x ?y ?z WHERE { ?x ex:kind \"3\"^^xsd:int . ?x ex:link ?y . ?y ex:link ?z . "
                + "?z ex:pair ?x }";

        // the pair is looked up by the node the second link leads to and by the first node, which no child gives
        // together, so it stays fenced, comparing both in its index condition, while the links are looked up in place
        String sql = check(query, 0, true, true);
        assertThat(sql.split(Pattern.quote(" OFFSET 0) AS "), -1).length - 1, is(1));
        assertThat(sql, containsString("ANY(ARRAY["));
    }


    @Test
    public void optionalInPlace() throws Exception
    {
        String query = "SELECT ?x ?y WHERE { ?x ex:kind \"3\"^^xsd:int OPTIONAL { ?x ex:link ?y } }";

        // the right side looked up for every node is merged into the left join, which compares by an array as well
        String sql = check(query, 0, true, true);
        assertThat(sql, containsString(" LEFT JOIN LATERAL (SELECT "));
        assertThat(sql, containsString("ANY(ARRAY["));
        assertThat(sql, not(containsString(" OFFSET 0")));

        for(boolean nestedLoops : countsDisabledNodes() ? List.of(true, false) : List.of(true))
        {
            String plan = explain(sql, nestedLoops);
            assertThat(plan, containsString("Gather"));
            assertThat(plan.indexOf("Nested Loop Left Join"), greaterThan(plan.indexOf("Gather")));
            assertThat(plan, not(containsString("Seq Scan on links")));
            assertThat(plan, not(containsString("Hash")));
        }

        // under the settings of the engine, the left join is not hashed either
        assertThat(explain(sql, false), containsString("Nested Loop Left Join"));
    }
}
