package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.iri;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
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
 * The method the join optimizer of the engine chooses for an OPTIONAL: the right side of a few solutions of the left
 * side is evaluated for each of them, as a lateral subquery whose table accesses are restricted by the values of the
 * solution and fenced by OFFSET 0, so that the database looks them up by the indexes; the right side of many solutions
 * is evaluated once and hashed, as it is without the optimizer. A variable the left side may leave unbound restricts
 * the right side for the solutions that bind it, which the subquery tells from the others. The solutions are those of
 * the joins left to the planner.
 */
public class OptionalLookupTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> ";

    /**
     * The start of the right side of a left join that is evaluated for every solution of the left side.
     */
    private static final String lookup = " LEFT JOIN LATERAL (";

    /**
     * Configuration with the join optimizer on.
     */
    private static SparqlDatabaseConfiguration optimized = null;

    /**
     * Configuration leaving the joins to the planner.
     */
    private static SparqlDatabaseConfiguration plain = null;


    /**
     * Creates the {@code optional_lookup_test} schema: the nodes 1 to 1000 with labels and with scores equal to their
     * numbers, 2000 links among the nodes 1 to 200 (every node links to ten), five nodes tagged, aliases of the nodes
     * holding their IRIs whole, marks of the nodes 1 to 200, all indexed, and notes of the nodes 1 to 500 with no
     * index, all analysed; and the two configurations mapping {@code ex:label}, {@code ex:score}, {@code ex:link},
     * {@code ex:tag}, {@code ex:alias}, {@code ex:note} and {@code ex:mark} over them.
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema optional_lookup_test");
            statement.execute(
                    "create table optional_lookup_test.nodes (id integer primary key, label varchar not null)");
            statement.execute("insert into optional_lookup_test.nodes select i, 'node ' || i "
                    + "from generate_series(1, 1000) as i");
            statement.execute("create table optional_lookup_test.links (src integer not null, dst integer not null)");
            statement.execute("insert into optional_lookup_test.links select i % 200 + 1, "
                    + "(i * 7 + i / 200) % 200 + 1 from generate_series(1, 2000) as i");
            statement.execute("create index on optional_lookup_test.links (src)");
            statement.execute("create index on optional_lookup_test.links (dst)");
            statement.execute("create table optional_lookup_test.few (id integer not null, tag varchar not null)");
            statement.execute(
                    "insert into optional_lookup_test.few select i * 3, 'tag ' || i from generate_series(1, 5) as i");
            statement.execute("create index on optional_lookup_test.few (id)");
            statement.execute(
                    "create table optional_lookup_test.scores (id integer primary key, score integer not null)");
            statement.execute("insert into optional_lookup_test.scores select i, i from generate_series(1, 1000) as i");
            statement.execute("create index on optional_lookup_test.scores (score)");
            statement.execute(
                    "create table optional_lookup_test.aliases (iri varchar not null, alias varchar not null)");
            statement.execute("insert into optional_lookup_test.aliases select 'http://example.org/n/' || i, "
                    + "'alias ' || i from generate_series(1, 1000) as i");
            statement
                    .execute("insert into optional_lookup_test.aliases values ('http://example.org/other', 'no node')");
            statement.execute("create index on optional_lookup_test.aliases (iri)");
            statement.execute("create table optional_lookup_test.notes (id integer not null, note varchar not null)");
            statement.execute(
                    "insert into optional_lookup_test.notes select i, 'note ' || i from generate_series(1, 500) as i");
            statement
                    .execute("create table optional_lookup_test.marks (id integer primary key, mark varchar not null)");
            statement.execute(
                    "insert into optional_lookup_test.marks select i, 'mark ' || i from generate_series(1, 200) as i");
            statement.execute("""
                    analyze optional_lookup_test.nodes, optional_lookup_test.links, optional_lookup_test.few, \
                    optional_lookup_test.scores, optional_lookup_test.aliases, optional_lookup_test.notes, \
                    optional_lookup_test.marks""");
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

        DatabaseTable nodes = new DatabaseTable("optional_lookup_test", "nodes");
        DatabaseTable links = new DatabaseTable("optional_lookup_test", "links");
        DatabaseTable few = new DatabaseTable("optional_lookup_test", "few");
        DatabaseTable scores = new DatabaseTable("optional_lookup_test", "scores");
        DatabaseTable aliases = new DatabaseTable("optional_lookup_test", "aliases");
        DatabaseTable notes = new DatabaseTable("optional_lookup_test", "notes");
        DatabaseTable marks = new DatabaseTable("optional_lookup_test", "marks");

        config.addQuadMapping(nodes, null, config.createIriMapping("node", "id"), config.createIriMapping("ex:label"),
                config.createLiteralMapping(xsdString, "label"));
        config.addQuadMapping(links, null, config.createIriMapping("node", "src"), config.createIriMapping("ex:link"),
                config.createIriMapping("node", "dst"));
        config.addQuadMapping(few, null, config.createIriMapping("node", "id"), config.createIriMapping("ex:tag"),
                config.createLiteralMapping(xsdString, "tag"));
        config.addQuadMapping(scores, null, config.createIriMapping("node", "id"), config.createIriMapping("ex:score"),
                config.createLiteralMapping(xsdInt, "score"));
        config.addQuadMapping(aliases, null, config.createIriMapping(iri, "iri"), config.createIriMapping("ex:alias"),
                config.createLiteralMapping(xsdString, "alias"));
        config.addQuadMapping(notes, null, config.createIriMapping("node", "id"), config.createIriMapping("ex:note"),
                config.createLiteralMapping(xsdString, "note"));
        config.addQuadMapping(marks, null, config.createIriMapping("node", "id"), config.createIriMapping("ex:mark"),
                config.createLiteralMapping(xsdString, "mark"));

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


    /**
     * Returns the number of the occurrences of the text in the SQL code.
     */
    private static int count(String sql, String text)
    {
        return (sql.length() - sql.replace(text, "").length()) / text.length();
    }


    @Test
    public void tableAccess() throws Exception
    {
        String query = "SELECT ?x ?y WHERE { ?x ex:tag ?t OPTIONAL { ?x ex:link ?y } }";

        // the links of the five tagged nodes are looked up by the index of their source
        String sql = translate(optimized, query);
        assertThat(sql, containsString(lookup + "SELECT "));
        assertThat(sql, containsString("\"optional_lookup_test\".\"links\" WHERE (((\"left0\".\"id\" = \"src\")))"));
        assertThat(sql, containsString(" OFFSET 0 ) AS \"right0\" ON "));

        // the planner is left to itself without the optimizer
        assertThat(translate(plain, query), not(containsString("LATERAL")));

        // the tagged nodes 3, 6, 9, 12 and 15 link to ten nodes each
        assertThat(check(query), hasSize(50));

        // a node that links to no other node is kept
        assertThat(check("SELECT ?x ?t ?l WHERE { ?x ex:label ?l FILTER(?l = 'node 777') OPTIONAL { ?x ex:tag ?t } }"),
                hasSize(1));
    }


    @Test
    public void manySolutions() throws Exception
    {
        // the links are not many more than the thousand nodes, so the sides are hashed
        String query = "SELECT ?x ?y WHERE { ?x ex:label ?l OPTIONAL { ?x ex:link ?y } }";

        assertThat(translate(optimized, query), not(containsString("LATERAL")));
        assertThat(check(query), hasSize(2800));
    }


    @Test
    public void noIndex() throws Exception
    {
        // the notes have no index to look them up by, so they would be scanned for every tagged node
        String query = "SELECT ?x ?n WHERE { ?x ex:tag ?t OPTIONAL { ?x ex:note ?n } }";

        assertThat(translate(optimized, query), not(containsString("LATERAL")));
        assertThat(check(query), hasSize(5));
    }


    @Test
    public void join() throws Exception
    {
        String query = "SELECT ?x ?y ?l WHERE { ?x ex:tag ?t OPTIONAL { ?x ex:link ?y . ?y ex:label ?l } }";

        // the links of a tagged node are looked up, and the labels of their targets after them
        String sql = translate(optimized, query);
        assertThat(sql, containsString(lookup + "SELECT "));
        assertThat(sql, containsString("\"optional_lookup_test\".\"links\" WHERE (((\"left0\".\"id\" = \"src\")))"));
        assertThat(sql, containsString(" CROSS JOIN LATERAL (SELECT "));
        assertThat(sql, containsString("\"optional_lookup_test\".\"nodes\" WHERE "));

        assertThat(check(query), hasSize(50));

        // the member of the join that the solution of the left side does not restrict is looked up from the other one
        query = "SELECT ?x ?y ?s WHERE { ?x ex:tag ?t OPTIONAL { ?y ex:score ?s . ?x ex:link ?y FILTER(?s > 100) } }";

        assertThat(translate(optimized, query), containsString(lookup + "SELECT "));
        assertThat(check(query), hasSize(12));
    }


    @Test
    public void conditions() throws Exception
    {
        // the filter of the OPTIONAL stays the condition of the join
        String query = "SELECT ?x ?y WHERE { ?x ex:tag ?t OPTIONAL { ?x ex:link ?y FILTER(?y != ?x && ?t != 'tag 2') } }";

        String sql = translate(optimized, query);
        assertThat(sql, containsString(lookup + "SELECT "));
        assertThat(sql, containsString(" OFFSET 0 ) AS \"right0\" ON "));

        assertThat(check(query), hasSize(41));
    }


    @Test
    public void filterAbove() throws Exception
    {
        // the scores are as many as the links are, so the sides are hashed
        assertThat(translate(optimized, "SELECT ?x ?y WHERE { ?x ex:score ?s OPTIONAL { ?x ex:link ?y } }"),
                not(containsString("LATERAL")));

        // a filter above the OPTIONAL leaves few of the scores, which the database finds before the join
        String query = "SELECT ?x ?y WHERE { ?x ex:score ?s OPTIONAL { ?x ex:link ?y } FILTER(?s < 5) }";

        assertThat(translate(optimized, query), containsString(lookup + "SELECT "));
        assertThat(check(query), hasSize(40));

        // a wide range leaves most of them
        query = "SELECT ?x ?y WHERE { ?x ex:score ?s OPTIONAL { ?x ex:link ?y } FILTER(?s > 100) }";

        assertThat(translate(optimized, query), not(containsString("LATERAL")));
        assertThat(check(query), hasSize(1800));

        // a condition on a variable of the right side is evaluated after the join
        query = "SELECT ?x ?y WHERE { ?x ex:score ?s OPTIONAL { ?x ex:link ?y } FILTER(!bound(?y)) }";

        assertThat(translate(optimized, query), not(containsString("LATERAL")));
        assertThat(check(query), hasSize(800));
    }


    @Test
    public void conditionOnLeftVariable() throws Exception
    {
        // the filter of the OPTIONAL tests a variable of the right side, so the database evaluates it there and looks
        // the links up for the few labels it leaves
        String query = "SELECT ?x ?y ?l WHERE { ?x ex:score ?s OPTIONAL { ?x ex:label ?l . ?x ex:link ?y "
                + "FILTER(STRENDS(?l, ' 77')) } }";

        assertThat(translate(optimized, query), containsString(" CROSS JOIN LATERAL (SELECT "));
        assertThat(check(query), hasSize(1009));

        // a variable the left side binds is taken from that side, so the database tests it only when it joins the
        // sides, and the labels and the links are hashed whole
        query = "SELECT ?x ?y ?l WHERE { ?x ex:score ?s OPTIONAL { ?x ex:label ?l . ?x ex:link ?y "
                + "FILTER(STRENDS(STR(?x), '/77')) } }";

        assertThat(translate(optimized, query), not(containsString("LATERAL")));
        assertThat(check(query), hasSize(1009));
    }


    @Test
    public void nested() throws Exception
    {
        String query = "SELECT ?x ?y ?l WHERE { ?x ex:tag ?t OPTIONAL { ?x ex:link ?y OPTIONAL { ?y ex:label ?l } } }";

        // the links are looked up for a tagged node, and the labels for each of the links found
        String sql = translate(optimized, query);
        assertThat(count(sql, lookup), is(2));
        assertThat(sql, containsString("\"optional_lookup_test\".\"links\" WHERE (((\"left0\".\"id\" = \"src\")))"));
        assertThat(sql, containsString("\"optional_lookup_test\".\"nodes\" WHERE (((\"left0\".\"dst\" = \"id\")))"));

        assertThat(check(query), hasSize(50));

        // the five tags of the links found for a tagged node are hashed, as they are fewer than the links
        query = "SELECT ?x ?y ?u WHERE { ?x ex:tag ?t OPTIONAL { ?x ex:link ?y OPTIONAL { ?y ex:tag ?u } } }";

        assertThat(count(translate(optimized, query), lookup), is(1));
        assertThat(check(query), hasSize(50));
    }


    @Test
    public void union() throws Exception
    {
        String query = "SELECT ?x ?y WHERE { ?x ex:tag ?t OPTIONAL { { ?x ex:link ?y } UNION { ?y ex:link ?x } } }";

        // each branch is looked up by its own index
        String sql = translate(optimized, query);
        assertThat(count(sql, lookup), is(1));
        assertThat(sql, containsString("\"optional_lookup_test\".\"links\" WHERE (((\"left0\".\"id\" = \"src\")))"));
        assertThat(sql, containsString("\"optional_lookup_test\".\"links\" WHERE (((\"left0\".\"id\" = \"dst\")))"));

        assertThat(check(query), hasSize(100));
    }


    @Test
    public void binding() throws Exception
    {
        String query = "SELECT ?x ?y ?z WHERE { ?x ex:tag ?t OPTIONAL { ?x ex:link ?y BIND(STR(?y) AS ?z) } }";

        assertThat(translate(optimized, query), containsString(lookup + "SELECT "));
        assertThat(check(query), hasSize(50));

        // a right side that is not deterministic is evaluated once
        query = "SELECT ?x ?y ?z WHERE { ?x ex:tag ?t OPTIONAL { ?x ex:link ?y BIND(RAND() AS ?z) } }";

        assertThat(translate(optimized, query), not(containsString("LATERAL")));
        assertThat(execute(optimized, query), hasSize(50));
    }


    @Test
    public void generalClass() throws Exception
    {
        // the aliases hold the nodes as whole IRIs, which the number of a tagged node is converted to
        String query = "SELECT ?x ?a WHERE { ?x ex:tag ?t OPTIONAL { ?x ex:alias ?a } }";

        String sql = translate(optimized, query);
        assertThat(sql, containsString(lookup + "SELECT "));
        assertThat(sql, containsString("('http://example.org/n/' || (\"left0\".\"id\")::varchar)::varchar = \"iri\""));

        assertThat(check(query), hasSize(5));

        // the links hold the numbers of the nodes, which a whole IRI is converted to, so that their index is used
        query = "SELECT ?x ?y WHERE { ?x ex:alias 'alias 3' OPTIONAL { ?x ex:link ?y } }";

        sql = translate(optimized, query);
        assertThat(sql, containsString(lookup + "SELECT "));
        assertThat(sql, containsString("THEN right(\"left0\".\"iri\", -21)::int4 END = \"src\""));

        assertThat(check(query), hasSize(10));

        // an IRI of no node is converted to no number, so it has no link
        assertThat(check("SELECT ?x ?y WHERE { ?x ex:alias 'no node' OPTIONAL { ?x ex:link ?y } }"), hasSize(1));
    }


    @Test
    public void subselect() throws Exception
    {
        String links = "\"optional_lookup_test\".\"links\" WHERE (((\"left0\".\"id\" = \"src\")))";

        // the links of a tagged node are looked up and counted
        String query = "SELECT ?x ?c WHERE { ?x ex:tag ?t "
                + "OPTIONAL { SELECT ?x (COUNT(?y) AS ?c) WHERE { ?x ex:link ?y } GROUP BY ?x } }";

        String sql = translate(optimized, query);
        assertThat(sql, containsString(lookup + "SELECT "));
        assertThat(sql, containsString(links + " OFFSET 0 ) AS \"outer0\" GROUP BY \"src\""));

        assertThat(translate(plain, query), not(containsString("LATERAL")));
        assertThat(check(query), hasSize(5));

        // the links of many nodes are counted at once
        query = "SELECT ?x ?c WHERE { ?x ex:label ?l "
                + "OPTIONAL { SELECT ?x (COUNT(?y) AS ?c) WHERE { ?x ex:link ?y } GROUP BY ?x } }";

        assertThat(translate(optimized, query), not(containsString("LATERAL")));
        assertThat(check(query), hasSize(1000));

        // the nodes two links away are looked up for a tagged node and deduplicated
        query = "SELECT ?x ?z WHERE { ?x ex:tag ?t "
                + "OPTIONAL { SELECT DISTINCT ?x ?z WHERE { ?x ex:link ?y . ?y ex:link ?z } } }";

        sql = translate(optimized, query);
        assertThat(sql, containsString(lookup + "SELECT "));
        assertThat(sql, containsString(links + " OFFSET 0"));

        check(query);

        // a slice is picked from all the links, whichever node they belong to
        query = "SELECT ?x ?y WHERE { ?x ex:tag ?t "
                + "OPTIONAL { SELECT ?x ?y WHERE { ?x ex:link ?y } ORDER BY ?x ?y LIMIT 500 } }";

        assertThat(translate(optimized, query), not(containsString("LATERAL")));
        check(query);

        // a count is no grouping variable, so it restricts nothing
        query = "SELECT ?x ?y WHERE { ?x ex:tag ?t BIND(10 AS ?c) "
                + "OPTIONAL { SELECT ?y (COUNT(?z) AS ?c) WHERE { ?y ex:link ?z } GROUP BY ?y } }";

        assertThat(translate(optimized, query), not(containsString("LATERAL")));
        assertThat(check(query), hasSize(1000));
    }


    @Test
    public void difference() throws Exception
    {
        // the links of a tagged node are looked up, and the tags of their targets are tested for them
        String query = "SELECT ?x ?y WHERE { ?x ex:tag ?t OPTIONAL { ?x ex:link ?y MINUS { ?y ex:tag ?u } } }";

        String sql = translate(optimized, query);
        assertThat(sql, containsString(lookup + "SELECT "));
        assertThat(sql, containsString("\"optional_lookup_test\".\"links\" WHERE (((\"left0\".\"id\" = \"src\")))"));

        check(query);
    }


    @Test
    public void unrestricted() throws Exception
    {
        // the right side shares no variable with the left one
        String query = "SELECT ?x ?y WHERE { ?x ex:tag ?t OPTIONAL { ?y ex:tag 'tag 1' } }";

        assertThat(translate(optimized, query), not(containsString("LATERAL")));
        assertThat(check(query), hasSize(5));

        // a walk is not restricted by a solution of the left side
        query = "SELECT ?x ?y WHERE { ?x ex:tag 'tag 1' OPTIONAL { ?x ex:link+ ?y } }";

        assertThat(translate(optimized, query), not(containsString(lookup)));
        check(query);
    }


    @Test
    public void unboundVariable() throws Exception
    {
        // a tagged node need not link to a node, but the label of the node it links to is looked up
        String query = "SELECT ?x ?y ?l WHERE { ?x ex:tag ?t OPTIONAL { ?x ex:link ?y } OPTIONAL { ?y ex:label ?l } }";

        String sql = translate(optimized, query);
        assertThat(count(sql, lookup), is(2));
        assertThat(sql,
                containsString("\"optional_lookup_test\".\"nodes\" WHERE (((\"left0\".\"y#node_par0\" = \"id\")))"));
        assertThat(sql,
                containsString(" OFFSET 0) AS tab WHERE (\"left0\".\"y#node_par0\" IS NOT NULL) UNION ALL SELECT "));

        // the labels are kept for the nodes that link to none, which are joined with them all
        assertThat(sql, containsString(" AS MATERIALIZED (SELECT "));
        assertThat(sql, containsString(
                " FROM \"cte0\" AS tab WHERE (\"left0\".\"y#node_par0\" IS NULL) OFFSET 0 ) AS \"right0\" ON "));

        assertThat(translate(plain, query), not(containsString("LATERAL")));
        assertThat(check(query), hasSize(50));

        // the nodes 199 and 200 link to ten nodes each, the nodes 201 and 202 to none
        query = "SELECT ?x ?y ?l WHERE { ?x ex:score ?s FILTER(?s > 198 && ?s < 203) OPTIONAL { ?x ex:link ?y } "
                + "OPTIONAL { ?y ex:label ?l } }";

        assertThat(count(translate(optimized, query), lookup), is(2));
        assertThat(check(query), hasSize(2020));

        // the five tags are compared with the nodes pairwise, which costs less than looking them up
        query = "SELECT ?x ?y ?u WHERE { ?x ex:score ?s FILTER(?s > 198 && ?s < 203) OPTIONAL { ?x ex:link ?y } "
                + "OPTIONAL { ?y ex:tag ?u } }";

        assertThat(count(translate(optimized, query), lookup), is(1));
        assertThat(check(query), hasSize(30));

        // the two hundred marks are few enough for the database to scan them instead of looking them up, which would
        // cost what comparing them pairwise does
        query = "SELECT ?x ?y ?m WHERE { ?x ex:tag ?t OPTIONAL { ?x ex:link ?y } OPTIONAL { ?y ex:mark ?m } }";

        assertThat(count(translate(optimized, query), lookup), is(1));
        assertThat(check(query), hasSize(50));

        // the notes have no index to look them up by, so the sides are compared pairwise
        query = "SELECT ?x ?y ?n WHERE { ?x ex:tag ?t OPTIONAL { ?x ex:link ?y } OPTIONAL { ?y ex:note ?n } }";

        assertThat(count(translate(optimized, query), lookup), is(1));
        assertThat(check(query), hasSize(50));

        // the links back are looked up by the tagged node, and by the node it links to as well when there is one
        query = "SELECT ?x ?y ?z WHERE { ?x ex:tag ?t OPTIONAL { ?x ex:link ?y } OPTIONAL { ?y ex:link ?x, ?z } }";

        sql = translate(optimized, query);
        assertThat(count(sql, lookup), is(2));
        assertThat(sql, not(containsString(" MATERIALIZED ")));
        assertThat(count(sql, ") AS tab WHERE (\"left0\".\"y#node_par0\" IS "), is(2));

        check(query);

        // the links back of many nodes are hashed by the node, whether it links to a node or not
        query = "SELECT ?x ?y WHERE { ?x ex:label ?l OPTIONAL { ?x ex:link ?y } OPTIONAL { ?y ex:link ?x } }";

        assertThat(translate(optimized, query), not(containsString("LATERAL")));
        assertThat(check(query), hasSize(2800));
    }
}
