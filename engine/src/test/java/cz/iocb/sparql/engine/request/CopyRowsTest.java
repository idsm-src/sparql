package cz.iocb.sparql.engine.request;

import static cz.iocb.sparql.engine.database.SqlType.BOOL;
import static cz.iocb.sparql.engine.database.SqlType.DATE;
import static cz.iocb.sparql.engine.database.SqlType.FLOAT4;
import static cz.iocb.sparql.engine.database.SqlType.FLOAT8;
import static cz.iocb.sparql.engine.database.SqlType.INT2;
import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.database.SqlType.INT8;
import static cz.iocb.sparql.engine.database.SqlType.NUMERIC;
import static cz.iocb.sparql.engine.database.SqlType.RDFBOX;
import static cz.iocb.sparql.engine.database.SqlType.TIMESTAMPTZ;
import static cz.iocb.sparql.engine.database.SqlType.VARCHAR;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DateTimeException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.SqlType;
import cz.iocb.sparql.testing.Database;



/**
 * The rows of a statement that the database copies to the client (see {@link CopyRows}) give the columns as the rows of
 * a result set give them: the texts holding the characters a copy escapes, NULLs and empty texts among several columns,
 * and the values of the types of the columns of the result classes, among them the floating-point infinities and NaN,
 * the dates and the timestamps before Christ, beyond the year 9999 and at the infinities, and the timestamps of time
 * zones whose offsets have minutes or seconds; a text of another form is an error. Rows closed before their end leave
 * the request usable, and rows not closed end when the request runs another statement or is closed.
 */
public class CopyRowsTest
{
    /**
     * The time zones the timestamps are read in: offsets of whole hours, of minutes, negative ones, and offsets of
     * seconds in the past.
     */
    private static final List<String> zones = List.of("UTC", "Europe/Prague", "Asia/Kolkata", "America/St_Johns",
            "Pacific/Chatham");

    /**
     * Configuration without mappings.
     */
    private static SparqlDatabaseConfiguration config = null;


    /**
     * Reader of a column of the current row.
     */
    @FunctionalInterface
    private interface ColumnReader
    {
        /**
         * Reads the column of the current row.
         */
        Object read(ResultRows rows, int index) throws SQLException;
    }


    /**
     * Creates the configuration.
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        config = new SparqlDatabaseConfiguration(null, connectionPool, new DatabaseSchema(connectionPool), true);
    }


    /**
     * The query of the values, each an SQL expression, cast to the type, a row for each value.
     */
    private static String values(List<String> values, SqlType type)
    {
        return values.stream().map(v -> "(" + v + ")")
                .collect(Collectors.joining(", ", "SELECT v::" + type.getName() + " FROM (VALUES ", ") AS t(v)"));
    }


    /**
     * Reads the columns of all rows of the statement in the time zone, by a result set or by a copy.
     */
    private static List<Object> read(String sql, int columns, ColumnReader reader, String zone, boolean copied)
            throws SQLException
    {
        try(Request request = new Engine(config).getRequest())
        {
            request.getStatement().execute("SET LOCAL TIME ZONE '" + zone + "'");

            try(ResultRows rows = copied ? request.copy(sql) :
                    new ResultSetRows(request.getStatement().executeQuery(sql)))
            {
                List<Object> values = new ArrayList<>();

                while(rows.next())
                    for(int i = 1; i <= columns; i++)
                        values.add(reader.read(rows, i));

                return values;
            }
        }
    }


    /**
     * Checks that a copy reads the columns of all rows of the statement as a result set reads them, in every time zone
     * given, and returns what it has read in the last one.
     */
    private static List<Object> check(String sql, int columns, ColumnReader reader, List<String> zones)
            throws SQLException
    {
        List<Object> values = null;

        for(String zone : zones)
        {
            values = read(sql, columns, reader, zone, true);
            assertThat(values, is(read(sql, columns, reader, zone, false)));
        }

        return values;
    }


    /**
     * Checks that a copy reads the values as a result set reads them as values of the type, in every time zone given.
     */
    private static void check(List<String> values, SqlType type, List<String> zones) throws SQLException
    {
        assertThat(check(values(values, type), 1, (rows, i) -> rows.getValue(i, type), zones), hasSize(values.size()));
    }


    @Test
    @DisplayName("texts holding the characters a copy escapes")
    void texts() throws SQLException
    {
        List<String> values = List.of("''", "'plain'", "'tab' || chr(9) || 'here'", "'line' || chr(10) || 'feed'",
                "chr(13) || chr(10)", "'back\\slash'", "'\\N'", "'\\\\N'", "'N'", "'end\\'", "'\\t'",
                "chr(8) || chr(12) || chr(11) || chr(1) || chr(31) || chr(127)",
                "chr(269) || chr(26085) || chr(128512)", "repeat('x' || chr(9), 10000)", "NULL");

        assertThat(check(values(values, VARCHAR), 1, ResultRows::getString, List.of("UTC")), hasItem("tab\there"));
        check(values, VARCHAR, List.of("UTC"));
    }


    @Test
    @DisplayName("NULLs and empty texts among several columns")
    void columns() throws SQLException
    {
        String sql = """
                SELECT 1::int4, NULL::varchar, ''::varchar, 'a' || chr(9) || 'b', NULL::int4, \
                '-2147483648'::int4, '\\N'::varchar, NULL::varchar""";

        List<Object> texts = check(sql, 8, ResultRows::getString, List.of("UTC"));
        assertThat(texts, is(Arrays.<Object> asList("1", null, "", "a\tb", null, "-2147483648", "\\N", null)));

        assertThat(check(sql, 1, ResultRows::getInt, List.of("UTC")), is(List.<Object> of(1)));
        assertThat(check("SELECT NULL::int4, 7::int4", 2, ResultRows::getInt, List.of("UTC")),
                is(List.<Object> of(0, 7)));
    }


