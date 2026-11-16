package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDouble;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
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
import cz.iocb.sparql.engine.mapping.TermMapping;
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
 * {@code COUNT(DISTINCT *)} counts the distinct solutions of a group: two solutions are the same when they bind the
 * same variables to the same terms, a variable that both leave unbound included, so a solution leaving every variable
 * unbound counts as well, and the solutions of a pattern without variables are all the same. The blank nodes of a
 * pattern are no variables of its solutions, and the zeros of the two signs are different terms.
 *
 * The table {@code link} holds the rows (1, 2), (1, 2), (1, 3) and (4, 5) of items, the table {@code num} holds the
 * numbers 0, -0 and 0 of the item 1; neither of them is deduplicated.
 */
public class CountDistinctSolutionsTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> PREFIX item: <http://example.org/item/> ";

    /**
     * Configuration mapping the tables.
     */
    private static SparqlDatabaseConfiguration config = null;

    /**
     * The same configuration with the join optimizer enabled.
     */
    private static SparqlDatabaseConfiguration optimized = null;


    /**
     * Creates the {@code count_distinct_solutions_test} schema with the tables {@code link} and {@code num} and maps
     * the rows of the first one by ex:link between items and those of the second one by ex:num from an item to an
     * xsd:double.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema count_distinct_solutions_test");
            statement.execute("create table count_distinct_solutions_test.link (s int not null, o int not null)");
            statement.execute("insert into count_distinct_solutions_test.link values (1, 2), (1, 2), (1, 3), (4, 5)");
            statement.execute("create table count_distinct_solutions_test.num (s int not null, d float8 not null)");
            statement.execute("insert into count_distinct_solutions_test.num values (1, '0'), (1, '-0'), (1, '0')");
        }

        DatabaseSchema schema = new DatabaseSchema(connectionPool);

        config = configuration(connectionPool, schema, false);
        optimized = configuration(connectionPool, schema, true);
    }


    /**
     * Configuration mapping the tables.
     *
     * @param connectionPool pool of the test database
     * @param schema schema of the test database
     * @param joinOptimizer whether the join optimizer is enabled
     * @return configuration mapping the tables
     * @throws SQLException on database errors
     */
    private static SparqlDatabaseConfiguration configuration(DataSource connectionPool, DatabaseSchema schema,
            boolean joinOptimizer) throws SQLException
    {
        SparqlDatabaseConfiguration configuration = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);
        configuration.addPrefix("ex", "http://example.org/");
        configuration.addIriClass(new IntegerUserIriClass("item", INT4, "http://example.org/item/"));

        DatabaseTable link = new DatabaseTable("count_distinct_solutions_test", "link");
        DatabaseTable num = new DatabaseTable("count_distinct_solutions_test", "num");
        TermMapping subject = configuration.createIriMapping("item", "s");

        configuration.addQuadMapping(link, null, subject, configuration.createIriMapping("ex:link"),
                configuration.createIriMapping("item", "o"));
        configuration.addQuadMapping(num, null, subject, configuration.createIriMapping("ex:num"),
                configuration.createLiteralMapping(xsdDouble, "d"));
        configuration.setJoinOptimizer(joinOptimizer);

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
        try(Request request = new Engine(configuration).getRequest(); Result result = request.execute(prefix + query))
        {
            List<List<RdfTerm>> rows = new ArrayList<>();

            while(result.next())
                rows.add(Arrays.asList(result.getRow()));

            return rows;
        }
    }


    /**
     * Row of xsd:integer literals.
     *
     * @param values the values of the literals
     * @return row of the literals
     */
    private static List<RdfTerm> counts(int... values)
    {
        return Arrays.stream(values).mapToObj(v -> (RdfTerm) new TypedLiteral(Integer.toString(v), xsdIntegerIri))
                .toList();
    }


    /**
     * Row of an item and an xsd:integer literal.
     *
     * @param id identifier of the item
     * @param count the value of the literal
     * @return row of the item and the literal
     */
    private static List<RdfTerm> group(int id, int count)
    {
        return List.of(new Iri("http://example.org/item/" + id),
                new TypedLiteral(Integer.toString(count), xsdIntegerIri));
    }


    @Test
    @DisplayName("duplicate solutions are counted once")
    void duplicates() throws Exception
    {
        assertThat(execute("SELECT (COUNT(DISTINCT *) AS ?c) WHERE { VALUES ?x { 1 1 2 } }"), contains(counts(2)));

        assertThat(execute("SELECT (COUNT(DISTINCT *) AS ?c) WHERE { ?s ex:link ?o }"), contains(counts(3)));

        assertThat(execute("SELECT (COUNT(DISTINCT *) AS ?c) (COUNT(*) AS ?n) WHERE { ?s ex:link ?o }"),
                contains(counts(3, 4)));

        assertThat(execute("SELECT (COUNT(DISTINCT *) AS ?c) WHERE { { ?s ex:link ?o } UNION { ?s ex:link ?o } }"),
                contains(counts(3)));
    }


    @Test
    @DisplayName("the distinct solutions of every group")
    void groups() throws Exception
    {
        assertThat(execute("SELECT ?s (COUNT(DISTINCT *) AS ?c) WHERE { ?s ex:link ?o } GROUP BY ?s"),
                containsInAnyOrder(group(1, 2), group(4, 1)));

        assertThat(execute("SELECT ?s WHERE { ?s ex:link ?o } GROUP BY ?s HAVING(COUNT(DISTINCT *) = 2)"),
                contains(List.of(new Iri("http://example.org/item/1"))));
    }


    @Test
    @DisplayName("the blank nodes of a pattern are no variables of its solutions")
    void blankNodes() throws Exception
    {
        assertThat(execute("SELECT (COUNT(DISTINCT *) AS ?c) (COUNT(*) AS ?n) WHERE { ?s ex:link [] }"),
                contains(counts(2, 4)));
    }


    @Test
    @DisplayName("a variable that solutions leave unbound")
    void unboundVariables() throws Exception
    {
        assertThat(execute("SELECT (COUNT(DISTINCT *) AS ?c) WHERE { VALUES ?x { UNDEF UNDEF } }"),
                contains(counts(1)));

        // the solution leaving the variable unbound is one of the two distinct solutions
        assertThat(execute("SELECT (COUNT(DISTINCT *) AS ?c) WHERE { VALUES ?x { UNDEF 1 1 } }"), contains(counts(2)));

        assertThat(execute("SELECT (COUNT(DISTINCT *) AS ?c) WHERE { VALUES (?x ?y) { (UNDEF 1) (UNDEF 1) (1 UNDEF) "
                + "(1 UNDEF) (UNDEF UNDEF) } }"), contains(counts(3)));

        assertThat(execute("SELECT (COUNT(DISTINCT *) AS ?c) WHERE { VALUES ?s { item:1 item:1 item:9 } "
                + "OPTIONAL { ?s ex:link ?o FILTER(?o = item:5) } }"), contains(counts(2)));
    }


    @Test
    @DisplayName("the zeros of the two signs are different terms")
    void zeros() throws Exception
    {
        assertThat(execute("SELECT (COUNT(DISTINCT *) AS ?c) WHERE { ?s ex:num ?d }"), contains(counts(2)));
    }


    @Test
    @DisplayName("the solutions of a pattern without variables are all the same")
    void noVariables() throws Exception
    {
        assertThat(execute("SELECT (COUNT(DISTINCT *) AS ?c) (COUNT(*) AS ?n) WHERE { item:1 ex:link item:2 }"),
                contains(counts(1, 2)));
    }
}
