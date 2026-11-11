package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.iri;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
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
import cz.iocb.sparql.engine.mapping.ConstantIriMapping;
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
 * EXISTS and NOT EXISTS of a union whose branches bind a variable of the current solution in different classes. The
 * test compares the solutions of the pattern with the current solution, and the pattern is optimised for the classes
 * the current solution binds the variable in, so a branch may bind the variable in a class without columns, which the
 * union merges with the overlapping classes of the other branches: the comparison has to stay exact for every class of
 * the current solution (it once failed on the null columns), and a solution binding the variable in a class without
 * columns must not be taken for one leaving the variable unbound, so a variable a branch leaves unbound is merged with
 * its current value inside the pattern, branch by branch.
 */
public class ExistsUnionComparisonTest
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
     * Creates the {@code exists_union_comparison_test} schema, whose tables are mapped with the IRI classes {@code a},
     * {@code b}, {@code c} ({@code http://example.org/a/<id>} and so on) and {@code y} ({@code http://example.org/x/1}
     * followed by the id): in the default graph {@code a ex:p1 b}, {@code a ex:p2 a}, {@code b ex:p3 c},
     * {@code b ex:p6} with IRIs of any class, {@code a ex:p7} with terms of any kind and {@code y ex:p11 a};
     * {@code b ex:p9} with a string in the graph {@code ex:g1}; {@code c ex:p1 a} and {@code a ex:p2 b} in the graph
     * {@code ex:g2}. The default graph includes the named graphs.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            String tables = "exists_union_comparison_test.";

            statement.execute("create schema exists_union_comparison_test");
            statement.execute("create table " + tables + "ab (s int not null, o int not null)");
            statement.execute("insert into " + tables + "ab values (1, 10), (2, 20), (3, 30)");
            statement.execute("create table " + tables + "aa (s int not null, o int not null)");
            statement.execute("insert into " + tables + "aa values (1, 2), (2, 3)");
            statement.execute("create table " + tables + "bc (s int not null, o int not null)");
            statement.execute("insert into " + tables + "bc values (10, 100), (20, 200)");
            statement.execute("create table " + tables + "biri (s int not null, o varchar not null)");
            statement.execute("insert into " + tables + "biri values (10, 'http://example.org/a/1'), "
                    + "(20, 'http://example.org/x/15'), (30, 'http://example.org/other/1')");
            statement.execute("create table " + tables + "abox (s int not null, o sparql.rdfbox not null)");
            statement.execute("insert into " + tables + "abox values (1, '<http://example.org/b/10>'), (2, '\"x\"'), "
                    + "(3, '\"1\"^^<http://www.w3.org/2001/XMLSchema#int>'), (1, '<http://example.org/x/7>')");
            statement.execute("create table " + tables + "bstr (s int not null, o varchar not null)");
            statement.execute("insert into " + tables + "bstr values (10, 'w')");
            statement.execute("create table " + tables + "ya (s int not null, o int not null)");
            statement.execute("insert into " + tables + "ya values (5, 1), (3, 2)");
            statement.execute("create table " + tables + "ca (s int not null, o int not null)");
            statement.execute("insert into " + tables + "ca values (100, 1)");
            statement.execute("create table " + tables + "ab2 (s int not null, o int not null)");
            statement.execute("insert into " + tables + "ab2 values (3, 30)");
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
        configuration.addIriClass(new IntegerUserIriClass("y", INT4, "http://example.org/x/1"));

        ConstantIriMapping g1 = configuration.createIriMapping("ex:g1");
        ConstantIriMapping g2 = configuration.createIriMapping("ex:g2");

        configuration.addQuadMapping(table("ab"), null, configuration.createIriMapping("a", "s"),
                configuration.createIriMapping("ex:p1"), configuration.createIriMapping("b", "o"));

        configuration.addQuadMapping(table("aa"), null, configuration.createIriMapping("a", "s"),
                configuration.createIriMapping("ex:p2"), configuration.createIriMapping("a", "o"));

        configuration.addQuadMapping(table("bc"), null, configuration.createIriMapping("b", "s"),
                configuration.createIriMapping("ex:p3"), configuration.createIriMapping("c", "o"));

        configuration.addQuadMapping(table("biri"), null, configuration.createIriMapping("b", "s"),
                configuration.createIriMapping("ex:p6"), configuration.createIriMapping(iri, "o"));

        configuration.addQuadMapping(table("abox"), null, configuration.createIriMapping("a", "s"),
                configuration.createIriMapping("ex:p7"), configuration.createTermMapping(box, "o"));

        configuration.addQuadMapping(table("bstr"), g1, configuration.createIriMapping("b", "s"),
                configuration.createIriMapping("ex:p9"), configuration.createLiteralMapping(xsdString, "o"));

        configuration.addQuadMapping(table("ya"), null, configuration.createIriMapping("y", "s"),
                configuration.createIriMapping("ex:p11"), configuration.createIriMapping("a", "o"));

        configuration.addQuadMapping(table("ca"), g2, configuration.createIriMapping("c", "s"),
                configuration.createIriMapping("ex:p1"), configuration.createIriMapping("a", "o"));

        configuration.addQuadMapping(table("ab2"), g2, configuration.createIriMapping("a", "s"),
                configuration.createIriMapping("ex:p2"), configuration.createIriMapping("b", "o"));

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
        return new DatabaseTable("exists_union_comparison_test", name);
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
     * The EXISTS tests of the SQL, each as its pattern and the condition comparing the solutions of the pattern with
     * the current solution (empty when there is none).
     *
     * @param sql the SQL
     * @return the patterns and the conditions of the EXISTS tests of the SQL
     */
    private static List<String[]> tests(String sql)
    {
        List<String[]> tests = new ArrayList<>();
        String head = "EXISTS (SELECT 1 FROM (";

        for(int begin = sql.indexOf(head); begin >= 0; begin = sql.indexOf(head, begin + 1))
        {
            int pattern = closing(sql, begin + head.length());
            int test = closing(sql, pattern + 1);

            tests.add(new String[] { sql.substring(begin + head.length(), pattern), sql.substring(pattern, test) });
        }

        return tests;
    }


    /**
     * Position of the parenthesis closing the one that the text at the position lies in.
     *
     * @param sql the SQL
     * @param position position inside the parentheses
     * @return position of the closing parenthesis
     */
    private static int closing(String sql, int position)
    {
        for(int depth = 1; depth > 0; position++)
            depth += sql.charAt(position) == '(' ? 1 : sql.charAt(position) == ')' ? -1 : 0;

        return position - 1;
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
    @DisplayName("NOT EXISTS of any triple with an object the current solution may leave unbound")
    void anyTriple() throws Exception
    {
        String query = "SELECT ?o WHERE { { ?s ex:p1 ?w } UNION { ?o ex:p6 ?x } FILTER NOT EXISTS { ?v ?p ?o } }";

        assertThat(execute(query), empty());
    }


    @Test
    @DisplayName("EXISTS of a union binding the variable in classes the current solution has not")
    void existsUnion() throws Exception
    {
        String query = "SELECT ?w WHERE { { ?s ex:p1 ?w } UNION { ?w ex:p3 ?o } "
                + "FILTER EXISTS { { ?v ex:p6 ?o } UNION { ?v ex:p11 ?o } } }";

        assertThat(execute(query), containsInAnyOrder(row(resource("a/1")), row(resource("b/10")),
                row(resource("b/20")), row(resource("b/30"))));
    }


    @Test
    @DisplayName("NOT EXISTS of a union with the variable bound by an OPTIONAL")
    void optionalNotExists() throws Exception
    {
        String query = "SELECT ?x WHERE { OPTIONAL { ?o ex:p6 ?x } "
                + "FILTER NOT EXISTS { { ?v ex:p7 ?o } UNION { ?v ex:p2 ?o } } }";

        assertThat(execute(query), containsInAnyOrder(row(resource("x/15"))));
    }


    @Test
    @DisplayName("EXISTS of a union leaving the variable unbound in a branch")
    void existsUnbound() throws Exception
    {
        String query = "SELECT ?s ?o WHERE { { ?s ex:p3 ?o } UNION { ?s ex:p6 ?q } "
                + "FILTER EXISTS { { ?v ex:p11 ?o } UNION { ?s ex:p9 ?z } } }";

        assertThat(execute(query), containsInAnyOrder(row(resource("b/10"), resource("c/100")),
                row(resource("b/10"), null), row(resource("b/20"), null), row(resource("b/30"), null)));
    }


    @Test
    @DisplayName("NOT EXISTS of a union leaving the variable unbound in a branch")
    void notExistsUnbound() throws Exception
    {
        String query = "SELECT ?s ?o WHERE { { ?s ex:p3 ?o } UNION { ?s ex:p6 ?q } "
                + "FILTER NOT EXISTS { { ?v ex:p11 ?o } UNION { ?s ex:p9 ?z } } }";

        assertThat(execute(query), containsInAnyOrder(row(resource("b/20"), resource("c/200"))));
    }


    @Test
    @DisplayName("a union compared exactly keeps the semi-join form")
    void comparableUnion() throws Exception
    {
        String query = "SELECT ?s WHERE { { ?s ?p ?o } UNION { ?s ?p ?x } FILTER NOT EXISTS { ?o ?d ?a } }";

        assertThat(execute(query), containsInAnyOrder(row(resource("a/1")), row(resource("a/2")), row(resource("a/3")),
                row(resource("b/10")), row(resource("b/20")), row(resource("b/30"))));

        // the patterns do not refer to the current solution, only the tests compare them with it
        List<String> patterns = tests(translate(query)).stream().map(t -> t[0]).toList();
        assertThat(patterns, not(empty()));
        assertThat(patterns, everyItem(not(containsString("\"outer0\""))));
    }


    @Test
    @DisplayName("a variable a branch of the union leaves unbound is compared inside the branch, not by a disjunction")
    void unboundMergedInBranch() throws Exception
    {
        String query = "SELECT ?s ?o WHERE { ?s ex:p3 ?o "
                + "FILTER NOT EXISTS { { ?v ex:p11 ?o } UNION { ?s ex:p9 ?z } } }";

        assertThat(execute(query), containsInAnyOrder(row(resource("b/20"), resource("c/200"))));

        // each branch agrees with the current solution by itself, so the test has nothing left to compare
        List<String[]> tests = tests(translate(query));
        assertThat(tests, hasSize(1));
        assertThat(tests.get(0)[0], containsString("\"outer0\""));
        assertThat(tests.get(0)[1], not(containsString(" OR ")));
    }
}
