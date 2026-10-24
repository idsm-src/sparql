package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
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
import cz.iocb.sparql.engine.imcode.SqlSelect;
import cz.iocb.sparql.engine.mapping.classes.IntegerUserIriClass;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.TypedLiteral;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.LimitExceedException;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Request.PreparedQuery;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.translator.ServiceException;
import cz.iocb.sparql.engine.translator.TranslateVisitor;
import cz.iocb.sparql.testing.Database;



/**
 * Received solutions of a SERVICE call that bind a variable to a value of a class the parent does not accept are left
 * out before they are stored. The fake endpoint answers every query with three subjects: a node, an other and an IRI of
 * no class; only the nodes have labels, so a join with the labels or a filter comparing the subject with a node accepts
 * the nodes alone.
 */
public class ServiceAcceptedClassesTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> ";

    /**
     * Names of the variables of a query.
     */
    private static final Pattern variablePattern = Pattern.compile("\\?([A-Za-z0-9_]+)");

    /**
     * The subject outside the classes of the nodes and the others.
     */
    private static final Iri thing = new Iri("http://other.org/thing");

    /**
     * The fake endpoint.
     */
    private static HttpServer server = null;

    /**
     * URL of the fake endpoint.
     */
    private static String endpoint = null;

    /**
     * Configuration mapping the labels.
     */
    private static SparqlDatabaseConfiguration config = null;


    /**
     * Starts the fake endpoint and creates the {@code service_accepted_test} schema with the labels of the nodes 1 and
     * 2, and the configuration mapping them.
     */
    @BeforeAll
    static void init() throws SQLException, IOException
    {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/sparql", ServiceAcceptedClassesTest::answer);
        server.start();

        endpoint = "http://127.0.0.1:" + server.getAddress().getPort() + "/sparql";

        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema service_accepted_test");
            statement.execute(
                    "create table service_accepted_test.labels (id integer not null, " + "label varchar not null)");
            statement.execute("insert into service_accepted_test.labels values (1, 'one'), (2, 'two')");
        }

        DatabaseSchema schema = new DatabaseSchema(connectionPool);

        config = new SparqlDatabaseConfiguration(null, connectionPool, schema, false);
        config.addPrefix("ex", "http://example.org/");
        config.addIriClass(new IntegerUserIriClass("node", INT4, "http://example.org/n/"));
        config.addIriClass(new IntegerUserIriClass("other", INT4, "http://example.org/a/"));

        DatabaseTable labels = new DatabaseTable("service_accepted_test", "labels");

        config.addQuadMapping(labels, null, config.createIriMapping("node", "id"), config.createIriMapping("ex:label"),
                config.createLiteralMapping(xsdString, "label"));
    }


    /**
     * Stops the fake endpoint.
     */
    @AfterAll
    static void stop()
    {
        server.stop(0);
    }


    /**
     * Answers a query sent over the SPARQL protocol: the first variable of the query is bound to the node 1, the other
     * 10 and the IRI of no class, the second one to a string, in the XML results format.
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

        StringBuilder builder = new StringBuilder();
        builder.append("<?xml version=\"1.0\"?><sparql xmlns=\"http://www.w3.org/2005/sparql-results#\"><head>");

        for(String variable : variables)
            builder.append("<variable name=\"").append(variable).append("\"/>");

        builder.append("</head><results>");

        for(Iri subject : List.of(node(1), other(10), thing))
        {
            builder.append("<result><binding name=\"").append(variables.get(0)).append("\"><uri>");
            builder.append(subject.getValue()).append("</uri></binding>");
            builder.append("<binding name=\"").append(variables.get(1)).append("\">");
            builder.append("<literal>x</literal></binding></result>");
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
     * Call of the fake endpoint with the pattern {@code ?s ex:p ?o}.
     */
    private static String service()
    {
        return "SERVICE <" + endpoint + "> { ?s ex:p ?o } ";
    }


    /**
     * Runs the query and returns all rows.
     */
    private static List<List<RdfTerm>> execute(String query)
            throws SQLException, TranslateExceptions, LimitExceedException, ServiceException
    {
        Engine engine = new Engine(config);

        try(Request request = engine.getRequest(); Result result = request.execute(prefix + query))
        {
            List<List<RdfTerm>> rows = new ArrayList<>();

            while(result.next())
                rows.add(Arrays.asList(result.getRow()));

            return rows;
        }
    }


    /**
     * Returns the SQL code the query is translated to once the call is evaluated.
     */
    private static String translate(String query) throws Exception
    {
        Engine engine = new Engine(config);

        try(Request request = engine.getRequest())
        {
            PreparedQuery prepared = request.prepareQuery(prefix + query, null);
            SqlSelect imcode = new TranslateVisitor(request).translate(prepared.getSyntaxTree(), null, null, List.of());

            return imcode.optimize(request, false).optimize(request, true).translate(request);
        }
    }


    /**
     * Expected row.
     */
    private static List<RdfTerm> row(RdfTerm... terms)
    {
        return Arrays.asList(terms);
    }


    /**
     * IRI of the node with the given number.
     */
    private static Iri node(int number)
    {
        return new Iri("http://example.org/n/" + number);
    }


    /**
     * IRI of the other with the given number.
     */
    private static Iri other(int number)
    {
        return new Iri("http://example.org/a/" + number);
    }


    /**
     * String literal.
     */
    private static TypedLiteral string(String value)
    {
        return new TypedLiteral(value, xsdStringIri);
    }


    @Test
    @DisplayName("without a restricting parent every received solution is kept")
    void receivedSolutionsKept() throws Exception
    {
        String query = "SELECT ?s WHERE { " + service() + "}";

        assertThat(execute(query), containsInAnyOrder(row(node(1)), row(other(10)), row(thing)));
        assertThat(translate(query), containsString("other.org/thing"));
    }


    @Test
    @DisplayName("a join above the call leaves out the received solutions of other classes")
    void joinPrunesReceivedSolutions() throws Exception
    {
        // the ordered sub-select keeps the call apart from the join, so that it is evaluated without a context
        String query = "SELECT ?s ?l WHERE { { SELECT ?s WHERE { " + service() + "} ORDER BY ?s } ?s ex:label ?l }";

        assertThat(execute(query), containsInAnyOrder(row(node(1), string("one"))));
        assertThat(translate(query), not(containsString("other.org/thing")));
        // the single solution left is stored as one row of the VALUES list, not as three rows of which two bind nothing
        assertThat(translate(query), not(containsString("), (")));
    }


    @Test
    @DisplayName("a filter above the call leaves out the received solutions of other classes")
    void filterPrunesReceivedSolutions() throws Exception
    {
        String query = "SELECT ?s WHERE { " + service() + "FILTER(?s = <http://example.org/n/1>) }";

        assertThat(execute(query), containsInAnyOrder(row(node(1))));
        assertThat(translate(query), not(containsString("other.org/thing")));
    }
}
