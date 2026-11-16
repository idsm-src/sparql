package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.mapping.ConstantIriMapping;
import cz.iocb.sparql.engine.mapping.classes.BuiltinClasses;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.TypedLiteral;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.LimitExceedException;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.translator.ServiceException;
import cz.iocb.sparql.testing.Database;



/**
 * Failing calls of SERVICE SILENT: a call to an unreachable endpoint yields the solutions of its context with the
 * variables of the call unbound, also when the context holds an earlier call, which must not stay in the evaluated
 * query unevaluated; the same call without SILENT is an error reported by the checked exception of the request. A call
 * naming its endpoint by a variable is a call of its own for every endpoint: a failing endpoint leaves the solutions
 * naming it as they are, also when it has answered an earlier request of the call, and the solutions naming the other
 * endpoints get what those answer. The failing endpoints are ports of the loopback address nothing listens on, so no
 * network is needed; the answering ones are served on the loopback interface: {@code ok} answers every request with the
 * solution {@code ?y = "remote"}, {@code flaky} answers so its first request only and fails then.
 */
public class SilentServiceTest
{
    /**
     * Prefix declaration prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> ";

    /**
     * Call of an unreachable endpoint binding {@code ?x} and {@code ?y}.
     */
    private static final String first = "SERVICE SILENT <http://127.0.0.1:1/sparql> { ?x ex:p ?y } ";

    /**
     * Call of another unreachable endpoint binding {@code ?z} for {@code ?y}.
     */
    private static final String second = "SERVICE SILENT <http://127.0.0.1:2/sparql> { ?y ex:q ?z } ";

    /**
     * The mapped item.
     */
    private static final Iri item = new Iri("http://example.org/item/1");

    /**
     * Label of the mapped item.
     */
    private static final TypedLiteral label = new TypedLiteral("one", xsdStringIri);

    /**
     * An IRI the context of a call binds.
     */
    private static final Iri a = new Iri("http://example.org/a");

    /**
     * Another IRI the context of a call binds.
     */
    private static final Iri b = new Iri("http://example.org/b");

    /**
     * The value the answering endpoints bind {@code ?y} to.
     */
    private static final TypedLiteral remote = new TypedLiteral("remote", xsdStringIri);

    /**
     * An unreachable endpoint.
     */
    private static final Iri dead = new Iri("http://127.0.0.1:1/sparql");

    /**
     * Requests the endpoint {@code flaky} has received.
     */
    private static final AtomicInteger flakyRequests = new AtomicInteger();

    /**
     * Engine over the configuration mapping the table.
     */
    private static Engine engine = null;

    /**
     * The server of the answering endpoints.
     */
    private static HttpServer server = null;

    /**
     * The endpoint answering every request.
     */
    private static Iri ok = null;

    /**
     * The endpoint answering its first request only.
     */
    private static Iri flaky = null;


    /**
     * Creates the {@code silent_service_test} schema with the table {@code item} of one labelled IRI and maps it, and
     * starts the answering endpoints on a free port of the loopback interface.
     *
     * @throws SQLException on database errors
     * @throws IOException if the endpoints cannot be started
     */
    @BeforeAll
    static void init() throws SQLException, IOException
    {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/ok", exchange -> answer(exchange, true));
        server.createContext("/flaky", exchange -> answer(exchange, flakyRequests.incrementAndGet() == 1));
        server.start();

        String base = "http://127.0.0.1:" + server.getAddress().getPort() + "/";

        ok = new Iri(base + "ok");
        flaky = new Iri(base + "flaky");

        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema silent_service_test");
            statement.execute("create table silent_service_test.item (iri varchar collate \"C\" primary key, "
                    + "label varchar collate \"C\" not null)");
            statement.execute("insert into silent_service_test.item values ('http://example.org/item/1', 'one')");
        }

