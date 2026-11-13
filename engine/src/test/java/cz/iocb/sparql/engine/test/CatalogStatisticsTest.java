package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.database.SqlType.VARCHAR;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.lessThan;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.CatalogStatistics;
import cz.iocb.sparql.engine.database.Condition;
import cz.iocb.sparql.engine.database.Conditions;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.database.ExpressionColumn;
import cz.iocb.sparql.engine.database.GroupedStatistics;
import cz.iocb.sparql.engine.database.SqlCode;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.database.TableStatistics;
import cz.iocb.sparql.engine.database.ValueColumn;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.testing.Database;



/**
 * Statistics read from the database: the catalog statistics of an analysed table give its rows, the frequency of a most
 * common value, the average frequency of the other values and the distinct values of its columns; the grouped
 * statistics read the side tables for a bound group column and fall back to the catalog otherwise; the configuration
 * serves the registered statistics and the catalog statistics of other tables.
 */
public class CatalogStatisticsTest
{
    /**
     * The table of 1000 rows: {@code p} holds 'a' in 900 rows and 'b' in 100, {@code s} is 0 in 500 rows and distinct
     * otherwise, {@code o} ranges over 10 values.
     */
    private static final DatabaseTable table = new DatabaseTable("statistics_test", "triples");

    /**
     * The statistics of the table per value of {@code p}.
     */
    private static final DatabaseTable statisticsTable = new DatabaseTable("statistics_test", "triples_statistics");

    /**
     * The most common values of the table per value of {@code p}.
     */
    private static final DatabaseTable valuesTable = new DatabaseTable("statistics_test", "triples_values");

    private static final TableColumn p = new TableColumn("p", VARCHAR);

    private static final TableColumn s = new TableColumn("s", INT4);

    private static final TableColumn o = new TableColumn("o", INT4);

    /**
     * Configuration of the test database.
     */
    private static SparqlDatabaseConfiguration config;


