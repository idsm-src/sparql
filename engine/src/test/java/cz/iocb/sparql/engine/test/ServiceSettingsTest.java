package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDecimal;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInteger;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDecimalIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerIri;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import cz.iocb.sparql.engine.error.MessageType;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.mapping.TermMapping;
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
 * Settings of the calls of remote SERVICE endpoints ({@link ServiceSettings}): by default only the IRI values of the
 * shared variables are sent in the VALUES clause of a call and a literal value constrains the received solutions
 * locally, the received literals are brought to their canonical forms, and the context solutions asking the same query
 * of the same endpoint share one request; the settings can send the literals too, keep the received lexical forms, and
 * limit the context solutions, the results and the followed redirects, for all endpoints or for one. The endpoint is a
 * server on the loopback interface answering the pattern {@code ?s :p ?o} from a few fixed remote triples, some of them
 * with non-canonical literals, matching a VALUES clause by the lexical forms, as a store keeping the literals as given
 * does. The local data is one table (so that the context of a call is one table access, not a union evaluated per
 * member) and one constant quad.
 */
public class ServiceSettingsTest
{
    /**
     * A remote triple with a literal object.
     *
     * @param subject local name of the subject
     * @param value lexical form of the object
     * @param type datatype IRI of the object
     */
    private static record RemoteTriple(String subject, String value, Iri type)
    {
    }


    /**
     * Prefix declaration prepended to every query.
     */
    private static final String prefix = "PREFIX : <http://example.org/> ";

    /**
     * Triples of the remote endpoint, the objects of {@code :a}, {@code :c} and {@code :d} in non-canonical forms.
     */
    private static final List<RemoteTriple> remote = List.of(new RemoteTriple("a", "01", xsdIntegerIri),
            new RemoteTriple("b", "2", xsdIntegerIri), new RemoteTriple("c", "+3", xsdIntegerIri),
            new RemoteTriple("d", "1.50", xsdDecimalIri));

    /**
     * Variables of a query, in the order of their first occurrence.
     */
    private static final Pattern variablePattern = Pattern.compile("\\?([A-Za-z0-9_]+)");

    /**
     * The VALUES clause of a query: the variables and the terms of its single row.
     */
    private static final Pattern valuesPattern = Pattern
            .compile("values \\(((?: \\?[A-Za-z0-9_]+)+)\\) \\{\\((.*)\\)\\}");

    /**
     * A term of a VALUES row: an IRI, a literal or {@code undef}.
     */
    private static final Pattern termPattern = Pattern
            .compile("<[^>]*>|'[^']*'(?:\\^\\^<[^>]*>|@[A-Za-z0-9-]+)?|undef");

    /**
     * Queries received by the endpoints, each prefixed by the path of the endpoint and a space, in the order of their
     * arrival.
     */
    private static final List<String> queries = Collections.synchronizedList(new ArrayList<>());

    private static final Iri a = iri("a");
    private static final Iri b = iri("b");
    private static final Iri d = iri("d");

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
     * URL of the endpoint redirecting twice (permanently) before answering.
     */
    private static String redirected = null;

    /**
     * Connection pool of the database.
     */
    private static DataSource connectionPool = null;

    /**
     * Schema of the database.
     */
    private static DatabaseSchema schema = null;


