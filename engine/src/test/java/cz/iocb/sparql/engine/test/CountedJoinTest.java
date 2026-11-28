package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.config.EngineSettings.JoinOptimizerMode;
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
 * Counts of the solutions of joins that multiply the solutions of their children, which are counted by the counts of
 * the children grouped by the variables they share and by the grouping variables: without grouping and per group, with
 * three children, with no solution, and with a shared variable a child may leave unbound. A child that the grouping
 * does not reduce is joined as it is; a join that does not multiply the solutions of its children, a join none of whose
 * children the grouping reduces and a count of distinct values are counted as they are.
 */
public class CountedJoinTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> ";

    /**
     * The column of the count of a child of a join counted by the counts of its children.
     */
    private static final Pattern counted = Pattern.compile("\"@count[0-9]+#");

    /**
     * Configuration mapping the test tables.
     */
    private static SparqlDatabaseConfiguration config = null;

    /**
     * The same configuration with the join optimizer enabled.
     */
    private static SparqlDatabaseConfiguration optimized = null;


    /**
     * Creates the {@code counted_join_test} schema, whose tables are mapped with the IRI class {@code a}
     * ({@code http://example.org/a/<id>}): {@code ex:p} relates each of {@code a/1} to {@code a/5} to {@code a/1} to
     * {@code a/40} and {@code a/6} to {@code a/1}; {@code ex:q} relates each of {@code a/1} to {@code a/5} to
     * {@code a/1} to {@code a/30} and {@code a/7} to {@code a/1}; {@code ex:t} relates each of {@code a/1} to
     * {@code a/5} to {@code a/1} to {@code a/3}; {@code ex:r} relates each of {@code a/1} to {@code a/200} to itself;
     * {@code ex:z} relates {@code a/100} to {@code a/1} to {@code a/300}; {@code ex:w} relates {@code a/1} and
     * {@code a/2} to {@code a/1}; and {@code ex:v} relates {@code a/1} to {@code a/1} to {@code a/50} and {@code a/2}
     * to {@code a/1} to {@code a/10}.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            String tables = "counted_join_test.";

            statement.execute("create schema counted_join_test");

            for(String table : List.of("p", "q", "t", "z", "w", "v"))
                statement.execute("create table " + tables + table + " (s int not null, o int not null)");

            statement.execute("create table " + tables + "r (s int primary key, o int not null)");
            statement.execute("insert into " + tables
                    + "p select s, o from generate_series(1, 5) as s, generate_series(1, 40) as o");
            statement.execute("insert into " + tables + "p values (6, 1)");
            statement.execute("insert into " + tables
                    + "q select s, o from generate_series(1, 5) as s, generate_series(1, 30) as o");
            statement.execute("insert into " + tables + "q values (7, 1)");
            statement.execute("insert into " + tables
                    + "t select s, o from generate_series(1, 5) as s, generate_series(1, 3) as o");
            statement.execute("insert into " + tables + "r select i, i from generate_series(1, 200) as i");
            statement.execute("insert into " + tables + "z select 100, i from generate_series(1, 300) as i");
            statement.execute("insert into " + tables + "w values (1, 1), (2, 1)");
            statement.execute("insert into " + tables + "v select 1, i from generate_series(1, 50) as i");
            statement.execute("insert into " + tables + "v select 2, i from generate_series(1, 10) as i");
            statement.execute("analyze " + tables + "p, " + tables + "q, " + tables + "t, " + tables + "r, " + tables
                    + "z, " + tables + "w, " + tables + "v");
        }

        DatabaseSchema schema = new DatabaseSchema(connectionPool);

        config = configuration(connectionPool, schema);
        optimized = configuration(connectionPool, schema);
        optimized.setEngineSettings(optimized.getEngineSettings().withJoinOptimizer(JoinOptimizerMode.SETTINGS));
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

        for(String table : List.of("p", "q", "t", "r", "z", "w", "v"))
            configuration.addQuadMapping(new DatabaseTable("counted_join_test", table), null,
                    configuration.createIriMapping("a", "s"), configuration.createIriMapping("ex:" + table),
                    configuration.createIriMapping("a", "o"));

        return configuration;
    }


    /**
     * Rows of the query, which have to be the same with the join optimizer enabled.
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
        List<List<RdfTerm>> rows = execute(config, query);

        assertThat(execute(optimized, query), containsInAnyOrder(rows.toArray()));

        return rows;
    }


    /**
     * Rows of the query under the configuration.
     *
     * @param configuration the configuration
     * @param query the query, without the prefix declarations
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
     * SQL of the query under the configuration.
     *
     * @param configuration the configuration
     * @param query the query, without the prefix declarations
     * @return SQL of the query
     * @throws Exception on errors
     */
    private static String translate(SparqlDatabaseConfiguration configuration, String query) throws Exception
    {
        Engine engine = new Engine(configuration);

        try(Request request = engine.getRequest())
        {
            PreparedQuery prepared = request.prepareQuery(prefix + query, null);
            SqlSelect imcode = new TranslateVisitor(request).translate(prepared.getSyntaxTree(), null, null, List.of());

            return imcode.optimize(request, false).optimize(request, true).translate(request);
        }
    }


    /**
     * Checks that the query is counted by the counts of the number of the children of its join, or as it is when the
     * number is zero, under both configurations.
     *
     * @param query the query, without the prefix declarations
     * @param children the number of the children of the join counted by their counts
     * @throws Exception on errors
     */
    private static void assertCounted(String query, int children) throws Exception
    {
        for(SparqlDatabaseConfiguration configuration : List.of(config, optimized))
            assertThat(counted.matcher(translate(configuration, query)).results().map(MatchResult::group).distinct()
                    .count(), is((long) children));
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
     * @param id the number of the IRI
     * @return IRI of the class {@code a}
     */
    private static Iri a(int id)
    {
        return new Iri("http://example.org/a/" + id);
    }


    /**
     * The integer literal.
     *
     * @param value the value
     * @return the integer literal
     */
    private static TypedLiteral integer(long value)
    {
        return new TypedLiteral(Long.toString(value), xsdIntegerIri);
    }


    @Test
    public void counted() throws Exception
    {
        String query = "SELECT (COUNT(*) AS ?c) WHERE { ?s ex:p ?o1 . ?s ex:q ?o2 }";

        // the subjects a/1 to a/5 have 40 objects of ex:p and 30 of ex:q each
        assertCounted(query, 2);
        assertThat(execute(query), containsInAnyOrder(row(integer(6000))));
    }


    @Test
    public void countedPerGroup() throws Exception
    {
        String query = "SELECT ?o (COUNT(*) AS ?c) WHERE { ?s ex:p ?o . ?s ex:q ?o2 } GROUP BY ?o";

        // every object of ex:p is the object of the five subjects with 30 objects of ex:q each; ex:p is grouped by both
        // its variables, which leaves its solutions as they are, so only ex:q is counted
        List<List<RdfTerm>> expected = new ArrayList<>();

        for(int o = 1; o <= 40; o++)
            expected.add(row(a(o), integer(150)));

        assertCounted(query, 1);
        assertThat(execute(query), containsInAnyOrder(expected.toArray()));
    }


    @Test
    public void countedPerSharedVariable() throws Exception
    {
        String query = "SELECT ?s (COUNT(*) AS ?c) WHERE { ?s ex:p ?o1 . ?s ex:q ?o2 } GROUP BY ?s";

        // the subjects a/6 and a/7 have objects of one of the predicates only, so they make no group
        List<List<RdfTerm>> expected = new ArrayList<>();

        for(int s = 1; s <= 5; s++)
            expected.add(row(a(s), integer(1200)));

        assertCounted(query, 2);
        assertThat(execute(query), containsInAnyOrder(expected.toArray()));
    }


    @Test
    public void countedThreeChildren() throws Exception
    {
        String query = "SELECT (COUNT(*) AS ?c) WHERE { ?s ex:p ?o1 . ?s ex:q ?o2 . ?s ex:t ?o3 }";

        assertCounted(query, 3);
        assertThat(execute(query), containsInAnyOrder(row(integer(18000))));
    }


    @Test
    public void countedWithoutSolutions() throws Exception
    {
        String query = "SELECT (COUNT(*) AS ?c) WHERE { ?s ex:p ?o1 . ?s ex:z ?o2 }";
        String grouped = "SELECT ?o1 (COUNT(*) AS ?c) WHERE { ?s ex:p ?o1 . ?s ex:z ?o2 } GROUP BY ?o1";

        // the subject a/100 of ex:z has no object of ex:p: the count is zero, and no group is made
        assertCounted(query, 2);
        assertThat(execute(query), containsInAnyOrder(row(integer(0))));
        assertCounted(grouped, 1);
        assertThat(execute(grouped), empty());
    }


    @Test
    public void countedWithUnboundSharedVariable() throws Exception
    {
        String query = "SELECT (COUNT(*) AS ?c) WHERE { ?s ex:p ?o1 OPTIONAL { ?o1 ex:w ?x } ?x ex:v ?y }";

        // the 11 solutions whose object a/1 or a/2 relates to a/1 by ex:w join the 50 objects of a/1 by ex:v, and the
        // other 190 solutions, which leave ?x unbound, join all 60 solutions of ex:v
        assertCounted(query, 2);
        assertThat(execute(query), containsInAnyOrder(row(integer(11 * 50 + 190 * 60))));
    }


    @Test
    public void notMultiplyingJoin() throws Exception
    {
        String query = "SELECT (COUNT(*) AS ?c) WHERE { ?s ex:p ?o1 . ?s ex:r ?o2 }";

        // every subject has a single object of ex:r, so the join has as many solutions as ex:p
        assertCounted(query, 0);
        assertThat(execute(query), containsInAnyOrder(row(integer(201))));
    }


    @Test
    public void distinctCount() throws Exception
    {
        String query = "SELECT (COUNT(DISTINCT ?o1) AS ?c) WHERE { ?s ex:p ?o1 . ?s ex:q ?o2 }";

        assertCounted(query, 0);
        assertThat(execute(query), containsInAnyOrder(row(integer(40))));
    }


    @Test
    public void childrenNotReduced() throws Exception
    {
        String query = "SELECT ?o1 ?o2 (COUNT(*) AS ?c) WHERE { ?s ex:p ?o1 . ?s ex:q ?o2 } GROUP BY ?o1 ?o2";

        // both children would be grouped by both their variables, which leaves their solutions as they are, so the join
        // is counted as it is, although it multiplies them; every pair of objects has the five subjects a/1 to a/5
        List<List<RdfTerm>> expected = new ArrayList<>();

        for(int o1 = 1; o1 <= 40; o1++)
            for(int o2 = 1; o2 <= 30; o2++)
                expected.add(row(a(o1), a(o2), integer(5)));

        assertCounted(query, 0);
        assertThat(execute(query), containsInAnyOrder(expected.toArray()));
    }
}
