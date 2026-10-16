package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInteger;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdBooleanIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerIri;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
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
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.mapping.ConstantIriMapping;
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
 * EXISTS patterns calling a remote SERVICE: the call cannot be evaluated for the current solution of the filter, so it
 * is evaluated by itself and its solutions are joined with the current solution, and the SQL of the test exists only
 * once the second optimisation pass has evaluated the call, wherever the test stands (a filter, a BIND, an OPTIONAL, an
 * aggregate, a HAVING, a joined group or another EXISTS). The endpoint is a server on the loopback interface answering
 * the pattern {@code ?s
 * 
<p>
 *  ?o} from a few fixed remote triples.
 */
public class ExistsServiceTest
{
    /**
     * A remote triple with an integer object.
     *
     * @param subject local name of the subject
     * @param predicate local name of the predicate
     * @param object the object
     */
    private static record RemoteTriple(String subject, String predicate, int object)
    {
    }


    /**
     * Prefix declaration prepended to every query.
     */
    private static final String prefix = "PREFIX : <http://example.org/> ";

    /**
     * Triples of the remote endpoint.
     */
    private static final List<RemoteTriple> remote = List.of(new RemoteTriple("a", "p", 1),
            new RemoteTriple("b", "p", 3), new RemoteTriple("a", "q", 7));

    /**
     * Variables of a query, in the order of their first occurrence.
     */
    private static final Pattern variablePattern = Pattern.compile("\\?([A-Za-z0-9_]+)");

    /**
     * The predicate IRI of a pattern.
     */
    private static final Pattern predicatePattern = Pattern.compile("<http://example.org/([a-z]+)>");

    private static final Iri a = iri("a");
    private static final Iri b = iri("b");
    private static final Iri c = iri("c");
    private static final Iri p = iri("p");
    private static final Iri q = iri("q");
    private static final TypedLiteral yes = new TypedLiteral("true", xsdBooleanIri);
    private static final TypedLiteral no = new TypedLiteral("false", xsdBooleanIri);

    /**
     * The endpoint server.
     */
    private static HttpServer server = null;

    /**
     * URL of the endpoint.
     */
    private static String endpoint = null;

    /**
     * Engine over the local configuration.
     */
    private static Engine engine = null;


    /**
     * Starts the endpoint on a free port of the loopback interface and creates the local configuration of constant
     * quads: {@code :a} has {@code :p 1} and {@code :q 1, 2}, {@code :b} has {@code :p 3} and {@code :q 4, 5},
     * {@code :c} has {@code :p 2}.
     *
     * @throws SQLException on database errors
     * @throws IOException if the endpoint cannot be started
     */
    @BeforeAll
    static void init() throws SQLException, IOException
    {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/sparql", ExistsServiceTest::answer);
        server.start();

        endpoint = "http://127.0.0.1:" + server.getAddress().getPort() + "/sparql";

        DataSource connectionPool = Database.getPool();
        DatabaseSchema schema = new DatabaseSchema(connectionPool);

        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, connectionPool, schema, false);

        quad(config, a, p, 1);
        quad(config, a, q, 1);
        quad(config, a, q, 2);
        quad(config, b, p, 3);
        quad(config, b, q, 4);
        quad(config, b, q, 5);
        quad(config, c, p, 2);

