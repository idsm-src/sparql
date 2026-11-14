package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerIri;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import cz.iocb.sparql.engine.mapping.TermMapping;
import cz.iocb.sparql.engine.mapping.classes.BuiltinClasses;
import cz.iocb.sparql.engine.mapping.classes.IntegerUserIriClass;
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
 * SERVICE calls whose endpoint is named by a variable, and calls evaluated for the solutions of their siblings. The
 * value of the variable comes from the other members the call is joined with, wherever they stand: before the call in
 * its group (they are its context then), after it, or around the group of the call; and so it does inside an EXISTS
 * pattern and with the reordering of calls, where a call has no context of its own and the join evaluates it for the
 * solutions of its siblings. The siblings that refer to the current solution of an EXISTS pattern have a meaning only
 * at their place, so the call is not evaluated for them. A call that no member of its join names the endpoint of asks
 * no endpoint.
 *
 * The table {@code item} holds the items 1 to 6 with the values 1 to 6, the notes {@code x} of the item 1 and {@code y}
 * of the item 3, and the endpoints of the items: the first one for the odd items, the second one for the even ones. The
 * endpoints are servers on the loopback interface answering the pattern of a subject and an object: the first one from
 * the remote triples of the items 1, 3 and 5 with the objects 10, 30 and 50, the second one from those of the items 2
 * and 4 with the objects 200 and 400, both honouring the subject of the VALUES row of a request.
 */
