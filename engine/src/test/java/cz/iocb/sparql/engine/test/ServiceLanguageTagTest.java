package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.rdfLangString;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import cz.iocb.sparql.engine.mapping.classes.IntegerUserIriClass;
import cz.iocb.sparql.engine.rdf.LangStringLiteral;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.TypedLiteral;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.LimitExceedException;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.translator.ServiceException;
import cz.iocb.sparql.testing.Database;



/**
 * Language tags of the terms stored for a SERVICE call. The tag of a term becomes a part of the name of its class, and
 * so of the names of the columns the term is stored in (those of a VALUES list for a small result, those of a temporary
 * table for a large one), and the SQL converting the term writes it as a constant; whatever characters it holds, it
 * must stay a mere identifier or a mere string there. A literal received from the endpoint has to carry a tag written
 * as the grammar of SPARQL writes one, otherwise the answer is rejected, and an empty tag stands for none. A tag stored
 * in the database is not checked, and it reaches the stored result with the context solutions of the call. The endpoint
 * is a server on the loopback interface answering every query with the number of solutions and the language tag given
 * in its URL.
 */
public class ServiceLanguageTagTest
{
    /**
     * Prefix declaration prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> ";

    /**
     * Number of the solutions a call receives for a large result: the temporary table is created while the solutions of
     * the second of the three calls arrive, so the columns of the third one are added to it.
     */
    private static final int largeSize = 600;

    /**
     * Language tag of the second item, holding both kinds of quotes.
     */
    private static final String secondTag = "a\"b'c";

    /**
     * Language tag of the third item, holding both kinds of quotes.
     */
    private static final String thirdTag = "d'e\"f";

    /**
     * Names of the variables of a query.
     */
    private static final Pattern variablePattern = Pattern.compile("\\?([A-Za-z0-9_]+)");

    /**
     * The fake endpoint.
     */
    private static HttpServer server = null;

    /**
     * URL of the fake endpoint, without the parameters telling its answer.
     */
    private static String endpoint = null;

    /**
     * Engine over the configuration mapping the labels of the items.
     */
    private static Engine engine = null;


    /**
     * Starts the fake endpoint and creates the {@code service_tag_test} schema with three items, whose labels have the
     * language tags {@code en}, {@code a"b'c} and {@code d'e"f}, and the configuration mapping them.
     *
     * @throws SQLException on database errors
     * @throws IOException if the endpoint cannot be started
     */
    @BeforeAll
    static void init() throws SQLException, IOException
    {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/sparql", ServiceLanguageTagTest::answer);
        server.start();

        endpoint = "http://127.0.0.1:" + server.getAddress().getPort() + "/sparql";

        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema service_tag_test");
            statement.execute("create table service_tag_test.item (id int not null primary key, "
                    + "label varchar collate \"C\" not null, lang varchar collate \"C\" not null)");
            statement.execute("insert into service_tag_test.item values (1, 'one', 'en'), "
                    + "(2, 'two', 'a\"b''c'), (3, 'three', 'd''e\"f')");
        }