    /**
     * Creates the {@code statistics_test} schema: the table, analysed, and its side tables computed by SQL.
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema statistics_test");
            statement.execute("create table statistics_test.triples (p varchar not null, s integer not null, "
                    + "o integer not null)");
            statement.execute("insert into statistics_test.triples select case when i <= 900 then 'a' else 'b' end, "
                    + "case when i % 2 = 0 then 0 else i end, i % 10 from generate_series(1, 1000) as i");
            statement.execute("analyze statistics_test.triples");
            statement.execute("""
                    create table statistics_test.triples_statistics as select p as key_value, \
                    column_name, row_count, distinct_count, effective_count from (select p, 's' as column_name, \
                    sum(c) as row_count, count(*) as distinct_count, sum(c)^2 / sum(c * c) as effective_count \
                    from (select p, s, count(*) as c from statistics_test.triples group by p, s) t group by p \
                    union all select p, 'o', sum(c), count(*), sum(c)^2 / sum(c * c) from (select p, o, count(*) \
                    as c from statistics_test.triples group by p, o) t group by p) u""");
            statement.execute("""
                    create table statistics_test.triples_values as select p as key_value, 's' as \
                    column_name, s::text as value, count(*) as row_count from statistics_test.triples group by p, \
                    s having count(*) > 1""");
        }

        config = new SparqlDatabaseConfiguration(null, connectionPool, new DatabaseSchema(connectionPool), true);
        config.addTableStatistics(table, new GroupedStatistics(table, p, statisticsTable, valuesTable));
    }


    private static Conditions equal(TableColumn column, String value)
    {
        Condition condition = new Condition();
        condition.addAreEqual(column, new ValueColumn(value, column.getType()));

        return new Conditions(condition);
    }


    private static Conditions equal(TableColumn column1, String value1, TableColumn column2, String value2)
    {
        Condition condition = new Condition();
        condition.addAreEqual(column1, new ValueColumn(value1, column1.getType()));
        condition.addAreEqual(column2, new ValueColumn(value2, column2.getType()));

        return new Conditions(condition);
    }


    @Test
    public void catalogRows() throws SQLException
    {
        TableStatistics statistics = new CatalogStatistics(table);

        try(Request request = new Engine(config).getRequest())
        {
            assertThat(statistics.estimateRows(request, new Conditions(true)), closeTo(1000, 1));
            assertThat(statistics.estimateRows(request, equal(p, "a")), closeTo(900, 10));
            assertThat(statistics.estimateRows(request, equal(s, "0")), closeTo(500, 10));

            // a value outside the most common ones gets the average of the remaining ones
            double other = statistics.estimateRows(request, equal(s, "7"));
            assertThat(other, greaterThan(0.5));
            assertThat(other, lessThan(2.0));

            // the planner multiplies the selectivities of the columns
            assertThat(statistics.estimateRows(request, equal(p, "a", s, "0")), closeTo(450, 10));
            assertThat(statistics.estimateRows(request, equal(o, "3")), closeTo(100, 10));
        }
    }


    @Test
    public void catalogDistinct() throws SQLException
    {
        TableStatistics statistics = new CatalogStatistics(table);

        try(Request request = new Engine(config).getRequest())
        {
            assertThat(statistics.estimateDistinct(request, o, new Conditions(true)), closeTo(10, 0.5));
            assertThat(statistics.estimateDistinct(request, p, new Conditions(true)), closeTo(2, 0.5));
            assertThat(statistics.estimateDistinct(request, p, equal(p, "a")), closeTo(1, 0));

            // 501 distinct values of s, half of the rows sharing one of them, corrected for the skew
            double distinct = statistics.estimateDistinct(request, s, new Conditions(true));
            assertThat(distinct, greaterThan(40.0));
            assertThat(distinct, lessThan(501.0));

            // capped by the rows
            assertThat(statistics.estimateDistinct(request, s, equal(o, "3")), lessThan(101.0));
        }
    }


    @Test
    public void groupedStatistics() throws SQLException
    {
        TableStatistics statistics = config.getTableStatistics(table);

        try(Request request = new Engine(config).getRequest())
        {
            // the groups
            assertThat(statistics.estimateRows(request, equal(p, "a")), closeTo(900, 0));
            assertThat(statistics.estimateRows(request, equal(p, "b")), closeTo(100, 0));
            assertThat(statistics.estimateRows(request, equal(p, "c")), closeTo(0, 0));

            // a most common value of the group and another value: 450 rows of s = 0 among 'a', the other 450 rows
            // over the other 450 values
            assertThat(statistics.estimateRows(request, equal(p, "a", s, "0")), closeTo(450, 0));
            assertThat(statistics.estimateRows(request, equal(p, "a", s, "7")), closeTo(1, 1e-9));

            // a column without most common values kept: the average
            assertThat(statistics.estimateRows(request, equal(p, "b", o, "3")), closeTo(10, 1e-9));

            // distinct values within the group, corrected for the skew and capped by the rows
            assertThat(statistics.estimateDistinct(request, o, equal(p, "b")), closeTo(10, 1e-9));
            assertThat(statistics.estimateDistinct(request, s, equal(p, "b", o, "3")), closeTo(10, 1e-9));
            assertThat(statistics.estimateDistinct(request, p, equal(p, "b")), closeTo(1, 0));

            // without the group column the catalog answers
            assertThat(statistics.estimateRows(request, new Conditions(true)), closeTo(1000, 1));
            assertThat(statistics.estimateDistinct(request, p, new Conditions(true)), closeTo(2, 0.5));
        }
    }


    @Test
    public void statisticsOfTheConfiguration() throws SQLException
    {
        DatabaseTable other = new DatabaseTable("statistics_test", "triples_values");

        assertThat(config.getTableStatistics(other), instanceOf(CatalogStatistics.class));
        assertThat(config.getTableStatistics(table), instanceOf(GroupedStatistics.class));

        try(Request request = new Engine(config).getRequest())
        {
            Condition condition = new Condition();
            condition.addAreEqual(new TableColumn("column_name", VARCHAR),
                    new ExpressionColumn(new SqlCode("'s'::text"), VARCHAR));

            assertThat(config.getTableStatistics(other).estimateRows(request, new Conditions(condition)),
                    greaterThan(0.0));
        }
    }
}
