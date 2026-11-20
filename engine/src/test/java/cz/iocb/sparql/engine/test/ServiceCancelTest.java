package cz.iocb.sparql.engine.test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
 * SERVICE calls of a cancelled request (see {@link Request#cancel}): a request without a time limit that waits for an
 * endpoint not answering, or reads an answer sent slowly, ends within a fraction of a second once it is cancelled by
 * another thread, also for a SILENT call, whose failure the cancellation is not, and the connection to the endpoint is
 * closed. The endpoints are served on the loopback interface: {@code silent} answers nothing for ten seconds,
 * {@code slow} sends the beginning of an answer and then a space every 100 milliseconds for ten seconds; both tell when
 * a request has arrived, and {@code slow} also when the request has closed the connection.
 */
public class ServiceCancelTest
{
    /**
     * Time in milliseconds by which a cancelled request must end, far below the ten seconds the endpoints hold a
     * request for.
     */
    private static final long bound = 3_000L;

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
     * Counted down when a request arrives at an endpoint.
     */
    private static volatile CountDownLatch arrived = null;

    /**
     * Counted down when the endpoint answering slowly finds its connection closed.
     */
    private static volatile CountDownLatch closed = null;


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
        server.createContext("/silent", ServiceCancelTest::answerNothing);
        server.createContext("/slow", ServiceCancelTest::answerSlowly);
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
        arrived.countDown();

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
     * seconds, telling when the connection is found closed.
     *
     * @param exchange the request
     */
    private static void answerSlowly(HttpExchange exchange)
    {
        try
        {
            exchange.getRequestBody().readAllBytes();
            exchange.getResponseHeaders().add("content-type", "application/sparql-results+xml");
            exchange.sendResponseHeaders(200, 0);

            try(OutputStream out = exchange.getResponseBody())
            {
                out.write("<?xml version=\"1.0\"?><sparql xmlns=\"http://www.w3.org/2005/sparql-results#\">"
                        .getBytes(UTF_8));
                out.flush();
                arrived.countDown();

                for(int i = 0; i < 100; i++)
                {
                    out.write(' ');
                    out.flush();
                    Thread.sleep(100);
                }
            }
        }
        catch(IOException e)
        {
            closed.countDown();
        }
        catch(InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }


    /**
     * Runs the query without a time limit by another thread, cancels the request once the endpoint has the call, and
     * checks that the evaluation ends by the bound with an error.
     *
     * @param query the query
     * @throws Exception on errors of the test itself
     */
    private static void assertCancelled(String query) throws Exception
    {
        arrived = new CountDownLatch(1);
        closed = new CountDownLatch(1);

        try(ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
                Request request = engine.getRequest())
        {
            Future<?> evaluation = executor.submit(() -> {
                try(Result result = request.execute(query, 0, -1, 0))
                {
                    while(result.next())
                        result.getRow();
                }

                return null;
            });

            assertTrue(arrived.await(10, TimeUnit.SECONDS), "the call has not arrived");

            long start = System.nanoTime();
            request.cancel();

            ExecutionException exception = assertThrows(ExecutionException.class,
                    () -> evaluation.get(bound, TimeUnit.MILLISECONDS));

            assertThat(exception.getCause(), instanceOf(SQLException.class));
            assertThat((System.nanoTime() - start) / 1_000_000L, lessThan(bound));
        }
    }


    @Test
    @DisplayName("a call waiting for an endpoint that does not answer ends with the cancelled request")
    void endpointAnsweringNothing() throws Exception
    {
        assertCancelled("SELECT * WHERE { SERVICE <" + silent + "> { ?s ?p ?o } }");
    }


    @Test
    @DisplayName("a SILENT call waiting for an endpoint that does not answer ends the cancelled request")
    void silentCallOfEndpointAnsweringNothing() throws Exception
    {
        assertCancelled("SELECT * WHERE { SERVICE SILENT <" + silent + "> { ?s ?p ?o } }");
    }


    @Test
    @DisplayName("a call reading a slow answer ends with the cancelled request and closes the connection")
    void endpointAnsweringSlowly() throws Exception
    {
        assertCancelled("SELECT * WHERE { SERVICE <" + slow + "> { ?s ?p ?o } }");
        assertTrue(closed.await(bound, TimeUnit.MILLISECONDS), "the connection has not been closed");
    }
}
