package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT2;
import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.database.SqlType.INT8;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.config.EngineSettings.JoinOptimizerMode;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.database.SqlType;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.mapping.classes.BuiltinClasses;
import cz.iocb.sparql.engine.mapping.classes.IntegerUserIriClass;
import cz.iocb.sparql.engine.mapping.classes.UserIriClass;
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
 * Integer IRI classes of a fixed width that has room for more digits than the largest number of their SQL type: an IRI
 * of a greater number is no member, since no id the database can hold makes it, so it matches no IRI of the class and
 * is no error when it is converted to the class. Before, such an IRI was taken for a member, and the conversion of the
 * number to the SQL type failed.
 *
 * Every class has a table of the members with the ids one and the largest number of the type; the table {@code raw}
 * holds, as text, the IRI of the largest number of every class followed by IRIs of greater numbers.
 */
public class FixedWidthIriClassTest
{
    /**
     * A class with the IRI of the largest number of its type, the value mapped to it and IRIs of greater numbers.
     *
     * @param iriClass the class
     * @param largest the IRI of the largest number
     * @param value the value of the largest number
     * @param greater IRIs of greater numbers
     */
    private static record Numbers(UserIriClass iriClass, String largest, int value, List<String> greater)
    {
    }


    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX : <http://example.org/> ";

    /**
     * Namespace of the IRIs of the classes.
     */
    private static final String ns = "http://example.org/";

    /**
     * The classes with their IRIs.
     */
    private static final List<Numbers> numbers = List.of(
            new Numbers(new IntegerUserIriClass("w2", INT2, ns + "w2/", 5), ns + "w2/32767", 2,
                    List.of(ns + "w2/32768", ns + "w2/99999")),
            new Numbers(new IntegerUserIriClass("w4", INT4, ns + "w4/", 10), ns + "w4/2147483647", 4,
                    List.of(ns + "w4/2147483648", ns + "w4/9999999999")),
            new Numbers(new IntegerUserIriClass("w8", INT8, ns + "w8/", 19), ns + "w8/9223372036854775807", 6,
                    List.of(ns + "w8/9223372036854775808", ns + "w8/9999999999999999999")),
            new Numbers(new IntegerUserIriClass("w2s", INT2, ns + "w2s/", 7, "_S"), ns + "w2s/0032767_S", 8,
                    List.of(ns + "w2s/0032768_S", ns + "w2s/1000001_S")));

    /**
     * The IRIs of the table {@code raw}, in the order of its rows.
     */
    private static final List<String> raw = numbers.stream()
            .flatMap(n -> Stream.concat(Stream.of(n.largest()), n.greater().stream())).toList();

    /**
     * Configuration mapping the tables.
     */
    private static SparqlDatabaseConfiguration config = null;

    /**
     * The same configuration with the join optimizer enabled.
     */
    private static SparqlDatabaseConfiguration optimized = null;


    /**
     * Creates the {@code fixed_width_iri_class_test} schema with the table {@code raw} of the IRIs and a table of
     * values for every class, and maps the IRIs by :raw and the values by :v.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            String tables = "fixed_width_iri_class_test.";

            statement.execute("create schema fixed_width_iri_class_test");
            statement.execute("create table " + tables + "raw (l int primary key, t varchar not null)");

            for(int i = 0; i < raw.size(); i++)
                statement.execute("insert into " + tables + "raw values (" + (i + 1) + ", '" + raw.get(i) + "')");

            statement.execute("create table " + tables + "w2 (id int2 not null, v int not null)");
            statement.execute("insert into " + tables + "w2 values (1, 1), (32767, 2)");
            statement.execute("create table " + tables + "w4 (id int4 not null, v int not null)");
            statement.execute("insert into " + tables + "w4 values (1, 3), (2147483647, 4)");
            statement.execute("create table " + tables + "w8 (id int8 not null, v int not null)");
            statement.execute("insert into " + tables + "w8 values (1, 5), (9223372036854775807, 6)");
            statement.execute("create table " + tables + "w2s (id int2 not null, v int not null)");
            statement.execute("insert into " + tables + "w2s values (1, 7), (32767, 8)");
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
        configuration.addPrefix("ex", ns);

        UserIriClass lnk = new IntegerUserIriClass("lnk", INT4, ns + "lnk/");

        configuration.addIriClass(lnk);
        configuration.addQuadMapping(new DatabaseTable("fixed_width_iri_class_test", "raw"), null,
                configuration.createIriMapping(lnk, "l"), configuration.createIriMapping("ex:raw"),
                configuration.createIriMapping(BuiltinClasses.iri, "t"));

        for(Numbers n : numbers)
        {
            UserIriClass iriClass = n.iriClass();

            configuration.addIriClass(iriClass);
            configuration.addQuadMapping(new DatabaseTable("fixed_width_iri_class_test", iriClass.getResourceName()),
                    null, configuration.createIriMapping(iriClass, "id"), configuration.createIriMapping("ex:v"),
                    configuration.createLiteralMapping(xsdInt, "v"));
        }

        configuration.setEngineSettings(configuration.getEngineSettings()
                .withJoinOptimizer(joinOptimizer ? JoinOptimizerMode.SETTINGS : JoinOptimizerMode.OFF));

        return configuration;
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
     * Rows of the IRIs of the table {@code raw}, one IRI each.
     *
     * @param iris the IRIs
     * @return rows of the IRIs
     */
    private static Object[] rows(Stream<String> iris)
    {
        return iris.map(iri -> List.<RdfTerm> of(new Iri(ns + "lnk/" + (raw.indexOf(iri) + 1)))).toArray();
    }


