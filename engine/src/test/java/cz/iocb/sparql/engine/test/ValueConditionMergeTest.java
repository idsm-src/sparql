package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.rdfLangString;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
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
import cz.iocb.sparql.engine.mapping.classes.IntegerUserIriClass;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.LangStringLiteral;
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
 * Table accesses whose rows are restricted to a value (the term a filter requires of a variable, taken into the
 * conditions of the accesses binding the variable) are merged like the accesses without one: an access to a referenced
 * table is merged into the access referencing it, which takes the value in the columns of the foreign key; a distinct
 * union of a referenced and a referencing access is merged into the referenced one when the referencing one is
 * restricted to the same value; a distinct union that does not restrict the access it is joined with is left out; a
 * union of two accesses to one table whose conditions are complementary is read by a single scan restricted to the
 * value; and a CONSTRUCT with one template over an access is that access itself.
 *
 * The table {@code parent} holds the items 1 to 5 of the kinds 1, 2, 2, 3 and 1, the items 2, 4 and 5 having a note;
 * the rows 10 to 13 of the table {@code child} reference the items 3, 4, 3 and 5; the table {@code labels} holds the
 * labels of the items 1, 3 and 4 with an optional language; the table {@code pair} with the composite key holds the
 * pairs (1, 7), (1, 8) and (2, 7), which the rows 20 to 22 of the table {@code pair_child} reference.
 */
