package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.database.SqlType.INT8;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.sql.DataSource;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.config.EngineSettings.JoinOptimizerMode;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.Conditions;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.imcode.SqlSelect;
import cz.iocb.sparql.engine.mapping.ConstantIriMapping;
import cz.iocb.sparql.engine.mapping.JoinTableQuadMapping.JoinColumns;
import cz.iocb.sparql.engine.mapping.classes.IntegerUserIriClass;
import cz.iocb.sparql.engine.mapping.classes.StringUserIriClass;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.TypedLiteral;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Request.PreparedQuery;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.translator.TranslateVisitor;
import cz.iocb.sparql.testing.Database;



/**
 * Dictionary tables: a row of a table declared so that constants select by a key is read when the query is translated,
 * and the access to the table is taken by the key it is joined by, so that the table referencing that key is restricted
 * by constants and the dictionary table is not read by the query.
 *
 * The table {@code kind} holds the kinds red (1), green (2) and blue (3), identified by their names, of which blue has
 * no label; the table {@code item} holds the items 10 and 11 of the kind red, 12 of the kind green and 13 of the kind
 * blue, and the table {@code follows} says that red follows green and blue and that green follows blue. The table
 * {@code term} numbers five terms kept in boxes, and the table {@code fact} relates their numbers: s1 to s2 and to
 * {@code "x"}, s2 to {@code "x"} and to the integer 5.
 */
