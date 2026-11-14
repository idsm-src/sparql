package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDecimal;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdFloat;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdLong;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdBooleanIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
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
 * A value stored in another type of SQL than the floating-point datatype it is compared in (an integer or a decimal
 * compared with an xsd:float or an xsd:double, an xsd:float compared with an xsd:double) is compared with a constant in
 * the type it is stored in: instead of the column being wrapped in the function converting it, the constant is replaced
 * by the values of the stored type it lies between, so that the database can look the rows up by an index of the column
 * and estimate them by its statistics. The conversion rounds, so several integers, and a whole range of decimals, may
 * be equal to one constant.
 *
 * The values are held by the column {@code v} of the tables {@code t_int}, {@code t_long}, {@code t_decimal} and
 * {@code t_float} and mapped by the predicates {@code ex:int}, {@code ex:long}, {@code ex:decimal} and
 * {@code ex:float}; the row of a value has its position among the values, counted from one, as its identifier.
 */
public class FloatingConstantComparisonTest
{
    /**
     * A datatype with the SQL type storing its values and with the values of its table.
     *
     * @param name the local name of the datatype
     * @param literalClass the class of the canonical literals of the datatype
     * @param type the SQL type of the column
     * @param values the values of the table, as SQL writes them
     */
    private record Stored(String name, LiteralClass literalClass, String type, List<String> values)
    {
    }


    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> "
            + "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> ";

    /**
     * The datatypes with the values of their tables: the integers around the greatest ones the floating-point datatypes
     * hold exactly (2<sup>24</sup>, 2<sup>53</sup>) and at the ends of the types, the decimals around the middles of
     * the double nearest to 0.1 with its neighbours and beyond the range of the datatypes, and the special values of
     * xsd:float.
     */
    private static final List<Stored> kinds = List.of(
            new Stored("int", xsdInt, "int4",
                    List.of("-2147483648", "-16777217", "-1", "0", "3", "4", "16777216", "16777217", "16777218",
                            "16777219", "2147483583", "2147483584", "2147483647")),
            new Stored("long", xsdLong, "int8",
                    List.of("-9223372036854775808", "-9007199254740993", "0", "3", "16777217", "9007199254740992",
                            "9007199254740993", "9007199254740994", "9007199254740995", "9223372036854775295",
                            "9223372036854775296", "9223372036854775807")),
            new Stored("decimal", xsdDecimal, "numeric",
                    List.of("-0.5", "0", "0.1", "0.099999999999999998612221219218554324470460414886474609374",
                            "0.099999999999999998612221219218554324470460414886474609375",
                            "0.100000000000000012490009027033011079765856266021728515625",
                            "0.100000000000000012490009027033011079765856266021728515626",
                            "0.100000001490116119384765625", "2.5", "3", "16777217", "9007199254740993",
                            "0.0000000000000000000000000000000000000000000007", "1e-330", "1e400",
                            "340282356779733661637539395458142568448.0001")),
            new Stored("float", xsdFloat, "float4", List.of("-0.5", "0", "'-0'", "0.1", "2.5", "3", "16777216",
                    "3.4028235e38", "1e-45", "'NaN'", "'Infinity'", "'-Infinity'")));

    /**
     * Constants the values are compared with: numbers the stored types hold and numbers between their values, the
     * greatest integers the datatypes hold exactly and their neighbours, the ends of the integer types and of
     * xsd:float, and the special values.
     */
    private static final List<String> constants = List.of("0.0e0", "-0.0e0", "3.0e0", "3.5e0", "2.5e0", "1.0e-1",
            "16777216.0e0", "16777217.0e0", "9007199254740992.0e0", "9007199254740994.0e0", "2147483648.0e0",
            "9.223372036854776e18", "-9.223372036854776e18", "3.4028234663852886e38", "3.4028235677973366e38",
            "1.0e300", "-1.0e300", "4.9e-324", "1.0e-50", "'INF'^^xsd:double", "'-INF'^^xsd:double",
            "'NaN'^^xsd:double", "'3'^^xsd:float", "'3.5'^^xsd:float", "'0.1'^^xsd:float", "'16777216'^^xsd:float",
            "'16777218'^^xsd:float", "'2.1474836E9'^^xsd:float", "'9.223372E18'^^xsd:float",
            "'3.4028235E38'^^xsd:float", "'1.0E-45'^^xsd:float", "'INF'^^xsd:float", "'NaN'^^xsd:float");

