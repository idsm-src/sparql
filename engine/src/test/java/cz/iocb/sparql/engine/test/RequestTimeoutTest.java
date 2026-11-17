package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.database.SqlType.VARCHAR;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.CatalogStatistics;
import cz.iocb.sparql.engine.database.Condition;
import cz.iocb.sparql.engine.database.Conditions;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.database.SQLRuntimeException;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.database.TableStatistics;
import cz.iocb.sparql.engine.database.ValueColumn;
import cz.iocb.sparql.engine.mapping.classes.IntegerUserIriClass;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.testing.Database;



/**
 * The time limit and the cancellation of a request: a request out of time fails at once with the SQL state 57014 of a
 * cancelled statement, and a cancelled one with an SQL exception, both checked, instead of giving every further
 * statement another second or escaping unchecked; and neither leaves the frequency of a value it did not look up in the
 * statistics, which the configuration keeps for the following requests.
 *
 * The analysed table {@code item} holds 1000 items, half of them of the value {@code 1}, mapped by ex:v; the join
 * optimizer, which reads the statistics, is enabled.
 */
public class RequestTimeoutTest
{
    /**
     * The table of the items.
     */
    private static final DatabaseTable table = new DatabaseTable("request_timeout_test", "item");

    /**
     * The query, a pattern the join optimizer estimates.
     */
    private static final String query = "PREFIX ex: <http://example.org/> SELECT ?s WHERE { ?s ex:v '1' }";

    /**
     * Configuration mapping the table.
     */
    private static SparqlDatabaseConfiguration config = null;


    /**
     * Creates the {@code request_timeout_test} schema with the analysed table and maps it.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema request_timeout_test");
            statement.execute("create table request_timeout_test.item (s int not null, v varchar not null)");
            statement.execute("insert into request_timeout_test.item select i, case when i % 2 = 0 then '1' else "
                    + "i::varchar end from generate_series(1, 1000) as i");
            statement.execute("analyze request_timeout_test.item");
        }

        config = new SparqlDatabaseConfiguration(null, connectionPool, new DatabaseSchema(connectionPool), true);
        config.addPrefix("ex", "http://example.org/");
        config.addIriClass(new IntegerUserIriClass("item", INT4, "http://example.org/item/"));
        config.addQuadMapping(table, null, config.createIriMapping("item", "s"), config.createIriMapping("ex:v"),
                config.createLiteralMapping(xsdString, "v"));
        config.setJoinOptimizer(true);
    }


    /**
     * The condition that the column {@code v} holds the value {@code 1}.
     *
     * @return the condition
     */
    private static Conditions valueOne()
    {
        Condition condition = new Condition();
        condition.addAreEqual(new TableColumn("v", VARCHAR), new ValueColumn("1", VARCHAR));

        return new Conditions(condition);
    }


    /**
     * The rows the statistics estimate for the value {@code 1}, read by a request of its own.
     *
     * @param statistics the statistics
     * @return the estimated rows
     * @throws SQLException on database errors
     */
    private static double estimate(TableStatistics statistics) throws SQLException
    {
        try(Request request = new Engine(config).getRequest())
        {
            return statistics.estimateRows(request, valueOne());
        }
    }


    @Test
    @DisplayName("a request out of time fails at once with the SQL state of a cancelled statement")
    void elapsed() throws Exception
    {
        try(Request request = new Engine(config).getRequest())
        {
            long begin = System.nanoTime();
            SQLException exception = assertThrows(SQLException.class, () -> request.execute(query, 0, -1, 1));

            assertThat(exception.getSQLState(), is("57014"));
            assertThat((System.nanoTime() - begin) / 1e9, closeTo(0, 0.9));
        }

        // the frequency the request did not look up is not kept
        assertThat(estimate(config.getTableStatistics(table)), closeTo(500, 20));
    }


    @Test
    @DisplayName("a cancelled request fails with a checked exception and leaves the statistics as they are")
    void cancelled() throws Exception
    {
        try(Request request = new Engine(config).getRequest())
        {
            request.cancel();
            assertThrows(SQLException.class, () -> request.execute(query));
        }

        TableStatistics statistics = new CatalogStatistics(table);

        try(Request request = new Engine(config).getRequest())
        {
            request.cancel();
            assertThrows(SQLRuntimeException.class, () -> statistics.estimateRows(request, valueOne()));
        }

        assertThat(estimate(statistics), closeTo(500, 20));
    }
}
