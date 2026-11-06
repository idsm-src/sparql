package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
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
import cz.iocb.sparql.engine.mapping.classes.IntegerUserIriClass;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.LimitExceedException;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.translator.ServiceException;
import cz.iocb.sparql.testing.Database;



/**
 * Joins whose sides bind a shared variable in the same two classes. A side presents its classes of the variable that
 * overlap one class of the other sides as their union (see {@link UnionJoinClassTest}), and the sides are optimised
 * against the bindings the other sides had at the end of the previous round, so a side presenting the classes merged
 * and another one presenting them apart followed each other and swapped their presentations in every round: the
 * optimisation of a join, a left join, a minus, or a lateral join evaluating a pattern per named graph never ended.
 * Here a side binds the variable in the classes {@code a} and {@code b}, and in a branch that the first round drops, in
 * the generic IRI class, which overlaps both of them.
 */
public class JoinClassCycleTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> ";

    /**
     * A group binding {@code ?s} in the class {@code a} or {@code b}, and in a branch matching nothing, in the generic
     * IRI class.
     */
    private static final String group = "{ VALUES ?v { 1 } OPTIONAL { { ?w ex:p ?v BIND(IRI(STR(?w)) AS ?s) } "
            + "UNION { ?s ex:p ?z } UNION { ?z ex:p ?s } } } ";

    /**
     * A union binding {@code ?s} in the class {@code a} or {@code b}.
     */
    private static final String union = "{ { ?s ex:p ?r } UNION { ?r2 ex:p ?s } } ";

    /**
     * Configuration mapping the test tables.
     */
    private static SparqlDatabaseConfiguration config = null;

    /**
     * The same configuration with the join optimizer enabled.
     */
    private static SparqlDatabaseConfiguration optimized = null;


    /**
     * Creates the {@code join_class_cycle_test} schema, whose tables are mapped with the IRI classes {@code a}
     * ({@code http://example.org/a/<id>}) and {@code b} ({@code http://example.org/b/<id>}): {@code a/1 ex:p b/10} and
     * {@code a/2 ex:p b/20} in the default graph; {@code a/1 ex:q b/11} in the graph {@code a/1}, {@code a/3 ex:q b/30}
     * in the graph {@code a/3}, {@code a/2 ex:q b/21} in the graph {@code b/10} and {@code a/1 ex:q b/12} in the graph
     * {@code b/20}, so that the names of the graphs belong to both classes.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            String tables = "join_class_cycle_test.";

            statement.execute("create schema join_class_cycle_test");
            statement.execute("create table " + tables + "ab (s int not null, o int not null)");
            statement.execute("insert into " + tables + "ab values (1, 10), (2, 20)");
            statement.execute("create table " + tables + "ga (g int not null, s int not null, o int not null)");
            statement.execute("insert into " + tables + "ga values (1, 1, 11), (3, 3, 30)");
            statement.execute("create table " + tables + "gb (g int not null, s int not null, o int not null)");
            statement.execute("insert into " + tables + "gb values (10, 2, 21), (20, 1, 12)");
        }

        DatabaseSchema schema = new DatabaseSchema(connectionPool);

        config = configuration(connectionPool, schema);
        optimized = configuration(connectionPool, schema);
        optimized.setJoinOptimizer(true);
    }


    /**
     * Configuration mapping the test tables.
     *
     * @param connectionPool the connection pool of the database
     * @param schema the database schema
     * @return configuration mapping the test tables
     * @throws SQLException on database errors
     */
    private static SparqlDatabaseConfiguration configuration(DataSource connectionPool, DatabaseSchema schema)
            throws SQLException
    {
        SparqlDatabaseConfiguration configuration = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);
        configuration.addPrefix("ex", "http://example.org/");
        configuration.addIriClass(new IntegerUserIriClass("a", INT4, "http://example.org/a/"));
        configuration.addIriClass(new IntegerUserIriClass("b", INT4, "http://example.org/b/"));

        configuration.addQuadMapping(table("ab"), null, configuration.createIriMapping("a", "s"),
                configuration.createIriMapping("ex:p"), configuration.createIriMapping("b", "o"));

        configuration.addQuadMapping(table("ga"), configuration.createIriMapping("a", "g"),
                configuration.createIriMapping("a", "s"), configuration.createIriMapping("ex:q"),
                configuration.createIriMapping("b", "o"));

        configuration.addQuadMapping(table("gb"), configuration.createIriMapping("b", "g"),
                configuration.createIriMapping("a", "s"), configuration.createIriMapping("ex:q"),
                configuration.createIriMapping("b", "o"));

        return configuration;
    }


    /**
     * Test table of the given name.
     *
     * @param name the name of the table
     * @return test table of the given name
     */
    private static DatabaseTable table(String name)
    {
        return new DatabaseTable("join_class_cycle_test", name);
    }


    /**
     * Rows of the query, which have to be the same with the join optimizer enabled; both evaluations have to end within
     * thirty seconds.
     *
     * @param query the query, without the prefix declaration
     * @return rows of the query
     */
    private static List<List<RdfTerm>> execute(String query)
    {
        return assertTimeoutPreemptively(Duration.ofSeconds(30), () -> {
            List<List<RdfTerm>> rows = execute(config, query);

            assertThat(execute(optimized, query), containsInAnyOrder(rows.toArray()));

            return rows;
        });
    }


    /**
     * Rows of the query under the configuration.
     *
     * @param configuration the configuration
     * @param query the query, without the prefix declaration
     * @return rows of the query
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws LimitExceedException if the generated SQL is too long
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static List<List<RdfTerm>> execute(SparqlDatabaseConfiguration configuration, String query)
            throws SQLException, TranslateExceptions, LimitExceedException, ServiceException
    {
        Engine engine = new Engine(configuration);

        try(Request request = engine.getRequest(); Result result = request.execute(prefix + query))
        {
            List<List<RdfTerm>> rows = new ArrayList<>();

            while(result.next())
                rows.add(Arrays.asList(result.getRow()));

            return rows;
        }
    }


    /**
     * Row of the terms.
     *
     * @param terms the terms
     * @return row of the terms
     */
    private static List<RdfTerm> row(RdfTerm... terms)
    {
        return Arrays.asList(terms);
    }


    /**
     * IRI {@code http://example.org/<name>}.
     *
     * @param name the local name
     * @return IRI {@code http://example.org/<name>}
     */
    private static Iri resource(String name)
    {
        return new Iri("http://example.org/" + name);
    }


    @Test
    @DisplayName("a join of two groups binding the variable in the same two classes")
    void join()
    {
        String query = "SELECT ?s WHERE { " + group + "{ ?q ex:p ?r OPTIONAL " + union + "} }";

        assertThat(execute(query), containsInAnyOrder(row(resource("a/1")), row(resource("a/2")), row(resource("b/10")),
                row(resource("b/10")), row(resource("b/20")), row(resource("b/20"))));
    }


    @Test
    @DisplayName("an OPTIONAL binding the variable in the same two classes as its left side")
    void leftJoin()
    {
        String query = "SELECT ?s WHERE { " + group + "OPTIONAL " + union + "}";

        assertThat(execute(query), containsInAnyOrder(row(resource("a/1")), row(resource("a/2")), row(resource("b/10")),
                row(resource("b/20"))));
    }


    @Test
    @DisplayName("a MINUS binding the variable in the same two classes as its left side")
    void minus()
    {
        String query = "SELECT ?s WHERE { " + group + "MINUS " + union + "}";

        assertThat(execute(query), empty());
    }


    @Test
    @DisplayName("a pattern evaluated per named graph binding the graph variable in the classes of the graph names")
    void lateralJoin()
    {
        String query = """
                SELECT ?g WHERE { GRAPH ?g { VALUES ?v { 1 } OPTIONAL { { ?w ex:q ?v BIND(IRI(STR(?w)) AS ?g) } \
                UNION { ?g ex:q ?z } UNION { ?z ex:q ?g } } } }""";

        assertThat(execute(query), containsInAnyOrder(row(resource("a/1")), row(resource("a/3"))));
    }


    @Test
    @DisplayName("BINDs of variables of the current solution inside EXISTS, in an OPTIONAL and in a joined group")
    void existsMerges()
    {
        String query = """
                SELECT ?s ?v WHERE { VALUES (?s ?v) { (<http://example.org/a/1> <http://example.org/b/10>) \
                (<http://example.org/b/10> UNDEF) (UNDEF <http://example.org/a/2>) } \
                FILTER EXISTS { BIND(1 AS ?v) OPTIONAL { ?w ex:p ?v . BIND(IRI(STR(?w)) AS ?s) } \
                { BIND(?v AS ?s) } } }""";

        assertThat(execute(query), containsInAnyOrder(row(resource("b/10"), null)));
    }
}
