package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdBooleanIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDateIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDateTimeIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDayTimeDurationIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDecimalIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDoubleIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdFloatIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.mapping.ConstantIriMapping;
import cz.iocb.sparql.engine.mapping.classes.IntegerUserIriClass;
import cz.iocb.sparql.engine.rdf.BlankNode;
import cz.iocb.sparql.engine.rdf.DirLangStringLiteral;
import cz.iocb.sparql.engine.rdf.DirLangStringLiteral.Direction;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.LangStringLiteral;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.TripleTerm;
import cz.iocb.sparql.engine.rdf.TypedLiteral;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.LimitExceedException;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.translator.ServiceException;
import cz.iocb.sparql.testing.Database;



/**
 * ORDER BY over terms of every kind: the terms given by VALUES, ordered by the keys of their own classes, come in the
 * same order as the same terms read from a box column, ordered by the total order of the box, so that the order of a
 * variable does not depend on how its terms are stored. The kinds come as the box has them (IRIs, literals of unknown
 * datatypes, language-tagged strings with a base direction and without, strings, durations, dates, date-times,
 * numerics, booleans, triple terms), the strings compared by code points like in the box, and the IRIs by their text,
 * which an IRI class delivers through the columns of its identifiers. Numerics are ordered by their values also when
 * one class holds the values of several datatypes, like a value of IF with branches of two datatypes, a sum (an integer
 * when there is nothing to sum) or an average.
 */
