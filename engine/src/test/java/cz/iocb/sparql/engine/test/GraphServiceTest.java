package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
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
 * Remote SERVICE calls inside a {@code GRAPH ?g} pattern evaluated separately in every named graph. The pattern refers
 * to the graph of the current solution through the alias of a lateral join, and so may the context of a call (the
 * solutions before it in its group), which a statement of its own evaluated without the alias: the context is now
 * evaluated in all graphs at once, joined laterally with the named graphs again, and the received solutions are kept
 * for the graph of the current solution, in the right sides of OPTIONAL and MINUS, in sub-selects, in UNION branches
 * and in nested graphs; the context limit counts the context solutions of all graphs. The copies of a call that a union
 * splits for differently restricted parents do not share a result built for narrower restrictions, and a failing SILENT
 * endpoint is asked once. The graphs {@code a:100}, {@code a:101} (class {@code a}), {@code b:200} (class {@code b}),
 * {@code a:102} and {@code <http://other.org/G>} (constant graphs) hold {@code ex:s} triples, two graphs name an
 * endpoint by {@code ex:ep}. The endpoints are servers on the loopback interface honouring the VALUES row of a request:
 * {@code svc} answers {@code (?x ?y)} = {@code (a:1 b:10)}, {@code (b:20 "lit")}, {@code svc2} answers {@code (?x ?z)}
 * = {@code (a:2 a:3)}, {@code (a:5 -)}, {@code (- a:7)}, {@code svcg} answers {@code (?g ?v)} = {@code (a:100 "ok")},
 * {@code (a:999 "no")}, and {@code svcfail} fails.
 */
public class GraphServiceTest
{
    /**
     * Name of the schema of the test tables.
     */
    private static final String schemaName = "graph_service_test";

    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> PREFIX a: <http://example.org/a/> "
            + "PREFIX b: <http://example.org/b/> ";

    /**
     * A term of the VALUES row of a request: an IRI, {@code undef} or a literal.
     */
    private static final Pattern termPattern = Pattern
            .compile("<[^>]*>|undef|\"(?:[^\"\\\\]|\\\\.)*\"(?:\\^\\^<[^>]*>|@[\\w-]+)?");

    /**
     * Answers of the endpoints by their names: solutions as maps of variable names to terms in the syntax of SPARQL.
     */
    private static final Map<String, List<Map<String, String>>> answers = new LinkedHashMap<>();

    /**
     * Requests received by the endpoints, by their names.
     */
    private static final Map<String, AtomicInteger> requests = new ConcurrentHashMap<>();

    private static final Iri g = new Iri("http://other.org/G");
    private static final TypedLiteral lit = new TypedLiteral("lit", xsdStringIri);
    private static final TypedLiteral ok = new TypedLiteral("ok", xsdStringIri);

    /**
     * The endpoint server.
     */
    private static HttpServer server = null;

    /**
     * Base URL of the endpoints.
     */
    private static String base = null;

    /**
     * Connection pool of the database.
     */
    private static DataSource connectionPool = null;

    /**
     * Schema of the database.
     */
    private static DatabaseSchema schema = null;

    /**
     * Requests received by the endpoints during the last execution, by their names.
     */
    private static Map<String, Integer> counts = Map.of();


    /**
     * Starts the endpoints on a free port of the loopback interface and creates the test tables.
     *
     * @throws SQLException on database errors
     * @throws IOException if the endpoints cannot be started
     */
    @BeforeAll
    static void init() throws SQLException, IOException
    {
        answers.put("svc", List.of(solution("x", "<http://example.org/a/1>", "y", "<http://example.org/b/10>"),
                solution("x", "<http://example.org/b/20>", "y", "\"lit\"")));
        answers.put("svc2", List.of(solution("x", "<http://example.org/a/2>", "z", "<http://example.org/a/3>"),
                solution("x", "<http://example.org/a/5>"), solution("z", "<http://example.org/a/7>")));
        answers.put("svcg", List.of(solution("g", "<http://example.org/a/100>", "v", "\"ok\""),
                solution("g", "<http://example.org/a/999>", "v", "\"no\"")));
        answers.put("svcfail", List.of());

        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);

        for(String name : answers.keySet())
            server.createContext("/" + name, exchange -> answer(name, exchange));

        server.start();

        base = "http://127.0.0.1:" + server.getAddress().getPort() + "/";

        connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            String tables = schemaName + ".";

            statement.execute("create schema " + schemaName);
            statement.execute("create table " + tables + "ga (g int not null, s int not null, o int not null)");
            statement.execute(
                    "insert into " + tables + "ga values (100, 1, 2), (100, 2, 3), (101, 2, 3), (101, 3, 101)");
            statement.execute("create table " + tables + "gb (g int not null, s int not null, o int not null)");
            statement.execute("insert into " + tables + "gb values (200, 1, 5), (200, 5, 1)");
            statement.execute("create table " + tables + "gk (s int not null, o int not null)");
            statement.execute("insert into " + tables + "gk values (3, 4)");
            statement.execute("create table " + tables + "gc (s int not null, o int not null)");
            statement.execute("insert into " + tables + "gc values (6, 7)");
            statement.execute("create table " + tables + "ge (g int not null, s int not null, e varchar not null)");
            statement.execute(
                    "insert into " + tables + "ge values (100, 1, '" + base + "svc'), (101, 2, '" + base + "svc2')");
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
     * Solution of the variables, given as pairs of names and terms in the syntax of SPARQL.
     *
     * @param pairs names of the variables followed by their terms
     * @return solution of the variables
     */
    private static Map<String, String> solution(String... pairs)
    {
        Map<String, String> solution = new LinkedHashMap<>();

        for(int i = 0; i < pairs.length; i += 2)
            solution.put(pairs[i], pairs[i + 1]);

        return solution;
    }


    /**
     * Answers a query sent over the SPARQL protocol to the endpoint: the solutions of the endpoint compatible with the
     * VALUES row of the query, merged with it, in the XML results format; the endpoint {@code svcfail} fails.
     *
     * @param name name of the endpoint
     * @param exchange the request
     * @throws IOException on communication errors
     */
    private static void answer(String name, HttpExchange exchange) throws IOException
    {
        String body = new String(exchange.getRequestBody().readAllBytes(), UTF_8);
        String query = URLDecoder.decode(body.replaceFirst("^query=", ""), UTF_8);

        requests.computeIfAbsent(name, _ -> new AtomicInteger()).incrementAndGet();

        if(name.equals("svcfail"))
        {
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
            return;
        }

        List<String> variables = new ArrayList<>();
        List<String> values = new ArrayList<>();
        int start = query.lastIndexOf("values (");

        if(start >= 0)
        {
            int end = query.indexOf(')', start);

            for(String variable : query.substring(start + 8, end).trim().split("\\s+"))
                variables.add(variable.substring(1));

            Matcher matcher = termPattern.matcher(query.substring(query.indexOf("{(", end) + 2));

            while(values.size() < variables.size() && matcher.find())
                values.add(matcher.group());
        }

        Set<String> head = new LinkedHashSet<>(variables);

        for(Map<String, String> solution : answers.get(name))
            head.addAll(solution.keySet());

        StringBuilder builder = new StringBuilder();
        builder.append("<?xml version=\"1.0\"?><sparql xmlns=\"http://www.w3.org/2005/sparql-results#\"><head>");

        for(String variable : head)
            builder.append("<variable name=\"").append(variable).append("\"/>");

        builder.append("</head><results>");

        for(Map<String, String> solution : answers.get(name))
        {
            Map<String, String> merged = new LinkedHashMap<>(solution);
            boolean compatible = true;

            for(int i = 0; i < variables.size(); i++)
            {
                String value = values.get(i);
                String own = solution.get(variables.get(i));

                if(value.equals("undef"))
                    continue;
                else if(own == null)
                    merged.put(variables.get(i), value);
                else if(!own.equals(value))
                    compatible = false;
            }

            if(!compatible)
                continue;

            builder.append("<result>");

            for(Map.Entry<String, String> entry : merged.entrySet())
            {
                String term = entry.getValue();

                builder.append("<binding name=\"").append(entry.getKey()).append("\">");

                if(term.startsWith("<"))
                    builder.append("<uri>").append(term, 1, term.length() - 1).append("</uri>");
                else
                    builder.append("<literal>").append(term, 1, term.length() - 1).append("</literal>");

                builder.append("</binding>");
            }

            builder.append("</result>");
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
     * Configuration mapping the test tables.
     *
     * @param settings default settings of the remote endpoints
     * @param joinOptimizer whether the join optimizer is enabled
     * @return configuration mapping the test tables
     * @throws SQLException on database errors
     */
    private static SparqlDatabaseConfiguration configuration(ServiceSettings settings, boolean joinOptimizer)
            throws SQLException
    {
        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);
        config.addPrefix("ex", "http://example.org/");
        config.addIriClass(new IntegerUserIriClass("a", INT4, "http://example.org/a/"));
        config.addIriClass(new IntegerUserIriClass("b", INT4, "http://example.org/b/"));

        config.addQuadMapping(table("ga"), config.createIriMapping("a", "g"), config.createIriMapping("a", "s"),
                config.createIriMapping("ex:s"), config.createIriMapping("a", "o"));
        config.addQuadMapping(table("gb"), config.createIriMapping("b", "g"), config.createIriMapping("a", "s"),
                config.createIriMapping("ex:s"), config.createIriMapping("a", "o"));
        config.addQuadMapping(table("gk"), config.createIriMapping(g), config.createIriMapping("a", "s"),
                config.createIriMapping("ex:s"), config.createIriMapping("a", "o"));
        config.addQuadMapping(table("gc"), config.createIriMapping(a(102)), config.createIriMapping("a", "s"),
                config.createIriMapping("ex:s"), config.createIriMapping("a", "o"));
        config.addQuadMapping(table("ge"), config.createIriMapping("a", "g"), config.createIriMapping("a", "s"),
                config.createIriMapping("ex:ep"), config.createIriMapping(BuiltinClasses.iri, "e"));

        config.setServiceSettings(settings);
        config.setJoinOptimizer(joinOptimizer);

        return config;
    }


    /**
     * Test table of the given name.
     *
     * @param name the name of the table
     * @return test table of the given name
     */
    private static DatabaseTable table(String name)
    {
        return new DatabaseTable(schemaName, name);
    }


    /**
     * Rows of the query under the default settings, which have to be the same, asking the same requests, with the join
     * optimizer enabled; the requests are kept in {@link #counts}.
     *
     * @param query the query, without the prefix declarations; {@code <svc>} and the like name the endpoints
     * @return rows of the query
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws LimitExceedException if the generated SQL is too long
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static List<List<RdfTerm>> execute(String query)
            throws SQLException, TranslateExceptions, LimitExceedException, ServiceException
    {
        return execute(ServiceSettings.DEFAULT, query);
    }


    /**
     * Rows of the query under the settings, which have to be the same, asking the same requests, with the join
     * optimizer enabled; the requests are kept in {@link #counts}.
     *
     * @param settings default settings of the remote endpoints
     * @param query the query, without the prefix declarations; {@code <svc>} and the like name the endpoints
     * @return rows of the query
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws LimitExceedException if the generated SQL is too long
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static List<List<RdfTerm>> execute(ServiceSettings settings, String query)
            throws SQLException, TranslateExceptions, LimitExceedException, ServiceException
    {
        List<List<RdfTerm>> rows = execute(configuration(settings, false), query);
        Map<String, Integer> plain = counts;

        assertThat(execute(configuration(settings, true), query), containsInAnyOrder(rows.toArray()));
        assertThat(counts, is(plain));

        return rows;
    }


    /**
     * Rows of the query under the configuration; the requests the endpoints receive are kept in {@link #counts}.
     *
     * @param config the configuration
     * @param query the query, without the prefix declarations; {@code <svc>} and the like name the endpoints
     * @return rows of the query
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws LimitExceedException if the generated SQL is too long
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static List<List<RdfTerm>> execute(SparqlDatabaseConfiguration config, String query)
            throws SQLException, TranslateExceptions, LimitExceedException, ServiceException
    {
        String text = prefix + query.replaceAll("<(svc\\w*)>", "<" + base + "$1>");

        requests.clear();

        try(Request request = new Engine(config).getRequest(); Result result = request.execute(text))
        {
            List<List<RdfTerm>> rows = new ArrayList<>();

            while(result.next())
                rows.add(Arrays.asList(result.getRow()));

            return rows;
        }
        finally
        {
            Map<String, Integer> received = new HashMap<>();

            for(Map.Entry<String, AtomicInteger> entry : requests.entrySet())
                received.put(entry.getKey(), entry.getValue().get());

            counts = received;
        }
    }


    /**
     * Requests the endpoint received during the last execution.
     *
     * @param name name of the endpoint
     * @return requests the endpoint received during the last execution
     */
    private static int requests(String name)
    {
        return counts.getOrDefault(name, 0);
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
     * IRI of the class {@code a}.
     *
     * @param id the identifier
     * @return IRI of the class {@code a}
     */
    private static Iri a(int id)
    {
        return new Iri("http://example.org/a/" + id);
    }


    /**
     * IRI of the class {@code b}.
     *
     * @param id the identifier
     * @return IRI of the class {@code b}
     */
    private static Iri b(int id)
    {
        return new Iri("http://example.org/b/" + id);
    }


    /**
     * IRI of the endpoint.
     *
     * @param name name of the endpoint
     * @return IRI of the endpoint
     */
    private static Iri endpoint(String name)
    {
        return new Iri(base + name);
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
    @DisplayName("a context referring to the graph of the current solution is evaluated in every graph")
    void context() throws Exception
    {
        assertThat(
                execute("SELECT ?g ?x ?y WHERE { GRAPH ?g { BIND(a:1 AS ?x) FILTER EXISTS { ?x ex:s ?w } "
                        + "SERVICE <svc> { ?x ex:p ?y } } }"),
                containsInAnyOrder(row(a(100), a(1), b(10)), row(b(200), a(1), b(10))));
        assertThat(requests("svc"), is(1));

        assertThat(
                execute("SELECT ?g ?x ?y ?z WHERE { GRAPH ?g { OPTIONAL { ?x ex:s ?z } "
                        + "SERVICE <svc> { ?x ex:p ?y } } }"),
                containsInAnyOrder(row(a(100), a(1), b(10), a(2)), row(b(200), a(1), b(10), a(5))));

        assertThat(
                execute("SELECT ?g ?x ?y ?z WHERE { GRAPH ?g { { BIND(a:1 AS ?x) } UNION { OPTIONAL { ?x ex:s ?z } "
                        + "SERVICE <svc> { ?x ex:p ?y } } } }"),
                containsInAnyOrder(row(a(100), a(1), b(10), a(2)), row(a(100), a(1), null, null),
                        row(a(101), a(1), null, null), row(a(102), a(1), null, null), row(b(200), a(1), b(10), a(5)),
                        row(b(200), a(1), null, null), row(g, a(1), null, null)));

        // without a reference to the graph, the context is evaluated once as before
        assertThat(
                execute("SELECT ?g ?x ?y WHERE { GRAPH ?g { BIND(a:1 AS ?x) OPTIONAL { SERVICE <svc> { "
                        + "?x ex:p ?y } } } }"),
                containsInAnyOrder(row(a(100), a(1), b(10)), row(a(101), a(1), b(10)), row(a(102), a(1), b(10)),
                        row(b(200), a(1), b(10)), row(g, a(1), b(10))));
        assertThat(requests("svc"), is(1));
    }


    @Test
    @DisplayName("a call in a sub-select and in a nested graph evaluated in every graph")
    void nested() throws Exception
    {
        assertThat(
                execute("SELECT ?g ?n WHERE { GRAPH ?g { SELECT (COUNT(*) AS ?n) WHERE { OPTIONAL { ?x ex:s ?z } "
                        + "SERVICE <svc> { ?x ex:p ?y } } } }"),
                containsInAnyOrder(row(a(100), integer(1)), row(a(101), integer(0)), row(a(102), integer(0)),
                        row(b(200), integer(1)), row(g, integer(0))));

        List<List<RdfTerm>> rows = new ArrayList<>();

        for(Iri graph : List.of(a(100), a(101), a(102), b(200), g))
        {
            rows.add(row(graph, a(100), a(1), b(10), a(2)));
            rows.add(row(graph, b(200), a(1), b(10), a(5)));
        }

        assertThat(
                execute("SELECT ?g ?h ?x ?y ?z WHERE { GRAPH ?g { BIND(a:1 AS ?x) GRAPH ?h { "
                        + "OPTIONAL { ?x ex:s ?z } SERVICE <svc> { ?x ex:p ?y } } } }"),
                containsInAnyOrder(rows.toArray()));
    }


    @Test
    @DisplayName("SERVICE ?ep, a failing SERVICE SILENT and a call binding the graph variable in every graph")
    void endpoints() throws Exception
    {
        assertThat(
                execute("SELECT ?g ?ep ?x ?y WHERE { GRAPH ?g { VALUES ?ep { <svc> <svc2> } "
                        + "FILTER EXISTS { ?s ex:ep ?ep } SERVICE ?ep { ?x ex:p ?y } } }"),
                containsInAnyOrder(row(a(100), endpoint("svc"), a(1), b(10)), row(a(100), endpoint("svc"), b(20), lit),
                        row(a(101), endpoint("svc2"), a(2), null), row(a(101), endpoint("svc2"), a(5), null),
                        row(a(101), endpoint("svc2"), null, null)));

        assertThat(
                execute("SELECT ?g ?x ?y ?z WHERE { GRAPH ?g { OPTIONAL { ?x ex:s ?z } "
                        + "SERVICE SILENT <svcfail> { ?x ex:p ?y } } }"),
                containsInAnyOrder(row(a(100), a(1), null, a(2)), row(a(100), a(2), null, a(3)),
                        row(a(101), a(2), null, a(3)), row(a(101), a(3), null, a(101)), row(a(102), a(6), null, a(7)),
                        row(b(200), a(1), null, a(5)), row(b(200), a(5), null, a(1)), row(g, a(3), null, a(4))));
        assertThat(requests("svcfail"), is(1));

        assertThat(
                execute("SELECT ?g ?v ?x ?z WHERE { GRAPH ?g { OPTIONAL { ?x ex:s ?z } "
                        + "SERVICE <svcg> { ?g ex:p ?v } } }"),
                containsInAnyOrder(row(a(100), ok, a(1), a(2)), row(a(100), ok, a(2), a(3))));
    }


    @Test
    @DisplayName("the context limit counts the context solutions of all graphs")
    void contextLimit() throws Exception
    {
        String query = "SELECT ?g ?x ?y ?z WHERE { GRAPH ?g { OPTIONAL { ?x ex:s ?z } SERVICE <svc> { ?x ex:p ?y } } }";

        assertThat(execute(ServiceSettings.DEFAULT.withContextLimit(8), query),
                containsInAnyOrder(row(a(100), a(1), b(10), a(2)), row(b(200), a(1), b(10), a(5))));

        ServiceException e = assertThrows(ServiceException.class,
                () -> execute(ServiceSettings.DEFAULT.withContextLimit(7), query));

        assertThat(e.getMessage(), is(MessageType.serviceContextLimitExceeded.getText()));
    }


    @Test
    @DisplayName("copies of a call split for differently restricted parents do not share their results")
    void restrictions() throws Exception
    {
        assertThat(
                execute("SELECT ?x ?y WHERE { { VALUES ?x { a:1 } } UNION { VALUES ?x { b:20 } } "
                        + "OPTIONAL { SERVICE <svc> { ?x ex:p ?y } } }"),
                containsInAnyOrder(row(a(1), b(10)), row(b(20), lit)));

        assertThat(
                execute("SELECT ?g ?q ?w ?x ?y ?z WHERE { GRAPH ?g { ?q ex:s ?w "
                        + "OPTIONAL { ?x ex:s ?z SERVICE <svc> { ?x ex:p ?y } } } }"),
                containsInAnyOrder(row(a(100), a(1), a(2), a(1), b(10), a(2)),
                        row(a(100), a(2), a(3), a(1), b(10), a(2)), row(a(101), a(2), a(3), null, null, null),
                        row(a(101), a(3), a(101), null, null, null), row(a(102), a(6), a(7), null, null, null),
                        row(b(200), a(1), a(5), a(1), b(10), a(5)), row(b(200), a(5), a(1), a(1), b(10), a(5)),
                        row(g, a(3), a(4), null, null, null)));
    }
}
