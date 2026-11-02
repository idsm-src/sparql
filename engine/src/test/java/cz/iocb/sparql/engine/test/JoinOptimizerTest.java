package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
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
import cz.iocb.sparql.engine.imcode.SqlSelect;
import cz.iocb.sparql.engine.mapping.ConstantIriMapping;
import cz.iocb.sparql.engine.mapping.classes.IntegerUserIriClass;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Request.PreparedQuery;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.translator.TranslateVisitor;
import cz.iocb.sparql.testing.Database;



/**
 * The join optimizer of the engine: a few rows joined with many are the outer side of an index lookup of the many,
 * written as a lateral subquery fenced by OFFSET 0; two big sides are joined by an explicit hash join; the solutions
 * are those of the joins left to the planner, also for an OPTIONAL, a MINUS and a property path evaluated under the
 * settings the optimizer makes.
 */
public class JoinOptimizerTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> ";

    /**
     * Configuration with the join optimizer on.
     */
    private static SparqlDatabaseConfiguration optimized = null;

    /**
     * Configuration leaving the joins to the planner.
     */
    private static SparqlDatabaseConfiguration plain = null;


    /**
     * Creates the {@code join_optimizer_test} schema: the nodes 1 to 1000 with labels, 2000 links among the nodes 1 to
     * 200 (every node links to ten), and five nodes tagged, all indexed and analysed; and the two configurations
     * mapping {@code ex:label}, {@code ex:link} and {@code ex:tag} over them.
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema join_optimizer_test");
            statement
                    .execute("create table join_optimizer_test.nodes (id integer primary key, label varchar not null)");
            statement.execute("insert into join_optimizer_test.nodes select i, 'node ' || i "
                    + "from generate_series(1, 1000) as i");
            statement.execute("create table join_optimizer_test.links (src integer not null, dst integer not null)");
            statement.execute("insert into join_optimizer_test.links select i % 200 + 1, "
                    + "(i * 7 + i / 200) % 200 + 1 from generate_series(1, 2000) as i");
            statement.execute("create index on join_optimizer_test.links (src)");
            statement.execute("create index on join_optimizer_test.links (dst)");
            statement.execute("create table join_optimizer_test.few (id integer not null, tag varchar not null)");
            statement.execute(
                    "insert into join_optimizer_test.few select i, 'tag ' || i from generate_series(1, 5) " + "as i");
            statement.execute("create index on join_optimizer_test.few (id)");
            statement.execute("analyze join_optimizer_test.nodes, join_optimizer_test.links, join_optimizer_test.few");
        }

        DatabaseSchema schema = new DatabaseSchema(connectionPool);

        optimized = configure(connectionPool, schema);
        optimized.setJoinOptimizer(true);

        plain = configure(connectionPool, schema);
    }


    /**
     * Creates a configuration mapping the test tables.
     */
    private static SparqlDatabaseConfiguration configure(DataSource connectionPool, DatabaseSchema schema)
            throws SQLException
    {
        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);

        config.addPrefix("ex", "http://example.org/");
        config.addIriClass(new IntegerUserIriClass("node", INT4, "http://example.org/n/"));

        ConstantIriMapping graph = config.createIriMapping("<http://example.org/graph>");
        DatabaseTable nodes = new DatabaseTable("join_optimizer_test", "nodes");
        DatabaseTable links = new DatabaseTable("join_optimizer_test", "links");
        DatabaseTable few = new DatabaseTable("join_optimizer_test", "few");

        config.addQuadMapping(nodes, graph, config.createIriMapping("node", "id"), config.createIriMapping("ex:label"),
                config.createLiteralMapping(xsdString, "label"));
        config.addQuadMapping(links, graph, config.createIriMapping("node", "src"), config.createIriMapping("ex:link"),
                config.createIriMapping("node", "dst"));
        config.addQuadMapping(few, graph, config.createIriMapping("node", "id"), config.createIriMapping("ex:tag"),
                config.createLiteralMapping(xsdString, "tag"));

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
    public void lookupOfTheManyFromTheFew() throws Exception
    {
        String query = "SELECT ?x ?y WHERE { ?x ex:tag ?t . ?x ex:link ?y }";

        String sql = translate(optimized, query);
        assertThat(sql, containsString(" CROSS JOIN LATERAL (SELECT "));
        assertThat(sql, containsString("\"join_optimizer_test\".\"links\" WHERE "));
        assertThat(sql, containsString(" OFFSET 0) AS "));
        assertThat(sql, not(containsString("\"join_optimizer_test\".\"few\" WHERE ")));

        assertThat(check(query), hasSize(50));
    }


    @Test
    public void hashJoinOfTheMany() throws Exception
    {
        String query = "SELECT ?x ?z WHERE { ?x ex:link ?y . ?y ex:link ?z }";

        String sql = translate(optimized, query);
        assertThat(sql, containsString(" JOIN (SELECT "));
        assertThat(sql, containsString(" ON ("));
        assertThat(sql, not(containsString("LATERAL")));

        assertThat(check(query), hasSize(20000));
    }


    @Test
    public void treeOfThree() throws Exception
    {
        String query = "SELECT ?x ?l ?y WHERE { ?x ex:tag ?t . ?x ex:label ?l . ?x ex:link ?y }";

        String sql = translate(optimized, query);
        assertThat(sql, containsString(" CROSS JOIN LATERAL ("));

        assertThat(check(query), hasSize(50));
    }


    @Test
    public void optionalAndMinus() throws Exception
    {
        assertThat(check("SELECT ?x ?y WHERE { ?x ex:tag ?t OPTIONAL { ?x ex:link ?y } }"), hasSize(50));
        assertThat(check("SELECT ?x WHERE { ?x ex:label ?l MINUS { ?x ex:link ?y } }"), hasSize(800));
        assertThat(check("SELECT ?x WHERE { ?x ex:label ?l FILTER NOT EXISTS { ?x ex:link ?y } }"), hasSize(800));
    }


    @Test
    public void propertyPath() throws Exception
    {
        String query = "SELECT DISTINCT ?y WHERE { <http://example.org/n/1> ex:link+ ?y }";

        String sql = translate(optimized, query);
        assertThat(sql, containsString("recursion AS "));
        assertThat(sql, containsString(" CROSS JOIN LATERAL ("));

        check(query);
    }


    @Test
    public void values() throws Exception
    {
        String query = "SELECT ?x ?y WHERE { VALUES ?x { <http://example.org/n/1> <http://example.org/n/2> } "
                + "?x ex:link ?y }";

        assertThat(check(query), hasSize(20));
    }
}
