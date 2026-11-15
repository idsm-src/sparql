package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdBooleanType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdByteType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDateTimeType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDateType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDayTimeDurationType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDecimalType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDoubleType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdFloatType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdLongType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdNegativeIntegerType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdNonNegativeIntegerType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdNonPositiveIntegerType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdPositiveIntegerType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdShortType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdUnsignedByteType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdUnsignedIntType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdUnsignedLongType;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdUnsignedShortType;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.graph.NodeFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.mapping.classes.IntegerUserIriClass;
import cz.iocb.sparql.engine.mapping.datatypes.Datatype;
import cz.iocb.sparql.engine.rdf.Literal;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.TypedLiteral;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.test.SparqlTest.Quad;
import cz.iocb.sparql.testing.Database;



/**
 * The canonical lexical forms of the literals of every datatype that has a constructor function. A cast gives the
 * literal of the cast value in the canonical form, also when the operand has the datatype already, whichever class
 * holds the operand: a constant, a literal stored in the classes of a {@link TypedQuadTable} variant or in a box. That
 * form is the one the datatype gives for the lexical form of the operand ({@link Datatype#getCanonicalLexicalForm}) and
 * the one the database prints for the cast value (STR), so the engine and the extension agree on the canonical forms. A
 * literal received from a remote SERVICE endpoint is brought to that form as well; the endpoint is a server on the
 * loopback interface answering every query with the literals of the datatype given in its URL.
 */
public class CanonicalFormTest
{
    /**
     * A datatype with valid lexical forms of its literals, canonical and others.
     *
     * @param datatype the datatype
     * @param forms the lexical forms
     */
    private record Forms(Datatype datatype, List<String> forms)
    {
        /**
         * The local name of the datatype, which names the tests.
         */
        @Override
        public String toString()
        {
            return datatype.getTypeIri().getValue().replaceFirst(".*#", "");
        }
    }


    /**
     * Predicate of the stored triples.
     */
    private static final String predicate = "http://example.org/p";

    /**
     * The datatypes with the lexical forms that are cast.
     */
    private static final List<Forms> datatypes = List.of(
            new Forms(xsdBooleanType, List.of("true", "false", "1", "0", " true ", " 0")),
            new Forms(xsdByteType, List.of("7", "+7", "07", "-07")),
            new Forms(xsdUnsignedByteType, List.of("7", "07", "+7")),
            new Forms(xsdShortType, List.of("7", "+7", "-007")), new Forms(xsdUnsignedShortType, List.of("7", "007")),
            new Forms(xsdIntType, List.of("7", "+7", "-07")), new Forms(xsdUnsignedIntType, List.of("7", "07")),
            new Forms(xsdLongType, List.of("7", "007", "+7")), new Forms(xsdUnsignedLongType, List.of("7", "07")),
            new Forms(xsdIntegerType, List.of("1", "01", "+1", "-01", "-0", "+0", " 5 ", "000")),
            new Forms(xsdNonPositiveIntegerType, List.of("-1", "-01", "0", "-0")),
            new Forms(xsdNegativeIntegerType, List.of("-1", "-01")),
            new Forms(xsdNonNegativeIntegerType, List.of("1", "+1", "01")),
            new Forms(xsdPositiveIntegerType, List.of("1", "+1", "01")),
            new Forms(xsdDecimalType, List.of("1.5", "1.50", "+2.0", "01.5", "3", ".5", "5.", "-0.0", "0", " 1.5 ")),
            new Forms(xsdFloatType,
                    List.of("1.0E0", "1.0", "100", "1e0", "NaN", "INF", "-INF", "-0", "0", ".5", "5.", "1.5e3", "+1",
                            " 1 ")),
            new Forms(xsdDoubleType,
                    List.of("1.0E0", "1.0", "100", "1e0", "NaN", "INF", "-INF", "-0", "0", ".5", "5.", "1.5e3", "+1",
                            " 1 ", "1.0E00", "0.1e1")),
            new Forms(xsdDateTimeType,
                    List.of("2002-10-10T17:00:00Z", "2002-10-10T17:00:00.000Z", "2002-10-10T17:00:00+00:00",
                            "2002-10-10T17:00:00", "2002-10-10T17:00:00.0", "2002-10-10T12:00:00-05:00")),
            new Forms(xsdDateType, List.of("2002-10-10Z", "2002-10-10+00:00", "2002-10-10", "2002-10-10-05:00")),
            new Forms(xsdDayTimeDurationType,
                    List.of("PT1M", "PT60S", "P0DT1M", "PT1M0S", " PT1M ", "PT0S", "-PT0S", "P1D", "PT24H", "PT30S",
                            "-PT90S", "PT0.50S", "P1DT1H1M1.000001S", "P106751991DT4H54.775807S",
                            "-P106751991DT4H54.775808S")),
            new Forms(xsdStringType, List.of("abc", " a  b ")));

