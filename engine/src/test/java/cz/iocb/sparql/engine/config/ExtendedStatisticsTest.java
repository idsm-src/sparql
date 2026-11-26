package cz.iocb.sparql.engine.config;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.sql.DataSource;
import org.apache.tomcat.jdbc.pool.PoolConfiguration;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.config.ExtendedStatistics.Definition;
import cz.iocb.sparql.engine.database.Conditions;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.mapping.ConstantIriMapping;
import cz.iocb.sparql.engine.mapping.JoinTableQuadMapping.JoinColumns;
import cz.iocb.sparql.engine.mapping.classes.GenericUserIriClass;
import cz.iocb.sparql.engine.mapping.classes.IntegerUserIriClass;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.testing.Database;



/**
 * The extended statistics a configuration needs and the statements making them: the columns of a table holding a term
 * of a quad mapping together and the columns joining the tables of a join mapping need statistics, unless they are a
 * single column, hold an expression or hold a unique key of the table, and the statistics of a set of columns are kept
 * by those of a larger set; the statements make the statistics, the function a role that does not own the tables reads
 * them through and nothing more when they are run again, drop the statistics they have made before that are no longer
 * needed, leave the other statistics alone and refuse an object of a needed name defined otherwise; the command line
 * prints the script of a configuration class.
 */
public class ExtendedStatisticsTest
{
    /**
     * The schema of the test tables.
     */
    private static final String schema = "extended_statistics_test";

    /**
     * The role that may read the tables but does not own them.
     */
    private static final String readerRole = "extended_statistics_reader";

    /**
     * A table name long enough to make the name of its statistics longer than an identifier.
     */
    private static final String longName = "a_table_with_a_long_name_holding_composite_iris";

    /**
     * The pool of the connections of the owner of the tables.
     */
    private static org.apache.tomcat.jdbc.pool.DataSource pool = null;

    /**
     * Pool of the connections of the role that does not own the tables.
     */
    private static org.apache.tomcat.jdbc.pool.DataSource readerPool = null;


    /**
     * Configuration of the test tables, created as the endpoint creates a configuration, with the function reading the
     * statistics named.
     */
    public static class Mapping extends SparqlDatabaseConfiguration
    {
        /**
         * Creates the configuration.
         *
         * @param service the service IRI
         * @param pool the connection pool of the database
         * @param schema the database schema
         * @throws SQLException on database errors
         */
        public Mapping(String service, DataSource pool, DatabaseSchema schema) throws SQLException
        {
            super(service, pool, schema, true);

            addPrefix("ex", "http://example.org/");
            addIriClass(new GenericUserIriClass("item", ExtendedStatisticsTest.schema, "item", List.of(INT4, INT4),
                    "http://example\\.org/item/[0-9]+-[0-9]+"));
            addIriClass(new GenericUserIriClass("entry", ExtendedStatisticsTest.schema, "entry",
                    List.of(INT4, INT4, INT4), "http://example\\.org/entry/[0-9]+-[0-9]+-[0-9]+"));
            addIriClass(new IntegerUserIriClass("label", INT4, "http://example.org/label/"));

            ConstantIriMapping graph = createIriMapping("<http://example.org/graph>");
            DatabaseTable items = table("items");
            DatabaseTable refs = table("refs");
            DatabaseTable entries = table("entries");
            DatabaseTable labels = table("labels");
            DatabaseTable links = table("links");
            DatabaseTable targets = table("targets");
            DatabaseTable named = table(longName);

            addQuadMapping(items, graph, createIriMapping("item", "kind", "serial"), createIriMapping("ex:label"),
                    createLiteralMapping(xsdString, "label"));
            addQuadMapping(refs, graph, createIriMapping("item", "kind", "serial"), createIriMapping("ex:weight"),
                    createLiteralMapping(xsdInt, "weight"));
            addQuadMapping(refs, graph, createIriMapping("item", "(kind + 0)::int4", "serial"),
                    createIriMapping("ex:shifted"), createLiteralMapping(xsdInt, "weight"));
            addQuadMapping(entries, graph, createIriMapping("entry", "kind", "serial", "version"),
                    createIriMapping("ex:note"), createLiteralMapping(xsdString, "note"));
            addQuadMapping(entries, graph, createIriMapping("item", "kind", "serial"), createIriMapping("ex:entry"),
                    createIriMapping("entry", "kind", "serial", "version"));
            addQuadMapping(labels, graph, createIriMapping("label", "id"), createIriMapping("ex:label"),
                    createLiteralMapping(xsdString, "label"));
            addQuadMapping(List.of(links, targets),
                    List.of(new JoinColumns(List.of(getColumn(links, "kind"), getColumn(links, "serial")),
                            List.of(getColumn(targets, "kind"), getColumn(targets, "serial")))),
                    graph, createIriMapping("item", "kind", "serial"), createIriMapping("ex:target"),
                    createLiteralMapping(xsdString, "name"));
            addQuadMapping(named, graph, createIriMapping("item", "kind", "serial"), createIriMapping("ex:named"),
                    createLiteralMapping(xsdString, "name"));

            setExtendedStatisticsFunction(ExtendedStatisticsTest.schema, "ndistinct");
        }
    }


