package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDouble;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDoubleIri;
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
 * Walks of property paths over steps that lead back to the node they depart from. Such a row of the step gives the
 * recursion only a pair it holds already, so the recursive term leaves it out before the join; the first step keeps it,
 * as a loop makes a node reach itself. The nodes are compared as terms: a step between two zeros of xsd:double of
 * different signs leads to another node.
 */
public class WalkLoopTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> PREFIX a: <http://example.org/a/> ";

    /**
     * Configuration mapping the test tables.
     */
    private static SparqlDatabaseConfiguration config = null;

    /**
     * The same configuration with the join optimizer enabled.
     */
    private static SparqlDatabaseConfiguration optimized = null;


    /**
     * Creates the {@code walk_loop_test} schema, whose tables are mapped with the IRI classes {@code a}
     * ({@code http://example.org/a/<id>}) and {@code b} ({@code http://example.org/b/<id>}): {@code ex:r} relates
     * {@code a/1}, {@code a/2} and {@code a/3} to each other and to themselves, {@code a/4} to itself and {@code a/5}
     * to {@code a/6}; {@code ex:t} holds the triples of {@code ex:r} and {@code a/3 ex:t b/30}, {@code b/30 ex:t b/30},
     * {@code b/30 ex:t b/31} and {@code b/31 ex:t b/30}; {@code ex:p} and {@code ex:q} give xsd:double numbers of
     * {@code a/1}, {@code a/2} and {@code a/3}, so that the path {@code ^ex:p/ex:q} leads from 5 to 0, from 0 to -0 and
     * from 7 to 7.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            String tables = "walk_loop_test.";

            statement.execute("create schema walk_loop_test");
            statement.execute("create table " + tables + "r (s int not null, o int not null, primary key (s, o))");
            statement.execute("insert into " + tables
                    + "r values (1, 1), (2, 2), (3, 3), (1, 2), (2, 1), (2, 3), (3, 2), (4, 4), (5, 6)");
            statement.execute("create table " + tables + "ab (s int not null, o int not null)");
            statement.execute("insert into " + tables + "ab values (3, 30)");
            statement.execute("create table " + tables + "bb (s int not null, o int not null)");
            statement.execute("insert into " + tables + "bb values (30, 30), (30, 31), (31, 30)");
            statement.execute("create table " + tables + "p (s int not null, o float8 not null)");
            statement.execute("insert into " + tables + "p values (1, 5), (2, '0'), (3, 7)");
            statement.execute("create table " + tables + "q (s int not null, o float8 not null)");
            statement.execute("insert into " + tables + "q values (1, '0'), (2, '-0'), (3, 7)");
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

        configuration.addQuadMapping(table("ab"), null, configuration.createIriMapping("a", "s"),
                configuration.createIriMapping("ex:t"), configuration.createIriMapping("b", "o"));
        configuration.addQuadMapping(table("bb"), null, configuration.createIriMapping("b", "s"),
                configuration.createIriMapping("ex:t"), configuration.createIriMapping("b", "o"));
        configuration.addQuadMapping(table("p"), null, configuration.createIriMapping("a", "s"),
                configuration.createIriMapping("ex:p"), configuration.createLiteralMapping(xsdDouble, "o"));
        configuration.addQuadMapping(table("q"), null, configuration.createIriMapping("a", "s"),
                configuration.createIriMapping("ex:q"), configuration.createLiteralMapping(xsdDouble, "o"));

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
        return new DatabaseTable("walk_loop_test", name);
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


    @Test
    @DisplayName("a walk over a step whose nodes relate to themselves")
    void loops() throws Exception
    {
        String query = "SELECT ?x ?y WHERE { ?x ex:r+ ?y }";

        assertThat(translate(config, query), containsString("not ((\"tab1\".\"s\" = \"tab1\".\"o\"))"));
        assertThat(translate(optimized, query), containsString("not ((\"s\" = \"o\"))"));
        assertThat(execute(query),
                containsInAnyOrder(row(a(1), a(1)), row(a(1), a(2)), row(a(1), a(3)), row(a(2), a(1)), row(a(2), a(2)),
                        row(a(2), a(3)), row(a(3), a(1)), row(a(3), a(2)), row(a(3), a(3)), row(a(4), a(4)),
                        row(a(5), a(6))));
    }


    @Test
    @DisplayName("a walk of zero or more steps from a constant")
    void zeroOrMore() throws Exception
    {
        assertThat(execute("SELECT ?y WHERE { a:1 ex:r* ?y }"), containsInAnyOrder(row(a(1)), row(a(2)), row(a(3))));
        assertThat(execute("SELECT ?y WHERE { a:5 ex:r* ?y }"), containsInAnyOrder(row(a(5)), row(a(6))));
    }


    @Test
    @DisplayName("a walk whose step reaches nodes of two classes, joined branch by branch")
    void lateralStep() throws Exception
    {
        String query = "SELECT ?x ?y WHERE { ?x ex:t+ ?y }";

        assertThat(translate(config, query), containsString(" CROSS JOIN LATERAL "));
        assertThat(execute(query),
                containsInAnyOrder(row(a(1), a(1)), row(a(1), a(2)), row(a(1), a(3)), row(a(1), b(30)),
                        row(a(1), b(31)), row(a(2), a(1)), row(a(2), a(2)), row(a(2), a(3)), row(a(2), b(30)),
                        row(a(2), b(31)), row(a(3), a(1)), row(a(3), a(2)), row(a(3), a(3)), row(a(3), b(30)),
                        row(a(3), b(31)), row(a(4), a(4)), row(a(5), a(6)), row(b(30), b(30)), row(b(30), b(31)),
                        row(b(31), b(30)), row(b(31), b(31))));
    }


    @Test
    @DisplayName("a walk over a step between literals, whose zeros of different signs are different nodes")
    void literalLoops() throws Exception
    {
        assertThat(execute("SELECT ?x ?y WHERE { ?x (^ex:p/ex:q)+ ?y }"),
                containsInAnyOrder(row(number("5.0E0"), number("0.0E0")), row(number("5.0E0"), number("-0.0E0")),
                        row(number("0.0E0"), number("-0.0E0")), row(number("7.0E0"), number("7.0E0"))));
    }


    @Test
    @DisplayName("a walk seeded with the values of its start")
    void seeded() throws Exception
    {
        assertThat(execute("SELECT ?x ?y WHERE { VALUES ?x { a:1 a:5 } ?x ex:r+ ?y }"),
                containsInAnyOrder(row(a(1), a(1)), row(a(1), a(2)), row(a(1), a(3)), row(a(5), a(6))));
    }


    @Test
    @DisplayName("a walk without the start the parent does not need")
    void withoutStart() throws Exception
    {
        assertThat(execute("SELECT DISTINCT ?y WHERE { ?x ex:r+ ?y }"),
                containsInAnyOrder(row(a(1)), row(a(2)), row(a(3)), row(a(4)), row(a(6))));
    }


    @Test
    @DisplayName("a walk inside EXISTS starting from the node of the current solution")
    void exists() throws Exception
    {
        assertThat(execute("SELECT DISTINCT ?x WHERE { ?x ex:r ?z FILTER EXISTS { ?x ex:r+ ?y FILTER(?y = a:3) } }"),
                containsInAnyOrder(row(a(1)), row(a(2)), row(a(3))));
    }
}
