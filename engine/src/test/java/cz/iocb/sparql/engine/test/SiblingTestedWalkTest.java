package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
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
import cz.iocb.sparql.engine.mapping.classes.IntegerUserIriClass;
import cz.iocb.sparql.engine.rdf.Iri;
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
 * Walks of property paths whose start a sibling in their join binds always without being distinct over it, so that it
 * cannot seed them: the walk starts only from the nodes the sibling binds, tested by an existence test of the sibling,
 * which keeps its place in the join. The test hashes the sibling, or probes it by an index for every start, keeps a
 * walk evaluated for every start by itself so, takes the initial solutions of a walk evaluated as a whole, turns a walk
 * whose end the sibling binds, and is left out when the sibling binds about every start of the walk.
 */
public class SiblingTestedWalkTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> PREFIX a: <http://example.org/a/> ";

    /**
     * The beginning of the SQL of a walk evaluated for every start by itself.
     */
    private static final String perStart = " AS \"starts\" CROSS JOIN LATERAL (WITH RECURSIVE ";

    /**
     * The SQL of the test of the start against a sibling.
     */
    private static final String tested = ") AS \"sibling\" WHERE ";

    /**
     * The SQL of the test of the start against a sibling probed by an index for every start.
     */
    private static final String probed = " OFFSET 0) AS \"sibling\" WHERE ";

    /**
     * Configuration mapping the test tables.
     */
    private static SparqlDatabaseConfiguration config = null;

    /**
     * The same configuration with the join optimizer enabled.
     */
    private static SparqlDatabaseConfiguration optimized = null;


    /**
     * Creates the {@code sibling_tested_walk_test} schema, whose tables are mapped with the IRI classes {@code a}
     * ({@code http://example.org/a/<id>}) and {@code b} ({@code http://example.org/b/<id>}): {@code ex:r} relates
     * {@code a/1} to {@code a/2}, {@code a/2} to {@code a/3}, {@code a/3} to {@code a/1} and {@code a/4}, {@code a/5}
     * to itself, {@code a/6} to {@code a/7}, and each of {@code a/20} to {@code a/28} to the next one, and so does
     * {@code ex:u}, read from a table without an index; {@code ex:q} relates {@code a/1} to {@code a/2}, {@code a/2} to
     * {@code a/3} and each of {@code a/20} to {@code a/28} to the next one in the graph {@code a/100}, and {@code a/2}
     * to {@code a/4} and {@code a/4} to {@code a/1} in the graph {@code a/101}; {@code ex:stage} relates the contexts
     * {@code b/10} and {@code b/11} to {@code a/1}, {@code b/12} to {@code a/5} and {@code b/13} to {@code a/9};
     * {@code ex:code} relates each of {@code b/1} to {@code b/200} to {@code a/1} or {@code a/3}, read from a table
     * with an index of the code.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            String tables = "sibling_tested_walk_test.";
            String rows = " values (1, 2), (2, 3), (3, 1), (3, 4), (5, 5), (6, 7)";
            String chain = " select i, i + 1 from generate_series(20, 28) as i";

            statement.execute("create schema sibling_tested_walk_test");
            statement.execute("create table " + tables + "r (s int not null, o int not null, primary key (s, o))");
            statement.execute("insert into " + tables + "r" + rows);
            statement.execute("insert into " + tables + "r" + chain);
            statement.execute("create table " + tables + "u (s int not null, o int not null)");
            statement.execute("insert into " + tables + "u" + rows);
            statement.execute("insert into " + tables + "u" + chain);
            statement.execute("create table " + tables
                    + "q (g int not null, s int not null, o int not null, primary key (g, s, o))");
            statement.execute("insert into " + tables + "q values (100, 1, 2), (100, 2, 3), (101, 2, 4), (101, 4, 1)");
            statement.execute("insert into " + tables + "q select 100, i, i + 1 from generate_series(20, 28) as i");
            statement.execute("create table " + tables + "c (id int primary key, stage int not null)");
            statement.execute("insert into " + tables + "c values (10, 1), (11, 1), (12, 5), (13, 9)");
            statement.execute("create table " + tables + "e (id int primary key, code int not null)");
            statement.execute("insert into " + tables + "e select i, 1 + 2 * (i % 2) from generate_series(1, 200) i");
            statement.execute("create index on " + tables + "e (code)");
            statement.execute(
                    "analyze " + tables + "r, " + tables + "u, " + tables + "q, " + tables + "c, " + tables + "e");
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

        configuration.addQuadMapping(table("r"), null, configuration.createIriMapping("a", "s"),
                configuration.createIriMapping("ex:r"), configuration.createIriMapping("a", "o"));
        configuration.addQuadMapping(table("u"), null, configuration.createIriMapping("a", "s"),
                configuration.createIriMapping("ex:u"), configuration.createIriMapping("a", "o"));
        configuration.addQuadMapping(table("q"), configuration.createIriMapping("a", "g"),
                configuration.createIriMapping("a", "s"), configuration.createIriMapping("ex:q"),
                configuration.createIriMapping("a", "o"));
        configuration.addQuadMapping(table("c"), null, configuration.createIriMapping("b", "id"),
                configuration.createIriMapping("ex:stage"), configuration.createIriMapping("a", "stage"));
        configuration.addQuadMapping(table("e"), null, configuration.createIriMapping("b", "id"),
                configuration.createIriMapping("ex:code"), configuration.createIriMapping("a", "code"));

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
        return new DatabaseTable("sibling_tested_walk_test", name);
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
     * SQL of the query under the configuration.
     *
     * @param configuration the configuration
     * @param query the query, without the prefix declarations
     * @return SQL of the query
     * @throws Exception on errors
     */
    private static String translate(SparqlDatabaseConfiguration configuration, String query) throws Exception
    {
        Engine engine = new Engine(configuration);

        try(Request request = engine.getRequest())
        {
            PreparedQuery prepared = request.prepareQuery(prefix + query, null);
            SqlSelect imcode = new TranslateVisitor(request).translate(prepared.getSyntaxTree(), null, null, List.of());

            return imcode.optimize(request, false).optimize(request, true).translate(request);
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
     * IRI of the class {@code a}.
     *
     * @param id the id
     * @return IRI of the class {@code a}
     */
    private static Iri a(int id)
    {
        return new Iri("http://example.org/a/" + id);
    }


    /**
     * IRI of the class {@code b}.
     *
     * @param id the id
     * @return IRI of the class {@code b}
     */
    private static Iri b(int id)
    {
        return new Iri("http://example.org/b/" + id);
    }


    /**
     * Literal of xsd:integer.
     *
     * @param value the value
     * @return literal of xsd:integer
     */
    private static TypedLiteral integer(int value)
    {
        return new TypedLiteral(Integer.toString(value), xsdIntegerIri);
    }


    /**
     * The contexts of {@code ex:stage} with the nodes their stages lead to by {@code ex:r} (or by {@code ex:u}, which
     * holds the same triples).
     *
     * @return the contexts with the nodes their stages lead to
     */
    private static Object[] reached()
    {
        return new Object[] { row(b(10), a(1)), row(b(10), a(2)), row(b(10), a(3)), row(b(10), a(4)), row(b(11), a(1)),
                row(b(11), a(2)), row(b(11), a(3)), row(b(11), a(4)), row(b(12), a(5)) };
    }


    @Test
    @DisplayName("a walk evaluated for every start by itself starts from the nodes a sibling hashed binds")
    void hashed() throws Exception
    {
        String query = "SELECT ?c ?y WHERE { ?c ex:stage ?x . ?x ex:r+ ?y }";

        for(SparqlDatabaseConfiguration configuration : List.of(config, optimized))
        {
            String sql = translate(configuration, query);

            assertThat(sql, containsString(perStart));
            assertThat(sql, containsString(tested));
            assertThat(sql, not(containsString(probed)));
        }

        assertThat(execute(query), containsInAnyOrder(reached()));
    }


    @Test
    @DisplayName("a walk starts from the nodes a sibling binds, probed by an index for every start")
    void probed() throws Exception
    {
        String query = "SELECT (COUNT(*) AS ?n) WHERE { ?e ex:code ?x . ?x ex:r+ ?y }";

        assertThat(translate(optimized, query), containsString(probed));
        assertThat(translate(config, query), containsString(tested));
        assertThat(execute(query), containsInAnyOrder(row(integer(800))));
    }


    @Test
    @DisplayName("a walk whose end a sibling binds is turned to start from the nodes the sibling binds")
    void fromEnd() throws Exception
    {
        String query = "SELECT ?x ?c WHERE { ?x ex:r+ ?y . ?c ex:stage ?y }";

        assertThat(translate(config, query), containsString(tested));
        assertThat(translate(optimized, query), containsString(tested));
        assertThat(execute(query), containsInAnyOrder(row(a(1), b(10)), row(a(1), b(11)), row(a(2), b(10)),
                row(a(2), b(11)), row(a(3), b(10)), row(a(3), b(11)), row(a(5), b(12))));
    }


    @Test
    @DisplayName("a walk evaluated as a whole takes only the initial solutions starting in the nodes a sibling binds")
    void whole() throws Exception
    {
        String query = "SELECT ?c ?y WHERE { ?c ex:stage ?x . ?x ex:u+ ?y }";

        for(SparqlDatabaseConfiguration configuration : List.of(config, optimized))
        {
            String sql = translate(configuration, query);

            assertThat(sql, not(containsString(perStart)));
            assertThat(sql, containsString(tested));
        }

        assertThat(execute(query), containsInAnyOrder(reached()));
    }


    @Test
    @DisplayName("a sibling binding about every start of the walk does not test it")
    void untested() throws Exception
    {
        String query = "SELECT (COUNT(*) AS ?n) WHERE { ?x ex:r ?z . ?x ex:r+ ?y }";

        assertThat(translate(config, query), not(containsString(tested)));
        assertThat(translate(optimized, query), not(containsString(tested)));
        assertThat(execute(query), containsInAnyOrder(row(integer(63))));
    }


    @Test
    @DisplayName("a walk in a graph starts from the nodes a sibling binds")
    void graph() throws Exception
    {
        String query = "SELECT ?c ?g ?y WHERE { ?c ex:stage ?x . GRAPH ?g { ?x ex:q+ ?y } }";

        assertThat(translate(config, query), containsString(tested));
        assertThat(execute(query), containsInAnyOrder(row(b(10), a(100), a(2)), row(b(10), a(100), a(3)),
                row(b(11), a(100), a(2)), row(b(11), a(100), a(3))));
    }


    @Test
    @DisplayName("a walk in an OPTIONAL starts from the nodes a sibling binds")
    void optional() throws Exception
    {
        String query = "SELECT ?c ?y WHERE { ?c ex:stage ?s OPTIONAL { ?c ex:stage ?x . ?x ex:r+ ?y } }";

        List<Object> rows = new ArrayList<>(Arrays.asList(reached()));
        rows.add(row(b(13), null));

        assertThat(execute(query), containsInAnyOrder(rows.toArray()));
    }


    @Test
    @DisplayName("a walk in an EXISTS pattern starts from the nodes a sibling restricted by the current solution binds")
    void exists() throws Exception
    {
        String query = "SELECT ?c WHERE { ?c ex:stage ?s FILTER EXISTS { ?c ex:stage ?x . ?x ex:r+ ?y . ?y ex:r ?z } }";

        assertThat(execute(query), containsInAnyOrder(row(b(10)), row(b(11)), row(b(12))));
    }
}
