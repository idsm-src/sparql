package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.iri;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static java.util.stream.Collectors.joining;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
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
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.LimitExceedException;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Request.PreparedQuery;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.translator.ServiceException;
import cz.iocb.sparql.engine.translator.TranslateVisitor;
import cz.iocb.sparql.testing.Database;



/**
 * Table accesses of an EXISTS pattern restricted to the values of the current solution. Where a part of the pattern is
 * evaluated for the current solution by itself (the right side of an OPTIONAL or a MINUS, the WHERE clause of a
 * sub-select, the initial solutions of a walk starting from a node of the current solution), the table accesses binding
 * a variable of the current solution take its value into their conditions, converted to the class of their columns, so
 * that the database can look the rows up by an index of the table; a value the current solution may leave unbound and a
 * variable the test itself compares stay out of the accesses. A pattern that reads the current solution anyway (in a
 * filter the test cannot evaluate itself, in a BIND) takes the values of all the variables it binds so, because the
 * database evaluates it for every current solution anew whatever the accesses do; a constant of the current solution is
 * taken by any pattern.
 */
public class ExistsValueConditionTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> ";

    /**
     * Name of the schema of the test tables.
     */
    private static final String schemaName = "exists_value_condition_test";

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
     * Creates the {@code exists_value_condition_test} schema, whose tables are mapped with the IRI classes {@code a},
     * {@code b}, {@code c} and {@code g} ({@code http://example.org/a/<id>} and so on): in the default graph
     * {@code a ex:p1 b}, {@code a ex:p2 a}, {@code b ex:p3 c}, {@code b ex:p6} with IRIs of any class, {@code a ex:p7}
     * with terms of any kind, {@code a ex:sub a}, {@code ex:p4} from both {@code a} and {@code c} to {@code b}, each
     * from a table of its own, and {@code a ex:name} with a string and {@code a ex:rank} with an integer from one table
     * keyed by the subject; in the graphs {@code g} named by a column {@code a ex:link a} and {@code a ex:mark} with an
     * integer. The default graph includes the named graphs.
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
            statement.execute("create table " + tables + "ab (s int not null, o int not null)");
            statement.execute("insert into " + tables + "ab values (1, 10), (2, 20), (3, 30), (4, 10)");
            statement.execute("create table " + tables + "aa (s int not null, o int not null)");
            statement.execute("insert into " + tables + "aa values (1, 2), (1, 3), (2, 3), (2, 7), (3, 1), (5, 5)");
            statement.execute("create table " + tables + "bc (s int not null, o int not null)");
            statement.execute("insert into " + tables + "bc values (10, 100), (20, 200)");
            statement.execute("create table " + tables + "biri (s int not null, o varchar not null)");
            statement.execute("insert into " + tables + "biri values (10, 'http://example.org/a/1'), "
                    + "(20, 'http://example.org/a/9'), (30, 'http://example.org/b/10'), "
                    + "(40, 'http://example.org/other/1')");
            statement.execute("create table " + tables + "abox (s int not null, o sparql.rdfbox not null)");
            statement.execute("insert into " + tables + "abox values (1, '<http://example.org/a/2>'), (2, '\"x\"'), "
                    + "(3, '<http://example.org/b/20>'), (4, '\"1\"^^<http://www.w3.org/2001/XMLSchema#int>')");
            statement.execute("create table " + tables + "sub (s int not null, o int not null)");
            statement.execute("insert into " + tables + "sub values (2, 1), (3, 2), (4, 9), (5, 4)");
            statement.execute("create table " + tables + "info (id int primary key, name varchar not null, rank int)");
            statement.execute(
                    "insert into " + tables + "info values (1, 'one', 10), (2, 'two', null), " + "(3, 'three', 30)");
            statement.execute("create table " + tables + "link (g int not null, s int not null, o int not null)");
            statement.execute(
                    "insert into " + tables + "link values (1, 1, 2), (1, 2, 3), (2, 3, 4), (2, 4, 5), " + "(2, 2, 5)");
            statement.execute("create table " + tables + "mark (g int not null, s int not null, o int not null)");
            statement.execute("insert into " + tables + "mark values (1, 1, 10), (2, 1, 0), (2, 3, 0), (1, 3, 0)");
            statement.execute("create table " + tables + "ad (s int not null, o int not null)");
            statement.execute("insert into " + tables + "ad values (1, 10), (1, 20), (9, 20)");
            statement.execute("create table " + tables + "cd (s int not null, o int not null)");
            statement.execute("insert into " + tables + "cd values (100, 10)");
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
        configuration.addIriClass(new IntegerUserIriClass("g", INT4, "http://example.org/g/"));

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

        configuration.addQuadMapping(table("sub"), null, configuration.createIriMapping("a", "s"),
                configuration.createIriMapping("ex:sub"), configuration.createIriMapping("a", "o"));

        configuration.addQuadMapping(table("info"), null, configuration.createIriMapping("a", "id"),
                configuration.createIriMapping("ex:name"), configuration.createLiteralMapping(xsdString, "name"));

        configuration.addQuadMapping(table("info"), null, configuration.createIriMapping("a", "id"),
                configuration.createIriMapping("ex:rank"), configuration.createLiteralMapping(xsdInt, "rank"));

        configuration.addQuadMapping(table("link"), configuration.createIriMapping("g", "g"),
                configuration.createIriMapping("a", "s"), configuration.createIriMapping("ex:link"),
                configuration.createIriMapping("a", "o"));

        configuration.addQuadMapping(table("mark"), configuration.createIriMapping("g", "g"),
                configuration.createIriMapping("a", "s"), configuration.createIriMapping("ex:mark"),
                configuration.createLiteralMapping(xsdInt, "o"));

        configuration.addQuadMapping(table("ad"), null, configuration.createIriMapping("a", "s"),
                configuration.createIriMapping("ex:p4"), configuration.createIriMapping("b", "o"));

        configuration.addQuadMapping(table("cd"), null, configuration.createIriMapping("c", "s"),
                configuration.createIriMapping("ex:p4"), configuration.createIriMapping("b", "o"));

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
     * SQL of the pattern of the first EXISTS test of the query, without the condition comparing the solutions of the
     * pattern with the current solution.
     *
     * @param query the query, without the prefix declaration
     * @return SQL of the pattern of the first EXISTS test
     * @throws Exception on errors
     */
    private static String pattern(String query) throws Exception
    {
        String sql = translate(query);
        String head = "EXISTS (SELECT 1 FROM (";
        int begin = sql.indexOf(head) + head.length();
        int end = begin;

        for(int depth = 1; depth > 0; end++)
            depth += sql.charAt(end) == '(' ? 1 : sql.charAt(end) == ')' ? -1 : 0;

        return sql.substring(begin, end - 1);
    }


    /**
     * SQL of an access to the test table restricted by the conditions.
     *
     * @param name the name of the table
     * @param conditions SQL of the conditions
     * @return SQL of the access from its FROM clause on
     */
    private static String access(String name, String... conditions)
    {
        return "FROM \"" + schemaName + "\".\"" + name + "\" WHERE "
                + Arrays.stream(conditions).map(c -> "((" + c + "))").collect(joining(" AND "));
    }


    /**
     * Number of the references to the current solution in the SQL.
     *
     * @param sql the SQL
     * @return number of the references to the current solution
     */
    private static int references(String sql)
    {
        int count = 0;

        for(int index = sql.indexOf(currentSolution); index >= 0; index = sql.indexOf(currentSolution, index + 1))
            count++;

        return count;
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
    @DisplayName("the right side of an OPTIONAL looks up the rows of the current solution")
    void optionalRightSide() throws Exception
    {
        String query = "SELECT ?s WHERE { ?s ex:p1 ?b "
                + "FILTER EXISTS { ?x ex:p3 ?c OPTIONAL { ?s ex:p2 ?z } FILTER(bound(?z)) } }";

        assertThat(execute(query),
                containsInAnyOrder(row(resource("a/1")), row(resource("a/2")), row(resource("a/3"))));
        assertThat(pattern(query), containsString(access("aa", "\"s\" = \"outer0\".\"s\"")));
    }


    @Test
    @DisplayName("an IRI of any class is converted to the class of the column")
    void iriOfAnyClass() throws Exception
    {
        String query = "SELECT ?q WHERE { ?b ex:p6 ?q "
                + "FILTER EXISTS { ?x ex:p3 ?c OPTIONAL { ?q ex:p2 ?z } FILTER(bound(?z)) } }";

        // an IRI of another class, or of no row, is in no row of the table
        assertThat(execute(query), containsInAnyOrder(row(resource("a/1"))));

        String pattern = pattern(query);
        String table = "FROM \"" + schemaName + "\".\"aa\" WHERE ";
        assertThat(pattern, containsString(table + "((\"s\" = CASE WHEN sparql.regex_string(\"outer0\".\"o\", "));
        assertThat(pattern, containsString(" THEN right(\"outer0\".\"o\", -21)::int4 END)) "));
    }


    @Test
    @DisplayName("a term of any kind is converted to the class of the column")
    void termOfAnyKind() throws Exception
    {
        String query = "SELECT ?q WHERE { ?a ex:p7 ?q "
                + "FILTER EXISTS { SELECT (COUNT(*) AS ?n) WHERE { ?q ex:p3 ?z } HAVING(COUNT(*) > 0) } }";

        assertThat(execute(query), containsInAnyOrder(row(resource("b/20"))));

        String pattern = pattern(query);
        String table = "FROM \"" + schemaName + "\".\"bc\" WHERE ";
        assertThat(pattern, containsString(
                table + "((\"s\" = CASE WHEN sparql.regex_string(sparql.rdfbox_get_iri(\"outer0\".\"o\"), "));
        assertThat(pattern, containsString(" THEN right(sparql.rdfbox_get_iri(\"outer0\".\"o\"), -21)::int4 END)) "));
    }


    @Test
    @DisplayName("a literal is compared with the column of its datatype")
    void literal() throws Exception
    {
        String query = "SELECT ?s WHERE { ?s ex:rank ?r "
                + "FILTER EXISTS { SELECT (COUNT(*) AS ?n) WHERE { ?x ex:mark ?r } HAVING(COUNT(*) > 0) } }";

        assertThat(execute(query), containsInAnyOrder(row(resource("a/1"))));
        assertThat(pattern(query), containsString(access("mark", "\"o\" = \"outer0\".\"rank\"")));
    }


    @Test
    @DisplayName("a value of several classes is compared in the class of the column")
    void valueOfSeveralClasses() throws Exception
    {
        String query = "SELECT ?o WHERE { { SELECT ?o WHERE { { ?s ex:p1 ?o } UNION { ?s ex:p2 ?o } } LIMIT 100 } "
                + "FILTER EXISTS { ?x ex:p3 ?c OPTIONAL { ?o ex:p2 ?z } FILTER(bound(?z)) } }";

        assertThat(execute(query), containsInAnyOrder(row(resource("a/2")), row(resource("a/3")), row(resource("a/3")),
                row(resource("a/1")), row(resource("a/5"))));
        assertThat(pattern(query), containsString(access("aa", "\"s\" = \"outer0\".\"o#a_par0\"")));
    }


    @Test
    @DisplayName("a value of a class the column cannot hold leaves no solution")
    void valueOfDisjointClass() throws Exception
    {
        String query = "SELECT ?s WHERE { ?s ex:p1 ?b "
                + "FILTER NOT EXISTS { ?x ex:p3 ?c OPTIONAL { ?b ex:p2 ?z } FILTER(bound(?z)) } }";

        assertThat(execute(query), containsInAnyOrder(row(resource("a/1")), row(resource("a/2")), row(resource("a/3")),
                row(resource("a/4"))));
        // the pattern has no solution for any current solution, so the test is gone
        assertThat(translate(query), not(containsString("EXISTS")));
    }


    @Test
    @DisplayName("a value the current solution may leave unbound is compared outside the access")
    void valueLeftUnbound() throws Exception
    {
        String query = "SELECT ?s ?t WHERE { ?s ex:p1 ?b OPTIONAL { ?s ex:sub ?t } "
                + "FILTER EXISTS { ?x ex:p3 ?c OPTIONAL { ?t ex:p2 ?z } FILTER(bound(?z)) } }";

        // any row is compatible with a current solution leaving the variable unbound
        assertThat(execute(query), containsInAnyOrder(row(resource("a/1"), null), row(resource("a/2"), resource("a/1")),
                row(resource("a/3"), resource("a/2"))));
        assertThat(pattern(query), not(containsString("FROM \"" + schemaName + "\".\"aa\" WHERE ")));
    }


    @Test
    @DisplayName("a sub-select with an aggregate counts the rows of the current solution")
    void aggregate() throws Exception
    {
        String query = "SELECT ?s WHERE { ?s ex:p1 ?b "
                + "FILTER EXISTS { SELECT (COUNT(*) AS ?n) WHERE { ?s ex:p2 ?z } HAVING(COUNT(*) > 1) } }";

        assertThat(execute(query), containsInAnyOrder(row(resource("a/1")), row(resource("a/2"))));

        // the condition of the access is the only place reading the current solution
        String pattern = pattern(query);
        assertThat(pattern, containsString(access("aa", "\"s\" = \"outer0\".\"s\"")));
        assertThat(references(pattern), equalTo(1));
    }


    @Test
    @DisplayName("a sub-select with a limit takes the rows of the current solution")
    void limit() throws Exception
    {
        String query = "SELECT ?s WHERE { ?s ex:p1 ?b "
                + "FILTER EXISTS { SELECT ?z WHERE { ?s ex:p2 ?z . ?z ex:p1 ?w } LIMIT 1 } }";

        assertThat(execute(query),
                containsInAnyOrder(row(resource("a/1")), row(resource("a/2")), row(resource("a/3"))));

        String pattern = pattern(query);
        assertThat(pattern, containsString(access("aa", "\"s\" = \"outer0\".\"s\"")));
        assertThat(references(pattern), equalTo(1));
    }


    @Test
    @DisplayName("the right side of a MINUS looks up the rows of the current solution")
    void minusRightSide() throws Exception
    {
        String query = "SELECT ?s WHERE { ?s ex:p1 ?b FILTER EXISTS { ?x ex:p3 ?c MINUS { ?s ex:p2 ?z } } }";

        assertThat(execute(query), containsInAnyOrder(row(resource("a/4"))));

        String pattern = pattern(query);
        assertThat(pattern, containsString(access("aa", "\"s\" = \"outer0\".\"s\"")));
        assertThat(references(pattern), equalTo(1));
    }


    @Test
    @DisplayName("every branch of a union is restricted in its own column")
    void unionBranches() throws Exception
    {
        String query = "SELECT ?s WHERE { ?s ex:p1 ?b FILTER NOT EXISTS { ?x ex:p3 ?c "
                + "OPTIONAL { { ?s ex:p2 ?z } UNION { ?z ex:p2 ?s } } FILTER(bound(?z)) } }";

        assertThat(execute(query), containsInAnyOrder(row(resource("a/4"))));

        String pattern = pattern(query);
        assertThat(pattern, containsString(access("aa", "\"s\" = \"outer0\".\"s\"")));
        assertThat(pattern, containsString(access("aa", "\"o\" = \"outer0\".\"s\"")));
    }


    @Test
    @DisplayName("an access binding two variables of the current solution is restricted by both")
    void twoValues() throws Exception
    {
        String query = "SELECT ?g ?s WHERE { GRAPH ?g { ?s ex:mark ?m } "
                + "FILTER EXISTS { ?x ex:p3 ?c OPTIONAL { GRAPH ?g { ?s ex:link ?e } } FILTER(bound(?e)) } }";

        assertThat(execute(query),
                containsInAnyOrder(row(resource("g/1"), resource("a/1")), row(resource("g/2"), resource("a/3"))));
        assertThat(pattern(query),
                containsString(access("link", "\"g\" = \"outer0\".\"g\"", "\"s\" = \"outer0\".\"s\"")));
    }


    @Test
    @DisplayName("accesses joined on a key are merged with their restriction")
    void joinOnKey() throws Exception
    {
        String query = "SELECT ?s WHERE { ?s ex:p1 ?b FILTER EXISTS { ?x ex:p3 ?c "
                + "OPTIONAL { ?s ex:name ?n ; ex:rank ?r } FILTER(bound(?n)) } }";

        assertThat(execute(query), containsInAnyOrder(row(resource("a/1")), row(resource("a/3"))));

        String pattern = pattern(query);
        assertThat(pattern, containsString(access("info", "\"rank\" IS NOT NULL", "\"id\" = \"outer0\".\"s\"")));
        assertThat(pattern.split("\"info\"", -1).length, equalTo(2));
    }


    @Test
    @DisplayName("accesses left-joined on a key are merged with their restriction")
    void leftJoinOnKey() throws Exception
    {
        String query = "SELECT ?s WHERE { ?s ex:p1 ?b FILTER EXISTS { ?x ex:p3 ?c "
                + "OPTIONAL { ?s ex:name ?n OPTIONAL { ?s ex:rank ?r } } FILTER(bound(?n)) } }";

        assertThat(execute(query),
                containsInAnyOrder(row(resource("a/1")), row(resource("a/2")), row(resource("a/3"))));

        String pattern = pattern(query);
        assertThat(pattern, containsString(access("info", "\"id\" = \"outer0\".\"s\"")));
        assertThat(pattern.split("\"info\"", -1).length, equalTo(2));
    }


    @Test
    @DisplayName("a pattern nested in another one reads the current solution of its own test")
    void nestedPattern() throws Exception
    {
        String query = "SELECT ?s WHERE { ?s ex:p1 ?b FILTER EXISTS { ?s ex:p2 ?t "
                + "FILTER NOT EXISTS { ?x ex:p3 ?c OPTIONAL { ?t ex:p2 ?z } FILTER(bound(?z)) } } }";

        assertThat(execute(query), containsInAnyOrder(row(resource("a/2"))));
        assertThat(pattern(query), containsString(access("aa", "\"s\" = \"outer1\".\"o\"")));
    }


    @Test
    @DisplayName("a variable the test compares by itself is not restricted in the pattern")
    void comparedByTest() throws Exception
    {
        String query = "SELECT ?s WHERE { ?s ex:p1 ?b FILTER EXISTS { ?s ex:p2 ?z } }";

        assertThat(execute(query),
                containsInAnyOrder(row(resource("a/1")), row(resource("a/2")), row(resource("a/3"))));
        // the pattern does not refer to the current solution, so the database may evaluate the test as a semi-join
        assertThat(references(pattern(query)), equalTo(0));
    }


    @Test
    @DisplayName("a filter in the branches of a union makes the branches look up the rows of the current solution")
    void filterInUnionBranches() throws Exception
    {
        String query = "SELECT ?q WHERE { ?b ex:p6 ?q FILTER NOT EXISTS { ?q ex:p4 ?z FILTER(?z != ?b) } }";

        assertThat(execute(query),
                containsInAnyOrder(row(resource("a/9")), row(resource("b/10")), row(resource("other/1"))));

        // the test cannot evaluate a filter distributed over a union, so the pattern reads the current solution anyway
        String pattern = pattern(query);
        String condition = " WHERE ((\"s\" = CASE WHEN sparql.regex_string(\"outer0\".\"o\", ";
        assertThat(pattern, containsString("FROM \"" + schemaName + "\".\"ad\"" + condition));
        assertThat(pattern, containsString("FROM \"" + schemaName + "\".\"cd\"" + condition));
    }


    @Test
    @DisplayName("a filter below a join makes the join look up the rows of the current solution")
    void filterBelowJoin() throws Exception
    {
        String query = "SELECT ?s ?y WHERE { ?s ex:p2 ?y "
                + "FILTER EXISTS { { ?y ex:p2 ?z FILTER(?z != ?s) } ?z ex:p1 ?w } }";

        assertThat(execute(query), containsInAnyOrder(row(resource("a/1"), resource("a/2")),
                row(resource("a/2"), resource("a/3")), row(resource("a/3"), resource("a/1"))));
        assertThat(pattern(query), containsString(access("aa", "\"s\" = \"outer0\".\"o\"")));
    }


    @Test
    @DisplayName("a BIND reading the current solution makes the pattern look up the rows of the current solution")
    void bindOfCurrentSolution() throws Exception
    {
        String query = "SELECT ?s ?b WHERE { ?s ex:p1 ?b "
                + "FILTER EXISTS { ?s ex:p2 ?z . ?z ex:p2 ?u BIND(str(?b) AS ?l) FILTER(strlen(?l) > 3) } }";

        assertThat(execute(query), containsInAnyOrder(row(resource("a/1"), resource("b/10")),
                row(resource("a/2"), resource("b/20")), row(resource("a/3"), resource("b/30"))));
        assertThat(pattern(query), containsString(access("aa", "\"s\" = \"outer0\".\"s\"")));
    }


    @Test
    @DisplayName("a constant of the current solution is taken by a pattern that does not read the current solution")
    void constantOfCurrentSolution() throws Exception
    {
        String query = "SELECT ?s WHERE { BIND(<http://example.org/a/1> AS ?s) FILTER EXISTS { ?s ex:p2 ?z } }";

        assertThat(execute(query), containsInAnyOrder(row(resource("a/1"))));

        String pattern = pattern(query);
        assertThat(pattern, containsString(access("aa", "\"s\" = '1'::int4")));
        assertThat(references(pattern), equalTo(0));
    }


    @Test
    @DisplayName("a nested pattern takes the values of the test whose input it reads, not of the test around it")
    void nestedPatternReadingItsTest() throws Exception
    {
        String query = "SELECT ?s WHERE { ?s ex:p1 ?b FILTER EXISTS { ?s ex:p2 ?t "
                + "FILTER NOT EXISTS { { ?t ex:p2 ?u FILTER(?u != ?s) } ?u ex:p2 ?v } } }";

        assertThat(execute(query), containsInAnyOrder(row(resource("a/1")), row(resource("a/2"))));

        // the pattern around the nested one reads nothing of its own test, which the database evaluates as a semi-join
        String pattern = pattern(query);
        assertThat(references(pattern), equalTo(0));
        assertThat(pattern, containsString(access("aa", "\"s\" = \"outer1\".\"o\"")));
    }


    @Test
    @DisplayName("a pattern of a test in the filter of an OPTIONAL takes the values of both sides")
    void testInOptional() throws Exception
    {
        String query = "SELECT ?s ?y WHERE { ?s ex:p1 ?b OPTIONAL { ?s ex:p2 ?y "
                + "FILTER EXISTS { { ?y ex:p2 ?z FILTER(?z != ?s) } ?b ex:p3 ?c } } }";

        assertThat(execute(query), containsInAnyOrder(row(resource("a/1"), resource("a/2")),
                row(resource("a/2"), resource("a/3")), row(resource("a/3"), null), row(resource("a/4"), null)));

        String pattern = pattern(query);
        assertThat(pattern, containsString(access("aa", "\"s\" = \"right0\".\"o\"")));
        assertThat(pattern, containsString(access("bc", "\"s\" = \"left0\".\"o\"")));
    }


    @Test
    @DisplayName("the initial solutions of a walk look up the node the walk starts from")
    void walkStart() throws Exception
    {
        String query = "SELECT ?s WHERE { ?s ex:p1 ?b FILTER EXISTS { ?s ex:sub+ ?e . ?e ex:p2 ?w } }";

        assertThat(execute(query), containsInAnyOrder(row(resource("a/2")), row(resource("a/3"))));

        // the non-recursive term has nothing left to compare with the node
        String pattern = pattern(query);
        String start = pattern.substring(pattern.indexOf("WITH RECURSIVE"), pattern.indexOf(" UNION SELECT "));
        assertThat(start, endsWith(access("sub", "\"s\" = \"outer0\".\"s\"") + ") AS \"tab\""));
    }


    @Test
    @DisplayName("the nodes a walk of zero steps starts from are looked up in the class of each column")
    void zeroLengthWalkStart() throws Exception
    {
        String query = "SELECT ?q WHERE { ?b ex:p6 ?q FILTER EXISTS { ?q ex:sub* ?e . ?e ex:p2 ?w } }";

        assertThat(execute(query), containsInAnyOrder(row(resource("a/1"))));

        String pattern = pattern(query);
        String table = "FROM \"" + schemaName + "\".\"sub\" WHERE ";
        assertThat(pattern, containsString(table + "((\"s\" = CASE WHEN sparql.regex_string(\"outer0\".\"o\", "));
        assertThat(pattern, containsString(access("biri", "\"o\" = \"outer0\".\"o\"")));
        assertThat(pattern, containsString(
                access("abox", "\"o\" operator(sparql.@=) sparql.rdfbox_create_from_iri(\"outer0\".\"o\")")));
    }


    @Test
    @DisplayName("the steps of a walk starting in a graph of the current solution stay in that graph")
    void walkInGraph() throws Exception
    {
        String query = "SELECT ?g ?s WHERE { GRAPH ?g { ?s ex:mark ?m } "
                + "FILTER EXISTS { GRAPH ?g { ?s ex:link+ ?e } ?e ex:p2 ?w } }";

        assertThat(execute(query),
                containsInAnyOrder(row(resource("g/1"), resource("a/1")), row(resource("g/2"), resource("a/3"))));

        // the non-recursive term looks up the node in the graph, the recursive one the steps in the graph
        String pattern = pattern(query);
        String start = access("link", "\"g\" = \"outer0\".\"g\"", "\"s\" = \"outer0\".\"s\"");
        assertThat(pattern, containsString(start + ") AS \"tab\""));
        assertThat(pattern, containsString(access("link", "\"g\" = \"outer0\".\"g\"") + ") AS \"tab1\""));
    }


    @Test
    @DisplayName("a walk on the right side of an OPTIONAL starts from the node and is still compared with it")
    void walkOnRightSide() throws Exception
    {
        String query = "SELECT ?s WHERE { ?s ex:p1 ?b "
                + "FILTER NOT EXISTS { ?x ex:p3 ?c OPTIONAL { ?s ex:sub* ?e . ?e ex:p2 ?w } FILTER(bound(?e)) } }";

        assertThat(execute(query), containsInAnyOrder(row(resource("a/4"))));
        assertThat(pattern(query), containsString(access("sub", "\"s\" = \"outer0\".\"s\"")));
    }
}
