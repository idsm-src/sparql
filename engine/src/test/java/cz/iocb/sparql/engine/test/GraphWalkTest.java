package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
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
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.LimitExceedException;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Request.PreparedQuery;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.translator.ServiceException;
import cz.iocb.sparql.engine.translator.TranslateVisitor;
import cz.iocb.sparql.testing.Database;



/**
 * Walks of property paths inside {@code GRAPH ?g}. A path has to stay in one graph, so the recursion carries the graph
 * of the first step and joins every further step with it. The step was joined with the graph only as far as the parent
 * needed the graph variable: when the parent did not need it, the paths crossed the graphs, and when it needed the
 * variable in some of its classes only (a MINUS or an OPTIONAL whose other side binds it in fewer classes), the
 * translation failed. In a pattern evaluated per graph, the side of an OPTIONAL or a MINUS that would seed a walk of
 * the other side reads the graph of the evaluation, so its solutions cannot be shared in a common table expression,
 * which would not see the graph; a side holding a whole pattern evaluated per graph reads nothing from outside, so it
 * is shared and seeds the walk like any other.
 */
public class GraphWalkTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> ";

    /**
     * Configuration mapping the test tables.
     */
    private static SparqlDatabaseConfiguration config = null;

    /**
     * The same configuration with the join optimizer enabled.
     */
    private static SparqlDatabaseConfiguration optimized = null;


    /**
     * Creates the {@code graph_walk_test} schema, whose tables are mapped with the IRI classes {@code a}
     * ({@code http://example.org/a/<id>}) and {@code b} ({@code http://example.org/b/<id>}): {@code a/1 ex:s a/2} in
     * the graph {@code a/100}, {@code a/2 ex:s a/3} in the graph {@code a/101} and {@code a/3 ex:s a/4} in the graph
     * {@code b/200}; {@code ex:t} holds the same triples and {@code a/2 ex:t b/30} in the graph {@code a/100};
     * {@code ex:u} holds only the triples of the graphs of the class {@code a}; and {@code b/200 ex:r a/1} is in the
     * default graph.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            String tables = "graph_walk_test.";

            statement.execute("create schema graph_walk_test");
            statement.execute("create table " + tables + "sa (g int not null, s int not null, o int not null)");
            statement.execute("insert into " + tables + "sa values (100, 1, 2), (101, 2, 3)");
            statement.execute("create table " + tables + "sb (g int not null, s int not null, o int not null)");
            statement.execute("insert into " + tables + "sb values (200, 3, 4)");
            statement.execute("create table " + tables + "sab (g int not null, s int not null, o int not null)");
            statement.execute("insert into " + tables + "sab values (100, 2, 30)");
            statement.execute("create table " + tables + "r (s int not null, o int not null)");
            statement.execute("insert into " + tables + "r values (200, 1)");
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

        for(String predicate : List.of("ex:s", "ex:t", "ex:u"))
            configuration.addQuadMapping(table("sa"), configuration.createIriMapping("a", "g"),
                    configuration.createIriMapping("a", "s"), configuration.createIriMapping(predicate),
                    configuration.createIriMapping("a", "o"));

        for(String predicate : List.of("ex:s", "ex:t"))
            configuration.addQuadMapping(table("sb"), configuration.createIriMapping("b", "g"),
                    configuration.createIriMapping("a", "s"), configuration.createIriMapping(predicate),
                    configuration.createIriMapping("a", "o"));

        configuration.addQuadMapping(table("sab"), configuration.createIriMapping("a", "g"),
                configuration.createIriMapping("a", "s"), configuration.createIriMapping("ex:t"),
                configuration.createIriMapping("b", "o"));

        configuration.addQuadMapping(table("r"), null, configuration.createIriMapping("b", "s"),
                configuration.createIriMapping("ex:r"), configuration.createIriMapping("a", "o"));

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
        return new DatabaseTable("graph_walk_test", name);
    }


    /**
     * Rows of the query, which have to be the same with the join optimizer enabled.
     *
     * @param query the query, without the prefix declaration
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
     * The code of the first common table expression of the statement, the shared solutions the others are made of.
     *
     * @param sql SQL of a statement
     * @return the code of the first common table expression of the statement, empty when it declares none
     */
    private static String shared(String sql)
    {
        String begin = "WITH \"cte0\" AS (";

        if(!sql.startsWith(begin))
            return "";

        return sql.substring(begin.length(), sql.indexOf("\"cte1\" AS ("));
    }


    /**
     * SQL of the query under the configuration without the join optimizer.
     *
     * @param query the query, without the prefix declaration
     * @return SQL of the query
     * @throws Exception on errors
     */
    private static String translate(String query) throws Exception
    {
        Engine engine = new Engine(config);

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


    @Test
    @DisplayName("a walk in a graph the parent does not need")
    void unneededGraph() throws Exception
    {
        String query = "SELECT ?x ?y WHERE { GRAPH ?g { ?x ex:s+ ?y } }";

        assertThat(execute(query), containsInAnyOrder(row(a(1), a(2)), row(a(2), a(3)), row(a(3), a(4))));
    }


    @Test
    @DisplayName("a walk in a graph a MINUS compares in one class")
    void minus() throws Exception
    {
        String query = "SELECT ?x ?y WHERE { GRAPH ?g { ?x ex:s+ ?y } MINUS { ?g ex:r ?z } }";

        assertThat(execute(query), containsInAnyOrder(row(a(1), a(2)), row(a(2), a(3))));
    }


    @Test
    @DisplayName("a walk whose step reaches nodes of two classes, in a graph an OPTIONAL compares in one class")
    void lateralStep() throws Exception
    {
        String query = "SELECT ?x ?y ?z WHERE { GRAPH ?g { ?x ex:t+ ?y } OPTIONAL { ?g ex:r ?z } }";

        assertThat(execute(query), containsInAnyOrder(row(a(1), a(2), null), row(a(1), b(30), null),
                row(a(2), a(3), null), row(a(2), b(30), null), row(a(3), a(4), a(1))));
    }


    @Test
    @DisplayName("a walk of zero or more steps over one table, in a graph an OPTIONAL compares in one class")
    void singleTableStep() throws Exception
    {
        String query = "SELECT ?x ?y WHERE { GRAPH ?g { ?x ex:u* ?y } OPTIONAL { ?g ex:r ?z } }";

        assertThat(execute(query),
                containsInAnyOrder(row(a(1), a(1)), row(a(1), a(2)), row(a(2), a(2)), row(a(2), a(2)), row(a(2), a(3)),
                        row(a(3), a(3)), row(a(3), a(3)), row(a(4), a(4)), row(b(30), b(30))));
    }


    @Test
    @DisplayName("a walk in a graph inside EXISTS")
    void exists() throws Exception
    {
        String query = "SELECT DISTINCT ?x WHERE { ?x ex:s ?o "
                + "FILTER EXISTS { GRAPH ?g { ?x ex:s+ ?y FILTER(?y = <http://example.org/a/4>) } } }";

        assertThat(execute(query), containsInAnyOrder(row(a(3))));
    }


    @Test
    @DisplayName("a walk in a graph seeded with the values of its start")
    void seeded() throws Exception
    {
        String query = "SELECT ?x ?y WHERE { VALUES ?x { <http://example.org/a/1> <http://example.org/a/2> } "
                + "GRAPH ?g { ?x ex:s+ ?y } }";

        assertThat(execute(query), containsInAnyOrder(row(a(1), a(2)), row(a(2), a(3))));
    }


    @Test
    @DisplayName("a walk in a graph to a constant end")
    void constantEnd() throws Exception
    {
        String query = "SELECT ?x WHERE { GRAPH ?g { ?x ex:s+ <http://example.org/a/4> } }";

        assertThat(execute(query), containsInAnyOrder(row(a(3))));
    }


    @Test
    @DisplayName("a walk in an OPTIONAL of a pattern evaluated per graph, whose left side reads the graph")
    void optionalPerGraph() throws Exception
    {
        String query = "SELECT ?g ?x ?z ?w WHERE { GRAPH ?g { { SELECT ?x ?z ?w WHERE "
                + "{ ?x ex:s ?y OPTIONAL { ?x ex:t+ ?z . ?z ex:t ?w } } } } }";

        assertThat(execute(query), containsInAnyOrder(row(a(100), a(1), a(2), b(30)), row(a(101), a(2), null, null),
                row(b(200), a(3), null, null)));
    }


    @Test
    @DisplayName("a walk in a MINUS of a pattern evaluated per graph, whose left side reads the graph")
    void minusPerGraph() throws Exception
    {
        String query = "SELECT ?g ?x WHERE { GRAPH ?g { { SELECT ?x WHERE "
                + "{ ?x ex:s ?y MINUS { ?x ex:t+ ?z . ?z ex:t ?w } } } } }";

        assertThat(execute(query), containsInAnyOrder(row(a(101), a(2)), row(b(200), a(3))));
    }


    @Test
    @DisplayName("a walk in an OPTIONAL seeded from a left side holding a pattern evaluated per graph")
    void optionalAfterPatternPerGraph() throws Exception
    {
        String query = "SELECT ?g ?x ?z ?w WHERE { GRAPH ?g { { SELECT ?x WHERE { ?x ex:s ?y } } } "
                + "OPTIONAL { ?x ex:t+ ?z . ?z ex:t ?w } }";

        assertThat(shared(translate(query)), containsString(" CROSS JOIN LATERAL "));
        assertThat(execute(query), containsInAnyOrder(row(a(100), a(1), a(2), a(3)), row(a(100), a(1), a(2), b(30)),
                row(a(100), a(1), a(3), a(4)), row(a(101), a(2), a(3), a(4)), row(b(200), a(3), null, null)));
    }


    @Test
    @DisplayName("a walk in a MINUS seeded from a left side holding a pattern evaluated per graph")
    void minusAfterPatternPerGraph() throws Exception
    {
        String query = "SELECT ?g ?x WHERE { GRAPH ?g { { SELECT ?x WHERE { ?x ex:s ?y } } } "
                + "MINUS { ?x ex:t+ ?z . ?z ex:t ?w } }";

        assertThat(shared(translate(query)), containsString(" CROSS JOIN LATERAL "));
        assertThat(execute(query), containsInAnyOrder(row(b(200), a(3))));
    }
}
