package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.iri;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
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
import cz.iocb.sparql.engine.mapping.classes.IntegerUserIriClass;
import cz.iocb.sparql.engine.mapping.classes.StringUserIriClass;
import cz.iocb.sparql.engine.mapping.classes.TripleTermClass;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.TripleTerm;
import cz.iocb.sparql.engine.rdf.TypedLiteral;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.LimitExceedException;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.translator.ServiceException;
import cz.iocb.sparql.testing.Database;



/**
 * Patterns matching triple terms stored in three ways: as boxes ({@code sparql.rdfbox} column mapped by the box class),
 * as the columns of their components (mapped by a triple term class) and as a constant mapping. Triple terms with
 * variables inside are decomposed into their components, constants inside become conditions, a repeated variable an
 * equality, and the variables of the components join with the other patterns.
 */
public class TripleTermMappingTest
{
    private static final String prefix = "PREFIX : <http://example.org/> "
            + "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> ";
    private static final Iri a = iri("a");
    private static final Iri b = iri("b");
    private static final Iri c = iri("c");
    private static final Iri d = iri("d");
    private static final Iri p = iri("p");
    private static final Iri q = iri("q");
    private static final TypedLiteral x = string("x");
    private static final TypedLiteral y = string("y");
    private static final TypedLiteral z = string("z");

    private static DataSource connectionPool = null;
    private static DatabaseSchema schema = null;
    private static SparqlDatabaseConfiguration config = null;


    @BeforeAll
    static void init() throws SQLException
    {
        connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema tt_test");

            statement.execute("create table tt_test.boxed (id int not null primary key, term sparql.rdfbox not null)");
            statement.execute("""
                    insert into tt_test.boxed values \
                    (1, '<<( <http://example.org/a> <http://example.org/p> "x" )>>'), \
                    (2, '<<( <http://example.org/b> <http://example.org/p> \
                    "1"^^<http://www.w3.org/2001/XMLSchema#integer> )>>'), \
                    (3, '<<( <http://example.org/a> <http://example.org/q> \
                    <<( <http://example.org/c> <http://example.org/p> "y" )>> )>>'), \
                    (4, '<http://example.org/notatriple>'), \
                    (5, '<<( <http://example.org/a> <http://example.org/p> <http://example.org/a> )>>')""");

            statement.execute("create table tt_test.columns (id int not null primary key, "
                    + "s varchar collate \"C\" not null, p varchar collate \"C\" not null, o int not null)");
            statement.execute("""
                    insert into tt_test.columns values \
                    (10, 'http://example.org/a', 'http://example.org/p', 5), \
                    (11, 'http://example.org/c', 'http://example.org/q', 7)""");

            statement.execute("create table tt_test.name (id varchar collate \"C\" not null primary key, "
                    + "name varchar collate \"C\" not null)");
            statement.execute("insert into tt_test.name values ('a', 'Alice'), ('b', 'Bob'), ('c', 'Carol')");
        }

        schema = new DatabaseSchema(connectionPool);

        IntegerUserIriClass reifier = new IntegerUserIriClass("reifier", INT4, "http://example.org/reifier/");
        StringUserIriClass thing = new StringUserIriClass("thing", "http://example.org/", "[a-c]");

        config = new SparqlDatabaseConfiguration(null, connectionPool, schema, false);
        config.addPrefix("ex", "http://example.org/");
        config.addPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        config.addIriClass(reifier);
        config.addIriClass(thing);