    /**
     * Starts the endpoints on a free port of the loopback interface and creates the {@code service_settings_test}
     * schema with the table {@code item} of the local subjects {@code :a}, {@code :b} and {@code :c}, their integer
     * values 1, 2 and 4 and their endpoints (the other endpoint for {@code :b}, the main one for the rest).
     *
     * @throws SQLException on database errors
     * @throws IOException if the endpoints cannot be started
     */
    @BeforeAll
    static void init() throws SQLException, IOException
    {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/sparql", ServiceSettingsTest::answer);
        server.createContext("/other", ServiceSettingsTest::answer);
        server.createContext("/hop", ServiceSettingsTest::redirect);
        server.start();

        String base = "http://127.0.0.1:" + server.getAddress().getPort();
        endpoint = base + "/sparql";
        other = base + "/other";
        redirected = base + "/hop/2";

        connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema service_settings_test");
            statement.execute("create table service_settings_test.item (iri varchar collate \"C\" primary key, "
                    + "value numeric not null, endpoint varchar collate \"C\" not null)");
            statement.execute("insert into service_settings_test.item values ('" + a.getValue() + "', 1, '" + endpoint
                    + "'), ('" + b.getValue() + "', 2, '" + other + "'), ('" + iri("c").getValue() + "', 4, '"
                    + endpoint + "')");
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
     * Answers a query sent over the SPARQL protocol: the query is recorded, and the remote triples whose subject and
     * object match the terms of the VALUES row (if any) by their text are returned as bindings of the variables of the
     * query (the subject first, the object second) in the XML results format.
     *
     * @param exchange the request
     * @throws IOException on communication errors
     */
    private static void answer(HttpExchange exchange) throws IOException
    {
        String body = new String(exchange.getRequestBody().readAllBytes(), UTF_8);
        String query = URLDecoder.decode(body.replaceFirst("^query=", ""), UTF_8);

        queries.add(exchange.getRequestURI().getPath() + " " + query);

        List<String> variables = new ArrayList<>();
        Matcher matcher = variablePattern.matcher(query.split("values")[0]);

        while(matcher.find())
            if(!variables.contains(matcher.group(1)))
                variables.add(matcher.group(1));

        Map<String, String> values = new HashMap<>();
        Matcher valuesMatcher = valuesPattern.matcher(query);

        if(valuesMatcher.find())
        {
            String[] names = valuesMatcher.group(1).trim().split(" ");
            Matcher termMatcher = termPattern.matcher(valuesMatcher.group(2));

            for(String name : names)
            {
                termMatcher.find();

                if(!termMatcher.group().equals("undef"))
                    values.put(name.substring(1), termMatcher.group());
            }
        }

        StringBuilder builder = new StringBuilder();
        builder.append("<?xml version=\"1.0\"?><sparql xmlns=\"http://www.w3.org/2005/sparql-results#\"><head>");

        for(String variable : variables)
            builder.append("<variable name=\"").append(variable).append("\"/>");

        builder.append("</head><results>");

        for(RemoteTriple triple : remote)
        {
            String subject = "<" + iri(triple.subject()).getValue() + ">";
            String object = "'" + triple.value() + "'^^<" + triple.type().getValue() + ">";

            if(!values.getOrDefault(variables.get(0), subject).equals(subject))
                continue;

            if(!values.getOrDefault(variables.get(1), object).equals(object))
                continue;

            builder.append("<result><binding name=\"").append(variables.get(0)).append("\"><uri>");
            builder.append(iri(triple.subject()).getValue()).append("</uri></binding>");
            builder.append("<binding name=\"").append(variables.get(1)).append("\">");
            builder.append("<literal datatype=\"").append(triple.type().getValue()).append("\">");
            builder.append(triple.value()).append("</literal></binding></result>");
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
     * Redirects permanently (by the status 308, which the HTTP client does not follow by itself): {@code /hop/n} to
     * {@code /hop/n-1} and {@code /hop/1} to the main endpoint.
     *
     * @param exchange the request
     * @throws IOException on communication errors
     */
    private static void redirect(HttpExchange exchange) throws IOException
    {
        exchange.getRequestBody().readAllBytes();

        int hop = Integer.parseInt(exchange.getRequestURI().getPath().replaceFirst("^/hop/", ""));
        String location = hop > 1 ? redirected.replaceFirst("/2$", "/" + (hop - 1)) : endpoint;

        exchange.getResponseHeaders().add("location", location);
        exchange.sendResponseHeaders(308, -1);
        exchange.close();
    }


    /**
     * Engine over the local configuration with the given settings of remote endpoints: the table {@code item} mapped as
     * {@code ?s :v ?value} and {@code ?s :e ?endpoint}, and the constant quad {@code :d :w 1.5}.
     *
     * @param settings default settings of the remote endpoints
     * @param endpoints settings of particular endpoints, by URL
     * @return engine over the local configuration
     * @throws SQLException on database errors
     */
    private static Engine engine(ServiceSettings settings, Map<String, ServiceSettings> endpoints) throws SQLException
    {
        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, connectionPool, schema, false);

        DatabaseTable table = new DatabaseTable("service_settings_test", "item");
        TermMapping subject = config.createIriMapping(BuiltinClasses.iri, "iri");

        config.addQuadMapping(table, null, subject, config.createIriMapping(iri("v")),
                config.createLiteralMapping(xsdInteger, "value"));
        config.addQuadMapping(table, null, subject, config.createIriMapping(iri("e")),
                config.createIriMapping(BuiltinClasses.iri, "endpoint"));
        config.addQuadMapping(null, config.createIriMapping(d), config.createIriMapping(iri("w")),
                config.createLiteralMapping(xsdDecimal, new TypedLiteral("1.5", xsdDecimalIri)));

        config.setServiceSettings(settings);

        for(Map.Entry<String, ServiceSettings> entry : endpoints.entrySet())
            config.setServiceSettings(new Iri(entry.getKey()), entry.getValue());

        return new Engine(config);
    }


    /**
     * Engine over the local configuration with the given default settings of remote endpoints.
     *
     * @param settings default settings of the remote endpoints
     * @return engine over the local configuration
     * @throws SQLException on database errors
     */
    private static Engine engine(ServiceSettings settings) throws SQLException
    {
        return engine(settings, Map.of());
    }


    /**
     * Call of the main endpoint with the pattern {@code ?s :p ?o}.
     *
     * @return call of the main endpoint with the pattern {@code ?s :p ?o}
     */
    private static String service()
    {
        return service(endpoint);
    }


    /**
     * Call of the endpoint with the pattern {@code ?s :p ?o}.
     *
     * @param url URL of the endpoint, or the name of its variable
     * @return call of the endpoint with the pattern {@code ?s :p ?o}
     */
    private static String service(String url)
    {
        return "SERVICE " + (url.startsWith("?") ? url : "<" + url + ">") + " { ?s :p ?o } ";
    }


    /**
     * Rows of the query run by the engine.
     *
     * @param engine the engine
     * @param query the query, without the prefix declaration
     * @return rows of the query
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws LimitExceedException if the generated SQL is too long
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static List<List<RdfTerm>> execute(Engine engine, String query)
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
     * The xsd:integer literal of the lexical form.
     *
     * @param value the lexical form
     * @return the xsd:integer literal of the lexical form
     */
    private static TypedLiteral integer(String value)
    {
        return new TypedLiteral(value, xsdIntegerIri);
    }


    /**
     * The xsd:decimal literal of the lexical form.
     *
     * @param value the lexical form
     * @return the xsd:decimal literal of the lexical form
     */
    private static TypedLiteral decimal(String value)
    {
        return new TypedLiteral(value, xsdDecimalIri);
    }


    @Test
    @DisplayName("the limits of the settings are checked")
    void limits()
    {
        assertThrows(IllegalArgumentException.class, () -> ServiceSettings.DEFAULT.withContextLimit(0));
        assertThrows(IllegalArgumentException.class, () -> ServiceSettings.DEFAULT.withResultLimit(0));
        assertThrows(IllegalArgumentException.class, () -> ServiceSettings.DEFAULT.withRedirectLimit(-1));

        ServiceSettings settings = ServiceSettings.DEFAULT.withSendLiterals(true).withCanonicalizeLiterals(false)
                .withContextLimit(1).withResultLimit(2).withRedirectLimit(0);

        assertThat(settings, is(new ServiceSettings(true, false, 1, 2, 0)));
    }


    @Test
    @DisplayName("an endpoint has its own settings or the defaults, and an imported configuration brings its endpoints")
    void configuration() throws Exception
    {
        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, connectionPool, schema, false);
        SparqlDatabaseConfiguration imported = new SparqlDatabaseConfiguration(null, connectionPool, schema, false);

        Iri own = new Iri(endpoint);
        Iri foreign = new Iri(other);

        ServiceSettings defaults = ServiceSettings.DEFAULT.withContextLimit(10);
        ServiceSettings specific = ServiceSettings.DEFAULT.withContextLimit(20);
        ServiceSettings importedSpecific = ServiceSettings.DEFAULT.withContextLimit(30);

        config.setServiceSettings(defaults);
        config.setServiceSettings(own, specific);
        imported.setServiceSettings(ServiceSettings.DEFAULT.withContextLimit(40));
        imported.setServiceSettings(own, importedSpecific);
        imported.setServiceSettings(foreign, importedSpecific);

        assertThat(config.getServiceSettings(), is(defaults));
        assertThat(config.getServiceSettings(own), is(specific));
        assertThat(config.getServiceSettings(foreign), is(defaults));

        config.addService(imported, false);

        assertThat(config.getServiceSettings(), is(defaults));
        assertThat(config.getServiceSettings(own), is(specific));
        assertThat(config.getServiceSettings(foreign), is(importedSpecific));

        config.setServiceSettings(own, null);

        assertThat(config.getServiceSettings(own), is(defaults));
    }


    @Test
    @DisplayName("received literals are canonicalized by default and kept as received otherwise")
    void canonicalization() throws Exception
    {
        String query = "SELECT ?s ?o WHERE { " + service() + "}";

        assertThat(execute(engine(ServiceSettings.DEFAULT), query), containsInAnyOrder(row(a, integer("1")),
                row(b, integer("2")), row(iri("c"), integer("3")), row(d, decimal("1.5"))));
        assertThat(execute(engine(ServiceSettings.DEFAULT.withCanonicalizeLiterals(false)), query), containsInAnyOrder(
                row(a, integer("01")), row(b, integer("2")), row(iri("c"), integer("+3")), row(d, decimal("1.50"))));
    }


    @Test
    @DisplayName("a literal value of a shared variable constrains the received solutions locally by default")
    void localLiterals() throws Exception
    {
        String query = "SELECT ?s WHERE { ?s :v ?o " + service() + "}";

        assertThat(execute(engine(ServiceSettings.DEFAULT), query), containsInAnyOrder(row(a), row(b)));
        assertThat(queries.size(), is(3));
        assertThat(queries.get(0), not(containsString("'")));
        assertThat(queries.get(0), containsString("undef"));

        reset();
        assertThat(execute(engine(ServiceSettings.DEFAULT.withCanonicalizeLiterals(false)), query),
                containsInAnyOrder(row(b)));
        assertThat(queries.size(), is(3));

        String decimal = "SELECT ?s WHERE { ?s :w ?o " + service() + "}";

        reset();
        assertThat(execute(engine(ServiceSettings.DEFAULT), decimal), containsInAnyOrder(row(d)));
        assertThat(queries.size(), is(1));

        reset();
        assertThat(execute(engine(ServiceSettings.DEFAULT.withCanonicalizeLiterals(false)), decimal), empty());
    }


    @Test
    @DisplayName("a literal value of a shared variable is sent and matched by the endpoint when literals are sent")
    void sentLiterals() throws Exception
    {
        String query = "SELECT ?s WHERE { ?s :v ?o " + service() + "}";

        assertThat(execute(engine(ServiceSettings.DEFAULT.withSendLiterals(true)), query), containsInAnyOrder(row(b)));
        assertThat(queries.size(), is(3));
        assertThat(queries.get(0), containsString("'"));
        assertThat(queries.get(0), not(containsString("undef")));
    }


    @Test
    @DisplayName("context solutions asking the same query share one request")
    void sharedRequests() throws Exception
    {
        String query = "SELECT ?x ?s WHERE { ?x :v ?o " + service() + "}";

        assertThat(execute(engine(ServiceSettings.DEFAULT), query), containsInAnyOrder(row(a, a), row(b, b)));
        assertThat(queries.size(), is(1));
        assertThat(queries.get(0), not(containsString("values")));

        reset();
        assertThat(execute(engine(ServiceSettings.DEFAULT.withSendLiterals(true)), query),
                containsInAnyOrder(row(b, b)));
        assertThat(queries.size(), is(3));
    }


    @Test
    @DisplayName("the context limit bounds the context solutions of a call")
    void contextLimit() throws Exception
    {
        String query = "SELECT ?s WHERE { ?s :v ?o " + service() + "}";

        assertThat(execute(engine(ServiceSettings.DEFAULT.withContextLimit(3)), query),
                containsInAnyOrder(row(a), row(b)));

        ServiceException e = assertThrows(ServiceException.class,
                () -> execute(engine(ServiceSettings.DEFAULT.withContextLimit(2)), query));

        assertThat(e.getMessage(), is(MessageType.serviceContextLimitExceeded.getText()));
    }


    @Test
    @DisplayName("the result limit bounds the solutions collected from the endpoint")
    void resultLimit() throws Exception
    {
        String query = "SELECT ?s ?o WHERE { " + service() + "}";

        assertThat(execute(engine(ServiceSettings.DEFAULT.withResultLimit(4)), query).size(), is(4));

        ServiceException e = assertThrows(ServiceException.class,
                () -> execute(engine(ServiceSettings.DEFAULT.withResultLimit(3)), query));

        assertThat(e.getMessage(), is(MessageType.serviceResultLimitExceeded.getText()));
    }


    @Test
    @DisplayName("the redirect limit bounds the redirects followed by a request")
    void redirectLimit() throws Exception
    {
        String query = "SELECT ?s ?o WHERE { " + service(redirected) + "}";

        assertThat(execute(engine(ServiceSettings.DEFAULT.withRedirectLimit(2)), query).size(), is(4));
        assertThat(queries.size(), is(1));

        ServiceException e = assertThrows(ServiceException.class,
                () -> execute(engine(ServiceSettings.DEFAULT.withRedirectLimit(1)), query));

        assertThat(e.getMessage(), is(String.format(MessageType.badServiceEndpoint.getText(), redirected)));
    }


    @Test
    @DisplayName("an endpoint with settings of its own does not follow the defaults")
    void endpointSettings() throws Exception
    {
        Engine engine = engine(ServiceSettings.DEFAULT, Map.of(other, ServiceSettings.DEFAULT.withSendLiterals(true)));

        assertThat(execute(engine, "SELECT ?x ?s WHERE { ?x :v ?o " + service() + "}"),
                containsInAnyOrder(row(a, a), row(b, b)));
        assertThat(queries.size(), is(1));

        reset();
        assertThat(execute(engine, "SELECT ?x ?s WHERE { ?x :v ?o " + service(other) + "}"),
                containsInAnyOrder(row(b, b)));
        assertThat(queries.size(), is(3));
    }


    @Test
    @DisplayName("an endpoint named by a variable gets its own settings, the limits of the call are the defaults")
    void variableEndpoint() throws Exception
    {
        Engine engine = engine(ServiceSettings.DEFAULT,
                Map.of(other, ServiceSettings.DEFAULT.withSendLiterals(true).withContextLimit(1)));

        assertThat(execute(engine, "SELECT ?x ?s WHERE { ?x :e ?endpoint . ?x :v ?o " + service("?endpoint") + "}"),
                containsInAnyOrder(row(a, a), row(b, b)));
        assertThat(queries.size(), is(2));
        assertThat(queries, containsInAnyOrder(not(containsString("values")), containsString("'2'")));
    }
}