public class ValueConditionMergeTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = """
            PREFIX ex: <http://example.org/> PREFIX item: <http://example.org/item/> \
            PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\s""";

    /**
     * Configuration mapping the test tables.
     */
    private static SparqlDatabaseConfiguration config = null;


    /**
     * Creates the {@code value_merge_test} schema with its tables and maps them. The items of {@code parent} are typed
     * ex:Item (every item), ex:KindOne (the items of the kind 1), ex:NotSecond (the items other than 2) and ex:Noted
     * (the items with a note) and have their kinds as ex:kind; the rows of {@code child} refer to their items by
     * ex:refers (every row) and ex:refersNotSecond (the rows not referencing the item 2); the labels are mapped as
     * rdfs:label by a complementary pair of mappings (plain strings where the language is null, tagged strings
     * elsewhere); the pairs link their first item to the second one by ex:second, and the rows of {@code pair_child}
     * refer to the items of their pairs by ex:pairFirst and ex:pairSecond.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema value_merge_test");

            statement.execute("create table value_merge_test.parent (id int primary key, kind int not null, note int)");
            statement.execute("insert into value_merge_test.parent values (1, 1, null), (2, 2, 7), (3, 2, null), "
                    + "(4, 3, 9), (5, 1, 5)");

            statement.execute("create table value_merge_test.child (id int primary key, "
                    + "item int not null references value_merge_test.parent(id))");
            statement.execute("insert into value_merge_test.child values (10, 3), (11, 4), (12, 3), (13, 5)");

            statement.execute("create table value_merge_test.labels (id int not null, label varchar not null, "
                    + "language varchar)");
            statement.execute("insert into value_merge_test.labels values (1, 'one', null), (1, 'jedna', 'cs'), "
                    + "(3, 'drei', 'de'), (4, 'four', null), (4, 'vier', 'de')");

            statement.execute("create table value_merge_test.pair (a int, b int, primary key (a, b))");
            statement.execute("insert into value_merge_test.pair values (1, 7), (1, 8), (2, 7)");

            statement.execute("create table value_merge_test.pair_child (id int primary key, a int not null, "
                    + "b int not null, foreign key (a, b) references value_merge_test.pair)");
            statement.execute("insert into value_merge_test.pair_child values (20, 1, 7), (21, 1, 8), (22, 2, 7)");
        }

        DatabaseSchema schema = new DatabaseSchema(connectionPool);

        config = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);
        config.addPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        config.addPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
        config.addPrefix("ex", "http://example.org/");
        config.addIriClass(new IntegerUserIriClass("item", INT4, "http://example.org/item/"));
        config.addIriClass(new IntegerUserIriClass("child", INT4, "http://example.org/child/"));

        DatabaseTable parent = new DatabaseTable("value_merge_test", "parent");
        DatabaseTable child = new DatabaseTable("value_merge_test", "child");
        DatabaseTable labels = new DatabaseTable("value_merge_test", "labels");
        DatabaseTable pair = new DatabaseTable("value_merge_test", "pair");
        DatabaseTable pairChild = new DatabaseTable("value_merge_test", "pair_child");

        config.addQuadMapping(parent, null, config.createIriMapping("item", "id"), config.createIriMapping("rdf:type"),
                config.createIriMapping("ex:Item"));

        config.addQuadMapping(parent, null, config.createIriMapping("item", "id"), config.createIriMapping("rdf:type"),
                config.createIriMapping("ex:KindOne"), config.createAreEqualCondition("kind", "'1'::int4"));

        config.addQuadMapping(parent, null, config.createIriMapping("item", "id"), config.createIriMapping("rdf:type"),
                config.createIriMapping("ex:NotSecond"), config.createAreNotEqualCondition("id", "'2'::int4"));

        config.addQuadMapping(parent, null, config.createIriMapping("item", "id"), config.createIriMapping("rdf:type"),
                config.createIriMapping("ex:Noted"), config.createIsNotNullCondition(parent, "note"));

        config.addQuadMapping(parent, null, config.createIriMapping("item", "id"), config.createIriMapping("ex:kind"),
                config.createLiteralMapping(xsdInt, "kind"));

        config.addQuadMapping(child, null, config.createIriMapping("child", "id"), config.createIriMapping("ex:refers"),
                config.createIriMapping("item", "item"));

        config.addQuadMapping(child, null, config.createIriMapping("child", "id"),
                config.createIriMapping("ex:refersNotSecond"), config.createIriMapping("item", "item"),
                config.createAreNotEqualCondition("item", "'2'::int4"));

        config.addQuadMapping(labels, null, config.createIriMapping("item", "id"),
                config.createIriMapping("rdfs:label"), config.createLiteralMapping(xsdString, "label"),
                config.createIsNullCondition(labels, "language"));

        config.addQuadMapping(labels, null, config.createIriMapping("item", "id"),
                config.createIriMapping("rdfs:label"), config.createLiteralMapping(rdfLangString, "label", "language"));

        config.addQuadMapping(pair, null, config.createIriMapping("item", "a"), config.createIriMapping("ex:second"),
                config.createIriMapping("item", "b"));

        config.addQuadMapping(pairChild, null, config.createIriMapping("child", "id"),
                config.createIriMapping("ex:pairFirst"), config.createIriMapping("item", "a"));

        config.addQuadMapping(pairChild, null, config.createIriMapping("child", "id"),
                config.createIriMapping("ex:pairSecond"), config.createIriMapping("item", "b"));
    }


    /**
     * Translates the query into SQL.
     *
     * @param query the query, without the prefix declarations
     * @return the generated SQL
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws LimitExceedException if the generated SQL is too long
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static String translate(String query)
            throws SQLException, TranslateExceptions, LimitExceedException, ServiceException
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
     * True if the SQL reads the table.
     *
     * @param sql the SQL
     * @param table name of the table
     * @return true if the SQL reads the table, false otherwise
     */
    private static boolean reads(String sql, String table)
    {
        return sql.contains("\"value_merge_test\".\"" + table + "\"");
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
     * IRI of the item with the given id.
     *
     * @param id the id
     * @return IRI of the item
     */
    private static Iri item(int id)
    {
        return new Iri("http://example.org/item/" + id);
    }


    /**
     * IRI of the child row with the given id.
     *
     * @param id the id
     * @return IRI of the child row
     */
    private static Iri child(int id)
    {
        return new Iri("http://example.org/child/" + id);
    }


    /**
     * Plain string literal.
     *
     * @param value the value
     * @return plain string literal
     */
    private static TypedLiteral string(String value)
    {
        return new TypedLiteral(value, xsdStringIri);
    }


    /**
     * Literal of the datatype {@code xsd:int}.
     *
     * @param value the value
     * @return literal of the datatype {@code xsd:int}
     */
    private static TypedLiteral number(int value)
    {
        return new TypedLiteral(Integer.toString(value), xsdIntIri);
    }


    @Test
    @DisplayName("a referenced access restricted to a value is merged into the access referencing it")
    void foreignKeyJoin() throws Exception
    {
        // the filter restricts the access of the sub-select alone
        String query = "SELECT ?c WHERE { ?c ex:refers ?y { SELECT ?y WHERE { ?y a ex:Item FILTER(?y = item:3) } } }";

        assertThat(execute(query), containsInAnyOrder(row(child(10)), row(child(12))));

        String sql = translate(query);

        assertThat(reads(sql, "parent"), is(false));
        assertThat(sql, containsString("\"item\" = '3'::int4"));

        // the condition of the referenced access moves along with the value
        query = "SELECT ?c WHERE { ?c ex:refers ?y { SELECT ?y WHERE { ?y a ex:NotSecond FILTER(?y = item:4) } } }";

        assertThat(execute(query), containsInAnyOrder(row(child(11))));
        assertThat(reads(translate(query), "parent"), is(false));

        query = "SELECT ?c WHERE { ?c ex:refers ?y { SELECT ?y WHERE { ?y a ex:NotSecond FILTER(?y = item:2) } } }";

        assertThat(execute(query), empty());
    }


    @Test
    @DisplayName("a referenced access restricted to a value in a column of a composite key is merged")
    void compositeForeignKeyJoin() throws Exception
    {
        String query = """
                SELECT ?c WHERE { ?c ex:pairFirst ?y ; ex:pairSecond ?z \
                { SELECT ?y ?z WHERE { ?y ex:second ?z FILTER(?y = item:1) } } }""";

        assertThat(execute(query), containsInAnyOrder(row(child(20)), row(child(21))));

        String sql = translate(query);

        assertThat(reads(sql, "pair"), is(false));
        assertThat(sql, containsString("\"a\" = '1'::int4"));

        query = """
                SELECT ?c WHERE { ?c ex:pairFirst ?y ; ex:pairSecond ?z \
                { SELECT ?y ?z WHERE { ?y ex:second ?z FILTER(?z = item:7 && ?y = item:2) } } }""";

        assertThat(execute(query), containsInAnyOrder(row(child(22))));
        assertThat(reads(translate(query), "pair"), is(false));
    }


    @Test
    @DisplayName("a referenced access binding a column outside the key stays")
    void foreignKeyJoinOutsideKey() throws Exception
    {
        String query = """
                SELECT ?c ?k WHERE { ?c ex:refers ?y \
                { SELECT ?y ?k WHERE { ?y ex:kind ?k FILTER(?y = item:3) } } }""";

        assertThat(execute(query), containsInAnyOrder(row(child(10), number(2)), row(child(12), number(2))));

        String sql = translate(query);

        assertThat(reads(sql, "parent"), is(true));
        assertThat(reads(sql, "child"), is(true));
    }


    @Test
    @DisplayName("a distinct union restricted like the access it is joined with is left out")
    void distinctUnionJoin() throws Exception
    {
        // every row of the table child references an item, whatever the other branch of the union adds
        String query = """
                SELECT ?c WHERE { ?c ex:refers ?y \
                { SELECT DISTINCT ?y WHERE { { ?y a ex:Item } UNION { ?y ex:second ?z } } } FILTER(?y = item:3) }""";

        assertThat(execute(query), containsInAnyOrder(row(child(10)), row(child(12))));

        String sql = translate(query);

        assertThat(reads(sql, "parent"), is(false));
        assertThat(reads(sql, "pair"), is(false));
        assertThat(sql, containsString("\"item\" = '3'::int4"));

        // a union restricted by itself restricts the access
        query = """
                SELECT ?c WHERE { ?c ex:refers ?y \
                { SELECT DISTINCT ?y WHERE { { ?y a ex:Item } UNION { ?y ex:second ?z } FILTER(?y = item:3) } } }""";

        assertThat(execute(query), containsInAnyOrder(row(child(10)), row(child(12))));

        // a union none of whose branches holds every referenced item restricts the access as well
        query = """
                SELECT ?c WHERE { ?c ex:refers ?y \
                { SELECT DISTINCT ?y WHERE { { ?y a ex:KindOne } UNION { ?y a ex:Noted } } } FILTER(?y = item:3) }""";

        assertThat(execute(query), empty());

        query = """
                SELECT ?c WHERE { ?c ex:refers ?y \
                { SELECT DISTINCT ?y WHERE { { ?y a ex:KindOne } UNION { ?y a ex:Noted } } } FILTER(?y = item:5) }""";

        assertThat(execute(query), containsInAnyOrder(row(child(13))));
    }


    @Test
    @DisplayName("a distinct union of a referenced and a referencing access restricted alike is the referenced one")
    void distinctUnion() throws Exception
    {
        String query = "SELECT DISTINCT ?y WHERE { { ?y a ex:Item } UNION { ?c ex:refers ?y } FILTER(?y = item:3) }";

        assertThat(execute(query), containsInAnyOrder(row(item(3))));

        String sql = translate(query);

        assertThat(reads(sql, "child"), is(false));
        assertThat(sql, containsString("\"id\" = '3'::int4"));

        // the conditions have to agree as before
        query = """
                SELECT DISTINCT ?y WHERE { { ?y a ex:NotSecond } UNION { ?c ex:refersNotSecond ?y } \
                FILTER(?y = item:4) }""";

        assertThat(execute(query), containsInAnyOrder(row(item(4))));
        assertThat(reads(translate(query), "child"), is(false));

        query = "SELECT DISTINCT ?y WHERE { { ?y a ex:KindOne } UNION { ?c ex:refers ?y } FILTER(?y = item:3) }";

        assertThat(execute(query), containsInAnyOrder(row(item(3))));
        assertThat(reads(translate(query), "child"), is(true));

        query = "SELECT DISTINCT ?y WHERE { { ?y a ex:KindOne } UNION { ?c ex:refers ?y } FILTER(?y = item:1) }";

        assertThat(execute(query), containsInAnyOrder(row(item(1))));
    }


    @Test
    @DisplayName("a value restricting the referencing access alone leaves the referenced access as it is")
    void distinctUnionOfRestrictedChild() throws Exception
    {
        String query = "SELECT DISTINCT ?y WHERE { { ?y a ex:Item } UNION { ?c ex:refers ?y FILTER(?y = item:3) } }";

        assertThat(execute(query),
                containsInAnyOrder(row(item(1)), row(item(2)), row(item(3)), row(item(4)), row(item(5))));
        assertThat(reads(translate(query), "child"), is(false));
    }


    @Test
    @DisplayName("a value restricting the referenced access alone keeps the referencing access")
    void distinctUnionOfRestrictedParent() throws Exception
    {
        String query = "SELECT DISTINCT ?y WHERE { { ?y a ex:Item FILTER(?y = item:1) } UNION { ?c ex:refers ?y } }";

        assertThat(execute(query), containsInAnyOrder(row(item(1)), row(item(3)), row(item(4)), row(item(5))));

        String sql = translate(query);

        assertThat(reads(sql, "parent"), is(true));
        assertThat(reads(sql, "child"), is(true));
    }


    @Test
    @DisplayName("accesses restricted to a value of a current solution are merged alike")
    void currentSolution() throws Exception
    {
        // the filter reads the current solution, so the accesses of the pattern look the item of that solution up
        String query = """
                SELECT ?y WHERE { ?y ex:kind ?k FILTER EXISTS \
                { { SELECT DISTINCT ?y WHERE { { ?y a ex:Item } UNION { ?c ex:refers ?y } } } FILTER(?k < 3) } }""";

        assertThat(execute(query), containsInAnyOrder(row(item(1)), row(item(2)), row(item(3)), row(item(5))));
        assertThat(reads(translate(query), "child"), is(false));

        query = """
                SELECT ?y WHERE { ?y ex:kind ?k FILTER EXISTS { ?c ex:refers ?y \
                { SELECT DISTINCT ?y WHERE { { ?y a ex:Item } UNION { ?y rdfs:label ?l } } } FILTER(?k < 3) } }""";

        assertThat(execute(query), containsInAnyOrder(row(item(3)), row(item(5))));
        assertThat(reads(translate(query), "labels"), is(false));
    }


    @Test
    @DisplayName("a complementary pair of accesses restricted to a value is read by a single scan")
    void unionScan() throws Exception
    {
        String query = "SELECT ?c ?l WHERE { ?c ex:refers ?y OPTIONAL { ?y rdfs:label ?l } FILTER(?y = item:4) }";

        assertThat(execute(query), containsInAnyOrder(row(child(11), string("four")),
                row(child(11), new LangStringLiteral("vier", "de"))));

        String sql = translate(query);

        assertThat(sql, not(containsString("UNION ALL")));
        assertThat(sql, containsString("CASE WHEN"));
        assertThat(sql, containsString("\"id\" = '4'::int4"));

        query = "SELECT ?c ?l WHERE { ?c ex:refers ?y OPTIONAL { ?y rdfs:label ?l } FILTER(?y = item:3) }";

        assertThat(execute(query), containsInAnyOrder(row(child(10), new LangStringLiteral("drei", "de")),
                row(child(12), new LangStringLiteral("drei", "de"))));

        query = "SELECT ?c ?l WHERE { ?c ex:refers ?y OPTIONAL { ?y rdfs:label ?l } FILTER(?y = item:5) }";

        assertThat(execute(query), containsInAnyOrder(row(child(13), null)));
    }


    @Test
    @DisplayName("a CONSTRUCT with one template over an access restricted to a value is that access")
    void construct() throws Exception
    {
        String query = "CONSTRUCT { ?c ex:refers ?y } WHERE { ?c ex:refers ?y FILTER(?y = item:3) }";
        Iri refers = new Iri("http://example.org/refers");

        assertThat(execute(query),
                containsInAnyOrder(row(child(10), refers, item(3)), row(child(12), refers, item(3))));

        String sql = translate(query);

        // the rows of the table are distinct by its key, so there is nothing to deduplicate
        assertThat(sql, not(containsString("GROUP BY")));
        assertThat(sql, not(containsString("DISTINCT")));
        assertThat(sql, containsString("\"item\" = '3'::int4"));

        query = "CONSTRUCT { ?y ex:of ?c } WHERE { ?c ex:refers ?y FILTER(?c = <http://example.org/child/11>) }";

        assertThat(execute(query), containsInAnyOrder(row(item(4), new Iri("http://example.org/of"), child(11))));
    }
}
