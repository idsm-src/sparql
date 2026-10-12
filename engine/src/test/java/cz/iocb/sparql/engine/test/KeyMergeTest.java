package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
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
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.LimitExceedException;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Request.PreparedQuery;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.translator.ServiceException;
import cz.iocb.sparql.engine.translator.TranslateVisitor;
import cz.iocb.sparql.testing.Database;



/**
 * Merges of table accesses decided by keys: two accesses to one table joined on its primary key, an access to a child
 * table joined with an access to the parent table it references by a foreign key, and the deduplication of an access
 * implied by a key. The decisions must only count on equalities holding in every disjunct of the conditions, a parent
 * condition equating a key column with another column cannot be moved onto the child row, and a child row with a null
 * in any column of a foreign key references no parent row.
 *
 * The table {@code parent} holds the items 1 to 4 of the kinds 1, 2, 2 and 3, so only the items 1 and 2 are of their
 * own kind; the table {@code child} references the items 3, 4 and 1. The table {@code item} holds the rows (id, a, b)
 * (1, 7, 10), (7, 7, 70) and (3, 3, 30), so a is 7 or equal to id in every row. The table {@code pair} with the
 * composite key (a, b) holds the pairs (1, 1) and (5, 7); the table {@code pair_child} references the pair (5, 7), and
 * the table {@code pair_child_null}, whose column b is nullable, references the pair (1, 1) and holds the row (9,
 * NULL), which references no pair. The table {@code other} holds the item 50.
 */
public class KeyMergeTest
{
    /**
     * Prefix declaration prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> ";

    /**
     * Pool of the test database.
     */
    private static DataSource connectionPool = null;

    /**
     * Configuration mapping the tables.
     */
    private static SparqlDatabaseConfiguration config = null;


    /**
     * Creates the {@code keymerge_test} schema with its tables and maps them. The items of {@code parent} are typed
     * ex:Item and refer to the items of their kinds by ex:hasKind, the rows of {@code child} refer to their items by
     * ex:refers. The rows of {@code item} refer to the items of their a by ex:r when a is 7 or equal to id (a
     * disjunction whose second disjunct alone equates a with the key) and to the items of their b by ex:s. The pairs
     * are typed ex:Pair by their a, the rows of {@code pair_child} refer to the a of their pairs by ex:refersPair and
     * the rows of {@code pair_child_null} by ex:refersPairNull. The rows of {@code other} are typed ex:Other.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema keymerge_test");

            statement.execute("create table keymerge_test.parent (id int primary key, kind int not null)");
            statement.execute("insert into keymerge_test.parent values (1, 1), (2, 2), (3, 2), (4, 3)");

            statement.execute("create table keymerge_test.child (id int primary key, "
                    + "parent int not null references keymerge_test.parent(id))");
            statement.execute("insert into keymerge_test.child values (10, 3), (11, 4), (12, 1)");

            statement.execute("create table keymerge_test.item (id int primary key, a int not null, b int not null)");
            statement.execute("insert into keymerge_test.item values (1, 7, 10), (7, 7, 70), (3, 3, 30)");

            statement.execute("create table keymerge_test.pair (a int not null, b int not null, primary key (a, b))");
            statement.execute("insert into keymerge_test.pair values (1, 1), (5, 7)");

            statement.execute("create table keymerge_test.pair_child (id int primary key, a int not null, "
                    + "b int not null, foreign key (a, b) references keymerge_test.pair)");
            statement.execute("insert into keymerge_test.pair_child values (20, 5, 7)");

            statement.execute("create table keymerge_test.pair_child_null (id int primary key, a int not null, "
                    + "b int, foreign key (a, b) references keymerge_test.pair)");
            statement.execute("insert into keymerge_test.pair_child_null values (30, 1, 1), (31, 9, null)");

            statement.execute("create table keymerge_test.other (id int primary key)");
            statement.execute("insert into keymerge_test.other values (50)");
        }

        DatabaseSchema schema = new DatabaseSchema(connectionPool);

        config = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);
        config.addPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        config.addPrefix("ex", "http://example.org/");
        config.addIriClass(new IntegerUserIriClass("item", INT4, "http://example.org/item/"));
        config.addIriClass(new IntegerUserIriClass("child", INT4, "http://example.org/child/"));

        DatabaseTable parent = new DatabaseTable("keymerge_test", "parent");
        DatabaseTable child = new DatabaseTable("keymerge_test", "child");
        DatabaseTable item = new DatabaseTable("keymerge_test", "item");
        DatabaseTable pair = new DatabaseTable("keymerge_test", "pair");
        DatabaseTable pairChild = new DatabaseTable("keymerge_test", "pair_child");
        DatabaseTable pairChildNull = new DatabaseTable("keymerge_test", "pair_child_null");
        DatabaseTable other = new DatabaseTable("keymerge_test", "other");
        ConstantIriMapping graph = config.createIriMapping("<http://example.org/graph>");

        config.addQuadMapping(parent, graph, config.createIriMapping("item", "id"), config.createIriMapping("rdf:type"),
                config.createIriMapping("ex:Item"));

        config.addQuadMapping(parent, graph, config.createIriMapping("item", "id"),
                config.createIriMapping("ex:hasKind"), config.createIriMapping("item", "kind"));

        config.addQuadMapping(child, graph, config.createIriMapping("child", "id"),
                config.createIriMapping("ex:refers"), config.createIriMapping("item", "parent"));

        config.addQuadMapping(item, graph, config.createIriMapping("item", "id"), config.createIriMapping("ex:r"),
                config.createIriMapping("item", "a"), config.createAreEqualCondition(item, "a", "'7'::int4", "id"));

        config.addQuadMapping(item, graph, config.createIriMapping("item", "id"), config.createIriMapping("ex:s"),
                config.createIriMapping("item", "b"));

        config.addQuadMapping(pair, graph, config.createIriMapping("item", "a"), config.createIriMapping("rdf:type"),
                config.createIriMapping("ex:Pair"));

        config.addQuadMapping(pairChild, graph, config.createIriMapping("child", "id"),
                config.createIriMapping("ex:refersPair"), config.createIriMapping("item", "a"));

        config.addQuadMapping(pairChildNull, graph, config.createIriMapping("child", "id"),
                config.createIriMapping("ex:refersPairNull"), config.createIriMapping("item", "a"));

        config.addQuadMapping(other, graph, config.createIriMapping("item", "id"), config.createIriMapping("rdf:type"),
                config.createIriMapping("ex:Other"));
    }


    /**
     * Translates the query into SQL.
     *
     * @param query the query, without the prefix declaration
     * @return the generated SQL
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static String translate(String query) throws SQLException, TranslateExceptions, ServiceException
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
     * Runs the query and returns all rows.
     *
     * @param query the query, without the prefix declaration
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
     * Expected row of the given items.
     *
     * @param ids the item ids
     * @return row of the items
     */
    private static List<RdfTerm> items(int... ids)
    {
        List<RdfTerm> row = new ArrayList<>();

        for(int id : ids)
            row.add(new Iri("http://example.org/item/" + id));

        return row;
    }