    /**
     * Configuration mapping the tables.
     */
    private static SparqlDatabaseConfiguration config = null;


    /**
     * Creates the {@code floating_constant_test} schema and maps the values of every datatype by the predicate named
     * after the datatype.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema floating_constant_test");

            for(Stored kind : kinds)
            {
                String table = "floating_constant_test.t_" + kind.name();

                statement.execute("create table " + table + " (id int primary key, v " + kind.type() + " not null)");

                for(int i = 0; i < kind.values().size(); i++)
                    statement.execute(
                            "insert into " + table + " values (" + (i + 1) + ", " + kind.values().get(i) + ")");
            }
        }

        DatabaseSchema schema = new DatabaseSchema(connectionPool);

        config = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);
        config.addPrefix("ex", "http://example.org/");
        config.addIriClass(new IntegerUserIriClass("row", INT4, "http://example.org/row/"));

        TermMapping subject = config.createIriMapping("row", "id");

        for(Stored kind : kinds)
            config.addQuadMapping(new DatabaseTable("floating_constant_test", "t_" + kind.name()), null, subject,
                    config.createIriMapping("ex:" + kind.name()),
                    config.createLiteralMapping(kind.literalClass(), "v"));
    }


    /**
     * Translates the query selecting the subjects of the pattern into SQL.
     *
     * @param pattern the group graph pattern, without the braces
     * @return the generated SQL
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws LimitExceedException if the generated SQL is too long
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static String translate(String pattern)
            throws SQLException, TranslateExceptions, LimitExceedException, ServiceException
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


    /**
     * The value of a lexical form of xsd:float or xsd:double.
     *
     * @param form the lexical form
     * @param single true to read the form as an xsd:float, which is then an xsd:double as it is
     * @return the value
     */
    private static double parse(String form, boolean single)
    {
        return switch(form)
        {
            case "INF" -> Double.POSITIVE_INFINITY;
            case "-INF" -> Double.NEGATIVE_INFINITY;
            default -> single ? Float.parseFloat(form) : Double.parseDouble(form);
        };
    }


    /**
     * The stored value converted to the floating-point datatype it is compared in, as the conversions of XPath round
     * it: an integer and a decimal to the nearest value of the datatype, an xsd:float being an xsd:double as it is.
     *
     * @param kind the datatype of the value
     * @param form the canonical lexical form of the value
     * @param single true when the comparison takes xsd:float, false for xsd:double
     * @return the converted value
     */
    private static double convert(Stored kind, String form, boolean single)
    {
        return switch(kind.name())
        {
            case "int", "long" -> single ? (float) Long.parseLong(form) : (double) Long.parseLong(form);
            case "float" -> parse(form, true);
            default -> parse(form, single);
        };
    }


