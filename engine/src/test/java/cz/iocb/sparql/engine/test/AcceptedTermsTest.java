package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerIri;
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
 * A filter that holds for several terms of a variable and for no other (IN of the variable and constants, a disjunction
 * of equalities or of {@code sameTerm} of the variable with constants) hands the terms down as the accepted terms of
 * the variable, and a walk of a property path between two variables starts from them, whatever lies between the filter
 * and the walk (an OPTIONAL, a BIND, a deduplication, a grouping by the variable): the recursion then enumerates only
 * the paths from the terms, all of which the filter keeps, instead of every path. The filter stays where it is, and a
 * walk is the only node that takes the terms in, so the solutions are the same as without them.
 *
 * The paths of {@code ex:p} lead through nodes of the IRI classes {@code a} and {@code b} to strings and to numbers of
 * the datatypes {@code xsd:int} and {@code xsd:integer}, and only nodes of the class {@code a} have labels. The
 * expected solutions are computed from the links.
 */
public class AcceptedTermsTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> PREFIX a: <http://example.org/a/> "
            + "PREFIX b: <http://example.org/b/> ";

    /**
     * The links of {@code ex:p}, each a subject and an object: a cycle of the nodes a/1, a/3 and a/2, a chain coming to
     * it from a/7 through nodes of the class {@code b}, two strings and numbers of two datatypes.
     */
    private static final List<List<RdfTerm>> links = List.of(row(a(2), a(1)), row(a(3), a(2)), row(a(5), a(4)),
            row(a(1), a(3)), row(a(1), string("x")), row(a(6), string("y")), row(a(2), number(10)),
            row(a(5), number(20)), row(a(6), number(10)), row(a(3), integer(10)), row(a(7), integer(30)),
            row(b(1), a(3)), row(b(2), a(5)), row(a(4), b(1)), row(a(7), b(2)));

    /**
     * The nodes that have a label, all of the class {@code a}.
     */
    private static final List<Iri> labelled = List.of(a(1), a(2), a(4), a(6), a(7));

    /**
     * Configuration mapping the test tables.
     */
    private static SparqlDatabaseConfiguration config = null;


    /**
     * Creates the {@code accepted_terms_test} schema, whose tables are mapped with the IRI classes {@code a}
     * ({@code http://example.org/a/<id>}) and {@code b} ({@code http://example.org/b/<id>}): the links of {@code ex:p}
     * by the classes of their ends (see {@link #links}) and the labels keyed by their nodes (see {@link #labelled}).
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema accepted_terms_test");
            statement.execute("create table accepted_terms_test.aa (s int not null, o int not null)");
            statement.execute("insert into accepted_terms_test.aa values (2, 1), (3, 2), (5, 4), (1, 3)");
            statement.execute("create table accepted_terms_test.astring (s int not null, o varchar not null)");
            statement.execute("insert into accepted_terms_test.astring values (1, 'x'), (6, 'y')");
            statement.execute("create table accepted_terms_test.aint (s int not null, o int not null)");
            statement.execute("insert into accepted_terms_test.aint values (2, 10), (5, 20), (6, 10)");
            statement.execute("create table accepted_terms_test.ainteger (s int not null, o numeric not null)");
            statement.execute("insert into accepted_terms_test.ainteger values (3, 10), (7, 30)");
            statement.execute("create table accepted_terms_test.ba (s int not null, o int not null)");
            statement.execute("insert into accepted_terms_test.ba values (1, 3), (2, 5)");
            statement.execute("create table accepted_terms_test.ab (s int not null, o int not null)");
            statement.execute("insert into accepted_terms_test.ab values (4, 1), (7, 2)");
            statement.execute("create table accepted_terms_test.label (s int primary key, l varchar not null)");
            statement.execute("insert into accepted_terms_test.label values (1, 'one'), (2, 'two'), (4, 'four'), "
                    + "(6, 'six'), (7, 'seven')");
        }

        DatabaseSchema schema = new DatabaseSchema(connectionPool);

        config = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);
        config.addPrefix("ex", "http://example.org/");
        config.addIriClass(new IntegerUserIriClass("a", INT4, "http://example.org/a/"));
        config.addIriClass(new IntegerUserIriClass("b", INT4, "http://example.org/b/"));

        config.addQuadMapping(new DatabaseTable("accepted_terms_test", "aa"), null, config.createIriMapping("a", "s"),
                config.createIriMapping("ex:p"), config.createIriMapping("a", "o"));

        config.addQuadMapping(new DatabaseTable("accepted_terms_test", "astring"), null,
                config.createIriMapping("a", "s"), config.createIriMapping("ex:p"),
                config.createLiteralMapping(xsdString, "o"));

        config.addQuadMapping(new DatabaseTable("accepted_terms_test", "aint"), null, config.createIriMapping("a", "s"),
                config.createIriMapping("ex:p"), config.createLiteralMapping(xsdInt, "o"));

        config.addQuadMapping(new DatabaseTable("accepted_terms_test", "ainteger"), null,
                config.createIriMapping("a", "s"), config.createIriMapping("ex:p"),
                config.createLiteralMapping(xsdInteger, "o"));

        config.addQuadMapping(new DatabaseTable("accepted_terms_test", "ba"), null, config.createIriMapping("b", "s"),
                config.createIriMapping("ex:p"), config.createIriMapping("a", "o"));

        config.addQuadMapping(new DatabaseTable("accepted_terms_test", "ab"), null, config.createIriMapping("a", "s"),
                config.createIriMapping("ex:p"), config.createIriMapping("b", "o"));

        config.addQuadMapping(new DatabaseTable("accepted_terms_test", "label"), null,
                config.createIriMapping("a", "s"), config.createIriMapping("ex:label"),
                config.createLiteralMapping(xsdString, "l"));
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
     * The SQL code of the initial solutions of the walks of the query: the terms of its recursions that are not
     * recursive.
     *
     * @param query the query, without the prefix declarations
     * @return the SQL code of the initial solutions of the walks of the query
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws LimitExceedException if the generated SQL is too long
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static String initialSolutions(String query)
            throws SQLException, TranslateExceptions, LimitExceedException, ServiceException
    {
        Engine engine = new Engine(config);

        try(Request request = engine.getRequest())
        {
            PreparedQuery prepared = request.prepareQuery(prefix + query, null);
            SqlSelect imcode = new TranslateVisitor(request).translate(prepared.getSyntaxTree(), null, null, List.of());
            String sql = imcode.optimize(request, false).optimize(request, true).translate(request);

            StringBuilder builder = new StringBuilder();
            String keyword = "WITH RECURSIVE ";

            // the recursive term of a recursion follows the last UNION before the recursion is referred to
            for(int begin = sql.indexOf(keyword); begin != -1; begin = sql.indexOf(keyword, begin + 1))
            {
                int reference = sql.indexOf(" FROM recursion AS \"walk\"", begin);

                builder.append(sql, begin, sql.lastIndexOf(" UNION SELECT ", reference));
            }

            return builder.toString();
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
     * Literal of the datatype {@code xsd:int}.
     *
     * @param value the value
     * @return literal of the datatype {@code xsd:int}
     */
    private static TypedLiteral number(int value)
    {
        return new TypedLiteral(Integer.toString(value), xsdIntIri);
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
     * The paths of one or more links of {@code ex:p} that end in one of the given terms.
     *
     * @param ends the terms
     * @return the paths of one or more links that end in one of the terms
     */
    private static Set<List<RdfTerm>> pathsTo(RdfTerm... ends)
    {
        return paths(p -> List.of(ends).contains(p.get(1)));
    }


    /**
     * The paths of one or more links of {@code ex:p} that start in one of the given terms.
     *
     * @param starts the terms
     * @return the paths of one or more links that start in one of the terms
     */
    private static Set<List<RdfTerm>> pathsFrom(RdfTerm... starts)
    {
        return paths(p -> List.of(starts).contains(p.get(0)));
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
    @DisplayName("a walk starts from the terms a filter accepts of its end")
    void termsOfEnd() throws Exception
    {
        String query = "SELECT ?s ?o WHERE { ?s ex:p+ ?o FILTER(?o IN (a:1, a:2)) }";

        check(query, pathsTo(a(1), a(2)));
        check("SELECT ?s ?o WHERE { ?o ^ex:p+ ?s FILTER(?o IN (a:1, a:2)) }", pathsTo(a(1), a(2)));

        // the walk is turned, and its initial solutions are the links to the two nodes
        assertThat(initialSolutions(query), containsString("\"o\" = '1'::int4"));
        assertThat(initialSolutions(query), containsString("\"o\" = '2'::int4"));

        // the terms of several classes
        check("SELECT ?s ?o WHERE { ?s ex:p+ ?o FILTER(?o IN (a:1, b:1, 'x')) }", pathsTo(a(1), b(1), string("x")));
    }


    @Test
    @DisplayName("a walk starts from the terms a filter accepts of its start")
    void termsOfStart() throws Exception
    {
        String query = "SELECT ?s ?o WHERE { ?s ex:p+ ?o FILTER(?s IN (a:5, a:6)) }";

        check(query, pathsFrom(a(5), a(6)));
        assertThat(initialSolutions(query), containsString("\"s\" = '5'::int4"));

        check("SELECT ?s ?o WHERE { ?s ex:p+ ?o FILTER(?s IN (a:5, b:2, a:6)) }", pathsFrom(a(5), b(2), a(6)));
    }


    @Test
    @DisplayName("a filter without accepted terms leaves a walk as it is")
    void withoutTerms() throws Exception
    {
        Set<List<RdfTerm>> expected = paths(p -> !List.of(a(1), a(2)).contains(p.get(1)));

        String query = "SELECT ?s ?o WHERE { ?s ex:p+ ?o FILTER(?o NOT IN (a:1, a:2)) }";

        check(query, expected);
        assertThat(initialSolutions(query), not(containsString("'1'::int4")));

        // the conditions are about different variables
        check("SELECT ?s ?o WHERE { ?s ex:p+ ?o FILTER(?s = a:5 || ?o = a:1) }",
                paths(p -> p.get(0).equals(a(5)) || p.get(1).equals(a(1))));

        // a member of the list is no constant
        check("SELECT ?s ?o WHERE { ?s ex:p+ ?o FILTER(?o IN (a:1, ?s)) }",
                paths(p -> p.get(1).equals(a(1)) || p.get(1).equals(p.get(0))));
    }


    @Test
    @DisplayName("a disjunction of equalities and identities with constants accepts their terms")
    void disjunction() throws Exception
    {
        String query = "SELECT ?s ?o WHERE { ?s ex:p+ ?o FILTER(?o = a:1 || sameTerm(?o, 'x') || b:1 = ?o) }";

        // the walk is split by the classes of its end, and every part starts from the terms of its class
        check(query, pathsTo(a(1), string("x"), b(1)));
        assertThat(initialSolutions(query), containsString("\"o\" = 'x'::varchar"));
        assertThat(initialSolutions(query), containsString("'1'::int4"));

        check("SELECT ?s ?o WHERE { ?s ex:p+ ?o FILTER(?o IN (a:1, a:2) || ?o = b:1) }", pathsTo(a(1), a(2), b(1)));
    }


    @Test
    @DisplayName("a list of one term requires the term like an equality does")
    void singleTerm() throws Exception
    {
        check("SELECT ?s ?o WHERE { ?s ex:p+ ?o FILTER(?o IN (a:1)) }", pathsTo(a(1)));
        check("SELECT ?s ?o WHERE { ?s ex:p+ ?o FILTER(?o = a:1 || a:1 = ?o) }", pathsTo(a(1)));
        check("SELECT ?s ?o WHERE { ?s ex:p+ ?o FILTER(?o IN ('x', 'x')) }", pathsTo(string("x")));

        assertThat(initialSolutions("SELECT ?s ?o WHERE { ?s ex:p+ ?o FILTER(?o IN (a:1)) }"),
                containsString("\"o\" = '1'::int4"));
    }


    @Test
    @DisplayName("a number accepts the literals of its value in the datatypes of the columns")
    void numbers() throws Exception
    {
        String query = "SELECT ?s ?o WHERE { ?s ex:p+ ?o FILTER(?o = 10) }";

        check(query, pathsTo(number(10), integer(10)));
        assertThat(initialSolutions(query), containsString("'10'::int4"));
        assertThat(initialSolutions(query), containsString("'10'::numeric"));

        query = "SELECT ?s ?o WHERE { ?s ex:p+ ?o FILTER(?o IN (30, 20.0)) }";

        check(query, pathsTo(integer(30), number(20)));
        assertThat(initialSolutions(query), containsString("'20'::int4"));
        assertThat(initialSolutions(query), containsString("'30'::numeric"));

        check("SELECT ?s ?o WHERE { ?s ex:p+ ?o FILTER(?o IN (10, 'x', a:1)) }",
                pathsTo(number(10), integer(10), string("x"), a(1)));

        // no value of the columns is equal to a number with a fractional part, so the list singles no terms out
        check("SELECT ?s ?o WHERE { ?s ex:p+ ?o FILTER(?o IN (10, 20.5)) }", pathsTo(number(10), integer(10)));

        // the identity takes the terms as they are
        check("SELECT ?s ?o WHERE { ?s ex:p+ ?o FILTER(sameTerm(?o, 10) || sameTerm(?o, 20)) }", pathsTo(integer(10)));
    }


    @Test
    @DisplayName("the terms reach a walk through the patterns between it and the filter")
    void throughPatterns() throws Exception
    {
        Set<List<RdfTerm>> paths = pathsTo(a(1), string("x"));

        // an OPTIONAL extending the solutions of the walk
        Set<List<RdfTerm>> optional = new HashSet<>();

        for(List<RdfTerm> path : paths)
            optional.add(row(path.get(0), path.get(1), labelled.contains(path.get(0)) ? string("label") : null));

        String query = """
                SELECT ?s ?o ?l WHERE { ?s ex:p+ ?o OPTIONAL { ?s ex:label ?y BIND('label' AS ?l) }
                FILTER(?o IN (a:1, 'x')) }""";

        check(query, optional);
        assertThat(initialSolutions(query), containsString("'1'::int4"));

        // a BIND
        query = "SELECT ?s ?o WHERE { ?s ex:p+ ?o BIND(STR(?o) AS ?t) FILTER(?o IN (a:1, 'x')) }";

        check(query, paths);
        assertThat(initialSolutions(query), containsString("'1'::int4"));

        // a deduplication
        query = "SELECT ?s ?o WHERE { { SELECT DISTINCT ?s ?o WHERE { ?s ex:p+ ?o } } FILTER(?o IN (a:1, 'x')) }";

        check(query, paths);
        assertThat(initialSolutions(query), containsString("'1'::int4"));

        // a grouping by the variable
        Set<List<RdfTerm>> counts = new HashSet<>();

        for(RdfTerm end : List.of(a(1), string("x")))
            counts.add(row(end, new TypedLiteral(Integer.toString(pathsTo(end).size()), xsdIntegerIri)));

        query = "SELECT ?o (COUNT(?s) AS ?c) WHERE { ?s ex:p+ ?o } GROUP BY ?o HAVING(?o IN (a:1, 'x'))";

        check(query, counts);
        assertThat(initialSolutions(query), containsString("'1'::int4"));

        // a join
        query = "SELECT ?s ?o WHERE { ?s ex:label ?l . ?s ex:p+ ?o FILTER(?o IN (a:1, 'x')) }";

        check(query, paths(p -> paths.contains(p) && labelled.contains(p.get(0))));
        assertThat(initialSolutions(query), containsString("'1'::int4"));
    }


    @Test
    @DisplayName("an optional walk starts from the terms the filter of the OPTIONAL accepts")
    void optionalWalk() throws Exception
    {
        Set<List<RdfTerm>> paths = pathsTo(a(1), string("x"));
        Set<List<RdfTerm>> expected = new HashSet<>();

        for(Iri node : labelled)
        {
            List<List<RdfTerm>> found = paths.stream().filter(p -> p.get(0).equals(node)).toList();

            if(found.isEmpty())
                expected.add(row(node, null));

            expected.addAll(found);
        }

        String query = "SELECT ?s ?o WHERE { ?s ex:label ?y OPTIONAL { ?s ex:p+ ?o FILTER(?o IN (a:1, 'x')) } }";

        check(query, expected);
        assertThat(initialSolutions(query), containsString("'1'::int4"));
    }


    @Test
    @DisplayName("a walk of zero or more steps starts from the accepted terms that are nodes of the graph")
    void zeroOrMoreSteps() throws Exception
    {
        Set<List<RdfTerm>> expected = pathsTo(a(1), string("x"));

        // the terms are paths of no step by themselves, but a/9 is no node of the graph
        expected.add(row(a(1), a(1)));
        expected.add(row(string("x"), string("x")));

        check("SELECT ?s ?o WHERE { ?s ex:p* ?o FILTER(?o IN (a:1, a:9, 'x')) }", expected);

        expected = pathsFrom(a(5), b(2));
        expected.add(row(a(5), a(5)));
        expected.add(row(b(2), b(2)));

        check("SELECT ?s ?o WHERE { ?s ex:p* ?o FILTER(?s = a:5 || ?s = b:2 || ?s = b:9) }", expected);
    }


    @Test
    @DisplayName("the terms several conditions accept of a variable are those all of them accept")
    void conjunction() throws Exception
    {
        check("SELECT ?s ?o WHERE { ?s ex:p+ ?o FILTER(?o IN (a:1, a:2)) FILTER(?o IN (a:2, a:3, 'x')) }",
                pathsTo(a(2)));

        check("SELECT ?s ?o WHERE { ?s ex:p+ ?o FILTER(?o IN (a:1, a:2) && ?o IN (a:3, 'x')) }", Set.of());

        check("SELECT ?s ?o WHERE { ?s ex:p+ ?o FILTER(?o IN (a:1, a:2, 'x') && ?o != a:2) }",
                pathsTo(a(1), string("x")));

        List<RdfTerm> starts = List.of(a(5), a(6), b(1));
        List<RdfTerm> ends = List.of(a(1), number(10), integer(10));

        check("SELECT ?s ?o WHERE { ?s ex:p+ ?o FILTER(?s IN (a:5, a:6, b:1) && ?o IN (a:1, 10)) }",
                paths(p -> starts.contains(p.get(0)) && ends.contains(p.get(1))));
    }


    @Test
    @DisplayName("terms of classes an end of a walk never takes leave no solution")
    void termsOfOtherClasses() throws Exception
    {
        // no path starts in a literal
        check("SELECT ?s ?o WHERE { ?s ex:p+ ?o FILTER(?s IN ('x', 'y')) }", Set.of());

        // IRIs of no class of the nodes
        check("SELECT ?s ?o WHERE { ?s ex:p+ ?o FILTER(?o IN (ex:one, ex:two)) }", Set.of());

        check("SELECT ?s ?o WHERE { ?s ex:p+ ?o FILTER(?o IN (ex:one, a:1)) }", pathsTo(a(1)));
    }


    @Test
    @DisplayName("a walk inside a tested or a subtracted pattern starts from the accepted terms")
    void testedPatterns() throws Exception
    {
        Set<List<RdfTerm>> paths = pathsTo(a(1), string("y"));
        Set<List<RdfTerm>> starts = new HashSet<>();
        Set<List<RdfTerm>> others = new HashSet<>();

        for(Iri node : labelled)
            (paths.stream().anyMatch(p -> p.get(0).equals(node)) ? starts : others).add(row(node));

        check("SELECT ?s WHERE { ?s ex:label ?y FILTER EXISTS { ?s ex:p+ ?o FILTER(?o IN (a:1, 'y')) } }", starts);
        check("SELECT ?s WHERE { ?s ex:label ?y FILTER NOT EXISTS { ?s ex:p+ ?o FILTER(?o IN (a:1, 'y')) } }", others);
        check("SELECT ?s WHERE { ?s ex:label ?y MINUS { ?s ex:p+ ?o FILTER(?o IN (a:1, 'y')) } }", others);
    }
}
