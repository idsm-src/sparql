package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genInt;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdByte;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdLong;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdNonNegativeInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdShort;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdUnsignedByte;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdUnsignedInt;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdUnsignedShort;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdBooleanIri;
import static java.util.Locale.ROOT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import java.math.BigDecimal;
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
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.imcode.SqlSelect;
import cz.iocb.sparql.engine.mapping.TermMapping;
import cz.iocb.sparql.engine.mapping.classes.IntegerUserIriClass;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.Literal;
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
 * An exact numeric stored in an integer type of SQL is compared with a constant in that type: the constant is converted
 * to the type of the column, instead of the column being promoted to the datatype the two are compared in (an xsd:int
 * and an xsd:integer are compared as integers, which are stored as numerics), so that the database can look the rows up
 * by an index of the column and estimate them by its statistics. A constant the type does not hold decides the
 * comparison by its position among the values of the type. A value that is, as it is stored, a value of the datatype it
 * is promoted to is taken without a conversion as well.
 *
 * The values of every datatype {@code xsd:<name>} are held by the column {@code v} of a table of their own (named
 * {@code t_<name>} in lower case) and mapped by the predicate {@code ex:<name>}; the row of a value has its position
 * among the values, counted from one, as its identifier. The table {@code lexical} holds the xsd:int literals 3, 03, 4
 * and +3.
 */
public class NumericConstantComparisonTest
{
    /**
     * A datatype with the SQL type storing its values and with the values of its table.
     *
     * @param name the local name of the datatype
     * @param literalClass the class of the canonical literals of the datatype
     * @param type the SQL type of the column
     * @param values the values of the table
     */
    private record Numeric(String name, LiteralClass literalClass, String type, List<String> values)
    {
        /**
         * The name of the table holding the values.
         *
         * @return the name of the table holding the values
         */
        private String table()
        {
            return "t_" + name.toLowerCase(ROOT);
        }
    }


    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> "
            + "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> ";

    /**
     * The datatypes: those stored in the integer types of SQL, with the least and the greatest values of the types they
     * are stored in or compared with, and two datatypes stored as numerics.
     */
    private static final List<Numeric> numerics = List.of(
            new Numeric("byte", xsdByte, "int2", List.of("-128", "-1", "0", "3", "127")),
            new Numeric("unsignedByte", xsdUnsignedByte, "int2", List.of("0", "3", "127", "128", "255")),
            new Numeric("short", xsdShort, "int2", List.of("-32768", "-129", "-128", "0", "3", "127", "128", "32767")),
            new Numeric("unsignedShort", xsdUnsignedShort, "int4",
                    List.of("0", "3", "255", "256", "32767", "32768", "65535")),
            new Numeric("int", xsdInt, "int4",
                    List.of("-2147483648", "-32769", "-32768", "-1", "0", "3", "4", "32767", "32768", "2147483647")),
            new Numeric("unsignedInt", xsdUnsignedInt, "int8",
                    List.of("0", "3", "65535", "65536", "2147483647", "2147483648", "4294967295")),
            new Numeric("long", xsdLong, "int8",
                    List.of("-9223372036854775808", "-2147483649", "-2147483648", "0", "3", "4", "2147483647",
                            "2147483648", "9223372036854775807")),
            new Numeric("integer", xsdInteger, "numeric",
                    List.of("-9223372036854775809", "0", "3", "4", "9223372036854775808")),
            new Numeric("nonNegativeInteger", xsdNonNegativeInteger, "numeric",
                    List.of("0", "3", "3", "4", "9223372036854775808")));

    /**
     * Constants the values are compared with: the least and the greatest values of the integer types of SQL, their
     * neighbours on both sides, and decimals between integers and beyond the ranges.
     */
    private static final List<String> constants = List.of("0", "3", "-1", "127", "128", "-128", "-129", "255", "256",
            "32767", "32768", "-32768", "-32769", "65535", "65536", "2147483647", "2147483648", "-2147483648",
            "-2147483649", "4294967295", "4294967296", "9223372036854775807", "9223372036854775808",
            "-9223372036854775808", "-9223372036854775809", "0.0", "3.0", "3.5", "-0.5", "127.5", "-128.5", "32767.5",
            "-32768.5", "2147483647.5", "-2147483648.5", "9223372036854775807.5", "-9223372036854775808.5");