    @Test
    @DisplayName("an integer is compared with the integers the constant lies between")
    void integers() throws Exception
    {
        String equal = translate("?s ex:int ?v FILTER(?v = 3.0e0)");
        String less = translate("?s ex:int ?v FILTER(?v < 3.5e0)");
        String reversed = translate("?s ex:int ?v FILTER(3.5e0 >= ?v)");

        assertThat(equal, containsString("(\"v\" = '3'::int4)"));
        assertThat(less, containsString("(\"v\" < '4'::int4)"));
        assertThat(reversed, containsString("(\"v\" <= '3'::int4)"));

        assertThat(subjects("?s ex:int ?v FILTER(?v = 3.0e0)"), contains(5));
        assertThat(subjects("?s ex:int ?v FILTER(?v < 3.5e0)"), contains(1, 2, 3, 4, 5));
        assertThat(subjects("?s ex:int ?v FILTER(?v = 3.5e0)"), is(empty()));

        // beyond the integers the datatype holds exactly, a constant is equal to every integer rounded to it
        String rounded = translate("?s ex:long ?v FILTER(?v = 9007199254740992.0e0)");
        String single = translate("?s ex:int ?v FILTER(?v > '16777216'^^xsd:float)");

        assertThat(rounded, containsString("\"v\" >= '9007199254740992'::int8"));
        assertThat(rounded, containsString("\"v\" <= '9007199254740993'::int8"));
        assertThat(single, containsString("(\"v\" > '16777217'::int4)"));

        assertThat(subjects("?s ex:long ?v FILTER(?v = 9007199254740992.0e0)"), contains(6, 7));
        assertThat(subjects("?s ex:long ?v FILTER(?v != 9007199254740992.0e0)"),
                contains(1, 2, 3, 4, 5, 8, 9, 10, 11, 12));
        assertThat(subjects("?s ex:int ?v FILTER(?v = '16777216'^^xsd:float)"), contains(7, 8));
        assertThat(subjects("?s ex:int ?v FILTER(?v > '16777216'^^xsd:float)"), contains(9, 10, 11, 12, 13));

        // a constant beyond the range of the type
        assertThat(subjects("?s ex:long ?v FILTER(?v = 9.223372036854776e18)"), contains(11, 12));
        assertThat(subjects("?s ex:int ?v FILTER(?v < 1.0e300)"), contains(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13));

        for(String sql : List.of(equal, less, reversed, rounded, single))
            assertThat(sql, not(containsString("sparql.cast_as_")));
    }


    @Test
    @DisplayName("a decimal is compared with the middles of the constant and its neighbours")
    void decimals() throws Exception
    {
        String equal = translate("?s ex:decimal ?v FILTER(?v = 2.5e0)");
        String less = translate("?s ex:decimal ?v FILTER(?v < 1.0e-1)");

        assertThat(equal, containsString("\"v\" >= '2.4999999999999997779553950749686919152736663818359375'::numeric"));
        assertThat(equal, containsString("\"v\" <= '2.5000000000000002220446049250313080847263336181640625'::numeric"));
        assertThat(less,
                containsString("(\"v\" < '0.099999999999999998612221219218554324470460414886474609375'::numeric)"));

        for(String sql : List.of(equal, less))
            assertThat(sql, not(containsString("sparql.cast_as_")));

        // every decimal rounded to the double is equal to it, the middles included, as its last bit is zero
        assertThat(subjects("?s ex:decimal ?v FILTER(?v = 1.0e-1)"), contains(3, 5, 6));
        assertThat(subjects("?s ex:decimal ?v FILTER(?v < 1.0e-1)"), contains(1, 2, 4, 13, 14));
        assertThat(subjects("?s ex:decimal ?v FILTER(?v > 1.0e-1)"), contains(7, 8, 9, 10, 11, 12, 15, 16));

        // the xsd:float nearest to 0.1 is another number, to which more decimals are rounded
        assertThat(subjects("?s ex:decimal ?v FILTER(?v = '0.1'^^xsd:float)"), contains(3, 4, 5, 6, 7, 8));

        // the decimals beyond the range of the datatype are rounded to an infinity
        assertThat(subjects("?s ex:decimal ?v FILTER(?v = 'INF'^^xsd:double)"), contains(15));
        assertThat(subjects("?s ex:decimal ?v FILTER(?v = 'INF'^^xsd:float)"), contains(15, 16));
        assertThat(subjects("?s ex:decimal ?v FILTER(?v = 0.0e0)"), contains(2, 14));
    }


