package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
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
import java.util.stream.IntStream;
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
import cz.iocb.sparql.engine.mapping.classes.IntegerUserIriClass;
import cz.iocb.sparql.engine.mapping.classes.StringUserIriClass;
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
 * Received terms of a SERVICE call classified in overlapping classes (the compounds, the compounds with a small id, a
 * subclass of them, see {@link SmallCompoundIriClass}, and the compounds with an even id, declared to share IRIs with
 * both) are stored in the union of the classes, as the classes of a variable have to be disjoint: a small result as a
 * VALUES list, a large one in a temporary table. Unions, comparisons and joins above the call then see every received
 * term.
 */
public class ServiceOverlappingClassesTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> ";

    /**
     * Prefix of the IRIs of the compounds.
     */
    private static final String iriPrefix = "http://example.org/compound/";

    /**
     * Number of the compounds the large result receives, more than a VALUES list holds.
     */
    private static final int largeSize = 1500;

    /**
     * Names of the variables of a query.
     */
    private static final Pattern variablePattern = Pattern.compile("\\?([A-Za-z0-9_]+)");

    /**
     * The fake endpoint.
     */
    private static HttpServer server = null;

    /**
     * URL of the fake endpoint, without the path telling the size of the result.
     */
    private static String endpoint = null;

    /**
     * Configuration mapping the labels of the compounds.
     */
    private static SparqlDatabaseConfiguration config = null;


    /**
     * Starts the fake endpoint and creates the {@code service_overlap_test} schema with the labels of the compounds 2,
     * 41, 42, 141 and 142, and the configuration mapping them.
     */
    @BeforeAll
    static void init() throws SQLException, IOException
    {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/small", exchange -> answer(exchange, List.of(41, 141, 42)));
        server.createContext("/large", exchange -> answer(exchange, IntStream.range(0, largeSize).boxed().toList()));
        server.start();

        endpoint = "http://127.0.0.1:" + server.getAddress().getPort();

        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema service_overlap_test");
            statement.execute("create table service_overlap_test.compound (id int not null primary key, "
                    + "label varchar collate \"C\" not null)");
            statement.execute("insert into service_overlap_test.compound values (2, 'two'), (41, 'forty-one'), "
                    + "(42, 'forty-two'), (141, 'hundred forty-one'), (142, 'hundred forty-two')");
        }

        DatabaseSchema schema = new DatabaseSchema(connectionPool);

        IntegerUserIriClass compound = new IntegerUserIriClass("compound", INT4, iriPrefix);
        StringUserIriClass even = new StringUserIriClass("even", iriPrefix, "[0-9]*[02468]");
        SmallCompoundIriClass small = new SmallCompoundIriClass("small", compound, iriPrefix);

        config = new SparqlDatabaseConfiguration(null, connectionPool, schema, false);
        config.addPrefix("ex", "http://example.org/");
        config.addIriClass(compound);
        config.addIriClass(even);
        config.addIriClass(small);
        config.addIriClassOverlap(compound, even);
        config.addIriClassOverlap(small, even);

        config.addQuadMapping(new DatabaseTable("service_overlap_test", "compound"), null,
                config.createIriMapping("compound", "id"), config.createIriMapping("ex:label"),
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
     * Answers a query sent over the SPARQL protocol: the first variable of the query is bound to the compounds with the
     * given ids, in the XML results format.
     *
     * @param exchange the request
     * @param ids the ids of the compounds
     * @throws IOException on communication errors
     */
    private static void answer(HttpExchange exchange, List<Integer> ids) throws IOException
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

        for(int id : ids)
        {
            builder.append("<result><binding name=\"").append(variables.get(0)).append("\"><uri>");
            builder.append(iriPrefix).append(id).append("</uri></binding></result>");
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
     * Call of the fake endpoint with the pattern {@code ?s ex:p ?o}, answered by the compounds of the given result.
     */
    private static String service(String result)
    {
        return "SERVICE <" + endpoint + "/" + result + "> { ?s ex:p ?o } ";
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
     * Expected row.
     */
    private static List<RdfTerm> row(RdfTerm... terms)
    {
        return Arrays.asList(terms);
    }


    /**
     * IRI of the compound with the given id.
     */
    private static Iri compound(int id)
    {
        return new Iri(iriPrefix + id);
    }


    /**
     * String literal.
     */
    private static TypedLiteral string(String value)
    {
        return new TypedLiteral(value, xsdStringIri);
    }


    /**
     * Integer literal.
     */
    private static TypedLiteral integer(int value)
    {
        return new TypedLiteral(Integer.toString(value), xsdIntegerIri);
    }


    @Test
    @DisplayName("a small result keeps the received terms of every class in a union and in a comparison")
    void smallResult() throws Exception
    {
        // compound 41 takes the subclass, 141 the class itself and 42 the intersection of the subclass with the even
        assertThat(
                execute("SELECT DISTINCT ?s WHERE { { " + service("small")
                        + "} UNION { BIND(<http://example.org/compound/142> AS ?s) } }"),
                containsInAnyOrder(row(compound(41)), row(compound(141)), row(compound(42)), row(compound(142))));

        assertThat(
                execute("SELECT ?s (COUNT(*) AS ?c) WHERE { " + service("small")
                        + "?t ex:label ?l FILTER(?t != ?s) } GROUP BY ?s"),
                containsInAnyOrder(row(compound(41), integer(4)), row(compound(141), integer(4)),
                        row(compound(42), integer(4))));
    }


    @Test
    @DisplayName("a large result keeps the received terms of every class in a union, a comparison and a join")
    void largeResult() throws Exception
    {
        assertThat(
                execute("SELECT (COUNT(DISTINCT ?s) AS ?c) WHERE { { " + service("large")
                        + "} UNION { BIND(<http://example.org/compound/142> AS ?s) } }"),
                containsInAnyOrder(row(integer(largeSize))));

        assertThat(
                execute("SELECT (COUNT(*) AS ?c) WHERE { " + service("large")
                        + "FILTER(?s != <http://example.org/compound/141>) }"),
                containsInAnyOrder(row(integer(largeSize - 1))));

        assertThat(execute("SELECT ?s ?l WHERE { " + service("large") + "?s ex:label ?l }"),
                containsInAnyOrder(row(compound(2), string("two")), row(compound(41), string("forty-one")),
                        row(compound(42), string("forty-two")), row(compound(141), string("hundred forty-one")),
                        row(compound(142), string("hundred forty-two"))));
    }
}
