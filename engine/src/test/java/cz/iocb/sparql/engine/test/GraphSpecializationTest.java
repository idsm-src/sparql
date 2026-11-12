package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
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
 * A pattern evaluated per graph optimised for the graphs of every branch of their enumeration. A pattern of
 * {@code GRAPH ?g} whose solutions need not come from a triple of the graph, and a sub-select inside such a clause, are
 * evaluated for every named graph anew, joined laterally with the enumeration of the graphs. The optimisation splits
 * the enumeration into branches, one for every graph a mapping gives by a constant and one for the graphs of every
 * class, and the pattern reads the graph as its branch provides it: the pattern of a constant graph keeps only the
 * table accesses mapped to that graph, with nothing left to compare, and the pattern of the graphs of a class only the
 * accesses whose graphs are of that class, instead of every branch holding all accesses of the pattern with the graphs
 * compared when the statement runs.
 */
public class GraphSpecializationTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> ";

    /**
     * Name of the schema of the test tables.
     */
    private static final String schemaName = "graph_specialization_test";

    /**
     * Configuration mapping the test tables.
     */
    private static SparqlDatabaseConfiguration config = null;

    /**
     * The same configuration with the join optimizer enabled.
     */
    private static SparqlDatabaseConfiguration optimized = null;


    /**
     * Creates the {@code graph_specialization_test} schema, whose tables are mapped with the IRI classes {@code a} and
     * {@code g} ({@code http://example.org/a/<id>} and so on): {@code a ex:link a} of the tables {@code first},
     * {@code second} and {@code third} in the graphs {@code ex:first}, {@code ex:second} and {@code ex:third}, of the
     * table {@code link} in the graphs {@code g} named by a column, and of the table {@code nine} in the graph
     * {@code g/9}, a constant of the class of that column; {@code a ex:mark} with an integer in the graph
     * {@code ex:first}. The table {@code third} is empty, so the graph {@code ex:third} does not exist. The
     * {@code ex:link} triples of the graphs form the chain {@code a/1}, {@code a/2}, {@code a/3} ({@code ex:first}),
     * {@code a/4} ({@code ex:second}), {@code a/5} ({@code g/9}), which a walk must not follow across the graphs.
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
            statement.execute("create table " + tables + "first (s int not null, o int not null)");
            statement.execute("insert into " + tables + "first values (1, 2), (2, 3)");
            statement.execute("create table " + tables + "second (s int not null, o int not null)");
            statement.execute("insert into " + tables + "second values (3, 4)");
            statement.execute("create table " + tables + "third (s int not null, o int not null)");
            statement.execute("create table " + tables + "mark (s int not null, o int not null)");
            statement.execute("insert into " + tables + "mark values (1, 10), (9, 0)");
            statement.execute("create table " + tables + "link (g int not null, s int not null, o int not null)");
            statement.execute("insert into " + tables + "link values (1, 1, 2), (1, 5, 6), (2, 7, 8)");
            statement.execute("create table " + tables + "nine (s int not null, o int not null)");
            statement.execute("insert into " + tables + "nine values (4, 5)");
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

        for(String name : List.of("first", "second", "third"))
            configuration.addQuadMapping(table(name), configuration.createIriMapping("ex:" + name),
                    configuration.createIriMapping("a", "s"), configuration.createIriMapping("ex:link"),
                    configuration.createIriMapping("a", "o"));

        configuration.addQuadMapping(table("mark"), configuration.createIriMapping("ex:first"),
                configuration.createIriMapping("a", "s"), configuration.createIriMapping("ex:mark"),
                configuration.createLiteralMapping(xsdInt, "o"));

        configuration.addQuadMapping(table("link"), configuration.createIriMapping("g", "g"),
                configuration.createIriMapping("a", "s"), configuration.createIriMapping("ex:link"),
                configuration.createIriMapping("a", "o"));

        configuration.addQuadMapping(table("nine"), configuration.createIriMapping(resource("g/9")),
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
     * Number of the accesses to the test table in the SQL.
     *
     * @param sql the SQL
     * @param name the name of the table
     * @return number of the accesses to the test table in the SQL
     */
    private static int reads(String sql, String name)
    {
        String access = "FROM \"" + schemaName + "\".\"" + name + "\"";
        int count = 0;

        for(int index = sql.indexOf(access); index != -1; index = sql.indexOf(access, index + 1))
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
    @DisplayName("a sub-select evaluated per graph reads the table of a constant graph for that graph only")
    void subSelect() throws Exception
    {
        String query = "SELECT ?g ?c WHERE { GRAPH ?g { SELECT (COUNT(*) AS ?c) WHERE { ?s ex:link ?e } } }";

        assertThat(execute(query),
                containsInAnyOrder(row(resource("first"), integer(2)), row(resource("second"), integer(1)),
                        row(resource("g/1"), integer(2)), row(resource("g/2"), integer(1)),
                        row(resource("g/9"), integer(1))));

        String sql = translate(query);

        // every table is read once by the enumeration of the graphs and once by the pattern, in the branch of its graph
        for(String name : List.of("first", "second", "third", "link", "nine"))
            assertThat(reads(sql, name), is(2));

        // the table of another predicate is read by the enumeration only
        assertThat(reads(sql, "mark"), is(1));

        // a constant graph leaves nothing to compare, and the pattern of such a graph is joined as it is
        for(String name : List.of("first", "second", "third"))
            assertThat(sql, not(containsString("'http://example.org/" + name + "'::varchar = ")));

        assertThat(sql.split(" CROSS JOIN LATERAL ", -1).length, is(2));

        // the graphs of the class of the graph column are compared in that class, also the one given by a constant
        assertThat(sql, containsString("\"link\" WHERE ((\"g\" = \"lateral0\".\"g#g_par0\"))"));
        assertThat(sql, containsString("\"nine\" WHERE (('9'::int4 = \"lateral0\".\"g#g_par0\"))"));
    }


    @Test
    @DisplayName("the right side of an OPTIONAL evaluated per graph reads the table of a constant graph for that graph")
    void optional() throws Exception
    {
        String query = "SELECT ?g ?s ?e WHERE { GRAPH ?g { OPTIONAL { ?s ex:link ?e } } }";

        assertThat(execute(query),
                containsInAnyOrder(row(resource("first"), resource("a/1"), resource("a/2")),
                        row(resource("first"), resource("a/2"), resource("a/3")),
                        row(resource("second"), resource("a/3"), resource("a/4")),
                        row(resource("g/1"), resource("a/1"), resource("a/2")),
                        row(resource("g/1"), resource("a/5"), resource("a/6")),
                        row(resource("g/2"), resource("a/7"), resource("a/8")),
                        row(resource("g/9"), resource("a/4"), resource("a/5"))));

        String sql = translate(query);

        for(String name : List.of("first", "second", "third", "link", "nine"))
            assertThat(reads(sql, name), is(2));
    }


    @Test
    @DisplayName("both sides of a MINUS evaluated per graph read the tables of a constant graph for that graph")
    void minus() throws Exception
    {
        String query = "SELECT ?g ?s WHERE { GRAPH ?g { OPTIONAL { ?s ex:link ?e } MINUS { ?s ex:mark ?m } } }";

        // the subject with a mark in the graph ex:first stays in the graph g/1
        assertThat(execute(query),
                containsInAnyOrder(row(resource("first"), resource("a/2")), row(resource("second"), resource("a/3")),
                        row(resource("g/1"), resource("a/1")), row(resource("g/1"), resource("a/5")),
                        row(resource("g/2"), resource("a/7")), row(resource("g/9"), resource("a/4"))));

        String sql = translate(query);

        for(String name : List.of("first", "second", "third", "link", "nine", "mark"))
            assertThat(reads(sql, name), is(2));
    }


    @Test
    @DisplayName("the sides of a MINUS evaluated per graph do not share the graph")
    void minusWithoutSharedVariable() throws Exception
    {
        String query = "SELECT ?g ?s WHERE { GRAPH ?g { OPTIONAL { ?s ex:link ?e } MINUS { ?x ex:mark ?m } } }";

        assertThat(execute(query),
                containsInAnyOrder(row(resource("first"), resource("a/1")), row(resource("first"), resource("a/2")),
                        row(resource("second"), resource("a/3")), row(resource("g/1"), resource("a/1")),
                        row(resource("g/1"), resource("a/5")), row(resource("g/2"), resource("a/7")),
                        row(resource("g/9"), resource("a/4"))));
    }


    @Test
    @DisplayName("an EXISTS pattern evaluated per graph reads the table of a constant graph for that graph only")
    void existsPattern() throws Exception
    {
        String query = "SELECT ?g ?x WHERE { GRAPH ?g { BIND(1 AS ?x) FILTER EXISTS { ?s ex:mark ?m } } }";

        assertThat(execute(query), containsInAnyOrder(row(resource("first"), integer(1))));
        assertThat(reads(translate(query), "mark"), is(2));
    }


    @Test
    @DisplayName("an EXISTS pattern in the HAVING clause of a sub-select evaluated per graph stays in the graph")
    void existsInHaving() throws Exception
    {
        String query = "SELECT ?g ?c WHERE { GRAPH ?g { SELECT (COUNT(*) AS ?c) WHERE { ?s ex:link ?e } "
                + "HAVING(EXISTS { ?x ex:mark ?m }) } }";

        assertThat(execute(query), containsInAnyOrder(row(resource("first"), integer(2))));
        assertThat(reads(translate(query), "mark"), is(2));
    }


    @Test
    @DisplayName("an EXISTS pattern in a sub-select evaluated per graph stays in the graph")
    void existsPatternInSubSelect() throws Exception
    {
        String query = "SELECT ?g ?n WHERE { GRAPH ?g { SELECT (COUNT(*) AS ?n) WHERE { ?s ex:link ?e "
                + "FILTER NOT EXISTS { ?s ex:mark ?m } } } }";

        assertThat(execute(query),
                containsInAnyOrder(row(resource("first"), integer(1)), row(resource("second"), integer(1)),
                        row(resource("g/1"), integer(2)), row(resource("g/2"), integer(1)),
                        row(resource("g/9"), integer(1))));

        String sql = translate(query);

        for(String name : List.of("first", "second", "third", "link", "nine", "mark"))
            assertThat(reads(sql, name), is(2));
    }


    @Test
    @DisplayName("a walk evaluated per graph stays in the graph")
    void walk() throws Exception
    {
        String query = "SELECT ?g ?s ?e WHERE { GRAPH ?g { OPTIONAL { ?s ex:link+ ?e } } }";

        assertThat(execute(query),
                containsInAnyOrder(row(resource("first"), resource("a/1"), resource("a/2")),
                        row(resource("first"), resource("a/2"), resource("a/3")),
                        row(resource("first"), resource("a/1"), resource("a/3")),
                        row(resource("second"), resource("a/3"), resource("a/4")),
                        row(resource("g/1"), resource("a/1"), resource("a/2")),
                        row(resource("g/1"), resource("a/5"), resource("a/6")),
                        row(resource("g/2"), resource("a/7"), resource("a/8")),
                        row(resource("g/9"), resource("a/4"), resource("a/5"))));
    }
}
