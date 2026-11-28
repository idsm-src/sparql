package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDouble;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdFloat;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDoubleIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdFloatIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.config.EngineSettings.JoinOptimizerMode;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.Conditions;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.imcode.SqlSelect;
import cz.iocb.sparql.engine.mapping.TermMapping;
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
 * The zeros of the two signs in columns of the floating-point types. The xsd:double values {@code 0.0E0} and
 * {@code -0.0E0} are equal, and so are those of xsd:float, but they are different RDF terms, whereas the equality of
 * the database takes them for one value. Wherever the terms have to be the same (a variable shared by the members of a
 * join, by the sides of an OPTIONAL or a MINUS or by an EXISTS pattern and its solution, sameTerm, a constant of a
 * pattern or of VALUES) and wherever solutions are grouped or deduplicated (DISTINCT, GROUP BY, the aggregates of
 * distinct values, a mapping whose rows are deduplicated, a walk of a property path), the zeros are told apart; the
 * operators {@code =} and IN compare the values. Every NaN is the same term, whatever its sign bit is.
 *
 * The table {@code item} holds the items 1 to 6 with the numbers 0, -0, 1.5, NaN, a NaN with the sign bit set and 0,
 * each as a {@code float8} and as a {@code float4}; the table {@code other} holds the items 10 to 12 with the numbers
 * 0, -0 and NaN; the table {@code multi} holds the rows of the item 7 with the numbers 0, -0 and 0 and those of the
 * item 8 with the numbers 0, NaN and a NaN with the sign bit set.
 */
