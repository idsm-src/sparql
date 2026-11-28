package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDouble;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDoubleIri;
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
import cz.iocb.sparql.engine.config.EngineSettings.JoinOptimizerMode;
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
 * Walks of property paths evaluated for every start by itself: a walk between two variables from all its initial
 * solutions, whose initial solutions and step can be looked up by an index, joins its distinct starts laterally with a
 * recursion that reads the initial solutions of one start and keeps only the nodes that start reaches, instead of the
 * pairs of all starts. The paths stay in the graph of their start, the starts that are equal values without being the
 * same terms stay apart, and a walk whose tables cannot be looked up, a seeded walk, a walk of zero or more steps and a
 * walk starting from the node of the current solution of an EXISTS pattern are evaluated as a whole.
 */
public class WalkPerStartTest
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
     * Configuration mapping the test tables.
     */
    private static SparqlDatabaseConfiguration config = null;

    /**
     * The same configuration with the join optimizer enabled.
     */
    private static SparqlDatabaseConfiguration optimized = null;


    /**
     * Creates the {@code walk_per_start_test} schema, whose tables are mapped with the IRI classes {@code a}
     * ({@code http://example.org/a/<id>}) and {@code b} ({@code http://example.org/b/<id>}): {@code ex:r} relates
     * {@code a/1} to {@code a/2}, {@code a/2} to {@code a/3}, {@code a/3} to {@code a/1} and {@code a/4}, {@code a/5}
     * to itself and {@code a/6} to {@code a/7}, and so does {@code ex:u}, read from a table without an index;
     * {@code ex:q} relates {@code a/1} to {@code a/2} and {@code a/2} to {@code a/3} in the graph {@code a/100}, and
     * {@code a/2} to {@code a/4} and {@code a/4} to {@code a/1} in the graph {@code a/101}; {@code ex:t} holds the
     * triples of {@code ex:r} and {@code a/4 ex:t b/30}, {@code b/30 ex:t b/31} and {@code b/31 ex:t b/30};
     * {@code ex:n} relates xsd:double numbers: 0 to 1, -0 to 2 and 1 to 3.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            String tables = "walk_per_start_test.";
            String rows = " values (1, 2), (2, 3), (3, 1), (3, 4), (5, 5), (6, 7)";

            statement.execute("create schema walk_per_start_test");
            statement.execute("create table " + tables + "r (s int not null, o int not null, primary key (s, o))");
            statement.execute("insert into " + tables + "r" + rows);
            statement.execute("create table " + tables + "u (s int not null, o int not null)");
            statement.execute("insert into " + tables + "u" + rows);
            statement.execute("create table " + tables
                    + "q (g int not null, s int not null, o int not null, primary key (g, s, o))");
            statement.execute("insert into " + tables + "q values (100, 1, 2), (100, 2, 3), (101, 2, 4), (101, 4, 1)");
            statement.execute("create table " + tables + "ab (s int not null, o int not null, primary key (s, o))");
            statement.execute("insert into " + tables + "ab values (4, 30)");
            statement.execute("create table " + tables + "bb (s int not null, o int not null, primary key (s, o))");
            statement.execute("insert into " + tables + "bb values (30, 31), (31, 30)");
            statement
                    .execute("create table " + tables + "n (s float8 not null, o float8 not null, primary key (s, o))");
            statement.execute("insert into " + tables + "n values ('0', 1), ('-0', 2), (1, 3)");
        }

        DatabaseSchema schema = new DatabaseSchema(connectionPool);

        config = configuration(connectionPool, schema);
        optimized = configuration(connectionPool, schema);
        optimized.setEngineSettings(optimized.getEngineSettings().withJoinOptimizer(JoinOptimizerMode.SETTINGS));
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

        for(String predicate : List.of("ex:r", "ex:t"))
            configuration.addQuadMapping(table("r"), null, configuration.createIriMapping("a", "s"),
                    configuration.createIriMapping(predicate), configuration.createIriMapping("a", "o"));

        configuration.addQuadMapping(table("u"), null, configuration.createIriMapping("a", "s"),
                configuration.createIriMapping("ex:u"), configuration.createIriMapping("a", "o"));
        configuration.addQuadMapping(table("q"), configuration.createIriMapping("a", "g"),
                configuration.createIriMapping("a", "s"), configuration.createIriMapping("ex:q"),
                configuration.createIriMapping("a", "o"));
        configuration.addQuadMapping(table("ab"), null, configuration.createIriMapping("a", "s"),
                configuration.createIriMapping("ex:t"), configuration.createIriMapping("b", "o"));
        configuration.addQuadMapping(table("bb"), null, configuration.createIriMapping("b", "s"),
                configuration.createIriMapping("ex:t"), configuration.createIriMapping("b", "o"));
        configuration.addQuadMapping(table("n"), null, configuration.createLiteralMapping(xsdDouble, "s"),
                configuration.createIriMapping("ex:n"), configuration.createLiteralMapping(xsdDouble, "o"));

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
        return new DatabaseTable("walk_per_start_test", name);
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
     * Literal of xsd:double in its canonical form.
     *
     * @param lexicalForm the canonical lexical form
     * @return literal of xsd:double
     */
    private static TypedLiteral number(String lexicalForm)
    {
        return new TypedLiteral(lexicalForm, xsdDoubleIri);
    }


    /**
     * The pairs of the walk of {@code ex:r} (or of {@code ex:u}, which holds the same triples).
     *
     * @return the pairs of the walk of {@code ex:r}
     */
    private static Object[] pairs()
    {
        return new Object[] { row(a(1), a(1)), row(a(1), a(2)), row(a(1), a(3)), row(a(1), a(4)), row(a(2), a(1)),
                row(a(2), a(2)), row(a(2), a(3)), row(a(2), a(4)), row(a(3), a(1)), row(a(3), a(2)), row(a(3), a(3)),
                row(a(3), a(4)), row(a(5), a(5)), row(a(6), a(7)) };
    }


    @Test
    @DisplayName("a walk from all its initial solutions over a table looked up by an index")
    void perStart() throws Exception
    {
        String query = "SELECT ?x ?y WHERE { ?x ex:r+ ?y }";

        assertThat(translate(config, query), containsString(perStart));
        assertThat(translate(optimized, query), containsString(perStart));
        assertThat(execute(query), containsInAnyOrder(pairs()));
    }


    @Test
    @DisplayName("a walk over a table without an index is evaluated as a whole")
    void withoutIndex() throws Exception
    {
        String query = "SELECT ?x ?y WHERE { ?x ex:u+ ?y }";

        assertThat(translate(config, query), not(containsString(perStart)));
        assertThat(execute(query), containsInAnyOrder(pairs()));
    }


    @Test
    @DisplayName("a walk in a graph stays in the graph of its start")
    void graph() throws Exception
    {
        String query = "SELECT ?g ?x ?y WHERE { GRAPH ?g { ?x ex:q+ ?y } }";

        assertThat(translate(config, query), containsString(perStart));
        assertThat(translate(optimized, query), containsString(perStart));
        assertThat(execute(query), containsInAnyOrder(row(a(100), a(1), a(2)), row(a(100), a(1), a(3)),
                row(a(100), a(2), a(3)), row(a(101), a(2), a(4)), row(a(101), a(2), a(1)), row(a(101), a(4), a(1))));
    }


    @Test
    @DisplayName("a walk in a graph the parent does not need stays in the graph of its start")
    void unneededGraph() throws Exception
    {
        assertThat(execute("SELECT ?x ?y WHERE { GRAPH ?g { ?x ex:q+ ?y } }"), containsInAnyOrder(row(a(1), a(2)),
                row(a(1), a(3)), row(a(2), a(3)), row(a(2), a(4)), row(a(2), a(1)), row(a(4), a(1))));
    }


    @Test
    @DisplayName("a walk whose step reaches nodes of two classes")
    void twoClasses() throws Exception
    {
        String query = "SELECT ?x ?y WHERE { ?x ex:t+ ?y }";

        assertThat(translate(config, query), containsString(perStart));
        assertThat(execute(query),
                containsInAnyOrder(row(a(1), a(1)), row(a(1), a(2)), row(a(1), a(3)), row(a(1), a(4)), row(a(1), b(30)),
                        row(a(1), b(31)), row(a(2), a(1)), row(a(2), a(2)), row(a(2), a(3)), row(a(2), a(4)),
                        row(a(2), b(30)), row(a(2), b(31)), row(a(3), a(1)), row(a(3), a(2)), row(a(3), a(3)),
                        row(a(3), a(4)), row(a(3), b(30)), row(a(3), b(31)), row(a(4), b(30)), row(a(4), b(31)),
                        row(a(5), a(5)), row(a(6), a(7)), row(b(30), b(30)), row(b(30), b(31)), row(b(31), b(30)),
                        row(b(31), b(31))));
    }


    @Test
    @DisplayName("starts of zeros of different signs are different starts")
    void zeros() throws Exception
    {
        String query = "SELECT ?x ?y WHERE { ?x ex:n+ ?y }";

        assertThat(translate(config, query), containsString(perStart));
        assertThat(execute(query),
                containsInAnyOrder(row(number("0.0E0"), number("1.0E0")), row(number("0.0E0"), number("3.0E0")),
                        row(number("-0.0E0"), number("2.0E0")), row(number("1.0E0"), number("3.0E0"))));
    }


    @Test
    @DisplayName("a seeded walk is evaluated as a whole")
    void seeded() throws Exception
    {
        String query = "SELECT ?x ?y WHERE { VALUES ?x { a:1 a:5 } ?x ex:r+ ?y }";

        assertThat(translate(config, query), not(containsString(perStart)));
        assertThat(execute(query), containsInAnyOrder(row(a(1), a(1)), row(a(1), a(2)), row(a(1), a(3)),
                row(a(1), a(4)), row(a(5), a(5))));
    }


    @Test
    @DisplayName("a walk of zero or more steps is evaluated as a whole")
    void zeroOrMore() throws Exception
    {
        assertThat(translate(config, "SELECT ?x ?y WHERE { ?x ex:r* ?y }"), not(containsString(perStart)));
    }


    @Test
    @DisplayName("a walk inside EXISTS starting from the node of the current solution is evaluated as a whole")
    void exists() throws Exception
    {
        String query = "SELECT DISTINCT ?x WHERE { ?x ex:r ?z FILTER EXISTS { ?x ex:r+ ?y FILTER(?y = a:4) } }";

        assertThat(translate(config, query), not(containsString(perStart)));
        assertThat(execute(query), containsInAnyOrder(row(a(1)), row(a(2)), row(a(3))));
    }


    @Test
    @DisplayName("a walk without the start the parent does not need")
    void withoutStart() throws Exception
    {
        assertThat(execute("SELECT DISTINCT ?y WHERE { ?x ex:r+ ?y }"),
                containsInAnyOrder(row(a(1)), row(a(2)), row(a(3)), row(a(4)), row(a(5)), row(a(7))));
    }
}
