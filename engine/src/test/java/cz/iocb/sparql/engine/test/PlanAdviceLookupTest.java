package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.PGConnection;
import org.postgresql.copy.CopyManager;
import org.postgresql.copy.CopyOut;
import org.postgresql.core.BaseConnection;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.database.PlanAdvice;
import cz.iocb.sparql.engine.mapping.classes.IntegerUserIriClass;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.testing.Database;



/**
 * The lookups of the join optimizer made by plan advice (see {@link SparqlDatabaseConfiguration#setPlanAdvice}), which
 * needs the module {@code pg_plan_advice} of PostgreSQL 19 loaded in the sessions of the test database; the tests are
 * skipped otherwise. A lookup is a plain join of the statement, without the fence of {@code OFFSET 0} and compared by
 * plain equalities, and its table is read under an alias by which the advice makes the planner look it up by a nested
 * loop, which the database hands to parallel workers, and caches by the values looked up where the optimizer expects
 * them to repeat; the joins the optimizer hashes are hashed, and every item of the advice is followed. A statement read
 * by fetches is advised as well, and so is one whose rows the database copies to the client, which the database
 * evaluates as advised, and the right side of an OPTIONAL looked up for every solution; a lookup by the values of two
 * children stays fenced, and so does a lookup in the pattern of an expression. The solutions are those of the joins
 * left to the planner.
 */
