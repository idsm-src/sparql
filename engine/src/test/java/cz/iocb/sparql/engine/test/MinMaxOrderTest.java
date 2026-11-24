package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.database.SqlType.VARCHAR;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.imcode.SqlSelect;
import cz.iocb.sparql.engine.mapping.ConstantIriMapping;
import cz.iocb.sparql.engine.mapping.classes.EnumUserIriClass;
import cz.iocb.sparql.engine.mapping.classes.IntegerUserIriClass;
import cz.iocb.sparql.engine.mapping.classes.MapUserIriClass;
import cz.iocb.sparql.engine.mapping.classes.StringUserIriClass;
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
 * MIN and MAX of IRIs: the least and the greatest IRI are the first and the last IRI of ORDER BY, which orders IRIs by
 * their text, also where the stored values of an IRI class are ordered otherwise: the ids of a mapping table, the
 * numbers of IRIs written without padding or padded to a minimal width, whose digits are ordered as text, the ids
 * followed by a suffix, and the stored values of an enumeration. A class whose stored values are ordered as its IRIs,
 * like the numbers of a fixed width, is aggregated as it is stored. The order column of the IRIs of a mapping table is
 * read by a join with the table, or along with the rows of the table that the solutions read, and not looked up for
 * every solution. Where the default collation of the database does not order by code points, the character keys are
 * compared in the collation "C", and the values get the default collation back, so that they can be compared with a
 * column of another code-point collation.
 */
