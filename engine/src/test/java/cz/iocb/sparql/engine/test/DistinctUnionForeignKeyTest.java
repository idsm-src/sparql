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
import cz.iocb.sparql.engine.database.Conditions;
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
 * Distinct unions of an access to a parent table and an access to a child table that references it by a foreign key. A
 * child row whose key columns are not null has the values of the parent row it references, so the union can be merged
 * into the parent access only when that access takes every parent row or when the condition of the child implies its
 * condition. Otherwise the merged access would miss the child rows that reference parent rows failing its condition,
 * or, widened by the condition of the child, take parent rows that no child row references.
 *
 * The table {@code parent} holds the items 1 to 4 of the kinds 1, 2, 2 and 3, the items 2 and 4 having a note, the
 * table {@code child} references the items 3 (flag 0) and 4 (flag 1), so no child row references the items 1 and 2. The
 * table {@code pair} with the composite key (a, b) holds the pairs (1, 7) and (2, 2); the table {@code pair_child}
 * references the pair (1, 7), and the table {@code pair_child_null}, whose column b is nullable, references the pair
 * (1, 7) and holds the row (9, NULL), which references no pair.
 */
public class DistinctUnionForeignKeyTest
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
     * Creates the {@code fkunion_test} schema with its tables and maps them. The items of {@code parent} are typed
     * ex:Item (every item), ex:KindOne (the items of the kind 1), ex:NotSecond (the items other than 2), ex:Noted (the
     * items with a note) and ex:Unnoted (the items without a note), and the rows of {@code child} refer to their items
     * by ex:refers (every row), ex:refersNotSecond (the rows not referencing the item 2), ex:refersNotThird (the rows
     * not referencing the item 3) and ex:refersFlagged (the rows with the flag 1). The pairs are typed ex:Pair (every
     * pair) and ex:PairNotSeven (the pairs whose b is not 7), the rows of {@code pair_child} refer to the a of their
     * pairs by ex:equalNotSeven (a = b and a is not 7) and ex:notSeven (a is not 7), and the rows of
     * {@code pair_child_null} by ex:refersPair (every row).
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema fkunion_test");

            statement.execute("create table fkunion_test.parent (id int primary key, kind int not null, note int)");
            statement.execute(
                    "insert into fkunion_test.parent values (1, 1, null), (2, 2, 7), (3, 2, null), " + "(4, 3, 9)");

            statement.execute("create table fkunion_test.child (id int primary key, "
                    + "parent int not null references fkunion_test.parent(id), flag int not null)");
            statement.execute("insert into fkunion_test.child values (10, 3, 0), (11, 4, 1)");

            statement.execute("create table fkunion_test.pair (a int not null, b int not null, primary key (a, b))");
            statement.execute("insert into fkunion_test.pair values (1, 7), (2, 2)");

            statement.execute("create table fkunion_test.pair_child (id int primary key, a int not null, "
                    + "b int not null, foreign key (a, b) references fkunion_test.pair)");
            statement.execute("insert into fkunion_test.pair_child values (20, 1, 7)");

            statement.execute("create table fkunion_test.pair_child_null (id int primary key, a int not null, b int, "
                    + "foreign key (a, b) references fkunion_test.pair)");
            statement.execute("insert into fkunion_test.pair_child_null values (30, 1, 7), (31, 9, null)");
        }

        DatabaseSchema schema = new DatabaseSchema(connectionPool);

        config = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);
        config.addPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        config.addPrefix("ex", "http://example.org/");
        config.addIriClass(new IntegerUserIriClass("item", INT4, "http://example.org/item/"));
        config.addIriClass(new IntegerUserIriClass("child", INT4, "http://example.org/child/"));

        DatabaseTable parent = new DatabaseTable("fkunion_test", "parent");
        DatabaseTable child = new DatabaseTable("fkunion_test", "child");
        DatabaseTable pair = new DatabaseTable("fkunion_test", "pair");
        DatabaseTable pairChild = new DatabaseTable("fkunion_test", "pair_child");
        DatabaseTable pairChildNull = new DatabaseTable("fkunion_test", "pair_child_null");
        ConstantIriMapping graph = config.createIriMapping("<http://example.org/graph>");

        config.addQuadMapping(parent, graph, config.createIriMapping("item", "id"), config.createIriMapping("rdf:type"),
                config.createIriMapping("ex:Item"));

        config.addQuadMapping(parent, graph, config.createIriMapping("item", "id"), config.createIriMapping("rdf:type"),
                config.createIriMapping("ex:KindOne"), config.createAreEqualCondition("kind", "'1'::int4"));

        config.addQuadMapping(parent, graph, config.createIriMapping("item", "id"), config.createIriMapping("rdf:type"),
                config.createIriMapping("ex:NotSecond"), config.createAreNotEqualCondition("id", "'2'::int4"));

        config.addQuadMapping(parent, graph, config.createIriMapping("item", "id"), config.createIriMapping("rdf:type"),
                config.createIriMapping("ex:Noted"), config.createIsNotNullCondition(parent, "note"));

        config.addQuadMapping(parent, graph, config.createIriMapping("item", "id"), config.createIriMapping("rdf:type"),
                config.createIriMapping("ex:Unnoted"), config.createIsNullCondition(parent, "note"));

        config.addQuadMapping(child, graph, config.createIriMapping("child", "id"),
                config.createIriMapping("ex:refers"), config.createIriMapping("item", "parent"));

        config.addQuadMapping(child, graph, config.createIriMapping("child", "id"),
                config.createIriMapping("ex:refersNotSecond"), config.createIriMapping("item", "parent"),
                config.createAreNotEqualCondition("parent", "'2'::int4"));

        config.addQuadMapping(child, graph, config.createIriMapping("child", "id"),
                config.createIriMapping("ex:refersNotThird"), config.createIriMapping("item", "parent"),
                config.createAreNotEqualCondition("parent", "'3'::int4"));

        config.addQuadMapping(child, graph, config.createIriMapping("child", "id"),
                config.createIriMapping("ex:refersFlagged"), config.createIriMapping("item", "parent"),
                config.createAreEqualCondition("flag", "'1'::int4"));

        config.addQuadMapping(pair, graph, config.createIriMapping("item", "a"), config.createIriMapping("rdf:type"),
                config.createIriMapping("ex:Pair"));

        config.addQuadMapping(pair, graph, config.createIriMapping("item", "a"), config.createIriMapping("rdf:type"),
                config.createIriMapping("ex:PairNotSeven"), config.createAreNotEqualCondition("b", "'7'::int4"));

        config.addQuadMapping(pairChild, graph, config.createIriMapping("child", "id"),
                config.createIriMapping("ex:equalNotSeven"), config.createIriMapping("item", "a"),
                Conditions.and(config.createAreEqualCondition(pairChild, "a", "b"),
                        config.createAreNotEqualCondition(pairChild, "a", "'7'::int4")));

        config.addQuadMapping(pairChild, graph, config.createIriMapping("child", "id"),
                config.createIriMapping("ex:notSeven"), config.createIriMapping("item", "a"),
                config.createAreNotEqualCondition(pairChild, "a", "'7'::int4"));

        config.addQuadMapping(pairChildNull, graph, config.createIriMapping("child", "id"),
                config.createIriMapping("ex:refersPair"), config.createIriMapping("item", "a"));
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
     * Expected row of the item with the given id.
     *
     * @param id the item id
     * @return row of the item
     */
    private static List<RdfTerm> item(int id)
    {
        return Arrays.asList(new Iri("http://example.org/item/" + id));
    }


    @Test
    @DisplayName("a parent with a condition does not take the parent rows that only the child condition admits")
    void conditionedParent() throws Exception
    {
        String query = "SELECT DISTINCT ?y WHERE { { ?y a ex:KindOne } UNION { ?c ex:refers ?y } }";

        assertThat(execute(query), containsInAnyOrder(item(1), item(3), item(4)));
    }


    @Test
    @DisplayName("a parent with a condition after the child in the union")
    void conditionedParentAfterChild() throws Exception
    {
        String query = "SELECT DISTINCT ?y WHERE { { ?c ex:refers ?y } UNION { ?y a ex:KindOne } }";

        assertThat(execute(query), containsInAnyOrder(item(1), item(3), item(4)));
    }


    @Test
    @DisplayName("a child condition on the key that does not imply the parent condition")
    void unrelatedChildCondition() throws Exception
    {
        String query = "SELECT DISTINCT ?y WHERE { { ?y a ex:KindOne } UNION { ?c ex:refersNotThird ?y } }";

        assertThat(execute(query), containsInAnyOrder(item(1), item(4)));
    }


    @Test
    @DisplayName("a child condition on a column outside the key")
    void childConditionOutsideKey() throws Exception
    {
        String query = "SELECT DISTINCT ?y WHERE { { ?y a ex:KindOne } UNION { ?c ex:refersFlagged ?y } }";

        assertThat(execute(query), containsInAnyOrder(item(1), item(4)));
    }


    @Test
    @DisplayName("a parent without a condition absorbs the child")
    void unconditionedParent() throws Exception
    {
        String query = "SELECT DISTINCT ?y WHERE { { ?y a ex:Item } UNION { ?c ex:refers ?y } }";

        assertThat(execute(query), containsInAnyOrder(item(1), item(2), item(3), item(4)));
        assertThat(translate(query), not(containsString("\"child\"")));
    }


    @Test
    @DisplayName("parent conditions complementary in a null test take every parent row and absorb the child")
    void complementaryParentConditions() throws Exception
    {
        String query = "SELECT DISTINCT ?y WHERE { { ?y a ex:Noted } UNION { ?y a ex:Unnoted } "
                + "UNION { ?c ex:refers ?y } }";

        assertThat(execute(query), containsInAnyOrder(item(1), item(2), item(3), item(4)));
        assertThat(translate(query), not(containsString("\"child\"")));
    }


    @Test
    @DisplayName("a child condition implying the parent condition lets the parent absorb the child")
    void implyingChildCondition() throws Exception
    {
        String query = "SELECT DISTINCT ?y WHERE { { ?y a ex:NotSecond } UNION { ?c ex:refersNotSecond ?y } }";

        assertThat(execute(query), containsInAnyOrder(item(1), item(3), item(4)));
        assertThat(translate(query), not(containsString("\"child\"")));
    }


    @Test
    @DisplayName("a child condition implying the parent condition, the child first in the union")
    void implyingChildConditionBeforeParent() throws Exception
    {
        String query = "SELECT DISTINCT ?y WHERE { { ?c ex:refersNotSecond ?y } UNION { ?y a ex:NotSecond } }";

        assertThat(execute(query), containsInAnyOrder(item(1), item(3), item(4)));
        assertThat(translate(query), not(containsString("\"child\"")));
    }


    @Test
    @DisplayName("an equality of the key columns in one disjunct of the child condition does not hold in the others")
    void compositeKeyEqualityInOneDisjunct() throws Exception
    {
        String query = "SELECT DISTINCT ?y WHERE { { ?c ex:equalNotSeven ?y } UNION { ?c ex:notSeven ?y } "
                + "UNION { ?y a ex:PairNotSeven } }";

        assertThat(execute(query), containsInAnyOrder(item(1), item(2)));
    }


    @Test
    @DisplayName("a child row with a null in the key references no parent row")
    void nullableKeyColumn() throws Exception
    {
        String query = "SELECT DISTINCT ?y WHERE { { ?y a ex:Pair } UNION { ?c ex:refersPair ?y } }";

        assertThat(execute(query), containsInAnyOrder(item(1), item(2), item(9)));
    }
}
