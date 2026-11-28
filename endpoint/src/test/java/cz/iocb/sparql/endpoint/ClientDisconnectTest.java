package cz.iocb.sparql.endpoint;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInt;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.coyote.http2.Http2Protocol;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.database.VirtualTable;
import cz.iocb.sparql.engine.database.VirtualTableDefinition;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.testing.Database;



/**
 * The requests whose clients close the connection. Served by Tomcat over HTTP/1.1 and over AJP, a request is cancelled
 * by the watch of the connection (see {@link ClientWatch}) soon after its client has closed the connection, while its
 * SQL statement runs, while it waits for a SERVICE call, which closes its connection then, and while it writes its
 * result; a request the client sends before it has the response to the previous one is served as it would be without
 * the watch. A result is written no further by the check of the writer (see {@link EndpointServlet.ClientCheck}) once
 * the client has reset its stream of HTTP/2, which the watch does not watch, and once the writer of a stub response
 * fails, as the writer of a response to a client that has gone does. The queries read virtual tables: {@code slow}
 * gives a row after thirty seconds, {@code pause} after one second and {@code many} gives a hundred million rows; a
 * statement runs while a backend of the database, not idle, has its text, and the SERVICE endpoint holds a call until
 * the call closes the connection.
 */
public class ClientDisconnectTest
{
    /**
     * Time in milliseconds by which a request must end once its client has closed the connection.
     */
    private static final long bound = 5_000L;

    /**
     * Text of the statement reading the virtual table {@code slow}.
     */
    private static final String slowText = "pg_sleep(30)";

    /**
     * Text of the statement reading the virtual table {@code many}.
     */
    private static final String manyText = "generate_series(1, 100000000)";

    /**
     * Query reading the virtual table {@code slow}.
     */
    private static final String slowQuery = "SELECT * WHERE { <http://example.org/s> <http://example.org/slow> ?o }";

    /**
     * Query reading the virtual table {@code many}.
     */
    private static final String manyQuery = "SELECT * WHERE { <http://example.org/s> <http://example.org/many> ?o }";

    /**
     * Query reading the virtual table {@code pause}.
     */
    private static final String pauseQuery = "SELECT * WHERE { <http://example.org/s> <http://example.org/pause> ?o }";

    /**
     * Directory of Tomcat.
     */
    @TempDir
    static Path base;

    /**
     * Engine over the configuration mapping the virtual tables.
     */
    private static Engine engine = null;

    /**
     * Tomcat serving the servlet.
     */
    private static Tomcat tomcat = null;

    /**
     * Port of the HTTP/1.1 connector.
     */
    private static int httpPort = 0;

    /**
     * Port of the AJP connector.
     */
    private static int ajpPort = 0;

    /**
     * The SERVICE endpoint, holding every call until the call closes the connection.
     */
    private static ServerSocket endpoint = null;

    /**
     * Counted down when a call arrives at the SERVICE endpoint.
     */
    private static volatile CountDownLatch called = null;

    /**
     * Counted down when a call closes its connection to the SERVICE endpoint.
     */
    private static volatile CountDownLatch released = null;


    /**
     * The servlet over the engine, initialized without the configuration of a deployment.
     */
    private static final class TestServlet extends EndpointServlet
    {
        /**
         * Serialization version.
         */
        private static final long serialVersionUID = 1L;


        /**
         * Creates the servlet over the engine.
         *
         * @param engine the engine
         */
        TestServlet(Engine engine)
        {
            super(engine);
        }


        @Override
        public void init(ServletConfig config)
        {
        }
    }


    /**
     * Starts the SERVICE endpoint and Tomcat with the HTTP/1.1 and AJP connectors on free ports of the loopback
     * interface, serving the servlet over the configuration mapping the virtual tables.
     *
     * @throws Exception if the servers cannot be started
     */
    @BeforeAll
    static void init() throws Exception
    {
        endpoint = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
        Thread.ofVirtual().start(ClientDisconnectTest::serveEndpoint);

        DataSource connectionPool = Database.getPool();
        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, connectionPool,
                new DatabaseSchema(connectionPool), false);

        addTable(config, "slow", "SELECT 1::int4 AS id FROM pg_sleep(30)");
        addTable(config, "pause", "SELECT 1::int4 AS id FROM pg_sleep(1)");
        // a function of the select list gives its rows one by one, whereas one of FROM gives all of them at once
        addTable(config, "many", "SELECT generate_series(1, 100000000)::int4 AS id");

        engine = new Engine(config);

        tomcat = new Tomcat();
        tomcat.setBaseDir(base.toString());

        Connector http = new Connector("HTTP/1.1");
        http.setPort(0);
        http.setProperty("address", "127.0.0.1");
        http.addUpgradeProtocol(new Http2Protocol());
        tomcat.getService().addConnector(http);