        DatabaseSchema schema = new DatabaseSchema(connectionPool);

        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);
        config.addPrefix("ex", "http://example.org/");

        DatabaseTable table = new DatabaseTable("silent_service_test", "item");
        ConstantIriMapping graph = config.createIriMapping("<http://example.org/graph>");

        config.addQuadMapping(table, graph, config.createIriMapping(BuiltinClasses.iri, "iri"),
                config.createIriMapping("ex:label"), config.createLiteralMapping(xsdString, "label"));

        engine = new Engine(config);
    }


    /**
     * Stops the answering endpoints.
     */
    @AfterAll
    static void stop()
    {
        server.stop(0);
    }


    /**
     * Answers a request to an endpoint: the solution {@code ?y = "remote"} in the XML results format, or a failure.
     *
     * @param exchange the request
     * @param success whether the endpoint answers
     * @throws IOException on communication errors
     */
    private static void answer(HttpExchange exchange, boolean success) throws IOException
    {
        exchange.getRequestBody().readAllBytes();

        if(!success)
        {
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
            return;
        }

        byte[] data = """
                <?xml version="1.0"?>
                <sparql xmlns="http://www.w3.org/2005/sparql-results#">
                  <head><variable name="y"/></head>
                  <results><result><binding name="y"><literal>remote</literal></binding></result></results>
                </sparql>
                """.getBytes(UTF_8);

        exchange.getResponseHeaders().add("content-type", "application/sparql-results+xml");
        exchange.sendResponseHeaders(200, data.length);

        try(OutputStream out = exchange.getResponseBody())
        {
            out.write(data);
        }
    }


    /**
     * Rows of the query.
     *
     * @param query the query, without the prefix declaration
     * @return rows of the query
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws LimitExceedException if the generated SQL is too long
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static List<List<RdfTerm>> execute(String query)
            throws SQLException, TranslateExceptions, LimitExceedException, ServiceException
    {
        try(Request request = engine.getRequest(); Result result = request.execute(prefix + query))
        {
            List<List<RdfTerm>> rows = new ArrayList<>();

            while(result.next())
                rows.add(Arrays.asList(result.getRow()));

            return rows;
        }
    }


    /**
     * Row of the terms.
     *
     * @param terms the terms
     * @return row of the terms
     */
    private static List<RdfTerm> row(RdfTerm... terms)
    {
        return Arrays.asList(terms);
    }


    @Test
    @DisplayName("a failing SILENT call yields its context with the variables of the call unbound")
    void silentCallWithLocalPattern() throws Exception
    {
        assertThat(execute("SELECT ?x ?l ?y WHERE { " + first + "?x ex:label ?l }"), contains(row(item, label, null)));
    }


    @Test
    @DisplayName("a failing SILENT call whose context is an earlier failing SILENT call")
    void chainedSilentCalls() throws Exception
    {
        assertThat(execute("SELECT ?x ?y ?z WHERE { " + first + second + "}"), contains(row(null, null, null)));
    }


    @Test
    @DisplayName("two failing SILENT calls joined with a local pattern")
    void chainedSilentCallsWithLocalPattern() throws Exception
    {
        assertThat(execute("SELECT ?x ?l ?y ?z WHERE { " + first + second + "?x ex:label ?l }"),
                contains(row(item, label, null, null)));
    }


    @Test
    @DisplayName("a failing call without SILENT is an error")
    void callWithoutSilentIsAnError()
    {
        assertThrows(ServiceException.class,
                () -> execute("SELECT ?x ?y ?z WHERE { " + first + second.replace("SERVICE SILENT", "SERVICE") + "}"));
    }


    @Test
    @DisplayName("a failing endpoint named by a variable of a SILENT call leaves the other endpoints their solutions")
    void silentCallOfSeveralEndpoints() throws Exception
    {
        assertThat(
                execute("SELECT ?ep ?x ?y WHERE { VALUES (?ep ?x) { (<" + ok.getValue() + "> ex:a) (<" + dead.getValue()
                        + "> ex:a) (<" + ok.getValue() + "> ex:b) } SERVICE SILENT ?ep { ?x ex:p ?y } }"),
                containsInAnyOrder(row(ok, a, remote), row(dead, a, null), row(ok, b, remote)));
    }


    @Test
    @DisplayName("an endpoint of a SILENT call failing after it has answered leaves all solutions naming it as they are")
    void silentCallOfEndpointFailingLater() throws Exception
    {
        flakyRequests.set(0);

        assertThat(
                execute("SELECT ?ep ?x ?y WHERE { VALUES (?ep ?x) { (<" + flaky.getValue() + "> ex:a) (<"
                        + ok.getValue() + "> ex:a) (<" + flaky.getValue() + "> ex:b) } "
                        + "SERVICE SILENT ?ep { ?x ex:p ?y } }"),
                containsInAnyOrder(row(flaky, a, null), row(ok, a, remote), row(flaky, b, null)));

        // the endpoint is not asked again once it has failed
        assertThat(flakyRequests.get(), is(2));
    }


    @Test
    @DisplayName("a failing endpoint named by a variable of a call without SILENT is an error")
    void callOfSeveralEndpointsWithoutSilentIsAnError()
    {
        assertThrows(ServiceException.class, () -> execute("SELECT ?ep ?x ?y WHERE { VALUES (?ep ?x) { (<"
                + ok.getValue() + "> ex:a) (<" + dead.getValue() + "> ex:a) } SERVICE ?ep { ?x ex:p ?y } }"));
    }
}
