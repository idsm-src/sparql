package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.iri;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.imcode.SqlSelect;
import cz.iocb.sparql.engine.mapping.ConstantIriMapping;
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
 * Pruning of the branches of a recursive step that cannot lead to a class the parent accepts. The step of
 * {@code ex:sub*} has three branches: the parents of the nodes, the parents of the others, and the node a parentless
 * other is a subclass of. From a node, the walk thus reaches the others, but it never comes back, so when the parent
 * joins the end variable with the labels, which only the nodes have, the branches over the others contribute nothing
 * and the recursion runs over the nodes alone. The same holds for the right side of a MINUS, for the keys of a GROUP BY
 * and for a union branch bound only in such classes, also inside the deduplicated zero-length paths of a walk between
 * variables, which enumerate every node of the graph. A FILTER accepts the classes in which its conditions can hold.
 * Where the parent keeps the solutions it cannot join (the left side of an OPTIONAL, a nullable sibling or left side),
 * computes an aggregate from them or picks them by a slice, the branches have to stay, and a walk whose end variable
 * keeps several classes then joins its step laterally, branch by branch, instead of by a disjunction. A walk between
 * two variables starts from the end the parent restricts more, and a sibling in a join that binds an end of the walk
 * always and distinct solutions over it (the labels, keyed by the node) seeds the walk: its solutions start the
 * recursion, which carries its variables along, so that it is evaluated once and the join needs it no more; read from a
 * source of the nodes, it replaces the zero-length paths altogether. A walk of which the parent needs only one end, and
 * not the multiplicities (a DISTINCT over the end, a MINUS, an EXISTS), reduces to its initial solutions, since a node
 * has a path iff it has a zero-length path or a first step.
 */