    /**
     * Configuration of a table one of whose needed statistics objects exists with another definition.
     */
    public static class Conflict extends SparqlDatabaseConfiguration
    {
        /**
         * Creates the configuration.
         *
         * @param service the service IRI
         * @param pool the connection pool of the database
         * @param schema the database schema
         * @throws SQLException on database errors
         */
        public Conflict(String service, DataSource pool, DatabaseSchema schema) throws SQLException
        {
            super(service, pool, schema, true);

            addIriClass(new GenericUserIriClass("item", ExtendedStatisticsTest.schema, "item", List.of(INT4, INT4),
                    "http://example\\.org/item/[0-9]+-[0-9]+"));

            addQuadMapping(table("conflicts"), null, createIriMapping("item", "kind", "serial"),
                    createIriMapping("<http://example.org/weight>"), createLiteralMapping(xsdInt, "weight"));
        }
    }


    /**
     * Creates the {@code extended_statistics_test} schema: the functions of the IRI classes of the items and of the
     * entries; the items, keyed by their kind and serial number; three thousand references to three hundred items of
     * three kinds and a hundred serial numbers; entries, labels, links to targets keyed by the kind and the serial
     * number; a table of a long name; a table of the conflict; and the role that does not own the tables, which may use
     * the schemas of the extension and of the tables and read the tables.
     */
    @BeforeAll
    static void init() throws SQLException
    {
        pool = Database.getPool();

        try(Connection connection = pool.getConnection(); Statement statement = connection.createStatement())
        {
            connection.setAutoCommit(true);
            statement.execute("create schema " + schema);
            statement.execute("create function " + schema + ".item(kind int4, serial int4) returns varchar "
                    + "language sql immutable as $$ select ('http://example.org/item/' || kind || '-' || serial)"
                    + "::varchar $$");
            statement.execute("create function " + schema + ".item_inv1(iri varchar) returns int4 language sql "
                    + "immutable as $$ select split_part(substring(iri, 25), '-', 1)::int4 $$");
            statement.execute("create function " + schema + ".item_inv2(iri varchar) returns int4 language sql "
                    + "immutable as $$ select split_part(substring(iri, 25), '-', 2)::int4 $$");
            statement.execute("create function " + schema + ".entry(kind int4, serial int4, version int4) "
                    + "returns varchar language sql immutable as $$ select ('http://example.org/entry/' || kind "
                    + "|| '-' || serial || '-' || version)::varchar $$");

            for(int i = 1; i <= 3; i++)
                statement.execute("create function " + schema + ".entry_inv" + i + "(iri varchar) returns int4 "
                        + "language sql immutable as $$ select split_part(substring(iri, 26), '-', " + i
                        + ")::int4 $$");

            statement.execute("create table " + schema + ".items (kind int4 not null, serial int4 not null, "
                    + "label varchar not null, primary key (kind, serial))");
            statement.execute("create table " + schema + ".refs (kind int4 not null, serial int4 not null, "
                    + "weight int4 not null)");
            statement.execute("insert into " + schema + ".refs select i / 100 % 3 + 1, i % 100, i "
                    + "from generate_series(0, 2999) as i");
            statement.execute("create table " + schema + ".entries (kind int4 not null, serial int4 not null, "
                    + "version int4 not null, note varchar not null)");
            statement.execute("insert into " + schema + ".entries select i % 5 + 1, i, i % 2, 'entry ' || i "
                    + "from generate_series(0, 99) as i");
            statement.execute("create table " + schema + ".labels (id int4 primary key, label varchar not null)");
            statement.execute("create table " + schema + ".links (kind int4 not null, serial int4 not null, "
                    + "target int4 not null)");
            statement.execute(
                    "insert into " + schema + ".links select i % 5 + 1, i, i from generate_series(0, 99) as i");
            statement.execute("create table " + schema + ".targets (kind int4 not null, serial int4 not null, "
                    + "name varchar not null, primary key (kind, serial))");
            statement.execute("create table " + schema + "." + longName + " (kind int4 not null, "
                    + "serial int4 not null, name varchar not null)");
            statement.execute("insert into " + schema + "." + longName + " select i % 5 + 1, i, 'name ' || i "
                    + "from generate_series(0, 99) as i");
            statement.execute("create table " + schema + ".conflicts (kind int4 not null, serial int4 not null, "
                    + "weight int4 not null)");
            statement.execute("do $$ begin create role " + readerRole + " nologin; "
                    + "exception when duplicate_object or unique_violation then null; end $$");
            statement.execute("grant usage on schema sparql, " + schema + " to " + readerRole);
            statement.execute("grant select on all tables in schema " + schema + " to " + readerRole);
        }

        PoolConfiguration shared = pool.getPoolProperties();
        PoolProperties properties = new PoolProperties();
        properties.setUrl(shared.getUrl());
        properties.setUsername(shared.getUsername());
        properties.setPassword(shared.getPassword());
        properties.setDriverClassName(shared.getDriverClassName());
        properties.setInitSQL("set role " + readerRole);

        readerPool = new org.apache.tomcat.jdbc.pool.DataSource(properties);
    }