public class PlanAdviceLookupTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> "
            + "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> ";

    /**
     * The settings that make the planner choose a parallel plan for the small test tables wherever one is possible.
     */
    private static final List<String> settings = List.of("max_parallel_workers_per_gather = 2",
            "parallel_setup_cost = 0", "parallel_tuple_cost = 0", "min_parallel_table_scan_size = 0",
            "min_parallel_index_scan_size = 0");

    /**
     * The query that sets the advice for the transaction and whether the nested loops are allowed, as the engine does.
     */
    private static final String adviseQuery = "SELECT set_config('pg_plan_advice.advice', ?, true), "
            + "set_config('enable_nestloop', ?, true)";

    /**
     * SQL code of the queries the statements of the advised configurations have run, and of the statements whose rows
     * they have copied, in their order.
     */
    private static final List<String> queries = new ArrayList<>();

    /**
     * Whether the sessions of the test database have the module {@code pg_plan_advice} loaded.
     */
    private static boolean available = false;

    /**
     * Configuration with the join optimizer on and its joins kept by plan advice, whose queries are recorded.
     */
    private static SparqlDatabaseConfiguration advised = null;

    /**
     * Configuration with the join optimizer on and its joins kept by plan advice, whose queries are recorded and whose
     * connections do not give the connection of the driver, so that the rows of a statement read in parts are read by
     * fetches.
     */
    private static SparqlDatabaseConfiguration fetching = null;

    /**
     * Configuration leaving the joins to the planner.
     */
    private static SparqlDatabaseConfiguration plain = null;


    /**
     * Creates the {@code plan_advice} schema, when the test database has the module: the nodes 1 to 2000 of ten kinds,
     * two links from every node and the links from the node 3 to 13 and from 13 to 23 (indexed by the source), ten
     * marks of every node (not indexed), ten groups of every node, which are the nodes 1 to 7 (not indexed), fifty
     * pairs of every node, among them the pair of 23 and 3 (indexed), and ten items of each of the nodes 1 to 20000
     * (indexed), analysed; and the two configurations mapping {@code ex:kind}, {@code ex:link}, {@code ex:mark},
     * {@code ex:group}, {@code ex:pair} and {@code ex:item} over them.
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection();
                Statement statement = connection.createStatement();
                ResultSet result = statement
                        .executeQuery("SELECT current_setting('pg_plan_advice.advice', true) IS NOT NULL"))
        {
            result.next();
            available = result.getBoolean(1);
        }

        if(!available)
            return;

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema plan_advice");
            statement.execute("create table plan_advice.nodes (id integer primary key, kind integer not null)");
            statement.execute("insert into plan_advice.nodes select i, i % 10 from generate_series(1, 2000) i");
            statement.execute("create table plan_advice.links (src integer not null, dst integer not null)");
            statement.execute("insert into plan_advice.links select i % 2000 + 1, (i * 7) % 2000 + 1 "
                    + "from generate_series(1, 4000) as i");
            statement.execute("insert into plan_advice.links values (3, 13), (13, 23)");
            statement.execute("create index on plan_advice.links (src)");
            statement.execute("create table plan_advice.marks (id integer not null, mark integer not null)");
            statement.execute("insert into plan_advice.marks select i % 2000 + 1, i % 7 "
                    + "from generate_series(1, 20000) as i");
            statement.execute("create table plan_advice.groups (id integer not null, grp integer not null)");
            statement.execute("insert into plan_advice.groups select i % 2000 + 1, i % 7 + 1 "
                    + "from generate_series(1, 20000) as i");
            statement.execute("create table plan_advice.pairs (a integer not null, b integer not null)");
            statement.execute("insert into plan_advice.pairs select i % 2000 + 1, (i * 13) % 2000 + 1 "
                    + "from generate_series(1, 100000) as i");
            statement.execute("insert into plan_advice.pairs values (23, 3)");
            statement.execute("create index on plan_advice.pairs (a, b)");
            statement.execute("create table plan_advice.items (grp integer not null, item integer not null)");
            statement.execute("""
                    insert into plan_advice.items select i % 20000 + 1, i \
                    from generate_series(1, 200000) as i""");
            statement.execute("create index on plan_advice.items (grp)");
            statement.execute("""
                    analyze plan_advice.nodes, plan_advice.links, plan_advice.marks, plan_advice.groups, \
                    plan_advice.pairs, plan_advice.items""");
        }

        DatabaseSchema schema = new DatabaseSchema(connectionPool);

        advised = configure(record(DataSource.class, connectionPool, true), schema);
        advised.setJoinOptimizer(true);
        advised.setPlanAdvice(true);

        fetching = configure(record(DataSource.class, connectionPool, false), schema);
        fetching.setJoinOptimizer(true);
        fetching.setPlanAdvice(true);

        plain = configure(connectionPool, schema);
    }


    /**
     * Skips the test when the test database does not have the module.
     */
    @BeforeEach
    void requireModule()
    {
        assumeTrue(available, "pg_plan_advice is not loaded in the sessions of the test database");
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

        DatabaseTable nodes = new DatabaseTable("plan_advice", "nodes");
        DatabaseTable links = new DatabaseTable("plan_advice", "links");
        DatabaseTable marks = new DatabaseTable("plan_advice", "marks");
        DatabaseTable groups = new DatabaseTable("plan_advice", "groups");
        DatabaseTable pairs = new DatabaseTable("plan_advice", "pairs");
        DatabaseTable items = new DatabaseTable("plan_advice", "items");

        config.addQuadMapping(nodes, null, config.createIriMapping("node", "id"), config.createIriMapping("ex:kind"),
                config.createLiteralMapping(xsdInt, "kind"));
        config.addQuadMapping(links, null, config.createIriMapping("node", "src"), config.createIriMapping("ex:link"),
                config.createIriMapping("node", "dst"));
        config.addQuadMapping(marks, null, config.createIriMapping("node", "id"), config.createIriMapping("ex:mark"),
                config.createLiteralMapping(xsdInt, "mark"));
        config.addQuadMapping(groups, null, config.createIriMapping("node", "id"), config.createIriMapping("ex:group"),
                config.createIriMapping("node", "grp"));
        config.addQuadMapping(pairs, null, config.createIriMapping("node", "a"), config.createIriMapping("ex:pair"),
                config.createIriMapping("node", "b"));
        config.addQuadMapping(items, null, config.createIriMapping("node", "grp"), config.createIriMapping("ex:item"),
                config.createLiteralMapping(xsdInt, "item"));

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
        return type.cast(Proxy.newProxyInstance(PlanAdviceLookupTest.class.getClassLoader(), new Class<?>[] { type },
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
     * Runs the query over the advised configuration with the given fetch size, checks that its solutions are those of
     * the plain configuration, that the request has been advised and not evaluated as one written for parallel workers
     * by conditions the database cannot hash, and returns the SQL code of the query it has run, which the statement
     * planned for the advice precedes.
     */
    private static String check(String query, int fetchSize) throws Exception
    {
        return check(advised, query, fetchSize);
    }


    /**
     * Runs the query over the advised configuration given with the given fetch size, checks that its solutions are
     * those of the plain configuration, that the request has been advised and not evaluated as one written for parallel
     * workers by conditions the database cannot hash, and returns the SQL code of the query it has run, which the
     * statement planned for the advice precedes.
     */
    private static String check(SparqlDatabaseConfiguration config, String query, int fetchSize) throws Exception
    {
        List<List<RdfTerm>> expected = expected(query);

        try(Request request = new Engine(config).getRequest())
        {
            int before = queries.size();

            try(Result result = request.execute(prefix + query, null, 0, -1, fetchSize, 0))
            {
                List<List<RdfTerm>> actual = read(result);

                assertThat(actual, containsInAnyOrder(expected.toArray()));
                assertThat(actual, hasSize(greaterThan(0)));
            }

            assertThat(request.isAdvised(), is(true));
            assertThat(request.isParallel(), is(false));

            String sql = queries.getLast();
            assertThat(queries.size() - before, greaterThan(1));
            assertThat(queries.get(queries.size() - 2), is("EXPLAIN (COSTS OFF, PLAN_ADVICE) " + sql));

            return sql;
        }
    }


    /**
     * The plan the database chooses for the SQL code under the advice the engine makes for it, with parallel plans made
     * as cheap as possible; the supplied advice closes the plan with what became of every item.
     */
    private static String explain(String sql) throws SQLException
    {
        try(Connection connection = Database.getPool().getConnection();
                Statement statement = connection.createStatement())
        {
            connection.setAutoCommit(false);

            try
            {
                statement.execute("SET LOCAL join_collapse_limit = 1; SET LOCAL enable_nestloop = off; "
                        + "SET LOCAL enable_mergejoin = off");

                for(String setting : settings)
                    statement.execute("SET LOCAL " + setting);

                // the lookups are advised under the settings, and a memoized one with the joins and the nested loops
                advise(connection, PlanAdvice.build(explainAdvice(statement, sql), false), false);

                if(sql.contains(" AS " + PlanAdvice.MEMOIZED_LOOKUP + " "))
                    advise(connection, PlanAdvice.build(explainAdvice(statement, sql), true), true);

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
     * The advice the planner generates for the SQL code under the settings in effect.
     */
    private static String explainAdvice(Statement statement, String sql) throws SQLException
    {
        StringBuilder generated = new StringBuilder();

        try(ResultSet result = statement.executeQuery("EXPLAIN (COSTS OFF, PLAN_ADVICE) " + sql))
        {
            boolean advice = false;

            while(result.next())
            {
                if(advice)
                    generated.append(result.getString(1)).append('\n');

                advice |= result.getString(1).equals("Generated Plan Advice:");
            }
        }

        return generated.toString();
    }


    /**
     * Sets the advice for the transaction and whether the nested loops are allowed.
     */
    private static void advise(Connection connection, String advice, boolean nestedLoops) throws SQLException
    {
        try(PreparedStatement statement = connection.prepareStatement(adviseQuery))
        {
            statement.setString(1, advice);
            statement.setString(2, nestedLoops ? "on" : "off");
            statement.execute();
        }
    }


    /**
     * Checks that the planner has followed every item of the advice in the plan.
     */
    private static void assertFollowed(String plan)
    {
        assertThat(plan, containsString("Supplied Plan Advice:"));
        assertThat(plan, containsString("/* matched */"));

        for(String feedback : List.of("not matched", "failed", "conflicting", "inapplicable", "partially matched"))
            assertThat(plan, not(containsString(feedback)));
    }


    @Test
    public void lookupAdvised() throws Exception
    {
        String query = "SELECT ?x ?y WHERE { ?x ex:kind \"3\"^^xsd:int . ?x ex:link ?y }";

        // the lookup of the links is a plain join under the alias of a lookup
        String sql = check(query, 0);
        assertThat(sql, containsString(" CROSS JOIN LATERAL (SELECT "));
        assertThat(sql, containsString(" FROM \"plan_advice\".\"links\" AS " + PlanAdvice.LOOKUP + " WHERE "));
        assertThat(sql, not(containsString("ANY(ARRAY[")));
        assertThat(sql, not(containsString(" OFFSET 0")));

        // which the advice makes the planner look up by a nested loop that the workers can do
        String plan = explain(sql);
        assertFollowed(plan);
        assertThat(plan, containsString("Gather"));
        assertThat(plan.indexOf("Nested Loop"), greaterThan(plan.indexOf("Gather")));
        assertThat(plan, containsString("on links " + PlanAdvice.LOOKUP));
        assertThat(plan, not(containsString("Seq Scan on links")));
        assertThat(plan, not(containsString("Hash")));
    }


    @Test
    public void lookupReadByFetches() throws Exception
    {
        // a statement read by fetches is evaluated without workers, but its lookups are advised as well
        String sql = check(fetching, "SELECT ?x ?y WHERE { ?x ex:kind \"3\"^^xsd:int . ?x ex:link ?y }", 10);
        assertThat(sql, containsString(" AS " + PlanAdvice.LOOKUP + " WHERE "));
        assertThat(sql, not(containsString(" OFFSET 0")));
    }


    @Test
    public void lookupCopied() throws Exception
    {
        String query = "SELECT ?x ?y WHERE { ?x ex:kind \"3\"^^xsd:int . ?x ex:link ?y }";

        // the lookups of a statement whose rows the database copies are advised as well, and the database evaluates
        // the copy as advised: the plan of the copy, which the database reports when the copy ends, looks the links up
        String sql = check(query, 10);
        assertThat(sql, containsString(" AS " + PlanAdvice.LOOKUP + " WHERE "));

        try(Request request = new Engine(advised).getRequest())
        {
            request.getStatement().execute("LOAD 'auto_explain'");
            request.getStatement()
                    .execute("SET LOCAL auto_explain.log_min_duration = 0; SET LOCAL auto_explain.log_level = notice");

            try(Result result = request.execute(prefix + query, null, 0, -1, 10, 0))
            {
                assertThat(read(result), hasSize(greaterThan(0)));

                String plan = String.join("\n", result.getWarnings());
                assertThat(plan, containsString("Nested Loop"));
                assertThat(plan, containsString("on links " + PlanAdvice.LOOKUP));
                assertThat(plan, not(containsString("Seq Scan on links")));
            }
        }
    }


    @Test
    public void hashJoinKept() throws Exception
    {
        String query = "SELECT ?x ?y ?m WHERE { ?x ex:kind \"3\"^^xsd:int . ?x ex:link ?y . ?x ex:mark ?m }";

        // the marks, which no index serves, are hashed, and the links are looked up
        String sql = check(query, 0);
        assertThat(sql, containsString(" AS " + PlanAdvice.LOOKUP + " WHERE "));

        String plan = explain(sql);
        assertFollowed(plan);
        assertThat(plan, containsString("Hash Join"));
        assertThat(plan, containsString("Seq Scan on marks"));
        assertThat(plan, containsString("on links " + PlanAdvice.LOOKUP));
        assertThat(plan, not(containsString("Seq Scan on links")));
    }


    @Test
    public void lookupByTwoChildrenFenced() throws Exception
    {
        String query = "SELECT ?x ?y ?z WHERE { ?x ex:kind \"3\"^^xsd:int . ?x ex:link ?y . ?y ex:link ?z . "
                + "?z ex:pair ?x }";

        // the pair is looked up by the node the second link leads to and by the first node, which no child gives
        // together, and the database would look it up by a part of them, so it stays fenced, comparing both in its
        // index condition, while the links are looked up in place
        String sql = check(query, 0);
        assertThat(sql.split(Pattern.quote(" OFFSET 0) AS "), -1).length - 1, is(1));
        assertThat(sql.split(Pattern.quote(" AS " + PlanAdvice.LOOKUP + " WHERE "), -1).length - 1, is(2));

        String plan = explain(sql);
        assertFollowed(plan);
        assertThat(plan, not(containsString("on pairs " + PlanAdvice.LOOKUP)));
        assertThat(Pattern.compile("Index Cond: \\(\\(a = [^)]+\\) AND \\(b = [^)]+\\)\\)").matcher(plan).find(),
                is(true));
    }


    @Test
    public void memoizedLookup() throws Exception
    {
        String query = "SELECT (COUNT(*) AS ?c) WHERE { ?x ex:kind \"3\"^^xsd:int . ?x ex:group ?g . ?g ex:item ?i }";

        // the items are looked up for the groups of the nodes, which are seven nodes, so the rows found are kept
        String sql = check(query, 0);
        assertThat(sql, containsString(" AS " + PlanAdvice.MEMOIZED_LOOKUP + " WHERE "));

        String plan = explain(sql);
        assertFollowed(plan);
        assertThat(plan, containsString("Memoize"));
        assertThat(plan, containsString("on items " + PlanAdvice.MEMOIZED_LOOKUP));
    }


    @Test
    public void lookupInExistsFenced() throws Exception
    {
        String query = "SELECT ?x WHERE { ?x ex:kind \"3\"^^xsd:int FILTER EXISTS { ?x ex:link ?y . ?y ex:link ?z } }";

        // the lookup in the pattern of the expression, which the database evaluates for every node, stays fenced, so
        // the statement is not planned for advice
        List<List<RdfTerm>> expected = expected(query);

        try(Request request = new Engine(advised).getRequest())
        {
            int before = queries.size();

            try(Result result = request.execute(prefix + query))
            {
                assertThat(read(result), containsInAnyOrder(expected.toArray()));
            }

            String sql = queries.getLast();
            assertThat(sql, containsString(" OFFSET 0) AS "));
            assertThat(sql, not(containsString(" AS " + PlanAdvice.LOOKUP + " ")));
            assertThat(queries.subList(before, queries.size()).stream().anyMatch(q -> q.startsWith("EXPLAIN")),
                    is(false));
        }
    }


    @Test
    public void optionalAdvised() throws Exception
    {
        String query = "SELECT ?x ?y WHERE { ?x ex:kind \"3\"^^xsd:int OPTIONAL { ?x ex:link ?y } }";

        // the right side looked up for every node is a plain left join under the alias of a lookup
        String sql = check(query, 0);
        assertThat(sql, containsString(" LEFT JOIN LATERAL (SELECT "));
        assertThat(sql, containsString(" AS " + PlanAdvice.LOOKUP + " WHERE "));
        assertThat(sql, not(containsString("ANY(ARRAY[")));
        assertThat(sql, not(containsString(" OFFSET 0")));

        String plan = explain(sql);
        assertFollowed(plan);
        assertThat(plan, containsString("Gather"));
        assertThat(plan.indexOf("Nested Loop Left Join"), greaterThan(plan.indexOf("Gather")));
        assertThat(plan, not(containsString("Seq Scan on links")));
        assertThat(plan, not(containsString("Hash")));
    }
}