    /**
     * Names of the variables of a query.
     */
    private static final Pattern variablePattern = Pattern.compile("\\?([A-Za-z0-9_]+)");

    /**
     * The fake endpoint.
     */
    private static HttpServer server = null;

    /**
     * URL of the fake endpoint, without the parameter telling its answer.
     */
    private static String endpoint = null;

    /**
     * Pool of the test database.
     */
    private static DataSource connectionPool = null;

    /**
     * Catalog of the test database, read after the table of the boxes was created.
     */
    private static DatabaseSchema schema = null;

    /**
     * Engine of the configuration mapping the table of the boxes.
     */
    private static Engine engine = null;


    /**
     * Starts the fake endpoint and creates the {@code canonical_form_test} schema with the table {@code literal}
     * holding every lexical form of every datatype in a box, and the configuration mapping its rows as the triples of
     * the items, the predicate and the literals.
     *
     * @throws SQLException on database errors
     * @throws IOException if the endpoint cannot be started
     */
    @BeforeAll
    static void init() throws SQLException, IOException
    {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/sparql", CanonicalFormTest::answer);
        server.start();

        endpoint = "http://127.0.0.1:" + server.getAddress().getPort() + "/sparql";

        connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema canonical_form_test");
            statement.execute(
                    "create table canonical_form_test.literal (id int primary key, value sparql.rdfbox not null)");

            int id = 0;

            for(Forms datatype : datatypes)
                for(String form : datatype.forms())
                    statement.execute("insert into canonical_form_test.literal values (" + id++ + ", '\"" + form
                            + "\"^^<" + datatype.datatype().getTypeIri().getValue() + ">')");
        }

