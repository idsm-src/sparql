package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.database.SqlType.VARCHAR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.Set;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.Condition;
import cz.iocb.sparql.engine.database.Conditions;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.database.Function;
import cz.iocb.sparql.engine.database.GroupedStatistics;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.database.TableStatistics;
import cz.iocb.sparql.engine.database.ValueColumn;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.testing.Database;



/**
 * Statistics of the tables renewed by the configuration: the check of the tables the statistics are read from, made at
 * the first statistics a request takes and at most once per interval, renews the statistics of a table that has been
 * analysed, those of a table whose side tables have been written to and those whose lookup has failed, but not the
 * statistics of an unchanged table, and a request keeps the statistics it has begun with.
 */
public class StatisticsRenewalTest
{
    private static final TableColumn p = new TableColumn("p", VARCHAR);

    private static final TableColumn a = new TableColumn("a", INT4);

    private static final TableColumn b = new TableColumn("b", INT4);


    /**
     * Creates the {@code statistics_renewal_test} schema, in which every test creates its own tables.
     */
    @BeforeAll
    static void init() throws SQLException
    {
        execute("create schema statistics_renewal_test");
    }


    /**
     * Runs the statements and has the counts of the rows they have written flushed, so that the requests see at once
     * the changes and the counts.
     */
    private static void execute(String... statements) throws SQLException
    {
        try(Connection connection = Database.getPool().getConnection();
                Statement statement = connection.createStatement())
        {
            connection.setAutoCommit(true);

            for(String sql : statements)
                statement.execute(sql);

            statement.execute("select pg_stat_force_next_flush()");
        }
    }


    /**
     * Creates a table of the column {@code a} holding the numbers from 1 to the given count, which autovacuum leaves
     * alone, and analyses it.
     */
    private static DatabaseTable createTable(String name, int count) throws SQLException
    {
        execute("create table statistics_renewal_test." + name + " (a int4 not null) with (autovacuum_enabled = false)",
                "insert into statistics_renewal_test." + name + " select generate_series(1, " + count + ")",
                "analyze statistics_renewal_test." + name);

        return new DatabaseTable("statistics_renewal_test", name);
    }


    /**
     * Adds rows to the table created by {@link #createTable}, up to the given count, and analyses it.
     */
    private static void growTable(DatabaseTable table, int count) throws SQLException
    {
        execute("insert into " + table + " select generate_series((select max(a) + 1 from " + table + "), " + count
                + ")", "analyze " + table);
    }


    /**
     * Creates a configuration checking the statistics at most once per the interval.
     */
    private static SparqlDatabaseConfiguration configure(Duration interval) throws SQLException
    {
        DataSource pool = Database.getPool();
        DatabaseSchema schema = new DatabaseSchema(pool);
        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, pool, schema, true);
        config.setEngineSettings(config.getEngineSettings().withStatisticsCheckInterval(interval));