    @Test
    @DisplayName("booleans and integers")
    void integers() throws SQLException
    {
        check(List.of("'t'", "'f'"), BOOL, List.of("UTC"));
        check(List.of("'-32768'", "'-1'", "'0'", "'32767'"), INT2, List.of("UTC"));
        check(List.of("'-2147483648'", "'0'", "'2147483647'"), INT4, List.of("UTC"));
        check(List.of("'-9223372036854775808'", "'0'", "'9223372036854775807'"), INT8, List.of("UTC"));
    }


    @Test
    @DisplayName("decimal and floating-point numbers")
    void numbers() throws SQLException
    {
        check(List.of("'0'", "'-0.5'", "'123.4500'", "'1e-30'", "'12345678901234567890.12345678901234567890'",
                "'-1e100'"), NUMERIC, List.of("UTC"));
        check(List.of("'0'", "'-0'", "'1.5'", "'0.1'", "'3.4028235e38'", "'1.4e-45'", "'1e-40'", "'16777217'", "'NaN'",
                "'Infinity'", "'-Infinity'"), FLOAT4, List.of("UTC"));
        check(List.of("'0'", "'-0'", "'0.1'", "'1.7976931348623157e308'", "'4.9e-324'", "'1e22'",
                "'123456789012345680'", "'NaN'", "'Infinity'", "'-Infinity'"), FLOAT8, List.of("UTC"));
    }


    @Test
    @DisplayName("dates before Christ, beyond the year 9999 and at the infinities")
    void dates() throws SQLException
    {
        check(List.of("'2020-02-29'", "'0001-01-01'", "'0001-12-31 BC'", "'0044-03-15 BC'", "'4713-01-01 BC'",
                "'10000-01-01'", "'5874897-12-31'", "'infinity'", "'-infinity'"), DATE, List.of("UTC"));
    }


    @Test
    @DisplayName("timestamps in time zones of offsets of minutes and seconds")
    void timestamps() throws SQLException
    {
        check(List.of("'2020-06-01 12:34:56.789012+00'", "'1850-01-01 00:00:00+00'", "'0044-03-15 12:00:00+00 BC'",
                "'0001-01-01 00:00:00.5+00'", "'20000-01-01 00:00:00+00'", "'2020-01-01 00:00:00.000001+00'",
                "'294276-12-31 23:59:59.999999+00'", "'4713-01-01 00:00:00+00 BC'", "'infinity'", "'-infinity'"),
                TIMESTAMPTZ, zones);
    }


    @Test
    @DisplayName("boxes read as texts")
    void boxes() throws SQLException
    {
        check(List.of("'<http://example.org/a>'", "'\"tab' || chr(9) || 'here\"@en'",
                "'\"1\"^^<http://www.w3.org/2001/XMLSchema#int>'", "NULL"), RDFBOX, List.of("UTC"));
    }


    @Test
    @DisplayName("a text of another form is an error")
    void unexpectedForms()
    {
        for(String text : List.of("2020-1-01", "2020-01-01 ", "x020-01-01", "2020-01-01 AD", "-2020-01-01"))
            assertThrows(DateTimeException.class, () -> CopyRows.toDate(text));

        for(String text : List.of("2020-01-01 00:00:00", "2020-01-01 00:00:00+1", "2020-01-01 00:00:00.+00",
                "2020-01-01 00:00:00.1234567890+00", "2020-01-01 00:00:00+00:", "2020-01-01 00:00:00+00:00:00:00",
                "2020-01-01T00:00:00+00"))
            assertThrows(DateTimeException.class, () -> CopyRows.toTimestamp(text));
    }


    @Test
    @DisplayName("rows closed before their end leave the request usable, and rows not closed end")
    @Timeout(60)
    void closedBeforeEnd() throws SQLException
    {
        try(Request request = new Engine(config).getRequest())
        {
            try(ResultRows rows = request.copy("SELECT i FROM generate_series(1, 10000000) AS i"))
            {
                assertThat(rows.next(), is(true));
                assertThat(rows.getInt(1), is(1));
            }

            try(ResultRows rows = request.copy("SELECT 2"))
            {
                assertThat(rows.next(), is(true));
                assertThat(rows.getInt(1), is(2));
                assertThat(rows.next(), is(false));
                assertThat(rows.next(), is(false));
            }

            // rows not closed end when the request runs another statement
            ResultRows open = request.copy("SELECT i FROM generate_series(1, 10000000) AS i");
            assertThat(open.next(), is(true));

            try(ResultSet result = request.getStatement().executeQuery("SELECT 4"))
            {
                assertThat(result.next(), is(true));
                assertThat(result.getInt(1), is(4));
            }

            assertThat(open.next(), is(false));

            // or when it is closed
            ResultRows rows = request.copy("SELECT i FROM generate_series(1, 10000000) AS i");
            assertThrows(SQLException.class, () -> rows.getString(1));
        }

        // and the connection, which the pool gives again, can run statements
        for(int i = 0; i < 10; i++)
            assertThat(read("SELECT NULL::varchar", 1, ResultRows::getString, "UTC", true),
                    is(Arrays.<Object> asList((Object) null)));
    }
}
