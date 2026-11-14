package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import cz.iocb.sparql.engine.config.ServiceSettings;
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
 * Filters restricting the solutions a remote SERVICE call is evaluated for. The endpoint is asked about every solution
 * of the context of a call (the solutions before the call in its group), so a condition that the context solutions
 * decide by themselves is evaluated on the context, wherever the filter stands: in the group of the call, behind the
 * members that follow the call, in an enclosing group, or as the filter of the OPTIONAL group of the call. A condition
 * that a variable of the call may decide, a nondeterministic one, and one whose EXISTS pattern reads the whole solution
 * wait for the received solutions.
 *
 * The table {@code item} holds the items 1 to 5 with the values 1 to 5, the notes {@code x} of the item 1 and {@code y}
 * of the item 3, the guess 30 of the item 1 and the URL of the endpoint. The endpoints are servers on the loopback
 * interface answering the pattern of a subject and an object from the remote triples of the items 1 to 5 and 9 with the
 * objects 10 to 50 and 90, honouring the subject of the VALUES row of a request.
 */
public class ServiceFilterContextTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX : <http://example.org/> PREFIX item: <http://example.org/item/> ";

    /**
     * IRI of the service of the local configuration.
     */
    private static final String local = "http://example.org/sparql";

    /**
     * Variables of a query, in the order of their first occurrence.
     */
    private static final Pattern variablePattern = Pattern.compile("\\?([A-Za-z0-9_]+)");

    /**
     * The item of the VALUES row of a query.
     */
    private static final Pattern itemPattern = Pattern
            .compile("values \\(.*\\) \\{\\(.*<http://example.org/item/(\\d+)>");

    /**
     * Queries received by the endpoints, in the order of their arrival.
     */
    private static final List<String> queries = Collections.synchronizedList(new ArrayList<>());

    /**
     * The endpoint server.
     */
    private static HttpServer server = null;

    /**
     * URL of the main endpoint.
     */
    private static String endpoint = null;

    /**
     * URL of the other endpoint, answering the same.
     */
    private static String other = null;

    /**
     * Connection pool of the database.
     */
    private static DataSource connectionPool = null;

    /**
     * Schema of the database.
     */
    private static DatabaseSchema schema = null;


    /**
     * Starts the endpoints on a free port of the loopback interface and creates the {@code service_filter_context_test}
     * schema with the table {@code item}.
     *
     * @throws SQLException on database errors
     * @throws IOException if the endpoints cannot be started
     */
    @BeforeAll
    static void init() throws SQLException, IOException
    {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/sparql", ServiceFilterContextTest::answer);
        server.createContext("/other", ServiceFilterContextTest::answer);
        server.start();

        String base = "http://127.0.0.1:" + server.getAddress().getPort();
        endpoint = base + "/sparql";
        other = base + "/other";

        connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema service_filter_context_test");
            statement.execute("create table service_filter_context_test.item (id int primary key, "
                    + "value numeric not null, note varchar, guess numeric, endpoint varchar collate \"C\" not null)");
            statement.execute("insert into service_filter_context_test.item values (1, 1, 'x', 30, '" + endpoint
                    + "'), (2, 2, null, null, '" + endpoint + "'), (3, 3, 'y', null, '" + endpoint
                    + "'), (4, 4, null, null, '" + endpoint + "'), (5, 5, null, null, '" + endpoint + "')");
        }

        schema = new DatabaseSchema(connectionPool);
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
     * Forgets the queries received so far.
     */
    @BeforeEach
    void reset()
    {
        queries.clear();
    }


    /**
     * Answers a query sent over the SPARQL protocol: the query is recorded, and the remote triples, that of the item of
     * the VALUES row only when the row has one, are returned as bindings of the first two variables of the query (the
     * subject and the object) in the XML results format.
     *
     * @param exchange the request
     * @throws IOException on communication errors
     */
    private static void answer(HttpExchange exchange) throws IOException
    {
        String body = new String(exchange.getRequestBody().readAllBytes(), UTF_8);
        String query = URLDecoder.decode(body.replaceFirst("^query=", ""), UTF_8);

        queries.add(query);

        List<String> variables = new ArrayList<>();
        Matcher matcher = variablePattern.matcher(query);

        while(matcher.find())
            if(!variables.contains(matcher.group(1)))
                variables.add(matcher.group(1));

        Matcher itemMatcher = itemPattern.matcher(query);
        String asked = itemMatcher.find() ? itemMatcher.group(1) : null;

        StringBuilder builder = new StringBuilder();
        builder.append("<?xml version=\"1.0\"?><sparql xmlns=\"http://www.w3.org/2005/sparql-results#\"><head>");
        builder.append("<variable name=\"").append(variables.get(0)).append("\"/>");
        builder.append("<variable name=\"").append(variables.get(1)).append("\"/>");
        builder.append("</head><results>");

        for(int id : new int[] { 1, 2, 3, 4, 5, 9 })
        {
            if(asked != null && !asked.equals(Integer.toString(id)))
                continue;

            builder.append("<result><binding name=\"").append(variables.get(0)).append("\"><uri>");
            builder.append(item(id).getValue()).append("</uri></binding>");
            builder.append("<binding name=\"").append(variables.get(1)).append("\">");
            builder.append("<literal datatype=\"").append(xsdIntegerIri.getValue()).append("\">");
            builder.append(10 * id).append("</literal></binding></result>");
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
     * Engine over the local configuration with the given settings of remote endpoints: the table {@code item} mapped as
     * {@code ?s :v ?value}, {@code ?s :n ?note}, {@code ?s :g ?guess} and {@code ?s :e ?endpoint}.
     *
     * @param settings default settings of the remote endpoints
     * @return engine over the local configuration
     * @throws SQLException on database errors
     */
    private static Engine engine(ServiceSettings settings) throws SQLException
    {
        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(local, connectionPool, schema, false);
        config.addIriClass(new IntegerUserIriClass("item", INT4, "http://example.org/item/"));

        DatabaseTable table = new DatabaseTable("service_filter_context_test", "item");
        TermMapping subject = config.createIriMapping("item", "id");

        config.addQuadMapping(table, null, subject, config.createIriMapping(iri("v")),
                config.createLiteralMapping(xsdInteger, "value"));
        config.addQuadMapping(table, null, subject, config.createIriMapping(iri("n")),
                config.createLiteralMapping(xsdString, "note"));
        config.addQuadMapping(table, null, subject, config.createIriMapping(iri("g")),
                config.createLiteralMapping(xsdInteger, "guess"));
        config.addQuadMapping(table, null, subject, config.createIriMapping(iri("e")),
                config.createIriMapping(BuiltinClasses.iri, "endpoint"));

        config.setServiceSettings(settings);

        return new Engine(config);
    }


    /**
     * Rows of the query run under the default settings.
     *
     * @param query the query, without the prefix declarations; {@code <ep>} and {@code <other>} name the endpoints
     * @return rows of the query
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws LimitExceedException if the generated SQL is too long
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static List<List<RdfTerm>> execute(String query)
            throws SQLException, TranslateExceptions, LimitExceedException, ServiceException
    {
        return execute(ServiceSettings.DEFAULT, false, query);
    }


    /**
     * Rows of the query; the queries the endpoints received before are forgotten.
     *
     * @param settings default settings of the remote endpoints
     * @param reorder whether the SERVICE calls may be evaluated independently of their context
     * @param query the query, without the prefix declarations; {@code <ep>} and {@code <other>} name the endpoints
     * @return rows of the query
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws LimitExceedException if the generated SQL is too long
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static List<List<RdfTerm>> execute(ServiceSettings settings, boolean reorder, String query)
            throws SQLException, TranslateExceptions, LimitExceedException, ServiceException
    {
        queries.clear();

        String text = prefix + query.replace("<ep>", "<" + endpoint + ">").replace("<other>", "<" + other + ">");

        try(Request request = engine(settings).getRequest(reorder); Result result = request.execute(text))
        {
            List<List<RdfTerm>> rows = new ArrayList<>();

            while(result.next())
                rows.add(Arrays.asList(result.getRow()));

            return rows;
        }
    }


    /**
     * The items the endpoints were asked about by the last query, in ascending order: the items of the VALUES rows of
     * the received queries, and zero for a query without one.
     *
     * @return the items the endpoints were asked about by the last query
     */
    private static List<Integer> asked()
    {
        List<Integer> items = new ArrayList<>();

        synchronized(queries)
        {
            for(String query : queries)
            {
                Matcher matcher = itemPattern.matcher(query);

                items.add(matcher.find() ? Integer.valueOf(matcher.group(1)) : 0);
            }
        }

        Collections.sort(items);

        return items;
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
     * IRI of the example namespace.
     *
     * @param name the local name
     * @return IRI of the example namespace
     */
    private static Iri iri(String name)
    {
        return new Iri("http://example.org/" + name);
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
     * The xsd:string literal of the value.
     *
     * @param value the value
     * @return the xsd:string literal of the value
     */
    private static TypedLiteral string(String value)
    {
        return new TypedLiteral(value, xsdStringIri);
    }


    @Test
    @DisplayName("a filter of a variable the context always binds restricts the context, wherever it stands")
    void contextVariable() throws Exception
    {
        assertThat(execute("SELECT ?s ?o WHERE { ?s :v ?v FILTER(?v = 3) SERVICE <ep> { ?s :p ?o } }"),
                contains(row(item(3), integer(30))));
        assertThat(asked(), contains(3));

        assertThat(execute("SELECT ?s ?o WHERE { ?s :v ?v SERVICE <ep> { ?s :p ?o } FILTER(?v = 3) }"),
                contains(row(item(3), integer(30))));
        assertThat(asked(), contains(3));

        // the limit applies to the context solutions the endpoint is asked about
        ServiceSettings settings = ServiceSettings.DEFAULT.withContextLimit(2);

        assertThat(execute(settings, false, "SELECT ?s ?o WHERE { ?s :v ?v SERVICE <ep> { ?s :p ?o } FILTER(?v > 3) }"),
                containsInAnyOrder(row(item(4), integer(40)), row(item(5), integer(50))));
        assertThat(asked(), contains(4, 5));
    }


    @Test
    @DisplayName("a filter of a variable only the call binds waits for the received solutions")
    void serviceVariable() throws Exception
    {
        String pattern = "?s :v ?v SERVICE <ep> { ?s :p ?o }";

        assertThat(execute("SELECT ?s WHERE { " + pattern + " FILTER(BOUND(?o)) }"),
                containsInAnyOrder(row(item(1)), row(item(2)), row(item(3)), row(item(4)), row(item(5))));
        assertThat(asked(), contains(1, 2, 3, 4, 5));

        assertThat(execute("SELECT ?s WHERE { " + pattern + " FILTER(!BOUND(?o)) }"), is(empty()));
        assertThat(asked(), contains(1, 2, 3, 4, 5));

        assertThat(execute("SELECT ?s WHERE { " + pattern + " FILTER(?o = 30) }"), contains(row(item(3))));
        assertThat(asked(), contains(1, 2, 3, 4, 5));
    }


    @Test
    @DisplayName("the conjuncts of a filter that the context decides restrict it by themselves")
    void conjunction() throws Exception
    {
        assertThat(execute("SELECT ?s ?o WHERE { ?s :v ?v SERVICE <ep> { ?s :p ?o } FILTER(?v > 2 && ?o < 50) }"),
                containsInAnyOrder(row(item(3), integer(30)), row(item(4), integer(40))));
        assertThat(asked(), contains(3, 4, 5));

        assertThat(execute("SELECT ?s WHERE { ?s :v ?v SERVICE <ep> { ?s :p ?o } FILTER(?v > 2 || ?o < 20) }"),
                containsInAnyOrder(row(item(1)), row(item(3)), row(item(4)), row(item(5))));
        assertThat(asked(), contains(1, 2, 3, 4, 5));
    }


    @Test
    @DisplayName("a nondeterministic condition is evaluated once, for the solutions of the call")
    void nondeterministicCondition() throws Exception
    {
        assertThat(execute("SELECT ?s WHERE { ?s :v ?v SERVICE <ep> { ?s :p ?o } FILTER(?v = 3 || RAND() < 0) }"),
                contains(row(item(3))));
        assertThat(asked(), contains(1, 2, 3, 4, 5));

        assertThat(execute("SELECT ?s WHERE { ?s :v ?v SERVICE <ep> { ?s :p ?o } FILTER(?v = 3 && RAND() < 2) }"),
                contains(row(item(3))));
        assertThat(asked(), contains(3));
    }


    @Test
    @DisplayName("an EXISTS filter restricts the context unless its pattern reads a variable of the call")
    void existsCondition() throws Exception
    {
        String pattern = "?s :v ?v SERVICE <ep> { ?s :p ?o }";

        assertThat(execute("SELECT ?s WHERE { " + pattern + " FILTER EXISTS { ?s :n ?n } }"),
                containsInAnyOrder(row(item(1)), row(item(3))));
        assertThat(asked(), contains(1, 3));

        assertThat(execute("SELECT ?s WHERE { " + pattern + " FILTER NOT EXISTS { ?s :n ?n } }"),
                containsInAnyOrder(row(item(2)), row(item(4)), row(item(5))));
        assertThat(asked(), contains(2, 4, 5));

        assertThat(execute("SELECT ?s WHERE { " + pattern + " FILTER EXISTS { ?s :g ?o } }"), is(empty()));
        assertThat(asked(), contains(1, 2, 3, 4, 5));

        // the sides of a MINUS share every variable of the current solution, among them that of the call
        assertThat(execute("SELECT ?s WHERE { " + pattern + " FILTER EXISTS { ?s :v ?w MINUS { ?s :n ?n } } }"),
                containsInAnyOrder(row(item(2)), row(item(4)), row(item(5))));
        assertThat(asked(), contains(1, 2, 3, 4, 5));
    }


    @Test
    @DisplayName("a variable the context may leave unbound decides in the context only when the call cannot bind it")
    void unboundContextVariable() throws Exception
    {
        assertThat(
                execute("SELECT ?s ?o WHERE { ?s :v ?v OPTIONAL { ?s :n ?n } SERVICE <ep> { ?s :p ?o } "
                        + "FILTER(!BOUND(?n)) }"),
                containsInAnyOrder(row(item(2), integer(20)), row(item(4), integer(40)), row(item(5), integer(50))));
        assertThat(asked(), contains(2, 4, 5));

        // the guess of the item 1 differs from its remote object, and the remote object of the item 3 is 30
        assertThat(execute(
                "SELECT ?s ?o WHERE { ?s :v ?v OPTIONAL { ?s :g ?o } SERVICE <ep> { ?s :p ?o } FILTER(?o = 30) }"),
                contains(row(item(3), integer(30))));
        assertThat(asked(), contains(1, 2, 3, 4, 5));
    }


    @Test
    @DisplayName("a filter gets to the context through the members that follow the call")
    void laterMembers() throws Exception
    {
        String pattern = "?s :v ?v SERVICE <ep> { ?s :p ?o }";

        assertThat(execute("SELECT ?s WHERE { " + pattern + " ?s :n ?n FILTER(?v > 2) }"), contains(row(item(3))));
        assertThat(asked(), contains(3, 4, 5));

        assertThat(execute("SELECT ?s WHERE { " + pattern + " ?s :n ?n FILTER(?n = 'y') }"), contains(row(item(3))));
        assertThat(asked(), contains(1, 2, 3, 4, 5));

        assertThat(execute("SELECT ?s ?n WHERE { " + pattern + " OPTIONAL { ?s :n ?n } FILTER(?v > 2) }"),
                containsInAnyOrder(row(item(3), string("y")), row(item(4), null), row(item(5), null)));
        assertThat(asked(), contains(3, 4, 5));

        assertThat(execute("SELECT ?s WHERE { " + pattern + " OPTIONAL { ?s :n ?n } FILTER(!BOUND(?n)) }"),
                containsInAnyOrder(row(item(2)), row(item(4)), row(item(5))));
        assertThat(asked(), contains(1, 2, 3, 4, 5));

        assertThat(execute("SELECT ?s WHERE { " + pattern + " MINUS { ?s :n ?n } FILTER(?v > 2) }"),
                containsInAnyOrder(row(item(4)), row(item(5))));
        assertThat(asked(), contains(3, 4, 5));

        assertThat(execute("SELECT ?s ?w WHERE { " + pattern + " BIND(?v + 1 AS ?w) FILTER(?v = 3) }"),
                contains(row(item(3), integer(4))));
        assertThat(asked(), contains(3));

        assertThat(execute("SELECT ?s ?w WHERE { " + pattern + " BIND(?v + 1 AS ?w) FILTER(?w = 4) }"),
                contains(row(item(3), integer(4))));
        assertThat(asked(), contains(1, 2, 3, 4, 5));
    }


    @Test
    @DisplayName("a filter of an enclosing group gets to the context of a call of a nested group")
    void enclosingGroups() throws Exception
    {
        String pattern = "?s :v ?v SERVICE <ep> { ?s :p ?o }";

        assertThat(execute("SELECT ?s ?o WHERE { { " + pattern + " } FILTER(?v = 3) }"),
                contains(row(item(3), integer(30))));
        assertThat(asked(), contains(3));

        assertThat(execute("SELECT ?s ?o WHERE { { " + pattern + " } UNION { ?s :v ?v . ?s :g ?o } FILTER(?v = 1) }"),
                containsInAnyOrder(row(item(1), integer(10)), row(item(1), integer(30))));
        assertThat(asked(), contains(1));

        assertThat(
                execute("SELECT ?s ?o WHERE { { SELECT DISTINCT ?s ?v ?o WHERE { " + pattern + " } } FILTER(?v = 3) }"),
                contains(row(item(3), integer(30))));
        assertThat(asked(), contains(3));

        // the solutions of a group agree on the variables they are grouped by, but not on an aggregate
        assertThat(execute("SELECT ?v ?c WHERE { { SELECT ?v (COUNT(?o) AS ?c) WHERE { " + pattern + " } GROUP BY ?v } "
                + "FILTER(?v = 3) }"), contains(row(integer(3), integer(1))));
        assertThat(asked(), contains(3));

        assertThat(execute("SELECT ?v WHERE { { SELECT ?v (COUNT(?o) AS ?c) WHERE { " + pattern + " } GROUP BY ?v } "
                + "FILTER(?c = 1 && ?v > 3) }"), containsInAnyOrder(row(integer(4)), row(integer(5))));
        assertThat(asked(), contains(4, 5));

        assertThat(execute("SELECT ?c WHERE { { SELECT (COUNT(?o) AS ?c) WHERE { " + pattern + " } } FILTER(?c > 0) }"),
                contains(row(integer(5))));
        assertThat(asked(), contains(1, 2, 3, 4, 5));

        // a slice takes its solutions before the filter is applied
        assertThat(execute("SELECT ?s ?o WHERE { { SELECT ?s ?v ?o WHERE { ?s :v ?v SERVICE <ep> { ?s :p ?o } } "
                + "ORDER BY ?v LIMIT 2 } FILTER(?v = 3) }"), is(empty()));
        assertThat(asked(), contains(1, 2, 3, 4, 5));
    }


    @Test
    @DisplayName("the filter of an OPTIONAL or a MINUS group restricts the context of a call of the group")
    void optionalAndMinusGroups() throws Exception
    {
        assertThat(
                execute("SELECT ?t ?s ?o WHERE { ?t :n ?n OPTIONAL { ?s :v ?v SERVICE <ep> { ?s :p ?o } "
                        + "FILTER(?v = 3) } }"),
                containsInAnyOrder(row(item(1), item(3), integer(30)), row(item(3), item(3), integer(30))));
        assertThat(asked(), contains(3));

        // the conjunct comparing the sides stays a condition of the join
        assertThat(
                execute("SELECT ?t ?o WHERE { ?t :n ?n . ?t :v ?w OPTIONAL { ?s :v ?v SERVICE <ep> { ?s :p ?o } "
                        + "FILTER(?v = ?w && ?v > 2) } }"),
                containsInAnyOrder(row(item(1), null), row(item(3), integer(30))));
        assertThat(asked(), contains(3, 4, 5));

        assertThat(execute("SELECT ?s WHERE { ?s :v ?v MINUS { ?s :v ?w SERVICE <ep> { ?s :p ?o } FILTER(?w > 2) } }"),
                containsInAnyOrder(row(item(1)), row(item(2))));
        assertThat(asked(), contains(3, 4, 5));
    }


    @Test
    @DisplayName("a filter restricts the contexts of all the calls it gets to")
    void severalCalls() throws Exception
    {
        assertThat(execute("SELECT ?s ?o ?u WHERE { ?s :v ?v SERVICE <ep> { ?s :p ?o } SERVICE <other> { ?s :p ?u } "
                + "FILTER(?v = 3) }"), contains(row(item(3), integer(30), integer(30))));
        assertThat(asked(), contains(3, 3));

        assertThat(execute("SELECT ?s ?o WHERE { ?s :v ?v . ?s :e ?e SERVICE ?e { ?s :p ?o } FILTER(?v = 3) }"),
                contains(row(item(3), integer(30))));
        assertThat(asked(), contains(3));
    }


    @Test
    @DisplayName("a reordered call is evaluated for the filtered solutions of its siblings")
    void reorderedCall() throws Exception
    {
        ServiceSettings settings = ServiceSettings.DEFAULT;

        assertThat(execute(settings, true, "SELECT ?s ?o WHERE { ?s :v ?v SERVICE <ep> { ?s :p ?o } FILTER(?v = 3) }"),
                contains(row(item(3), integer(30))));
        assertThat(asked(), contains(3));

        // the members that follow the call belong to its siblings too
        assertThat(
                execute(settings, true,
                        "SELECT ?s ?o WHERE { ?s :v ?v SERVICE <ep> { ?s :p ?o } ?s :n ?n FILTER(?n = 'y') }"),
                contains(row(item(3), integer(30))));
        assertThat(asked(), contains(3));

        assertThat(execute(settings, true, "SELECT ?s WHERE { ?s :v ?v SERVICE <ep> { ?s :p ?o } FILTER(?o = 30) }"),
                contains(row(item(3))));
        assertThat(asked(), contains(1, 2, 3, 4, 5));
    }


    @Test
    @DisplayName("a call of the service of the configuration is translated in place and filtered like its group")
    void localService() throws Exception
    {
        assertThat(execute("SELECT ?s ?n WHERE { ?s :v ?v SERVICE <" + local + "> { ?s :n ?n } FILTER(?v = 3) }"),
                contains(row(item(3), string("y"))));
        assertThat(asked(), is(empty()));
    }
}
