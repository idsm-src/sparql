package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
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
 * The reordering of SERVICE calls ({@link Engine#getRequest(boolean)}): a call is evaluated before its siblings and
 * asked about the distinct combinations of the shared variables the siblings bind, keeping a variable only as long as
 * the combinations are estimated to fit into the context limit of its endpoint, so a context of more solutions than the
 * limit asks the pattern without the variable instead of failing. The endpoint is a server on the loopback interface
 * answering the pattern {@code ?s :q ?x} from the remote subjects {@code :i2}, {@code :i4}, {@code :i6} and
 * {@code :i30}, each with the string {@code x} followed by its number; the analysed table {@code item} holds the local
 * subjects {@code :i1} to {@code :i20}, mapped by :p to their numbers; the context limit of the endpoint is 5. Of
 * several calls, the one leaving out the fewest shared variables is evaluated first; the variables of its pattern that
 * no sibling binds do not count.
 */
public class ServiceReorderTest
{
    /**
     * Prefix declaration prepended to every query.
     */
    private static final String prefix = "PREFIX : <http://example.org/> ";

    /**
     * Numbers of the remote subjects.
     */
    private static final List<Integer> remote = List.of(2, 4, 6, 30);

    /**
     * The first variable of a query, the subject of its pattern.
     */
    private static final Pattern variablePattern = Pattern.compile("\\?([A-Za-z0-9_]+)");

    /**
     * The VALUES clause of a query giving the subject, with the IRI of its single row.
     */
    private static final Pattern valuesPattern = Pattern
            .compile("values \\( \\?[A-Za-z0-9_]+\\) \\{\\(<([^>]*)>\\)\\}");

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
     * Engine over the local configuration.
     */
    private static Engine engine = null;


    /**
     * Starts the endpoint on a free port of the loopback interface, creates the {@code service_reorder_test} schema
     * with the analysed table {@code item}, and maps it.
     *
     * @throws SQLException on database errors
     * @throws IOException if the endpoint cannot be started
     */
    @BeforeAll
    static void init() throws SQLException, IOException
    {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/sparql", ServiceReorderTest::answer);
        server.start();

        endpoint = "http://127.0.0.1:" + server.getAddress().getPort() + "/sparql";

        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema service_reorder_test");
            statement.execute("create table service_reorder_test.item (iri varchar collate \"C\" primary key, "
                    + "v int not null)");
            statement.execute("insert into service_reorder_test.item select 'http://example.org/i' || i, i "
                    + "from generate_series(1, 20) as i");
            statement.execute("analyze service_reorder_test.item");
        }

        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, connectionPool,
                new DatabaseSchema(connectionPool), false);

        config.addQuadMapping(new DatabaseTable("service_reorder_test", "item"), null,
                config.createIriMapping(BuiltinClasses.iri, "iri"), config.createIriMapping(iri("p")),
                config.createLiteralMapping(xsdInt, "v"));
        config.setServiceSettings(new Iri(endpoint), ServiceSettings.DEFAULT.withContextLimit(5));

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
     * Forgets the queries received so far.
     */
    @BeforeEach
    void reset()
    {
        queries.clear();
    }


    /**
     * Answers a query sent over the SPARQL protocol: the query is recorded, and the remote subjects, only the one the
     * VALUES row gives if there is one, are returned with their strings as bindings of the first and the second
     * variable of the query in the XML results format.
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

        Matcher valuesMatcher = valuesPattern.matcher(query);
        String value = valuesMatcher.find() ? valuesMatcher.group(1) : null;

        StringBuilder builder = new StringBuilder();
        builder.append("<?xml version=\"1.0\"?><sparql xmlns=\"http://www.w3.org/2005/sparql-results#\"><head>");

        for(String variable : variables)
            builder.append("<variable name=\"").append(variable).append("\"/>");

        builder.append("</head><results>");

        for(int number : remote)
        {
            String subject = iri("i" + number).getValue();

            if(value != null && !value.equals(subject))
                continue;

            builder.append("<result><binding name=\"").append(variables.get(0)).append("\"><uri>");
            builder.append(subject).append("</uri></binding>");
            builder.append("<binding name=\"").append(variables.get(1)).append("\"><literal>x");
            builder.append(number).append("</literal></binding></result>");
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
     * Rows of the query run with the reordering of calls.
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
        try(Request request = engine.getRequest(true); Result result = request.execute(prefix + query))
        {
            List<List<RdfTerm>> rows = new ArrayList<>();

            while(result.next())
                rows.add(Arrays.asList(result.getRow()));

            return rows;
        }
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
     * The row of a local subject joined with its remote string.
     *
     * @param number the number of the subject
     * @return the row of the subject, its number and its string
     */
    private static List<RdfTerm> row(int number)
    {
        return List.of(iri("i" + number), new TypedLiteral(Integer.toString(number), xsdIntIri),
                new TypedLiteral("x" + number, xsdStringIri));
    }


    @Test
    @DisplayName("a context of more solutions than the limit asks the pattern without the shared variable")
    void beyondLimit() throws Exception
    {
        assertThat(execute("SELECT ?s ?v ?x WHERE { ?s :p ?v SERVICE <" + endpoint + "> { ?s :q ?x } }"),
                containsInAnyOrder(row(2), row(4), row(6)));

        assertThat(queries.size(), is(1));
        assertThat(queries.get(0), not(containsString("values")));
    }


    @Test
    @DisplayName("a context of fewer solutions than the limit sends the values of the shared variable")
    void withinLimit() throws Exception
    {
        assertThat(execute(
                "SELECT ?s ?v ?x WHERE { VALUES ?s { :i2 :i3 } ?s :p ?v SERVICE <" + endpoint + "> { ?s :q ?x } }"),
                containsInAnyOrder(row(2)));

        assertThat(queries.size(), is(2));
        assertThat(queries, everyItem(containsString("values")));
    }


    @Test
    @DisplayName("the call leaving out the fewest shared variables is evaluated first, whatever its own variables are")
    void privateVariables() throws Exception
    {
        execute("SELECT * WHERE { VALUES ?s { :i2 :i3 } ?s :p ?v . ?t :p ?w SERVICE <" + endpoint
                + "> { ?s :q ?x . ?x :r ?y } SERVICE <" + endpoint + "> { ?s :q ?w } }");

        // the first call leaves out no variable the siblings bind, the second one leaves out ?w, as the siblings give
        // more combinations of ?s and ?w than the limit
        assertThat(queries.get(0), containsString(iri("r").getValue()));
    }
}