    /**
     * Configuration mapping the tables.
     */
    private static SparqlDatabaseConfiguration config = null;


    /**
     * Creates the {@code numeric_constant_test} schema and maps the values of every datatype by the predicate named
     * after the datatype, and the literals of the table {@code lexical} by ex:lexical.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema numeric_constant_test");

            for(Numeric numeric : numerics)
            {
                String table = "numeric_constant_test." + numeric.table();

                statement.execute("create table " + table + " (id int primary key, v " + numeric.type() + " not null)");

                for(int i = 0; i < numeric.values().size(); i++)
                    statement.execute(
                            "insert into " + table + " values (" + (i + 1) + ", " + numeric.values().get(i) + ")");
            }

            statement.execute("create table numeric_constant_test.lexical (id int primary key, v int not null, "
                    + "form varchar not null)");
            statement.execute("insert into numeric_constant_test.lexical values (1, 3, ''), (2, 3, '03'), (3, 4, ''), "
                    + "(4, 3, '+3')");
        }

        DatabaseSchema schema = new DatabaseSchema(connectionPool);

        config = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);
        config.addPrefix("ex", "http://example.org/");
        config.addIriClass(new IntegerUserIriClass("row", INT4, "http://example.org/row/"));

        TermMapping subject = config.createIriMapping("row", "id");

        for(Numeric numeric : numerics)
            config.addQuadMapping(new DatabaseTable("numeric_constant_test", numeric.table()), null, subject,
                    config.createIriMapping("ex:" + numeric.name()),
                    config.createLiteralMapping(numeric.literalClass(), "v"));

        config.addQuadMapping(new DatabaseTable("numeric_constant_test", "lexical"), null, subject,
                config.createIriMapping("ex:lexical"), config.createLiteralMapping(genInt, "v", "form"));
    }


    /**
     * Translates the query selecting the subjects of the pattern into SQL.
     *
     * @param pattern the group graph pattern, without the braces
     * @return the generated SQL
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static String translate(String pattern) throws SQLException, TranslateExceptions, ServiceException
    {
        Engine engine = new Engine(config);

        try(Request request = engine.getRequest())
        {
            PreparedQuery prepared = request.prepareQuery(prefix + "SELECT ?s WHERE { " + pattern + " }", null);

            SqlSelect imcode = new TranslateVisitor(request).translate(prepared.getSyntaxTree(), null, null, List.of());

            return imcode.optimize(request, false).optimize(request, true).translate(request);
        }
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
     * Identifiers of the rows that are the subjects of the solutions of the pattern, in ascending order.
     *
     * @param pattern the group graph pattern, without the braces
     * @return identifiers of the rows that are the subjects of the solutions of the pattern
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws LimitExceedException if the generated SQL is too long
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static List<Integer> subjects(String pattern)
            throws SQLException, TranslateExceptions, LimitExceedException, ServiceException
    {
        return execute("SELECT ?s WHERE { " + pattern + " }").stream()
                .map(r -> Integer.valueOf(((Iri) r.get(0)).getValue().substring("http://example.org/row/".length())))
                .sorted().toList();
    }


    /**
     * The xsd:boolean literal of the value.
     *
     * @param value the value
     * @return the xsd:boolean literal of the value
     */
    private static TypedLiteral bool(boolean value)
    {
        return new TypedLiteral(Boolean.toString(value), xsdBooleanIri);
    }