public class ServiceEndpointVariableTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX : <http://example.org/> PREFIX item: <http://example.org/item/> ";

    /**
     * The item of the VALUES row of a query.
     */
    private static final Pattern itemPattern = Pattern
            .compile("values \\(.*\\) \\{\\(.*<http://example.org/item/(\\d+)>");

    /**
     * Requests received by the endpoints, each as the path of its endpoint followed by the item of its VALUES row, or
     * by an asterisk when it has none.
     */
    private static final List<String> requests = Collections.synchronizedList(new ArrayList<>());

    /**
     * The endpoint server.
     */
    private static HttpServer server = null;

    /**
     * URL of the first endpoint.
     */
    private static String first = null;

    /**
     * Engine over the local configuration.
     */
    private static Engine engine = null;


    /**
     * Starts the endpoints on a free port of the loopback interface, creates the {@code service_endpoint_variable_test}
     * schema with the table {@code item} and maps it as {@code ?s :v ?value}, {@code ?s :n ?note} and
     * {@code ?s :e ?endpoint}.
     *
     * @throws SQLException on database errors
     * @throws IOException if the endpoints cannot be started
     */
    @BeforeAll
    static void init() throws SQLException, IOException
    {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/first", e -> answer(e, new int[] { 1, 3, 5 }, 10));
        server.createContext("/second", e -> answer(e, new int[] { 2, 4 }, 100));
        server.start();

        String base = "http://127.0.0.1:" + server.getAddress().getPort();
        first = base + "/first";

        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema service_endpoint_variable_test");
            statement.execute("create table service_endpoint_variable_test.item (id int primary key, "
                    + "value numeric not null, note varchar, endpoint varchar collate \"C\" not null)");

            for(int id = 1; id <= 6; id++)
                statement.execute("insert into service_endpoint_variable_test.item values (" + id + ", " + id + ", "
                        + (id == 1 ? "'x'" : id == 3 ? "'y'" : "null") + ", '" + base
                        + (id % 2 == 1 ? "/first" : "/second") + "')");
        }

        DatabaseSchema schema = new DatabaseSchema(connectionPool);

        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration("http://example.org/sparql",
                connectionPool, schema, false);
        config.addIriClass(new IntegerUserIriClass("item", INT4, "http://example.org/item/"));

        DatabaseTable table = new DatabaseTable("service_endpoint_variable_test", "item");
        TermMapping subject = config.createIriMapping("item", "id");

        config.addQuadMapping(table, null, subject, config.createIriMapping(new Iri("http://example.org/v")),
                config.createLiteralMapping(xsdInteger, "value"));
        config.addQuadMapping(table, null, subject, config.createIriMapping(new Iri("http://example.org/n")),
                config.createLiteralMapping(xsdString, "note"));
        config.addQuadMapping(table, null, subject, config.createIriMapping(new Iri("http://example.org/e")),
                config.createIriMapping(BuiltinClasses.iri, "endpoint"));

        engine = new Engine(config);
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
     * Answers a query sent over the SPARQL protocol: the request is recorded, and the remote triples of the endpoint,
     * that of the item of the VALUES row only when the row has one, are returned as bindings of the variables {@code s}
     * and {@code o} in the XML results format.
     *
     * @param exchange the request
     * @param items the items the endpoint has triples of
     * @param factor the object of a triple divided by its item
     * @throws IOException on communication errors
     */
    private static void answer(HttpExchange exchange, int[] items, int factor) throws IOException
    {
        String body = new String(exchange.getRequestBody().readAllBytes(), UTF_8);
        String query = URLDecoder.decode(body.replaceFirst("^query=", ""), UTF_8);

        Matcher matcher = itemPattern.matcher(query);
        String asked = matcher.find() ? matcher.group(1) : null;

        requests.add(exchange.getRequestURI().getPath() + " " + (asked != null ? asked : "*"));

        StringBuilder builder = new StringBuilder();
        builder.append("<?xml version=\"1.0\"?><sparql xmlns=\"http://www.w3.org/2005/sparql-results#\"><head>");
        builder.append("<variable name=\"s\"/><variable name=\"o\"/></head><results>");

        for(int id : items)
        {
            if(asked != null && !asked.equals(Integer.toString(id)))
                continue;

            builder.append("<result><binding name=\"s\"><uri>").append(item(id).getValue());
            builder.append("</uri></binding><binding name=\"o\"><literal datatype=\"");
            builder.append(xsdIntegerIri.getValue()).append("\">").append(factor * id);
            builder.append("</literal></binding></result>");
        }

        builder.append("</results></sparql>");

        byte[] data = builder.toString().getBytes(UTF_8);

        exchange.getResponseHeaders().add("content-type", "application/sparql-results+xml");
        exchange.sendResponseHeaders(200, data.length);

        try(OutputStream out = exchange.getResponseBody())
        {
            out.write(data);
        }
    }


    /**
     * Rows of the query; the requests the endpoints received before are forgotten.
     *
     * @param reorder whether the SERVICE calls may be evaluated independently of their context
     * @param query the query, without the prefix declarations; {@code <first>} names the first endpoint
     * @return rows of the query
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws LimitExceedException if the generated SQL is too long
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static List<List<RdfTerm>> execute(boolean reorder, String query)
            throws SQLException, TranslateExceptions, LimitExceedException, ServiceException
    {
        requests.clear();

        String text = prefix + query.replace("<first>", "<" + first + ">");

        try(Request request = engine.getRequest(reorder); Result result = request.execute(text))
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


    /**
     * IRI of the item.
     *
     * @param id identifier of the item
     * @return IRI of the item
     */
    private static Iri item(int id)
    {
        return new Iri("http://example.org/item/" + id);
    }


    /**
     * The xsd:integer literal of the value.
     *
     * @param value the value
     * @return the xsd:integer literal of the value
     */
    private static TypedLiteral integer(int value)
    {
        return new TypedLiteral(Integer.toString(value), xsdIntegerIri);
    }


    /**
     * The remote triples of the items, each item asking the endpoint the table names for it.
     *
     * @return the items with their remote objects
     */
    private static List<List<RdfTerm>> remote()
    {
        return List.of(row(item(1), integer(10)), row(item(3), integer(30)), row(item(5), integer(50)),
                row(item(2), integer(200)), row(item(4), integer(400)));
    }


    @Test
    @DisplayName("the endpoint named by a member before the call, with and without the reordering of calls")
    void memberBeforeCall() throws Exception
    {
        for(boolean reorder : new boolean[] { false, true })
        {
            assertThat(execute(reorder, "SELECT ?s ?o WHERE { ?s :e ?e SERVICE ?e { ?s :p ?o } }"),
                    containsInAnyOrder(remote().toArray()));

            // every item asks the endpoint the table names for it
            assertThat(requests,
                    containsInAnyOrder("/first 1", "/second 2", "/first 3", "/second 4", "/first 5", "/second 6"));
        }
    }


    @Test
    @DisplayName("the endpoint named by a member after the call or around its group")
    void memberElsewhere() throws Exception
    {
        for(boolean reorder : new boolean[] { false, true })
        {
            assertThat(execute(reorder, "SELECT ?s ?o WHERE { SERVICE ?e { ?s :p ?o } ?s :e ?e }"),
                    containsInAnyOrder(remote().toArray()));

            assertThat(execute(reorder, "SELECT ?s ?o WHERE { ?s :e ?e { SERVICE ?e { ?s :p ?o } } }"),
                    containsInAnyOrder(remote().toArray()));

            String pattern = "?s :v ?v SERVICE ?e { ?s :p ?o } ?s :e ?e FILTER(?v > 3)";

            assertThat(execute(reorder, "SELECT ?s ?o WHERE { " + pattern + " }"),
                    containsInAnyOrder(row(item(5), integer(50)), row(item(4), integer(400))));
        }
    }


    @Test
    @DisplayName("the endpoint named inside an EXISTS pattern")
    void insideExists() throws Exception
    {
        for(boolean reorder : new boolean[] { false, true })
        {
            String call = "?s :e ?e SERVICE ?e { ?s :p ?o }";

            assertThat(execute(reorder, "SELECT ?s WHERE { ?s :v ?v FILTER EXISTS { " + call + " } }"),
                    containsInAnyOrder(row(item(1)), row(item(2)), row(item(3)), row(item(4)), row(item(5))));

            assertThat(execute(reorder, "SELECT ?s WHERE { ?s :v ?v FILTER NOT EXISTS { " + call + " } }"),
                    containsInAnyOrder(row(item(6))));

            // the bound value is that of the current solution, so the call is not evaluated for it, and the filter
            // it decides is not evaluated on the solutions the call is evaluated for
            String pattern = "BIND(?v AS ?w) ?s :e ?e SERVICE ?e { ?s :p ?o } FILTER(?w < 4)";

            assertThat(execute(reorder, "SELECT ?s WHERE { ?s :v ?v FILTER EXISTS { " + pattern + " } }"),
                    containsInAnyOrder(row(item(1)), row(item(2)), row(item(3))));
        }
    }


    @Test
    @DisplayName("a reordered call beside members of an EXISTS pattern that refer to the current solution")
    void besideCurrentSolution() throws Exception
    {
        for(boolean reorder : new boolean[] { true, false })
        {
            // the right side of the OPTIONAL takes the item of the current solution
            String optional = "?z :v ?w OPTIONAL { ?s :n ?n } SERVICE <first> { ?z :p ?o } FILTER(BOUND(?n))";

            assertThat(execute(reorder, "SELECT ?s WHERE { ?s :v ?v FILTER EXISTS { " + optional + " } }"),
                    containsInAnyOrder(row(item(1)), row(item(3))));

            // the sub-select takes the item of the current solution
            String select = "{ SELECT ?w WHERE { ?s :v ?w } } SERVICE <first> { ?s :p ?o } FILTER(?o > ?w * 10 - 1)";

            assertThat(execute(reorder, "SELECT ?s WHERE { ?s :v ?v FILTER EXISTS { " + select + " } }"),
                    containsInAnyOrder(row(item(1)), row(item(3)), row(item(5))));
        }
    }


    @Test
    @DisplayName("a call that no member of its join names the endpoint of asks no endpoint")
    void noEndpoint() throws Exception
    {
        for(boolean reorder : new boolean[] { false, true })
        {
            assertThat(execute(reorder, "SELECT ?s ?o WHERE { SERVICE ?e { ?s :p ?o } }"), is(empty()));
            assertThat(requests, is(empty()));

            assertThat(execute(reorder, "SELECT ?s ?o WHERE { ?s :v ?v SERVICE ?e { ?s :p ?o } }"), is(empty()));
            assertThat(requests, is(empty()));
        }
    }
}