        Connector ajp = new Connector("AJP/1.3");
        ajp.setPort(0);
        ajp.setProperty("address", "127.0.0.1");
        ajp.setProperty("secretRequired", "false");
        tomcat.getService().addConnector(ajp);

        Context context = tomcat.addContext("", base.toString());
        Tomcat.addServlet(context, "sparql", new TestServlet(engine));
        context.addServletMapping("/sparql", "sparql");

        tomcat.start();

        httpPort = http.getLocalPort();
        ajpPort = ajp.getLocalPort();
    }


    /**
     * Stops the servers.
     *
     * @throws Exception if the servers cannot be stopped
     */
    @AfterAll
    static void stop() throws Exception
    {
        tomcat.stop();
        tomcat.destroy();
        endpoint.close();
    }


    /**
     * Maps a virtual table of one integer column {@code id} as the objects of the triples of the subject {@code ex:s}
     * and the predicate {@code ex:<name>}.
     *
     * @param config the configuration
     * @param name the name of the table
     * @param query the query of the table
     */
    private static void addTable(SparqlDatabaseConfiguration config, String name, String query)
    {
        VirtualTable table = new VirtualTable(name);
        VirtualTableDefinition definition = new VirtualTableDefinition(query);
        definition.addColumn(new TableColumn("id", INT4));
        config.addVirtualTable(table, definition);

        config.addQuadMapping(table, null, config.createIriMapping(new Iri("http://example.org/s")),
                config.createIriMapping(new Iri("http://example.org/" + name)),
                config.createLiteralMapping(xsdInt, "id"));
    }


    /**
     * Accepts the calls of the SERVICE endpoint, each held by a thread of its own.
     */
    private static void serveEndpoint()
    {
        while(true)
        {
            try
            {
                Socket socket = endpoint.accept();
                Thread.ofVirtual().start(() -> holdCall(socket));
            }
            catch(IOException e)
            {
                return;
            }
        }
    }


    /**
     * Reads a call until it closes the connection, answering nothing.
     *
     * @param socket the connection of the call
     */
    private static void holdCall(Socket socket)
    {
        try(socket)
        {
            InputStream input = socket.getInputStream();
            byte[] buffer = new byte[8192];

            if(input.read(buffer) > 0)
                called.countDown();

            while(input.read(buffer) >= 0)
                continue;
        }
        catch(IOException e)
        {
            // the call has reset the connection
        }

        released.countDown();
    }


    /**
     * Number of the backends of the database, not idle, whose current or last statement has the text.
     *
     * @param text the text
     * @return number of the backends, not idle, whose current or last statement has the text
     * @throws SQLException on database errors
     */
    private static int running(String text) throws SQLException
    {
        try(Connection connection = Database.getPool().getConnection();
                PreparedStatement statement = connection.prepareStatement("""
                        SELECT count(*) FROM pg_stat_activity
                        WHERE datname = current_database() AND pid <> pg_backend_pid() AND state <> 'idle'
                            AND strpos(query, ?) > 0"""))
        {
            // the activity of the backends is read once in a transaction, and the pool gives the connection back
            // without the auto-commit mode that a request has turned off
            connection.setAutoCommit(true);
            statement.setString(1, text);

            try(ResultSet result = statement.executeQuery())
            {
                result.next();
                return result.getInt(1);
            }
        }
    }


    /**
     * Waits until a statement with the text runs, or no longer runs.
     *
     * @param text the text of the statement
     * @param runs whether to wait until the statement runs, or until it no longer runs
     * @param millis the longest time to wait in milliseconds
     * @throws Exception if the wait is interrupted or the database fails
     */
    private static void awaitRunning(String text, boolean runs, long millis) throws Exception
    {
        long end = System.nanoTime() + millis * 1_000_000L;

        while((running(text) > 0) != runs)
        {
            if(System.nanoTime() > end)
                fail("the statement " + (runs ? "has not started" : "still runs") + " after " + millis + " ms");

            Thread.sleep(50);
        }
    }


    /**
     * Sends a GET request of the query to the HTTP/1.1 connector, asking for the SPARQL JSON results.
     *
     * @param socket the connection
     * @param query the query
     * @param close whether to ask Tomcat to close the connection after the response
     * @throws IOException on communication errors
     */
    private static void sendHttp(Socket socket, String query, boolean close) throws IOException
    {
        OutputStream output = socket.getOutputStream();
        String request = "GET /sparql?query=" + URLEncoder.encode(query, UTF_8) + " HTTP/1.1\r\nHost: localhost\r\n"
                + "Accept: application/sparql-results+json\r\n" + (close ? "Connection: close\r\n" : "") + "\r\n";

        output.write(request.getBytes(US_ASCII));
        output.flush();
    }


    /**
     * Sends a GET request of the query to the AJP connector, as Apache httpd forwards it, asking for the SPARQL JSON
     * results.
     *
     * @param socket the connection
     * @param query the query
     * @throws IOException on communication errors
     */
    private static void sendAjp(Socket socket, String query) throws IOException
    {
        ByteArrayOutputStream message = new ByteArrayOutputStream();

        message.write(2); // forward request
        message.write(2); // GET
        writeAjpString(message, "HTTP/1.1");
        writeAjpString(message, "/sparql");
        writeAjpString(message, "127.0.0.1"); // address of the client
        writeAjpString(message, "localhost"); // host of the client
        writeAjpString(message, "localhost"); // name of the server
        writeAjpInteger(message, 80); // port of the server
        message.write(0); // no TLS
        writeAjpInteger(message, 2); // headers
        writeAjpInteger(message, 0xA00B); // Host
        writeAjpString(message, "localhost");
        writeAjpInteger(message, 0xA001); // Accept
        writeAjpString(message, "application/sparql-results+json");
        message.write(0x05); // query string
        writeAjpString(message, "query=" + URLEncoder.encode(query, UTF_8));
        message.write(0xFF); // end of the attributes

        OutputStream output = socket.getOutputStream();
        output.write(new byte[] { 0x12, 0x34, (byte) (message.size() >> 8), (byte) message.size() });
        message.writeTo(output);
        output.flush();
    }


    /**
     * Writes a string to an AJP message: its length, its bytes and a zero byte.
     *
     * @param message the message
     * @param value the string
     */
    private static void writeAjpString(ByteArrayOutputStream message, String value)
    {
        byte[] bytes = value.getBytes(UTF_8);

        writeAjpInteger(message, bytes.length);
        message.writeBytes(bytes);
        message.write(0);
    }


    /**
     * Writes an integer to an AJP message as two bytes.
     *
     * @param message the message
     * @param value the integer
     */
    private static void writeAjpInteger(ByteArrayOutputStream message, int value)
    {
        message.write(value >> 8);
        message.write(value);
    }


    @Test
    @DisplayName("the statement of a request ends soon after its HTTP/1.1 client has closed the connection")
    void httpClientClosingWhileStatementRuns() throws Exception
    {
        try(Socket socket = new Socket(InetAddress.getLoopbackAddress(), httpPort))
        {
            sendHttp(socket, slowQuery, false);
            awaitRunning(slowText, true, bound);
        }

        awaitRunning(slowText, false, bound);
    }


    @Test
    @DisplayName("the statement of a request ends soon after Apache httpd has closed the AJP connection")
    void ajpClientClosingWhileStatementRuns() throws Exception
    {
        try(Socket socket = new Socket(InetAddress.getLoopbackAddress(), ajpPort))
        {
            sendAjp(socket, slowQuery);
            awaitRunning(slowText, true, bound);
        }

        awaitRunning(slowText, false, bound);
    }


    @Test
    @DisplayName("a SERVICE call closes its connection soon after the client of the request has closed its own")
    void clientClosingWhileServiceCallWaits() throws Exception
    {
        called = new CountDownLatch(1);
        released = new CountDownLatch(1);

        try(Socket socket = new Socket(InetAddress.getLoopbackAddress(), httpPort))
        {
            sendHttp(socket, "SELECT * WHERE { SERVICE <http://127.0.0.1:" + endpoint.getLocalPort() + "/sparql> "
                    + "{ ?s ?p ?o } }", false);
            assertTrue(called.await(bound, TimeUnit.MILLISECONDS), "the call has not arrived");
        }

        assertTrue(released.await(bound, TimeUnit.MILLISECONDS), "the call has not closed its connection");
    }


    @Test
    @DisplayName("the statement of a request writing its result ends soon after the client has closed the connection")
    void clientClosingWhileResultIsWritten() throws Exception
    {
        try(Socket socket = new Socket(InetAddress.getLoopbackAddress(), httpPort))
        {
            sendHttp(socket, manyQuery, false);

            InputStream input = socket.getInputStream();
            byte[] buffer = new byte[8192];
            int length = 0;

            for(int count; length < 100_000 && (count = input.read(buffer)) >= 0;)
                length += count;

            assertTrue(length >= 100_000, "the result has ended");
        }

        awaitRunning(manyText, false, bound);
    }


    @Test
    @DisplayName("the statement of a request writing its result ends soon after the client has reset the HTTP/2 stream")
    void http2ClientResettingWhileResultIsWritten() throws Exception
    {
        URI uri = URI.create("http://127.0.0.1:" + httpPort + "/sparql?query=" + URLEncoder.encode(manyQuery, UTF_8));
        HttpRequest request = HttpRequest.newBuilder(uri).header("accept", "application/sparql-results+json").build();

        try(HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build())
        {
            HttpResponse<InputStream> response = client.send(request, BodyHandlers.ofInputStream());

            // a stream of HTTP/2 is not watched, so only the check of the writer ends the writing
            assertEquals(HttpClient.Version.HTTP_2, response.version());

            // closing the body before its end resets the stream
            try(InputStream input = response.body())
            {
                byte[] buffer = new byte[8192];
                int length = 0;

                for(int count; length < 100_000 && (count = input.read(buffer)) >= 0;)
                    length += count;

                assertTrue(length >= 100_000, "the result has ended");
            }

            awaitRunning(manyText, false, bound);
        }
    }


    @Test
    @DisplayName("a request sent before the response to the previous one is served")
    void pipelinedRequest() throws Exception
    {
        try(Socket socket = new Socket(InetAddress.getLoopbackAddress(), httpPort))
        {
            sendHttp(socket, pauseQuery, false);
            Thread.sleep(200);

            // the watch finds the second request while the first one runs, and gives it back to Tomcat
            sendHttp(socket, "ASK {}", true);

            String responses = new String(socket.getInputStream().readAllBytes(), UTF_8);

            assertEquals(2, responses.split("HTTP/1\\.1 200 ", -1).length - 1, responses);
            assertTrue(responses.contains("\"value\": \"1\""), responses);
            assertTrue(responses.contains("\"boolean\": true"), responses);
        }
    }


    @Test
    @DisplayName("a result is written no further once the writer of the response has failed")
    void failingWriter()
    {
        int failedRows = writeToFailingWriter(ServletSettings.DEFAULT);

        // at most the rows begun till the next check
        assertTrue(failedRows <= ServletSettings.DEFAULT.clientCheckRows(), failedRows + " rows begun");
    }


    @Test
    @DisplayName("the response is checked as often as the settings of the servlet say")
    void failingWriterCheckedOften()
    {
        // after every ten rows, whereas by default the rows begun till the next check are some five hundred
        int byRows = writeToFailingWriter(ServletSettings.DEFAULT.withClientCheckRows(10));

        assertTrue(byRows <= 10, byRows + " rows begun");

        // before every row, as no time has to pass between two checks
        int byTime = writeToFailingWriter(ServletSettings.DEFAULT.withClientCheckInterval(Duration.ZERO));

        assertEquals(1, byTime);
    }


    /**
     * Writes the result of a query of many rows in JSON by a servlet with the given settings to a response whose writer
     * fails, as the writer of a response to a client that has gone does once its buffer is sent, from the row in the
     * middle between two checks of the client by default, and checks that the response keeps the status it has been
     * committed with.
     *
     * @param settings the settings of the servlet
     * @return the number of the rows begun once the writer has failed, the row it has failed at included
     */
    private static int writeToFailingWriter(ServletSettings settings)
    {
        int[] rows = { 0 };
        int[] failedRows = { 0 };
        int[] written = { 0 };

        Writer failing = new Writer()
        {
            @Override
            public void write(char[] buffer, int offset, int length) throws IOException
            {
                // a row of the bindings begins by a write of its own
                boolean begins = new String(buffer, offset, length).equals("\t{");

                if(begins)
                    rows[0]++;

                if(rows[0] > 1500)
                {
                    if(begins)
                        failedRows[0]++;

                    throw new IOException("the client has gone");
                }

                written[0] += length;
            }


            @Override
            public void flush()
            {
            }


            @Override
            public void close()
            {
            }
        };

        PrintWriter writer = new PrintWriter(failing);
        int[] status = { HttpServletResponse.SC_OK };

        HttpServletResponse response = (HttpServletResponse) Proxy.newProxyInstance(
                HttpServletResponse.class.getClassLoader(), new Class<?>[] { HttpServletResponse.class },
                (_, method, args) -> switch(method.getName())
                {
                    case "getWriter" -> writer;
                    case "isCommitted" -> written[0] > 0;
                    case "setStatus" -> status[0] = (Integer) args[0];
                    case "getStatus" -> status[0];
                    default -> EndpointServletTest.defaultValue(method.getReturnType());
                });

        HttpServletRequest request = (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(), new Class<?>[] { HttpServletRequest.class },
                (_, method, args) -> switch(method.getName())
                {
                    case "getMethod" -> "GET";
                    case "getRequestURI" -> "/sparql";
                    case "getParameter" -> "query".equals(args[0]) ? manyQuery : null;
                    case "getHeader" -> "accept".equalsIgnoreCase((String) args[0]) ?
                            "application/sparql-results+json" : null;
                    default -> EndpointServletTest.defaultValue(method.getReturnType());
                });

        EndpointServlet servlet = new EndpointServlet(engine, settings);

        assertTimeoutPreemptively(Duration.ofSeconds(30), () -> servlet.doGet(request, response));
        assertEquals(HttpServletResponse.SC_OK, status[0]);

        return failedRows[0];
    }
}
