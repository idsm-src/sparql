package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.database.GroupedStatistics;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.imcode.SqlSelect;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Request.PreparedQuery;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.translator.TranslateVisitor;
import cz.iocb.sparql.testing.Database;



/**
 * The join optimizer over a single table holding all the triples in boxes, with the statistics of the table kept per
 * predicate (see {@link GroupedStatistics}), where a constant is an expression putting the term into a box: a term that
 * a filter requires of a variable restricts the rows of the access binding the variable like a constant of the triple
 * pattern does, so the access of a few rows is the outer side of an index lookup; the terms a filter accepts of a
 * variable become a disjunction in the conditions of the access, which the database looks up by an index and the
 * optimizer estimates by the statistics, whereas the filter itself compares the values of boxes. The solutions are
 * those of the joins left to the planner.
 */
public class BoxTableOptimizerTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> ";

    /**
     * The lookup of the links of a node, evaluated for every row of the outer side.
     */
    private static final String lookup = " CROSS JOIN LATERAL (SELECT ";

    /**
     * Configuration with the join optimizer on.
     */
    private static SparqlDatabaseConfiguration optimized = null;

    /**
     * Configuration leaving the joins to the planner.
     */
    private static SparqlDatabaseConfiguration plain = null;


    /**
     * Creates the {@code box_table_test} schema: the table of the triples of 2000 nodes, each with a kind
     * ({@code "rare"} for ten of them, {@code "odd"} for other ten, {@code "common"} for the others) and five links to
     * other nodes, indexed by the predicate with the subject and with the object and analysed, and its statistics per
     * predicate; and the two configurations mapping the table by boxes.
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        String counts = """
                (select p, 's' as column_name, s::text as value, count(*) as c from box_table_test.triples
                group by p, s union all select p, 'o', o::text, count(*) from box_table_test.triples group by p, o)""";

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema box_table_test");
            statement.execute("create table box_table_test.triples (s sparql.rdfbox not null, "
                    + "p sparql.rdfbox not null, o sparql.rdfbox not null)");
            statement.execute("""
                    insert into box_table_test.triples
                    select sparql.rdfbox_create_from_iri('http://example.org/n/' || i),
                        sparql.rdfbox_create_from_iri('http://example.org/kind'),
                        sparql.rdfbox_create_from_string(
                            case when i % 200 = 0 then 'rare' when i % 200 = 100 then 'odd' else 'common' end)
                    from generate_series(1, 2000) as i""");
            statement.execute("""
                    insert into box_table_test.triples
                    select sparql.rdfbox_create_from_iri('http://example.org/n/' || i),
                        sparql.rdfbox_create_from_iri('http://example.org/link'),
                        sparql.rdfbox_create_from_iri('http://example.org/n/' || ((i * 7 + j * 13) % 2000 + 1))
                    from generate_series(1, 2000) as i, generate_series(1, 5) as j""");
            statement.execute("create index on box_table_test.triples (p, s)");
            statement.execute("create index on box_table_test.triples (p, o)");
            statement.execute("analyze box_table_test.triples");
            statement.execute("create table box_table_test.triples_statistics as select p as key_value, column_name, "
                    + "sum(c)::bigint as row_count, count(*)::float8 as distinct_count, "
                    + "(sum(c)^2 / sum(c * c))::float8 as effective_count from " + counts
                    + " t group by p, column_name");
            statement.execute("create table box_table_test.triples_values as select p as key_value, column_name, "
                    + "value, c as row_count from " + counts + " t where c > 1");
        }

        DatabaseSchema schema = new DatabaseSchema(connectionPool);

        optimized = configure(connectionPool, schema);
        optimized.setJoinOptimizer(true);

        plain = configure(connectionPool, schema);
    }


    /**
     * Creates a configuration mapping the table of the triples, with its statistics per predicate.
     */
    private static SparqlDatabaseConfiguration configure(DataSource connectionPool, DatabaseSchema schema)
            throws SQLException
    {
        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);

        config.addPrefix("ex", "http://example.org/");

        DatabaseTable table = new DatabaseTable("box_table_test", "triples");

        config.addQuadMapping(table, null, config.createTermMapping(box, "s"), config.createTermMapping(box, "p"),
                config.createTermMapping(box, "o"));

        config.addTableStatistics(table,
                new GroupedStatistics(table, (TableColumn) config.getColumn(table, "p"),
                        new DatabaseTable("box_table_test", "triples_statistics"),
                        new DatabaseTable("box_table_test", "triples_values")));

        return config;
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
     * Returns the SQL code the query is translated to with the join optimizer on.
     */
    private static String translate(String query) throws Exception
    {
        return translate(optimized, query);
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
     * Checks that the optimized evaluation of the query gives the solutions of the plain one.
     */
    private static List<List<RdfTerm>> check(String query) throws Exception
    {
        List<List<RdfTerm>> expected = execute(plain, query);
        List<List<RdfTerm>> actual = execute(optimized, query);

        assertThat(actual, containsInAnyOrder(expected.toArray()));

        return actual;
    }


    @Test
    public void constantOfThePattern() throws Exception
    {
        String query = "SELECT ?x ?y WHERE { ?x ex:kind \"rare\" . ?x ex:link ?y }";

        assertThat(translate(query), containsString(lookup));
        assertThat(check(query), hasSize(50));
    }


    @Test
    public void termRequiredByAFilter() throws Exception
    {
        // the ten nodes of the kind are the outer side of a lookup of their links
        String rare = "SELECT ?x ?y WHERE { ?x ex:kind ?k . ?x ex:link ?y FILTER(?k = \"rare\") }";

        assertThat(translate(rare), containsString(lookup));
        assertThat(check(rare), hasSize(50));

        // nearly all the nodes are of the other kind, so the sides are hashed
        String common = "SELECT ?x ?y WHERE { ?x ex:kind ?k . ?x ex:link ?y FILTER(?k = \"common\") }";

        assertThat(translate(common), not(containsString(lookup)));
        assertThat(check(common), hasSize(9900));

        // an IRI, which a box holds by another expression than a string
        String node = "SELECT ?x ?k WHERE { ?x ex:kind ?k . ?x ex:link ?y FILTER(?y = <http://example.org/n/21>) }";

        assertThat(translate(node), containsString(lookup));
        assertThat(check(node), hasSize(5));

        // the same term by the identity of terms
        String same = "SELECT ?x ?y WHERE { ?x ex:kind ?k . ?x ex:link ?y FILTER(sameTerm(?k, \"rare\")) }";

        assertThat(translate(same), containsString(lookup));
        assertThat(check(same), hasSize(50));
    }

    @Test
    public void termsAcceptedByAFilter() throws Exception
    {
        String rare = "\"o\" operator(sparql.@=) sparql.rdfbox_create_from_string('rare'::varchar)";
        String odd = "\"o\" operator(sparql.@=) sparql.rdfbox_create_from_string('odd'::varchar)";

        // the twenty nodes of the two kinds are read by the index and are the outer side of a lookup of their links
        String in = "SELECT ?x ?y WHERE { ?x ex:kind ?k . ?x ex:link ?y FILTER(?k IN (\"rare\", \"odd\")) }";

        assertThat(translate(in), containsString(rare));
        assertThat(translate(in), containsString(odd));
        assertThat(translate(in), containsString(lookup));
        assertThat(check(in), hasSize(100));

        // a disjunction of equalities accepts the terms as well
        String or = "SELECT ?x ?y WHERE { ?x ex:kind ?k . ?x ex:link ?y FILTER(?k = \"rare\" || ?k = \"odd\") }";

        assertThat(translate(or), containsString(rare));
        assertThat(translate(or), containsString(lookup));
        assertThat(check(or), hasSize(100));

        // nearly all the nodes are of the accepted kinds, which the statistics tell, so the sides are hashed
        String common = "SELECT ?x ?y WHERE { ?x ex:kind ?k . ?x ex:link ?y FILTER(?k IN (\"common\", \"rare\")) }";

        assertThat(translate(common), containsString(rare));
        assertThat(translate(common), not(containsString(lookup)));
        assertThat(check(common), hasSize(9950));

        // IRIs, whose links are looked up from the accepted objects
        String nodes = "SELECT ?x ?k WHERE { ?x ex:kind ?k . ?x ex:link ?y "
                + "FILTER(?y IN (<http://example.org/n/21>, <http://example.org/n/22>)) }";

        assertThat(translate(nodes), containsString(lookup));
        assertThat(check(nodes), hasSize(10));

        // the filter is evaluated above a grouping, and the access below takes the terms in all the same
        String grouped = "SELECT ?k (COUNT(?x) AS ?c) WHERE { ?x ex:kind ?k } GROUP BY ?k "
                + "HAVING(?k IN (\"rare\", \"odd\"))";

        assertThat(translate(grouped), containsString(rare));
        assertThat(check(grouped), hasSize(2));

        // without the optimizer the database plans by its own estimates, so the terms are left to the filter
        assertThat(translate(plain, in), not(containsString(rare)));
    }
}