    @Test
    @DisplayName("a constant is compared in the SQL type the column is stored in")
    void columnType() throws Exception
    {
        String equal = translate("?s ex:int ?v FILTER(?v = 3)");
        String greater = translate("?s ex:long ?v FILTER(?v >= 2147483648)");
        String typed = translate("?s ex:short ?v FILTER(?v != '3'^^xsd:int)");

        assertThat(equal, containsString("(\"v\" = '3'::int4)"));
        assertThat(greater, containsString("(\"v\" >= '2147483648'::int8)"));
        assertThat(typed, containsString("(\"v\" != '3'::int2)"));

        // an unsigned datatype is stored in a signed type holding all its values
        String unsigned = translate("?s ex:unsignedShort ?v FILTER(?v < 256)");

        assertThat(unsigned, containsString("(\"v\" < '256'::int4)"));

        // the operator is swapped when the constant is the left operand
        String swapped = translate("?s ex:int ?v FILTER(3 < ?v)");

        assertThat(swapped, containsString("(\"v\" > '3'::int4)"));

        // the members of a list are compared one by one, and a variable bound to a constant is a constant too
        String list = translate("?s ex:int ?v FILTER(?v IN (3, 4))");
        String bound = translate("VALUES ?c { 3 } ?s ex:int ?v FILTER(?v = ?c)");

        assertThat(list, containsString("\"v\" = '3'::int4"));
        assertThat(list, containsString("\"v\" = '4'::int4"));
        assertThat(bound, containsString("= '3'::int4"));

        for(String sql : List.of(equal, greater, typed, unsigned, swapped, list, bound))
            assertThat(sql, not(containsString("sparql.cast_as_")));

        assertThat(subjects("?s ex:int ?v FILTER(?v = 3)"), contains(6));
        assertThat(subjects("?s ex:long ?v FILTER(?v >= 2147483648)"), contains(8, 9));
        assertThat(subjects("?s ex:short ?v FILTER(?v != '3'^^xsd:int)"), contains(1, 2, 3, 4, 6, 7, 8));
        assertThat(subjects("?s ex:unsignedShort ?v FILTER(?v < 256)"), contains(1, 2, 3));
        assertThat(subjects("?s ex:int ?v FILTER(3 < ?v)"), contains(7, 8, 9, 10));
        assertThat(subjects("?s ex:int ?v FILTER(?v IN (3, 4))"), contains(6, 7));
        assertThat(subjects("VALUES ?c { 3 } ?s ex:int ?v FILTER(?v = ?c)"), contains(6));
    }


