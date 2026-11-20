package cz.iocb.sparql.engine.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
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
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.imcode.SqlSelect;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Request.PreparedQuery;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.test.SparqlTest.Quad;
import cz.iocb.sparql.engine.translator.TranslateVisitor;
import cz.iocb.sparql.testing.Database;



/**
 * Quads stored as {@link MixedQuadTable} keeps them, with only the IRIs and the blank nodes numbered: the boxes of the
 * numbered terms and the boxes of the other objects are mapped by disjoint classes derived from the box, so a triple
 * pattern reads only the table of quads that can hold its object, whether a constant, a join or a filter tells what the
 * object is, and a variable shared by two objects is compared by numbers among the resources and by boxes among the
 * other terms. With the tables of the quads declared to be probed tables, a pattern of a predicate that has no quad in
 * one of them reads only the other one. The data: {@code ex:a} knows {@code ex:b} and a blank node, {@code ex:b} knows
 * {@code ex:a}, the three have names, {@code ex:a} and {@code ex:b} ages, {@code ex:a} says a triple term and likes
 * both {@code ex:b} and a literal, and the graph {@code ex:g} holds an acquaintance and a name of {@code ex:c}.
 */
public class MixedQuadTableTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> ";

    /**
     * The table of the quads whose objects are numbered as the generated code names it.
     */
    private static final String resourceQuads = "\"sparql_test\".\"resource_quads\"";

    /**
     * The table of the quads whose objects are kept in boxes as the generated code names it.
     */
    private static final String valueQuads = "\"sparql_test\".\"value_quads\"";

    /**
     * Configuration leaving the table of the resources a table like any other.
     */
    private static SparqlDatabaseConfiguration plain = null;

    /**
     * Configuration declaring the table of the resources a dictionary table and the tables of the quads probed tables,
     * with the join optimizer on.
     */
    private static SparqlDatabaseConfiguration dictionary = null;


    /**
     * Creates the tables unless they exist, stores the data and makes the two configurations.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema if not exists sparql_test");
            MixedQuadTable.create(statement);
        }

        DatabaseSchema schema = new DatabaseSchema(connectionPool);

        plain = new SparqlDatabaseConfiguration(null, connectionPool, schema, false);
        MixedQuadTable.map(plain, MixedQuadTable.Variant.PLAIN);

        dictionary = new SparqlDatabaseConfiguration(null, connectionPool, schema, false);
        MixedQuadTable.map(dictionary, MixedQuadTable.Variant.DICTIONARY);

        Node a = iri("a");
        Node b = iri("b");
        Node c = iri("c");
        Node x = NodeFactory.createBlankNode("x");
        Node knows = iri("knows");
        Node name = iri("name");
        Node age = iri("age");
        Node twentyFive = NodeFactory.createLiteralDT("25", XSDDatatype.XSDinteger);

        List<Quad> quads = new ArrayList<>();
        quads.add(new Quad(null, a, knows, b));
        quads.add(new Quad(null, a, knows, x));
        quads.add(new Quad(null, a, name, NodeFactory.createLiteralString("A")));
        quads.add(new Quad(null, a, age, NodeFactory.createLiteralDT("30", XSDDatatype.XSDinteger)));
        quads.add(new Quad(null, b, knows, a));
        quads.add(new Quad(null, b, name, NodeFactory.createLiteralLang("B", "en")));
        quads.add(new Quad(null, b, age, twentyFive));
        quads.add(new Quad(null, x, name, NodeFactory.createLiteralString("X")));
        quads.add(new Quad(null, a, iri("says"), NodeFactory.createTripleTerm(b, age, twentyFive)));
        quads.add(new Quad(null, a, iri("likes"), b));
        quads.add(new Quad(null, a, iri("likes"), NodeFactory.createLiteralString("tea")));
        quads.add(new Quad(iri("g"), c, knows, a));
        quads.add(new Quad(iri("g"), c, name, NodeFactory.createLiteralString("C")));

        try(Request request = new Engine(plain).getRequest())
        {
            MixedQuadTable.store(request, quads);
        }
    }


    /**
     * Returns the node of an IRI of the example namespace.
     */
    private static Node iri(String name)
    {
        return NodeFactory.createURI("http://example.org/" + name);
    }


    /**
     * Runs the query over the configuration and returns all rows.
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
     * Returns the solutions of the query, which both configurations have to give alike.
     */
    private static List<List<RdfTerm>> check(String query) throws Exception
    {
        List<List<RdfTerm>> expected = execute(plain, query);
        List<List<RdfTerm>> actual = execute(dictionary, query);

        assertThat(actual, containsInAnyOrder(expected.toArray()));

        return actual;
    }


    /**
     * Checks that the query reads only one of the two tables of quads, over both configurations.
     */
    private static void checkTable(String query, String read, String skipped) throws Exception
    {
        for(SparqlDatabaseConfiguration config : List.of(plain, dictionary))
        {
            String sql = translate(config, query);

            assertThat(sql, containsString(read));
            assertThat(sql, not(containsString(skipped)));
        }
    }


    /**
     * Returns how many times the SQL code holds the text.
     */
    private static int count(String sql, String text)
    {
        return sql.split(Pattern.quote(text), -1).length - 1;
    }


    @Test
    void constantObject() throws Exception
    {
        // an IRI is the object of the numbered quads only
        String resource = "SELECT ?s WHERE { ?s ex:knows ex:b }";

        checkTable(resource, resourceQuads, valueQuads);
        assertThat(check(resource), hasSize(1));

        // its number is read when the query is translated, so the table of the resources is read for the subject only
        String sql = translate(dictionary, resource);
        assertThat(sql, not(containsString("rdfbox_create_from_iri")));
        assertThat(sql, containsString("::int8 = \"o\""));
        assertThat(sql, containsString("::int8 = \"p\""));

        // a literal and a triple term are objects of the other quads only, where they are kept as they are
        String literal = "SELECT ?s WHERE { ?s ex:name \"A\" }";

        checkTable(literal, valueQuads, resourceQuads);
        assertThat(translate(dictionary, literal),
                containsString("\"o\" operator(sparql.@=) sparql.rdfbox_create_from_string('A'::varchar)"));
        assertThat(check(literal), hasSize(1));

        String triple = "SELECT ?s WHERE { ?s ex:says <<( ex:b ex:age 25 )>> }";

        checkTable(triple, valueQuads, resourceQuads);
        assertThat(check(triple), hasSize(1));

        // a variable may be either
        String variable = "SELECT ?s ?o WHERE { ?s ex:says ?o }";

        assertThat(translate(plain, variable), containsString(resourceQuads));
        assertThat(translate(plain, variable), containsString(valueQuads));
        assertThat(check(variable), hasSize(1));

        assertThat(check("SELECT ?s ?p ?o WHERE { ?s ?p ?o }"), hasSize(11));
    }


    @Test
    void unknownConstant() throws Exception
    {
        // a resource that has no number is in no quad
        String resource = "SELECT ?s WHERE { ?s ex:knows ex:nobody }";

        assertThat(translate(dictionary, resource), not(containsString("sparql_test")));
        assertThat(check(resource), empty());

        // the literals have no numbers, so their quads are read
        String literal = "SELECT ?s WHERE { ?s ex:name \"Nobody\" }";

        assertThat(translate(dictionary, literal), containsString(valueQuads));
        assertThat(check(literal), empty());
    }


    @Test
    void objectAsSubject() throws Exception
    {
        // the object of the first pattern is the subject of the second one, so it is a resource: the other quads are
        // read for the second pattern only, and the two patterns are joined by the numbers
        String query = "SELECT ?s ?x WHERE { ?s ex:likes ?o . ?o ex:likes ?x }";

        for(SparqlDatabaseConfiguration config : List.of(plain, dictionary))
        {
            String sql = translate(config, query);

            assertThat(count(sql, valueQuads), is(1));
            assertThat(count(sql, " UNION ALL "), is(1));
        }

        assertThat(translate(dictionary, query), containsString("\".\"s\" = \""));
        assertThat(check(query), empty());

        assertThat(check("SELECT ?s ?x WHERE { ?s ex:knows ?o . ?o ex:name ?x }"), hasSize(3));
    }


    @Test
    void sharedObject() throws Exception
    {
        // an object shared by two patterns is a resource in both or another term in both: the numbers are compared in
        // one branch and the boxes in the other one
        String query = "SELECT ?a ?b WHERE { ?a ex:likes ?n . ?b ex:likes ?n }";

        for(SparqlDatabaseConfiguration config : List.of(plain, dictionary))
        {
            String sql = translate(config, query);

            assertThat(count(sql, " UNION ALL "), is(1));
            assertThat(count(sql, resourceQuads), is(2));
            assertThat(count(sql, valueQuads), is(2));
            assertThat(sql, containsString("\".\"o\" = \""));
            assertThat(sql, containsString("\".\"o\" operator(sparql.@=) \""));
        }

        assertThat(check(query), hasSize(2));
    }


    @Test
    void filteredObject() throws Exception
    {
        // what a filter requires of the object tells its table as well: a test of its kind
        checkTable("SELECT ?s ?o WHERE { ?s ?p ?o FILTER(isLiteral(?o)) }", valueQuads, resourceQuads);
        checkTable("SELECT ?s ?o WHERE { ?s ?p ?o FILTER(isTriple(?o)) }", valueQuads, resourceQuads);
        checkTable("SELECT ?s ?o WHERE { ?s ?p ?o FILTER(isIRI(?o)) }", resourceQuads, valueQuads);
        checkTable("SELECT ?s ?o WHERE { ?s ?p ?o FILTER(isBlank(?o)) }", resourceQuads, valueQuads);

        assertThat(check("SELECT ?s ?o WHERE { ?s ?p ?o FILTER(isLiteral(?o)) }"), hasSize(6));
        assertThat(check("SELECT ?s ?o WHERE { ?s ?p ?o FILTER(isTriple(?o)) }"), hasSize(1));
        assertThat(check("SELECT ?s ?o WHERE { ?s ?p ?o FILTER(isIRI(?o)) }"), hasSize(3));
        assertThat(check("SELECT ?s ?o WHERE { ?s ?p ?o FILTER(isBlank(?o)) }"), hasSize(1));

        // a comparison of its value, which no resource has
        String value = "SELECT ?s WHERE { ?s ex:age ?a FILTER(?a > 26) }";

        checkTable(value, valueQuads, resourceQuads);
        assertThat(check(value), hasSize(1));

        // and the term it has to be
        String resource = "SELECT ?s ?o WHERE { ?s ?p ?o FILTER(?o = ex:b) }";

        checkTable(resource, resourceQuads, valueQuads);
        assertThat(check(resource), hasSize(2));

        String literal = "SELECT ?s ?o WHERE { ?s ?p ?o FILTER(?o = \"A\") }";

        checkTable(literal, valueQuads, resourceQuads);
        assertThat(check(literal), hasSize(1));
    }


    @Test
    void probedTables() throws Exception
    {
        // no name is a resource, which the table of the numbered quads tells when it is asked for a quad of the
        // predicate, so a pattern of a name reads the other table only
        String name = "SELECT ?s ?n WHERE { ?s ex:name ?n }";

        assertThat(translate(dictionary, name), not(containsString(resourceQuads)));
        assertThat(check(name), hasSize(3));

        // the tables are asked by the numbers of the constants, which the plain configuration does not read
        assertThat(translate(plain, name), containsString(resourceQuads));

        // so a join of such patterns has no branch for every combination of the tables
        String join = "SELECT * WHERE { ?a ex:name ?n . ?a ex:age ?g . ?a ex:knows ?k . ?k ex:name ?m }";

        assertThat(count(translate(dictionary, join), " UNION ALL "), is(0));
        assertThat(count(translate(plain, join), " UNION ALL "), is(7));
        assertThat(check(join), hasSize(3));

        // a predicate with objects of both kinds keeps both tables
        String likes = "SELECT ?s ?o WHERE { ?s ex:likes ?o }";

        assertThat(translate(dictionary, likes), containsString(resourceQuads));
        assertThat(translate(dictionary, likes), containsString(valueQuads));
        assertThat(check(likes), hasSize(2));

        // a constant object is asked about together with the predicate: nobody is known to know ex:c
        String nobody = "SELECT ?s WHERE { ?s ex:knows ex:c }";

        assertThat(translate(dictionary, nobody), not(containsString("sparql_test")));
        assertThat(check(nobody), empty());

        // what the index of a table does not begin with is not asked about, so its quads are read
        String literal = "SELECT ?s WHERE { ?s ex:name \"Nobody\" }";

        assertThat(translate(dictionary, literal), containsString(valueQuads));
        assertThat(check(literal), empty());
    }


    @Test
    void namedGraph() throws Exception
    {
        assertThat(check("SELECT ?g ?s ?o WHERE { GRAPH ?g { ?s ?p ?o } }"), hasSize(2));

        String query = "SELECT ?s WHERE { GRAPH ex:g { ?s ex:knows ex:a } }";

        checkTable(query, resourceQuads, valueQuads);
        assertThat(translate(dictionary, query), containsString("::int8 = \"g\""));
        assertThat(check(query), hasSize(1));
    }
}
