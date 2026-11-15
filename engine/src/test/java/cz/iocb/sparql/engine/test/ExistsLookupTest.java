package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInt;
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
import cz.iocb.sparql.engine.mapping.classes.IntegerUserIriClass;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Request.PreparedQuery;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.translator.TranslateVisitor;
import cz.iocb.sparql.testing.Database;



/**
 * The method the join optimizer of the engine chooses for the test of a pattern: the pattern of an EXISTS or a NOT
 * EXISTS for the solutions of the filter holding it, the right side of a MINUS for the solutions of its left side. The
 * pattern of a few solutions is evaluated for each of them, its table accesses restricted by the values of the solution
 * and fenced by OFFSET 0, so that the database looks them up by the indexes; the pattern of many solutions is evaluated
 * once and hashed, as it is without the optimizer. The solutions are those of the joins left to the planner.
 */
public class ExistsLookupTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> ";

    /**
     * The links restricted to those of the solution a test of a filter is evaluated for.
     */
    private static final String links = "\"exists_lookup_test\".\"links\" WHERE (((\"outer0\".\"id\" = \"src\")))";

    /**
     * Configuration with the join optimizer on.
     */
    private static SparqlDatabaseConfiguration optimized = null;

    /**
     * Configuration leaving the joins to the planner.
     */
    private static SparqlDatabaseConfiguration plain = null;


    /**
     * Creates the {@code exists_lookup_test} schema: the nodes 1 to 1000 with labels and with scores equal to their
     * numbers, 2000 links among the nodes 1 to 200 (every node links to ten), five nodes tagged, all indexed, and notes
     * of the nodes 1 to 500 with no index, all analysed; and the two configurations mapping {@code ex:label},
     * {@code ex:score}, {@code ex:link}, {@code ex:tag} and {@code ex:note} over them.
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema exists_lookup_test");
            statement.execute("create table exists_lookup_test.nodes (id integer primary key, label varchar not null)");
            statement.execute(
                    "insert into exists_lookup_test.nodes select i, 'node ' || i from generate_series(1, 1000) as i");
            statement.execute("create table exists_lookup_test.links (src integer not null, dst integer not null)");
            statement.execute("insert into exists_lookup_test.links select i % 200 + 1, "
                    + "(i * 7 + i / 200) % 200 + 1 from generate_series(1, 2000) as i");
            statement.execute("create index on exists_lookup_test.links (src)");
            statement.execute("create index on exists_lookup_test.links (dst)");
            statement.execute("create table exists_lookup_test.few (id integer not null, tag varchar not null)");
            statement.execute(
                    "insert into exists_lookup_test.few select i * 3, 'tag ' || i from generate_series(1, 5) as i");
            statement.execute("create index on exists_lookup_test.few (id)");
            statement
                    .execute("create table exists_lookup_test.scores (id integer primary key, score integer not null)");
            statement.execute("insert into exists_lookup_test.scores select i, i from generate_series(1, 1000) as i");
            statement.execute("create index on exists_lookup_test.scores (score)");
            statement.execute("create table exists_lookup_test.notes (id integer not null, note varchar not null)");
            statement.execute(
                    "insert into exists_lookup_test.notes select i, 'note ' || i from generate_series(1, 500) as i");
            statement.execute("""
                    analyze exists_lookup_test.nodes, exists_lookup_test.links, exists_lookup_test.few, \
                    exists_lookup_test.scores, exists_lookup_test.notes""");
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

        DatabaseTable nodes = new DatabaseTable("exists_lookup_test", "nodes");
        DatabaseTable links = new DatabaseTable("exists_lookup_test", "links");
        DatabaseTable few = new DatabaseTable("exists_lookup_test", "few");
        DatabaseTable scores = new DatabaseTable("exists_lookup_test", "scores");
        DatabaseTable notes = new DatabaseTable("exists_lookup_test", "notes");

        config.addQuadMapping(nodes, null, config.createIriMapping("node", "id"), config.createIriMapping("ex:label"),
                config.createLiteralMapping(xsdString, "label"));
        config.addQuadMapping(links, null, config.createIriMapping("node", "src"), config.createIriMapping("ex:link"),
                config.createIriMapping("node", "dst"));
        config.addQuadMapping(few, null, config.createIriMapping("node", "id"), config.createIriMapping("ex:tag"),
                config.createLiteralMapping(xsdString, "tag"));
        config.addQuadMapping(scores, null, config.createIriMapping("node", "id"), config.createIriMapping("ex:score"),
                config.createLiteralMapping(xsdInt, "score"));
        config.addQuadMapping(notes, null, config.createIriMapping("node", "id"), config.createIriMapping("ex:note"),
                config.createLiteralMapping(xsdString, "note"));

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
    public void exists() throws Exception
    {
        String query = "SELECT ?x WHERE { ?x ex:tag ?t FILTER EXISTS { ?x ex:link ?y } }";

        // the links of each of the five tagged nodes are looked up by the index of their source
        String sql = translate(optimized, query);
        assertThat(sql, containsString(" WHERE EXISTS (SELECT 1 FROM (SELECT "));
        assertThat(sql, containsString(links + " OFFSET 0) AS \"tab\" WHERE "));

        // the planner is left to itself without the optimizer
        assertThat(translate(plain, query), not(containsString(" OFFSET 0")));

        assertThat(check(query), hasSize(5));
    }


    @Test
    public void notExists() throws Exception
    {
        // the nodes 199 and 200 link to other nodes, the nodes 201 and 202 do not
        String query = "SELECT ?x WHERE { ?x ex:score ?s FILTER(?s > 198 && ?s < 203) "
                + "FILTER NOT EXISTS { ?x ex:link ?y } }";

        String sql = translate(optimized, query);
        assertThat(sql, containsString(" AND NOT EXISTS (SELECT 1 FROM (SELECT "));
        assertThat(sql, containsString(links + " OFFSET 0) AS \"tab\" WHERE "));

        assertThat(check(query), hasSize(2));
    }


    @Test
    public void manySolutions() throws Exception
    {
        // the links are not many more than the thousand nodes, so the sides are hashed
        String query = "SELECT ?x WHERE { ?x ex:label ?l FILTER EXISTS { ?x ex:link ?y } }";

        assertThat(translate(optimized, query), not(containsString(" OFFSET 0")));
        assertThat(check(query), hasSize(200));

        // a filter leaves a few of the nodes, for which the links are looked up
        query = "SELECT ?x WHERE { ?x ex:label ?l FILTER(?l = 'node 7' || ?l = 'node 777') "
                + "FILTER EXISTS { ?x ex:link ?y } }";

        assertThat(translate(optimized, query), containsString(links + " OFFSET 0) AS \"tab\" WHERE "));
        assertThat(check(query), hasSize(1));
    }


    @Test
    public void noIndex() throws Exception
    {
        // the notes have no index to look them up by, so they would be scanned for every tagged node
        String query = "SELECT ?x WHERE { ?x ex:tag ?t FILTER EXISTS { ?x ex:note ?n } }";

        assertThat(translate(optimized, query), not(containsString(" OFFSET 0")));
        assertThat(check(query), hasSize(5));
    }


    @Test
    public void join() throws Exception
    {
        String query = "SELECT ?x WHERE { ?x ex:tag ?t "
                + "FILTER EXISTS { ?x ex:link ?y . ?y ex:score ?s FILTER(?s > 150) } }";

        // the links of a tagged node are looked up, and the scores of their targets after them
        String sql = translate(optimized, query);
        assertThat(sql, containsString(links + " OFFSET 0) AS "));
        assertThat(sql, containsString(" CROSS JOIN LATERAL (SELECT "));

        check(query);
    }


    @Test
    public void nested() throws Exception
    {
        // the links of a tagged node are looked up, and the tag of each of their targets as well
        String query = "SELECT ?x WHERE { ?x ex:tag ?t "
                + "FILTER EXISTS { ?x ex:link ?y FILTER NOT EXISTS { ?y ex:tag ?u } } }";

        assertThat(translate(optimized, query), containsString(links + " OFFSET 0 ) AS \"outer1\" WHERE NOT EXISTS "));
        assertThat(check(query), hasSize(5));
    }


    @Test
    public void unrestricted() throws Exception
    {
        // the pattern shares no variable with the solutions it is tested for
        String query = "SELECT ?x WHERE { ?x ex:tag ?t FILTER EXISTS { ?y ex:tag 'tag 1' } }";

        assertThat(translate(optimized, query), not(containsString(" OFFSET 0")));
        assertThat(check(query), hasSize(5));

        // a pattern that is not deterministic is evaluated once
        query = "SELECT ?x WHERE { ?x ex:tag ?t FILTER EXISTS { ?x ex:link ?y FILTER(RAND() < 2) } }";

        assertThat(translate(optimized, query), not(containsString(" OFFSET 0")));
        assertThat(execute(optimized, query), hasSize(5));

        // a variable the solutions may leave unbound restricts nothing
        query = "SELECT ?x ?y WHERE { ?x ex:score ?s FILTER(?s > 198 && ?s < 203) OPTIONAL { ?x ex:link ?y } "
                + "FILTER EXISTS { ?y ex:tag ?u } }";

        assertThat(translate(optimized, query), not(containsString("\"exists_lookup_test\".\"few\" WHERE ")));
        check(query);
    }


    @Test
    public void minus() throws Exception
    {
        // the nodes 199 and 200 link to other nodes, the nodes 201 and 202 do not
        String query = "SELECT ?x WHERE { ?x ex:score ?s FILTER(?s > 198 && ?s < 203) MINUS { ?x ex:link ?y } }";

        // the links of each of the nodes are looked up by the index of their source
        String sql = translate(optimized, query);
        assertThat(sql, containsString(") AS \"left0\" WHERE NOT EXISTS (SELECT 1 FROM (SELECT "));
        assertThat(sql, containsString("\"exists_lookup_test\".\"links\" WHERE (((\"left0\".\"id\" = \"src\"))) "
                + "OFFSET 0) AS \"tab1\" WHERE "));

        assertThat(translate(plain, query), not(containsString(" OFFSET 0")));
        assertThat(check(query), hasSize(2));

        // the links are not many more than the thousand nodes, so the sides are hashed
        query = "SELECT ?x WHERE { ?x ex:label ?l MINUS { ?x ex:link ?y } }";

        assertThat(translate(optimized, query), not(containsString(" OFFSET 0")));
        assertThat(check(query), hasSize(800));

        // the links of a tagged node are looked up and counted
        query = "SELECT ?x WHERE { ?x ex:tag ?t "
                + "MINUS { SELECT ?x WHERE { ?x ex:link ?y } GROUP BY ?x HAVING (COUNT(?y) > 10) } }";

        sql = translate(optimized, query);
        assertThat(sql,
                containsString("\"exists_lookup_test\".\"links\" WHERE (((\"left0\".\"id\" = \"src\"))) OFFSET 0"));

        assertThat(check(query), hasSize(5));

        // a difference in the difference refers to the solutions of its own left side
        query = "SELECT ?x ?y WHERE { ?x ex:tag ?t . ?x ex:link ?y MINUS { ?y ex:link ?z MINUS { ?z ex:tag ?u } } }";

        check(query);
    }
}
