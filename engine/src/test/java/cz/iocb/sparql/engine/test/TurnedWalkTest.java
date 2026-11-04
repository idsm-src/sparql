package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
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
import cz.iocb.sparql.engine.mapping.ConstantIriMapping;
import cz.iocb.sparql.engine.mapping.classes.IntegerUserIriClass;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.TypedLiteral;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.LimitExceedException;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.translator.ServiceException;
import cz.iocb.sparql.testing.Database;



/**
 * Walks of property paths between two variables whose paths pass through nodes of several classes, which the
 * optimisation turns to start from the end the parent accepts in narrower classes, restricting the initial solutions to
 * those classes, and which are then seeded from the other end, so turned back: the seeded walk has to start from all
 * the initial solutions again, also those whose first step leads to a node of a class the parent does not accept, and a
 * segment of a walk split by the classes of its start must not be turned into the whole walk.
 */
public class TurnedWalkTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> ";

    /**
     * Configuration mapping the test tables.
     */
    private static SparqlDatabaseConfiguration config = null;


    /**
     * Creates the {@code turned_walk_test} schema, whose tables are mapped with the IRI classes {@code a}
     * ({@code http://example.org/a/<id>}) and {@code b} ({@code http://example.org/b/<id>}): in the default graph
     * {@code a/1 ex:p b/10}, {@code a/2 ex:p b/20} and {@code a/1 ex:q "x"}; in the graph {@code ex:g1}, which the
     * default graph includes, {@code b/10 ex:r a/1}, {@code b/10 ex:t "w"}, {@code b/10 ex:u b/30} and
     * {@code b/30 ex:u b/40}.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema turned_walk_test");
            statement.execute("create table turned_walk_test.ab (s int not null, o int not null)");
            statement.execute("insert into turned_walk_test.ab values (1, 10), (2, 20)");
            statement.execute("create table turned_walk_test.astring (s int not null, v varchar not null)");
            statement.execute("insert into turned_walk_test.astring values (1, 'x')");
            statement.execute("create table turned_walk_test.ba (s int not null, o int not null)");
            statement.execute("insert into turned_walk_test.ba values (10, 1)");
            statement.execute("create table turned_walk_test.bstring (s int not null, v varchar not null)");
            statement.execute("insert into turned_walk_test.bstring values (10, 'w')");
            statement.execute("create table turned_walk_test.bb (s int not null, o int not null)");
            statement.execute("insert into turned_walk_test.bb values (10, 30), (30, 40)");
        }

        DatabaseSchema schema = new DatabaseSchema(connectionPool);

        config = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);
        config.addPrefix("ex", "http://example.org/");
        config.addIriClass(new IntegerUserIriClass("a", INT4, "http://example.org/a/"));
        config.addIriClass(new IntegerUserIriClass("b", INT4, "http://example.org/b/"));

        ConstantIriMapping graph = config.createIriMapping("ex:g1");

        config.addQuadMapping(new DatabaseTable("turned_walk_test", "ab"), null, config.createIriMapping("a", "s"),
                config.createIriMapping("ex:p"), config.createIriMapping("b", "o"));

        config.addQuadMapping(new DatabaseTable("turned_walk_test", "astring"), null, config.createIriMapping("a", "s"),
                config.createIriMapping("ex:q"), config.createLiteralMapping(xsdString, "v"));

        config.addQuadMapping(new DatabaseTable("turned_walk_test", "ba"), graph, config.createIriMapping("b", "s"),
                config.createIriMapping("ex:r"), config.createIriMapping("a", "o"));

        config.addQuadMapping(new DatabaseTable("turned_walk_test", "bstring"), graph,
                config.createIriMapping("b", "s"), config.createIriMapping("ex:t"),
                config.createLiteralMapping(xsdString, "v"));

        config.addQuadMapping(new DatabaseTable("turned_walk_test", "bb"), graph, config.createIriMapping("b", "s"),
                config.createIriMapping("ex:u"), config.createIriMapping("b", "o"));
    }


    /**
     * Rows of the query.
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
     * IRI of the class {@code a}.
     *
     * @param id the id
     * @return IRI of the class {@code a}
     */
    private static Iri a(int id)
    {
        return new Iri("http://example.org/a/" + id);
    }


    /**
     * IRI of the class {@code b}.
     *
     * @param id the id
     * @return IRI of the class {@code b}
     */
    private static Iri b(int id)
    {
        return new Iri("http://example.org/b/" + id);
    }


    /**
     * String literal.
     *
     * @param value the value
     * @return string literal
     */
    private static TypedLiteral string(String value)
    {
        return new TypedLiteral(value, xsdStringIri);
    }


    @Test
    @DisplayName("a walk seeded from its start after being turned to its end")
    void seededFromStart() throws Exception
    {
        String query = "SELECT ?o WHERE { VALUES ?o { <http://example.org/a/1> <http://example.org/b/10> } "
                + "?o (ex:r|ex:p)+ ?x . ?x ex:q ?l }";

        assertThat(execute(query), containsInAnyOrder(row(a(1)), row(b(10))));
    }


    @Test
    @DisplayName("a walk in EXISTS seeded from the current solution")
    void seededFromCurrentSolution() throws Exception
    {
        String query = "SELECT ?o WHERE { ?s ?p ?o FILTER EXISTS { ?o (ex:r|ex:p)+ ?x . ?x ex:q ?l } }";

        assertThat(execute(query), containsInAnyOrder(row(a(1)), row(b(10))));
    }


    @Test
    @DisplayName("a walk in EXISTS joined with a GRAPH pattern")
    void joinedWithGraph() throws Exception
    {
        String query = "SELECT ?s ?o WHERE { ?s ?p ?o "
                + "FILTER EXISTS { { ?s (ex:p|ex:r)+ ?x } { GRAPH ex:g1 { ?x ?d ?o } } } }";

        assertThat(execute(query), containsInAnyOrder(row(b(10), a(1)), row(b(10), string("w")), row(b(10), b(30))));
    }


    @Test
    @DisplayName("a walk of zero or more steps over literal nodes in NOT EXISTS")
    void zeroOrMoreSteps() throws Exception
    {
        String query = "SELECT ?s ?o WHERE { ?s ?p ?o "
                + "FILTER NOT EXISTS { { ?o (ex:p|ex:r)* ?x } { GRAPH ex:g1 { ?x ex:t ?w } } } }";

        assertThat(execute(query), containsInAnyOrder(row(a(1), string("x")), row(a(2), b(20)), row(b(10), string("w")),
                row(b(10), b(30)), row(b(30), b(40))));
    }


    @Test
    @DisplayName("segments of a walk split by the classes of its end")
    void segments() throws Exception
    {
        String query = "SELECT ?o ?x WHERE { VALUES ?o { <http://example.org/a/1> <http://example.org/b/10> } "
                + "?o (ex:r|ex:p|ex:u)+ ?x . { ?x ex:p ?z } UNION { ?x ex:r ?z } }";

        assertThat(execute(query),
                containsInAnyOrder(row(a(1), a(1)), row(a(1), b(10)), row(b(10), a(1)), row(b(10), b(10))));
    }
}
