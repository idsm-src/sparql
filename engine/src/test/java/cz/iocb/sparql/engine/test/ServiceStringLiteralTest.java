package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInteger;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.sql.SQLException;
import java.util.ArrayList;
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
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.mapping.ConstantIriMapping;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.TypedLiteral;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.LimitExceedException;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.translator.ServiceException;
import cz.iocb.sparql.testing.Database;



/**
 * Literals sent to a remote SERVICE endpoint: a literal of the pattern is sent as the query wrote it, a plain string
 * without the datatype and {@code "str"^^xsd:string} with it, so that an endpoint distinguishing the two forms (RDF 1.0
 * semantics) receives the same term; literals of other datatypes keep their datatype and language-tagged strings their
 * tag, and the separator of GROUP_CONCAT stays a plain string. A value bound to a shared variable by the local pattern
 * has no written form: by default a literal value is not sent at all (and the VALUES clause of nothing but
 * {@code undef} is left out), and when the settings of the endpoint ask for literals to be sent, it is sent in the
 * VALUES clause with its datatype, an xsd:string value too. The endpoint is a server on the loopback interface that
 * records the queries it receives and answers each with no solutions.
 */
public class ServiceStringLiteralTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX : <http://example.org/> "
            + "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> ";

    /**
     * IRI of the xsd:integer datatype in the sent queries.
     */
    private static final String integerType = "^^<" + xsdIntegerIri.getValue() + ">";

    /**
     * IRI of the xsd:string datatype in the sent queries.
     */
    private static final String stringType = "^^<" + xsdStringIri.getValue() + ">";

    /**
     * Variables of a query, in the order of their first occurrence.
     */
    private static final Pattern variablePattern = Pattern.compile("\\?([A-Za-z0-9_]+)");

    /**
     * Queries received by the endpoint, in the order of their arrival.
     */
    private static final List<String> queries = Collections.synchronizedList(new ArrayList<>());

    /**
     * The endpoint server.
     */
    private static HttpServer server = null;

    /**
     * URL of the endpoint.
     */
    private static String endpoint = null;

    /**
     * Engine over the local configuration with the default settings of remote endpoints.
     */
    private static Engine engine = null;

    /**
     * Engine over the local configuration sending the literal values of the shared variables.
     */
    private static Engine literalEngine = null;


    /**
     * Starts the endpoint on a free port of the loopback interface and creates the local configuration of two constant
     * quads: {@code :a :p "str"} and {@code :a :q 1}.
     *
     * @throws SQLException on database errors
     * @throws IOException if the endpoint cannot be started
     */
    @BeforeAll
    static void init() throws SQLException, IOException
    {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/sparql", ServiceStringLiteralTest::answer);
        server.start();

        endpoint = "http://127.0.0.1:" + server.getAddress().getPort() + "/sparql";

        DataSource connectionPool = Database.getPool();
        DatabaseSchema schema = new DatabaseSchema(connectionPool);

        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, connectionPool, schema, false);

        config.addQuadMapping(null, new ConstantIriMapping(iri("a")), new ConstantIriMapping(iri("p")),
                config.createLiteralMapping("str"));
        config.addQuadMapping(null, new ConstantIriMapping(iri("a")), new ConstantIriMapping(iri("q")),
                config.createLiteralMapping(xsdInteger, new TypedLiteral("1", xsdIntegerIri)));

        engine = new Engine(config);

        SparqlDatabaseConfiguration literalConfig = new SparqlDatabaseConfiguration(null, connectionPool, schema,
                false);
        literalConfig.addService(config, true);
        literalConfig.setServiceSettings(literalConfig.getServiceSettings().withSendLiterals(true));

        literalEngine = new Engine(literalConfig);
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
     * Forgets the queries received so far.
     */
    @BeforeEach
    void reset()
    {
        queries.clear();
    }


    /**
     * Answers a query sent over the SPARQL protocol: the query is recorded and an empty result with the variables of
     * the query in its head is returned in the XML results format.
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
        Matcher matcher = variablePattern.matcher(query.split("values")[0]);

        while(matcher.find())
            if(!variables.contains(matcher.group(1)))
                variables.add(matcher.group(1));

        StringBuilder builder = new StringBuilder();
        builder.append("<?xml version=\"1.0\"?><sparql xmlns=\"http://www.w3.org/2005/sparql-results#\"><head>");

        for(String variable : variables)
            builder.append("<variable name=\"").append(variable).append("\"/>");

        builder.append("</head><results></results></sparql>");

        byte[] data = builder.toString().getBytes(UTF_8);

        exchange.getResponseHeaders().add("content-type", "application/sparql-results+xml");
        exchange.sendResponseHeaders(200, data.length);

        try(OutputStream out = exchange.getResponseBody())
        {
            out.write(data);
        }
    }


    /**
     * Runs the query by the engine and returns the queries the endpoint received for it.
     *
     * @param engine the engine
     * @param query the query, without the prefix declarations
     * @return the queries the endpoint received
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws LimitExceedException if the generated SQL is too long
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static List<String> sent(Engine engine, String query)
            throws SQLException, TranslateExceptions, LimitExceedException, ServiceException
    {
        try(Request request = engine.getRequest(); Result result = request.execute(prefix + query))
        {
            while(result.next());
        }

        return new ArrayList<>(queries);
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
     * The sole query the endpoint received for the query run by the engine with the default settings.
     *
     * @param query the query, without the prefix declarations
     * @return the query the endpoint received
     * @throws Exception on errors
     */
    private static String sole(String query) throws Exception
    {
        return sole(engine, query);
    }


    /**
     * The sole query the endpoint received for the query run by the engine.
     *
     * @param engine the engine
     * @param query the query, without the prefix declarations
     * @return the query the endpoint received
     * @throws Exception on errors
     */
    private static String sole(Engine engine, String query) throws Exception
    {
        List<String> received = sent(engine, query);
        assertThat(received.size(), is(1));
        return received.get(0);
    }


    @Test
    @DisplayName("a plain string of the pattern is sent without the datatype")
    void plainString() throws Exception
    {
        String query = sole("SELECT ?s WHERE { " + service("?s :p \"str\"") + "}");

        assertThat(query, containsString(" 'str' "));
        assertThat(query, not(containsString("XMLSchema#string")));
    }


    @Test
    @DisplayName("an explicit xsd:string of the pattern is sent with the datatype")
    void explicitString() throws Exception
    {
        String query = sole("SELECT ?s WHERE { " + service("?s :p \"str\"^^xsd:string") + "}");

        assertThat(query, containsString(" 'str'" + stringType + " "));
    }


    @Test
    @DisplayName("the separator of GROUP_CONCAT is sent as a plain string")
    void separator() throws Exception
    {
        String query = sole("SELECT ?s ?c WHERE { "
                + service("SELECT ?s (GROUP_CONCAT(?o; SEPARATOR=\",\") AS ?c) " + "WHERE { ?s :p ?o } GROUP BY ?s")
                + "}");

        assertThat(query, containsString("separator =',' "));
        assertThat(query, not(containsString("XMLSchema#string")));
    }


    @Test
    @DisplayName("other literals of the pattern keep their datatype or language tag")
    void otherLiterals() throws Exception
    {
        String query = sole("SELECT ?s WHERE { " + service(
                "?s :p 1 . ?s :q \"2\"^^xsd:integer . ?s :r \"a\"@en . " + "?s :t \"b\"@en--ltr . ?s :u \"c\"^^:type")
                + "}");

        assertThat(query, containsString(" '1'" + integerType + " "));
        assertThat(query, containsString(" '2'" + integerType + " "));
        assertThat(query, containsString(" 'a'@en "));
        assertThat(query, containsString(" 'b'@en--ltr "));
        assertThat(query, containsString(" 'c'^^<http://example.org/type> "));
    }


    @Test
    @DisplayName("a literal value of a shared variable is not sent by default")
    void sharedLiteralNotSent() throws Exception
    {
        assertThat(sole("SELECT ?s WHERE { :a :p ?v " + service("?s :r ?v") + "}"), not(containsString("values")));

        reset();
        assertThat(sole("SELECT ?s WHERE { :a :q ?v " + service("?s :r ?v") + "}"), not(containsString("values")));
    }


    @Test
    @DisplayName("an xsd:string value of a shared variable is sent with the datatype when literals are sent")
    void sharedString() throws Exception
    {
        String query = sole(literalEngine, "SELECT ?s WHERE { :a :p ?v " + service("?s :r ?v") + "}");

        assertThat(query, containsString("values ( ?v) {('str'" + stringType + " )}"));
    }


    @Test
    @DisplayName("an xsd:integer value of a shared variable is sent with the datatype when literals are sent")
    void sharedInteger() throws Exception
    {
        String query = sole(literalEngine, "SELECT ?s WHERE { :a :q ?v " + service("?s :r ?v") + "}");

        assertThat(query, containsString("values ( ?v) {('1'" + integerType + " )}"));
    }
}
