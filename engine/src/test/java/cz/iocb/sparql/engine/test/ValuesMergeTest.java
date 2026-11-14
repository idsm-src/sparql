package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
import static java.util.stream.Collectors.joining;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntFunction;
import java.util.stream.IntStream;
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
 * VALUES joined with a table access. A short list is merged into the access, whose conditions get a disjunct for every
 * row of the list; a long list stays a list the access is joined with, and so does a list whose rows would multiply the
 * disjuncts the access has got from another list, because the time of handling the conditions grows with the square of
 * the number of their disjuncts. A single row adds no disjunct, so it is merged always. Two accesses that have taken in
 * lists of one variable are merged into an access of the values the lists have in common, as two different values
 * contradict each other; two accesses that have taken in lists of different variables are merged, along a key or a
 * foreign key, only while the lists give few disjuncts together, and stay apart otherwise.
 *
 * The table {@code item} holds the items 1 to 300, the item i with the number 10 * i and the label {@code l<i>}; the
 * table {@code ref} holds the references 1 to 300, the reference i referring to the item i.
 */
public class ValuesMergeTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> "
            + "PREFIX item: <http://example.org/item/> PREFIX ref: <http://example.org/ref/> ";

    /**
     * Configuration mapping the table.
     */
    private static SparqlDatabaseConfiguration config = null;


    /**
     * Creates the {@code values_merge_test} schema with the tables {@code item} and {@code ref} and maps the numbers of
     * the items by ex:num, their labels by ex:label and their type ex:Item, and the items of the references by ex:ref.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema values_merge_test");
            statement.execute("create table values_merge_test.item (id int primary key, num int not null, "
                    + "label varchar not null)");
            statement.execute("insert into values_merge_test.item select i, 10 * i, 'l' || i "
                    + "from generate_series(1, 300) as i");
            statement.execute("create table values_merge_test.ref (id int primary key, "
                    + "item int not null references values_merge_test.item(id))");
            statement.execute("insert into values_merge_test.ref select i, i from generate_series(1, 300) as i");
        }

        DatabaseSchema schema = new DatabaseSchema(connectionPool);

        config = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);
        config.addPrefix("ex", "http://example.org/");
        config.addPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        config.addIriClass(new IntegerUserIriClass("item", INT4, "http://example.org/item/"));
        config.addIriClass(new IntegerUserIriClass("ref", INT4, "http://example.org/ref/"));

        DatabaseTable table = new DatabaseTable("values_merge_test", "item");
        DatabaseTable ref = new DatabaseTable("values_merge_test", "ref");
        ConstantIriMapping graph = config.createIriMapping("<http://example.org/graph>");

        config.addQuadMapping(table, graph, config.createIriMapping("item", "id"), config.createIriMapping("ex:num"),
                config.createLiteralMapping(xsdInt, "num"));
        config.addQuadMapping(table, graph, config.createIriMapping("item", "id"), config.createIriMapping("ex:label"),
                config.createLiteralMapping(xsdString, "label"));
        config.addQuadMapping(table, graph, config.createIriMapping("item", "id"), config.createIriMapping("rdf:type"),
                config.createIriMapping("ex:Item"));
        config.addQuadMapping(ref, graph, config.createIriMapping("ref", "id"), config.createIriMapping("ex:ref"),
                config.createIriMapping("item", "item"));
    }


    /**
     * Translates the query into SQL.
     *
     * @param query the query, without the prefix declarations
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
     * The items of a range, as a VALUES clause writes them.
     *
     * @param first the first item
     * @param last the last item
     * @return the items separated by spaces
     */
    private static String items(int first, int last)
    {
        return IntStream.rangeClosed(first, last).mapToObj(i -> "item:" + i).collect(joining(" "));
    }


    /**
     * The references of a range, as a VALUES clause writes them.
     *
     * @param first the first reference
     * @param last the last reference
     * @return the references separated by spaces
     */
    private static String refs(int first, int last)
    {
        return IntStream.rangeClosed(first, last).mapToObj(i -> "ref:" + i).collect(joining(" "));
    }


    /**
     * The labels of the items 1 to the given one, as a VALUES clause writes them.
     *
     * @param count the number of the items
     * @return the labels separated by spaces
     */
    private static String labels(int count)
    {
        return IntStream.rangeClosed(1, count).mapToObj(i -> "'l" + i + "'").collect(joining(" "));
    }


    /**
     * Expected rows of the items of a range, each with a term of its own.
     *
     * @param first the first item
     * @param last the last item
     * @param term gives the term of an item
     * @return the rows of the items and their terms
     */
    private static Object[] rows(int first, int last, IntFunction<RdfTerm> term)
    {
        return IntStream.rangeClosed(first, last).mapToObj(i -> Arrays.asList(item(i), term.apply(i))).toArray();
    }


    /**
     * An item as its IRI.
     *
     * @param item the item
     * @return the IRI of the item
     */
    private static RdfTerm item(int item)
    {
        return new Iri("http://example.org/item/" + item);
    }


    /**
     * A reference as its IRI.
     *
     * @param ref the reference
     * @return the IRI of the reference
     */
    private static RdfTerm ref(int ref)
    {
        return new Iri("http://example.org/ref/" + ref);
    }


    /**
     * The number of an item as its xsd:int literal.
     *
     * @param item the item
     * @return the literal of the number of the item
     */
    private static RdfTerm number(int item)
    {
        return new TypedLiteral(Integer.toString(10 * item), xsdIntIri);
    }


    /**
     * The label of an item as its xsd:string literal.
     *
     * @param item the item
     * @return the literal of the label of the item
     */
    private static RdfTerm label(int item)
    {
        return new TypedLiteral("l" + item, xsdStringIri);
    }


    /**
     * How many times the SQL holds the text.
     *
     * @param sql the SQL
     * @param text the text
     * @return number of the occurrences of the text in the SQL
     */
    private static int count(String sql, String text)
    {
        return (sql.length() - sql.replace(text, "").length()) / text.length();
    }


    /**
     * How many times the SQL reads the table.
     *
     * @param sql the SQL
     * @param table the name of a table of the test schema
     * @return number of the accesses to the table in the SQL
     */
    private static int reads(String sql, String table)
    {
        return count(sql, "\"values_merge_test\".\"" + table + "\"");
    }


    /**
     * How many lists of values the SQL holds.
     *
     * @param sql the SQL
     * @return number of the lists of values in the SQL
     */
    private static int lists(String sql)
    {
        return count(sql, "(VALUES ");
    }


    @Test
    @DisplayName("a short list is merged into the access")
    void shortList() throws Exception
    {
        String query = "SELECT ?s ?n WHERE { VALUES ?s { " + items(1, 8) + " } ?s ex:num ?n }";

        assertThat(execute(query), containsInAnyOrder(rows(1, 8, ValuesMergeTest::number)));
        assertThat(lists(translate(query)), is(0));
    }


    @Test
    @DisplayName("a long list stays a list joined with the access")
    void longList() throws Exception
    {
        String query = "SELECT ?s ?n WHERE { VALUES ?s { " + items(1, 200) + " } ?s ex:num ?n }";

        assertThat(execute(query), containsInAnyOrder(rows(1, 200, ValuesMergeTest::number)));
        assertThat(lists(translate(query)), is(1));
    }


    @Test
    @DisplayName("short lists of two variables are merged while they give few disjuncts together")
    void fewDisjuncts() throws Exception
    {
        String query = "SELECT ?s ?l WHERE { VALUES ?s { " + items(1, 5) + " } VALUES ?l { " + labels(5)
                + " } ?s ex:label ?l }";

        assertThat(execute(query), containsInAnyOrder(rows(1, 5, ValuesMergeTest::label)));
        assertThat(lists(translate(query)), is(0));
    }


    @Test
    @DisplayName("a list that would multiply the disjuncts of a merged list stays a list")
    void multipliedDisjuncts() throws Exception
    {
        String query = "SELECT ?s ?l WHERE { VALUES ?s { " + items(1, 20) + " } VALUES ?l { " + labels(20)
                + " } ?s ex:label ?l }";

        assertThat(execute(query), containsInAnyOrder(rows(1, 20, ValuesMergeTest::label)));
        assertThat(lists(translate(query)), is(1));
    }


    @Test
    @DisplayName("two accesses with lists of one variable are merged into an access of their common values")
    void commonValues() throws Exception
    {
        String query = "SELECT ?s ?n WHERE { { VALUES ?s { " + items(1, 20) + " } ?s ex:num ?n } { VALUES ?s { "
                + items(11, 30) + " } ?s ex:label ?l } }";

        assertThat(execute(query), containsInAnyOrder(rows(11, 20, ValuesMergeTest::number)));

        String sql = translate(query);

        // one access with an equality for each common item, and no comparison of two different items
        assertThat(reads(sql, "item"), is(1));
        assertThat(count(sql, "::int4"), is(10));
    }


    @Test
    @DisplayName("two accesses with short lists of two variables are merged while the lists give few disjuncts together")
    void fewDisjunctsOfTwoAccesses() throws Exception
    {
        String query = "SELECT ?s ?l WHERE { VALUES ?s { " + items(1, 5) + " } VALUES ?l { " + labels(5)
                + " } ?s ex:num ?n . ?s ex:label ?l }";

        assertThat(execute(query), containsInAnyOrder(rows(1, 5, ValuesMergeTest::label)));

        String sql = translate(query);

        assertThat(reads(sql, "item"), is(1));
        assertThat(lists(sql), is(0));
    }


    @Test
    @DisplayName("two accesses with lists of two variables stay apart when the lists would be multiplied")
    void multipliedDisjunctsOfTwoAccesses() throws Exception
    {
        String query = "SELECT ?s ?l WHERE { VALUES ?s { " + items(1, 20) + " } VALUES ?l { " + labels(20)
                + " } ?s ex:num ?n . ?s ex:label ?l }";

        assertThat(execute(query), containsInAnyOrder(rows(1, 20, ValuesMergeTest::label)));

        String sql = translate(query);

        // each access with the list of its own variable, an equality for each item
        assertThat(reads(sql, "item"), is(2));
        assertThat(lists(sql), is(0));
        assertThat(count(sql, "::int4"), is(20));
    }


    @Test
    @DisplayName("a referenced access is merged into the referencing one while their lists give few disjuncts together")
    void fewDisjunctsAlongForeignKey() throws Exception
    {
        String query = "SELECT ?r ?s WHERE { VALUES ?r { " + refs(1, 5) + " } VALUES ?s { " + items(1, 5)
                + " } ?r ex:ref ?s . ?s a ex:Item }";

        assertThat(execute(query), containsInAnyOrder(
                IntStream.rangeClosed(1, 5).mapToObj(i -> Arrays.asList(ref(i), item(i))).toArray()));

        String sql = translate(query);

        assertThat(reads(sql, "ref"), is(1));
        assertThat(reads(sql, "item"), is(0));
        assertThat(lists(sql), is(0));
    }


    @Test
    @DisplayName("a referenced access stays apart from the referencing one when their lists would be multiplied")
    void multipliedDisjunctsAlongForeignKey() throws Exception
    {
        String query = "SELECT ?r ?s WHERE { VALUES ?r { " + refs(1, 20) + " } VALUES ?s { " + items(1, 20)
                + " } ?r ex:ref ?s . ?s a ex:Item }";

        assertThat(execute(query), containsInAnyOrder(
                IntStream.rangeClosed(1, 20).mapToObj(i -> Arrays.asList(ref(i), item(i))).toArray()));

        String sql = translate(query);

        // each access with the list of its own variable, an equality for each reference and for each item
        assertThat(reads(sql, "ref"), is(1));
        assertThat(reads(sql, "item"), is(1));
        assertThat(lists(sql), is(0));
        assertThat(count(sql, "::int4"), is(40));
    }


    @Test
    @DisplayName("a single row is merged into an access of any number of disjuncts")
    void singleRow() throws Exception
    {
        // the distinct union of the accesses is one access with a disjunct for every label
        String union = IntStream.rangeClosed(1, 40).mapToObj(i -> "{ ?s ex:label 'l" + i + "' }")
                .collect(joining(" UNION "));
        String query = "SELECT ?s WHERE { VALUES ?s { item:7 } { SELECT DISTINCT ?s WHERE { " + union + " } } }";

        assertThat(execute(query), containsInAnyOrder(Arrays.<RdfTerm> asList(new Iri("http://example.org/item/7"))));

        String sql = translate(query);

        assertThat(sql, not(containsString("UNION")));
        assertThat(lists(sql), is(0));
    }
}