public class MinMaxOrderTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX : <http://example.org/> ";

    /**
     * Engine over the configuration mapping the test tables.
     */
    private static Engine engine = null;

    /**
     * Engine over the same mappings with the join optimizer.
     */
    private static Engine optimizedEngine = null;

    /**
     * Engines over the same mappings with a schema taking the default collation of the database for one that does not
     * order by code points, with the planner's joins and with the join optimizer.
     */
    private static List<Engine> collatedEngines = null;


    /**
     * Creates the {@code min_max_order_test} schema: the items with a value of every IRI class, the mapping tables of
     * the mapped classes, and the labels in a column of the collation {@code ucs_basic}.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema min_max_order_test");
            statement.execute(
                    "create table min_max_order_test.codes (id int4 primary key, name varchar not null unique)");
            statement.execute("insert into min_max_order_test.codes values (1, 'zeta'), (2, 'beta'), (3, 'alpha')");
            statement.execute(
                    "create table min_max_order_test.pages (id int4 primary key, name varchar not null unique)");
            statement.execute("insert into min_max_order_test.pages values (1, 'a'), (2, 'a-')");
            statement.execute("""
                    create table min_max_order_test.item (id int4 primary key, grp int4 not null, \
                    num int4 not null, pad int4 not null, fixed int4 not null, code int4, page int4 not null, \
                    word varchar not null, member int4 not null)""");
            statement.execute("""
                    insert into min_max_order_test.item values (1, 1, 2, 5, 7, 1, 1, 'a', 2), \
                    (2, 1, 10, 100, 12, 2, 2, 'a-', 1), (3, 2, 3, 99, 9, 3, 1, 'b', 2), \
                    (4, 2, 9, 7, 100, null, 2, 'a~', 1)""");
            statement.execute("create table min_max_order_test.label (id int4 primary key, "
                    + "word varchar collate \"ucs_basic\" not null)");
            statement.execute("insert into min_max_order_test.label values (1, 'a-'), (2, 'b')");
        }

        engine = new Engine(configuration(connectionPool, new DatabaseSchema(connectionPool), false));
        optimizedEngine = new Engine(configuration(connectionPool, new DatabaseSchema(connectionPool), true));

        DatabaseSchema collated = new DatabaseSchema(connectionPool)
        {
            {
                codepointDefaultCollation = false;
            }
        };

        collatedEngines = List.of(new Engine(configuration(connectionPool, collated, false)),
                new Engine(configuration(connectionPool, collated, true)));
    }


    /**
     * Configuration mapping the groups of the items as {@code :group}, their values by a predicate named after the IRI
     * class of the values, the labels as {@code :label}, and the names of the codes as {@code :name}.
     *
     * @param connectionPool the connection pool
     * @param schema schema of the test database
     * @param joinOptimizer whether the join optimizer is enabled
     * @return configuration mapping the test tables
     * @throws SQLException on database errors
     */
    private static SparqlDatabaseConfiguration configuration(DataSource connectionPool, DatabaseSchema schema,
            boolean joinOptimizer) throws SQLException
    {
        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);
        config.addPrefix("", "http://example.org/");

        config.addIriClass(new IntegerUserIriClass("item", INT4, "http://example.org/item/"));
        config.addIriClass(new IntegerUserIriClass("num", INT4, "http://example.org/num/"));
        config.addIriClass(new IntegerUserIriClass("pad", INT4, "http://example.org/pad/", -2));
        config.addIriClass(new IntegerUserIriClass("fixed", INT4, "http://example.org/fixed/", 3));
        config.addIriClass(new MapUserIriClass("code", INT4, new DatabaseTable("min_max_order_test", "codes"),
                new TableColumn("id", INT4), new TableColumn("name", VARCHAR), "http://example.org/code/"));
        config.addIriClass(new MapUserIriClass("page", INT4, new DatabaseTable("min_max_order_test", "pages"),
                new TableColumn("id", INT4), new TableColumn("name", VARCHAR), "http://example.org/page/", null,
                ".html"));
        config.addIriClass(new StringUserIriClass("word", "http://example.org/word/", null, ".html"));
        config.addIriClass(new EnumUserIriClass("member", INT4, Map.of(iri("member/x"), "2", iri("member/y"), "1")));

        DatabaseTable item = new DatabaseTable("min_max_order_test", "item");
        ConstantIriMapping graph = config.createIriMapping("<http://example.org/graph>");

        config.addQuadMapping(item, graph, config.createIriMapping("item", "id"), config.createIriMapping(":group"),
                config.createLiteralMapping(xsdInt, "grp"));

        for(String name : List.of("num", "pad", "fixed", "code", "page", "word", "member"))
            config.addQuadMapping(item, graph, config.createIriMapping("item", "id"),
                    config.createIriMapping(":" + name), config.createIriMapping(name, name));

        config.addQuadMapping(new DatabaseTable("min_max_order_test", "label"), graph,
                config.createIriMapping("item", "id"), config.createIriMapping(":label"),
                config.createIriMapping("word", "word"));

        config.addQuadMapping(new DatabaseTable("min_max_order_test", "codes"), graph,
                config.createIriMapping("code", "id"), config.createIriMapping(":name"),
                config.createLiteralMapping(xsdString, "name"));

        config.setJoinOptimizer(joinOptimizer);

        return config;
    }


    /**
     * Runs the query and returns all rows.
     *
     * @param engine the engine
     * @param query the query, without the prefix declarations
     * @return the rows
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws LimitExceedException if the generated SQL is too long
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static List<List<RdfTerm>> execute(Engine engine, String query)
            throws SQLException, TranslateExceptions, LimitExceedException, ServiceException
    {
        try(Request request = engine.getRequest(); Result result = request.execute(prefix + query))
        {
            List<List<RdfTerm>> rows = new ArrayList<>();

            while(result.next())
                rows.add(Arrays.asList(result.getRow()));

            return rows;
        }
    }


    /**
     * Translates the query into SQL.
     *
     * @param engine the engine
     * @param query the query, without the prefix declarations
     * @return the generated SQL
     * @throws Exception on errors
     */
    private static String translate(Engine engine, String query) throws Exception
    {
        try(Request request = engine.getRequest())
        {
            PreparedQuery prepared = request.prepareQuery(prefix + query, null);
            SqlSelect imcode = new TranslateVisitor(request).translate(prepared.getSyntaxTree(), null, null, List.of());

            return imcode.optimize(request, false).optimize(request, true).translate(request);
        }
    }


    /**
     * IRI of the example namespace.
     *
     * @param name the local name
     * @return IRI of the example namespace
     */
    private static Iri iri(String name)
    {
        return new Iri("http://example.org/" + name);
    }


    /**
     * Expected row.
     *
     * @param terms the terms of the row
     * @return the row
     */
    private static List<RdfTerm> row(RdfTerm... terms)
    {
        return Arrays.asList(terms);
    }


    /**
     * Checks that MIN and MAX (also with DISTINCT) of the values of the predicate are the given IRIs, and that ORDER BY
     * gives them as the first and the last value.
     *
     * @param engine the engine
     * @param predicate the local name of the predicate
     * @param min the least value
     * @param max the greatest value
     * @throws Exception on errors
     */
    private static void assertExtremes(Engine engine, String predicate, Iri min, Iri max) throws Exception
    {
        String pattern = "WHERE { ?s :" + predicate + " ?v } ";

        List<List<RdfTerm>> ordered = execute(engine, "SELECT ?v " + pattern + "ORDER BY ?v");

        assertEquals(min, ordered.getFirst().getFirst());
        assertEquals(max, ordered.getLast().getFirst());

        assertEquals(List.of(row(min, max)), execute(engine, "SELECT (MIN(?v) AS ?min) (MAX(?v) AS ?max) " + pattern));
        assertEquals(List.of(row(min, max)),
                execute(engine, "SELECT (MIN(DISTINCT ?v) AS ?min) (MAX(DISTINCT ?v) AS ?max) " + pattern));
    }


    @Test
    @DisplayName("the numbers of IRIs written without padding are ordered as text")
    void unpaddedNumbers() throws Exception
    {
        assertExtremes(engine, "num", iri("num/10"), iri("num/9"));
    }


    @Test
    @DisplayName("the numbers of IRIs padded to a minimal width are ordered as text")
    void minimalWidthNumbers() throws Exception
    {
        assertExtremes(engine, "pad", iri("pad/05"), iri("pad/99"));
    }


    @Test
    @DisplayName("the numbers of IRIs of a fixed width are aggregated as they are stored")
    void fixedWidthNumbers() throws Exception
    {
        assertExtremes(engine, "fixed", iri("fixed/007"), iri("fixed/100"));

        assertThat(translate(engine, "SELECT (MIN(?v) AS ?min) WHERE { ?s :fixed ?v }"),
                matchesPattern("(?s).*sparql\\.min\\(\"[^\"]*\"\\).*"));
    }


    @Test
    @DisplayName("the ids of a mapping table are ordered as the IRIs they stand for")
    void mappedIds() throws Exception
    {
        for(Engine tested : List.of(engine, optimizedEngine))
        {
            assertExtremes(tested, "code", iri("code/alpha"), iri("code/zeta"));

            // the order column of the IRIs is read by a join with the mapping table, and the IRIs are made of it
            assertThat(translate(tested, "SELECT (MIN(?v) AS ?min) (MAX(?v) AS ?max) WHERE { ?s :code ?v }"),
                    not(containsString("@rctab")));
        }
    }


    @Test
    @DisplayName("the ids of a mapping table are ordered as the IRIs with their suffix")
    void mappedIdsWithSuffix() throws Exception
    {
        for(Engine tested : List.of(engine, optimizedEngine))
        {
            assertExtremes(tested, "page", iri("page/a-.html"), iri("page/a.html"));

            assertThat(translate(tested, "SELECT (MIN(?v) AS ?min) (MAX(?v) AS ?max) WHERE { ?s :page ?v }"),
                    not(containsString("@rctab")));
        }
    }


    @Test
    @DisplayName("the order column of the IRIs read along with the rows of the mapping table the solutions read")
    void mappedIdsOfTheSolutions() throws Exception
    {
        String query = "SELECT (MIN(?c) AS ?min) (MAX(?c) AS ?max) WHERE { ?c :name ?n }";

        for(Engine tested : List.of(engine, optimizedEngine))
        {
            assertEquals(List.of(row(iri("code/alpha"), iri("code/zeta"))), execute(tested, query));

            // the access of the solutions reads the order column along, without a join or a lookup
            String sql = translate(tested, query);
            assertThat(sql, not(containsString("@rctab")));
            assertThat(sql, not(containsString("JOIN")));
        }
    }


    @Test
    @DisplayName("the IRIs of two variables of mapping tables")
    void twoMappedVariables() throws Exception
    {
        for(Engine tested : List.of(engine, optimizedEngine))
            assertEquals(List.of(row(iri("code/alpha"), iri("page/a.html"), iri("code/zeta"))), execute(tested,
                    "SELECT (MIN(?c) AS ?a) (MAX(?p) AS ?b) (MAX(?c) AS ?d) WHERE { ?s :code ?c ; :page ?p }"));
    }


    @Test
    @DisplayName("the ids of IRIs with a suffix are ordered as the IRIs with the suffix")
    void idsWithSuffix() throws Exception
    {
        assertExtremes(engine, "word", iri("word/a-.html"), iri("word/b.html"));
    }


    @Test
    @DisplayName("the stored values of an enumeration are ordered as the IRIs they stand for")
    void enumeration() throws Exception
    {
        assertExtremes(engine, "member", iri("member/x"), iri("member/y"));
    }


    @Test
    @DisplayName("the least and the greatest IRI of every group")
    void groups() throws Exception
    {
        List<RdfTerm> first = row(new TypedLiteral("1", xsdIntIri), new TypedLiteral("2", xsdIntegerIri),
                iri("code/beta"), iri("code/zeta"));
        List<RdfTerm> second = row(new TypedLiteral("2", xsdIntIri), new TypedLiteral("1", xsdIntegerIri),
                iri("code/alpha"), iri("code/alpha"));

        for(Engine tested : List.of(engine, optimizedEngine))
            assertEquals(List.of(first, second), execute(tested, "SELECT ?g (COUNT(?v) AS ?count) (MIN(?v) AS ?min) "
                    + "(MAX(?v) AS ?max) WHERE { ?s :group ?g ; :code ?v } GROUP BY ?g ORDER BY ?g"));
    }


    @Test
    @DisplayName("an unbound value makes MIN an error, whereas MAX passes it by")
    void unboundValue() throws Exception
    {
        for(Engine tested : List.of(engine, optimizedEngine))
            assertEquals(List.of(row(null, iri("code/zeta"))), execute(tested,
                    "SELECT (MIN(?v) AS ?min) (MAX(?v) AS ?max) WHERE { ?s :group ?g OPTIONAL { ?s :code ?v } }"));
    }


    @Test
    @DisplayName("the character keys are compared in the collation C where the default one orders otherwise")
    void collatedKeys() throws Exception
    {
        for(Engine collatedEngine : collatedEngines)
        {
            assertExtremes(collatedEngine, "num", iri("num/10"), iri("num/9"));
            assertExtremes(collatedEngine, "code", iri("code/alpha"), iri("code/zeta"));
            assertExtremes(collatedEngine, "page", iri("page/a-.html"), iri("page/a.html"));
            assertExtremes(collatedEngine, "word", iri("word/a-.html"), iri("word/b.html"));
            assertExtremes(collatedEngine, "member", iri("member/x"), iri("member/y"));

            assertEquals(
                    List.of(row(new TypedLiteral("http://example.org/word/a-.html", xsdStringIri),
                            new TypedLiteral("http://example.org/word/b.html", xsdStringIri))),
                    execute(collatedEngine,
                            "SELECT (MIN(STR(?v)) AS ?min) (MAX(STR(?v)) AS ?max) WHERE { ?s :word ?v }"));

            assertThat(translate(collatedEngine, "SELECT (MIN(?v) AS ?min) WHERE { ?s :word ?v }"),
                    containsString("COLLATE \"C\")) COLLATE \"default\""));
        }

        assertThat(translate(engine, "SELECT (MIN(?v) AS ?min) WHERE { ?s :word ?v }"), not(containsString("COLLATE")));
    }


    @Test
    @DisplayName("the least value compared with a column of another code-point collation")
    void collatedJoin() throws Exception
    {
        String query = "SELECT ?s WHERE { { SELECT (MIN(?w) AS ?m) WHERE { ?i :word ?w } } ?s :label ?m }";

        assertEquals(List.of(row(iri("item/1"))), execute(engine, query));

        for(Engine collatedEngine : collatedEngines)
            assertEquals(List.of(row(iri("item/1"))), execute(collatedEngine, query));
    }
}