        schema = new DatabaseSchema(connectionPool);

        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);
        config.addIriClass(new IntegerUserIriClass("item", INT4, "http://example.org/item/"));
        config.addQuadMapping(new DatabaseTable("canonical_form_test", "literal"),
                config.createIriMapping("<http://example.org/graph>"), config.createIriMapping("item", "id"),
                config.createIriMapping("<" + predicate + ">"), config.createTermMapping(box, "value"));

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
     * Answers a query sent over the SPARQL protocol in the XML results format with the literals of the datatype whose
     * position in the list of the datatypes the parameter {@code datatype} of the URL gives, as an endpoint keeping its
     * literals as they were written returns them: the first variable of the query is bound to the position of a lexical
     * form among the forms of the datatype, the second one to the literal of that form.
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

        String parameter = exchange.getRequestURI().getRawQuery().replaceFirst("^datatype=", "");
        Forms datatype = datatypes.get(Integer.parseInt(parameter));

        StringBuilder builder = new StringBuilder();
        builder.append("<?xml version=\"1.0\"?><sparql xmlns=\"http://www.w3.org/2005/sparql-results#\"><head>");

        for(String variable : variables)
            builder.append("<variable name=\"").append(variable).append("\"/>");

        builder.append("</head><results>");

        for(int i = 0; i < datatype.forms().size(); i++)
        {
            builder.append("<result><binding name=\"").append(variables.get(0)).append("\"><literal datatype=\"");
            builder.append(xsdIntegerIri.getValue()).append("\">").append(i).append("</literal></binding>");
            builder.append("<binding name=\"").append(variables.get(1)).append("\"><literal datatype=\"");
            builder.append(datatype.datatype().getTypeIri().getValue()).append("\">");
            builder.append(datatype.forms().get(i)).append("</literal></binding></result>");
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
     * The datatypes with their lexical forms.
     *
     * @return the datatypes with their lexical forms
     */
    static List<Forms> getDatatypes()
    {
        return datatypes;
    }


    /**
     * The datatypes with their lexical forms combined with the choices of the classes of the literals and of the
     * temporal literals that the {@link TypedQuadTable} variants make.
     *
     * @return the datatypes combined with the variants
     */
    static Stream<Arguments> getStoredDatatypes()
    {
        List<TypedQuadTable.Variant> variants = TypedQuadTable.getVariants().stream()
                .filter(v -> v.blankNodes() == TypedQuadTable.BlankNodeMode.COMPOSITE).toList();

        return datatypes.stream().flatMap(d -> variants.stream().map(v -> Arguments.of(d, v)));
    }


    /**
     * A literal of the datatype as written in a query.
     *
     * @param datatype the datatype
     * @param form the lexical form
     * @return the literal as written in a query
     */
    private static String constant(Forms datatype, String form)
    {
        return "\"" + form + "\"^^<" + datatype.datatype().getTypeIri().getValue() + ">";
    }


    /**
     * The expressions casting the term to the datatype: the cast value and what the database prints for it.
     *
     * @param datatype the datatype
     * @param term the term, as written in a query
     * @return the two expressions
     */
    private static String casts(Forms datatype, String term)
    {
        String cast = "<" + datatype.datatype().getTypeIri().getValue() + ">(" + term + ")";

        return "(" + cast + " AS ?c) (STR(" + cast + ") AS ?s)";
    }


    /**
     * Rows of the query.
     *
     * @param request the request to run the query
     * @param query the query
     * @return rows of the query
     * @throws Exception if the query fails
     */
    private static List<List<RdfTerm>> execute(Request request, String query) throws Exception
    {
        List<List<RdfTerm>> rows = new ArrayList<>();

        try(Result result = request.execute(query))
        {
            while(result.next())
                rows.add(Arrays.asList(result.getRow()));
        }

        return rows;
    }


    /**
     * Checks the rows of a literal, its cast to the datatype and what the database prints for the cast value: there is
     * a row for every lexical form, and both casts have the canonical form the datatype gives for the literal.
     *
     * @param datatype the datatype
     * @param rows the rows
     */
    private static void check(Forms datatype, List<List<RdfTerm>> rows)
    {
        assertThat(rows.stream().map(r -> ((Literal) r.get(0)).getValue()).toList(),
                containsInAnyOrder(datatype.forms().toArray()));

        for(List<RdfTerm> row : rows)
        {
            String form = ((Literal) row.get(0)).getValue();
            String canonical = datatype.datatype().getCanonicalLexicalForm(form);

            assertEquals(new TypedLiteral(canonical, datatype.datatype().getTypeIri()), row.get(1), form);
            assertEquals(new TypedLiteral(canonical, xsdStringIri), row.get(2), form);
        }
    }


    @ParameterizedTest(name = "{0}")
    @MethodSource("getDatatypes")
    @DisplayName("a constant cast to its datatype has the canonical form")
    void constants(Forms datatype) throws Exception
    {
        try(Request request = engine.getRequest())
        {
            for(String form : datatype.forms())
            {
                String constant = constant(datatype, form);

                check(new Forms(datatype.datatype(), List.of(form)), execute(request,
                        "SELECT (" + constant + " AS ?v) " + casts(datatype, constant) + " WHERE { }"));
            }

            String values = datatype.forms().stream().map(f -> constant(datatype, f)).collect(joining(" "));

            check(datatype,
                    execute(request, "SELECT ?v " + casts(datatype, "?v") + " WHERE { VALUES ?v { " + values + " } }"));
        }
    }


    @ParameterizedTest(name = "{0}")
    @MethodSource("getDatatypes")
    @DisplayName("a literal in a box cast to its datatype has the canonical form")
    void boxed(Forms datatype) throws Exception
    {
        try(Request request = engine.getRequest())
        {
            check(datatype, execute(request, "SELECT ?o " + casts(datatype, "?o") + " WHERE { ?x <" + predicate
                    + "> ?o FILTER(DATATYPE(?o) = <" + datatype.datatype().getTypeIri().getValue() + ">) }"));
        }
    }


    @ParameterizedTest(name = "{0} in {1}")
    @MethodSource("getStoredDatatypes")
    @DisplayName("a literal in the classes of its datatype cast to the datatype has the canonical form")
    void stored(Forms datatype, TypedQuadTable.Variant variant) throws Exception
    {
        List<Quad> quads = new ArrayList<>();

        for(String form : datatype.forms())
            quads.add(new Quad(null, NodeFactory.createURI("http://example.org/s" + quads.size()),
                    NodeFactory.createURI(predicate), NodeFactory.createLiteralDT(form,
                            TypeMapper.getInstance().getSafeTypeByName(datatype.datatype().getTypeIri().getValue()))));

        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, connectionPool, schema, false);

        try(Request request = new Engine(config).getRequest())
        {
            TypedQuadTable.store(config, request, variant, quads);

            check(datatype,
                    execute(request, "SELECT ?o " + casts(datatype, "?o") + " WHERE { ?x <" + predicate + "> ?o }"));
        }
    }


    @ParameterizedTest(name = "{0}")
    @MethodSource("getDatatypes")
    @DisplayName("a literal received from a SERVICE endpoint has the canonical form")
    void received(Forms datatype) throws Exception
    {
        String service = "SERVICE <" + endpoint + "?datatype=" + datatypes.indexOf(datatype) + "> { ?i <" + predicate
                + "> ?o }";

        try(Request request = engine.getRequest())
        {
            List<List<RdfTerm>> rows = execute(request, "SELECT ?i ?o (STR(?o) AS ?s) WHERE { " + service + " }");

            assertEquals(datatype.forms().size(), rows.size());

            // the literal the endpoint gave and what the database prints for the value stored for it
            for(List<RdfTerm> row : rows)
            {
                String form = datatype.forms().get(Integer.parseInt(((Literal) row.get(0)).getValue()));
                String canonical = datatype.datatype().getCanonicalLexicalForm(form);

                assertEquals(new TypedLiteral(canonical, datatype.datatype().getTypeIri()), row.get(1), form);
                assertEquals(new TypedLiteral(canonical, xsdStringIri), row.get(2), form);
            }
        }
    }
}
