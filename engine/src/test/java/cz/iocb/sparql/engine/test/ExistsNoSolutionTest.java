package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.iri;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
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
import cz.iocb.sparql.engine.rdf.TypedLiteral;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.LimitExceedException;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.translator.ServiceException;
import cz.iocb.sparql.testing.Database;



/**
 * EXISTS held by a node whose input has no solution: a pattern that the classes of its variables leave no solution of,
 * or the right side of an OPTIONAL that joins no solution of the left side. The pattern of the test is translated for
 * the values of the variables its current solution binds always (a merge with such a value binds its variable only in
 * the classes of the value), so it is not optimised for bindings that leave such a variable out: it once was, the
 * merges fell away with their values, and a comparison inside the pattern met a class they had excluded, which has no
 * columns.
 */
public class ExistsNoSolutionTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> ";

    /**
     * Pattern without a solution: {@code ex:p9} binds its subject in the class {@code b}, whereas the objects of
     * {@code ex:p11} are of the classes {@code a} and {@code c}.
     */
    private static final String none = "?w ex:p9 ?w . ?s ex:p11 ?w . ?s ex:p3 ?o . ";

    /**
     * Test whose pattern merges the subject of the right side of its OPTIONAL with the current value of {@code ?s} and
     * compares it inside.
     */
    private static final String test = "EXISTS { OPTIONAL { ?v ex:p1 ?s . ?s ex:p11 ?o . FILTER(?v = ?s) } }";

    /**
     * Configuration mapping the test tables.
     */
    private static SparqlDatabaseConfiguration config = null;

    /**
     * The same configuration with the join optimizer enabled.
     */
    private static SparqlDatabaseConfiguration optimized = null;


    /**
     * Creates the {@code exists_no_solution_test} schema, whose tables are mapped with the IRI classes {@code a},
     * {@code b}, {@code c} and {@code x} ({@code http://example.org/a/<id>} and so on): {@code b ex:p1} and
     * {@code b ex:p9} with IRIs of any class, {@code b ex:p2} with a string and {@code c ex:p2} with IRIs of any class,
     * {@code b ex:p3} with a string and {@code a ex:p3} with terms of any kind, {@code c ex:p10 x} and
     * {@code b ex:p10 c}, {@code x ex:p11 a} and {@code b ex:p11 c}, {@code a ex:p12 b} and {@code x ex:p12 x}.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            String tables = "exists_no_solution_test.";

            statement.execute("create schema exists_no_solution_test");
            statement.execute("create table " + tables + "biri (s int not null, o varchar not null)");
            statement.execute("insert into " + tables + "biri values (10, 'http://example.org/a/1'), "
                    + "(20, 'http://example.org/x/15'), (10, 'http://example.org/b/10')");
            statement.execute("create table " + tables + "ciri (s int not null, o varchar not null)");
            statement.execute("insert into " + tables + "ciri values (100, 'http://example.org/a/1'), "
                    + "(200, 'http://example.org/b/20')");
            statement.execute("create table " + tables + "bstr (s int not null, o varchar not null)");
            statement.execute("insert into " + tables + "bstr values (10, 'w'), (20, 'v')");
            statement.execute("create table " + tables + "abox (s int not null, o sparql.rdfbox not null)");
            statement.execute("insert into " + tables + "abox values (1, '<http://example.org/b/10>'), (2, '\"w\"')");
            statement.execute("create table " + tables + "xa (s int not null, o int not null)");
            statement.execute("insert into " + tables + "xa values (13, 1), (15, 2)");
            statement.execute("create table " + tables + "bc (s int not null, o int not null)");
            statement.execute("insert into " + tables + "bc values (10, 100), (20, 200)");
            statement.execute("create table " + tables + "cx (s int not null, o int not null)");
            statement.execute("insert into " + tables + "cx values (100, 13), (200, 15)");
            statement.execute("create table " + tables + "ab (s int not null, o int not null)");
            statement.execute("insert into " + tables + "ab values (1, 10), (2, 20)");
            statement.execute("create table " + tables + "xx (s int not null, o int not null)");
            statement.execute("insert into " + tables + "xx values (13, 13), (15, 25)");
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
        configuration.addIriClass(new IntegerUserIriClass("c", INT4, "http://example.org/c/"));
        configuration.addIriClass(new IntegerUserIriClass("x", INT4, "http://example.org/x/"));

        configuration.addQuadMapping(table("biri"), null, configuration.createIriMapping("b", "s"),
                configuration.createIriMapping("ex:p1"), configuration.createIriMapping(iri, "o"));

        configuration.addQuadMapping(table("biri"), null, configuration.createIriMapping("b", "s"),
                configuration.createIriMapping("ex:p9"), configuration.createIriMapping(iri, "o"));

        configuration.addQuadMapping(table("bstr"), null, configuration.createIriMapping("b", "s"),
                configuration.createIriMapping("ex:p2"), configuration.createLiteralMapping(xsdString, "o"));

        configuration.addQuadMapping(table("ciri"), null, configuration.createIriMapping("c", "s"),
                configuration.createIriMapping("ex:p2"), configuration.createIriMapping(iri, "o"));

        configuration.addQuadMapping(table("bstr"), null, configuration.createIriMapping("b", "s"),
                configuration.createIriMapping("ex:p3"), configuration.createLiteralMapping(xsdString, "o"));

        configuration.addQuadMapping(table("abox"), null, configuration.createIriMapping("a", "s"),
                configuration.createIriMapping("ex:p3"), configuration.createTermMapping(box, "o"));

        configuration.addQuadMapping(table("cx"), null, configuration.createIriMapping("c", "s"),
                configuration.createIriMapping("ex:p10"), configuration.createIriMapping("x", "o"));

        configuration.addQuadMapping(table("bc"), null, configuration.createIriMapping("b", "s"),
                configuration.createIriMapping("ex:p10"), configuration.createIriMapping("c", "o"));

        configuration.addQuadMapping(table("xa"), null, configuration.createIriMapping("x", "s"),
                configuration.createIriMapping("ex:p11"), configuration.createIriMapping("a", "o"));

        configuration.addQuadMapping(table("bc"), null, configuration.createIriMapping("b", "s"),
                configuration.createIriMapping("ex:p11"), configuration.createIriMapping("c", "o"));

        configuration.addQuadMapping(table("ab"), null, configuration.createIriMapping("a", "s"),
                configuration.createIriMapping("ex:p12"), configuration.createIriMapping("b", "o"));

        configuration.addQuadMapping(table("xx"), null, configuration.createIriMapping("x", "s"),
                configuration.createIriMapping("ex:p12"), configuration.createIriMapping("x", "o"));

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
        return new DatabaseTable("exists_no_solution_test", name);
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


    /**
     * Rows of the subjects and the objects of {@code ex:p10}, each followed by an unbound variable.
     *
     * @return rows of the subjects and the objects of {@code ex:p10}, each followed by an unbound variable
     */
    private static Object[] links()
    {
        return new Object[] { row(resource("b/10"), resource("c/100"), null),
                row(resource("b/20"), resource("c/200"), null), row(resource("c/100"), resource("x/13"), null),
                row(resource("c/200"), resource("x/15"), null) };
    }


    @Test
    @DisplayName("a filter of a pattern without a solution")
    void filter() throws Exception
    {
        assertThat(execute("SELECT * WHERE { " + none + "FILTER(" + test + ") }"), empty());
        assertThat(execute("SELECT * WHERE { " + none + "FILTER(!(" + test + ")) }"), empty());
        assertThat(execute("SELECT * WHERE { " + none + "FILTER(?s = ?w || " + test + ") }"), empty());
    }


    @Test
    @DisplayName("a BIND, a projection and an ordering of a pattern without a solution")
    void bind() throws Exception
    {
        assertThat(execute("SELECT * WHERE { " + none + "BIND(" + test + " AS ?b) }"), empty());
        assertThat(execute("SELECT (" + test + " AS ?e) WHERE { " + none + "}"), empty());
        assertThat(execute("SELECT * WHERE { " + none + "} ORDER BY (" + test + ")"), empty());
    }


    @Test
    @DisplayName("no solutions make no group of a GROUP BY clause and the one group there is without it")
    void having() throws Exception
    {
        TypedLiteral zero = new TypedLiteral("0", xsdIntegerIri);

        assertThat(execute("SELECT ?s (COUNT(*) AS ?c) WHERE { " + none + "} GROUP BY ?s"), empty());
        assertThat(execute("SELECT ?s (COUNT(*) AS ?c) WHERE { " + none + "} GROUP BY ?s HAVING(" + test + ")"),
                empty());

        // the pattern of the test starts with an OPTIONAL, so it has a solution for the group
        assertThat(execute("SELECT (COUNT(*) AS ?c) WHERE { " + none + "}"), contains(row(zero)));
        assertThat(execute("SELECT (COUNT(*) AS ?c) WHERE { " + none + "} HAVING(" + test + ")"), contains(row(zero)));
    }


    @Test
    @DisplayName("the right side of an OPTIONAL without a solution")
    void optionalRightSide() throws Exception
    {
        String query = "SELECT ?a ?b ?s WHERE { ?a ex:p10 ?b OPTIONAL { " + none + "FILTER(" + test + ") } }";

        assertThat(execute(query), containsInAnyOrder(links()));
    }


    @Test
    @DisplayName("the right side of an OPTIONAL without a solution, whose variable the left side may leave unbound")
    void optionalVariable() throws Exception
    {
        // the left side binds ?t in its OPTIONAL only, so the variable stays, possibly unbound, without the right side
        String query = """
                SELECT ?s ?t WHERE { ?s ex:p10 ?o OPTIONAL { ?o ex:p12 ?t }
                OPTIONAL { ?w ex:p9 ?w . ?t ex:p11 ?w . ?t ex:p3 ?q .
                FILTER(EXISTS { OPTIONAL { ?v ex:p1 ?t . ?t ex:p11 ?q . FILTER(?v = ?t) } }) } }
                """;

        assertThat(execute(query), containsInAnyOrder(row(resource("b/10"), null), row(resource("b/20"), null),
                row(resource("c/100"), resource("x/13")), row(resource("c/200"), resource("x/25"))));
    }


    @Test
    @DisplayName("a pattern without a solution inside a NOT EXISTS and a MINUS")
    void nestedPattern() throws Exception
    {
        String nested = "{ " + none + "FILTER(" + test + ") }";

        assertThat(execute("SELECT ?a ?b ?z WHERE { ?a ex:p10 ?b FILTER NOT EXISTS " + nested + " }"),
                containsInAnyOrder(links()));
        assertThat(execute("SELECT ?a ?b ?z WHERE { ?a ex:p10 ?b MINUS " + nested + " }"), containsInAnyOrder(links()));
    }


    @Test
    @DisplayName("a condition of an OPTIONAL whose right side joins no solution of the left side")
    void optionalCondition() throws Exception
    {
        // ex:p12 continues the objects of ex:p10 only to IRIs of the class x, which no subject of ex:p10 is
        String query = "SELECT * WHERE { ?s ex:p10 ?o OPTIONAL { ?o ex:p12 ?t "
                + "FILTER(?t = ?s && EXISTS { ?t ex:p2 ?u FILTER(?u != ?o) }) } }";

        assertThat(execute(query), containsInAnyOrder(links()));
    }
}
