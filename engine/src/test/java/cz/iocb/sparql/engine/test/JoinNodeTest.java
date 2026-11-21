package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
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
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Request.PreparedQuery;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.translator.TranslateVisitor;
import cz.iocb.sparql.testing.Database;



/**
 * Nodes joining the tables of join mappings by the keys of terms: where a table of a join mapping holds a term in
 * columns containing a key, the patterns reading the table by the term of one variable share the node joining it, so
 * the tables they join to the rows of the term are joined to each other directly. The solutions have to be those the
 * mappings give when the key is not known, where every pattern gets nodes of its own.
 *
 * The table {@code kind} holds the kinds red (1), green (2), blue (3) and grey (4), identified by their names, of which
 * blue has no label; the table {@code follows} says that red follows green and blue, that green follows blue and that
 * blue follows itself; the table {@code item} holds the items 10 (of the kind red, its other kind green), 11 (red), 12
 * (green, blue), 13 (blue) and 14 (grey, red).
 */
public class JoinNodeTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = """
            PREFIX ex: <http://example.org/> \
            PREFIX kind: <http://example.org/kind/> \
            PREFIX item: <http://example.org/item/> """;

    /**
     * A comparison of the join columns of two accesses to the table {@code follows} in the generated code.
     */
    private static final Pattern direct = Pattern.compile("\"tab\\d+\"\\.\"[ab]\" = \"tab\\d+\"\\.\"[ab]\"");

    /**
     * Configuration knowing the names of the kinds to be a key.
     */
    private static SparqlDatabaseConfiguration shared = null;

    /**
     * Configuration knowing the names of the kinds to be a key, with the join optimizer on.
     */
    private static SparqlDatabaseConfiguration optimized = null;

    /**
     * Configuration that does not know the names of the kinds to be a key.
     */
    private static SparqlDatabaseConfiguration plain = null;


    /**
     * Creates the {@code join_node_test} schema with its tables and the three configurations mapping them; the schema
     * of the plain one is read before the names of the kinds are declared unique.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema join_node_test");

            statement.execute(
                    "create table join_node_test.kind (id int primary key, name varchar not null, label varchar)");
            statement.execute("""
                    insert into join_node_test.kind values (1, 'red', 'Red'), (2, 'green', 'Green'), \
                    (3, 'blue', null), (4, 'grey', 'Grey')""");

            statement.execute("""
                    create table join_node_test.follows (a int not null references join_node_test.kind(id), \
                    b int not null references join_node_test.kind(id), primary key (a, b))""");
            statement.execute("insert into join_node_test.follows values (1, 2), (1, 3), (2, 3), (3, 3)");

            statement.execute("""
                    create table join_node_test.item (id int primary key, \
                    kind int not null references join_node_test.kind(id), \
                    other int references join_node_test.kind(id))""");
            statement.execute("insert into join_node_test.item values (10, 1, 2), (11, 1, null), (12, 2, 3), "
                    + "(13, 3, null), (14, 4, 1)");
        }

        plain = configure(connectionPool, new DatabaseSchema(connectionPool));

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create unique index on join_node_test.kind (name)");
        }

        DatabaseSchema schema = new DatabaseSchema(connectionPool);

        shared = configure(connectionPool, schema);

        optimized = configure(connectionPool, schema);
        optimized.setJoinOptimizer(true);
    }


    /**
     * Creates a configuration mapping the tables: the kind and the other kind of an item (ex:kind, ex:other), the kinds
     * following a kind (ex:follows) and the label of a kind (ex:label).
     *
     * @param connectionPool the connection pool of the test database
     * @param schema the schema of the test database
     * @return the configuration
     * @throws SQLException on database errors
     */
    private static SparqlDatabaseConfiguration configure(DataSource connectionPool, DatabaseSchema schema)
            throws SQLException
    {
        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);

        config.addPrefix("ex", "http://example.org/");
        config.addIriClass(new IntegerUserIriClass("item", INT4, "http://example.org/item/"));
        config.addIriClass(new StringUserIriClass("kind", "http://example.org/kind/"));

        DatabaseTable kind = new DatabaseTable("join_node_test", "kind");
        DatabaseTable item = new DatabaseTable("join_node_test", "item");
        DatabaseTable follows = new DatabaseTable("join_node_test", "follows");
        ConstantIriMapping graph = config.createIriMapping("<http://example.org/graph>");

        config.addQuadMapping(asList(item, kind),
                asList(new JoinColumns(new TableColumn("kind", INT4), new TableColumn("id", INT4))), graph,
                config.createIriMapping("item", "id"), config.createIriMapping("ex:kind"),
                config.createIriMapping("kind", "name"));

        config.addQuadMapping(asList(item, kind),
                asList(new JoinColumns(new TableColumn("other", INT4), new TableColumn("id", INT4))), graph,
                config.createIriMapping("item", "id"), config.createIriMapping("ex:other"),
                config.createIriMapping("kind", "name"));

        config.addQuadMapping(asList(kind, follows, kind),
                asList(new JoinColumns(new TableColumn("id", INT4), new TableColumn("a", INT4)),
                        new JoinColumns(new TableColumn("b", INT4), new TableColumn("id", INT4))),
                graph, config.createIriMapping("kind", "name"), config.createIriMapping("ex:follows"),
                config.createIriMapping("kind", "name"));

        config.addQuadMapping(kind, graph, config.createIriMapping("kind", "name"), config.createIriMapping("ex:label"),
                config.createLiteralMapping(xsdString, "label"));

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
            PreparedQuery prepared = request.prepareQuery(prefix + query, null);
            SqlSelect imcode = new TranslateVisitor(request).translate(prepared.getSyntaxTree(), null, null, List.of());

            return imcode.optimize(request, false).optimize(request, true).translate(request);
        }
    }


    /**
     * Checks that the query gives the same solutions whether the patterns share the nodes joining the tables or not,
     * with the join optimizer and without it.
     *
     * @param query the query, without the prefix declarations
     * @return the solutions
     * @throws Exception when the query fails
     */
    private static List<List<RdfTerm>> check(String query) throws Exception
    {
        List<List<RdfTerm>> expected = execute(plain, query);

        assertThat(execute(shared, query), containsInAnyOrder(expected.toArray()));
        assertThat(execute(optimized, query), containsInAnyOrder(expected.toArray()));

        return expected;
    }


    /**
     * Returns how many times the SQL code holds the text.
     *
     * @param sql the SQL code
     * @param text the text
     * @return the number of the occurrences of the text in the code
     */
    private static int count(String sql, String text)
    {
        return sql.split(Pattern.quote(text), -1).length - 1;
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
     * The item with the given number.
     *
     * @param id the number of the item
     * @return the IRI of the item
     */
    private static Iri item(int id)
    {
        return new Iri("http://example.org/item/" + id);
    }


    /**
     * The kind with the given name.
     *
     * @param name the name of the kind
     * @return the IRI of the kind
     */
    private static Iri kind(String name)
    {
        return new Iri("http://example.org/kind/" + name);
    }


    @Test
    @DisplayName("two patterns sharing a variable join their tables directly")
    void sharedVariable() throws Exception
    {
        String query = "SELECT ?a ?x ?b WHERE { ?a ex:follows ?x . ?x ex:follows ?b }";

        assertThat(check(query),
                containsInAnyOrder(row(kind("red"), kind("green"), kind("blue")),
                        row(kind("red"), kind("blue"), kind("blue")), row(kind("green"), kind("blue"), kind("blue")),
                        row(kind("blue"), kind("blue"), kind("blue"))));

        assertThat(direct.matcher(translate(shared, query)).find(), is(true));
        assertThat(direct.matcher(translate(plain, query)).find(), is(false));
    }


    @Test
    @DisplayName("a variable at both ends of a pattern")
    void sameVariableTwice() throws Exception
    {
        assertThat(check("SELECT ?x WHERE { ?x ex:follows ?x }"), containsInAnyOrder(row(kind("blue"))));

        assertThat(check("SELECT ?i WHERE { ?i ex:kind ?x . ?x ex:follows ?x }"), containsInAnyOrder(row(item(13))));
    }


    @Test
    @DisplayName("patterns of different mappings sharing a variable")
    void differentMappings() throws Exception
    {
        assertThat(check("SELECT ?i ?n WHERE { ?i ex:kind ?k . ?k ex:follows ?n }"),
                containsInAnyOrder(row(item(10), kind("green")), row(item(10), kind("blue")),
                        row(item(11), kind("green")), row(item(11), kind("blue")), row(item(12), kind("blue")),
                        row(item(13), kind("blue"))));

        assertThat(check("SELECT ?i ?j WHERE { ?i ex:kind ?k . ?j ex:other ?k }"), containsInAnyOrder(
                row(item(10), item(14)), row(item(11), item(14)), row(item(12), item(10)), row(item(13), item(12))));

        assertThat(check("SELECT ?i ?l WHERE { ?i ex:other ?k . ?k ex:label ?l . ?n ex:follows ?k }"), hasSize(1));

        assertThat(check("SELECT ?a ?i WHERE { ?a ex:follows ?x . ?x ex:follows ?b . ?i ex:kind ?x }"), hasSize(4));
    }


    @Test
    @DisplayName("a shared variable across the sides of an OPTIONAL")
    void optional() throws Exception
    {
        assertThat(check("SELECT ?i ?n WHERE { ?i ex:kind ?k OPTIONAL { ?k ex:follows ?n } }"),
                containsInAnyOrder(row(item(10), kind("green")), row(item(10), kind("blue")),
                        row(item(11), kind("green")), row(item(11), kind("blue")), row(item(12), kind("blue")),
                        row(item(13), kind("blue")), row(item(14), null)));

        // a solution leaving the variable unbound is extended by every solution of the right side
        assertThat(check("SELECT ?i ?o ?n WHERE { ?i ex:kind ?k OPTIONAL { ?i ex:other ?o } "
                + "OPTIONAL { ?o ex:follows ?n } }"), hasSize(12));

        assertThat(check("SELECT ?i ?o ?n WHERE { ?i ex:kind ?k OPTIONAL { ?i ex:other ?o . ?o ex:follows ?n } }"),
                hasSize(6));
    }


    @Test
    @DisplayName("a shared variable in MINUS and in EXISTS patterns")
    void tests() throws Exception
    {
        assertThat(check("SELECT ?i WHERE { ?i ex:kind ?k MINUS { ?k ex:follows ?n } }"),
                containsInAnyOrder(row(item(14))));

        assertThat(check("SELECT ?i WHERE { ?i ex:kind ?k FILTER EXISTS { ?k ex:follows ?n } }"),
                containsInAnyOrder(row(item(10)), row(item(11)), row(item(12)), row(item(13))));

        assertThat(check("SELECT ?i WHERE { ?i ex:kind ?k FILTER NOT EXISTS { ?n ex:follows ?k } }"),
                containsInAnyOrder(row(item(10)), row(item(11)), row(item(14))));

        assertThat(check("SELECT ?i WHERE { ?i ex:kind ?k MINUS { ?j ex:other ?k . ?k ex:follows ?n } }"),
                containsInAnyOrder(row(item(14))));
    }


    @Test
    @DisplayName("the sides of a MINUS are compared by the nodes of the variables they share")
    void minusByNodes() throws Exception
    {
        String kindTable = "\"join_node_test\".\"kind\"";

        // the kind following another and the kind another follows are given by their numbers on both sides, so the
        // sides are compared by the numbers, and the table of the kinds, which gives the names, is read for the result
        // only
        String query = "SELECT ?a WHERE { ?a ex:follows ?k MINUS { ?k ex:follows ?k } }";

        assertThat(count(translate(shared, query), kindTable), is(1));
        assertThat(count(translate(optimized, query), kindTable), is(1));
        assertThat(count(translate(plain, query), kindTable), is(4));
        assertThat(check(query), containsInAnyOrder(row(kind("red"))));

        // a kind the query asks for is read for the solutions that are left
        String needed = "SELECT ?a ?k WHERE { ?a ex:follows ?k MINUS { ?k ex:follows ?k } }";

        assertThat(count(translate(shared, needed), kindTable), is(2));
        assertThat(check(needed), containsInAnyOrder(row(kind("red"), kind("green"))));

        // two differences of one group, and a difference inside another one
        String two = "SELECT ?a WHERE { ?a ex:follows ?k . ?k ex:follows ?n MINUS { ?k ex:follows ?k } "
                + "MINUS { ?n ex:follows ?m . ?m ex:follows ?m } }";

        assertThat(count(translate(shared, two), kindTable), is(1));
        check(two);

        String nested = "SELECT ?a WHERE { ?a ex:follows ?k MINUS { ?k ex:follows ?n MINUS { ?n ex:follows ?n } } }";

        assertThat(count(translate(shared, nested), kindTable), is(1));
        check(nested);

        // a node named after another table of its mapping is not shared, and the sides are compared by the terms
        check("SELECT ?i WHERE { ?i ex:kind ?k MINUS { ?k ex:follows ?n } }");

        // a variable the left side may leave unbound is compared as before
        check("SELECT ?i WHERE { ?i ex:kind ?k OPTIONAL { ?i ex:other ?o } MINUS { ?o ex:follows ?n } }");

        // and so is one the right side binds in a group of its own
        check("SELECT ?i WHERE { ?i ex:kind ?k MINUS { { ?k ex:follows ?n } ?k ex:label ?l } }");
    }


    @Test
    @DisplayName("the sides of an OPTIONAL are compared by the nodes of the variables they share")
    void optionalByNodes() throws Exception
    {
        String kindTable = "\"join_node_test\".\"kind\"";

        // the kind another follows and the kind others follow are given by their numbers on both sides, so the sides
        // are compared by the numbers, and the table of the kinds is read only for the names the query asks for
        String query = "SELECT ?a ?n WHERE { ?a ex:follows ?k OPTIONAL { ?k ex:follows ?n } }";

        assertThat(count(translate(shared, query), kindTable), is(2));
        assertThat(count(translate(optimized, query), kindTable), is(2));
        assertThat(count(translate(plain, query), kindTable), is(4));
        assertThat(check(query), hasSize(4));

        // a shared kind the query asks for is read on the left side, which every solution has
        String needed = "SELECT ?a ?k ?n WHERE { ?a ex:follows ?k OPTIONAL { ?k ex:follows ?n } }";

        assertThat(count(translate(shared, needed), kindTable), is(3));
        assertThat(check(needed), hasSize(4));

        // and so is one a condition of the OPTIONAL refers to
        check("SELECT ?a ?n WHERE { ?a ex:follows ?k OPTIONAL { ?k ex:follows ?n FILTER(?k != kind:green) } }");
        check("SELECT ?a ?n WHERE { ?a ex:follows ?k OPTIONAL { ?k ex:follows ?n FILTER(?k != ?n) } }");

        // an OPTIONAL inside another one, and one after another
        String nested = "SELECT ?a ?m WHERE { ?a ex:follows ?k OPTIONAL { ?k ex:follows ?n "
                + "OPTIONAL { ?n ex:follows ?m } } }";

        assertThat(count(translate(shared, nested), kindTable), is(2));
        check(nested);

        check("SELECT ?a ?n ?m WHERE { ?a ex:follows ?k OPTIONAL { ?k ex:follows ?n } OPTIONAL { ?m ex:follows ?k } }");

        // a variable the left side may leave unbound is compared as before
        check("SELECT ?i ?n WHERE { ?i ex:kind ?x OPTIONAL { ?i ex:other ?o } OPTIONAL { ?o ex:follows ?n } }");

        // the right side of a difference shares the nodes of an OPTIONAL before it
        check("SELECT ?a ?n WHERE { ?a ex:follows ?k OPTIONAL { ?k ex:follows ?n } MINUS { ?n ex:follows ?n } }");
    }


    @Test
    @DisplayName("the pattern of an EXISTS is compared by the nodes it shares with the solutions it is tested for")
    void existsByNodes() throws Exception
    {
        String kindTable = "\"join_node_test\".\"kind\"";

        // the kind another follows is given by its number in the solutions of the group and in those of the pattern, so
        // the test compares the numbers, and the table of the kinds is read for the result only
        for(String test : List.of("EXISTS", "NOT EXISTS"))
        {
            String query = "SELECT ?a WHERE { ?a ex:follows ?k FILTER " + test + " { ?k ex:follows ?n } }";

            assertThat(count(translate(shared, query), kindTable), is(1));
            assertThat(count(translate(optimized, query), kindTable), is(1));
            assertThat(count(translate(plain, query), kindTable) > 1, is(true));
            check(query);
        }

        // a kind the query asks for is read for the solutions that pass the test
        String needed = "SELECT ?a ?k WHERE { ?a ex:follows ?k FILTER EXISTS { ?k ex:follows ?n } }";

        assertThat(count(translate(shared, needed), kindTable), is(2));
        assertThat(check(needed), hasSize(4));

        // a pattern of several triple patterns, and two tests of one filter
        String chain = "SELECT ?a WHERE { ?a ex:follows ?k FILTER EXISTS { ?k ex:follows ?n . ?n ex:follows ?m } }";

        assertThat(count(translate(shared, chain), kindTable), is(1));
        check(chain);

        String two = "SELECT ?a WHERE { ?a ex:follows ?k FILTER(EXISTS { ?k ex:follows ?n } "
                + "&& NOT EXISTS { ?k ex:follows ?k }) }";

        assertThat(count(translate(shared, two), kindTable), is(1));
        check(two);

        // a pattern with another member than triple patterns is compared by the variables as before
        check("SELECT ?a WHERE { ?a ex:follows ?k FILTER EXISTS { ?k ex:follows ?n OPTIONAL { ?n ex:follows ?m } } }");
        check("SELECT ?a WHERE { ?a ex:follows ?k FILTER EXISTS { ?k ex:follows+ ?n } }");
        check("SELECT ?a WHERE { ?a ex:follows ?k FILTER EXISTS { ?k ex:follows ?n FILTER(?n != ?a) } }");

        // and so is a test outside the filters of a group
        check("SELECT ?a ?e WHERE { ?a ex:follows ?k BIND(EXISTS { ?k ex:follows ?n } AS ?e) }");
        check("SELECT ?a ?n WHERE { ?a ex:follows ?k OPTIONAL { ?k ex:follows ?n "
                + "FILTER EXISTS { ?n ex:follows ?n } } }");

        // a test of a group inside the pattern of another one
        check("SELECT ?a WHERE { ?a ex:follows ?k FILTER EXISTS { ?k ex:follows ?n "
                + "FILTER EXISTS { ?n ex:follows ?m } } }");
    }


    @Test
    @DisplayName("a shared variable bound by the branches of a union")
    void union() throws Exception
    {
        assertThat(check("SELECT ?i ?k ?n WHERE { { ?i ex:kind ?k } UNION { ?i ex:other ?k } ?k ex:follows ?n }"),
                hasSize(10));

        // a branch binding the variable without reading the table of the kinds
        assertThat(check("SELECT ?i ?k ?n WHERE { ?i ex:kind ?k { { ?k ex:follows ?n } UNION "
                + "{ VALUES ?k { kind:grey kind:red } } } }"), hasSize(9));

        assertThat(check("SELECT ?k ?n WHERE { { ?k ex:follows ?n } UNION { ?n ex:follows ?k } ?i ex:other ?k }"),
                hasSize(8));
    }


    @Test
    @DisplayName("a shared variable across a sub-select, a BIND and inline data")
    void scopes() throws Exception
    {
        assertThat(check("SELECT ?i ?c WHERE { ?i ex:kind ?k { SELECT ?k (COUNT(?n) AS ?c) "
                + "WHERE { ?k ex:follows ?n } GROUP BY ?k } }"), hasSize(4));

        assertThat(check("SELECT ?i ?n WHERE { ?i ex:kind ?k BIND(?k AS ?m) ?m ex:follows ?n }"), hasSize(6));

        assertThat(check("SELECT ?i ?n WHERE { VALUES ?k { kind:red kind:grey } ?i ex:kind ?k . ?k ex:follows ?n }"),
                hasSize(4));

        assertThat(check("SELECT ?a ?b WHERE { ?a ex:follows ?x . ?b ex:follows ?y FILTER(?x = ?y) }"), hasSize(10));

        assertThat(check("SELECT ?i ?n WHERE { GRAPH ?g { ?i ex:kind ?k } ?k ex:follows ?n }"), hasSize(6));
    }


    @Test
    @DisplayName("a shared variable under an aggregation and a deduplication")
    void aggregation() throws Exception
    {
        assertThat(check("SELECT ?k (COUNT(*) AS ?c) WHERE { ?i ex:kind ?k . ?k ex:follows ?n } GROUP BY ?k"),
                hasSize(3));

        assertThat(check("SELECT DISTINCT ?k WHERE { ?i ex:kind ?k . ?k ex:follows ?n }"),
                containsInAnyOrder(row(kind("red")), row(kind("green")), row(kind("blue"))));

        assertThat(check("SELECT (COUNT(*) AS ?c) WHERE { ?a ex:follows ?x . ?x ex:follows ?b }"), not(empty()));
    }


    @Test
    @DisplayName("a walk seeded by a table of a join mapping whose node the other table still needs")
    void seededWalk() throws Exception
    {
        // the table of the kinds is distinct over the start of the walk, so it seeds the walk, whereas the table of
        // the items stays outside and is joined with the walk by the node joining the two tables
        assertThat(check("SELECT ?i ?n WHERE { ?i ex:kind ?k . ?k ex:follows* ?n }"),
                containsInAnyOrder(row(item(10), kind("red")), row(item(10), kind("green")),
                        row(item(10), kind("blue")), row(item(11), kind("red")), row(item(11), kind("green")),
                        row(item(11), kind("blue")), row(item(12), kind("green")), row(item(12), kind("blue")),
                        row(item(13), kind("blue")), row(item(14), kind("grey"))));

        assertThat(check("SELECT ?i ?n WHERE { ?i ex:kind ?k . ?n ex:follows* ?k }"), hasSize(8));

        assertThat(check("SELECT ?i ?k ?n WHERE { ?i ex:other ?k . ?k ex:follows* ?n }"), hasSize(6));

        assertThat(check("SELECT ?n WHERE { item:12 ex:kind ?k . ?k ex:follows* ?n }"),
                containsInAnyOrder(row(kind("green")), row(kind("blue"))));

        assertThat(check("SELECT ?i ?j WHERE { ?i ex:kind ?k . ?k ex:follows* ?n . ?j ex:other ?n }"), hasSize(9));

        assertThat(check("SELECT ?i ?n WHERE { ?i ex:kind ?k . ?k ex:follows? ?n }"), hasSize(10));

        assertThat(check("SELECT ?i ?n WHERE { ?i ex:kind ?k . ?k ex:follows+ ?n }"), hasSize(6));
    }


    @Test
    @DisplayName("a shared variable at an end of a property path")
    void paths() throws Exception
    {
        assertThat(check("SELECT ?a ?b WHERE { ?a ex:follows+ ?b }"), hasSize(4));

        assertThat(check("SELECT ?i ?n WHERE { ?i ex:kind/ex:follows ?n }"), hasSize(6));

        assertThat(check("SELECT ?a ?b WHERE { ?a ex:follows* ?b }"), hasSize(15));

        assertThat(check("SELECT ?i ?n WHERE { ?i ex:kind ?k . ?n ex:follows+ ?k }"), hasSize(4));

        assertThat(check("SELECT ?a ?b WHERE { ?a ex:follows ?x . ?x (ex:follows|^ex:follows) ?b }"), hasSize(14));
    }
}