    /**
     * Whether the class takes the number, written with the width, for a member, which it has to do just when the number
     * is not greater than the largest number of the type.
     *
     * @param sqlType the SQL type of the class
     * @param width the width of the class
     * @param number the number
     * @param max the largest number of the type
     * @return whether the class takes the number for a member as it has to
     */
    private static boolean decides(SqlType sqlType, int width, BigInteger number, BigInteger max)
    {
        IntegerUserIriClass iriClass = new IntegerUserIriClass("w", sqlType, ns, width);
        String digits = number.toString();
        Iri iri = new Iri(ns + "0".repeat(width - digits.length()) + digits);

        return iriClass.match(null, iri) == (number.compareTo(max) <= 0);
    }


    @Test
    @DisplayName("the numbers of a fixed width that a class admits")
    void ranges()
    {
        // every number of the widths up to one digit beyond the largest smallint
        for(int width = 1; width <= 6; width++)
        {
            IntegerUserIriClass iriClass = new IntegerUserIriClass("w", INT2, ns, width);

            for(int number = 0; number < BigInteger.TEN.pow(width).intValue(); number++)
            {
                String digits = Integer.toString(number);
                Iri iri = new Iri(ns + "0".repeat(width - digits.length()) + digits);

                assertThat(iri.getValue(), iriClass.match(null, iri), is(number <= Short.MAX_VALUE));
            }
        }

        // the numbers around the largest integer and bigint, which differ from it in one digit
        for(SqlType sqlType : List.of(INT4, INT8))
        {
            BigInteger max = BigInteger.valueOf(sqlType == INT4 ? Integer.MAX_VALUE : Long.MAX_VALUE);
            String digits = max.toString();

            for(int width = digits.length(); width <= digits.length() + 2; width++)
            {
                List<BigInteger> samples = new ArrayList<>(List.of(BigInteger.ZERO, BigInteger.ONE, max,
                        max.add(BigInteger.ONE), BigInteger.TEN.pow(digits.length()).subtract(BigInteger.ONE),
                        BigInteger.TEN.pow(width).subtract(BigInteger.ONE)));

                for(int i = 0; i < digits.length(); i++)
                    for(int d = 0; d <= 9; d++)
                        samples.add(new BigInteger(digits.substring(0, i) + d + digits.substring(i + 1)));

                for(BigInteger number : samples)
                    assertThat(sqlType + " " + width + " " + number, decides(sqlType, width, number, max), is(true));
            }
        }
    }


    @Test
    @DisplayName("constant IRIs of numbers greater than the largest number of the type")
    void constants() throws Exception
    {
        for(Numbers n : numbers)
        {
            String values = n.greater().stream().map(iri -> "<" + iri + ">").reduce("<" + n.largest() + ">",
                    (a, b) -> a + " " + b);
            Object[] value = { List.<RdfTerm> of(new TypedLiteral(Integer.toString(n.value()), xsdIntIri)) };

            assertThat(execute("SELECT ?v WHERE { <" + n.largest() + "> :v ?v }"), containsInAnyOrder(value));

            for(String iri : n.greater())
                assertThat(execute("SELECT ?v WHERE { <" + iri + "> :v ?v }"), is(empty()));

            assertThat(execute("SELECT ?v WHERE { VALUES ?s { " + values + " } ?s :v ?v }"), containsInAnyOrder(value));

            assertThat(execute("SELECT ?v WHERE { ?s :v ?v FILTER(?s IN (" + values.replace(" ", ", ") + ")) }"),
                    containsInAnyOrder(value));
        }
    }


    @Test
    @DisplayName("IRIs held as text converted to a class of a fixed width")
    void conversions() throws Exception
    {
        assertThat(
                execute("SELECT ?l WHERE { ?l :raw ?t FILTER EXISTS { { SELECT (COUNT(*) AS ?c) WHERE { ?t :v ?v } } "
                        + "FILTER(?c > 0) } }"),
                containsInAnyOrder(rows(numbers.stream().map(Numbers::largest))));

        assertThat(execute("SELECT ?l WHERE { ?l :raw ?t FILTER NOT EXISTS { { SELECT ?v WHERE { ?t :v ?v } } } }"),
                containsInAnyOrder(rows(numbers.stream().flatMap(n -> n.greater().stream()))));
    }
}
