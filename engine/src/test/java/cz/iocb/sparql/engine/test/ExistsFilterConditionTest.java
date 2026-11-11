package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdBooleanIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
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
 * A filter at the top of an EXISTS pattern that refers to the current solution. Left inside the pattern, such a filter
 * makes the whole pattern refer to the current solution, which the database then evaluates for every current solution
 * anew; so the test evaluates the filter itself, next to the comparison of the solutions of the pattern with the
 * current solution, and the pattern below stays independent of the current solution.
 */
public class ExistsFilterConditionTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> ";

    /**
     * Name of the schema of the test tables.
     */
    private static final String schemaName = "exists_filter_condition_test";

    /**
     * The alias through which a pattern reads the current solution of a test at the top of the query.
     */
    private static final String currentSolution = "\"outer0\"";

    /**
     * Configuration mapping the test tables.
     */
    private static SparqlDatabaseConfiguration config = null;

    /**
     * The same configuration with the join optimizer enabled.
     */
    private static SparqlDatabaseConfiguration optimized = null;


    /**
     * Creates the {@code exists_filter_condition_test} schema, whose tables are mapped with the IRI classes {@code a},
     * {@code b} and {@code c} ({@code http://example.org/a/<id>} and so on): {@code a ex:val} with an integer,
     * {@code a ex:next a}, {@code a ex:name} with a string, and {@code ex:rel} from both {@code a} and {@code c} to
     * {@code b}, each from a table of its own.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            String tables = schemaName + ".";

            statement.execute("create schema " + schemaName);
            statement.execute("create table " + tables + "val (s int not null, o int not null)");
            statement.execute("insert into " + tables + "val values (1, 10), (1, 15), (2, 20), (3, 20), (4, 5)");
            statement.execute("create table " + tables + "aa (s int not null, o int not null)");
            statement.execute("insert into " + tables + "aa values (1, 2), (2, 3), (3, 1), (4, 4)");
            statement.execute("create table " + tables + "name (s int not null, o varchar not null)");
            statement.execute("insert into " + tables + "name values (1, 'one'), (2, 'two')");
            statement.execute("create table " + tables + "ab (s int not null, o int not null)");
            statement.execute("insert into " + tables + "ab values (1, 10), (2, 20), (3, 10)");
            statement.execute("create table " + tables + "cb (s int not null, o int not null)");
            statement.execute("insert into " + tables + "cb values (7, 10), (8, 30)");
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

        configuration.addQuadMapping(table("val"), null, configuration.createIriMapping("a", "s"),
                configuration.createIriMapping("ex:val"), configuration.createLiteralMapping(xsdInt, "o"));

        configuration.addQuadMapping(table("aa"), null, configuration.createIriMapping("a", "s"),
                configuration.createIriMapping("ex:next"), configuration.createIriMapping("a", "o"));

        configuration.addQuadMapping(table("name"), null, configuration.createIriMapping("a", "s"),
                configuration.createIriMapping("ex:name"), configuration.createLiteralMapping(xsdString, "o"));

        configuration.addQuadMapping(table("ab"), null, configuration.createIriMapping("a", "s"),
                configuration.createIriMapping("ex:rel"), configuration.createIriMapping("b", "o"));

        configuration.addQuadMapping(table("cb"), null, configuration.createIriMapping("c", "s"),
                configuration.createIriMapping("ex:rel"), configuration.createIriMapping("b", "o"));

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
        return new DatabaseTable(schemaName, name);
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
     * The EXISTS tests of the SQL of the query under the configuration without the join optimizer, in the order they
     * start in: each as its pattern and the rest of the test, which names the pattern and holds the condition of the
     * test.
     *
     * @param query the query, without the prefix declaration
     * @return the patterns and the conditions of the EXISTS tests of the query
     * @throws Exception on errors
     */
    private static List<String[]> tests(String query) throws Exception
    {
        Engine engine = new Engine(config);
        String sql;

        try(Request request = engine.getRequest())
        {
            PreparedQuery prepared = request.prepareQuery(prefix + query, null);
            SqlSelect imcode = new TranslateVisitor(request).translate(prepared.getSyntaxTree(), null, null, List.of());

            sql = imcode.optimize(request, false).optimize(request, true).translate(request);
        }

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


    /**
     * Literal of the datatype {@code xsd:int}.
     *
     * @param value the value
     * @return literal of the datatype {@code xsd:int}
     */
    private static TypedLiteral integer(int value)
    {
        return new TypedLiteral(Integer.toString(value), xsdIntIri);
    }


    /**
     * Literal of the datatype {@code xsd:boolean}.
     *
     * @param value the value
     * @return literal of the datatype {@code xsd:boolean}
     */
    private static TypedLiteral bool(boolean value)
    {
        return new TypedLiteral(Boolean.toString(value), xsdBooleanIri);
    }


    @Test
    @DisplayName("a filter comparing the pattern with the current solution is the only condition of the test")
    void filterAlone() throws Exception
    {
        String query = "SELECT ?s WHERE { ?s ex:val ?n FILTER NOT EXISTS { ?t ex:val ?k FILTER(?k > ?n) } }";

        assertThat(execute(query), containsInAnyOrder(row(resource("a/2")), row(resource("a/3"))));

        List<String[]> tests = tests(query);
        assertThat(tests, hasSize(1));
        assertThat(tests.get(0)[0], not(containsString(currentSolution)));
        assertThat(tests.get(0)[1], containsString(" WHERE (\"o\" > \"outer0\".\"o\")"));
    }


    @Test
    @DisplayName("the filter follows the comparison of the pattern with the current solution")
    void filterAfterComparison() throws Exception
    {
        String query = "SELECT ?s ?n WHERE { ?s ex:val ?n FILTER NOT EXISTS { ?s ex:val ?k FILTER(?k > ?n) } }";

        assertThat(execute(query),
                containsInAnyOrder(row(resource("a/1"), integer(15)), row(resource("a/2"), integer(20)),
                        row(resource("a/3"), integer(20)), row(resource("a/4"), integer(5))));

        // the filter reads the columns of its input, which keeps the alias the filter would give it
        List<String[]> tests = tests(query);
        assertThat(tests, hasSize(1));
        assertThat(tests.get(0)[0], not(containsString(currentSolution)));
        assertThat(tests.get(0)[1], containsString(
                ") AS \"outer1\" WHERE (((\"outer0\".\"s\" = \"outer1\".\"s\"))) AND (\"o\" > \"outer0\".\"o\")"));
    }


    @Test
    @DisplayName("two filters of the pattern are evaluated by the test together")
    void twoFilters() throws Exception
    {
        String query = "SELECT ?s ?n WHERE { ?s ex:val ?n "
                + "FILTER NOT EXISTS { ?s ex:val ?k FILTER(?k > ?n) FILTER(?k != 20) } }";

        assertThat(execute(query),
                containsInAnyOrder(row(resource("a/1"), integer(15)), row(resource("a/2"), integer(20)),
                        row(resource("a/3"), integer(20)), row(resource("a/4"), integer(5))));

        List<String[]> tests = tests(query);
        assertThat(tests, hasSize(1));
        assertThat(tests.get(0)[0], not(containsString(currentSolution)));
        assertThat(tests.get(0)[1], containsString("(\"o\" > \"outer0\".\"o\")"));
    }


    @Test
    @DisplayName("a filter above a join reads the columns the join exposes for it")
    void filterAboveJoin() throws Exception
    {
        String query = "SELECT ?s WHERE { ?s ex:next ?y "
                + "FILTER EXISTS { ?y ex:next ?z . ?z ex:val ?k FILTER(?z != ?s) } }";

        assertThat(execute(query),
                containsInAnyOrder(row(resource("a/1")), row(resource("a/2")), row(resource("a/3"))));

        List<String[]> tests = tests(query);
        assertThat(tests, hasSize(1));
        assertThat(tests.get(0)[0], not(containsString(currentSolution)));
        assertThat(tests.get(0)[1], containsString(" != \"outer0\".\"s\")"));
    }


    @Test
    @DisplayName("a filter above an OPTIONAL is evaluated by the test")
    void filterAboveOptional() throws Exception
    {
        String query = "SELECT ?s ?n WHERE { ?s ex:val ?n "
                + "FILTER EXISTS { ?s ex:next ?z OPTIONAL { ?z ex:val ?k } FILTER(!bound(?k) || ?k > ?n) } }";

        assertThat(execute(query),
                containsInAnyOrder(row(resource("a/1"), integer(10)), row(resource("a/1"), integer(15))));

        List<String[]> tests = tests(query);
        assertThat(tests, hasSize(1));
        assertThat(tests.get(0)[0], not(containsString(currentSolution)));
        assertThat(tests.get(0)[1], containsString(" > \"outer0\".\"o\")"));
    }


    @Test
    @DisplayName("a filter above a BIND reads the bound variable")
    void filterAboveBind() throws Exception
    {
        String query = "SELECT ?s ?n WHERE { ?s ex:val ?n "
                + "FILTER EXISTS { ?t ex:val ?k BIND(?k * 2 AS ?d) FILTER(?d = ?n) } }";

        assertThat(execute(query), containsInAnyOrder(row(resource("a/1"), integer(10)),
                row(resource("a/2"), integer(20)), row(resource("a/3"), integer(20))));

        List<String[]> tests = tests(query);
        assertThat(tests, hasSize(1));
        assertThat(tests.get(0)[0], not(containsString(currentSolution)));
        assertThat(tests.get(0)[1], containsString(currentSolution));
    }


    @Test
    @DisplayName("a filter of a group nested in the pattern is evaluated by the test")
    void filterOfNestedGroup() throws Exception
    {
        String query = "SELECT ?s ?n WHERE { ?s ex:val ?n FILTER EXISTS { { ?t ex:val ?k FILTER(?k > ?n) } } }";

        assertThat(execute(query), containsInAnyOrder(row(resource("a/1"), integer(10)),
                row(resource("a/1"), integer(15)), row(resource("a/4"), integer(5))));

        List<String[]> tests = tests(query);
        assertThat(tests, hasSize(1));
        assertThat(tests.get(0)[0], not(containsString(currentSolution)));
        assertThat(tests.get(0)[1], containsString(" WHERE (\"o\" > \"outer0\".\"o\")"));
    }


    @Test
    @DisplayName("a filter that does not refer to the current solution stays in the pattern")
    void filterOfPatternOnly() throws Exception
    {
        String query = "SELECT ?s WHERE { ?s ex:next ?y FILTER EXISTS { ?s ex:val ?k FILTER(?k > 12) } }";

        assertThat(execute(query),
                containsInAnyOrder(row(resource("a/1")), row(resource("a/2")), row(resource("a/3"))));

        List<String[]> tests = tests(query);
        assertThat(tests, hasSize(1));
        assertThat(tests.get(0)[0], containsString(" AS \"outer1\" WHERE "));
        assertThat(tests.get(0)[1], containsString(") AS \"tab\" WHERE (((\"outer0\".\"s\" = \"tab\".\"s\")))"));
    }


    @Test
    @DisplayName("a test nested in the filter reads the input of the filter and the current solution")
    void nestedTest() throws Exception
    {
        String query = "SELECT ?s WHERE { ?s ex:next ?y FILTER EXISTS { ?y ex:next ?z "
                + "FILTER(?z != ?s && NOT EXISTS { ?z ex:val ?k . ?s ex:val ?k }) } }";

        assertThat(execute(query), containsInAnyOrder(row(resource("a/1")), row(resource("a/2"))));

        // the nested test compares its pattern with the input of the filter under the alias the filter would give it
        List<String[]> tests = tests(query);
        assertThat(tests, hasSize(2));
        assertThat(tests.get(0)[0], not(containsString(currentSolution)));
        assertThat(tests.get(0)[1], containsString("(\"o\" != \"outer0\".\"s\") and NOT EXISTS ("));
        assertThat(tests.get(1)[1], containsString("\"outer1\".\"o\" = "));
        assertThat(tests.get(1)[1], containsString("\"outer0\".\"s\" = "));
    }


    @Test
    @DisplayName("a test in a BIND evaluates the filter of its pattern")
    void testInBind() throws Exception
    {
        String query = "SELECT ?s ?n ?b WHERE { ?s ex:val ?n BIND(EXISTS { ?t ex:val ?k FILTER(?k > ?n) } AS ?b) }";

        assertThat(execute(query),
                containsInAnyOrder(row(resource("a/1"), integer(10), bool(true)),
                        row(resource("a/1"), integer(15), bool(true)), row(resource("a/2"), integer(20), bool(false)),
                        row(resource("a/3"), integer(20), bool(false)), row(resource("a/4"), integer(5), bool(true))));

        List<String[]> tests = tests(query);
        assertThat(tests, hasSize(1));
        assertThat(tests.get(0)[0], not(containsString(currentSolution)));
        assertThat(tests.get(0)[1], containsString(" WHERE (\"o\" > \"outer0\".\"o\")"));
    }


    @Test
    @DisplayName("a test in the filter of an OPTIONAL evaluates the filter of its pattern")
    void testInOptional() throws Exception
    {
        String query = "SELECT ?s ?n ?y WHERE { ?s ex:val ?n "
                + "OPTIONAL { ?s ex:next ?y FILTER NOT EXISTS { ?y ex:val ?k FILTER(?k > ?n) } } }";

        assertThat(execute(query), containsInAnyOrder(row(resource("a/1"), integer(10), null),
                row(resource("a/1"), integer(15), null), row(resource("a/2"), integer(20), resource("a/3")),
                row(resource("a/3"), integer(20), resource("a/1")), row(resource("a/4"), integer(5), resource("a/4"))));

        // the current solution of the test is read from both sides of the left join
        List<String[]> tests = tests(query);
        assertThat(tests, hasSize(1));
        assertThat(tests.get(0)[0], not(containsString("\"left0\"")));
        assertThat(tests.get(0)[1], containsString(
                ") AS \"outer1\" WHERE (((\"right0\".\"o\" = \"outer1\".\"s\"))) AND (\"o\" > \"left0\".\"o\")"));
    }


    @Test
    @DisplayName("a filter distributed over the branches of a union keeps its meaning")
    void filterAboveUnion() throws Exception
    {
        String query = "SELECT ?b ?n WHERE { ?t ex:rel ?b . ?t ex:val ?n "
                + "FILTER NOT EXISTS { ?x ex:rel ?b FILTER(?x != ?t) } }";

        assertThat(execute(query), containsInAnyOrder(row(resource("b/20"), integer(20))));
    }


    @Test
    @DisplayName("a filter that fails for every solution of the pattern leaves the test without a solution")
    void failingFilter() throws Exception
    {
        String query = "SELECT ?s WHERE { ?s ex:val ?n FILTER NOT EXISTS { ?t ex:name ?k FILTER(?k > ?n) } }";

        assertThat(execute(query), containsInAnyOrder(row(resource("a/1")), row(resource("a/1")), row(resource("a/2")),
                row(resource("a/3")), row(resource("a/4"))));
    }
}