        return config;
    }


    /**
     * The statistics of the table a new request takes.
     */
    private static TableStatistics statistics(SparqlDatabaseConfiguration config, DatabaseTable table)
            throws SQLException
    {
        try(Request request = new Engine(config).getRequest())
        {
            return request.getTableStatistics(table);
        }
    }


    /**
     * The rows of the table satisfying the conditions as a new request estimates them.
     */
    private static double rows(SparqlDatabaseConfiguration config, DatabaseTable table, Conditions conditions)
            throws SQLException
    {
        try(Request request = new Engine(config).getRequest())
        {
            return request.getTableStatistics(table).estimateRows(request, conditions);
        }
    }


    /**
     * The distinct combinations of the values of the columns {@code a} and {@code b} of the table as a new request
     * estimates them.
     */
    private static double combinations(SparqlDatabaseConfiguration config, DatabaseTable table) throws SQLException
    {
        try(Request request = new Engine(config).getRequest())
        {
            return request.getTableStatistics(table).estimateDistinct(request, Set.of(a, b), new Conditions(true));
        }
    }


    private static Conditions equal(TableColumn column1, String value1, TableColumn column2, String value2)
    {
        Condition condition = new Condition();
        condition.addAreEqual(column1, new ValueColumn(value1, column1.getType()));
        condition.addAreEqual(column2, new ValueColumn(value2, column2.getType()));

        return new Conditions(condition);
    }


    @Test
    public void analysedTable() throws SQLException
    {
        DatabaseTable table = createTable("analysed", 1000);
        SparqlDatabaseConfiguration config = configure(Duration.ZERO);

        // the statistics taken before their first check are renewed by it, as nothing tells what they have read
        TableStatistics first = statistics(config, table);
        TableStatistics checked = statistics(config, table);
        assertNotSame(first, checked);

        // the table has not changed since
        assertSame(checked, statistics(config, table));
        assertEquals(1000, rows(config, table, new Conditions(true)), 1);

        growTable(table, 2000);

        assertNotSame(checked, statistics(config, table));
        assertEquals(2000, rows(config, table, new Conditions(true)), 1);
    }


    @Test
    public void tableNeverAnalysed() throws SQLException
    {
        DatabaseTable table = new DatabaseTable("statistics_renewal_test", "unanalysed");

        execute("""
                create table statistics_renewal_test.unanalysed (a int4 not null) \
                with (autovacuum_enabled = false)""",
                "insert into statistics_renewal_test.unanalysed select generate_series(1, 5000)");

        SparqlDatabaseConfiguration config = configure(Duration.ZERO);
        statistics(config, table);
        statistics(config, table);

        // the rows of a table the catalog knows nothing about yet
        assertEquals(1000, rows(config, table, new Conditions(true)), 1);

        execute("analyze statistics_renewal_test.unanalysed");

        assertEquals(5000, rows(config, table, new Conditions(true)), 1);
    }


    @Test
    public void writtenSideTable() throws SQLException
    {
        DatabaseTable table = new DatabaseTable("statistics_renewal_test", "triples");
        DatabaseTable statisticsTable = new DatabaseTable("statistics_renewal_test", "triples_statistics");
        DatabaseTable valuesTable = new DatabaseTable("statistics_renewal_test", "triples_values");

        execute("""
                create table statistics_renewal_test.triples (p varchar not null, a int4 not null) \
                with (autovacuum_enabled = false)""",
                "insert into statistics_renewal_test.triples select 'x', i % 10 from generate_series(1, 100) as i",
                "analyze statistics_renewal_test.triples");

        execute("""
                create table statistics_renewal_test.triples_statistics (key_value varchar, column_name text, \
                row_count bigint, distinct_count float8, effective_count float8) with (autovacuum_enabled = false)""",
                "insert into statistics_renewal_test.triples_statistics values ('x', 'a', 100, 10, 10)");

        execute("""
                create table statistics_renewal_test.triples_values (key_value varchar, column_name text, \
                value text, row_count bigint) with (autovacuum_enabled = false)""",
                "insert into statistics_renewal_test.triples_values values ('x', 'a', '3', 10)");

        SparqlDatabaseConfiguration config = configure(Duration.ZERO);
        config.addTableStatistics(table, new GroupedStatistics(table, p, statisticsTable, valuesTable));
        statistics(config, table);
        statistics(config, table);

        assertEquals(10, rows(config, table, equal(p, "x", a, "3")), 1e-9);

        execute("update statistics_renewal_test.triples_values set row_count = 40");

        assertEquals(40, rows(config, table, equal(p, "x", a, "3")), 1e-9);
    }


    @Test
    public void failedLookup() throws SQLException
    {
        DatabaseTable table = new DatabaseTable("statistics_renewal_test", "pairs");

        execute("""
                create table statistics_renewal_test.pairs (a int4 not null, b int4 not null) \
                with (autovacuum_enabled = false)""",
                "insert into statistics_renewal_test.pairs select i % 10, i / 10 % 10 from generate_series(0, 999) i");

        execute("""
                create statistics statistics_renewal_test.pairs_combinations (ndistinct) on a, b \
                from statistics_renewal_test.pairs""");

        execute("analyze statistics_renewal_test.pairs");

        // the function reading the extended statistics does not exist yet
        SparqlDatabaseConfiguration config = configure(Duration.ZERO);
        config.setEngineSettings(config.getEngineSettings()
                .withStatisticsFunction(new Function("statistics_renewal_test", "ndistinct")));
        statistics(config, table);
        statistics(config, table);

        // the lookup fails, so the combinations are taken for those of a column, also when it is made again
        assertEquals(10, combinations(config, table), 0.5);
        assertEquals(10, combinations(config, table), 0.5);

        execute("""
                create function statistics_renewal_test.ndistinct(schema_name name, table_name name) \
                returns setof text language sql stable \
                as $$ select n_distinct::text from pg_stats_ext \
                where schemaname = schema_name and tablename = table_name $$""");

        // the statistics of the unchanged table are renewed, as their lookup has failed
        assertEquals(100, combinations(config, table), 0.5);
    }


    @Test
    public void requestKeepsItsStatistics() throws SQLException
    {
        DatabaseTable table = createTable("kept", 1000);
        SparqlDatabaseConfiguration config = configure(Duration.ZERO);

        try(Request request = new Engine(config).getRequest())
        {
            TableStatistics kept = request.getTableStatistics(table);
            assertEquals(1000, kept.estimateRows(request, new Conditions(true)), 1);

            growTable(table, 2000);

            // another request takes the statistics renewed meanwhile
            assertEquals(2000, rows(config, table, new Conditions(true)), 1);

            // whereas the request keeps the statistics it has begun with
            assertSame(kept, request.getTableStatistics(table));
            assertEquals(1000, kept.estimateRows(request, new Conditions(true)), 1);
        }
    }


    @Test
    public void checkedOncePerInterval() throws SQLException
    {
        DatabaseTable table = createTable("seldom", 1000);
        SparqlDatabaseConfiguration config = configure(Duration.ofHours(1));

        TableStatistics first = statistics(config, table);
        assertEquals(1000, rows(config, table, new Conditions(true)), 1);

        growTable(table, 2000);

        // the next check is due in an hour
        assertSame(first, statistics(config, table));
        assertEquals(1000, rows(config, table, new Conditions(true)), 1);
    }
}