public class RecursivePruningTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> ";

    /**
     * The node outside the classes of the nodes and the others, held as a whole IRI.
     */
    private static final Iri thing = new Iri("http://other.org/thing");

    /**
     * Configuration mapping the test tables.
     */
    private static SparqlDatabaseConfiguration config = null;


    /**
     * Creates the {@code recursive_pruning_test} schema: the nodes (0 is the root, 1 and 3 its children, 2 the child of
     * 1) with their labels (keyed by the node, so that the labels are distinct over it and may seed a walk), the others
     * (10 and 20 are subclasses of the node 1, 11 of 10 and 12 of 11), the links from the nodes to whole IRIs (whose
     * general class overlaps every other, so that the zero-length paths are not split by class; the node 3 has two of
     * them, so the links are not distinct over the node), and the configuration mapping them: {@code ex:sub} over the
     * nodes and the others, {@code ex:next} over the nodes alone.
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema recursive_pruning_test");
            statement.execute("create table recursive_pruning_test.nodes (id integer not null, parent integer)");
            statement.execute("insert into recursive_pruning_test.nodes values (0, null), (1, 0), (2, 1), (3, 0)");
            statement.execute(
                    "create table recursive_pruning_test.labels (id integer primary key, " + "label varchar not null)");
            statement.execute("insert into recursive_pruning_test.labels values (0, 'zero'), (1, 'one'), (2, 'two'), "
                    + "(3, 'three')");
            statement.execute("create table recursive_pruning_test.others (id integer not null, parent integer)");
            statement.execute(
                    "insert into recursive_pruning_test.others values (10, null), (11, 10), (12, 11), " + "(20, null)");
            statement.execute("create table recursive_pruning_test.links (subject integer not null, "
                    + "object varchar not null)");
            statement.execute("insert into recursive_pruning_test.links values (3, 'http://other.org/thing'), "
                    + "(3, 'http://example.org/n/2')");
        }

        DatabaseSchema schema = new DatabaseSchema(connectionPool);

        config = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);
        config.addPrefix("ex", "http://example.org/");
        config.addIriClass(new IntegerUserIriClass("node", INT4, "http://example.org/n/"));
        config.addIriClass(new IntegerUserIriClass("other", INT4, "http://example.org/a/"));

        DatabaseTable nodes = new DatabaseTable("recursive_pruning_test", "nodes");
        DatabaseTable labels = new DatabaseTable("recursive_pruning_test", "labels");
        DatabaseTable others = new DatabaseTable("recursive_pruning_test", "others");
        DatabaseTable links = new DatabaseTable("recursive_pruning_test", "links");
        ConstantIriMapping graph = config.createIriMapping("<http://example.org/graph>");

        config.addQuadMapping(nodes, graph, config.createIriMapping("node", "id"), config.createIriMapping("ex:sub"),
                config.createIriMapping("node", "parent"));

        config.addQuadMapping(nodes, graph, config.createIriMapping("node", "id"), config.createIriMapping("ex:next"),
                config.createIriMapping("node", "parent"));

        config.addQuadMapping(links, graph, config.createIriMapping("node", "subject"),
                config.createIriMapping("ex:link"), config.createIriMapping(iri, "object"));

        config.addQuadMapping(labels, graph, config.createIriMapping("node", "id"), config.createIriMapping("ex:label"),
                config.createLiteralMapping(xsdString, "label"));

        config.addQuadMapping(others, graph, config.createIriMapping("other", "id"), config.createIriMapping("ex:sub"),
                config.createIriMapping("other", "parent"));

        config.addQuadMapping(others, graph, config.createIriMapping("other", "id"), config.createIriMapping("ex:sub"),
                config.createIriMapping("<http://example.org/n/1>"), config.createIsNullCondition(others, "parent"));
    }


    /**
     * Runs the query and returns all rows.
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
     * Returns the number of times the table is read by the SQL code.
     */
    private static int reads(String sql, String table)
    {
        String name = "\"recursive_pruning_test\".\"" + table + "\"";
        int count = 0;

        for(int i = sql.indexOf(name); i >= 0; i = sql.indexOf(name, i + 1))
            count++;

        return count;
    }


    /**
     * Returns the SQL code the query is translated to.
     */
    private static String translate(String query) throws Exception
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
     * Expected row.
     */
    private static List<RdfTerm> row(RdfTerm... terms)
    {
        return Arrays.asList(terms);
    }


    /**
     * IRI of the node with the given number.
     */
    private static Iri node(int number)
    {
        return new Iri("http://example.org/n/" + number);
    }


    /**
     * IRI of the other with the given number.
     */
    private static Iri other(int number)
    {
        return new Iri("http://example.org/a/" + number);
    }


    /**
     * String literal.
     */
    private static TypedLiteral string(String value)
    {
        return new TypedLiteral(value, xsdStringIri);
    }


    /**
     * Integer literal.
     */
    private static TypedLiteral integer(int value)
    {
        return new TypedLiteral(Integer.toString(value), xsdIntegerIri);
    }


    @Test
    @DisplayName("a join over the end variable prunes the branches that cannot lead to its classes")
    void joinPrunesBranches() throws Exception
    {
        String query = "SELECT ?x ?l WHERE { ?x ex:sub* <http://example.org/n/0> ; ex:label ?l }";

        assertThat(execute(query), containsInAnyOrder(row(node(0), string("zero")), row(node(1), string("one")),
                row(node(2), string("two")), row(node(3), string("three"))));
        assertThat(translate(query), not(containsString("others")));
        assertThat(translate(query), not(containsString(" OR ")));
        assertThat(translate(query), not(containsString("LATERAL")));
    }


    @Test
    @DisplayName("without a restricting parent the walk takes every branch")
    void unrestrictedWalk() throws Exception
    {
        String query = "SELECT ?x WHERE { ?x ex:sub* <http://example.org/n/0> }";

        assertThat(execute(query), containsInAnyOrder(row(node(0)), row(node(1)), row(node(2)), row(node(3)),
                row(other(10)), row(other(11)), row(other(12)), row(other(20))));
        assertThat(translate(query), containsString("others"));
        // the end variable keeps two classes, so the step is joined laterally instead of by a disjunction
        assertThat(translate(query), containsString("CROSS JOIN LATERAL"));
        assertThat(translate(query), not(containsString(" OR ")));
    }


    @Test
    @DisplayName("the right side of an OPTIONAL is pruned by the variables the left side always binds")
    void optionalRightSidePruned() throws Exception
    {
        String query = "SELECT ?x ?l ?d WHERE { ?x ex:label ?l "
                + "OPTIONAL { ?x ex:sub* <http://example.org/n/0> BIND('yes' AS ?d) } }";

        assertThat(execute(query),
                containsInAnyOrder(row(node(0), string("zero"), string("yes")),
                        row(node(1), string("one"), string("yes")), row(node(2), string("two"), string("yes")),
                        row(node(3), string("three"), string("yes"))));
        assertThat(translate(query), not(containsString("others")));
    }


    @Test
    @DisplayName("the left side of an OPTIONAL keeps every branch")
    void optionalLeftSideKept() throws Exception
    {
        String query = "SELECT ?x ?l WHERE { ?x ex:sub* <http://example.org/n/0> OPTIONAL { ?x ex:label ?l } }";

        assertThat(execute(query),
                containsInAnyOrder(row(node(0), string("zero")), row(node(1), string("one")),
                        row(node(2), string("two")), row(node(3), string("three")), row(other(10), null),
                        row(other(11), null), row(other(12), null), row(other(20), null)));
        assertThat(translate(query), containsString("others"));
    }


    @Test
    @DisplayName("a sibling that may leave the variable unbound keeps every branch")
    void nullableSiblingKept() throws Exception
    {
        String query = "SELECT ?x ?l WHERE { ?x ex:sub* <http://example.org/n/0> . "
                + "{ ?z ex:label ?l OPTIONAL { ?z ex:sub ?x } } }";

        assertThat(execute(query),
                containsInAnyOrder(row(node(0), string("zero")), row(node(1), string("zero")),
                        row(node(2), string("zero")), row(node(3), string("zero")), row(other(10), string("zero")),
                        row(other(11), string("zero")), row(other(12), string("zero")), row(other(20), string("zero")),
                        row(node(0), string("one")), row(node(1), string("two")), row(node(0), string("three"))));
        assertThat(translate(query), containsString("others"));
    }


    @Test
    @DisplayName("a slice picks its rows before the join, so the branches stay")
    void sliceKeepsBranches() throws Exception
    {
        String query = "SELECT ?x ?l WHERE { { SELECT ?x WHERE { ?x ex:sub* <http://example.org/n/0> } "
                + "ORDER BY ?x LIMIT 5 } ?x ex:label ?l }";

        assertThat(execute(query), containsInAnyOrder(row(node(0), string("zero"))));
        assertThat(translate(query), containsString("others"));
    }


    @Test
    @DisplayName("a single branch that cannot lead to an accepted class is left out")
    void singleBranchPruned() throws Exception
    {
        String query = "SELECT ?x ?l WHERE { ?x ex:sub* <http://example.org/a/10> ; ex:label ?l }";

        assertThat(execute(query), empty());
        assertThat(translate(query), not(containsString("others")));
    }


    @Test
    @DisplayName("the right side of a MINUS is pruned by the variables the left side always binds")
    void minusRightSidePruned() throws Exception
    {
        String query = "SELECT ?x ?l WHERE { ?x ex:label ?l MINUS { ?x ex:sub* <http://example.org/n/1> } }";

        assertThat(execute(query), containsInAnyOrder(row(node(0), string("zero")), row(node(3), string("three"))));
        assertThat(translate(query), not(containsString("others")));
    }


    @Test
    @DisplayName("a left side of a MINUS that may leave the variable unbound keeps every branch")
    void minusNullableLeftKept() throws Exception
    {
        String query = "SELECT ?l ?x WHERE { { ?z ex:label ?l OPTIONAL { ?z ex:sub ?x } } "
                + "MINUS { ?x ex:sub* <http://example.org/n/1> } }";

        assertThat(execute(query), containsInAnyOrder(row(string("zero"), null), row(string("one"), node(0)),
                row(string("three"), node(0))));
        assertThat(translate(query), containsString("others"));
    }


    @Test
    @DisplayName("a grouping variable is pruned by the classes the parent accepts")
    void groupKeyPruned() throws Exception
    {
        String query = "SELECT ?x ?l ?n WHERE { { SELECT ?x (COUNT(*) AS ?n) WHERE { "
                + "?x ex:sub* <http://example.org/n/0> } GROUP BY ?x } ?x ex:label ?l }";

        assertThat(execute(query),
                containsInAnyOrder(row(node(0), string("zero"), integer(1)), row(node(1), string("one"), integer(1)),
                        row(node(2), string("two"), integer(1)), row(node(3), string("three"), integer(1))));
        assertThat(translate(query), not(containsString("others")));
    }


    @Test
    @DisplayName("the solutions behind an aggregate keep every branch")
    void aggregateKeepsBranches() throws Exception
    {
        String query = "SELECT ?m ?l WHERE { { SELECT (MIN(?x) AS ?m) WHERE { ?x ex:sub* <http://example.org/n/0> } } "
                + "?m ex:label ?l }";

        assertThat(execute(query), empty());
        assertThat(translate(query), containsString("others"));
    }


    @Test
    @DisplayName("a union branch binding the variable only in classes the parent does not accept is left out")
    void unionBranchPruned() throws Exception
    {
        String query = "SELECT ?x ?l WHERE { { SELECT ?x WHERE { { ?x ex:sub <http://example.org/n/0> } "
                + "UNION { ?x ex:sub <http://example.org/a/10> } } ORDER BY ?x } ?x ex:label ?l }";

        assertThat(execute(query), containsInAnyOrder(row(node(1), string("one")), row(node(3), string("three"))));
        assertThat(translate(query), not(containsString("others")));
    }


    @Test
    @DisplayName("a union on the left side of an OPTIONAL keeps its branches")
    void unionUnderOptionalLeftKept() throws Exception
    {
        String query = "SELECT ?x ?l WHERE { { ?x ex:sub <http://example.org/n/0> } "
                + "UNION { ?x ex:sub <http://example.org/a/10> } OPTIONAL { ?x ex:label ?l } }";

        assertThat(execute(query),
                containsInAnyOrder(row(node(1), string("one")), row(node(3), string("three")), row(other(11), null)));
        assertThat(translate(query), containsString("others"));
    }


    @Test
    @DisplayName("the zero-length paths of a walk whose start the parent restricts enumerate only the accepted classes")
    void zeroPathPruned() throws Exception
    {
        String query = "SELECT ?x ?y WHERE { ?x ex:next* ?y . ?x ex:sub <http://example.org/a/10> }";

        assertThat(execute(query), containsInAnyOrder(row(other(11), other(11))));
        assertThat(translate(query), not(containsString("labels")));
    }


    @Test
    @DisplayName("the zero-length paths keep the nodes of the general class overlapping the accepted ones")
    void zeroPathGeneralKept() throws Exception
    {
        // the subclasses are not distinct over the start (the nodes have no key), so the walk is not seeded with them
        String query = "SELECT ?x ?y ?p WHERE { ?x ex:next* ?y . ?x ex:sub ?p }";

        assertThat(execute(query),
                containsInAnyOrder(row(node(1), node(1), node(0)), row(node(1), node(0), node(0)),
                        row(node(2), node(2), node(1)), row(node(2), node(1), node(1)), row(node(2), node(0), node(1)),
                        row(node(3), node(3), node(0)), row(node(3), node(0), node(0)),
                        row(other(10), other(10), node(1)), row(other(11), other(11), other(10)),
                        row(other(12), other(12), other(11)), row(other(20), other(20), node(1))));
        assertThat(translate(query), containsString("\"object\""));
    }


    @Test
    @DisplayName("without a restricting parent the zero-length paths enumerate every node")
    void zeroPathUnrestricted() throws Exception
    {
        String query = "SELECT ?x ?y WHERE { ?x ex:next* ?y }";

        assertThat(execute(query),
                containsInAnyOrder(row(node(0), node(0)), row(node(1), node(1)), row(node(2), node(2)),
                        row(node(3), node(3)), row(other(10), other(10)), row(other(11), other(11)),
                        row(other(12), other(12)), row(other(20), other(20)), row(string("zero"), string("zero")),
                        row(string("one"), string("one")), row(string("two"), string("two")),
                        row(string("three"), string("three")), row(thing, thing), row(node(1), node(0)),
                        row(node(2), node(1)), row(node(2), node(0)), row(node(3), node(0))));
        assertThat(translate(query), containsString("labels"));
        assertThat(translate(query), containsString("others"));
    }


    @Test
    @DisplayName("a filter comparing the end variable with a constant prunes the branches of other classes")
    void filterEqualityPrunesBranches() throws Exception
    {
        String query = "SELECT ?x WHERE { ?x ex:sub* <http://example.org/n/0> FILTER(?x = <http://example.org/n/2>) }";

        assertThat(execute(query), containsInAnyOrder(row(node(2))));
        assertThat(translate(query), not(containsString("others")));
    }


    @Test
    @DisplayName("a filter testing the end variable for membership prunes the branches of other classes")
    void filterMembershipPrunesBranches() throws Exception
    {
        String query = "SELECT ?x WHERE { ?x ex:sub* <http://example.org/n/0> "
                + "FILTER(?x IN (<http://example.org/n/1>, <http://example.org/n/2>)) }";

        assertThat(execute(query), containsInAnyOrder(row(node(1)), row(node(2))));
        assertThat(translate(query), not(containsString("others")));
    }


    @Test
    @DisplayName("a filter that can hold in every class keeps every branch")
    void inequalityFilterKeepsBranches() throws Exception
    {
        String query = "SELECT ?x WHERE { ?x ex:sub* <http://example.org/n/0> "
                + "FILTER(?x != <http://example.org/a/20>) }";

        assertThat(execute(query), containsInAnyOrder(row(node(0)), row(node(1)), row(node(2)), row(node(3)),
                row(other(10)), row(other(11)), row(other(12))));
        assertThat(translate(query), containsString("others"));
    }


    @Test
    @DisplayName("a filter of an OPTIONAL prunes the branches of the walk on its right side")
    void optionalFilterPrunesBranches() throws Exception
    {
        String query = "SELECT ?l ?x WHERE { ?z ex:label ?l "
                + "OPTIONAL { ?x ex:sub* <http://example.org/n/0> FILTER(?x = <http://example.org/n/2>) } }";

        assertThat(execute(query), containsInAnyOrder(row(string("zero"), node(2)), row(string("one"), node(2)),
                row(string("two"), node(2)), row(string("three"), node(2))));
        assertThat(translate(query), not(containsString("others")));
    }


    @Test
    @DisplayName("a walk whose end alone the parent restricts starts from the end, seeded with the sibling")
    void walkTurnedToRestrictedEnd() throws Exception
    {
        String query = "SELECT ?x ?y ?l WHERE { ?x ex:next* ?y . ?y ex:label ?l }";

        assertThat(execute(query),
                containsInAnyOrder(row(node(0), node(0), string("zero")), row(node(1), node(1), string("one")),
                        row(node(1), node(0), string("zero")), row(node(2), node(2), string("two")),
                        row(node(2), node(1), string("one")), row(node(2), node(0), string("zero")),
                        row(node(3), node(3), string("three")), row(node(3), node(0), string("zero"))));
        // started from the end and seeded with the labels, the walk enumerates no other node than the labelled ones
        String sql = translate(query);
        assertThat(sql, not(containsString("http://example.org/a/")));
        assertThat(sql, not(containsString("links")));
        assertThat(reads(sql, "labels"), is(1));
    }


    @Test
    @DisplayName("a walk whose end the parent restricts to narrower classes than the start starts from the end")
    void walkTurnedToNarrowerEnd() throws Exception
    {
        String query = "SELECT ?x ?y ?l WHERE { ?x ex:next* ?y . ?z ex:link ?x . ?y ex:label ?l }";

        assertThat(execute(query), containsInAnyOrder(row(node(2), node(2), string("two")),
                row(node(2), node(1), string("one")), row(node(2), node(0), string("zero"))));
        assertThat(translate(query), not(containsString("http://example.org/a/")));
    }


    @Test
    @DisplayName("a sibling distinct over the start of a walk seeds it and replaces its zero-length paths")
    void walkSeededByStart() throws Exception
    {
        String query = "SELECT ?x ?y ?l WHERE { ?x ex:next* ?y . ?x ex:label ?l }";

        assertThat(execute(query),
                containsInAnyOrder(row(node(0), node(0), string("zero")), row(node(1), node(1), string("one")),
                        row(node(1), node(0), string("one")), row(node(2), node(2), string("two")),
                        row(node(2), node(1), string("two")), row(node(2), node(0), string("two")),
                        row(node(3), node(3), string("three")), row(node(3), node(0), string("three"))));
        // the labels are a source of the nodes, so their rows are the zero-length paths, and the recursion carries the
        // label along instead of joining the labels afterwards
        String sql = translate(query);
        assertThat(sql, not(containsString("links")));
        assertThat(sql, not(containsString("others")));
        assertThat(reads(sql, "labels"), is(1));
    }


    @Test
    @DisplayName("a sibling restricted by a constant seeds the walk with the rows it keeps")
    void walkSeededByRestrictedSibling() throws Exception
    {
        String query = "SELECT ?x ?y WHERE { ?x ex:next* ?y . ?x ex:label \"two\" }";

        assertThat(execute(query),
                containsInAnyOrder(row(node(2), node(2)), row(node(2), node(1)), row(node(2), node(0))));
        String sql = translate(query);
        assertThat(sql, not(containsString("links")));
        assertThat(reads(sql, "labels"), is(1));
    }


    @Test
    @DisplayName("a sibling distinct over the start seeds the first step of a walk of one or more steps")
    void firstStepSeeded() throws Exception
    {
        String query = "SELECT ?x ?y ?l WHERE { ?x ex:next+ ?y . ?x ex:label ?l }";

        assertThat(execute(query),
                containsInAnyOrder(row(node(1), node(0), string("one")), row(node(2), node(1), string("two")),
                        row(node(2), node(0), string("two")), row(node(3), node(0), string("three"))));
        // the labels are read once, inside the recursion (joined with the first step) instead of after it
        String sql = translate(query);
        assertThat(reads(sql, "labels"), is(1));
        assertThat(sql.indexOf("\"labels\""), greaterThan(sql.indexOf("WITH RECURSIVE")));
    }


    @Test
    @DisplayName("a seeded walk keeps the end variable a sibling joins")
    void seededWalkKeepsSiblingEnd() throws Exception
    {
        String query = "SELECT ?x ?l ?z WHERE { ?x ex:label ?l . ?x ex:next* ?y . ?y ex:link ?z }";

        assertThat(execute(query),
                containsInAnyOrder(row(node(3), string("three"), thing), row(node(3), string("three"), node(2))));
        assertThat(reads(translate(query), "labels"), is(1));
    }


    @Test
    @DisplayName("an EXISTS keeps the walk whose end a sibling joins")
    void existsKeepsWalkWithJoinedEnd() throws Exception
    {
        String query = "SELECT ?x WHERE { ?x ex:label ?l FILTER EXISTS { ?x ex:next+ ?o . ?o ex:label 'zero' } }";

        assertThat(execute(query), containsInAnyOrder(row(node(1)), row(node(2)), row(node(3))));
        assertThat(translate(query), containsString("WITH RECURSIVE"));
    }


    @Test
    @DisplayName("a sibling that is not distinct over the start does not seed the walk")
    void nonDistinctSiblingKeepsWalk() throws Exception
    {
        String query = "SELECT ?x ?y ?z WHERE { ?x ex:next* ?y . ?x ex:link ?z }";

        assertThat(execute(query), containsInAnyOrder(row(node(3), node(3), thing), row(node(3), node(3), node(2)),
                row(node(3), node(0), thing), row(node(3), node(0), node(2))));
        // seeded with the two links of the node 3, the recursion would unite its paths into one; the zero-length
        // paths still enumerate the nodes (the labels are read as their source)
        assertThat(translate(query), containsString("labels"));
    }


    @Test
    @DisplayName("distinct VALUES seed the walk, looked up in the sources of the nodes")
    void valuesSeedWalk() throws Exception
    {
        String query = "SELECT ?x ?y WHERE { VALUES ?x { <http://example.org/n/2> } ?x ex:next* ?y }";

        assertThat(execute(query),
                containsInAnyOrder(row(node(2), node(2)), row(node(2), node(1)), row(node(2), node(0))));
        assertThat(translate(query), containsString("'2'::int4 = \"id\""));
    }


    @Test
    @DisplayName("the left side of an OPTIONAL does not seed the walk on its right side")
    void optionalLeftDoesNotSeed() throws Exception
    {
        String query = "SELECT ?x ?l ?y WHERE { ?x ex:label ?l OPTIONAL { ?x ex:next* ?y } }";

        assertThat(execute(query),
                containsInAnyOrder(row(node(0), string("zero"), node(0)), row(node(1), string("one"), node(1)),
                        row(node(1), string("one"), node(0)), row(node(2), string("two"), node(2)),
                        row(node(2), string("two"), node(1)), row(node(2), string("two"), node(0)),
                        row(node(3), string("three"), node(3)), row(node(3), string("three"), node(0))));
        // the left side would be evaluated twice (as the seed and for the solutions kept without a partner)
        assertThat(translate(query), containsString("links"));
    }


    @Test
    @DisplayName("a DISTINCT over the start of a walk needs only the zero-length paths")
    void distinctStartReducedToZeroPaths() throws Exception
    {
        String query = "SELECT DISTINCT ?x WHERE { ?x ex:next* ?y }";

        assertThat(execute(query),
                containsInAnyOrder(row(node(0)), row(node(1)), row(node(2)), row(node(3)), row(other(10)),
                        row(other(11)), row(other(12)), row(other(20)), row(string("zero")), row(string("one")),
                        row(string("two")), row(string("three")), row(thing)));
        assertThat(translate(query), not(containsString("WITH RECURSIVE")));
    }


    @Test
    @DisplayName("a DISTINCT over the start of a walk of one or more steps needs only the first step")
    void distinctStartReducedToFirstStep() throws Exception
    {
        String query = "SELECT DISTINCT ?x WHERE { ?x ex:next+ ?y }";

        assertThat(execute(query), containsInAnyOrder(row(node(1)), row(node(2)), row(node(3))));
        assertThat(translate(query), not(containsString("WITH RECURSIVE")));
    }


    @Test
    @DisplayName("a DISTINCT over the end of a walk of one or more steps needs only the first step")
    void distinctEndReducedToFirstStep() throws Exception
    {
        String query = "SELECT DISTINCT ?y WHERE { ?x ex:next+ ?y }";

        assertThat(execute(query), containsInAnyOrder(row(node(0)), row(node(1))));
        assertThat(translate(query), not(containsString("WITH RECURSIVE")));
    }


    @Test
    @DisplayName("a MINUS over a walk whose end it does not share tests only the first step")
    void minusReducedToFirstStep() throws Exception
    {
        String query = "SELECT ?x WHERE { ?x ex:label ?l MINUS { ?x ex:next+ ?y } }";

        assertThat(execute(query), containsInAnyOrder(row(node(0))));
        assertThat(translate(query), not(containsString("WITH RECURSIVE")));
    }


    @Test
    @DisplayName("an EXISTS over a walk whose end is bound nowhere else tests only the first step")
    void existsReducedToFirstStep() throws Exception
    {
        String query = "SELECT ?x WHERE { ?x ex:label ?l FILTER EXISTS { ?x ex:next+ ?y } }";

        assertThat(execute(query), containsInAnyOrder(row(node(1)), row(node(2)), row(node(3))));
        assertThat(translate(query), not(containsString("WITH RECURSIVE")));
    }


    @Test
    @DisplayName("the walk stays when its multiplicities matter")
    void walkKeptForMultiplicities() throws Exception
    {
        String query = "SELECT ?x WHERE { ?x ex:next+ ?y }";

        assertThat(execute(query), containsInAnyOrder(row(node(1)), row(node(2)), row(node(2)), row(node(3))));
        assertThat(translate(query), containsString("WITH RECURSIVE"));
    }


    @Test
    @DisplayName("a VALUES row of a class the parent does not accept is left out")
    void valuesRowPruned() throws Exception
    {
        String query = "SELECT ?x ?l WHERE { VALUES ?x { <http://example.org/n/1> <http://example.org/a/10> } "
                + "?x ex:label ?l }";

        assertThat(execute(query), containsInAnyOrder(row(node(1), string("one"))));
        assertThat(translate(query), not(containsString("'10'")));
    }
}