        engine = new Engine(config);
    }


    /**
     * Stops the endpoint.
     */
    @AfterAll
    static void stop()
    {
        server.stop(0);
    }


    /**
     * Answers a query sent over the SPARQL protocol: the remote triples of the predicate of the pattern are returned as
     * bindings of the variables of the query (the subject first, the object second) in the XML results format.
     *
     * @param exchange the request
     * @throws IOException on communication errors
     */
    private static void answer(HttpExchange exchange) throws IOException
    {
        String body = new String(exchange.getRequestBody().readAllBytes(), UTF_8);
        String query = URLDecoder.decode(body.replaceFirst("^query=", ""), UTF_8);

        List<String> variables = new ArrayList<>();
        Matcher matcher = variablePattern.matcher(query.split("values")[0]);

        while(matcher.find())
            if(!variables.contains(matcher.group(1)))
                variables.add(matcher.group(1));

        Matcher predicate = predicatePattern.matcher(query);
        predicate.find();

        StringBuilder builder = new StringBuilder();
        builder.append("<?xml version=\"1.0\"?><sparql xmlns=\"http://www.w3.org/2005/sparql-results#\"><head>");

        for(String variable : variables)
            builder.append("<variable name=\"").append(variable).append("\"/>");

        builder.append("</head><results>");

        for(RemoteTriple triple : remote)
        {
            if(!triple.predicate().equals(predicate.group(1)))
                continue;

            builder.append("<result><binding name=\"").append(variables.get(0)).append("\"><uri>");
            builder.append(iri(triple.subject()).getValue()).append("</uri></binding>");
            builder.append("<binding name=\"").append(variables.get(1)).append("\">");
            builder.append("<literal datatype=\"").append(xsdIntegerIri.getValue()).append("\">");
            builder.append(triple.object()).append("</literal></binding></result>");
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
     * Adds a constant quad with an xsd:integer object to the configuration.
     *
     * @param config the configuration
     * @param subject the subject
     * @param predicate the predicate
     * @param object the object
     */
    private static void quad(SparqlDatabaseConfiguration config, Iri subject, Iri predicate, int object)
    {
        config.addQuadMapping(null, new ConstantIriMapping(subject), new ConstantIriMapping(predicate),
                config.createLiteralMapping(xsdInteger, integer(object)));
    }


    /**
     * Call of the endpoint with the pattern.
     *
     * @param pattern the pattern of the call
     * @return call of the endpoint with the pattern
     */
    private static String service(String pattern)
    {
        return "SERVICE <" + endpoint + "> { " + pattern + " } ";
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
     * The xsd:integer literal of the value.
     *
     * @param value the value
     * @return the xsd:integer literal of the value
     */
    private static TypedLiteral integer(int value)
    {
        return new TypedLiteral(String.valueOf(value), xsdIntegerIri);
    }


    @Test
    @DisplayName("EXISTS, NOT EXISTS and a bound EXISTS with a remote SERVICE sharing the subject")
    void existence() throws Exception
    {
        assertThat(execute("SELECT ?s WHERE { ?s :p ?v FILTER EXISTS { " + service("?s :p ?o") + "} }"),
                containsInAnyOrder(row(a), row(b)));
        assertThat(execute("SELECT ?s WHERE { ?s :p ?v FILTER NOT EXISTS { " + service("?s :p ?o") + "} }"),
                contains(row(c)));
        assertThat(execute("SELECT ?s ?e WHERE { ?s :p ?v BIND(EXISTS { " + service("?s :p ?o") + "} AS ?e) }"),
                containsInAnyOrder(row(a, yes), row(b, yes), row(c, no)));
    }


    @Test
    @DisplayName("filters and a nested EXISTS over the solutions of the call")
    void nested() throws Exception
    {
        assertThat(execute("SELECT ?s WHERE { ?s :p ?v FILTER EXISTS { " + service("?s :p ?o") + "FILTER(?o > 2) } }"),
                contains(row(b)));
        assertThat(execute("SELECT ?s WHERE { ?s :p ?v FILTER EXISTS { " + service("?s :p ?o") + "FILTER EXISTS { "
                + service("?s :q ?n") + "} } }"), contains(row(a)));
        assertThat(execute("SELECT ?s WHERE { ?s :p ?v FILTER(EXISTS { " + service("?s :p ?o") + "} && ?v < 3) }"),
                contains(row(a)));
    }


    @Test
    @DisplayName("EXISTS with a remote SERVICE in a joined group, an OPTIONAL, an aggregate and a HAVING")
    void nodes() throws Exception
    {
        assertThat(execute("SELECT ?s ?n WHERE { { ?s :p ?v FILTER EXISTS { " + service("?s :p ?o") + "} } ?s :q ?n }"),
                containsInAnyOrder(row(a, integer(1)), row(a, integer(2)), row(b, integer(4)), row(b, integer(5))));
        assertThat(
                execute("SELECT ?s ?n WHERE { ?s :p ?v OPTIONAL { ?s :q ?n FILTER EXISTS { " + service("?s :p ?o")
                        + "} } }"),
                containsInAnyOrder(row(a, integer(1)), row(a, integer(2)), row(b, integer(4)), row(b, integer(5)),
                        row(c, null)));
        assertThat(execute("SELECT (SUM(IF(EXISTS { " + service("?s :p ?o") + "}, 1, 0)) AS ?c) WHERE { ?s :p ?v }"),
                contains(row(integer(2))));
        assertThat(execute("SELECT ?s (COUNT(*) AS ?c) WHERE { ?s :q ?n } GROUP BY ?s HAVING EXISTS { "
                + service("?s :p ?o") + "}"), containsInAnyOrder(row(a, integer(2)), row(b, integer(2))));
    }


    @Test
    @DisplayName("a failing SERVICE SILENT inside EXISTS yields the current solution")
    void silent() throws Exception
    {
        String call = "SERVICE SILENT <http://127.0.0.1:1/sparql> { ?s :p ?o } ";

        assertThat(execute("SELECT ?s WHERE { ?s :p ?v FILTER EXISTS { " + call + "} }"),
                containsInAnyOrder(row(a), row(b), row(c)));
        assertThat(execute("SELECT ?s WHERE { ?s :p ?v FILTER NOT EXISTS { " + call + "} }"), empty());
    }
}
