package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT8;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.database.SourceTable;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.imcode.SqlSelect;
import cz.iocb.sparql.engine.mapping.JoinTableQuadMapping.JoinColumns;
import cz.iocb.sparql.engine.mapping.TermMapping;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Request.PreparedQuery;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.translator.TranslateVisitor;
import cz.iocb.sparql.testing.Database;



/**
 * Join mappings whose tables are joined as a tree: a table of triples holding the numbers of their terms is joined to
 * the table numbering the terms three times, for the subject, for the predicate and for the object, so the predicate is
 * a column of a joined table too. The solutions have to be those of a table holding the same triples in boxes.
 *
 * The triples say that a knows b and c, that b knows c, that a, b and c have the names {@code "A"}, {@code "B"} and
 * {@code "C"}, and that c has the age 5; the table of the terms holds one term more, which no triple uses.
 */
public class JoinTreeMappingTest
{
    /**
     * Prefix declaration prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> ";

    /**
     * The table of the terms as the generated code names it.
     */
    private static final String termTable = "\"join_tree_test\".\"term\"";

    /**
     * Pool of the test database.
     */
    private static DataSource connectionPool = null;

    /**
     * Schema of the test database.
     */
    private static DatabaseSchema schema = null;

    /**
     * Configuration mapping the triples kept in boxes.
     */
    private static SparqlDatabaseConfiguration boxed = null;

    /**
     * Configuration mapping the numbered triples.
     */
    private static SparqlDatabaseConfiguration tree = null;

    /**
     * Configuration mapping the numbered triples, the table of the terms being a dictionary table.
     */
    private static SparqlDatabaseConfiguration dictionary = null;

    /**
     * Configuration mapping the numbered triples, the table of the terms being a dictionary table, with the join
     * optimizer on.
     */
    private static SparqlDatabaseConfiguration optimized = null;


    /**
     * Creates the {@code join_tree_test} schema with the table of the terms, the table of the numbered triples and the
     * table of the same triples in boxes, and the configurations mapping them.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema join_tree_test");

            statement.execute(
                    "create table join_tree_test.term (id int8 primary key, term sparql.rdfbox not null unique)");
            statement.execute("""
                    insert into join_tree_test.term values (1, '<http://example.org/a>'), \
                    (2, '<http://example.org/b>'), (3, '<http://example.org/c>'), \
                    (4, '<http://example.org/knows>'), (5, '<http://example.org/name>'), \
                    (6, '<http://example.org/age>'), (7, '"A"'), (8, '"B"'), (9, '"C"'), \
                    (10, '"5"^^<http://www.w3.org/2001/XMLSchema#integer>'), (11, '<http://example.org/unused>')""");

            statement.execute("""
                    create table join_tree_test.triple (subject int8 not null references join_tree_test.term(id), \
                    predicate int8 not null references join_tree_test.term(id), \
                    object int8 not null references join_tree_test.term(id), \
                    primary key (predicate, subject, object))""");
            statement.execute("""
                    insert into join_tree_test.triple values (1, 4, 2), (1, 4, 3), (2, 4, 3), (1, 5, 7), (2, 5, 8), \
                    (3, 5, 9), (3, 6, 10)""");

            statement.execute("""
                    create table join_tree_test.boxed as select s.term as s, p.term as p, o.term as o \
                    from join_tree_test.triple t join join_tree_test.term s on s.id = t.subject \
                    join join_tree_test.term p on p.id = t.predicate join join_tree_test.term o on o.id = t.object""");
        }

        schema = new DatabaseSchema(connectionPool);

        boxed = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);
        boxed.addPrefix("ex", "http://example.org/");
        boxed.addQuadMapping(new DatabaseTable("join_tree_test", "boxed"), null, boxed.createTermMapping(box, "s"),
                boxed.createTermMapping(box, "p"), boxed.createTermMapping(box, "o"));

        tree = configure(false);
        dictionary = configure(true);

        optimized = configure(true);
        optimized.setEngineSettings(optimized.getEngineSettings().withJoinOptimizer(JoinOptimizerMode.SETTINGS));
    }


    /**
     * Creates a configuration mapping the numbered triples: the table of the triples is joined to the table of the
     * terms by its subject, by its predicate and by its object, and the three terms are taken from the joined tables.
     *
     * @param declared whether the table of the terms is declared to be a dictionary table
     * @return the configuration
     * @throws SQLException on database errors
     */
    private static SparqlDatabaseConfiguration configure(boolean declared) throws SQLException
    {
        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);

        config.addPrefix("ex", "http://example.org/");

        DatabaseTable triple = new DatabaseTable("join_tree_test", "triple");
        DatabaseTable term = new DatabaseTable("join_tree_test", "term");
        TableColumn id = new TableColumn("id", INT8);
        TermMapping mapping = config.createTermMapping(box, "term");