    /**
     * Closes the pool of the connections of the role that does not own the tables.
     */
    @AfterAll
    static void close()
    {
        if(readerPool != null)
            readerPool.close();
    }


    /**
     * The test table of the name.
     */
    private static DatabaseTable table(String name)
    {
        return new DatabaseTable(schema, name);
    }


    /**
     * The statistics of the test configuration, whose function is granted to the role that does not own the tables.
     */
    private static ExtendedStatistics getStatistics() throws SQLException
    {
        return new ExtendedStatistics(new Mapping(null, pool, new DatabaseSchema(pool)), List.of(readerRole));
    }


    /**
     * Executes the statements on a connection of the owner of the tables, each committed at once, as the requests of
     * the tests leave their connections in the pool without the automatic commits.
     */
    private static void execute(String... statements) throws SQLException
    {
        try(Connection connection = pool.getConnection(); Statement statement = connection.createStatement())
        {
            connection.setAutoCommit(true);

            for(String sql : statements)
                statement.execute(sql);
        }
    }


    /**
     * Runs the statements on a connection of the owner of the tables.
     */
    private static void apply(ExtendedStatistics statistics) throws SQLException
    {
        try(Connection connection = pool.getConnection())
        {
            statistics.apply(connection);
        }
    }


    /**
     * The values of the first column of the rows of the query, as text.
     */
    private static List<String> query(String sql) throws SQLException
    {
        List<String> values = new ArrayList<>();

        try(Connection connection = pool.getConnection();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(sql))
        {
            while(result.next())
                values.add(result.getString(1));
        }

        return values;
    }


    /**
     * The statistics objects of the schema the statements have made, by name with the oid.
     */
    private static List<String> getMarked() throws SQLException
    {
        return query("select stxname || ' ' || oid from pg_statistic_ext where stxnamespace = '" + schema
                + "'::regnamespace and obj_description(oid, 'pg_statistic_ext') = '" + ExtendedStatistics.MARKER
                + "' order by stxname");
    }


    @Test
    public void definitions() throws SQLException
    {
        List<Definition> definitions = getStatistics().getDefinitions();

        assertEquals(4, definitions.size());

        // the long name is cut and ended by a hash
        Definition named = definitions.get(0);
        assertEquals(table(longName), named.table());
        assertEquals(List.of("kind", "serial"), named.columns());
        assertEquals(63, named.name().getBytes(UTF_8).length);
        assertThat(named.name(), startsWith(longName + "_kind_"));

        // the columns of the entries and of the items they refer to by the first two of them
        assertEquals(new Definition(table("entries"), "entries_kind_serial_version_stat",
                List.of("kind", "serial", "version")), definitions.get(1));

        // the columns of an item and the columns joining the links to the targets, of which they are a key
        assertEquals(new Definition(table("links"), "links_kind_serial_stat", List.of("kind", "serial")),
                definitions.get(2));

        // a term held in an expression needs none
        assertEquals(new Definition(table("refs"), "refs_kind_serial_stat", List.of("kind", "serial")),
                definitions.get(3));

        // the items, keyed by their columns, a single column and the targets need none, but they are mapped
        assertEquals(List.of(table(longName), table("entries"), table("items"), table("labels"), table("links"),
                table("refs"), table("targets")), getStatistics().getTables());
    }


