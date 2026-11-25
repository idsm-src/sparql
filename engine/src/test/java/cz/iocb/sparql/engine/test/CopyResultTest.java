package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.postgresql.PGConnection;
import org.postgresql.copy.CopyManager;
import org.postgresql.copy.CopyOut;
import org.postgresql.core.BaseConnection;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.database.VirtualTable;
import cz.iocb.sparql.engine.database.VirtualTableDefinition;
import cz.iocb.sparql.engine.mapping.classes.IntegerUserIriClass;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.testing.Database;



/**
 * The rows of a result read in parts, which the database copies to the client where the driver lets it: the database
 * evaluates the copied statement by parallel workers and sends its rows as they are read, a cancellation of the request
 * and its time limit end the copy, also a cancellation that comes before the statement of the copy reaches the
 * database, a result closed before its end leaves the request usable, the warnings the database raises while it copies
 * are those of the result, and the terms are those of the result read whole.
 *
 * The table {@code copy_result.texts} holds 100000 texts of 200 characters, mapped by ex:text; the virtual table
 * {@code slow} gives its row after 30 seconds, mapped by ex:slow.
 */
public class CopyResultTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> "
            + "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> ";

    /**
     * The settings that make the planner choose a parallel plan wherever one is possible.
     */
    private static final String parallel = "SET LOCAL parallel_setup_cost = 0; SET LOCAL parallel_tuple_cost = 0; "
            + "SET LOCAL min_parallel_table_scan_size = 0";

    /**
     * Engine over the configuration mapping the tables.
     */
    private static Engine engine = null;

    /**
     * Engine over the configuration mapping the tables over connections that cancel the request {@link #cancelled} just
     * before they send the statement of a copy, which they send 300 milliseconds later.
     */
    private static Engine delaying = null;

    /**
     * The request the connections of {@link #delaying} cancel.
     */
    private static volatile Request cancelled = null;


    /**
     * Creates the {@code copy_result} schema with the texts and the engines over the configurations mapping it and the
     * slow virtual table.
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema copy_result");
            statement.execute("create table copy_result.texts (id integer primary key, text varchar not null)");
            statement.execute("insert into copy_result.texts select i, repeat(chr(65 + i % 26), 200) "
                    + "from generate_series(1, 100000) as i");
            statement.execute("analyze copy_result.texts");
        }

        engine = new Engine(configure(connectionPool));
        delaying = new Engine(configure(delay(DataSource.class, connectionPool)));
    }


    /**
     * Creates the configuration mapping the texts and the slow virtual table over the connection pool.
     */
    private static SparqlDatabaseConfiguration configure(DataSource connectionPool) throws SQLException
    {
        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, connectionPool,
                new DatabaseSchema(connectionPool), true);

        config.addPrefix("ex", "http://example.org/");
        config.addIriClass(new IntegerUserIriClass("item", INT4, "http://example.org/item/"));
        config.addQuadMapping(new DatabaseTable("copy_result", "texts"), null, config.createIriMapping("item", "id"),
                config.createIriMapping("ex:text"), config.createLiteralMapping(xsdString, "text"));

        VirtualTable slow = new VirtualTable("slow");
        VirtualTableDefinition definition = new VirtualTableDefinition("SELECT 1::int4 AS id FROM pg_sleep(30)");
        definition.addColumn(new TableColumn("id", INT4));
        config.addVirtualTable(slow, definition);
        config.addQuadMapping(slow, null, config.createIriMapping("item", "id"), config.createIriMapping("ex:slow"),
                config.createLiteralMapping(xsdInt, "id"));

        return config;
    }


    /**
     * Wraps a connection pool, a connection or a connection of the driver so that the copy API of the connection of the
     * driver cancels the request {@link #cancelled} just before it sends the statement of a copy, which it sends 300
     * milliseconds later; the connections it gives are wrapped likewise.
     */
    private static <T> T delay(Class<T> type, T target)
    {
        return type.cast(Proxy.newProxyInstance(CopyResultTest.class.getClassLoader(), new Class<?>[] { type },
                (_, method, arguments) -> {
                    if(target instanceof PGConnection driver && method.getName().equals("getCopyAPI"))
                        return delayedCopyManager(driver);

                    try
                    {
                        Object result = method.invoke(target, arguments);

                        if(result instanceof Connection connection && method.getName().equals("getConnection"))
                            return delay(Connection.class, connection);

                        if(result instanceof PGConnection driver && method.getName().equals("unwrap"))
                            return delay(PGConnection.class, driver);

                        return result;
                    }
                    catch(InvocationTargetException e)
                    {
                        throw e.getCause();
                    }
                }));
    }


    /**
     * The copy API of the connection of the driver for {@link #delay}.
     */
    private static CopyManager delayedCopyManager(PGConnection driver) throws SQLException
    {
        return new CopyManager((BaseConnection) driver)
        {
            @Override
            public CopyOut copyOut(String sql) throws SQLException
            {
                cancelled.cancel();

                try
                {
                    Thread.sleep(300);
                }
                catch(InterruptedException e)
                {
                    throw new SQLException(e);
                }

                return super.copyOut(sql);
            }
        };
    }


    /**
     * The process of the database that serves the connection of the request.
     */
    private static int backend(Request request) throws SQLException
    {
        try(ResultSet result = request.getStatement().executeQuery("SELECT pg_backend_pid()"))
        {
            result.next();
            return result.getInt(1);
        }
    }


    /**
     * Reads a value of the activity of the backends of the database by a connection of its own.
     */
    private static String activity(String query, Object parameter) throws SQLException
    {
        try(Connection connection = Database.getPool().getConnection();
                PreparedStatement statement = connection.prepareStatement(query))
        {
            // the activity of the backends is read once in a transaction, and the pool gives the connection back
            // without the auto-commit mode that a request has turned off
            connection.setAutoCommit(true);
            statement.setObject(1, parameter);

            try(ResultSet result = statement.executeQuery())
            {
                result.next();
                return result.getString(1);
            }
        }
    }


    /**
     * Number of the backends of the database, not idle, whose current or last statement has the text.
     */
    private static int running(String text) throws SQLException
    {
        return Integer.parseInt(activity("""
                SELECT count(*) FROM pg_stat_activity
                WHERE datname = current_database() AND pid <> pg_backend_pid() AND state <> 'idle'
                    AND strpos(query, ?) > 0""", text));
    }


    /**
     * Reads all rows of the result of the query read with the fetch size.
     */
    private static List<List<RdfTerm>> read(String query, int fetchSize) throws Exception
    {
        try(Request request = engine.getRequest(); Result result = request.execute(query, null, 0, -1, fetchSize, 0))
        {
            List<List<RdfTerm>> rows = new ArrayList<>();

            while(result.next())
                rows.add(Arrays.asList(result.getRow()));

            return rows;
        }
    }


    @Test
    @DisplayName("the database evaluates a copied statement by parallel workers and sends its rows as they are read")
    void copiedByWorkers() throws Exception
    {
        try(Request request = engine.getRequest())
        {
            request.getStatement().execute(parallel);
            int pid = backend(request);

            try(Result result = request.execute(prefix + "SELECT ?s ?t WHERE { ?s ex:text ?t }", null, 0, -1, 1000, 0))
            {
                assertThat(result.next(), is(true));

                // the statement and its workers wait for the rows to be read
                assertThat(activity("SELECT state FROM pg_stat_activity WHERE pid = ?", pid), is("active"));
                assertThat(
                        Integer.parseInt(activity("SELECT count(*) FROM pg_stat_activity WHERE leader_pid = ?", pid)),
                        greaterThan(0));
            }

            // the result closed before its end leaves the request usable
            assertThat(activity("SELECT state FROM pg_stat_activity WHERE pid = ?", pid), is("idle in transaction"));

            String query = prefix + "SELECT ?t WHERE { <http://example.org/item/7> ex:text ?t } ORDER BY ?t";

            try(Result result = request.execute(query, null, 0, -1, 1000, 0))
            {
                assertThat(result.next(), is(true));
                assertThat(result.get(0).toString(), containsString("HHHHHHHH"));
                assertThat(result.next(), is(false));
            }
        }
    }


    @Test
    @DisplayName("the warnings the database raises while it copies are those of the result")
    void warnings() throws Exception
    {
        try(Request request = engine.getRequest())
        {
            request.getStatement().execute("LOAD 'auto_explain'");
            request.getStatement().execute(parallel);
            request.getStatement().execute("SET LOCAL auto_explain.log_min_duration = 0; "
                    + "SET LOCAL auto_explain.log_analyze = on; SET LOCAL auto_explain.log_level = notice");

            try(Result result = request.execute(prefix + "SELECT ?s ?t WHERE { ?s ex:text ?t }", null, 0, -1, 1000, 0))
            {
                int rows = 0;

                while(result.next())
                    rows++;

                assertThat(rows, is(100000));

                // the plan of the copy, which the database reports when the copy ends, has had workers
                String plan = String.join("\n", result.getWarnings());
                assertThat(plan, containsString("Workers Launched: "));
                assertThat(plan, not(containsString("Workers Launched: 0")));
            }
        }
    }


    @Test
    @DisplayName("a cancellation of the request ends the copy")
    void cancelled() throws Exception
    {
        try(Request request = engine.getRequest())
        {
            String query = prefix + "SELECT ?s ?v WHERE { ?s ex:slow ?v }";
            long begin = System.nanoTime();

            Thread canceller = Thread.ofVirtual().start(() -> {
                try
                {
                    Thread.sleep(500);
                    request.cancel();
                }
                catch(InterruptedException | SQLException e)
                {
                    throw new RuntimeException(e);
                }
            });

            assertThrows(SQLException.class, () -> {
                try(Result result = request.execute(query, null, 0, -1, 10, 0))
                {
                    result.next();
                }
            });

            canceller.join();
            assertThat((System.nanoTime() - begin) / 1e9, lessThan(10.0));
        }

        // the database has stopped the statement
        assertThat(running("pg_sleep(30)"), is(0));
    }


    @Test
    @DisplayName("a cancellation that comes before the statement of the copy reaches the database ends the copy")
    void cancelledBeforeStart() throws Exception
    {
        try(Request request = delaying.getRequest())
        {
            String query = prefix + "SELECT ?s ?v WHERE { ?s ex:slow ?v }";
            long begin = System.nanoTime();
            cancelled = request;

            assertThrows(SQLException.class, () -> {
                try(Result result = request.execute(query, null, 0, -1, 10, 0))
                {
                    result.next();
                }
            });

            assertThat((System.nanoTime() - begin) / 1e9, lessThan(10.0));
        }

        assertThat(running("pg_sleep(30)"), is(0));
    }


    @Test
    @DisplayName("the time limit of the request ends the copy")
    void timeLimit() throws Exception
    {
        try(Request request = engine.getRequest())
        {
            String query = prefix + "SELECT ?s ?v WHERE { ?s ex:slow ?v }";
            long begin = System.nanoTime();

            SQLException exception = assertThrows(SQLException.class, () -> {
                try(Result result = request.execute(query, null, 0, -1, 10, 1_000_000_000L))
                {
                    result.next();
                }
            });

            assertThat(exception.getSQLState(), is("57014"));
            assertThat((System.nanoTime() - begin) / 1e9, lessThan(10.0));
        }

        assertThat(running("pg_sleep(30)"), is(0));
    }


    @Test
    @DisplayName("the terms of a copied result are those of the result read whole")
    void terms() throws Exception
    {
        String query = prefix + """
                SELECT ?v WHERE { VALUES ?v { "a\\tb" "line\\nfeed\\\\" "" "x"@en "x"@en--ltr "y"@ar--rtl \
                "01"^^xsd:int 7 1.50 "NaN"^^xsd:double "INF"^^xsd:float "-0"^^xsd:double 1.5e-7 \
                "2020-01-01T10:00:00.25+05:30"^^xsd:dateTime "2020-01-01T10:00:00"^^xsd:dateTime \
                "-0044-03-15"^^xsd:date "2020-01-01Z"^^xsd:date "P1DT2H"^^xsd:dayTimeDuration true \
                "x"^^<http://example.org/unknown> <http://example.org/a> \
                <<( <http://example.org/a> <http://example.org/b> "c\\td" )>> } }""";

        List<List<RdfTerm>> terms = read(query, 0);

        assertThat(terms, hasSize(22));
        assertThat(read(query, 10), is(terms));
    }
}
