package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
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
import cz.iocb.sparql.engine.rdf.Literal;
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
 * A walk of a property path whose start the optimisation has restricted to some classes (the classes a filter above
 * accepts, the classes of a segment) keeps the restriction whatever becomes of it: the nodes above rely on the classes
 * of the bindings they were given, and a filter whose condition those classes decide is gone. So a walk turned the
 * other way round, by the restrictions of its parent or by the siblings seeding it from its end, delivers its new end
 * only in the classes it started in, though it passes through the nodes of the other classes; and a walk that does not
 * start in every node is not reduced to its initial solutions when the parent needs its end alone, since a node may be
 * reached from its starts through nodes that start none of its paths: it is walked without its start instead.
 *
 * The paths of {@code ex:p} lead through nodes of the IRI classes {@code a} and {@code b} to literals, and only nodes
 * of the class {@code a} have labels; the links of {@code ex:link} lead from nodes of both classes to nodes of the
 * class {@code a}. The expected solutions are computed from the links.
 */
public class RestrictedWalkTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> PREFIX a: <http://example.org/a/> "
            + "PREFIX b: <http://example.org/b/> ";

    /**
     * The links of {@code ex:p}, each a subject and an object: a cycle of the nodes a/1, a/3 and a/2, a chain coming to
     * it from a/7 through nodes of the class {@code b}, and two literals.
     */
    private static final List<List<RdfTerm>> links = List.of(row(a(2), a(1)), row(a(3), a(2)), row(a(5), a(4)),
            row(a(1), a(3)), row(a(1), string("x")), row(a(6), string("y")), row(b(1), a(3)), row(b(2), a(5)),
            row(a(4), b(1)), row(a(7), b(2)));

    /**
     * The nodes that have a label, all of the class {@code a}.
     */
    private static final List<Iri> labelled = List.of(a(1), a(2), a(4), a(6), a(7));

    /**
     * Configuration mapping the test tables.
     */
    private static SparqlDatabaseConfiguration config = null;


    /**
     * Creates the {@code restricted_walk_test} schema, whose tables are mapped with the IRI classes {@code a}
     * ({@code http://example.org/a/<id>}) and {@code b} ({@code http://example.org/b/<id>}): the links of {@code ex:p}
     * by the classes of their ends (see {@link #links}), the labels keyed by their nodes (see {@link #labelled}), and
     * the links of {@code ex:link} from the nodes of the class {@code a} (a cycle of a/1, a/2 and a/3, and a/4, a/5,
     * a/6 in a row) and from the nodes of the class {@code b} (b/1 to a/1, b/2 to a/4, b/7 to a/7).
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema restricted_walk_test");
            statement.execute("create table restricted_walk_test.aa (s int not null, o int not null)");
            statement.execute("insert into restricted_walk_test.aa values (2, 1), (3, 2), (5, 4), (1, 3)");
            statement.execute("create table restricted_walk_test.astring (s int not null, o varchar not null)");
            statement.execute("insert into restricted_walk_test.astring values (1, 'x'), (6, 'y')");
            statement.execute("create table restricted_walk_test.ba (s int not null, o int not null)");
            statement.execute("insert into restricted_walk_test.ba values (1, 3), (2, 5)");
            statement.execute("create table restricted_walk_test.ab (s int not null, o int not null)");
            statement.execute("insert into restricted_walk_test.ab values (4, 1), (7, 2)");
            statement.execute("create table restricted_walk_test.label (s int primary key, l varchar not null)");
            statement.execute("insert into restricted_walk_test.label values (1, 'one'), (2, 'two'), (4, 'four'), "
                    + "(6, 'six'), (7, 'seven')");
            statement.execute("create table restricted_walk_test.alink (s int not null, o int not null)");
            statement.execute("insert into restricted_walk_test.alink values (1, 2), (2, 3), (3, 1), (4, 5), (5, 6)");
            statement.execute("create table restricted_walk_test.blink (s int not null, o int not null)");
            statement.execute("insert into restricted_walk_test.blink values (1, 1), (2, 4), (7, 7)");
        }

        DatabaseSchema schema = new DatabaseSchema(connectionPool);

        config = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);
        config.addPrefix("ex", "http://example.org/");
        config.addIriClass(new IntegerUserIriClass("a", INT4, "http://example.org/a/"));
        config.addIriClass(new IntegerUserIriClass("b", INT4, "http://example.org/b/"));

        config.addQuadMapping(new DatabaseTable("restricted_walk_test", "aa"), null, config.createIriMapping("a", "s"),
                config.createIriMapping("ex:p"), config.createIriMapping("a", "o"));

        config.addQuadMapping(new DatabaseTable("restricted_walk_test", "astring"), null,
                config.createIriMapping("a", "s"), config.createIriMapping("ex:p"),
                config.createLiteralMapping(xsdString, "o"));

        config.addQuadMapping(new DatabaseTable("restricted_walk_test", "ba"), null, config.createIriMapping("b", "s"),
                config.createIriMapping("ex:p"), config.createIriMapping("a", "o"));

        config.addQuadMapping(new DatabaseTable("restricted_walk_test", "ab"), null, config.createIriMapping("a", "s"),
                config.createIriMapping("ex:p"), config.createIriMapping("b", "o"));

        config.addQuadMapping(new DatabaseTable("restricted_walk_test", "label"), null,
                config.createIriMapping("a", "s"), config.createIriMapping("ex:label"),
                config.createLiteralMapping(xsdString, "l"));

        config.addQuadMapping(new DatabaseTable("restricted_walk_test", "alink"), null,
                config.createIriMapping("a", "s"), config.createIriMapping("ex:link"),
                config.createIriMapping("a", "o"));

        config.addQuadMapping(new DatabaseTable("restricted_walk_test", "blink"), null,
                config.createIriMapping("b", "s"), config.createIriMapping("ex:link"),
                config.createIriMapping("a", "o"));
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
     * The SQL code the query is translated to.
     *
     * @param query the query, without the prefix declarations
     * @return the SQL code the query is translated to
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws LimitExceedException if the generated SQL is too long
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static String translate(String query)
            throws SQLException, TranslateExceptions, LimitExceedException, ServiceException
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


    /**
     * The paths of one or more links of {@code ex:p} that pass the test, each as its start and its end.
     *
     * @param test the test of a path, given as its start and its end
     * @return the paths of one or more links that pass the test
     */
    private static Set<List<RdfTerm>> paths(Predicate<List<RdfTerm>> test)
    {
        Set<List<RdfTerm>> paths = new HashSet<>(links);

        for(boolean extended = true; extended;)
        {
            extended = false;

            for(List<RdfTerm> path : List.copyOf(paths))
                for(List<RdfTerm> link : links)
                    if(path.get(1).equals(link.get(0)))
                        extended |= paths.add(row(path.get(0), link.get(1)));
        }

        paths.removeIf(test.negate());

        return paths;
    }


    /**
     * The values the rows have in the given column.
     *
     * @param rows the rows
     * @param column index of the column
     * @return the values the rows have in the column
     */
    private static Set<List<RdfTerm>> column(Set<List<RdfTerm>> rows, int column)
    {
        Set<List<RdfTerm>> values = new HashSet<>();

        for(List<RdfTerm> row : rows)
            values.add(row(row.get(column)));

        return values;
    }


    /**
     * Checks that the query gives the expected rows, each once.
     *
     * @param query the query, without the prefix declarations
     * @param expected the expected rows
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws LimitExceedException if the generated SQL is too long
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static void check(String query, Set<List<RdfTerm>> expected)
            throws SQLException, TranslateExceptions, LimitExceedException, ServiceException
    {
        List<List<RdfTerm>> rows = execute(query);

        assertThat(query, new HashSet<>(rows), is(expected));
        assertThat(query, rows, hasSize(expected.size()));
    }


    @Test
    @DisplayName("a walk restricted by a filter is not turned to all its starts again")
    void turnedAfterRestriction() throws Exception
    {
        // the filter accepts the start of the first walk in the class of its end alone
        check("SELECT DISTINCT ?x WHERE { ?y ex:link+ ?x . ?x ex:link+ ?z FILTER(?x = ?y) }",
                Set.of(row(a(1)), row(a(2)), row(a(3))));
    }


    @Test
    @DisplayName("the starts of the paths to nodes of a class")
    void startsOfPathsToClass() throws Exception
    {
        Set<List<RdfTerm>> expected = column(paths(p -> p.get(1) instanceof Literal), 0);

        // the walk is turned to start from the literals, and the filter that the classes of its start decide is gone
        String query = "SELECT DISTINCT ?s WHERE { ?s ex:p+ ?o FILTER(isLiteral(?o)) }";

        check(query, expected);
        check("SELECT DISTINCT ?s WHERE { ?o ^ex:p+ ?s FILTER(isLiteral(?o)) }", expected);

        // the walk carries the literals no more
        assertThat(translate(query), not(containsString("\"o#")));
    }


    @Test
    @DisplayName("the starts of the paths of zero or more steps to nodes of a class")
    void startsOfZeroOrMoreSteps() throws Exception
    {
        Set<List<RdfTerm>> expected = column(paths(p -> p.get(1) instanceof Literal), 0);

        // every literal of the graph is a path of no step by itself
        expected.add(row(string("x")));
        expected.add(row(string("y")));

        for(String label : List.of("one", "two", "four", "six", "seven"))
            expected.add(row(string(label)));

        check("SELECT DISTINCT ?s WHERE { ?s ex:p* ?o FILTER(isLiteral(?o)) }", expected);
    }


    @Test
    @DisplayName("the ends of the paths from the nodes of a segment")
    void endsOfSegment() throws Exception
    {
        // the condition holds for every start of the class a, so the segment of that class is left without a filter
        check("SELECT DISTINCT ?o WHERE { ?s ex:p+ ?o FILTER(?s != b:1 && ?s != b:2) }",
                column(paths(p -> !p.get(0).equals(b(1)) && !p.get(0).equals(b(2))), 1));

        check("SELECT DISTINCT ?o WHERE { ?s ex:p+ ?o FILTER(?s != a:4 && ?s != a:7) }",
                column(paths(p -> !p.get(0).equals(a(4)) && !p.get(0).equals(a(7))), 1));
    }


    @Test
    @DisplayName("a walk restricted by a filter and seeded from its end")
    void seededFromEnd() throws Exception
    {
        Set<List<RdfTerm>> expected = paths(p -> p.get(1) instanceof Literal && labelled.contains(p.get(0)));

        // the filter is gone before the labels seed the walk from the end the filter did not restrict
        String query = "SELECT ?s ?o WHERE { { ?o ^ex:p+ ?s FILTER(isLiteral(?o)) } ?s ex:label ?l }";

        check(query, expected);
        check("SELECT ?s ?o WHERE { ?o ^ex:p+ ?s . ?s ex:label ?l FILTER(isLiteral(?o)) }", expected);

        // the seeded walk passes through the nodes of both classes and delivers the literals alone
        assertThat(translate(query), containsString(" FROM recursion WHERE (\"o#string"));
    }


    @Test
    @DisplayName("a walk restricted by a filter on the right side of a MINUS")
    void minus() throws Exception
    {
        Set<List<RdfTerm>> starts = column(paths(p -> p.get(1) instanceof Literal), 0);
        Set<List<RdfTerm>> expected = new HashSet<>();

        for(Iri node : labelled)
            if(!starts.contains(row(node)))
                expected.add(row(node));

        check("SELECT ?s WHERE { ?s ex:label ?y MINUS { ?o ^ex:p+ ?s FILTER(isLiteral(?o)) } }", expected);
    }


    @Test
    @DisplayName("an optional walk to a term a filter requires")
    void optionalWalkToRequiredTerm() throws Exception
    {
        for(RdfTerm end : List.of(a(1), string("x"), b(1)))
        {
            Set<List<RdfTerm>> paths = paths(p -> p.get(1).equals(end));
            Set<List<RdfTerm>> expected = new HashSet<>();

            for(Iri node : labelled)
                expected.add(row(node, paths.contains(row(node, end)) ? end : null));

            String term = end instanceof Iri iri ? "<" + iri.getValue() + ">" : "'" + ((Literal) end).getValue() + "'";

            check("SELECT ?s ?o WHERE { ?s ex:label ?y OPTIONAL { ?s ex:p+ ?o FILTER(?o = " + term + ") } }", expected);
        }
    }
}
