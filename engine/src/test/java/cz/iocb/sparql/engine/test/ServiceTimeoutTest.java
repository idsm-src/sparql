package cz.iocb.sparql.engine.test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.testing.Database;



/**
 * SERVICE calls bounded by the time limit of the request: an endpoint that does not answer, or that sends its answer so
 * slowly that no single read times out, ends the request with the SQL state 57014 of a cancelled statement once the
 * limit elapses, also for a SILENT call, instead of holding it until the endpoint gives up. The endpoints are served on
 * the loopback interface: {@code silent} answers nothing for ten seconds, {@code slow} sends the beginning of an answer
 * and then a space every 100 milliseconds for ten seconds.
 */
public class ServiceTimeoutTest
{
    /**
     * Time limit of the requests in nanoseconds.
     */
    private static final long timeout = 1_000_000_000L;

    /**
     * Time in milliseconds by which a request must end: the limit with a margin, far below the ten seconds the
     * endpoints hold a request for.
     */
    private static final long bound = 5_000L;

    /**
     * Engine over a configuration mapping nothing.
     */
    private static Engine engine = null;

    /**
     * The server of the endpoints.
     */
    private static HttpServer server = null;

    /**
     * IRI of the endpoint answering nothing.
     */
    private static String silent = null;

    /**
     * IRI of the endpoint answering slowly.
     */
    private static String slow = null;


    /**
     * Starts the endpoints on a free port of the loopback interface, each request served by a thread of its own, so
     * that a request still held does not delay the next one.
     *
     * @throws SQLException on database errors
     * @throws IOException if the endpoints cannot be started
     */
    @BeforeAll
    static void init() throws SQLException, IOException
    {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.createContext("/silent", ServiceTimeoutTest::answerNothing);
        server.createContext("/slow", ServiceTimeoutTest::answerSlowly);
        server.start();

        String base = "http://127.0.0.1:" + server.getAddress().getPort() + "/";

        silent = base + "silent";
        slow = base + "slow";

        DataSource connectionPool = Database.getPool();

        engine = new Engine(
                new SparqlDatabaseConfiguration(null, connectionPool, new DatabaseSchema(connectionPool), true));
    }


    /**
     * Stops the endpoints.
     */
    @AfterAll
    static void stop()
    {
        server.stop(0);
    }


    /**
     * Answers a request after ten seconds by a failure.
     *
     * @param exchange the request
     * @throws IOException on communication errors
     */
    private static void answerNothing(HttpExchange exchange) throws IOException
    {
        exchange.getRequestBody().readAllBytes();

        try
        {
            Thread.sleep(10_000);
        }
        catch(InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }

        exchange.sendResponseHeaders(500, -1);
        exchange.close();
    }


    /**
     * Answers a request by the beginning of a results document followed by a space every 100 milliseconds for ten
     * seconds.
     *
     * @param exchange the request
     * @throws IOException on communication errors, like the request having given up
     */
    private static void answerSlowly(HttpExchange exchange) throws IOException
    {
        exchange.getRequestBody().readAllBytes();
        exchange.getResponseHeaders().add("content-type", "application/sparql-results+xml");
        exchange.sendResponseHeaders(200, 0);

        try(OutputStream out = exchange.getResponseBody())
        {
            out.write(
                    "<?xml version=\"1.0\"?><sparql xmlns=\"http://www.w3.org/2005/sparql-results#\">".getBytes(UTF_8));

            for(int i = 0; i < 100; i++)
            {
                out.write(' ');
                out.flush();
                Thread.sleep(100);
            }
        }
        catch(InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }


    /**
     * Runs the query under the time limit and checks that it times out by the bound.
     *
     * @param query the query
     */
    private static void assertTimesOut(String query)
    {
        long start = System.nanoTime();

        SQLException exception = assertThrows(SQLException.class, () -> {
            try(Request request = engine.getRequest(); Result result = request.execute(query, 0, -1, timeout))
            {
                while(result.next())
                    result.getRow();
            }
        });

        assertThat(exception.getSQLState(), is("57014"));
        assertThat((System.nanoTime() - start) / 1_000_000L, lessThan(bound));
    }


    @Test
    @DisplayName("a call of an endpoint that does not answer times out with the request")
    void endpointAnsweringNothing()
    {
        assertTimesOut("SELECT * WHERE { SERVICE <" + silent + "> { ?s ?p ?o } }");
    }


    @Test
    @DisplayName("a SILENT call of an endpoint that does not answer times out with the request")
    void silentCallOfEndpointAnsweringNothing()
    {
        assertTimesOut("SELECT * WHERE { SERVICE SILENT <" + silent + "> { ?s ?p ?o } }");
    }


    @Test
    @DisplayName("a call of an endpoint answering slowly times out with the request")
    void endpointAnsweringSlowly()
    {
        assertTimesOut("SELECT * WHERE { SERVICE <" + slow + "> { ?s ?p ?o } }");
    }
}