        config.addQuadMapping(new DatabaseTable("tt_test", "boxed"), null, config.createIriMapping(reifier, "id"),
                config.createIriMapping("rdf:reifies"), config.createTermMapping(box, "term"));
        config.addQuadMapping(new DatabaseTable("tt_test", "columns"), null, config.createIriMapping(reifier, "id"),
                config.createIriMapping("rdf:reifies"),
                config.createTermMapping(new TripleTermClass(iri, iri, xsdInt), "s", "p", "o"));
        config.addQuadMapping(new DatabaseTable("tt_test", "name"), null, config.createIriMapping(thing, "id"),
                config.createIriMapping("ex:name"), config.createLiteralMapping(xsdString, "name"));
        config.addQuadMapping(null, config.createIriMapping("<http://example.org/reifier/100>"),
                config.createIriMapping("rdf:reifies"), config.createTripleTermMapping(new TripleTerm(d, p, z)));
    }


    /**
     * Rows of the query.
     *
     * @param query the query, without the prefix declarations
     * @return rows of the query
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws LimitExceedException if the generated SQL is too long
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static List<List<RdfTerm>> execute(String query)
            throws SQLException, TranslateExceptions, LimitExceedException, ServiceException
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
     * IRI of the reifier with the given id.
     *
     * @param id the id
     * @return IRI of the reifier with the given id
     */
    private static Iri reifier(int id)
    {
        return new Iri("http://example.org/reifier/" + id);
    }


    /**
     * The xsd:string literal.
     *
     * @param value the value
     * @return the xsd:string literal
     */
    private static TypedLiteral string(String value)
    {
        return new TypedLiteral(value, xsdStringIri);
    }


    /**
     * Triple term of the components.
     *
     * @param subject the subject
     * @param predicate the predicate
     * @param object the object
     * @return triple term of the components
     */
    private static TripleTerm triple(RdfTerm subject, RdfTerm predicate, RdfTerm object)
    {
        return new TripleTerm(subject, predicate, object);
    }


    @Test
    @DisplayName("a triple term of variables matches the stored triple terms of every representation")
    void variables() throws Exception
    {
        List<List<RdfTerm>> expected = List.of(row(reifier(1), a, p, x),
                row(reifier(2), b, p, new TypedLiteral("1", xsdIntegerIri)), row(reifier(3), a, q, triple(c, p, y)),
                row(reifier(5), a, p, a), row(reifier(10), a, p, new TypedLiteral("5", xsdIntIri)),
                row(reifier(11), c, q, new TypedLiteral("7", xsdIntIri)), row(reifier(100), d, p, z));

        assertThat(execute("SELECT ?r ?s ?p ?o WHERE { ?r rdf:reifies <<( ?s ?p ?o )>> }"),
                containsInAnyOrder(expected.toArray()));

        assertThat(execute("SELECT ?r ?s ?p ?o WHERE { <<( ?s ?p ?o )>> ^rdf:reifies ?r }"),
                containsInAnyOrder(expected.toArray()));
    }


    @Test
    @DisplayName("constants inside a triple term become conditions on the components")
    void constants() throws Exception
    {
        assertThat(execute("SELECT ?r ?o WHERE { ?r rdf:reifies <<( :a :p ?o )>> }"), containsInAnyOrder(
                row(reifier(1), x), row(reifier(5), a), row(reifier(10), new TypedLiteral("5", xsdIntIri))));

        assertThat(execute("SELECT ?r ?s WHERE { ?r rdf:reifies <<( ?s :p \"x\" )>> }"),
                containsInAnyOrder(row(reifier(1), a)));

        assertThat(execute("SELECT ?r WHERE { ?r rdf:reifies <<( :a :p \"x\" )>> }"),
                containsInAnyOrder(row(reifier(1))));

        assertThat(execute("SELECT ?r WHERE { ?r rdf:reifies <<( :d :p \"z\" )>> }"),
                containsInAnyOrder(row(reifier(100))));

        assertThat(execute("SELECT ?r WHERE { ?r rdf:reifies <<( :d :p \"w\" )>> }"), containsInAnyOrder());
    }


    @Test
    @DisplayName("a repeated variable inside a triple term requires the same term")
    void repeatedVariable() throws Exception
    {
        assertThat(execute("SELECT ?r ?x ?p WHERE { ?r rdf:reifies <<( ?x ?p ?x )>> }"),
                containsInAnyOrder(row(reifier(5), a, p)));
    }


    @Test
    @DisplayName("a nested triple term pattern matches the nested stored triple term")
    void nested() throws Exception
    {
        assertThat(execute("SELECT ?r ?s ?a ?b ?c WHERE { ?r rdf:reifies <<( ?s :q <<( ?a ?b ?c )>> )>> }"),
                containsInAnyOrder(row(reifier(3), a, c, p, y)));
    }


    @Test
    @DisplayName("the variables of the components join with other patterns and are usable in expressions")
    void joins() throws Exception
    {
        assertThat(execute("SELECT ?r ?n WHERE { ?r rdf:reifies <<( ?s ?p ?o )>> . ?s :name ?n }"),
                containsInAnyOrder(row(reifier(1), string("Alice")), row(reifier(3), string("Alice")),
                        row(reifier(5), string("Alice")), row(reifier(2), string("Bob")),
                        row(reifier(10), string("Alice")), row(reifier(11), string("Carol"))));

        assertThat(execute(
                "SELECT ?r ?o WHERE { ?r rdf:reifies <<( :a :p ?o )>> . " + "?r rdf:reifies <<( ?s :p \"x\" )>> }"),
                containsInAnyOrder(row(reifier(1), x)));

        assertThat(execute("SELECT ?r WHERE { ?r rdf:reifies <<( ?s ?p ?o )>> FILTER(?o = 5) }"),
                containsInAnyOrder(row(reifier(10))));

        assertThat(execute("SELECT ?r WHERE { ?r rdf:reifies <<( ?s ?p ?o )>> FILTER(?o = 1) }"),
                containsInAnyOrder(row(reifier(2))));
    }


    @Test
    @DisplayName("a boxed term that is not a triple term matches a variable but no triple term pattern")
    void boxedTerms() throws Exception
    {
        assertThat(execute("SELECT ?r WHERE { ?r rdf:reifies ?t FILTER(!isTRIPLE(?t)) }"),
                containsInAnyOrder(row(reifier(4))));

        assertThat(execute("SELECT ?r WHERE { ?r rdf:reifies ?t FILTER(isTRIPLE(?t)) }"),
                containsInAnyOrder(row(reifier(1)), row(reifier(2)), row(reifier(3)), row(reifier(5)), row(reifier(10)),
                        row(reifier(11)), row(reifier(100))));
    }


    @Test
    @DisplayName("a negated property path matches triple terms too")
    void negatedPath() throws Exception
    {
        assertThat(execute("SELECT ?r ?o WHERE { ?r !:name <<( :a ?p ?o )>> }"),
                containsInAnyOrder(row(reifier(1), x), row(reifier(3), triple(c, p, y)), row(reifier(5), a),
                        row(reifier(10), new TypedLiteral("5", xsdIntIri))));
    }


    @Test
    @DisplayName("a triple term pattern at the end of a repeated path is matched against the end node")
    void repeatedPathEnd() throws Exception
    {
        TypedLiteral five = new TypedLiteral("5", xsdIntIri);

        assertThat(execute("SELECT ?r ?p ?o WHERE { ?r rdf:reifies+ <<( :a ?p ?o )>> }"),
                containsInAnyOrder(row(reifier(1), p, x), row(reifier(3), q, triple(c, p, y)), row(reifier(5), p, a),
                        row(reifier(10), p, five)));

        assertThat(execute("SELECT ?r ?s WHERE { ?r rdf:reifies+ <<( ?s :p ?s )>> }"),
                containsInAnyOrder(row(reifier(5), a)));

        assertThat(execute("SELECT ?r ?s ?p ?o WHERE { ?r rdf:reifies+ <<( :a :q <<( ?s ?p ?o )>> )>> }"),
                containsInAnyOrder(row(reifier(3), c, p, y)));

        assertThat(execute("SELECT ?r WHERE { ?r rdf:reifies+ <<( ?s ?p ?o )>> }"),
                containsInAnyOrder(row(reifier(1)), row(reifier(2)), row(reifier(3)), row(reifier(5)), row(reifier(10)),
                        row(reifier(11)), row(reifier(100))));
    }


    @Test
    @DisplayName("a triple term pattern at the start of a repeated path is matched against the start node")
    void repeatedPathStart() throws Exception
    {
        assertThat(execute("SELECT ?p ?o ?r WHERE { <<( :a ?p ?o )>> (^rdf:reifies)+ ?r }"),
                containsInAnyOrder(row(p, x, reifier(1)), row(q, triple(c, p, y), reifier(3)), row(p, a, reifier(5)),
                        row(p, new TypedLiteral("5", xsdIntIri), reifier(10))));
    }


    @Test
    @DisplayName("the zero-length path of * and ? binds a triple term pattern to the matching nodes of the graph")
    void zeroLengthPath() throws Exception
    {
        TypedLiteral one = new TypedLiteral("1", xsdIntegerIri);
        TypedLiteral five = new TypedLiteral("5", xsdIntIri);

        assertThat(execute("SELECT ?r ?s ?o WHERE { ?r rdf:reifies* <<( ?s :p ?o )>> }"),
                containsInAnyOrder(row(reifier(1), a, x), row(reifier(2), b, one), row(reifier(5), a, a),
                        row(reifier(10), a, five), row(reifier(100), d, z), row(triple(a, p, x), a, x),
                        row(triple(b, p, one), b, one), row(triple(a, p, a), a, a), row(triple(a, p, five), a, five),
                        row(triple(d, p, z), d, z)));

        assertThat(execute("SELECT ?r ?p ?o WHERE { ?r rdf:reifies? <<( :b ?p ?o )>> }"),
                containsInAnyOrder(row(reifier(2), p, one), row(triple(b, p, one), p, one)));
    }
}