        DatabaseSchema schema = new DatabaseSchema(connectionPool);

        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, connectionPool, schema, false);
        config.addPrefix("ex", "http://example.org/");
        config.addIriClass(new IntegerUserIriClass("item", INT4, "http://example.org/item/"));

        config.addQuadMapping(new DatabaseTable("service_tag_test", "item"), null,
                config.createIriMapping("item", "id"), config.createIriMapping("ex:label"),
                config.createLiteralMapping(rdfLangString, "label", "lang"));

        engine = new Engine(config);
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
     * Answers a query sent over the SPARQL protocol in the XML results format: the last variable of the query is bound
     * to as many literals as the parameter {@code rows} of the URL says, each with the {@code xml:lang} attribute given
     * by the parameter {@code lang}, or without the attribute when the URL has no such parameter.
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

        Map<String, String> parameters = new HashMap<>();

        for(String parameter : exchange.getRequestURI().getRawQuery().split("&"))
        {
            String[] parts = parameter.split("=", 2);
            parameters.put(parts[0], URLDecoder.decode(parts[1], UTF_8));
        }

        String lang = parameters.get("lang");
        String attribute = lang == null ? "" :
                " xml:lang=\"" + lang.replace("&", "&amp;").replace("<", "&lt;").replace("\"", "&quot;") + "\"";

        StringBuilder builder = new StringBuilder();
        builder.append("<?xml version=\"1.0\"?><sparql xmlns=\"http://www.w3.org/2005/sparql-results#\"><head>");

        for(String variable : variables)
            builder.append("<variable name=\"").append(variable).append("\"/>");

        builder.append("</head><results>");

        for(int i = 0; i < Integer.parseInt(parameters.get("rows")); i++)
        {
            builder.append("<result><binding name=\"").append(variables.getLast()).append("\"><literal");
            builder.append(attribute).append(">v").append(i).append("</literal></binding></result>");
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
     * Call of the fake endpoint with the pattern {@code ?s ex:p ?o}, which binds {@code ?o} to the given number of
     * literals with the given language tag.
     *
     * @param rows the number of the received solutions
     * @param lang the language tag of the received literals, or null for literals without a tag
     * @return call of the fake endpoint
     */
    private static String service(int rows, String lang)
    {
        String parameters = "?rows=" + rows + (lang != null ? "&lang=" + URLEncoder.encode(lang, UTF_8) : "");

        return "SERVICE <" + endpoint + parameters + "> { ?s ex:p ?o } ";
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
     * Language-tagged string.
     *
     * @param value the lexical form
     * @param tag the language tag
     * @return language-tagged string
     */
    private static LangStringLiteral lang(String value, String tag)
    {
        return new LangStringLiteral(value, tag);
    }


    /**
     * String literal.
     *
     * @param value the lexical form
     * @return string literal
     */
    private static TypedLiteral string(String value)
    {
        return new TypedLiteral(value, xsdStringIri);
    }


    /**
     * Integer literal.
     *
     * @param value the value
     * @return integer literal
     */
    private static TypedLiteral integer(int value)
    {
        return new TypedLiteral(Integer.toString(value), xsdIntegerIri);
    }


    @Test
    @DisplayName("a received literal keeps its language tag, in lower case")
    void receivedTag() throws Exception
    {
        assertThat(execute("SELECT ?o (LANG(?o) AS ?t) WHERE { " + service(1, "en-US") + "}"),
                contains(row(lang("v0", "en-us"), string("en-us"))));

        // not well-formed by BCP 47, but a tag a query can write as well
        assertThat(execute("SELECT ?o (LANG(?o) AS ?t) WHERE { " + service(1, "en-toolongsubtag") + "}"),
                contains(row(lang("v0", "en-toolongsubtag"), string("en-toolongsubtag"))));
    }


    @Test
    @DisplayName("a received literal with an empty language tag is a plain string")
    void emptyTag() throws Exception
    {
        assertThat(execute("SELECT ?o (LANG(?o) AS ?t) WHERE { " + service(1, "") + "}"),
                contains(row(string("v0"), string(""))));
    }


    @Test
    @DisplayName("an answer with a language tag the grammar cannot write is rejected")
    void invalidTag()
    {
        for(String tag : List.of("en us", "en_US", "en--ltr", "-en", "a\"b", "a'b"))
            assertThrows(ServiceException.class, () -> execute("SELECT ?o WHERE { " + service(1, tag) + "}"), tag);
    }


    @Test
    @DisplayName("a stored language tag with quotes is only a name and a string in the SQL of a small result")
    void storedTagSmallResult() throws Exception
    {
        assertThat(execute("SELECT ?l (LANG(?l) AS ?t) ?o WHERE { ?s ex:label ?l " + service(1, null) + "}"),
                containsInAnyOrder(row(lang("one", "en"), string("en"), string("v0")),
                        row(lang("two", secondTag), string(secondTag), string("v0")),
                        row(lang("three", thirdTag), string(thirdTag), string("v0"))));
    }


    @Test
    @DisplayName("a stored language tag with quotes is only a name and a string in the SQL of a large result")
    void storedTagLargeResult() throws Exception
    {
        assertThat(
                execute("SELECT ?l (LANG(?l) AS ?t) (COUNT(*) AS ?c) WHERE { ?s ex:label ?l " + service(largeSize, null)
                        + "} GROUP BY ?l"),
                containsInAnyOrder(row(lang("one", "en"), string("en"), integer(largeSize)),
                        row(lang("two", secondTag), string(secondTag), integer(largeSize)),
                        row(lang("three", thirdTag), string(thirdTag), integer(largeSize))));
    }


    @Test
    @DisplayName("a stored language tag with quotes is only a string in a comparison with the tag of another term")
    void storedTagComparison() throws Exception
    {
        // the label of a single item is stored in the class of its tag
        String item = "VALUES ?s { <http://example.org/item/2> } ?s ex:label ?l " + service(1, null);

        assertThat(execute("SELECT ?l ?m WHERE { " + item + "?t ex:label ?m FILTER(STRSTARTS(?l, ?m)) }"),
                contains(row(lang("two", secondTag), lang("two", secondTag))));

        assertThat(execute("SELECT ?l ?m WHERE { " + item + "?t ex:label ?m FILTER(STRSTARTS(?m, ?l)) }"),
                contains(row(lang("two", secondTag), lang("two", secondTag))));
    }
}
