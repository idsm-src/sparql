package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.database.SqlType.RDFBOX;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.iri;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.unsupportedIri;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDouble;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static java.util.stream.Collectors.toSet;
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
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.imcode.SqlSelect;
import cz.iocb.sparql.engine.imcode.expression.SqlBinaryComparison;
import cz.iocb.sparql.engine.mapping.ConstantIriMapping;
import cz.iocb.sparql.engine.mapping.classes.ClassRelations;
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
     * Creates the {@code join_optimizer_test} schema: the nodes 1 to 1000 with labels and with scores equal to their
     * numbers, 2000 links among the nodes 1 to 200 (every node links to ten), five nodes tagged, aliases of the nodes
     * holding their IRIs whole, and references from a node to the next one given by its whole IRI (none from every
     * tenth node, but for the tenth one, which refers to a resource that is no node), and notes of resources that are
     * no nodes, all indexed and analysed; and the two configurations mapping {@code ex:label}, {@code ex:score},
     * {@code ex:link}, {@code ex:tag}, {@code ex:alias}, {@code ex:ref} and {@code ex:note} over them.
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
            statement.execute(
                    "create table join_optimizer_test.scores (id integer primary key, score integer not null)");
            statement.execute("insert into join_optimizer_test.scores select i, i from generate_series(1, 1000) as i");
            statement.execute("create index on join_optimizer_test.scores (score)");
            statement
                    .execute("create table join_optimizer_test.aliases (iri varchar not null, alias varchar not null)");
            statement.execute("insert into join_optimizer_test.aliases select 'http://example.org/n/' || i, "
                    + "'alias ' || i from generate_series(1, 1000) as i");
            statement.execute("insert into join_optimizer_test.aliases values ('http://example.org/other', 'no node')");
            statement.execute("create index on join_optimizer_test.aliases (iri)");
            statement.execute("create table join_optimizer_test.refs (src integer not null, target varchar not null)");
            statement.execute("insert into join_optimizer_test.refs select i, 'http://example.org/n/' || (i + 1) "
                    + "from generate_series(1, 1000) as i where i % 10 != 0");
            statement.execute("insert into join_optimizer_test.refs values (10, 'http://example.org/other')");
            statement.execute("create index on join_optimizer_test.refs (src)");
            statement.execute("create table join_optimizer_test.others (iri varchar not null, note varchar not null)");
            statement.execute("insert into join_optimizer_test.others select 'http://example.org/other' || i, "
                    + "'note ' || i from generate_series(1, 1000) as i");
            statement.execute("insert into join_optimizer_test.others values ('http://example.org/other', 'note')");
            statement.execute("create index on join_optimizer_test.others (iri)");
            statement.execute("""
                    analyze join_optimizer_test.nodes, join_optimizer_test.links, join_optimizer_test.few, \
                    join_optimizer_test.scores, join_optimizer_test.aliases, join_optimizer_test.refs, \
                    join_optimizer_test.others""");
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
        DatabaseTable scores = new DatabaseTable("join_optimizer_test", "scores");
        DatabaseTable aliases = new DatabaseTable("join_optimizer_test", "aliases");
        DatabaseTable refs = new DatabaseTable("join_optimizer_test", "refs");
        DatabaseTable others = new DatabaseTable("join_optimizer_test", "others");

        config.addQuadMapping(nodes, graph, config.createIriMapping("node", "id"), config.createIriMapping("ex:label"),
                config.createLiteralMapping(xsdString, "label"));
        config.addQuadMapping(links, graph, config.createIriMapping("node", "src"), config.createIriMapping("ex:link"),
                config.createIriMapping("node", "dst"));
        config.addQuadMapping(few, graph, config.createIriMapping("node", "id"), config.createIriMapping("ex:tag"),
                config.createLiteralMapping(xsdString, "tag"));
        config.addQuadMapping(scores, graph, config.createIriMapping("node", "id"), config.createIriMapping("ex:score"),
                config.createLiteralMapping(xsdInt, "score"));
        config.addQuadMapping(aliases, graph, config.createIriMapping(iri, "iri"), config.createIriMapping("ex:alias"),
                config.createLiteralMapping(xsdString, "alias"));
        config.addQuadMapping(refs, graph, config.createIriMapping("node", "src"), config.createIriMapping("ex:ref"),
                config.createIriMapping(iri, "target"));
        config.addQuadMapping(others, graph, config.createIriMapping(unsupportedIri, "iri"),
                config.createIriMapping("ex:note"), config.createLiteralMapping(xsdString, "note"));

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
     * Returns the explanation of the query translated with the join optimizer on, which gives the tree of a join.
     */
    private static String explain(String query) throws Exception
    {
        Engine engine = new Engine(optimized);

        try(Request request = engine.getRequest())
        {
            PreparedQuery prepared = request.prepareQuery(prefix + query, null);
            SqlSelect imcode = new TranslateVisitor(request).translate(prepared.getSyntaxTree(), null, null, List.of());
            SqlSelect translated = imcode.optimize(request, false).optimize(request, true);

            translated.translate(request);

            return translated.getExplanation();
        }
    }


    /**
     * Returns the tree of the first join of an explanation: a child by its index, a lookup as {@code (outer -> child)}
     * and a hash join as {@code (left + right)}.
     */
    private static String getTree(String explanation)
    {
        Matcher matcher = Pattern.compile("join (\\(.*\\))$", Pattern.MULTILINE).matcher(explanation);

        assertThat(matcher.find(), is(true));

        return matcher.group(1);
    }


    /**
     * Returns the tables of the test schema that an explanation accesses, in the order of the accesses.
     */
    private static List<String> getTables(String explanation)
    {
        return Pattern.compile("access join_optimizer_test\\.(\\w+)").matcher(explanation).results()
                .map(r -> r.group(1)).toList();
    }


    /**
     * Returns the tree of a join without some of its children, the others numbered by their order among themselves.
     */
    private static String without(String tree, Set<Integer> removed)
    {
        return without(tree, new int[] { 0 }, removed);
    }


    /**
     * Reads the subtree at the position, which moves behind it, and returns it without the removed children, or null
     * when it holds no other ones.
     */
    private static String without(String tree, int[] position, Set<Integer> removed)
    {
        if(tree.charAt(position[0]) != '(')
        {
            int begin = position[0];

            while(position[0] < tree.length() && Character.isDigit(tree.charAt(position[0])))
                position[0]++;

            int child = Integer.parseInt(tree.substring(begin, position[0]));

            return removed.contains(child) ? null :
                    String.valueOf(child - removed.stream().filter(r -> r < child).count());
        }

        position[0]++;
        String left = without(tree, position, removed);

        String operator = tree.substring(position[0], tree.indexOf(' ', position[0] + 1) + 1);
        position[0] += operator.length();

        String right = without(tree, position, removed);
        position[0]++;

        return left == null ? right : right == null ? left : "(" + left + operator + right + ")";
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
    public void firstRows() throws Exception
    {
        String query = "SELECT ?x ?z WHERE { ?x ex:link ?y . ?y ex:link ?z }";
        String distinct = "SELECT DISTINCT ?x ?z WHERE { ?x ex:link ?y . ?y ex:link ?z }";

        // a join whose parent reads only its first solutions is planned for them: the many are looked up for the few
        // rows read instead of being hashed whole
        assertThat(getTree(explain(query)), containsString(" + "));
        assertThat(getTree(explain(query + " LIMIT 10")), containsString(" -> "));
        assertThat(getTree(explain(query + " OFFSET 100 LIMIT 10")), containsString(" -> "));

        // a slice that takes most of the solutions, picks them in an order or deduplicates them reads them all
        assertThat(getTree(explain(query + " LIMIT 19000")), containsString(" + "));
        assertThat(getTree(explain(query + " OFFSET 19000 LIMIT 10")), containsString(" + "));
        assertThat(getTree(explain(query + " ORDER BY ?x LIMIT 10")), containsString(" + "));
        assertThat(getTree(explain(distinct + " LIMIT 10")), containsString(" + "));

        // the slice of a sub-select
        String nested = "SELECT ?x ?z ?l WHERE { { " + query + " LIMIT 10 } ?x ex:label ?l }";
        Pattern sliced = Pattern.compile("select \\$x \\$z limit 10\\R.* join \\(\\d -> \\d\\)");
        assertThat(sliced.matcher(explain(nested)).find(), is(true));

        // the access the tree starts with is fenced, so that the database chooses its scan by all its rows
        Pattern fenced = Pattern.compile(" FROM \\(SELECT [^()]* OFFSET 0\\) AS \"tab\\d\" CROSS JOIN LATERAL ");
        assertThat(fenced.matcher(translate(optimized, query + " LIMIT 10")).find(), is(true));
        assertThat(fenced.matcher(translate(optimized, "SELECT ?x ?y WHERE { ?x ex:tag ?t . ?x ex:link ?y }")).find(),
                is(false));

        // the tree does not start from the side that has to be read far for its first row with a solution: a fifth of
        // the nodes have links, ten each, so the first solution takes five nodes, but a single link
        String few = explain("SELECT ?x ?l ?y WHERE { ?x ex:label ?l . ?x ex:link ?y } LIMIT 1");
        int links = getTables(few).indexOf("links");
        assertThat(getTree(few), is("(" + links + " -> " + (1 - links) + ")"));

        String more = explain("SELECT ?x ?l ?y WHERE { ?x ex:label ?l . ?x ex:link ?y } LIMIT 10");
        assertThat(getTree(more), is("(" + (1 - links) + " -> " + links + ")"));

        List<List<RdfTerm>> all = execute(plain, query);

        for(String slice : List.of(query + " LIMIT 10", query + " OFFSET 100 LIMIT 10", nested))
        {
            List<List<RdfTerm>> rows = execute(optimized, slice);

            assertThat(rows, hasSize(10));

            for(List<RdfTerm> row : rows)
                assertThat(all.contains(row.subList(0, 2)), is(true));
        }
    }


    @Test
    public void firstRowsThroughNodes() throws Exception
    {
        String pattern = "?x ex:link ?y . ?y ex:link ?z";

        // a bind gives a solution for every solution of its child, so the join is planned for the rows of the slice
        String bind = "SELECT ?x ?z ?b WHERE { " + pattern + " BIND(STR(?z) AS ?b) }";
        assertThat(getTree(explain(bind)), containsString(" + "));
        assertThat(getTree(explain(bind + " LIMIT 10")), containsString(" -> "));

        // so does an OPTIONAL for every solution of its left side
        String optional = "SELECT ?x ?z ?l WHERE { " + pattern + " OPTIONAL { ?z ex:label ?l } }";
        assertThat(getTree(explain(optional)), containsString(" + "));
        assertThat(getTree(explain(optional + " LIMIT 10")), containsString(" -> "));

        // the right side is looked up for the few solutions read of the left side instead of being hashed whole
        String looked = "SELECT ?x ?l ?y WHERE { ?x ex:label ?l OPTIONAL { ?x ex:link ?y } }";
        assertThat(translate(optimized, looked), not(containsString(" LEFT JOIN LATERAL ")));
        assertThat(translate(optimized, looked + " LIMIT 10"), containsString(" LEFT JOIN LATERAL "));
        assertThat(translate(optimized, looked + " ORDER BY ?l LIMIT 10"), not(containsString(" LEFT JOIN LATERAL ")));

        // a sub-select reads at most the rows its parent reads of it
        String select = "SELECT ?x ?z WHERE { " + pattern + " } LIMIT 5000";
        String nested = "SELECT ?x ?z WHERE { { " + select + " } } LIMIT 10";
        assertThat(getTree(explain(select)), containsString(" + "));
        assertThat(getTree(explain(nested)), containsString(" -> "));

        for(String query : List.of(bind, optional, looked))
        {
            List<List<RdfTerm>> all = execute(plain, query);
            List<List<RdfTerm>> rows = execute(optimized, query + " LIMIT 10");

            assertThat(rows, hasSize(10));

            for(List<RdfTerm> row : rows)
                assertThat(all.contains(row), is(true));
        }

        assertThat(execute(optimized, nested), hasSize(10));
    }


    @Test
    public void firstRowsUnderFilter() throws Exception
    {
        String pattern = "?x ex:link ?y . ?y ex:link ?z";
        String query = "SELECT ?x ?z WHERE { " + pattern + " }";

        // a filter reads as many more solutions of its child as its conditions drop: few for an inequality
        String unequal = "SELECT ?x ?z WHERE { " + pattern + " FILTER(?x != ?z) }";
        assertThat(getTree(explain(unequal)), containsString(" + "));
        assertThat(getTree(explain(unequal + " LIMIT 10")), containsString(" -> "));

        // a condition taken to keep few solutions makes a slice read most of them
        String contained = "SELECT ?x ?z WHERE { " + pattern + " FILTER(CONTAINS(STR(?x), STR(?z))) }";

        assertThat(getTree(explain(query + " LIMIT 50")), containsString(" -> "));
        assertThat(getTree(explain(contained + " LIMIT 50")), containsString(" + "));
        assertThat(getTree(explain(contained + " LIMIT 5")), containsString(" -> "));

        // a condition the join orders its children with in mind is counted by the join itself
        String one = "SELECT ?x ?z WHERE { " + pattern + " FILTER(CONTAINS(STR(?z), '9')) }";

        for(String filtered : List.of(unequal, contained, one))
        {
            List<List<RdfTerm>> all = execute(plain, filtered);
            List<List<RdfTerm>> rows = execute(optimized, filtered + " LIMIT 10");

            assertThat(rows, hasSize(10));

            for(List<RdfTerm> row : rows)
                assertThat(all.contains(row), is(true));
        }
    }


    @Test
    public void firstRowOfATest() throws Exception
    {
        String pattern = "?x ex:link ?y . ?y ex:link ?z";

        // the pattern of an ASK query is read for its first solution only
        String ask = "ASK { " + pattern + " }";
        assertThat(getTree(explain(ask)), containsString(" -> "));

        // so is the pattern of a test sharing no variable with its group, which the database evaluates by itself
        String apart = "SELECT ?n ?l WHERE { ?n ex:tag ?l FILTER EXISTS { " + pattern + " } }";
        assertThat(getTree(explain(apart)), containsString(" -> "));

        // a test sharing a variable is a join for the database, which reads the pattern whole
        String shared = "SELECT ?x ?l WHERE { ?x ex:label ?l FILTER EXISTS { " + pattern + " } }";
        assertThat(getTree(explain(shared)), containsString(" + "));

        assertThat(check(ask), hasSize(1));
        assertThat(check(apart), hasSize(5));
        assertThat(check(shared), hasSize(200));
    }


    @Test
    public void firstRowsOfTestedSolutions() throws Exception
    {
        String negated = "SELECT ?x ?l WHERE { ?x ex:label ?l FILTER NOT EXISTS { ?x ex:link ?y } }";
        String minus = "SELECT ?x ?l WHERE { ?x ex:label ?l MINUS { ?x ex:link ?y } }";
        String tested = "SELECT ?x ?l WHERE { ?x ex:label ?l FILTER EXISTS { ?x ex:link ?y } }";

        for(String query : List.of(negated, minus, tested))
        {
            // the pattern is tested for all the solutions at once, its solutions deduplicated to be hashed
            assertThat(translate(optimized, query), containsString(" GROUP BY \"src\""));
            assertThat(translate(optimized, query), not(containsString(" OFFSET 0")));

            // a slice in no order reads a few solutions, for each of which the pattern is looked up
            assertThat(translate(optimized, query + " LIMIT 10"), containsString(" = \"src\"))) OFFSET 0)"));
            assertThat(translate(optimized, query + " LIMIT 10"), not(containsString(" GROUP BY ")));

            // an ordered slice reads them all
            assertThat(translate(optimized, query + " ORDER BY ?l LIMIT 10"), containsString(" GROUP BY \"src\""));

            List<List<RdfTerm>> all = execute(plain, query);
            List<List<RdfTerm>> rows = execute(optimized, query + " LIMIT 10");

            assertThat(rows, hasSize(10));

            for(List<RdfTerm> row : rows)
                assertThat(all.contains(row), is(true));
        }
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
    public void deduplicatedTests() throws Exception
    {
        // a right side evaluated once is deduplicated, as the database may hash the left side and probe it
        String minus = "SELECT ?x WHERE { ?x ex:label ?l MINUS { ?x ex:link ?y } }";

        assertThat(translate(optimized, minus), containsString(" GROUP BY \"src\""));
        assertThat(translate(plain, minus), not(containsString(" GROUP BY ")));
        assertThat(check(minus), hasSize(800));

        // so is the pattern of an existence test
        for(String test : List.of("FILTER NOT EXISTS", "FILTER EXISTS"))
        {
            String query = "SELECT ?x WHERE { ?x ex:label ?l " + test + " { ?x ex:link ?y } }";

            assertThat(translate(optimized, query), containsString(" GROUP BY \"src\""));
            assertThat(translate(plain, query), not(containsString(" GROUP BY ")));
            assertThat(check(query), hasSize(test.contains("NOT") ? 800 : 200));
        }

        // solutions that a key of the table makes distinct are left as they are
        String distinct = "SELECT ?x ?y WHERE { ?x ex:link ?y MINUS { ?x ex:score ?s FILTER(?s > 100) } }";

        assertThat(translate(optimized, distinct), not(containsString(" GROUP BY ")));
        assertThat(check(distinct), hasSize(1000));

        // and so is a right side looked up for every solution of the left side
        String lookedUp = "SELECT ?x WHERE { ?x ex:tag ?t MINUS { ?x ex:link ?y } }";

        assertThat(translate(optimized, lookedUp), not(containsString(" GROUP BY ")));
        assertThat(check(lookedUp), hasSize(0));
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
    public void filterAboveJoin() throws Exception
    {
        // the labels and the links are many, so they are joined by hashing
        assertThat(translate(optimized, "SELECT ?x ?y WHERE { ?x ex:label ?l . ?x ex:link ?y }"),
                not(containsString("LATERAL")));

        // a filter above the join leaves few of the labels, so the links are looked up for them
        String query = "SELECT ?x ?y WHERE { ?x ex:label ?l . ?x ex:link ?y FILTER(regex(?l, '^node 1[0-9]$')) }";

        String sql = translate(optimized, query);
        assertThat(sql, containsString(" CROSS JOIN LATERAL (SELECT "));
        assertThat(sql, containsString("\"join_optimizer_test\".\"links\" WHERE "));
        assertThat(sql, not(containsString("\"join_optimizer_test\".\"nodes\" WHERE ")));

        assertThat(check(query), hasSize(100));

        // so does a filter above an OPTIONAL following the join, and a conjunct of a filter
        query = "SELECT ?x ?y ?t WHERE { ?x ex:label ?l . ?x ex:link ?y OPTIONAL { ?x ex:tag ?t } "
                + "FILTER(STRSTARTS(?l, 'node 19') && ?x != ?y) }";

        assertThat(translate(optimized, query), containsString(" CROSS JOIN LATERAL (SELECT "));
        assertThat(check(query), hasSize(110));

        // and the filter of an OPTIONAL for the join inside it
        query = "SELECT ?x ?y WHERE { ?x ex:tag ?t OPTIONAL { ?x ex:label ?l . ?x ex:link ?y "
                + "FILTER(CONTAINS(?l, 'e 3')) } }";

        assertThat(translate(optimized, query), containsString(" CROSS JOIN LATERAL (SELECT "));
        assertThat(check(query), hasSize(14));

        // a condition that nearly all the solutions satisfy leaves the join as it is
        query = "SELECT ?x ?y WHERE { ?x ex:label ?l . ?x ex:link ?y FILTER(?x != ?y) }";

        assertThat(translate(optimized, query), not(containsString("LATERAL")));
        check(query);
    }


    @Test
    public void filterOverSeveralVariables() throws Exception
    {
        String pattern = "?x ex:score ?s . ?x ex:link ?y . ?y ex:score ?t . ?y ex:label ?l";

        // the scores, the links and the labels are many, so they are joined by hashing
        assertThat(translate(optimized, "SELECT ?x ?y ?l WHERE { " + pattern + " }"), not(containsString("LATERAL")));

        // a filter requiring the two scores to be equal leaves few of the links joined with them, as a variable shared
        // by the scores would, so the labels are looked up
        String query = "SELECT ?x ?y ?l WHERE { " + pattern + " FILTER(?s = ?t) }";

        assertThat(translate(optimized, query), containsString(" CROSS JOIN LATERAL (SELECT "));
        assertThat(translate(optimized, query), containsString("\"join_optimizer_test\".\"nodes\" WHERE "));
        assertThat(check(query), hasSize(9));

        // so does a conjunct of a filter and a test of the same term: few tags join many scores
        query = "SELECT ?x ?y WHERE { ?x ex:tag ?t . ?y ex:score ?s . ?y ex:link ?z "
                + "FILTER(sameTerm(?x, ?y) && ?s != 77) }";

        assertThat(translate(optimized, query), containsString(" CROSS JOIN LATERAL (SELECT "));
        assertThat(check(query), hasSize(50));

        // how many solutions another comparison of two variables keeps could only be guessed, so it does not count
        query = "SELECT ?x ?y ?l WHERE { " + pattern + " FILTER(?s - ?t = 0) }";

        assertThat(translate(optimized, query), not(containsString("LATERAL")));
        assertThat(check(query), hasSize(9));

        // nor does an equality of two variables taken from one table, whose rows may hold the same value twice often
        query = "SELECT ?x ?y ?l WHERE { ?x ex:link ?y . ?x ex:label ?l FILTER(?x = ?y) }";

        assertThat(translate(optimized, query), not(containsString("LATERAL")));
        assertThat(check(query), hasSize(9));

        // a condition one of whose variables is bound above the join filters above it
        query = "SELECT ?x ?y ?n WHERE { ?x ex:label ?l . ?x ex:link ?y BIND(STRLEN(?l) AS ?n) FILTER(?n < ?x) }";

        check(query);
    }


    @Test
    public void filterEquality() throws Exception
    {
        String links = "\"join_optimizer_test\".\"links\" WHERE ";

        // the tags and the links share no variable, but the filter requires the same term of two of their variables,
        // so the links of the five tagged nodes are looked up
        String query = "SELECT ?x ?z WHERE { ?x ex:tag ?t . ?y ex:link ?z FILTER(?x = ?y) }";

        String sql = translate(optimized, query);
        assertThat(sql, containsString(" CROSS JOIN LATERAL (SELECT "));
        assertThat(sql, containsString(links + "((((\"tab1\".\"id\" = \"src\")))) OFFSET 0"));
        assertThat(sql, not(containsString(" CROSS JOIN (")));
        assertThat(check(query), hasSize(50));

        // a test of the same term does the same
        query = "SELECT ?x ?z WHERE { ?x ex:tag ?t . ?y ex:link ?z FILTER(sameTerm(?y, ?x)) }";

        assertThat(translate(optimized, query), containsString(" CROSS JOIN LATERAL (SELECT "));
        assertThat(check(query), hasSize(50));

        // many labels and many scores are hashed by the equality
        query = "SELECT ?x ?s WHERE { ?x ex:label ?l . ?y ex:score ?s FILTER(?x = ?y) }";

        sql = translate(optimized, query);
        assertThat(sql, not(containsString("LATERAL")));
        assertThat(sql, containsString(" JOIN (SELECT "));
        assertThat(sql, not(containsString(" CROSS JOIN ")));
        assertThat(check(query), hasSize(1000));

        // the aliases hold the nodes as whole IRIs, which the number of a tagged node is converted to
        query = "SELECT ?x ?a WHERE { ?x ex:tag ?t . ?y ex:alias ?a FILTER(?x = ?y) }";

        sql = translate(optimized, query);
        assertThat(sql, containsString(" CROSS JOIN LATERAL (SELECT "));
        assertThat(sql, containsString("::varchar = \"iri\""));
        assertThat(check(query), hasSize(5));

        // the equality orders the joins as a shared variable would: the few tagged nodes first, then the scores equal
        // to those of the nodes they link to
        query = "SELECT ?x ?y ?w WHERE { ?x ex:tag ?t . ?x ex:link ?y . ?y ex:score ?s . ?w ex:score ?u "
                + "FILTER(?s = ?u) }";

        assertThat(translate(optimized, query), not(containsString(" CROSS JOIN (")));
        assertThat(check(query), hasSize(50));

        // a variable a member of the join may leave unbound is compared by the filter only
        query = "SELECT ?x ?y ?s WHERE { { ?x ex:tag ?t OPTIONAL { ?x ex:link ?y } } ?z ex:score ?s FILTER(?y = ?z) }";

        assertThat(check(query), hasSize(50));

        // an equality in an OPTIONAL joins the members of the optional pattern
        query = "SELECT ?x ?z ?s WHERE { ?x ex:tag ?t OPTIONAL { ?x ex:link ?z . ?y ex:score ?s FILTER(?z = ?y) } }";

        assertThat(translate(optimized, query), not(containsString(" CROSS JOIN (")));
        assertThat(check(query), hasSize(50));

        // equal values need not be the same term
        assertThat(SqlBinaryComparison.isTermEquality(ClassRelations.NONE, xsdInt, xsdInt), is(true));
        assertThat(SqlBinaryComparison.isTermEquality(ClassRelations.NONE, xsdString, xsdString), is(true));
        assertThat(SqlBinaryComparison.isTermEquality(ClassRelations.NONE, iri, iri), is(true));
        assertThat(SqlBinaryComparison.isTermEquality(ClassRelations.NONE, xsdInt, xsdInteger), is(false));
        assertThat(SqlBinaryComparison.isTermEquality(ClassRelations.NONE, xsdDouble, xsdDouble), is(false));
        assertThat(SqlBinaryComparison.isTermEquality(ClassRelations.NONE, box, box), is(false));
    }


    @Test
    public void rangeFilter() throws Exception
    {
        // the scores and the links are many, so they are joined by hashing
        assertThat(translate(optimized, "SELECT ?x ?y WHERE { ?x ex:score ?s . ?x ex:link ?y }"),
                not(containsString("LATERAL")));

        // the statistics of the column tell that a narrow range leaves few of the scores to look the links up for
        String query = "SELECT ?x ?y WHERE { ?x ex:score ?s . ?x ex:link ?y FILTER(?s < 5) }";

        assertThat(translate(optimized, query), containsString(" CROSS JOIN LATERAL (SELECT "));
        assertThat(check(query), hasSize(40));

        // and that a wide range leaves most of them, however many bounds it has
        query = "SELECT ?x ?y WHERE { ?x ex:score ?s . ?x ex:link ?y FILTER(?s >= 5 && ?s < 900 && ?s != 7) }";

        assertThat(translate(optimized, query), not(containsString("LATERAL")));
        assertThat(check(query), hasSize(1950));

        // so they tell of a filter that is a member of the join
        query = "SELECT ?x ?y WHERE { { ?x ex:score ?s FILTER(?s IN (3, 4)) } ?x ex:link ?y }";

        assertThat(translate(optimized, query), containsString(" CROSS JOIN LATERAL (SELECT "));
        assertThat(check(query), hasSize(20));

        query = "SELECT ?x ?y WHERE { { ?x ex:score ?s FILTER(?s > 100) } ?x ex:link ?y }";

        assertThat(translate(optimized, query), not(containsString("LATERAL")));
        assertThat(check(query), hasSize(1000));
    }


    @Test
    public void rowsReadByAFilter() throws Exception
    {
        String nodes = "\"join_optimizer_test\".\"nodes\" WHERE ";

        // the filter leaves few of the thousand labels, but the database has to read them all to evaluate it, so the
        // labels of the five tagged nodes are looked up instead
        String query = "SELECT ?x ?l WHERE { ?x ex:tag ?t . ?x ex:label ?l FILTER(regex(?l, '^node [1-3]$')) }";

        String sql = translate(optimized, query);
        assertThat(sql, containsString(" CROSS JOIN LATERAL (SELECT "));
        assertThat(sql, containsString(nodes));
        assertThat(check(query), hasSize(3));

        // so are they for a comparison of a column that has no index
        query = "SELECT ?x ?l WHERE { ?x ex:tag ?t . ?x ex:label ?l FILTER(?l < 'node 1000') }";

        assertThat(translate(optimized, query), containsString(nodes));
        assertThat(check(query), hasSize(1));

        // an index of the scores serves a narrow range, so the few scores it leaves are read by it and hashed
        query = "SELECT ?x ?s WHERE { ?x ex:tag ?t . ?x ex:score ?s FILTER(?s < 4) }";

        assertThat(translate(optimized, query), not(containsString("LATERAL")));
        assertThat(check(query), hasSize(3));

        // an index of a box orders the terms, whereas a filter compares the values
        assertThat(INT4.isComparedByIndex(), is(true));
        assertThat(RDFBOX.isComparedByIndex(), is(false));
    }


    @Test
    public void manyChildren() throws Exception
    {
        // a join of more children than are planned exhaustively, none of which the others determine
        String query = """
                SELECT ?x ?z ?w ?v ?u ?q ?p WHERE { ?x ex:tag ?t . ?x ex:link ?z . ?z ex:ref ?rz . ?z ex:link ?w . \
                ?w ex:tag ?tw . ?w ex:ref ?rw . ?w ex:link ?v . ?v ex:ref ?rv . ?v ex:link ?u . ?u ex:tag ?tu . \
                ?u ex:ref ?ru . ?u ex:link ?q . ?q ex:ref ?rq . ?q ex:link ?p . ?p ex:tag ?tp . ?p ex:ref ?rp }""";

        // the next child is one sharing a variable with those joined so far, so the few tags, cheap as their joins
        // with each other are, are not joined into all their combinations first, and the links are looked up
        String sql = translate(optimized, query);
        assertThat(sql, not(containsString(" CROSS JOIN (")));
        assertThat(sql, containsString("\"join_optimizer_test\".\"links\" WHERE "));

        assertThat(check(query), hasSize(40));
    }


    @Test
    public void determinedChildren() throws Exception
    {
        String pattern = """
                ?x ex:tag ?t . ?x ex:link ?z . ?z ex:ref ?rz . ?z ex:link ?w . ?w ex:tag ?tw . ?w ex:ref ?rw . \
                ?w ex:link ?v . ?v ex:ref ?rv . ?v ex:link ?u . ?u ex:tag ?tu""";

        // the labels and the scores of the nodes: a node has one of each, and every node has them
        String determined = """
                ?x ex:label ?lx . ?z ex:label ?lz . ?w ex:label ?lw . ?v ex:label ?lv . ?u ex:label ?lu . \
                ?x ex:score ?sx . ?z ex:score ?sz . ?w ex:score ?sw . ?v ex:score ?sv . ?u ex:score ?su""";

        String few = explain("SELECT * WHERE { " + pattern + " }");
        String many = explain("SELECT * WHERE { " + pattern + " . " + determined + " }");

        List<String> tables = getTables(many);
        assertThat(tables, hasSize(20));

        // with them the join has more children than are planned exhaustively, so they are left out of the search and
        // joined to the tree chosen for the others, which is the tree of the join without them
        Set<Integer> removed = IntStream.range(0, tables.size())
                .filter(i -> tables.get(i).equals("nodes") || tables.get(i).equals("scores")).boxed().collect(toSet());

        assertThat(removed, hasSize(10));
        assertThat(getTables(few), hasSize(10));
        assertThat(without(getTree(many), removed), is(getTree(few)));

        assertThat(check("SELECT * WHERE { " + pattern + " . " + determined + " }"), hasSize(20));
    }


    @Test
    public void lookupAcrossClasses() throws Exception
    {
        // the aliases hold whole IRIs and the links the numbers of the nodes: the IRI of an alias is converted to the
        // number of its node, so that the links are looked up by the index of their source
        String query = "SELECT ?x ?y WHERE { ?x ex:alias 'alias 3' . ?x ex:link ?y }";

        String sql = translate(optimized, query);
        assertThat(sql, containsString(" CROSS JOIN LATERAL (SELECT "));
        assertThat(sql, containsString("::int4 END = \"src\""));

        assertThat(check(query), hasSize(10));

        // an IRI of no node is converted to no number, so it joins no link
        assertThat(check("SELECT ?x ?y WHERE { ?x ex:alias 'no node' . ?x ex:link ?y }"), hasSize(0));

        // the number of a tagged node is converted to its IRI, which the aliases are looked up by
        query = "SELECT ?x ?a WHERE { ?x ex:tag ?t . ?x ex:alias ?a }";

        sql = translate(optimized, query);
        assertThat(sql, containsString(" CROSS JOIN LATERAL (SELECT "));
        assertThat(sql, containsString("::varchar = \"iri\""));

        assertThat(check(query), hasSize(5));

        // a walk reaches the targets of the references as whole IRIs, each converted to the number of its node, by
        // which the next references are looked up; the walk ends at the resource that is no node
        query = "SELECT ?y WHERE { <http://example.org/n/1> ex:ref+ ?y }";

        sql = translate(optimized, query);
        assertThat(sql, containsString("recursion AS "));
        assertThat(sql, containsString("::int4 END = \"src\""));

        assertThat(check(query), hasSize(10));

        // an IRI cannot be told to belong to no IRI class of the configuration, so a whole IRI is not converted to the
        // class of the notes; that class is stored like any IRIs, though, so the notes are still looked up by their
        // index, with the IRIs compared as they are
        query = "SELECT ?x ?n WHERE { ?x ex:alias 'no node' . ?x ex:note ?n }";

        sql = translate(optimized, query);
        assertThat(sql, containsString(" CROSS JOIN LATERAL (SELECT "));
        assertThat(sql, containsString("\"join_optimizer_test\".\"others\" WHERE ((((\"tab"));
        assertThat(sql, containsString("\".\"iri\" = \"iri\""));

        assertThat(check(query), hasSize(1));
    }


    @Test
    public void values() throws Exception
    {
        String query = "SELECT ?x ?y WHERE { VALUES ?x { <http://example.org/n/1> <http://example.org/n/2> } "
                + "?x ex:link ?y }";

        assertThat(check(query), hasSize(20));
    }


    @Test
    public void filterAcceptingTerms() throws Exception
    {
        // the access takes the terms the filter accepts of its variable in, converted to the class of its column
        String query = "SELECT ?x ?y WHERE { ?x ex:link ?y "
                + "FILTER(?x IN (<http://example.org/n/1>, <http://example.org/n/2>)) }";

        String sql = translate(optimized, query);
        assertThat(sql, containsString("\"links\" WHERE (("));
        assertThat(sql, containsString("\"src\" = '1'::int4"));
        assertThat(sql, containsString("\"src\" = '2'::int4"));

        // without the optimizer the terms are left to the filter, which the database pushes down by itself
        assertThat(translate(plain, query), not(containsString("\"links\" WHERE ")));

        assertThat(check(query), hasSize(20));

        // a term the class of the column cannot hold is left out, and no row is left when it holds none of them
        assertThat(check("SELECT ?x ?y WHERE { ?x ex:link ?y "
                + "FILTER(?x IN (<http://example.org/n/1>, <http://example.org/other>)) }"), hasSize(10));
        assertThat(check("SELECT ?x ?y WHERE { ?x ex:link ?y "
                + "FILTER(?x IN (<http://example.org/other>, <http://example.org/another>)) }"), hasSize(0));

        // a variable that may be unbound restricts no access
        assertThat(check("SELECT ?x ?y WHERE { ?x ex:tag ?t OPTIONAL { ?x ex:link ?y } "
                + "FILTER(?y IN (<http://example.org/n/8>, <http://example.org/n/15>)) }"), hasSize(4));
    }
}