public class TermOrderTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX : <http://example.org/> "
            + "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> ";

    /**
     * The terms as the text forms of the box, in no particular order.
     */
    private static final List<String> boxed = List.of("\"a\"@en", "\"2020-01-01T00:00:00Z\"^^<%sdateTime>",
            "\"x\"^^<http://example.org/unknown>", "\"1\"^^<%sinteger>", "\"a\"@en--ltr", "<http://example.org/i>",
            "<http://example.org/item/10>", "<http://example.org/item/2>", "\"PT2S\"^^<%sdayTimeDuration>",
            "\"true\"^^<%sboolean>", "\"a\"", "\"b\"@en",
            "<<( <http://example.org/s> <http://example.org/p> <http://example.org/o> )>>", "\"a\"@en--rtl",
            "\"2020-01-01\"^^<%sdate>", "\"1.5\"^^<%sdecimal>", "\"x\"^^<http://example.org/other>",
            "\"PT1S\"^^<%sdayTimeDuration>", "\"a\"@de");

    /**
     * The same terms as a VALUES clause.
     */
    private static final String values = """
            VALUES ?v { "a"@en "2020-01-01T00:00:00Z"^^xsd:dateTime \
            "x"^^<http://example.org/unknown> 1 "a"@en--ltr <http://example.org/i> \
            <http://example.org/item/10> <http://example.org/item/2> \
            "PT2S"^^xsd:dayTimeDuration true "a" "b"@en \
            <<( <http://example.org/s> <http://example.org/p> <http://example.org/o> )>> "a"@en--rtl \
            "2020-01-01"^^xsd:date 1.5 "x"^^<http://example.org/other> "PT1S"^^xsd:dayTimeDuration \
            "a"@de }\s""";

    /**
     * The terms in the order of the box.
     */
    private static final List<RdfTerm> ordered = List.of(iri("i"), iri("item/10"), iri("item/2"),
            new TypedLiteral("x", iri("other")), new TypedLiteral("x", iri("unknown")),
            new DirLangStringLiteral("a", "en", Direction.RTL), new DirLangStringLiteral("a", "en", Direction.LTR),
            new LangStringLiteral("a", "de"), new LangStringLiteral("a", "en"), new LangStringLiteral("b", "en"),
            new TypedLiteral("a", xsdStringIri), new TypedLiteral("PT1S", xsdDayTimeDurationIri),
            new TypedLiteral("PT2S", xsdDayTimeDurationIri), new TypedLiteral("2020-01-01", xsdDateIri),
            new TypedLiteral("2020-01-01T00:00:00Z", xsdDateTimeIri), new TypedLiteral("1", xsdIntegerIri),
            new TypedLiteral("1.5", xsdDecimalIri), new TypedLiteral("true", xsdBooleanIri),
            new TripleTerm(iri("s"), iri("p"), iri("o")));

    /**
     * Engine over the configuration mapping the box column.
     */
    private static Engine engine = null;


    /**
     * Creates the table {@code item} holding the terms in a box column and maps it as {@code :term}.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema term_order_test");
            statement.execute("create table term_order_test.item (id int primary key, term sparql.rdfbox not null)");

            for(int i = 0; i < boxed.size(); i++)
                statement.execute("insert into term_order_test.item values (" + i + ", '"
                        + boxed.get(i).replace("%s", "http://www.w3.org/2001/XMLSchema#") + "'::sparql.rdfbox)");
        }

        DatabaseSchema schema = new DatabaseSchema(connectionPool);

        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);
        config.addPrefix("", "http://example.org/");
        config.addIriClass(new IntegerUserIriClass("item", INT4, "http://example.org/item/"));

        DatabaseTable table = new DatabaseTable("term_order_test", "item");
        ConstantIriMapping graph = config.createIriMapping("<http://example.org/graph>");

        config.addQuadMapping(table, graph, config.createIriMapping("<http://example.org/item>"),
                config.createIriMapping(":term"), config.createTermMapping(box, "term"));

        engine = new Engine(config);
    }


    /**
     * Values of {@code ?v} in the order the query returns them.
     *
     * @param query the query, without the prefix declarations
     * @return values of {@code ?v} in the order the query returns them
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws LimitExceedException if the generated SQL is too long
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static List<RdfTerm> execute(String query)
            throws SQLException, TranslateExceptions, LimitExceedException, ServiceException
    {
        try(Request request = engine.getRequest(); Result result = request.execute(prefix + query))
        {
            List<RdfTerm> terms = new ArrayList<>();

            while(result.next())
                terms.add(result.getRow()[0]);

            return terms;
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
     * The groups as the xsd:integer literals identifying them.
     *
     * @param groups the numbers of the groups
     * @return the xsd:integer literals of the numbers
     */
    private static List<RdfTerm> groups(int... groups)
    {
        return IntStream.of(groups).<RdfTerm> mapToObj(g -> new TypedLiteral(Integer.toString(g), xsdIntegerIri))
                .toList();
    }


    @Test
    @DisplayName("terms in their own classes are ordered as the box orders them")
    void nativeOrder() throws Exception
    {
        assertThat(execute("SELECT ?v WHERE { " + values + "} ORDER BY ?v"), contains(ordered.toArray()));
        assertThat(execute("SELECT ?v WHERE { " + values + "} ORDER BY DESC(?v)"),
                contains(ordered.reversed().toArray()));
    }


    @Test
    @DisplayName("terms read from a box column are ordered the same way")
    void boxedOrder() throws Exception
    {
        assertThat(execute("SELECT ?v WHERE { ?s :term ?v } ORDER BY ?v"), contains(ordered.toArray()));
        assertThat(execute("SELECT ?v WHERE { ?s :term ?v } ORDER BY DESC(?v)"),
                contains(ordered.reversed().toArray()));
    }


    @Test
    @DisplayName("IRIs of one class are ordered as their text through the columns of the class")
    void irisOfOneClass() throws Exception
    {
        String values = "VALUES ?v { <http://example.org/item/2> <http://example.org/item/10> "
                + "<http://example.org/item/3> } ";

        assertThat(execute("SELECT ?v WHERE { " + values + "} ORDER BY ?v"),
                contains(iri("item/10"), iri("item/2"), iri("item/3")));
        assertThat(execute("SELECT ?v WHERE { " + values + "} ORDER BY DESC(?v)"),
                contains(iri("item/3"), iri("item/2"), iri("item/10")));
    }


    @Test
    @DisplayName("numerics of integral and floating point types are ordered by their values")
    void mixedNumerics() throws Exception
    {
        assertThat(execute("SELECT ?v WHERE { VALUES ?v { 1 1.5e0 2 0.5 } } ORDER BY ?v"),
                contains(new TypedLiteral("0.5", xsdDecimalIri), new TypedLiteral("1", xsdIntegerIri),
                        new TypedLiteral("1.5e0", xsdDoubleIri), new TypedLiteral("2", xsdIntegerIri)));
    }


    @Test
    @DisplayName("numerics of one class holding several datatypes are ordered by their values")
    void numericsOfOneClass() throws Exception
    {
        // the branches of different datatypes make the class of the value of IF span both
        String pattern = "VALUES (?c ?a ?b) { %s } BIND(IF(?c, ?a, ?b) AS ?v) ";

        String doubles = pattern.formatted("(true 1 2.5e0) (false 4 0.5e0) (true 3 1.0e0) (false 0 2.0e0)");
        List<RdfTerm> orderedDoubles = List.of(new TypedLiteral("0.5e0", xsdDoubleIri),
                new TypedLiteral("1", xsdIntegerIri), new TypedLiteral("2.0e0", xsdDoubleIri),
                new TypedLiteral("3", xsdIntegerIri));

        assertThat(execute("SELECT ?v WHERE { " + doubles + "} ORDER BY ?v"), contains(orderedDoubles.toArray()));
        assertThat(execute("SELECT ?v WHERE { " + doubles + "} ORDER BY DESC(?v)"),
                contains(orderedDoubles.reversed().toArray()));
        assertThat(execute("SELECT (IF(?c, ?a, ?b) AS ?e) WHERE { " + doubles + "} ORDER BY IF(?c, ?a, ?b)"),
                contains(orderedDoubles.toArray()));

        String decimals = pattern.formatted("(true 1 2.5) (false 4 0.5) (true 3 1.0) (false 0 2.0)");
        List<RdfTerm> orderedDecimals = List.of(new TypedLiteral("0.5", xsdDecimalIri),
                new TypedLiteral("1", xsdIntegerIri), new TypedLiteral("2.0", xsdDecimalIri),
                new TypedLiteral("3", xsdIntegerIri));

        assertThat(execute("SELECT ?v WHERE { " + decimals + "} ORDER BY ?v"), contains(orderedDecimals.toArray()));
        assertThat(execute("SELECT ?v WHERE { " + decimals + "} ORDER BY DESC(?v)"),
                contains(orderedDecimals.reversed().toArray()));

        String floats = pattern.formatted("(true '3.5'^^xsd:float 2.5e0) (false '0.5'^^xsd:float 1.5e0) "
                + "(true '1'^^xsd:float 9.0e0) (false '7'^^xsd:float 4.5e0)");
        List<RdfTerm> orderedFloats = List.of(new TypedLiteral("1", xsdFloatIri),
                new TypedLiteral("1.5e0", xsdDoubleIri), new TypedLiteral("3.5", xsdFloatIri),
                new TypedLiteral("4.5e0", xsdDoubleIri));

        assertThat(execute("SELECT ?v WHERE { " + floats + "} ORDER BY ?v"), contains(orderedFloats.toArray()));
        assertThat(execute("SELECT ?v WHERE { " + floats + "} ORDER BY DESC(?v)"),
                contains(orderedFloats.reversed().toArray()));
    }


    @Test
    @DisplayName("sums and averages are ordered by their values")
    void aggregates() throws Exception
    {
        // the groups 1 to 4 have the sums 4.0, 0.5, 11.25 and 3.0 and the averages 2.0, 0.5, 5.625 and 3.0
        List<RdfTerm> bySum = groups(2, 4, 1, 3);
        List<RdfTerm> byAverage = groups(2, 1, 4, 3);

        for(String values : List.of("VALUES (?g ?x) { (1 1.5) (1 2.5) (2 0.5) (3 10.25) (3 1.0) (4 3.0) } ",
                "VALUES (?g ?x) { (1 1.5e0) (1 2.5e0) (2 0.5e0) (3 10.25e0) (3 1.0e0) (4 3.0e0) } ",
                "VALUES (?g ?x) { (1 '1.5'^^xsd:float) (1 '2.5'^^xsd:float) (2 '0.5'^^xsd:float) "
                        + "(3 '10.25'^^xsd:float) (3 '1'^^xsd:float) (4 '3'^^xsd:float) } "))
        {
            assertThat(execute("SELECT ?g (SUM(?x) AS ?v) WHERE { " + values + "} GROUP BY ?g ORDER BY ?v"),
                    contains(bySum.toArray()));
            assertThat(execute("SELECT ?g WHERE { " + values + "} GROUP BY ?g ORDER BY DESC(SUM(?x))"),
                    contains(bySum.reversed().toArray()));
            assertThat(execute("SELECT ?g (AVG(?x) AS ?v) WHERE { " + values + "} GROUP BY ?g ORDER BY ?v"),
                    contains(byAverage.toArray()));
            assertThat(execute("SELECT ?g WHERE { " + values + "} GROUP BY ?g ORDER BY DESC(AVG(?x))"),
                    contains(byAverage.reversed().toArray()));
        }

        // the groups 1 to 4 have the averages 1.5, 5.0, 5.5 and 3.0, which are decimals
        String integers = "VALUES (?g ?x) { (1 1) (1 2) (2 5) (3 10) (3 1) (4 3) } ";

        assertThat(execute("SELECT ?g (AVG(?x) AS ?v) WHERE { " + integers + "} GROUP BY ?g ORDER BY ?v"),
                contains(groups(1, 4, 2, 3).toArray()));
        assertThat(execute("SELECT ?g WHERE { " + integers + "} GROUP BY ?g ORDER BY DESC(AVG(?x))"),
                contains(groups(3, 2, 4, 1).toArray()));
    }


    @Test
    @DisplayName("blank nodes come before the other terms")
    void blankNodes() throws Exception
    {
        List<RdfTerm> terms = execute("SELECT ?v WHERE { { BIND(1 AS ?v) } UNION { BIND(BNODE() AS ?v) } "
                + "UNION { BIND(<http://example.org/i> AS ?v) } } ORDER BY ?v");

        assertInstanceOf(BlankNode.class, terms.get(0));
        assertEquals(List.of(iri("i"), new TypedLiteral("1", xsdIntegerIri)),
                terms.subList(1, 3).stream().collect(Collectors.toList()));
    }
}