public class DictionaryTableTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = """
            PREFIX ex: <http://example.org/> \
            PREFIX kind: <http://example.org/kind/> \
            PREFIX item: <http://example.org/item/> """;

    /**
     * The table of the kinds as the generated code names it.
     */
    private static final String kindTable = "\"dictionary_test\".\"kind\"";

    /**
     * The table of the terms as the generated code names it.
     */
    private static final String termTable = "\"dictionary_test\".\"term\"";

    /**
     * Configuration declaring the dictionary tables.
     */
    private static SparqlDatabaseConfiguration dictionary = null;

    /**
     * Configuration declaring the dictionary tables, with the join optimizer on.
     */
    private static SparqlDatabaseConfiguration optimized = null;

    /**
     * Configuration declaring no dictionary table.
     */
    private static SparqlDatabaseConfiguration plain = null;


    /**
     * Creates the {@code dictionary_test} schema with its tables and the three configurations mapping them.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema dictionary_test");

            statement.execute("create table dictionary_test.kind (id int primary key, "
                    + "name varchar not null unique, label varchar)");
            statement.execute("insert into dictionary_test.kind values (1, 'red', 'Red'), (2, 'green', 'Green'), "
                    + "(3, 'blue', null)");

            statement.execute("create table dictionary_test.item (id int primary key, "
                    + "kind int not null references dictionary_test.kind(id))");
            statement.execute("insert into dictionary_test.item values (10, 1), (11, 1), (12, 2), (13, 3)");

            statement.execute("""
                    create table dictionary_test.follows (a int not null references dictionary_test.kind(id), \
                    b int not null references dictionary_test.kind(id), primary key (a, b))""");
            statement.execute("insert into dictionary_test.follows values (1, 2), (1, 3), (2, 3)");

            statement.execute(
                    "create table dictionary_test.term (id int8 primary key, term sparql.rdfbox not null unique)");
            statement.execute("""
                    insert into dictionary_test.term values (1, '<http://example.org/s1>'), \
                    (2, '<http://example.org/s2>'), (3, '"x"'), \
                    (4, '"5"^^<http://www.w3.org/2001/XMLSchema#integer>'), (5, '<http://example.org/s5>')""");

            statement.execute("""
                    create table dictionary_test.fact (subject int8 not null references dictionary_test.term(id), \
                    object int8 not null references dictionary_test.term(id), primary key (subject, object))""");
            statement.execute("insert into dictionary_test.fact values (1, 2), (1, 3), (2, 3), (2, 4)");
        }

        DatabaseSchema schema = new DatabaseSchema(connectionPool);

        dictionary = configure(connectionPool, schema, true);

        optimized = configure(connectionPool, schema, true);
        optimized.setEngineSettings(optimized.getEngineSettings().withJoinOptimizer(JoinOptimizerMode.SETTINGS));

        plain = configure(connectionPool, schema, false);
    }


    /**
     * Creates a configuration mapping the tables: the kind of an item (ex:kind, and ex:labelledKind for the kinds that
     * have a label), the kinds following a kind (ex:follows), the number and the label of a kind (ex:code, ex:label),
     * and the facts of the numbered terms (ex:rel).
     *
     * @param connectionPool the connection pool of the test database
     * @param schema the schema of the test database
     * @param declared whether the tables of the kinds and of the terms are declared to be dictionary tables
     * @return the configuration
     * @throws SQLException on database errors
     */
    private static SparqlDatabaseConfiguration configure(DataSource connectionPool, DatabaseSchema schema,
            boolean declared) throws SQLException
    {
        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);

        config.addPrefix("ex", "http://example.org/");
        config.addIriClass(new IntegerUserIriClass("item", INT4, "http://example.org/item/"));
        config.addIriClass(new StringUserIriClass("kind", "http://example.org/kind/"));

        DatabaseTable kind = new DatabaseTable("dictionary_test", "kind");
        DatabaseTable item = new DatabaseTable("dictionary_test", "item");
        DatabaseTable follows = new DatabaseTable("dictionary_test", "follows");
        DatabaseTable term = new DatabaseTable("dictionary_test", "term");
        DatabaseTable fact = new DatabaseTable("dictionary_test", "fact");
        ConstantIriMapping graph = config.createIriMapping("<http://example.org/graph>");

        JoinColumns kindOfItem = new JoinColumns(new TableColumn("kind", INT4), new TableColumn("id", INT4));

        config.addQuadMapping(asList(item, kind), asList(kindOfItem), graph, config.createIriMapping("item", "id"),
                config.createIriMapping("ex:kind"), config.createIriMapping("kind", "name"));

        config.addQuadMapping(asList(item, kind), asList(kindOfItem), graph, config.createIriMapping("item", "id"),
                config.createIriMapping("ex:labelledKind"), config.createIriMapping("kind", "name"),
                asList(new Conditions(true), config.createIsNotNullCondition(kind, "label")));

        config.addQuadMapping(asList(kind, follows, kind),
                asList(new JoinColumns(new TableColumn("id", INT4), new TableColumn("a", INT4)),
                        new JoinColumns(new TableColumn("b", INT4), new TableColumn("id", INT4))),
                graph, config.createIriMapping("kind", "name"), config.createIriMapping("ex:follows"),
                config.createIriMapping("kind", "name"));

        config.addQuadMapping(kind, graph, config.createIriMapping("kind", "name"), config.createIriMapping("ex:code"),
                config.createLiteralMapping(xsdInt, "id"));

        config.addQuadMapping(kind, graph, config.createIriMapping("kind", "name"), config.createIriMapping("ex:label"),
                config.createLiteralMapping(xsdString, "label"));

        config.addQuadMapping(asList(term, fact, term),
                asList(new JoinColumns(new TableColumn("id", INT8), new TableColumn("subject", INT8)),
                        new JoinColumns(new TableColumn("object", INT8), new TableColumn("id", INT8))),
                graph, config.createTermMapping(box, "term"), config.createIriMapping("ex:rel"),
                config.createTermMapping(box, "term"));

        if(declared)
        {
            config.addDictionaryTable(kind);
            config.addDictionaryTable(term);
        }

        return config;
    }


    /**
     * Runs the query over the configuration and returns all rows.
     *
     * @param config the configuration
     * @param query the query, without the prefix declarations
     * @return rows of the query
     * @throws Exception when the query fails
     */
    private static List<List<RdfTerm>> execute(SparqlDatabaseConfiguration config, String query) throws Exception
    {
        Engine engine = new Engine(config);

        try(Request request = engine.getRequest(); Result result = request.execute(prefix + query))
        {
            List<List<RdfTerm>> rows = new ArrayList<>();

            while(result.next())
                rows.add(Arrays.asList(result.getRow()));

            return rows;
        }
    }


    /**
     * Returns the SQL code the query is translated to over the configuration.
     *
     * @param config the configuration
     * @param query the query, without the prefix declarations
     * @return the generated SQL
     * @throws Exception when the query cannot be translated
     */
    private static String translate(SparqlDatabaseConfiguration config, String query) throws Exception
    {
        Engine engine = new Engine(config);

        try(Request request = engine.getRequest())
        {
            return translate(request, query);
        }
    }


    /**
     * Returns the SQL code the query is translated to within the request.
     *
     * @param request the request
     * @param query the query, without the prefix declarations
     * @return the generated SQL
     * @throws Exception when the query cannot be translated
     */
    private static String translate(Request request, String query) throws Exception
    {
        PreparedQuery prepared = request.prepareQuery(prefix + query, null);
        SqlSelect imcode = new TranslateVisitor(request).translate(prepared.getSyntaxTree(), null, null, List.of());

        return imcode.optimize(request, false).optimize(request, true).translate(request);
    }


    /**
     * Checks that the query gives the same solutions whether the dictionary tables are declared or not, with the join
     * optimizer and without it.
     *
     * @param query the query, without the prefix declarations
     * @return the solutions
     * @throws Exception when the query fails
     */
    private static List<List<RdfTerm>> check(String query) throws Exception
    {
        List<List<RdfTerm>> expected = execute(plain, query);

        assertThat(execute(dictionary, query), containsInAnyOrder(expected.toArray()));
        assertThat(execute(optimized, query), containsInAnyOrder(expected.toArray()));

        return expected;
    }


    /**
     * Matcher of the SQL code comparing the column with the constant, whichever side the code writes the column on.
     *
     * @param column name of the column
     * @param constant SQL code of the constant
     * @return matcher of the SQL code comparing the column with the constant
     */
    private static Matcher<String> equality(String column, String constant)
    {
        String name = '"' + column + '"';

        return anyOf(containsString(name + " = " + constant), containsString(constant + " = " + name));
    }


    /**
     * Expected row of the terms.
     *
     * @param terms the terms
     * @return row of the terms
     */
    private static List<RdfTerm> row(RdfTerm... terms)
    {
        return Arrays.asList(terms);
    }


    /**
     * The IRI of the example namespace with the given local name.
     *
     * @param name the local name
     * @return the IRI
     */
    private static Iri iri(String name)
    {
        return new Iri("http://example.org/" + name);
    }


    /**
     * Expected row of the item with the given number.
     *
     * @param id the number of the item
     * @return row of the item
     */
    private static List<RdfTerm> item(int id)
    {
        return row(iri("item/" + id));
    }


    /**
     * Expected row of the kind with the given name.
     *
     * @param name the name of the kind
     * @return row of the kind
     */
    private static List<RdfTerm> kind(String name)
    {
        return row(iri("kind/" + name));
    }


    @Test
    @DisplayName("a constant object restricts the referencing table by the number of its row")
    void constantObject() throws Exception
    {
        String query = "SELECT ?i WHERE { ?i ex:kind kind:red }";

        assertThat(check(query), containsInAnyOrder(item(10), item(11)));

        for(SparqlDatabaseConfiguration config : List.of(dictionary, optimized))
        {
            assertThat(translate(config, query), not(containsString(kindTable)));
            assertThat(translate(config, query), equality("kind", "'1'::int4"));
        }

        assertThat(translate(plain, query), containsString(kindTable));
    }


    @Test
    @DisplayName("a constant the dictionary does not hold gives no solution")
    void unknownConstant() throws Exception
    {
        assertThat(check("SELECT ?i WHERE { ?i ex:kind kind:black }"), empty());

        assertThat(check("SELECT ?i WHERE { ?i ex:kind kind:red FILTER NOT EXISTS { ?i ex:kind kind:black } }"),
                containsInAnyOrder(item(10), item(11)));

        assertThat(check("SELECT ?i WHERE { ?i ex:kind kind:red MINUS { ?i ex:kind kind:black } }"),
                containsInAnyOrder(item(10), item(11)));

        assertThat(check("SELECT ?i ?j WHERE { ?i ex:kind kind:green OPTIONAL { ?j ex:kind kind:black } }"),
                containsInAnyOrder(row(iri("item/12"), null)));
    }


    @Test
    @DisplayName("constants of inline data restrict the referencing table by the numbers of their rows")
    void inlineData() throws Exception
    {
        String query = "SELECT ?i WHERE { VALUES ?k { kind:red kind:blue kind:black } ?i ex:kind ?k }";

        assertThat(check(query), containsInAnyOrder(item(10), item(11), item(13)));
        assertThat(translate(dictionary, query), not(containsString(kindTable)));
    }


    @Test
    @DisplayName("a variable a filter restricts to constants keeps its dictionary")
    void filteredVariable() throws Exception
    {
        assertThat(check("SELECT ?i WHERE { ?i ex:kind ?k FILTER(?k = kind:red) }"),
                containsInAnyOrder(item(10), item(11)));

        assertThat(check("SELECT ?i WHERE { ?i ex:kind ?k FILTER(?k IN (kind:red, kind:blue, kind:black)) }"),
                containsInAnyOrder(item(10), item(11), item(13)));

        assertThat(check("SELECT ?i ?k WHERE { ?i ex:kind ?k FILTER(?k != kind:red) }"),
                containsInAnyOrder(row(iri("item/12"), iri("kind/green")), row(iri("item/13"), iri("kind/blue"))));
    }


    @Test
    @DisplayName("a constant at an end of a chain of three tables")
    void chain() throws Exception
    {
        String query = "SELECT ?k WHERE { kind:red ex:follows ?k }";

        assertThat(check(query), containsInAnyOrder(kind("green"), kind("blue")));
        assertThat(translate(dictionary, query), equality("a", "'1'::int4"));

        assertThat(check("SELECT ?k WHERE { ?k ex:follows kind:blue }"),
                containsInAnyOrder(kind("red"), kind("green")));

        assertThat(check("SELECT ?k WHERE { kind:black ex:follows ?k }"), empty());
    }


    @Test
    @DisplayName("constants at both ends of a chain leave the dictionary unread")
    void bothEnds() throws Exception
    {
        String query = "SELECT ?i WHERE { kind:red ex:follows kind:blue . ?i ex:kind kind:green }";

        assertThat(check(query), containsInAnyOrder(item(12)));
        assertThat(translate(dictionary, query), not(containsString(kindTable)));

        assertThat(check("SELECT ?i WHERE { kind:blue ex:follows kind:red . ?i ex:kind kind:green }"), empty());
    }


    @Test
    @DisplayName("a constant shared by a pattern with a variable of the dictionary")
    void joinedPatterns() throws Exception
    {
        assertThat(check("SELECT ?i ?k WHERE { ?i ex:kind ?k . kind:red ex:follows ?k }"),
                containsInAnyOrder(row(iri("item/12"), iri("kind/green")), row(iri("item/13"), iri("kind/blue"))));

        assertThat(check("SELECT ?i WHERE { ?i ex:kind ?k . ?k ex:follows kind:blue }"),
                containsInAnyOrder(item(10), item(11), item(12)));
    }


    @Test
    @DisplayName("a row failing the condition of the mapping gives no solution")
    void mappingCondition() throws Exception
    {
        assertThat(check("SELECT ?i WHERE { ?i ex:labelledKind kind:red }"), containsInAnyOrder(item(10), item(11)));
        assertThat(check("SELECT ?i WHERE { ?i ex:labelledKind kind:blue }"), empty());
        assertThat(check("SELECT ?i WHERE { ?i ex:kind kind:blue }"), containsInAnyOrder(item(13)));
    }


    @Test
    @DisplayName("an access reading its key only is taken by it, an access reading another column is not")
    void boundColumns() throws Exception
    {
        String code = "SELECT ?c WHERE { kind:green ex:code ?c }";

        assertThat(check(code), containsInAnyOrder(row(new TypedLiteral("2", xsdIntIri))));
        assertThat(translate(dictionary, code), equality("id", "'2'::int4"));

        String label = "SELECT ?l WHERE { kind:green ex:label ?l }";

        assertThat(check(label), containsInAnyOrder(row(new TypedLiteral("Green", xsdStringIri))));
        assertThat(translate(dictionary, label), equality("name", "'green'::varchar"));

        assertThat(check("SELECT ?l WHERE { kind:blue ex:label ?l }"), empty());
    }


    @Test
    @DisplayName("constant terms of a dictionary of boxes")
    void boxes() throws Exception
    {
        String object = "SELECT ?o WHERE { ex:s1 ex:rel ?o }";

        assertThat(check(object), containsInAnyOrder(row(iri("s2")), row(new TypedLiteral("x", xsdStringIri))));
        assertThat(translate(dictionary, object), equality("subject", "'1'::int8"));

        String subject = "SELECT ?s WHERE { ?s ex:rel \"x\" }";

        assertThat(check(subject), containsInAnyOrder(row(iri("s1")), row(iri("s2"))));
        assertThat(translate(dictionary, subject), equality("object", "'3'::int8"));

        assertThat(check("SELECT ?s WHERE { ?s ex:rel 5 }"), containsInAnyOrder(row(iri("s2"))));
        assertThat(check("SELECT ?s WHERE { ?s ex:rel 6 }"), empty());
        assertThat(check("SELECT ?o WHERE { ex:s5 ex:rel ?o }"), empty());

        String both = "SELECT ?s WHERE { ex:s1 ex:rel ex:s2 . ?s ex:rel 5 }";

        assertThat(check(both), containsInAnyOrder(row(iri("s2"))));
        assertThat(translate(optimized, both), equality("object", "'4'::int8"));

        String count = "SELECT (COUNT(*) AS ?c) WHERE { ex:s2 ex:rel ?o }";

        assertThat(check(count), containsInAnyOrder(row(new TypedLiteral("2", xsdIntegerIri))));
        assertThat(translate(dictionary, count), not(containsString(termTable)));
        assertThat(translate(plain, count), containsString(termTable));
    }


    @Test
    @DisplayName("a row is read once for a request")
    void readOnce() throws Exception
    {
        Engine engine = new Engine(dictionary);

        try(Request request = engine.getRequest())
        {
            translate(request, "SELECT ?i ?j WHERE { ?i ex:kind kind:red . ?j ex:kind kind:red }");

            assertThat(request.getDictionaryRows(), aMapWithSize(1));
        }

        try(Request request = engine.getRequest())
        {
            assertThat(request.getDictionaryRows(), aMapWithSize(0));
        }
    }
}