public class FloatingPointZeroTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> PREFIX item: <http://example.org/item/> "
            + "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> ";

    /**
     * Configuration mapping the tables.
     */
    private static SparqlDatabaseConfiguration config = null;

    /**
     * The same configuration with the join optimizer enabled.
     */
    private static SparqlDatabaseConfiguration optimized = null;


    /**
     * Creates the {@code floating_point_zero_test} schema with the tables {@code item}, {@code other} and {@code multi}
     * and maps the numbers of the items by ex:d (xsd:double) and ex:f (xsd:float), those of the other items by ex:e
     * (xsd:double) and ex:g (xsd:float), and the rows of the table {@code multi} by ex:m and, deduplicated, by ex:u
     * (both xsd:double).
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema floating_point_zero_test");
            statement.execute("create table floating_point_zero_test.item (id int primary key, d float8 not null, "
                    + "f float4 not null)");
            statement.execute("""
                    insert into floating_point_zero_test.item values \
                    (1, '0', '0'), (2, '-0', '-0'), (3, 1.5, 1.5), (4, 'NaN', 'NaN'), \
                    (5, 'Infinity'::float8 - 'Infinity'::float8, 'Infinity'::float4 - 'Infinity'::float4), \
                    (6, '0', '0')""");
            statement.execute("create table floating_point_zero_test.other (id int primary key, d float8 not null, "
                    + "f float4 not null)");
            statement.execute("insert into floating_point_zero_test.other values (10, '0', '0'), (11, '-0', '-0'), "
                    + "(12, 'NaN', 'NaN')");
            statement.execute("create table floating_point_zero_test.multi (id int primary key, s int not null, "
                    + "d float8 not null)");
            statement.execute("insert into floating_point_zero_test.multi values (1, 7, '0'), (2, 7, '-0'), "
                    + "(3, 8, '0'), (4, 7, '0'), (5, 8, 'NaN'), (6, 8, -'NaN'::float8)");
        }

        DatabaseSchema schema = new DatabaseSchema(connectionPool);

        config = configuration(connectionPool, schema, false);
        optimized = configuration(connectionPool, schema, true);
    }


    /**
     * Configuration mapping the tables.
     *
     * @param connectionPool pool of the test database
     * @param schema schema of the test database
     * @param joinOptimizer whether the join optimizer is enabled
     * @return configuration mapping the tables
     * @throws SQLException on database errors
     */
    private static SparqlDatabaseConfiguration configuration(DataSource connectionPool, DatabaseSchema schema,
            boolean joinOptimizer) throws SQLException
    {
        SparqlDatabaseConfiguration configuration = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);
        configuration.addPrefix("ex", "http://example.org/");
        configuration.addIriClass(new IntegerUserIriClass("item", INT4, "http://example.org/item/"));

        DatabaseTable item = new DatabaseTable("floating_point_zero_test", "item");
        DatabaseTable other = new DatabaseTable("floating_point_zero_test", "other");
        DatabaseTable multi = new DatabaseTable("floating_point_zero_test", "multi");
        TermMapping subject = configuration.createIriMapping("item", "id");
        TermMapping owner = configuration.createIriMapping("item", "s");

        configuration.addQuadMapping(item, null, subject, configuration.createIriMapping("ex:d"),
                configuration.createLiteralMapping(xsdDouble, "d"));
        configuration.addQuadMapping(item, null, subject, configuration.createIriMapping("ex:f"),
                configuration.createLiteralMapping(xsdFloat, "f"));
        configuration.addQuadMapping(other, null, subject, configuration.createIriMapping("ex:e"),
                configuration.createLiteralMapping(xsdDouble, "d"));
        configuration.addQuadMapping(other, null, subject, configuration.createIriMapping("ex:g"),
                configuration.createLiteralMapping(xsdFloat, "f"));
        configuration.addQuadMapping(multi, null, owner, configuration.createIriMapping("ex:m"),
                configuration.createLiteralMapping(xsdDouble, "d"));
        configuration.addQuadMapping(multi, null, owner, configuration.createIriMapping("ex:u"),
                configuration.createLiteralMapping(xsdDouble, "d"), new Conditions(true), true);
        configuration.setEngineSettings(configuration.getEngineSettings()
                .withJoinOptimizer(joinOptimizer ? JoinOptimizerMode.SETTINGS : JoinOptimizerMode.OFF));

        return configuration;
    }


    /**
     * Translates the query into SQL.
     *
     * @param query the query, without the prefix declarations
     * @return the generated SQL
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static String translate(String query) throws SQLException, TranslateExceptions, ServiceException
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
     * Rows of the query, which have to be the same with the join optimizer enabled.
     *
     * @param query the query, without the prefix declarations
     * @return rows of the query
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws LimitExceedException if the generated SQL is too long
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static List<List<RdfTerm>> execute(String query)
            throws SQLException, TranslateExceptions, LimitExceedException, ServiceException
    {
        List<List<RdfTerm>> rows = execute(config, query);

        assertThat(execute(optimized, query), containsInAnyOrder(rows.toArray()));

        return rows;
    }


    /**
     * Rows of the query under the configuration.
     *
     * @param configuration the configuration
     * @param query the query, without the prefix declarations
     * @return rows of the query
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws LimitExceedException if the generated SQL is too long
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static List<List<RdfTerm>> execute(SparqlDatabaseConfiguration configuration, String query)
            throws SQLException, TranslateExceptions, LimitExceedException, ServiceException
    {
        try(Request request = new Engine(configuration).getRequest(); Result result = request.execute(prefix + query))
        {
            List<List<RdfTerm>> rows = new ArrayList<>();

            while(result.next())
                rows.add(Arrays.asList(result.getRow()));

            return rows;
        }
    }


    /**
     * IRI of the item.
     *
     * @param id identifier of the item
     * @return IRI of the item
     */
    private static Iri item(int id)
    {
        return new Iri("http://example.org/item/" + id);
    }


    /**
     * Rows of the items, one item each.
     *
     * @param ids identifiers of the items
     * @return rows of the items
     */
    private static Object[] items(int... ids)
    {
        return Arrays.stream(ids).mapToObj(id -> List.<RdfTerm> of(item(id))).toArray();
    }


    /**
     * Rows of the pairs of items.
     *
     * @param ids identifiers of the items, the first and the second item of every pair by turns
     * @return rows of the pairs of items
     */
    private static Object[] pairs(int... ids)
    {
        List<List<RdfTerm>> rows = new ArrayList<>();

        for(int i = 0; i < ids.length; i += 2)
            rows.add(List.of(item(ids[i]), item(ids[i + 1])));

        return rows.toArray();
    }


    /**
     * Row of an item and an xsd:double literal.
     *
     * @param id identifier of the item
     * @param number lexical form of the literal
     * @return row of the item and the literal
     */
    private static List<RdfTerm> number(int id, String number)
    {
        return List.of(item(id), new TypedLiteral(number, xsdDoubleIri));
    }


    /**
     * Rows of literals of the datatype, one literal each.
     *
     * @param datatype the datatype
     * @param numbers lexical forms of the literals
     * @return rows of the literals
     */
    private static Object[] numbers(Iri datatype, String... numbers)
    {
        return Arrays.stream(numbers).map(n -> List.<RdfTerm> of(new TypedLiteral(n, datatype))).toArray();
    }


    /**
     * Row of an xsd:double literal and the number of its solutions as an xsd:integer literal.
     *
     * @param number lexical form of the literal
     * @param count the number of the solutions
     * @return row of the literal and the number of its solutions
     */
    private static List<RdfTerm> group(String number, int count)
    {
        return List.of(new TypedLiteral(number, xsdDoubleIri),
                new TypedLiteral(Integer.toString(count), xsdIntegerIri));
    }


    @Test
    @DisplayName("a variable shared by the members of a join")
    void join() throws Exception
    {
        assertThat(execute("SELECT ?s ?o WHERE { ?s ex:d ?v . ?o ex:e ?v }"),
                containsInAnyOrder(pairs(1, 10, 2, 11, 4, 12, 5, 12, 6, 10)));

        assertThat(execute("SELECT ?s ?o WHERE { ?s ex:f ?v . ?o ex:g ?v }"),
                containsInAnyOrder(pairs(1, 10, 2, 11, 4, 12, 5, 12, 6, 10)));

        assertThat(execute("SELECT ?s ?o WHERE { ?s ex:d ?v . ?o ex:d ?v FILTER(?s != ?o) }"),
                containsInAnyOrder(pairs(1, 6, 6, 1, 4, 5, 5, 4)));

        // the database still hashes the join and looks it up by the plain equality
        assertThat(translate("SELECT ?s ?o WHERE { ?s ex:d ?v . ?o ex:e ?v }"),
                containsString("(\"tab0\".\"d\" = \"tab1\".\"d\" AND "));
    }


    @Test
    @DisplayName("a variable shared by the sides of an OPTIONAL or a MINUS, or by an EXISTS pattern and its solution")
    void optionalMinusExists() throws Exception
    {
        assertThat(execute("SELECT ?s ?o WHERE { ?s ex:d ?v OPTIONAL { ?o ex:e ?v } FILTER(BOUND(?o)) }"),
                containsInAnyOrder(pairs(1, 10, 2, 11, 4, 12, 5, 12, 6, 10)));

        assertThat(execute("SELECT ?o WHERE { ?o ex:e ?v MINUS { ?s ex:d ?v FILTER(?s != item:2) } }"),
                containsInAnyOrder(items(11)));

        assertThat(execute("SELECT ?o WHERE { ?o ex:e ?v FILTER NOT EXISTS { ?s ex:d ?v FILTER(?s != item:2) } }"),
                containsInAnyOrder(items(11)));

        assertThat(execute("SELECT ?o WHERE { ?o ex:e ?v FILTER EXISTS { ?s ex:d ?v FILTER(?s != item:2) } }"),
                containsInAnyOrder(items(10, 12)));
    }


    @Test
    @DisplayName("sameTerm tells the zeros apart, the operators = and IN compare their values")
    void sameTerm() throws Exception
    {
        String pattern = "?s ex:d ?v . ?o ex:e ?w";

        assertThat(execute("SELECT ?s ?o WHERE { " + pattern + " FILTER(sameTerm(?v, ?w)) }"),
                containsInAnyOrder(pairs(1, 10, 2, 11, 4, 12, 5, 12, 6, 10)));

        assertThat(execute("SELECT ?s ?o WHERE { " + pattern + " FILTER(!sameTerm(?v, ?w)) }"), containsInAnyOrder(
                pairs(1, 11, 1, 12, 2, 10, 2, 12, 3, 10, 3, 11, 3, 12, 4, 10, 4, 11, 5, 10, 5, 11, 6, 11, 6, 12)));

        // a NaN is equal to nothing
        assertThat(execute("SELECT ?s ?o WHERE { " + pattern + " FILTER(?v = ?w) }"),
                containsInAnyOrder(pairs(1, 10, 1, 11, 2, 10, 2, 11, 6, 10, 6, 11)));

        assertThat(execute("SELECT ?s WHERE { ?s ex:d ?v FILTER(sameTerm(?v, 0.0E0)) }"),
                containsInAnyOrder(items(1, 6)));

        assertThat(execute("SELECT ?s WHERE { ?s ex:d ?v FILTER(sameTerm(?v, -0.0E0)) }"),
                containsInAnyOrder(items(2)));

        assertThat(execute("SELECT ?s WHERE { ?s ex:d ?v FILTER(!sameTerm(?v, 0.0E0)) }"),
                containsInAnyOrder(items(2, 3, 4, 5)));

        assertThat(execute("SELECT ?s WHERE { ?s ex:d ?v BIND(-0.0E0 AS ?w) FILTER(sameTerm(?v, ?w)) }"),
                containsInAnyOrder(items(2)));

        assertThat(execute("SELECT ?s WHERE { ?s ex:d ?v FILTER(?v = 0.0E0) }"), containsInAnyOrder(items(1, 2, 6)));

        assertThat(execute("SELECT ?s WHERE { ?s ex:d ?v FILTER(?v IN (-0.0E0, 7.0E0)) }"),
                containsInAnyOrder(items(1, 2, 6)));
    }


    @Test
    @DisplayName("a constant of a pattern")
    void constant() throws Exception
    {
        assertThat(execute("SELECT ?s WHERE { ?s ex:d 0.0E0 }"), containsInAnyOrder(items(1, 6)));
        assertThat(execute("SELECT ?s WHERE { ?s ex:d -0.0E0 }"), containsInAnyOrder(items(2)));
        assertThat(execute("SELECT ?s WHERE { ?s ex:d 1.5E0 }"), containsInAnyOrder(items(3)));
        assertThat(execute("SELECT ?s WHERE { ?s ex:d 'NaN'^^xsd:double }"), containsInAnyOrder(items(4, 5)));

        assertThat(execute("SELECT ?s WHERE { ?s ex:f '0.0E0'^^xsd:float }"), containsInAnyOrder(items(1, 6)));
        assertThat(execute("SELECT ?s WHERE { ?s ex:f '-0.0E0'^^xsd:float }"), containsInAnyOrder(items(2)));

        // only a zero has to be told apart from the values equal to it
        assertThat(translate("SELECT ?s WHERE { ?s ex:d 1.5E0 }"), not(containsString("float8send")));
    }


    @Test
    @DisplayName("the constants of VALUES")
    void values() throws Exception
    {
        assertThat(execute("SELECT ?s ?v WHERE { ?s ex:d ?v VALUES ?v { 0.0E0 } }"),
                containsInAnyOrder(number(1, "0.0E0"), number(6, "0.0E0")));

        assertThat(execute("SELECT ?s ?v WHERE { ?s ex:d ?v VALUES ?v { -0.0E0 1.5E0 } }"),
                containsInAnyOrder(number(2, "-0.0E0"), number(3, "1.5E0")));

        assertThat(execute("SELECT ?s WHERE { ?s ex:d ?v MINUS { VALUES ?v { 0.0E0 } } }"),
                containsInAnyOrder(items(2, 3, 4, 5)));
    }


    @Test
    @DisplayName("DISTINCT")
    void distinct() throws Exception
    {
        Object[] doubles = numbers(xsdDoubleIri, "0.0E0", "-0.0E0", "1.5E0", "NaN");

        assertThat(execute("SELECT DISTINCT ?v WHERE { ?s ex:d ?v }"), containsInAnyOrder(doubles));

        assertThat(execute("SELECT DISTINCT ?v WHERE { ?s ex:f ?v }"),
                containsInAnyOrder(numbers(xsdFloatIri, "0.0E0", "-0.0E0", "1.5E0", "NaN")));

        // an ordered select deduplicates its rows by grouping them itself
        assertThat(execute("SELECT DISTINCT ?v WHERE { ?s ex:d ?v } ORDER BY ?s"), containsInAnyOrder(doubles));

        assertThat(execute("SELECT ?v WHERE { { SELECT DISTINCT ?v WHERE { ?s ex:d ?v } } }"),
                containsInAnyOrder(doubles));

        // only the columns of the floating-point types get the key telling the zeros apart
        assertThat(translate("SELECT DISTINCT ?s WHERE { ?s ex:d ?v }"), not(containsString("float8send")));
    }


    @Test
    @DisplayName("GROUP BY and the aggregates of distinct values")
    void groupBy() throws Exception
    {
        assertThat(execute("SELECT ?v (COUNT(*) AS ?c) WHERE { ?s ex:d ?v } GROUP BY ?v"),
                containsInAnyOrder(group("0.0E0", 2), group("-0.0E0", 1), group("1.5E0", 1), group("NaN", 2)));

        assertThat(execute("SELECT (COUNT(DISTINCT ?v) AS ?c) WHERE { ?s ex:d ?v }"),
                contains(List.of(new TypedLiteral("4", xsdIntegerIri))));

        // with another aggregate, the distinct values are counted by the aggregate itself
        assertThat(execute("SELECT (COUNT(DISTINCT ?v) AS ?c) (COUNT(*) AS ?n) WHERE { ?s ex:d ?v }"),
                contains(List.of(new TypedLiteral("4", xsdIntegerIri), new TypedLiteral("6", xsdIntegerIri))));

        // the zeros are two distinct values, so the average of the distinct values 0, -0 and 1.5 is a third of their
        // sum (the filter leaves the NaNs out, which are not equal to themselves)
        String aggregates = "(SUM(DISTINCT ?v) AS ?s) (AVG(DISTINCT ?v) AS ?a)";

        assertThat(execute("SELECT " + aggregates + " WHERE { ?i ex:d ?v FILTER(?v = ?v) }"),
                contains(List.of(new TypedLiteral("1.5E0", xsdDoubleIri), new TypedLiteral("5.0E-1", xsdDoubleIri))));

        assertThat(execute("SELECT (AVG(DISTINCT ?v) AS ?a) WHERE { ?i ex:f ?v FILTER(?v = ?v) }"),
                contains(List.of(new TypedLiteral("5.0E-1", xsdFloatIri))));
    }


    @Test
    @DisplayName("a mapping whose rows are deduplicated")
    void deduplicatedMapping() throws Exception
    {
        assertThat(execute("SELECT ?s ?v WHERE { ?s ex:u ?v }"),
                containsInAnyOrder(number(7, "0.0E0"), number(7, "-0.0E0"), number(8, "0.0E0"), number(8, "NaN")));

        assertThat(execute("SELECT ?s WHERE { ?s ex:u ?v }"), containsInAnyOrder(items(7, 7, 8, 8)));
    }


    @Test
    @DisplayName("a walk of a property path")
    void walk() throws Exception
    {
        Object[] pairs = { number(7, "0.0E0"), number(7, "-0.0E0"), number(8, "0.0E0"), number(8, "NaN") };

        assertThat(execute("SELECT ?s ?v WHERE { ?s ex:m+ ?v }"), containsInAnyOrder(pairs));
        assertThat(execute("SELECT ?s ?v WHERE { ?v ^ex:m+ ?s }"), containsInAnyOrder(pairs));

        assertThat(execute("SELECT ?v WHERE { item:7 ex:m* ?v }"), containsInAnyOrder(List.of(item(7)),
                List.of(new TypedLiteral("0.0E0", xsdDoubleIri)), List.of(new TypedLiteral("-0.0E0", xsdDoubleIri))));
    }
}