    @Test
    @DisplayName("an xsd:float is compared with the values of xsd:float nearest to an xsd:double")
    void floats() throws Exception
    {
        String equal = translate("?s ex:float ?v FILTER(?v = 2.5e0)");
        String less = translate("?s ex:float ?v FILTER(?v < 1.0e-1)");
        String beyond = translate("?s ex:float ?v FILTER(?v > 1.0e300)");

        assertThat(equal, containsString("(\"v\" operator(sparql.=) '2.5E0'::float4)"));
        assertThat(less, containsString("(\"v\" operator(sparql.<) '1.0E-1'::float4)"));
        assertThat(beyond, containsString("(\"v\" operator(sparql.>) '3.4028235E38'::float4)"));

        for(String sql : List.of(equal, less, beyond))
            assertThat(sql, not(containsString("sparql.cast_as_")));

        assertThat(subjects("?s ex:float ?v FILTER(?v = 2.5e0)"), contains(5));
        assertThat(subjects("?s ex:float ?v FILTER(?v > 1.0e300)"), contains(11));

        // the double 0.1 is no xsd:float: the nearest one is greater
        assertThat(subjects("?s ex:float ?v FILTER(?v = 1.0e-1)"), is(empty()));
        assertThat(subjects("?s ex:float ?v FILTER(?v < 1.0e-1)"), contains(1, 2, 3, 9, 12));
        assertThat(subjects("?s ex:float ?v FILTER(?v > 1.0e-1)"), contains(4, 5, 6, 7, 8, 11));

        // NaN is comparable with nothing, so it is different from everything
        assertThat(subjects("?s ex:float ?v FILTER(?v != 1.0e-1)"), contains(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12));
        assertThat(subjects("?s ex:float ?v FILTER(?v <= 'INF'^^xsd:double)"),
                contains(1, 2, 3, 4, 5, 6, 7, 8, 9, 11, 12));
        assertThat(subjects("?s ex:float ?v FILTER(?v = 'NaN'^^xsd:double)"), is(empty()));
    }


    @Test
    @DisplayName("a value stored in the type the comparison takes is compared as before")
    void sameType() throws Exception
    {
        assertThat(translate("?s ex:float ?v FILTER(?v = '2.5'^^xsd:float)"),
                containsString("(\"v\" operator(sparql.=) '2.5E0'::float4)"));

        assertThat(subjects("?s ex:float ?v FILTER(?v = '2.5'^^xsd:float)"), contains(5));
        assertThat(subjects("?s ex:float ?v FILTER(?v = 2.5)"), contains(5));
        assertThat(subjects("?s ex:float ?v FILTER(?v > 3)"), contains(7, 8, 11));
    }


    @Test
    @DisplayName("every comparison of a stored value with a constant has the result of the comparison after conversion")
    void results() throws Exception
    {
        for(Stored kind : kinds)
        {
            for(String constant : constants)
            {
                // a comparison with an xsd:float takes that datatype, as no value is stored as an xsd:double
                boolean single = constant.endsWith("^^xsd:float");
                String form = constant.startsWith("'") ? constant.substring(1, constant.lastIndexOf('\'')) : constant;
                double right = parse(form, single);

                String query = """
                        SELECT ?v (?v = %1$s AS ?a) (?v != %1$s AS ?b) (?v < %1$s AS ?c) (?v <= %1$s AS ?d) \
                        (?v > %1$s AS ?e) (?v >= %1$s AS ?f) (%1$s = ?v AS ?g) (%1$s != ?v AS ?h) \
                        (%1$s < ?v AS ?i) (%1$s <= ?v AS ?j) (%1$s > ?v AS ?k) (%1$s >= ?v AS ?l) \
                        WHERE { ?s ex:%2$s ?v }""";

                List<List<RdfTerm>> rows = execute(query.formatted(constant, kind.name()));

                assertThat(rows.size(), is(kind.values().size()));

                for(List<RdfTerm> row : rows)
                {
                    double left = convert(kind, ((Literal) row.get(0)).getValue(), single);
                    String reason = row.get(0) + " compared with " + constant;

                    assertThat(reason, row.subList(1, 13),
                            contains(bool(left == right), bool(left != right), bool(left < right), bool(left <= right),
                                    bool(left > right), bool(left >= right), bool(right == left), bool(right != left),
                                    bool(right < left), bool(right <= left), bool(right > left), bool(right >= left)));
                }
            }
        }
    }
}