    @Test
    public void statistics() throws Exception
    {
        apply(getStatistics());

        List<String> marked = getMarked();
        assertEquals(4, marked.size());

        // the statistics are of both kinds and computed
        assertEquals(List.of("{d,f}"),
                query("select distinct stxkind::text from pg_statistic_ext where stxnamespace = '" + schema
                        + "'::regnamespace and obj_description(oid, 'pg_statistic_ext') = '" + ExtendedStatistics.MARKER
                        + "'"));
        assertEquals(List.of("4"), query("select count(*) from pg_stats_ext e join pg_statistic_ext x "
                + "on x.stxnamespace = e.statistics_schemaname::text::regnamespace and x.stxname = e.statistics_name "
                + "where e.statistics_schemaname = '" + schema + "' and e.n_distinct is not null "
                + "and obj_description(x.oid, 'pg_statistic_ext') = '" + ExtendedStatistics.MARKER + "'"));

        // nothing is made again
        apply(getStatistics());
        assertEquals(marked, getMarked());

        // the role that does not own the tables reads the 300 combinations of the references through the function
        Mapping reader = new Mapping(null, readerPool, new DatabaseSchema(pool));

        try(Request request = new Engine(reader).getRequest())
        {
            assertThat(reader.getTableStatistics(table("refs")).estimateDistinct(request,
                    Set.of(new TableColumn("kind", INT4), new TableColumn("serial", INT4)), new Conditions(true)),
                    closeTo(300, 1));
        }
    }


    @Test
    public void cleanup() throws Exception
    {
        apply(getStatistics());

        execute("create statistics " + schema + ".refs_kind_weight_stat (ndistinct) on kind, weight from " + schema
                + ".refs",
                "comment on statistics " + schema + ".refs_kind_weight_stat is '" + ExtendedStatistics.MARKER + "'",
                "create statistics " + schema + ".refs_manual (mcv) on serial, weight from " + schema + ".refs");

        apply(getStatistics());

        // the statistics made before that are no longer needed are dropped, the others left alone
        assertEquals(List.of(), query("select stxname from pg_statistic_ext where stxname = 'refs_kind_weight_stat'"));
        assertEquals(List.of("refs_manual"),
                query("select stxname from pg_statistic_ext where stxname = 'refs_manual'"));
        assertEquals(4, getMarked().size());
    }


    @Test
    public void conflict() throws Exception
    {
        execute("create statistics " + schema + ".conflicts_kind_serial_stat (ndistinct) on kind, weight from " + schema
                + ".conflicts");

        ExtendedStatistics statistics = new ExtendedStatistics(new Conflict(null, pool, new DatabaseSchema(pool)),
                List.of());

        SQLException exception = assertThrows(SQLException.class, () -> apply(statistics));
        assertThat(exception.getMessage(), containsString("exists with another definition"));
    }


    @Test
    public void script() throws SQLException
    {
        ExtendedStatistics statistics = getStatistics();
        String script = statistics.getScript();

        assertThat(script, startsWith("-- "));
        assertThat(script, containsString("\n\\set ON_ERROR_STOP on\n\nBEGIN;\n\n"));
        assertThat(script, endsWith(";\n\nCOMMIT;\n"));

        for(String statement : statistics.getStatements())
            assertThat(script, containsString("\n" + statement + ";\n"));
    }


    @Test
    public void commandLine() throws Exception
    {
        PoolConfiguration properties = pool.getPoolProperties();
        String url = properties.getUrl() + (properties.getUrl().contains("?") ? "&" : "?") + "user="
                + URLEncoder.encode(properties.getUsername(), UTF_8) + "&password="
                + URLEncoder.encode(properties.getPassword(), UTF_8);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream original = System.out;

        try(PrintStream stream = new PrintStream(output, true, UTF_8))
        {
            System.setOut(stream);
            ExtendedStatistics.main(new String[] { "--reader", readerRole, "--function", schema + ".ndistinct",
                    Mapping.class.getName(), url });
        }
        finally
        {
            System.setOut(original);
        }

        assertEquals(getStatistics().getScript(), output.toString(UTF_8));
    }
}
