package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.imcode.SqlSelect;
import cz.iocb.sparql.engine.mapping.classes.IntegerUserIriClass;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Request.PreparedQuery;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.translator.TranslateVisitor;
import cz.iocb.sparql.testing.Database;



/**
 * The lookups a request makes in the database while it translates a query (see {@link Request#lookup}): the queries
 * given together cost one call, a failing query fails them all without aborting the transaction of the request, and the
 * join optimizer asks the planner for the estimates of a filter by a single call.
 */
public class LookupTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> ";

    /**
     * Number of the calls that the statements of the configuration have made to the database.
     */
    private static final AtomicInteger calls = new AtomicInteger();

    /**
     * Configuration with the join optimizer on, whose calls to the database are counted.
     */
    private static SparqlDatabaseConfiguration config = null;


    /**
     * Creates the {@code lookup_test} schema: the nodes 1 to 1000 with scores equal to their numbers and 2000 links
     * among the nodes 1 to 200, indexed and analysed; and the configuration mapping {@code ex:score} and
     * {@code ex:link} over them through a connection pool that counts the calls of its statements.
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema lookup_test");
            statement.execute("create table lookup_test.scores (id integer primary key, score integer not null)");
            statement.execute("insert into lookup_test.scores select i, i from generate_series(1, 1000) as i");
            statement.execute("create index on lookup_test.scores (score)");
            statement.execute("create table lookup_test.links (src integer not null, dst integer not null)");
            statement.execute("insert into lookup_test.links select i % 200 + 1, (i * 7 + i / 200) % 200 + 1 "
                    + "from generate_series(1, 2000) as i");
            statement.execute("create index on lookup_test.links (src)");
            statement.execute("analyze lookup_test.scores, lookup_test.links");
        }

        config = new SparqlDatabaseConfiguration(null, count(DataSource.class, connectionPool),
                new DatabaseSchema(connectionPool), true);

        config.addPrefix("ex", "http://example.org/");
        config.addIriClass(new IntegerUserIriClass("node", INT4, "http://example.org/n/"));

        DatabaseTable scores = new DatabaseTable("lookup_test", "scores");
        DatabaseTable links = new DatabaseTable("lookup_test", "links");

        config.addQuadMapping(scores, null, config.createIriMapping("node", "id"), config.createIriMapping("ex:score"),
                config.createLiteralMapping(xsdInt, "score"));
        config.addQuadMapping(links, null, config.createIriMapping("node", "src"), config.createIriMapping("ex:link"),
                config.createIriMapping("node", "dst"));

        config.setJoinOptimizer(true);
    }


    /**
     * Wraps a connection pool, a connection or a statement so that every execution of a statement is counted; the
     * connections and the statements it gives are wrapped likewise.
     */
    private static <T> T count(Class<T> type, T target)
    {
        return type.cast(Proxy.newProxyInstance(LookupTest.class.getClassLoader(), new Class<?>[] { type },
                (_, method, arguments) -> call(target, method, arguments)));
    }


    /**
     * Calls the method of the wrapped object for {@link #count}.
     */
    private static Object call(Object target, Method method, Object[] arguments) throws Throwable
    {
        if(target instanceof Statement && method.getName().startsWith("execute"))
            calls.incrementAndGet();

        try
        {
            Object result = method.invoke(target, arguments);

            if(result instanceof Connection connection && method.getName().equals("getConnection"))
                return count(Connection.class, connection);

            if(result instanceof Statement statement && method.getName().equals("createStatement"))
                return count(Statement.class, statement);

            return result;
        }
        catch(InvocationTargetException e)
        {
            throw e.getCause();
        }
    }


    /**
     * Reads the first column of the result of a lookup query.
     */
    private static List<String> read(ResultSet result) throws SQLException
    {
        List<String> values = new ArrayList<>();

        while(result.next())
            values.add(result.getString(1));

        return values;
    }


    /**
     * Returns the SQL code the query is translated to.
     */
    private static String translate(Request request, String query) throws Exception
    {
        PreparedQuery prepared = request.prepareQuery(prefix + query, null);
        SqlSelect imcode = new TranslateVisitor(request).translate(prepared.getSyntaxTree(), null, null, List.of());

        return imcode.optimize(request, false).optimize(request, true).translate(request);
    }


    @Test
    public void results() throws Exception
    {
        try(Request request = new Engine(config).getRequest())
        {
            int before = calls.get();

            // the queries are separated by semicolons, which a string holding one is not split at
            assertThat(request.lookup(List.of("SELECT 1", "SELECT 2 UNION ALL SELECT 3", "SELECT 'a;b'"),
                    LookupTest::read), contains(List.of("1"), List.of("2", "3"), List.of("a;b")));

            assertThat(calls.get() - before, is(1));

            // no query needs no call
            assertThat(request.lookup(List.of(), LookupTest::read), empty());
            assertThat(calls.get() - before, is(1));
        }
    }


    @Test
    public void failure() throws Exception
    {
        try(Request request = new Engine(config).getRequest())
        {
            request.getStatement().execute("SET LOCAL application_name = 'lookup test'");

            // a failing query fails the queries given with it
            SQLException exception = assertThrows(SQLException.class,
                    () -> request.lookup(List.of("SELECT 1", "SELECT 1 / (SELECT 0)", "SELECT 2"), LookupTest::read));

            assertThat(exception.getMessage(), containsString("division by zero"));

            // the transaction of the request goes on, with what it has set before
            assertThat(request.lookup(List.of("SHOW application_name"), LookupTest::read),
                    contains(List.of("lookup test")));

            // so it does when the result of a query cannot be read
            assertThrows(SQLException.class, () -> request.lookup(List.of("SELECT 1"), _ -> {
                throw new SQLException("unreadable");
            }));

            assertThat(request.lookup(List.of("SELECT 3"), LookupTest::read), contains(List.of("3")));

            // and the query of the request is evaluated
            try(Result result = request.execute(prefix + "SELECT ?s WHERE { <http://example.org/n/7> ex:score ?s }"))
            {
                assertThat(result.next(), is(true));
            }
        }
    }


    @Test
    public void planEstimates() throws Exception
    {
        String query = "SELECT ?x ?y WHERE { ?x ex:score ?s . ?x ex:link ?y FILTER(?s < 5) }";

        // the first translation reads the statistics of the tables, which the configuration keeps
        try(Request request = new Engine(config).getRequest())
        {
            translate(request, query);
        }

        try(Request request = new Engine(config).getRequest())
        {
            int before = calls.get();

            // the planner is asked how many scores there are and how many of them the filter keeps, by one call
            assertThat(translate(request, query), containsString(" CROSS JOIN LATERAL (SELECT "));
            assertThat(calls.get() - before, is(1));
            assertThat(request.getRowEstimates().values(), hasSize(2));

            // and it is not asked again in the request
            translate(request, query);
            assertThat(calls.get() - before, is(1));
        }

        try(Request request = new Engine(config).getRequest())
        {
            int before = calls.get();

            // an evaluation adds a call asking whether the session allows parallel workers, a call setting the planner
            // up for the joins the engine has chosen and the query itself
            try(Result result = request.execute(prefix + query))
            {
                int rows = 0;

                while(result.next())
                    rows++;

                assertThat(rows, is(40));
            }

            assertThat(calls.get() - before, is(4));
        }
    }
}