        config.addQuadMapping(asList(triple, term, term, term),
                asList(new JoinColumns(0, new TableColumn("subject", INT8), 1, id),
                        new JoinColumns(0, new TableColumn("predicate", INT8), 2, id),
                        new JoinColumns(0, new TableColumn("object", INT8), 3, id)),
                0, null, 1, mapping, 2, mapping, 3, mapping);

        if(declared)
            config.addDictionaryTable(term);

        return config;
    }


    /**
     * Runs the query over the configuration and returns all rows.
     *
     * @param config the configuration
     * @param query the query, without the prefix declaration
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
     * @param query the query, without the prefix declaration
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
     * Checks that the query gives the solutions of the triples kept in boxes over every configuration mapping the
     * numbered triples.
     *
     * @param query the query, without the prefix declaration
     * @return the solutions
     * @throws Exception when the query fails
     */
    private static List<List<RdfTerm>> check(String query) throws Exception
    {
        List<List<RdfTerm>> expected = execute(boxed, query);

        for(SparqlDatabaseConfiguration config : List.of(tree, dictionary, optimized))
            assertThat(execute(config, query), containsInAnyOrder(expected.toArray()));

        return expected;
    }


    /**
     * Number of the accesses to the table of the terms in the generated code.
     *
     * @param sql the generated SQL
     * @return number of the accesses to the table of the terms
     */
    private static int terms(String sql)
    {
        return sql.split(termTable, -1).length - 1;
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


    @Test
    @DisplayName("a constant predicate selects the triples by its number")
    void constantPredicate() throws Exception
    {
        String query = "SELECT ?s ?o WHERE { ?s ex:knows ?o }";

        assertThat(check(query),
                containsInAnyOrder(row(iri("a"), iri("b")), row(iri("a"), iri("c")), row(iri("b"), iri("c"))));

        assertThat(translate(dictionary, query), equality("predicate", "'4'::int8"));
        assertThat(terms(translate(dictionary, query)), is(2));
        assertThat(terms(translate(tree, query)), is(3));

        String count = "SELECT (COUNT(*) AS ?c) WHERE { ?s ex:knows ?o }";

        assertThat(check(count), hasSize(1));
        assertThat(translate(dictionary, count), not(containsString(termTable)));

        assertThat(check("SELECT ?s ?o WHERE { ?s ex:unknown ?o }"), empty());
        assertThat(check("SELECT ?s ?o WHERE { ?s ex:unused ?o }"), empty());
    }


    @Test
    @DisplayName("a variable predicate is read from the table of the terms")
    void variablePredicate() throws Exception
    {
        assertThat(check("SELECT ?s ?p ?o WHERE { ?s ?p ?o }"), hasSize(7));

        assertThat(check("SELECT ?p WHERE { ex:a ?p ex:b }"), containsInAnyOrder(row(iri("knows"))));

        assertThat(check("SELECT ?p (COUNT(*) AS ?c) WHERE { ?s ?p ?o } GROUP BY ?p"), hasSize(3));

        assertThat(check("SELECT ?s ?o WHERE { VALUES ?p { ex:knows ex:age ex:unknown } ?s ?p ?o }"), hasSize(4));

        assertThat(check("SELECT ?s ?o WHERE { ?s ?p ?o FILTER(?p = ex:name) }"), hasSize(3));

        assertThat(check("SELECT ?s ?p WHERE { ?s ?p ?p }"), empty());

        assertThat(check("SELECT ?x ?p WHERE { ex:a ?p ?x . ?x ?p ex:c }"),
                containsInAnyOrder(row(iri("b"), iri("knows"))));
    }


    @Test
    @DisplayName("constant subjects and objects")
    void constantTerms() throws Exception
    {
        assertThat(check("SELECT ?s WHERE { ?s ex:name \"B\" }"), containsInAnyOrder(row(iri("b"))));
        assertThat(check("SELECT ?s WHERE { ?s ex:age 5 }"), containsInAnyOrder(row(iri("c"))));
        assertThat(check("SELECT ?s WHERE { ?s ex:age 6 }"), empty());
        assertThat(check("SELECT ?o WHERE { ex:a ex:knows ?o }"), containsInAnyOrder(row(iri("b")), row(iri("c"))));
        assertThat(check("SELECT ?x WHERE { ex:a ex:knows ex:b BIND(1 AS ?x) }"), hasSize(1));
        assertThat(check("SELECT ?x WHERE { ex:b ex:knows ex:a BIND(1 AS ?x) }"), empty());

        String query = "SELECT (COUNT(*) AS ?c) WHERE { ex:a ex:knows ex:b }";

        assertThat(check(query), hasSize(1));
        assertThat(translate(dictionary, query), not(containsString(termTable)));
    }


    @Test
    @DisplayName("patterns sharing a variable join the triples by its number")
    void joinedPatterns() throws Exception
    {
        String query = "SELECT ?n WHERE { ex:a ex:knows ?x . ?x ex:name ?n }";

        assertThat(check(query), hasSize(2));
        assertThat(terms(translate(dictionary, query)), is(1));

        assertThat(check("SELECT ?x ?y ?n WHERE { ?x ex:knows ?y . ?y ex:knows ?z . ?z ex:name ?n }"), hasSize(1));

        assertThat(check("SELECT ?s ?n ?a WHERE { ?s ex:name ?n OPTIONAL { ?s ex:age ?a } }"), hasSize(3));

        assertThat(check("SELECT ?s WHERE { ?s ex:knows ?o FILTER NOT EXISTS { ?o ex:knows ?z } }"), hasSize(2));

        assertThat(check("SELECT ?s WHERE { ?s ex:name ?n MINUS { ?s ex:knows ?o } }"),
                containsInAnyOrder(row(iri("c"))));

        assertThat(check("SELECT ?s (COUNT(?o) AS ?c) WHERE { ?s ex:knows ?o } GROUP BY ?s"), hasSize(2));

        assertThat(check("SELECT ?s WHERE { ?s ex:age ?a FILTER(?a > 3) }"), containsInAnyOrder(row(iri("c"))));

        assertThat(check("SELECT ?s ?n WHERE { ?s ex:name ?n } ORDER BY DESC(?n) LIMIT 2"), hasSize(2));
    }


    @Test
    @DisplayName("property paths over the numbered triples")
    void paths() throws Exception
    {
        assertThat(check("SELECT ?x WHERE { ex:a ex:knows+ ?x }"), containsInAnyOrder(row(iri("b")), row(iri("c"))));

        assertThat(check("SELECT ?x ?y WHERE { ?x ex:knows* ?y }"), hasSize(10));

        assertThat(check("SELECT ?x ?n WHERE { ex:a ex:knows* ?x . ?x ex:name ?n }"), hasSize(3));

        assertThat(check("SELECT ?n WHERE { ex:a ex:knows/ex:name ?n }"), hasSize(2));

        assertThat(check("SELECT ?x WHERE { ex:c ^ex:knows ?x }"), containsInAnyOrder(row(iri("a")), row(iri("b"))));

        assertThat(check("SELECT ?s ?o WHERE { ?s !ex:knows ?o }"), hasSize(4));

        assertThat(check("SELECT ?s ?o WHERE { ?s (ex:name|ex:age) ?o }"), hasSize(4));

        assertThat(check("SELECT ?x ?y WHERE { ?x ex:knows? ?y }"), hasSize(10));
    }


    @Test
    @DisplayName("joins that do not connect the tables as a tree are refused")
    void refusedJoins() throws Exception
    {
        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);

        SourceTable triple = new DatabaseTable("join_tree_test", "triple");
        SourceTable term = new DatabaseTable("join_tree_test", "term");
        TableColumn id = new TableColumn("id", INT8);
        TableColumn subject = new TableColumn("subject", INT8);
        TableColumn object = new TableColumn("object", INT8);
        TermMapping mapping = config.createTermMapping(box, "term");
        TermMapping predicate = config.createIriMapping("<http://example.org/p>");

        // a table no join reaches
        assertThrows(IllegalArgumentException.class,
                () -> config.addQuadMapping(asList(triple, term, term),
                        asList(new JoinColumns(0, subject, 1, id), new JoinColumns(1, id, 0, object)), 0, null, 1,
                        mapping, 0, predicate, 2, mapping));

        // a join fewer than the tables need
        assertThrows(IllegalArgumentException.class, () -> config.addQuadMapping(asList(triple, term, term),
                asList(new JoinColumns(0, subject, 1, id)), 0, null, 1, mapping, 0, predicate, 2, mapping));

        // a join of a table the mapping does not have, and a join of a table with itself
        assertThrows(IllegalArgumentException.class,
                () -> config.addQuadMapping(asList(triple, term, term),
                        asList(new JoinColumns(0, subject, 1, id), new JoinColumns(0, object, 3, id)), 0, null, 1,
                        mapping, 0, predicate, 2, mapping));

        assertThrows(IllegalArgumentException.class,
                () -> config.addQuadMapping(asList(triple, term, term),
                        asList(new JoinColumns(0, subject, 1, id), new JoinColumns(2, id, 2, id)), 0, null, 1, mapping,
                        0, predicate, 2, mapping));

        // a term taken from a table the mapping does not have
        assertThrows(IllegalArgumentException.class,
                () -> config.addQuadMapping(asList(triple, term, term),
                        asList(new JoinColumns(0, subject, 1, id), new JoinColumns(0, object, 2, id)), 0, null, 1,
                        mapping, 0, predicate, 3, mapping));

        // the joins of adjacent tables need not name them
        config.addQuadMapping(asList(term, triple, term),
                asList(new JoinColumns(id, subject), new JoinColumns(object, id)), null, mapping, predicate, mapping);
    }
}
