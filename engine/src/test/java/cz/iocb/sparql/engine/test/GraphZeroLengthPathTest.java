package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
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
 * Zero-length paths from a constant inside {@code GRAPH}. A constant matches itself in every graph, also where it is no
 * node, so the solution of such a path came without a graph: a walk of zero or more steps starting from it carried no
 * graph and joined its steps with the triples of all graphs, a graph variable at the end of the walk took nodes that
 * are no graphs, a path of zero or one step dropped the graph variable, and the solution held in a constant graph that
 * did not exist, or inside EXISTS for a value of the graph variable that is no graph. The solution is now paired with
 * every named graph, so that a walk starting from it carries the graph, and in a constant graph, it holds only when the
 * graph exists. A group whose OPTIONAL, MINUS or EXISTS comes before the members that bind the graph starts from the
 * named graphs, as it would otherwise be evaluated over all graphs at once, and a sub-select keeps the graph variable
 * of its pattern to itself. A graph computed by the current solution of an EXISTS is matched also with the graphs that
 * belong to no IRI class.
 */
public class GraphZeroLengthPathTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> PREFIX a: <http://example.org/a/> "
            + "PREFIX b: <http://example.org/b/> ";

    /**
     * Configuration mapping the test tables.
     */
    private static SparqlDatabaseConfiguration config = null;

    /**
     * The same configuration with the join optimizer enabled.
     */
    private static SparqlDatabaseConfiguration optimized = null;

    /**
     * Configuration mapping the test tables and the graph {@code ex:g1}, which belongs to no IRI class.
     */
    private static SparqlDatabaseConfiguration unclassified = null;

    /**
     * The same configuration with the join optimizer enabled.
     */
    private static SparqlDatabaseConfiguration unclassifiedOptimized = null;


    /**
     * Creates the {@code graph_zero_length_path_test} schema, whose tables are mapped with the IRI classes {@code a}
     * ({@code http://example.org/a/<id>}) and {@code b} ({@code http://example.org/b/<id>}): {@code a/1 ex:s a/2} in
     * the graph {@code a/100}, {@code a/2 ex:s a/3}, {@code a/3 ex:s a/101} and {@code a/2 ex:t a/3} in the graph
     * {@code a/101}, {@code a/3 ex:s a/4} in the graph {@code b/200}, and {@code b/200 ex:r a/1} in the default graph;
     * {@code a/5 ex:s a/6} in the graph {@code ex:g1} is mapped by the configurations of an unclassified graph only.
     * The tables of the graphs {@code a} and {@code b} are indexed by the graph and the subject and by the graph and
     * the object, so that a walk carrying the graph in both classes joins its steps laterally (see
     * {@link cz.iocb.sparql.engine.imcode.SqlRecursive}); the table of {@code ex:g1} has no index, so that the walks of
     * the configurations of an unclassified graph join their steps as a whole.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            String tables = "graph_zero_length_path_test.";

            statement.execute("create schema graph_zero_length_path_test");
            statement.execute("create table " + tables + "sa (g int not null, s int not null, o int not null)");
            statement.execute("insert into " + tables + "sa values (100, 1, 2), (101, 2, 3), (101, 3, 101)");
            statement.execute("create table " + tables + "sb (g int not null, s int not null, o int not null)");
            statement.execute("insert into " + tables + "sb values (200, 3, 4)");
            statement.execute("create table " + tables + "r (s int not null, o int not null)");
            statement.execute("insert into " + tables + "r values (200, 1)");
            statement.execute("create table " + tables + "t (g int not null, s int not null, o int not null)");
            statement.execute("insert into " + tables + "t values (101, 2, 3)");
            statement.execute("create table " + tables + "u (s int not null, o int not null)");
            statement.execute("insert into " + tables + "u values (5, 6)");

            for(String table : List.of("sa", "sb", "t"))
            {
                statement.execute("create index on " + tables + table + " (g, s)");
                statement.execute("create index on " + tables + table + " (g, o)");
            }
        }

        DatabaseSchema schema = new DatabaseSchema(connectionPool);

        config = configuration(connectionPool, schema, false);
        optimized = configuration(connectionPool, schema, false);
        optimized.setJoinOptimizer(true);
        unclassified = configuration(connectionPool, schema, true);
        unclassifiedOptimized = configuration(connectionPool, schema, true);
        unclassifiedOptimized.setJoinOptimizer(true);
    }


    /**
     * Configuration mapping the test tables.
     *
     * @param connectionPool the connection pool of the database
     * @param schema the database schema
     * @param withUnclassified whether the graph {@code ex:g1}, which belongs to no IRI class, is mapped too
     * @return configuration mapping the test tables
     * @throws SQLException on database errors
     */
    private static SparqlDatabaseConfiguration configuration(DataSource connectionPool, DatabaseSchema schema,
            boolean withUnclassified) throws SQLException
    {
        SparqlDatabaseConfiguration configuration = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);
        configuration.addPrefix("ex", "http://example.org/");
        configuration.addIriClass(new IntegerUserIriClass("a", INT4, "http://example.org/a/"));
        configuration.addIriClass(new IntegerUserIriClass("b", INT4, "http://example.org/b/"));

        configuration.addQuadMapping(table("sa"), configuration.createIriMapping("a", "g"),
                configuration.createIriMapping("a", "s"), configuration.createIriMapping("ex:s"),
                configuration.createIriMapping("a", "o"));

        configuration.addQuadMapping(table("sb"), configuration.createIriMapping("b", "g"),
                configuration.createIriMapping("a", "s"), configuration.createIriMapping("ex:s"),
                configuration.createIriMapping("a", "o"));

        configuration.addQuadMapping(table("r"), null, configuration.createIriMapping("b", "s"),
                configuration.createIriMapping("ex:r"), configuration.createIriMapping("a", "o"));

        configuration.addQuadMapping(table("t"), configuration.createIriMapping("a", "g"),
                configuration.createIriMapping("a", "s"), configuration.createIriMapping("ex:t"),
                configuration.createIriMapping("a", "o"));

        if(withUnclassified)
            configuration.addQuadMapping(table("u"), configuration.createIriMapping("ex:g1"),
                    configuration.createIriMapping("a", "s"), configuration.createIriMapping("ex:s"),
                    configuration.createIriMapping("a", "o"));

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
        return new DatabaseTable("graph_zero_length_path_test", name);
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
        return execute(config, optimized, query);
    }


    /**
     * Rows of the query under the configuration, which have to be the same under the configuration with the join
     * optimizer enabled.
     *
     * @param plain the configuration
     * @param withOptimizer the same configuration with the join optimizer enabled
     * @param query the query, without the prefix declarations
     * @return rows of the query
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws LimitExceedException if the generated SQL is too long
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static List<List<RdfTerm>> execute(SparqlDatabaseConfiguration plain,
            SparqlDatabaseConfiguration withOptimizer, String query)
            throws SQLException, TranslateExceptions, LimitExceedException, ServiceException
    {
        List<List<RdfTerm>> rows = execute(plain, query);

        assertThat(execute(withOptimizer, query), containsInAnyOrder(rows.toArray()));

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


    @Test
    @DisplayName("a walk of zero or more steps from a constant in a graph")
    void constantStart() throws Exception
    {
        String query = "SELECT ?g ?y WHERE { GRAPH ?g { a:1 ex:s* ?y } }";

        assertThat(execute(query),
                containsInAnyOrder(row(a(100), a(1)), row(a(100), a(2)), row(a(101), a(1)), row(b(200), a(1))));
    }


    @Test
    @DisplayName("a walk of zero or more steps to a constant in a graph")
    void constantEnd() throws Exception
    {
        String query = "SELECT ?g ?x WHERE { GRAPH ?g { ?x ex:s* a:3 } }";

        assertThat(execute(query),
                containsInAnyOrder(row(a(100), a(3)), row(a(101), a(2)), row(a(101), a(3)), row(b(200), a(3))));
    }


    @Test
    @DisplayName("a walk of zero or more steps between two constants in a graph")
    void constantEnds() throws Exception
    {
        String query = "SELECT ?g WHERE { GRAPH ?g { a:1 ex:s* a:3 } }";

        assertThat(execute(query), empty());
    }


    @Test
    @DisplayName("a walk of zero or more steps from a constant joined with a triple of the graph")
    void joinedWithTriple() throws Exception
    {
        String query = "SELECT ?g ?d ?e WHERE { GRAPH ?g { a:1 ex:s* ?d . ?d ex:s ?e } }";

        assertThat(execute(query), containsInAnyOrder(row(a(100), a(1), a(2))));
    }


    @Test
    @DisplayName("a walk of zero or more steps from a constant to the graph variable")
    void graphAtEnd() throws Exception
    {
        String query = "SELECT ?g WHERE { GRAPH ?g { a:2 ex:s* ?g } }";

        assertThat(execute(query), containsInAnyOrder(row(a(101))));
    }


    @Test
    @DisplayName("a walk of one or more steps that match the zero-length path from a constant in a graph")
    void zeroLengthSteps() throws Exception
    {
        String query = "SELECT ?g ?y WHERE { GRAPH ?g { a:1 (ex:s|ex:r?)+ ?y } }";

        assertThat(execute(query),
                containsInAnyOrder(row(a(100), a(1)), row(a(100), a(2)), row(a(101), a(1)), row(b(200), a(1))));
    }


    @Test
    @DisplayName("a walk of zero or more steps to a constant the steps cannot reach, inside EXISTS")
    void unreachableEnd() throws Exception
    {
        String query = "SELECT ?x WHERE { ?x ex:r ?o FILTER EXISTS { GRAPH ?g { ?y ex:s* \"z\" } } }";

        assertThat(execute(query), containsInAnyOrder(row(b(200))));
    }


    @Test
    @DisplayName("a path of zero or one step between two variables in a graph")
    void zeroOrOneStep() throws Exception
    {
        String query = "SELECT ?x ?y WHERE { GRAPH ?g { ?x ex:s? ?y } }";

        assertThat(execute(query),
                containsInAnyOrder(row(a(1), a(1)), row(a(1), a(2)), row(a(2), a(2)), row(a(2), a(2)), row(a(2), a(3)),
                        row(a(3), a(3)), row(a(3), a(3)), row(a(3), a(4)), row(a(3), a(101)), row(a(4), a(4)),
                        row(a(101), a(101))));
    }


    @Test
    @DisplayName("a path of zero or one step from a constant in a graph")
    void zeroOrOneStepFromConstant() throws Exception
    {
        String query = "SELECT ?g ?y WHERE { GRAPH ?g { a:1 ex:s? ?y } }";

        assertThat(execute(query),
                containsInAnyOrder(row(a(100), a(1)), row(a(100), a(2)), row(a(101), a(1)), row(b(200), a(1))));
    }


    @Test
    @DisplayName("a walk of zero or more steps from a constant in a graph that does not exist")
    void missingGraph() throws Exception
    {
        String query = "SELECT ?y WHERE { GRAPH b:999 { a:1 ex:s* ?y } }";

        assertThat(execute(query), empty());
    }


    @Test
    @DisplayName("a walk of zero or more steps from a constant that is no node of the graph")
    void constantGraph() throws Exception
    {
        String query = "SELECT ?y WHERE { GRAPH a:101 { a:1 ex:s* ?y } }";

        assertThat(execute(query), containsInAnyOrder(row(a(1))));
    }


    @Test
    @DisplayName("an OPTIONAL walk of zero or more steps from a constant in a graph evaluated per graph")
    void perGraph() throws Exception
    {
        String query = "SELECT ?g ?y WHERE { GRAPH ?g { OPTIONAL { a:1 ex:s* ?y } } }";

        assertThat(execute(query),
                containsInAnyOrder(row(a(100), a(1)), row(a(100), a(2)), row(a(101), a(1)), row(b(200), a(1))));
    }


    @Test
    @DisplayName("a walk of zero or more steps to a constant in a graph inside EXISTS")
    void exists() throws Exception
    {
        String query = "SELECT DISTINCT ?x WHERE { ?x ex:s ?o FILTER EXISTS { GRAPH ?g { ?x ex:s* a:4 } } }";

        assertThat(execute(query), containsInAnyOrder(row(a(3))));
    }


    @Test
    @DisplayName("a walk of zero or more steps from a constant inside EXISTS in a graph the current solution binds")
    void boundGraph() throws Exception
    {
        String query = "SELECT ?g WHERE { VALUES ?g { a:100 a:999 } FILTER EXISTS { GRAPH ?g { a:1 ex:s* ?y } } }";

        assertThat(execute(query), containsInAnyOrder(row(a(100))));
    }


    @Test
    @DisplayName("a walk of zero or more steps from a constant after an OPTIONAL starting the group")
    void leadingOptional() throws Exception
    {
        String query = "SELECT ?g ?b ?y WHERE { GRAPH ?g { OPTIONAL { ?a ex:s ?b } a:3 ex:s* ?y } }";

        assertThat(execute(query),
                containsInAnyOrder(row(a(100), a(2), a(3)), row(a(101), a(101), a(101)), row(a(101), a(101), a(3)),
                        row(a(101), a(3), a(101)), row(a(101), a(3), a(3)), row(b(200), a(4), a(3)),
                        row(b(200), a(4), a(4))));
    }


    @Test
    @DisplayName("a path of zero or one step from a constant after a MINUS following a BIND")
    void leadingMinus() throws Exception
    {
        String query = "SELECT ?g ?b WHERE { GRAPH ?g { BIND(1 AS ?k) MINUS { ?b ex:s a:2 } a:2 ex:s? ?b } }";

        assertThat(execute(query),
                containsInAnyOrder(row(a(100), a(2)), row(a(101), a(2)), row(a(101), a(3)), row(b(200), a(2))));
    }


    @Test
    @DisplayName("a walk of zero or more steps between two constants inside EXISTS in a graph evaluated per graph")
    void existsPerGraph() throws Exception
    {
        String query = "SELECT ?g ?y WHERE { GRAPH ?g { OPTIONAL { a:1 ex:s ?y } FILTER EXISTS { a:1 ex:s* a:2 } } }";

        assertThat(execute(query), containsInAnyOrder(row(a(100), a(2))));
    }


    @Test
    @DisplayName("two paths matching the zero-length path from constants in a graph")
    void twoPaths() throws Exception
    {
        String query = "SELECT ?g ?x ?y WHERE { GRAPH ?g { a:1 ex:s* ?x . a:3 ex:s? ?y } }";

        assertThat(execute(query), containsInAnyOrder(row(a(100), a(1), a(3)), row(a(100), a(2), a(3)),
                row(a(101), a(1), a(101)), row(a(101), a(1), a(3)), row(b(200), a(1), a(3)), row(b(200), a(1), a(4))));
    }


    @Test
    @DisplayName("a walk of zero or more steps from a constant in a graph a MINUS compares")
    void minus() throws Exception
    {
        String query = "SELECT ?y WHERE { GRAPH ?g { a:1 ex:s* ?y } MINUS { ?g ex:r ?z } }";

        assertThat(execute(query), containsInAnyOrder(row(a(1)), row(a(1)), row(a(2))));
    }


    @Test
    @DisplayName("a sub-select joined with an OPTIONAL in a graph evaluated per graph")
    void subSelect() throws Exception
    {
        String query = "SELECT ?g ?b ?d WHERE { GRAPH ?g { OPTIONAL { a:1 ex:s ?b } "
                + "{ SELECT ?d WHERE { ?d ex:s ?e } } } }";

        assertThat(execute(query), containsInAnyOrder(row(a(100), a(2), a(1)), row(a(101), null, a(2)),
                row(a(101), null, a(3)), row(b(200), null, a(3))));
    }


    @Test
    @DisplayName("a sub-select of a walk of zero or more steps from a constant inside EXISTS evaluated per graph")
    void subSelectInExists() throws Exception
    {
        String query = "SELECT ?g ?y WHERE { GRAPH ?g { OPTIONAL { ?a ex:t ?y } "
                + "FILTER EXISTS { SELECT ?z WHERE { a:1 ex:s* ?z } } } }";

        assertThat(execute(query), containsInAnyOrder(row(a(100), null), row(a(101), a(3)), row(b(200), null)));
    }


    @Test
    @DisplayName("a sub-select of a path of zero or one step from a constant inside NOT EXISTS evaluated per graph")
    void subSelectInNotExists() throws Exception
    {
        String query = "SELECT ?g ?y WHERE { GRAPH ?g { BIND(1 AS ?k) OPTIONAL { ?a ex:t ?y } "
                + "FILTER NOT EXISTS { SELECT ?z WHERE { a:1 ex:s? ?z } } } }";

        assertThat(execute(query), empty());
    }


    @Test
    @DisplayName("an EXISTS in a nested group binding no graph")
    void nestedExists() throws Exception
    {
        String query = "SELECT ?g ?a ?b WHERE { GRAPH ?g { ?a ex:s ?b { FILTER EXISTS { ?c ex:s a:2 } } } }";

        assertThat(execute(query), containsInAnyOrder(row(a(100), a(1), a(2))));
    }


    @Test
    @DisplayName("an OPTIONAL after a nested GRAPH clause of the same graph")
    void nestedGraph() throws Exception
    {
        String query = "SELECT ?g ?a ?b ?c WHERE { GRAPH ?g { GRAPH ?g { ?a ex:s ?b } OPTIONAL { ?b ex:s ?c } } }";

        assertThat(execute(query), containsInAnyOrder(row(a(100), a(1), a(2), null), row(a(101), a(2), a(3), a(101)),
                row(a(101), a(3), a(101), null), row(b(200), a(3), a(4), null)));
    }


    @Test
    @DisplayName("a walk of zero or more steps to a constant inside EXISTS after an OPTIONAL starting its group")
    void existsLeadingOptional() throws Exception
    {
        String query = "SELECT ?g ?a WHERE { GRAPH ?g { ?a ex:s ?b "
                + "FILTER EXISTS { OPTIONAL { ?b ex:s ?c } ?b ex:s* a:101 } } }";

        assertThat(execute(query), containsInAnyOrder(row(a(101), a(2)), row(a(101), a(3))));
    }


    @Test
    @DisplayName("a walk of zero or more steps from a constant inside EXISTS after a MINUS starting its group")
    void existsLeadingMinus() throws Exception
    {
        String query = "SELECT ?g ?a WHERE { GRAPH ?g { ?a ex:s ?b "
                + "FILTER EXISTS { MINUS { ?b ex:s ?c } a:1 ex:s* ?b } } }";

        assertThat(execute(query), containsInAnyOrder(row(a(100), a(1))));
    }


    @Test
    @DisplayName("a walk of zero or more steps from a constant after an OPTIONAL in a graph the current solution binds")
    void boundGraphLeadingOptional() throws Exception
    {
        String query = "SELECT ?g WHERE { VALUES ?g { a:100 a:999 } "
                + "FILTER EXISTS { GRAPH ?g { OPTIONAL { ?x ex:s ?y } a:1 ex:s* ?z } } }";

        assertThat(execute(query), containsInAnyOrder(row(a(100))));
    }


    @Test
    @DisplayName("two walks of zero or more steps from constants inside EXISTS in a graph")
    void twoPathsInExists() throws Exception
    {
        String query = "SELECT ?g ?x ?y WHERE { GRAPH ?g { ?x ex:t ?y "
                + "FILTER EXISTS { a:1 ex:s* ?z . a:2 ex:s* ?w } } }";

        assertThat(execute(query), containsInAnyOrder(row(a(101), a(2), a(3))));
    }


    @Test
    @DisplayName("two walks of zero or more steps from constants inside EXISTS in a graph the current solution binds")
    void boundGraphTwoPaths() throws Exception
    {
        String query = "SELECT ?x WHERE { ?x ex:r ?g FILTER EXISTS { GRAPH ?g { a:1 ex:s* ?z . a:2 ex:s* ?w } } }";

        assertThat(execute(query), empty());
    }


    @Test
    @DisplayName("a walk of zero or more steps from a constant joined with a union inside EXISTS in a graph")
    void unionInExists() throws Exception
    {
        String query = "SELECT ?g ?x ?t WHERE { GRAPH ?g { ?x ex:t ?t "
                + "FILTER EXISTS { a:3 ex:s* ?y . { a:1 ex:s* ?z } UNION { ?z ex:s ?w } } } }";

        assertThat(execute(query), containsInAnyOrder(row(a(101), a(2), a(3))));
    }


    @Test
    @DisplayName("a walk of zero or more steps from a constant inside EXISTS in computed graphs of any IRI class")
    void computedGraph() throws Exception
    {
        String query = """
                SELECT ?g WHERE { VALUES ?n { "http://example.org/a/100" "http://example.org/g1" \
                "http://example.org/a/999" } BIND(IRI(?n) AS ?g) \
                FILTER EXISTS { GRAPH ?g { ?x ex:s ?y FILTER EXISTS { a:1 ex:s* ?z } } } }""";

        assertThat(execute(unclassified, unclassifiedOptimized, query),
                containsInAnyOrder(row(a(100)), row(new Iri("http://example.org/g1"))));
    }


    @Test
    @DisplayName("two walks of zero or more steps from constants inside EXISTS in a graph bound or not")
    void boundOrUnboundGraphTwoPaths() throws Exception
    {
        String query = "SELECT ?g WHERE { VALUES ?g { a:100 UNDEF } "
                + "FILTER EXISTS { GRAPH ?g { a:1 ex:s* ?z . a:2 ex:s* ?w } } }";

        assertThat(execute(query), containsInAnyOrder(row(a(100)), row((RdfTerm) null)));
    }
}