    /**
     * Expected row of the given child row and item.
     *
     * @param child the child id
     * @param item the item id
     * @return row of the child and the item
     */
    private static List<RdfTerm> childItem(int child, int item)
    {
        return Arrays.asList(new Iri("http://example.org/child/" + child), new Iri("http://example.org/item/" + item));
    }


    @Test
    @DisplayName("a parent condition equating the key with another column is kept when the parent is merged")
    void parentEqualityWithNonKeyColumn() throws Exception
    {
        String query = "SELECT ?c ?y WHERE { ?c ex:refers ?y . ?y ex:hasKind ?y }";

        assertThat(execute(query), containsInAnyOrder(childItem(12, 1)));
    }


    @Test
    @DisplayName("a parent without a condition outside the key is merged into the child")
    void parentMergedIntoChild() throws Exception
    {
        String query = "SELECT ?c ?y WHERE { ?c ex:refers ?y . ?y a ex:Item }";

        assertThat(execute(query), containsInAnyOrder(childItem(10, 3), childItem(11, 4), childItem(12, 1)));
        assertThat(translate(query), not(containsString("\"keymerge_test\".\"parent\"")));
    }


    @Test
    @DisplayName("an equality with the key in one disjunct only does not join two accesses on the key")
    void keyEqualityInOneDisjunct() throws Exception
    {
        String query = "SELECT ?x ?v ?w WHERE { ?x ex:r ?v . ?v ex:s ?w }";

        assertThat(execute(query), containsInAnyOrder(items(1, 7, 70), items(7, 7, 70), items(3, 3, 30)));
    }


    @Test
    @DisplayName("an equality with the key in one disjunct only does not make the rows distinct")
    void distinctByKeyEqualityInOneDisjunct() throws Exception
    {
        String query = "SELECT DISTINCT ?v WHERE { ?x ex:r ?v }";

        assertThat(execute(query), containsInAnyOrder(items(7), items(3)));
    }


    @Test
    @DisplayName("a distinct access to the parent is merged into a child with a full not-null key")
    void distinctParentMergedIntoChild() throws Exception
    {
        String query = "SELECT ?c ?y WHERE { ?c ex:refersPair ?y . { SELECT DISTINCT ?y WHERE { ?y a ex:Pair } } }";

        assertThat(execute(query), containsInAnyOrder(childItem(20, 5)));
        assertThat(translate(query), not(containsString("\"keymerge_test\".\"pair\"")));
    }


    @Test
    @DisplayName("a child row with a null in a key column not joined references no parent row")
    void nullInUnjoinedKeyColumn() throws Exception
    {
        String query = "SELECT ?c ?y WHERE { ?c ex:refersPairNull ?y . { SELECT DISTINCT ?y WHERE { ?y a ex:Pair } } }";

        assertThat(execute(query), containsInAnyOrder(childItem(30, 1)));
    }


    @Test
    @DisplayName("a child row with a null in a key column not joined, the parent in a distinct union")
    void nullInUnjoinedKeyColumnWithUnion() throws Exception
    {
        String query = "SELECT ?c ?y WHERE { ?c ex:refersPairNull ?y . "
                + "{ SELECT DISTINCT ?y WHERE { { ?y a ex:Pair } UNION { ?y a ex:Other } } } }";

        assertThat(execute(query), containsInAnyOrder(childItem(30, 1)));
    }
}
