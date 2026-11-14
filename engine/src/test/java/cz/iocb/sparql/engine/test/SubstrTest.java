package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.rdfLangString;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.mapping.TermMapping;
import cz.iocb.sparql.engine.mapping.classes.IntegerUserIriClass;
import cz.iocb.sparql.engine.mapping.classes.LangStringWithTagClass;
import cz.iocb.sparql.engine.rdf.LangStringLiteral;
import cz.iocb.sparql.engine.rdf.Literal;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.TypedLiteral;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.LimitExceedException;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.translator.ServiceException;
import cz.iocb.sparql.testing.Database;



/**
 * SUBSTR takes any integers as the starting position and the length, as fn:substring of XPath does: the result holds
 * the characters at the positions {@code p} with {@code start <= p < start + length}, to the end of the string when no
 * length is given. So a start before the string shortens the length, and a start beyond the string, a length that is
 * not positive and the numbers beyond the range of an SQL integer leave the characters that fit, down to none, and are
 * never an error.
 *
 * The table {@code source} holds the string {@code abcdefgh} in a string column, in a column of English strings, with a
 * language tag in a column of its own, and in a box; the table {@code bounds} every pair of the starting positions and
 * the lengths tested, and the table {@code small} a few pairs in the integer type of SQL.
 */
public class SubstrTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> ";

    /**
     * The string the substrings are taken of.
     */
    private static final String string = "abcdefgh";

    /**
     * The starting positions tested: before, inside and beyond the string, at the bounds of the integer types of SQL
     * and beyond them.
     */
    private static final List<String> starts = List.of("-100000000000000000000", "-2147483649", "-5", "-1", "0", "1",
            "2", "8", "9", "100", "2147483647", "2147483648", "4294967295", "100000000000000000000");

    /**
     * The lengths tested: negative, zero, within the string and beyond it, at the bounds of the integer types of SQL
     * and beyond them.
     */
    private static final List<String> lengths = List.of("-100000000000000000000", "-2147483649", "-1", "0", "1", "3",
            "100", "2147483647", "2147483648", "100000000000000000000");

    /**
     * Engine over the configuration mapping the tables.
     */
    private static Engine engine = null;


    /**
     * Creates the {@code substr_test} schema and maps the string of the table {@code source} by ex:plain, ex:english,
     * ex:tagged and ex:boxed, the pairs of the table {@code bounds} by ex:start and ex:length, and the pairs of the
     * table {@code small} by ex:from and ex:count.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema substr_test");
            statement.execute("create table substr_test.source (id int primary key, plain varchar not null, "
                    + "lang varchar not null, boxed sparql.rdfbox not null)");
            statement.execute("insert into substr_test.source values (1, 'abcdefgh', 'cs', '\"abcdefgh\"@de')");
            statement.execute("create table substr_test.bounds (id int primary key, start numeric not null, "
                    + "length numeric not null)");

            for(int i = 0; i < starts.size(); i++)
                for(int j = 0; j < lengths.size(); j++)
                    statement.execute("insert into substr_test.bounds values (" + (i * lengths.size() + j) + ", "
                            + starts.get(i) + ", " + lengths.get(j) + ")");

            statement.execute("create table substr_test.small (id int primary key, start int not null, "
                    + "length int not null)");
            statement.execute("insert into substr_test.small values (1, 2, 3), (2, -1, 4), (3, 4, -2), (4, 9, 1), "
                    + "(5, 2147483647, 2147483647)");
        }

        DatabaseSchema schema = new DatabaseSchema(connectionPool);

        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);
        config.addPrefix("ex", "http://example.org/");
        config.addIriClass(new IntegerUserIriClass("row", INT4, "http://example.org/row/"));

        DatabaseTable source = new DatabaseTable("substr_test", "source");
        DatabaseTable bounds = new DatabaseTable("substr_test", "bounds");
        DatabaseTable small = new DatabaseTable("substr_test", "small");
        TermMapping subject = config.createIriMapping("row", "id");

        config.addQuadMapping(source, null, subject, config.createIriMapping("ex:plain"),
                config.createLiteralMapping(xsdString, "plain"));
        config.addQuadMapping(source, null, subject, config.createIriMapping("ex:english"),
                config.createLiteralMapping(LangStringWithTagClass.get("en"), "plain"));
        config.addQuadMapping(source, null, subject, config.createIriMapping("ex:tagged"),
                config.createLiteralMapping(rdfLangString, "plain", "lang"));
        config.addQuadMapping(source, null, subject, config.createIriMapping("ex:boxed"),
                config.createTermMapping(box, "boxed"));
        config.addQuadMapping(bounds, null, subject, config.createIriMapping("ex:start"),
                config.createLiteralMapping(xsdInteger, "start"));
        config.addQuadMapping(bounds, null, subject, config.createIriMapping("ex:length"),
                config.createLiteralMapping(xsdInteger, "length"));
        config.addQuadMapping(small, null, subject, config.createIriMapping("ex:from"),
                config.createLiteralMapping(xsdInt, "start"));
        config.addQuadMapping(small, null, subject, config.createIriMapping("ex:count"),
                config.createLiteralMapping(xsdInt, "length"));

        engine = new Engine(config);
    }


    /**
     * Runs the query and returns all rows.
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
        try(Request request = engine.getRequest(); Result result = request.execute(prefix + query))
        {
            List<List<RdfTerm>> rows = new ArrayList<>();

            while(result.next())
                rows.add(Arrays.asList(result.getRow()));

            return rows;
        }
    }


    /**
     * The characters of the string at the positions from the start on, as many as the length says, as fn:substring
     * defines them.
     *
     * @param start the starting position, the first character being at the position one
     * @param length the length, or null for all the characters from the start on
     * @return the characters of the string at the positions {@code p} with {@code start <= p < start + length}
     */
    private static String substring(BigInteger start, BigInteger length)
    {
        StringBuilder result = new StringBuilder();

        for(int i = 0; i < string.length(); i++)
        {
            BigInteger position = BigInteger.valueOf(i + 1);

            if(position.compareTo(start) >= 0 && (length == null || position.compareTo(start.add(length)) < 0))
                result.append(string.charAt(i));
        }

        return result.toString();
    }


    /**
     * Checks the substrings of the source for every pair of a starting position and a length.
     *
     * @param pattern the triple pattern binding the source to {@code ?t}
     * @param literal the literal of the kind of the source for a lexical form
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws LimitExceedException if the generated SQL is too long
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static void check(String pattern, Function<String, Literal> literal)
            throws SQLException, TranslateExceptions, LimitExceedException, ServiceException
    {
        List<List<RdfTerm>> rows = execute("SELECT ?s ?l (SUBSTR(?t, ?s, ?l) AS ?r) (SUBSTR(?t, ?s) AS ?q) WHERE { "
                + pattern + " ?b ex:start ?s ; ex:length ?l }");

        assertThat(rows, hasSize(starts.size() * lengths.size()));

        for(List<RdfTerm> row : rows)
        {
            BigInteger start = new BigInteger(((Literal) row.get(0)).getValue());
            BigInteger length = new BigInteger(((Literal) row.get(1)).getValue());
            String reason = "SUBSTR from " + start + " of the length " + length;

            assertThat(reason, row.get(2), is(literal.apply(substring(start, length))));
            assertThat(reason, row.get(3), is(literal.apply(substring(start, null))));
        }
    }


    @Test
    @DisplayName("SUBSTR of a string takes any start and any length")
    void string() throws Exception
    {
        check("?x ex:plain ?t .", value -> new TypedLiteral(value, xsdStringIri));
        check("BIND('abcdefgh' AS ?t)", value -> new TypedLiteral(value, xsdStringIri));
    }


    @Test
    @DisplayName("SUBSTR of a language-tagged string takes any start and any length")
    void languageTaggedString() throws Exception
    {
        check("?x ex:english ?t .", value -> new LangStringLiteral(value, "en"));
        check("?x ex:tagged ?t .", value -> new LangStringLiteral(value, "cs"));
        check("BIND('abcdefgh'@fr AS ?t)", value -> new LangStringLiteral(value, "fr"));
    }


    @Test
    @DisplayName("SUBSTR of a string in a box takes any start and any length")
    void box() throws Exception
    {
        check("?x ex:boxed ?t .", value -> new LangStringLiteral(value, "de"));
    }


    @Test
    @DisplayName("SUBSTR takes its integers in any integer datatype and as constants")
    void integers() throws Exception
    {
        List<List<RdfTerm>> columns = execute("SELECT (SUBSTR(?t, ?s, ?l) AS ?r) WHERE { ?x ex:plain ?t . "
                + "?b ex:from ?s ; ex:count ?l } ORDER BY ?b");

        assertThat(columns.stream().map(r -> ((Literal) r.get(0)).getValue()).toList(),
                contains("bcd", "ab", "", "", ""));

        List<List<RdfTerm>> constants = execute("""
                SELECT (SUBSTR(?t, 4294967296) AS ?r) (SUBSTR(?t, 2, -1) AS ?q) \
                (SUBSTR(?t, -4294967296, 4294967299) AS ?p) (SUBSTR(?t, 7, 100000000000) AS ?o) \
                WHERE { ?x ex:plain ?t }""");

        assertThat(constants, hasSize(1));
        assertThat(constants.get(0).stream().map(t -> ((Literal) t).getValue()).toList(), contains("", "", "ab", "gh"));
    }
}