    @Test
    @DisplayName("a constant beyond the range of the type decides the comparison")
    void beyondRange() throws Exception
    {
        String sql = translate("?s ex:int ?v FILTER(?v = 2147483648)");

        assertThat(sql, not(containsString("sparql.cast_as_")));
        assertThat(sql, not(containsString("2147483648")));

        assertThat(subjects("?s ex:int ?v FILTER(?v = 2147483648)"), is(empty()));
        assertThat(subjects("?s ex:int ?v FILTER(?v > 2147483648)"), is(empty()));
        assertThat(subjects("?s ex:int ?v FILTER(?v >= 2147483648)"), is(empty()));
        assertThat(subjects("?s ex:int ?v FILTER(?v != 2147483648)"), contains(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        assertThat(subjects("?s ex:int ?v FILTER(?v < 2147483648)"), contains(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        assertThat(subjects("?s ex:int ?v FILTER(?v <= 2147483648)"), contains(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));

        assertThat(subjects("?s ex:int ?v FILTER(?v = -2147483649)"), is(empty()));
        assertThat(subjects("?s ex:int ?v FILTER(?v < -2147483649)"), is(empty()));
        assertThat(subjects("?s ex:int ?v FILTER(?v > -2147483649)"), contains(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));

        // the least and the greatest values of the type are compared as any other
        assertThat(subjects("?s ex:int ?v FILTER(?v >= 2147483647)"), contains(10));
        assertThat(subjects("?s ex:int ?v FILTER(?v <= -2147483648)"), contains(1));

        // the comparison is false, not an error, so its negation holds
        assertThat(subjects("?s ex:int ?v FILTER(!(?v = 2147483648))"), contains(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));

        // whereas the comparison of an unbound operand is an error whatever the constant is
        assertThat(
                execute("SELECT ?s ?b WHERE { ?s ex:short ?w OPTIONAL { ?s ex:byte ?v } BIND(?v < 32768 AS ?b) } "
                        + "ORDER BY ?s").stream().map(r -> r.get(1)).toList(),
                contains(bool(true), bool(true), bool(true), bool(true), bool(true), null, null, null));
    }


    @Test
    @DisplayName("a constant between two integers is replaced by the integer deciding the comparison")
    void fraction() throws Exception
    {
        assertThat(translate("?s ex:int ?v FILTER(?v < 3.5)"), containsString("(\"v\" < '4'::int4)"));
        assertThat(translate("?s ex:int ?v FILTER(?v <= 3.5)"), containsString("(\"v\" <= '3'::int4)"));
        assertThat(translate("?s ex:int ?v FILTER(?v > 3.5)"), containsString("(\"v\" > '3'::int4)"));
        assertThat(translate("?s ex:int ?v FILTER(?v >= 3.5)"), containsString("(\"v\" >= '4'::int4)"));
        assertThat(translate("?s ex:int ?v FILTER(?v > -0.5)"), containsString("(\"v\" > '-1'::int4)"));

        // a decimal that is an integer is compared as the integer
        assertThat(translate("?s ex:int ?v FILTER(?v = 3.0)"), containsString("(\"v\" = '3'::int4)"));

        assertThat(translate("?s ex:int ?v FILTER(?v = 3.5)"), not(containsString("sparql.cast_as_")));

        assertThat(subjects("?s ex:int ?v FILTER(?v < 3.5)"), contains(1, 2, 3, 4, 5, 6));
        assertThat(subjects("?s ex:int ?v FILTER(?v <= 3.5)"), contains(1, 2, 3, 4, 5, 6));
        assertThat(subjects("?s ex:int ?v FILTER(?v > 3.5)"), contains(7, 8, 9, 10));
        assertThat(subjects("?s ex:int ?v FILTER(?v >= 3.5)"), contains(7, 8, 9, 10));
        assertThat(subjects("?s ex:int ?v FILTER(?v > -0.5)"), contains(5, 6, 7, 8, 9, 10));
        assertThat(subjects("?s ex:int ?v FILTER(?v = 3.0)"), contains(6));
        assertThat(subjects("?s ex:int ?v FILTER(?v = 3.5)"), is(empty()));
        assertThat(subjects("?s ex:int ?v FILTER(?v != 3.5)"), contains(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
    }


    @Test
    @DisplayName("a value stored as a value of the datatype it is promoted to is taken as it is")
    void storedValue() throws Exception
    {
        String decimal = translate("?s ex:integer ?v FILTER(?v > 0.5)");
        String integer = translate("?s ex:nonNegativeInteger ?v FILTER(?v = 3)");
        String narrow = translate("?s ex:byte ?v FILTER(?v = '3'^^xsd:short)");
        String columns = translate("?s ex:integer ?a . ?s ex:nonNegativeInteger ?b FILTER(?a = ?b)");
        String sum = translate("?s ex:integer ?v FILTER(?v + 0.5 > 3)");

        assertThat(decimal, containsString("(\"v\" > '0.5'::numeric)"));
        assertThat(integer, containsString("(\"v\" = '3'::numeric)"));
        assertThat(narrow, containsString("(\"v\" = '3'::int2)"));

        for(String sql : List.of(decimal, integer, narrow, columns, sum))
            assertThat(sql, not(containsString("sparql.cast_as_")));

        assertThat(subjects("?s ex:integer ?v FILTER(?v > 0.5)"), contains(3, 4, 5));
        assertThat(subjects("?s ex:nonNegativeInteger ?v FILTER(?v = 3)"), contains(2, 3));
        assertThat(subjects("?s ex:byte ?v FILTER(?v = '3'^^xsd:short)"), contains(4));
        assertThat(subjects("?s ex:integer ?a . ?s ex:nonNegativeInteger ?b FILTER(?a = ?b)"), contains(3, 4, 5));
        assertThat(subjects("?s ex:integer ?v FILTER(?v + 0.5 > 3)"), contains(3, 4, 5));
    }


    @Test
    @DisplayName("an operand that is no exact constant is compared in the common datatype")
    void commonDatatype() throws Exception
    {
        // two columns of different types
        assertThat(subjects("?s ex:int ?a . ?s ex:long ?b FILTER(?a > ?b)"), contains(1, 2, 3));
        assertThat(subjects("?s ex:int ?a . ?s ex:long ?b FILTER(?a < ?b)"), contains(4, 5, 6, 7, 8, 9));
        assertThat(subjects("?s ex:unsignedShort ?a . ?s ex:short ?b FILTER(?a = ?b)"), is(empty()));
        assertThat(subjects("?s ex:unsignedShort ?a . ?s ex:short ?b FILTER(?a > ?b)"), contains(1, 2, 3, 4, 5, 6, 7));

        // a constant of a floating-point datatype
        assertThat(subjects("?s ex:int ?v FILTER(?v = 3.0e0)"), contains(6));
        assertThat(subjects("?s ex:int ?v FILTER(?v < 3.5e0)"), contains(1, 2, 3, 4, 5, 6));
        assertThat(subjects("?s ex:int ?v FILTER(?v < 'INF'^^xsd:double)"), contains(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        assertThat(subjects("?s ex:int ?v FILTER(?v = 'NaN'^^xsd:double)"), is(empty()));

        // an expression
        assertThat(subjects("?s ex:int ?v FILTER(?v = 2 + 1)"), contains(6));
    }


    @Test
    @DisplayName("literals of every lexical form are compared by their values")
    void lexicalForm() throws Exception
    {
        String sql = translate("?s ex:lexical ?v FILTER(?v = 3)");

        assertThat(sql, containsString("(\"v\" = '3'::int4)"));
        assertThat(sql, not(containsString("sparql.cast_as_")));

        assertThat(subjects("?s ex:lexical ?v FILTER(?v = 3)"), contains(1, 2, 4));
        assertThat(subjects("?s ex:lexical ?v FILTER(?v > 3)"), contains(3));

        // the constant in another lexical form
        assertThat(subjects("?s ex:lexical ?v FILTER(?v = 03)"), contains(1, 2, 4));
        assertThat(subjects("?s ex:lexical ?v FILTER(?v = +3.00)"), contains(1, 2, 4));
        assertThat(subjects("?s ex:int ?v FILTER(?v = '+03'^^xsd:long)"), contains(6));
    }


    @Test
    @DisplayName("every comparison of a stored value with a constant has the result of the exact comparison")
    void results() throws Exception
    {
        for(Numeric numeric : numerics)
        {
            for(String constant : constants)
            {
                String query = """
                        SELECT ?v (?v = %1$s AS ?a) (?v != %1$s AS ?b) (?v < %1$s AS ?c) (?v <= %1$s AS ?d) \
                        (?v > %1$s AS ?e) (?v >= %1$s AS ?f) (%1$s = ?v AS ?g) (%1$s != ?v AS ?h) \
                        (%1$s < ?v AS ?i) (%1$s <= ?v AS ?j) (%1$s > ?v AS ?k) (%1$s >= ?v AS ?l) \
                        WHERE { ?s ex:%2$s ?v }""";

                List<List<RdfTerm>> rows = execute(query.formatted(constant, numeric.name()));

                assertThat(rows.size(), is(numeric.values().size()));

                for(List<RdfTerm> row : rows)
                {
                    int order = new BigDecimal(((Literal) row.get(0)).getValue()).compareTo(new BigDecimal(constant));
                    String reason = row.get(0) + " compared with " + constant;

                    assertThat(reason, row.subList(1, 13),
                            contains(bool(order == 0), bool(order != 0), bool(order < 0), bool(order <= 0),
                                    bool(order > 0), bool(order >= 0), bool(order == 0), bool(order != 0),
                                    bool(order > 0), bool(order >= 0), bool(order < 0), bool(order <= 0)));
                }
            }
        }
    }
}
