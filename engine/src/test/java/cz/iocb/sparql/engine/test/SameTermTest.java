package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdBooleanIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
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
 * The function sameTerm: true for the same terms, false for different ones, also for terms of classes that share no
 * term, and an error when an operand is unbound, whether the terms are held in one class, in a box or in several
 * classes. Operands that are compared in one class in every solution are compared by a plain condition on their
 * columns, which the database can use for an index or a join.
 *
 * The table {@code item} holds the items 1 to 6 with an optional reference to an item (5, 1, none, 5, 3, none), a term
 * in a box (the integer 1, the string abc, the item 5, the xsd:int 50, the xsd:int 5, the decimal 1.0) and an optional
 * xsd:int number (50, none, 5, 50, 1, none).
 */
public class SameTermTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> "
            + "PREFIX item: <http://example.org/item/> PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> ";

    /**
     * The value true.
     */
    private static final RdfTerm yes = new TypedLiteral("true", xsdBooleanIri);

    /**
     * The value false.
     */
    private static final RdfTerm no = new TypedLiteral("false", xsdBooleanIri);

    /**
     * Configuration mapping the table.
     */
    private static SparqlDatabaseConfiguration config = null;


    /**
     * Creates the {@code same_term_test} schema with the table {@code item} and maps the references of the items by
     * ex:ref, their boxed terms by ex:value and their numbers by ex:num.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema same_term_test");
            statement.execute("create table same_term_test.item (id int primary key, ref int, "
                    + "value sparql.rdfbox not null, num int)");
            statement.execute("""
                    insert into same_term_test.item values \
                    (1, 5, '"1"^^<http://www.w3.org/2001/XMLSchema#integer>', 50), (2, 1, '"abc"', null), \
                    (3, null, '<http://example.org/item/5>', 5), \
                    (4, 5, '"50"^^<http://www.w3.org/2001/XMLSchema#int>', 50), \
                    (5, 3, '"5"^^<http://www.w3.org/2001/XMLSchema#int>', 1), \
                    (6, null, '"1.0"^^<http://www.w3.org/2001/XMLSchema#decimal>', null)""");
        }

        DatabaseSchema schema = new DatabaseSchema(connectionPool);

        config = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);
        config.addPrefix("ex", "http://example.org/");
        config.addIriClass(new IntegerUserIriClass("item", INT4, "http://example.org/item/"));

        DatabaseTable table = new DatabaseTable("same_term_test", "item");
        ConstantIriMapping graph = config.createIriMapping("<http://example.org/graph>");

        config.addQuadMapping(table, graph, config.createIriMapping("item", "id"), config.createIriMapping("ex:ref"),
                config.createIriMapping("item", "ref"));
        config.addQuadMapping(table, graph, config.createIriMapping("item", "id"), config.createIriMapping("ex:value"),
                config.createTermMapping(box, "value"));
        config.addQuadMapping(table, graph, config.createIriMapping("item", "id"), config.createIriMapping("ex:num"),
                config.createLiteralMapping(xsdInt, "num"));
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
     * Row of an item with the value of sameTerm and the value of its negation for the item: both unbound for an error.
     *
     * @param item the item
     * @param same the value of sameTerm, or null for an error
     * @return row of the item, the value and its negation
     */
    private static List<RdfTerm> row(int item, Boolean same)
    {
        return row(item(item), same == null ? null : same ? yes : no, same == null ? null : same ? no : yes);
    }


    /**
     * Rows of the items 1 to 6 with the value of sameTerm of the operands and the value of its negation, ordered by the
     * items; the reference {@code ?x} and the number {@code ?n} of an item are unbound when the item has none.
     *
     * @param left the left operand
     * @param right the right operand
     * @return rows of the items, the values and their negations
     * @throws Exception if the query fails
     */
    private static List<List<RdfTerm>> values(String left, String right) throws Exception
    {
        return execute("SELECT ?s (sameTerm(%1$s, %2$s) AS ?r) (!sameTerm(%1$s, %2$s) AS ?d) ".formatted(left, right)
                + "WHERE { ?s ex:value ?v OPTIONAL { ?s ex:ref ?x } OPTIONAL { ?s ex:num ?n } } ORDER BY ?s");
    }


    @Test
    @DisplayName("terms of one class are the same term when their columns are equal")
    void oneClass() throws Exception
    {
        assertThat(values("?x", "item:5"),
                contains(row(1, true), row(2, false), row(3, null), row(4, true), row(5, false), row(6, null)));
        assertThat(values("item:5", "?x"),
                contains(row(1, true), row(2, false), row(3, null), row(4, true), row(5, false), row(6, null)));
        assertThat(values("?n", "'50'^^xsd:int"),
                contains(row(1, true), row(2, null), row(3, false), row(4, true), row(5, false), row(6, null)));
        assertThat(values("?x", "?s"),
                contains(row(1, false), row(2, false), row(3, null), row(4, false), row(5, false), row(6, null)));
    }


    @Test
    @DisplayName("terms of classes sharing no term are different, unless one of them is unbound")
    void differentClasses() throws Exception
    {
        // an xsd:int is no xsd:integer
        assertThat(values("?n", "50"),
                contains(row(1, false), row(2, null), row(3, false), row(4, false), row(5, false), row(6, null)));
        assertThat(values("?x", "?n"),
                contains(row(1, false), row(2, null), row(3, null), row(4, false), row(5, false), row(6, null)));
    }


    @Test
    @DisplayName("a term in a box is compared with a term of a class as the same term")
    void boxed() throws Exception
    {
        assertThat(values("?v", "?n"),
                contains(row(1, false), row(2, null), row(3, false), row(4, true), row(5, false), row(6, null)));
        assertThat(values("?v", "item:5"),
                contains(row(1, false), row(2, false), row(3, true), row(4, false), row(5, false), row(6, false)));
        assertThat(values("?v", "1"),
                contains(row(1, true), row(2, false), row(3, false), row(4, false), row(5, false), row(6, false)));
        assertThat(values("?v", "?x"),
                contains(row(1, false), row(2, false), row(3, null), row(4, false), row(5, false), row(6, null)));
    }


    @Test
    @DisplayName("an operand bound in several classes is the same term only in the class of the other operand")
    void severalClasses() throws Exception
    {
        String query = "SELECT ?id (sameTerm(?a, %1$s) AS ?r) (!sameTerm(?a, %1$s) AS ?d) WHERE { "
                + "VALUES (?id ?a) { (item:1 1) (item:2 'a') (item:3 item:5) (item:4 UNDEF) (item:5 1.0) } } ORDER BY ?id";

        assertThat(execute(query.formatted("1")),
                contains(row(1, true), row(2, false), row(3, false), row(4, null), row(5, false)));
        assertThat(execute(query.formatted("item:5")),
                contains(row(1, false), row(2, false), row(3, true), row(4, null), row(5, false)));
        assertThat(execute(query.formatted("?a")),
                contains(row(1, true), row(2, true), row(3, true), row(4, null), row(5, true)));
    }


    @Test
    @DisplayName("a filter comparing a variable of one class with a constant is a plain condition on its column")
    void filterByConstant() throws Exception
    {
        String same = "SELECT ?s WHERE { ?s ex:ref ?x FILTER(sameTerm(?x, item:5)) }";
        String different = "SELECT ?s WHERE { ?s ex:ref ?x FILTER(!sameTerm(?x, item:5)) }";

        assertThat(execute(same), containsInAnyOrder(row(item(1)), row(item(4))));
        assertThat(execute(different), containsInAnyOrder(row(item(2)), row(item(5))));

        assertThat(translate(same), not(containsString("COALESCE")));
        assertThat(translate(same), containsString("(\"ref\" = '5'::int4)"));

        // a term in a box as well
        String boxed = "SELECT ?s WHERE { ?s ex:value ?v FILTER(sameTerm(?v, item:5)) }";

        assertThat(execute(boxed), containsInAnyOrder(row(item(3))));
        assertThat(translate(boxed), not(containsString("COALESCE")));
    }


    @Test
    @DisplayName("a filter comparing two variables of one class is a plain condition joining their columns")
    void join() throws Exception
    {
        String query = "SELECT ?s ?t WHERE { ?s ex:ref ?x . ?t ex:ref ?y FILTER(sameTerm(?x, ?y)) }";

        assertThat(execute(query), containsInAnyOrder(row(item(1), item(1)), row(item(1), item(4)),
                row(item(4), item(1)), row(item(4), item(4)), row(item(2), item(2)), row(item(5), item(5))));
        assertThat(translate(query), not(containsString("COALESCE")));
    }
}
