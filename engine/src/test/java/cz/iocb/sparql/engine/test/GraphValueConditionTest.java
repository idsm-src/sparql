package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerIri;
import static java.util.stream.Collectors.joining;
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
 * Table accesses of a pattern evaluated per graph restricted to the graph of the current solution. A pattern of
 * {@code GRAPH ?g} whose solutions need not come from a triple of the graph, and a sub-select inside such a clause, are
 * evaluated for every named graph anew; wherever such a pattern has to stay in that graph (as a whole, in the WHERE
 * clause of a sub-select, on the right side of an OPTIONAL or a MINUS, in an EXISTS pattern), the table accesses take
 * the graph into their conditions, compared in the class of their graph column, so that the database can look the
 * triples of the graph up instead of reading the triples of all graphs for each of them.
 */
public class GraphValueConditionTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> ";

    /**
     * Name of the schema of the test tables.
     */
    private static final String schemaName = "graph_value_condition_test";

    /**
     * SQL of the condition restricting a table whose graph is a column to the graph of the current solution.
     */
    private static final String graphColumn = "\"g\" = \"lateral0\".\"g#g_par0\"";

    /**
     * Configuration mapping the test tables.
     */
    private static SparqlDatabaseConfiguration config = null;

    /**
     * The same configuration with the join optimizer enabled.
     */
    private static SparqlDatabaseConfiguration optimized = null;


    /**
     * Creates the {@code graph_value_condition_test} schema, whose tables are mapped with the IRI classes {@code a} and
     * {@code g} ({@code http://example.org/a/<id>} and so on): {@code a ex:link a} and {@code a ex:mark} with an
     * integer in the graphs {@code g} named by a column, and {@code a ex:link a} of another table in the graph
     * {@code ex:fixed}. The graph {@code g/3} has no {@code ex:link} triple.
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
            statement.execute("create table " + tables + "link (g int not null, s int not null, o int not null)");
            statement.execute(
                    "insert into " + tables + "link values (1, 1, 2), (1, 2, 3), (2, 3, 4), (2, 4, 5), (2, 2, 5)");
            statement.execute("create table " + tables + "mark (g int not null, s int not null, o int not null)");
            statement.execute(
                    "insert into " + tables + "mark values (1, 1, 10), (2, 1, 0), (2, 3, 0), (1, 3, 0), (3, 7, 0)");
            statement.execute("create table " + tables + "fixed (s int not null, o int not null)");
            statement.execute("insert into " + tables + "fixed values (1, 2), (8, 9)");
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
        configuration.addIriClass(new IntegerUserIriClass("g", INT4, "http://example.org/g/"));

        configuration.addQuadMapping(table("link"), configuration.createIriMapping("g", "g"),
                configuration.createIriMapping("a", "s"), configuration.createIriMapping("ex:link"),
                configuration.createIriMapping("a", "o"));

        configuration.addQuadMapping(table("mark"), configuration.createIriMapping("g", "g"),
                configuration.createIriMapping("a", "s"), configuration.createIriMapping("ex:mark"),
                configuration.createLiteralMapping(xsdInt, "o"));

        configuration.addQuadMapping(table("fixed"), configuration.createIriMapping("ex:fixed"),
                configuration.createIriMapping("a", "s"), configuration.createIriMapping("ex:link"),
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
     * Literal of the datatype {@code xsd:integer}.
     *
     * @param value the value
     * @return literal of the datatype {@code xsd:integer}
     */
    private static TypedLiteral integer(int value)
    {
        return new TypedLiteral(Integer.toString(value), xsdIntegerIri);
    }


    @Test
    @DisplayName("a sub-select evaluated per graph looks up the triples of the graph")
    void subSelect() throws Exception
    {
        String query = "SELECT ?g ?c WHERE { GRAPH ?g { SELECT (COUNT(*) AS ?c) WHERE { ?s ex:link ?e } } }";

        assertThat(execute(query),
                containsInAnyOrder(row(resource("g/1"), integer(2)), row(resource("g/2"), integer(3)),
                        row(resource("g/3"), integer(0)), row(resource("fixed"), integer(2))));

        // the graph is compared in the class of the graph column
        assertThat(translate(query), containsString(access("link", graphColumn)));
    }


    @Test
    @DisplayName("the right side of an OPTIONAL evaluated per graph looks up the triples of the graph")
    void optional() throws Exception
    {
        String query = "SELECT ?g ?s ?e WHERE { GRAPH ?g { OPTIONAL { ?s ex:link ?e } } }";

        assertThat(execute(query),
                containsInAnyOrder(row(resource("g/1"), resource("a/1"), resource("a/2")),
                        row(resource("g/1"), resource("a/2"), resource("a/3")),
                        row(resource("g/2"), resource("a/3"), resource("a/4")),
                        row(resource("g/2"), resource("a/4"), resource("a/5")),
                        row(resource("g/2"), resource("a/2"), resource("a/5")), row(resource("g/3"), null, null),
                        row(resource("fixed"), resource("a/1"), resource("a/2")),
                        row(resource("fixed"), resource("a/8"), resource("a/9"))));
        assertThat(translate(query), containsString(access("link", graphColumn)));
    }


    @Test
    @DisplayName("both sides of a MINUS evaluated per graph look up the triples of the graph")
    void minus() throws Exception
    {
        String query = "SELECT ?g ?s WHERE { GRAPH ?g { OPTIONAL { ?s ex:mark ?m } MINUS { ?s ex:link ?e } } }";

        assertThat(execute(query),
                containsInAnyOrder(row(resource("g/1"), resource("a/3")), row(resource("g/2"), resource("a/1")),
                        row(resource("g/3"), resource("a/7")), row(resource("fixed"), null)));

        String sql = translate(query);
        assertThat(sql, containsString(access("mark", graphColumn)));
        assertThat(sql, containsString(access("link", graphColumn)));
    }


    @Test
    @DisplayName("an EXISTS pattern evaluated per graph looks up the triples of the graph")
    void existsPattern() throws Exception
    {
        String query = "SELECT ?g ?x WHERE { GRAPH ?g { BIND(1 AS ?x) FILTER EXISTS { ?s ex:link ?e } } }";

        assertThat(execute(query), containsInAnyOrder(row(resource("g/1"), integer(1)),
                row(resource("g/2"), integer(1)), row(resource("fixed"), integer(1))));
        assertThat(translate(query), containsString("EXISTS (SELECT 1 " + access("link", graphColumn)));
    }


    @Test
    @DisplayName("an EXISTS pattern in a sub-select evaluated per graph looks up the triples of the graph")
    void existsPatternInSubSelect() throws Exception
    {
        String query = "SELECT ?g ?n WHERE { GRAPH ?g { SELECT (COUNT(*) AS ?n) WHERE { ?s ex:mark ?m "
                + "FILTER NOT EXISTS { ?s ex:link ?e } } } }";

        assertThat(execute(query),
                containsInAnyOrder(row(resource("g/1"), integer(1)), row(resource("g/2"), integer(1)),
                        row(resource("g/3"), integer(1)), row(resource("fixed"), integer(0))));

        String sql = translate(query);
        assertThat(sql, containsString(access("mark", graphColumn)));
        assertThat(sql, containsString(access("link", graphColumn)));
    }


    @Test
    @DisplayName("a sub-select joined with a triple of the graph looks up the triples of the graph")
    void joinedSubSelect() throws Exception
    {
        String query = "SELECT ?g ?s ?c WHERE { GRAPH ?g { ?s ex:mark ?m "
                + "{ SELECT ?s (COUNT(*) AS ?c) WHERE { ?s ex:link ?e } GROUP BY ?s } } }";

        assertThat(execute(query), containsInAnyOrder(row(resource("g/1"), resource("a/1"), integer(1)),
                row(resource("g/2"), resource("a/3"), integer(1))));
        assertThat(translate(query), containsString(access("link", graphColumn)));
    }


    @Test
    @DisplayName("the initial solutions and the steps of a walk evaluated per graph look up the triples of the graph")
    void walk() throws Exception
    {
        String query = "SELECT ?g ?s ?e WHERE { GRAPH ?g { OPTIONAL { ?s ex:link+ ?e "
                + "FILTER(?s = <http://example.org/a/1> || ?s = <http://example.org/a/3>) } } }";

        assertThat(execute(query),
                containsInAnyOrder(row(resource("g/1"), resource("a/1"), resource("a/2")),
                        row(resource("g/1"), resource("a/1"), resource("a/3")),
                        row(resource("g/2"), resource("a/3"), resource("a/4")),
                        row(resource("g/2"), resource("a/3"), resource("a/5")), row(resource("g/3"), null, null),
                        row(resource("fixed"), resource("a/1"), resource("a/2"))));

        String sql = translate(query);
        int start = sql.indexOf("WITH RECURSIVE");
        int step = sql.indexOf(" UNION SELECT ", start);
        assertThat(sql.substring(start, step), containsString(access("link", graphColumn)));
        assertThat(sql.substring(step), containsString(access("link", graphColumn)));
    }
}
