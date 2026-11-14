package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
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
import cz.iocb.sparql.engine.mapping.TermMapping;
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
 * Filters requiring a term of a variable: {@code sameTerm} of the variable and a constant, and an equality of the
 * variable and a constant IRI, hold only when the variable is that term, so the table accesses binding the variable
 * take the term into their own conditions, as they take a constant of a pattern, and the filter is left out once the
 * solutions are restricted for good; a walk of a property path starts from the term, or from its other end when the
 * term is required of that one. A condition other terms satisfy as well (an equality with a literal, an inequality, a
 * disjunction) stays a filter, and so does every condition where the solutions are not restricted for good: above a
 * walk, which may be turned later, above a slice, and for a variable that may be unbound.
 *
 * The table {@code item} holds the items 1 to 6 with the values 10 to 60, the kinds {@code a} (odd items) and {@code b}
 * and the note {@code x} of the item 1; the table {@code link} the links 1-2, 2-3, 3-4 and 5-6 between the items; the
 * table {@code source} the link of the source 1 to the item 1, so that a link starts from a term of two classes; the
 * table {@code part} the parts 1 and 2 of the item 2 and the part 3 of the item 5, referencing the items by a foreign
 * key.
 */
public class FilterTermTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> PREFIX item: <http://example.org/item/> "
            + "PREFIX source: <http://example.org/source/> PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> ";

    /**
     * Alias a filter gives its input, which the generated SQL holds only when a filter is left.
     */
    private static final String filtered = "\"outer0\"";

    /**
     * Configuration mapping the tables.
     */
    private static SparqlDatabaseConfiguration config = null;


    /**
     * Creates the {@code filter_term_test} schema and maps the values, kinds and notes of the items by ex:value,
     * ex:kind and ex:note, their type ex:Item by ex:type, the links between the items and from the sources to the items
     * by ex:link, and the items of the parts by ex:of.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema filter_term_test");
            statement.execute("create table filter_term_test.item (id int primary key, value int not null, "
                    + "kind varchar not null, note varchar)");
            statement.execute("insert into filter_term_test.item values (1, 10, 'a', 'x'), (2, 20, 'b', null), "
                    + "(3, 30, 'a', null), (4, 40, 'b', null), (5, 50, 'a', null), (6, 60, 'b', null)");
            statement.execute("create table filter_term_test.link (parent int not null, child int not null)");
            statement.execute("insert into filter_term_test.link values (1, 2), (2, 3), (3, 4), (5, 6)");
            statement.execute("create table filter_term_test.source (id int primary key, target int not null)");
            statement.execute("insert into filter_term_test.source values (1, 1)");
            statement.execute("create table filter_term_test.part (id int primary key, "
                    + "whole int not null references filter_term_test.item (id))");
            statement.execute("insert into filter_term_test.part values (1, 2), (2, 2), (3, 5)");
        }

        DatabaseSchema schema = new DatabaseSchema(connectionPool);

        config = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);
        config.addPrefix("ex", "http://example.org/");
        config.addIriClass(new IntegerUserIriClass("item", INT4, "http://example.org/item/"));
        config.addIriClass(new IntegerUserIriClass("source", INT4, "http://example.org/source/"));
        config.addIriClass(new IntegerUserIriClass("part", INT4, "http://example.org/part/"));

        DatabaseTable item = new DatabaseTable("filter_term_test", "item");
        DatabaseTable link = new DatabaseTable("filter_term_test", "link");
        DatabaseTable source = new DatabaseTable("filter_term_test", "source");
        DatabaseTable part = new DatabaseTable("filter_term_test", "part");
        TermMapping subject = config.createIriMapping("item", "id");

        config.addQuadMapping(item, null, subject, config.createIriMapping("ex:value"),
                config.createLiteralMapping(xsdInt, "value"));
        config.addQuadMapping(item, null, subject, config.createIriMapping("ex:kind"),
                config.createLiteralMapping(xsdString, "kind"));
        config.addQuadMapping(item, null, subject, config.createIriMapping("ex:note"),
                config.createLiteralMapping(xsdString, "note"));
        config.addQuadMapping(link, null, config.createIriMapping("item", "parent"), config.createIriMapping("ex:link"),
                config.createIriMapping("item", "child"));
        config.addQuadMapping(source, null, config.createIriMapping("source", "id"), config.createIriMapping("ex:link"),
                config.createIriMapping("item", "target"));
        config.addQuadMapping(item, null, subject, config.createIriMapping("ex:type"),
                config.createIriMapping("ex:Item"));
        config.addQuadMapping(part, null, config.createIriMapping("part", "id"), config.createIriMapping("ex:of"),
                config.createIriMapping("item", "whole"));
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
     * Number of occurrences of the text in the SQL.
     *
     * @param sql the SQL
     * @param text the text
     * @return number of occurrences of the text in the SQL
     */
    private static int count(String sql, String text)
    {
        return sql.split(java.util.regex.Pattern.quote(text), -1).length - 1;
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
     * IRI of the item.
     *
     * @param id identifier of the item
     * @return IRI of the item
     */
    private static Iri item(int id)
    {
        return new Iri("http://example.org/item/" + id);
    }


    /**
     * The xsd:int literal of the value.
     *
     * @param value the value
     * @return the xsd:int literal of the value
     */
    private static TypedLiteral number(int value)
    {
        return new TypedLiteral(Integer.toString(value), xsdIntIri);
    }


    /**
     * The xsd:string literal of the value.
     *
     * @param value the value
     * @return the xsd:string literal of the value
     */
    private static TypedLiteral string(String value)
    {
        return new TypedLiteral(value, xsdStringIri);
    }


    @Test
    @DisplayName("an IRI a filter requires of a variable restricts the table access like a constant of the pattern")
    void requiredIri() throws Exception
    {
        String constant = translate("SELECT ?v WHERE { item:5 ex:value ?v }");

        assertThat(translate("SELECT ?v WHERE { ?s ex:value ?v FILTER(?s = item:5) }"), is(constant));
        assertThat(translate("SELECT ?v WHERE { ?s ex:value ?v FILTER(item:5 = ?s) }"), is(constant));
        assertThat(translate("SELECT ?v WHERE { ?s ex:value ?v FILTER(sameTerm(?s, item:5)) }"), is(constant));

        assertThat(execute("SELECT ?s ?v WHERE { ?s ex:value ?v FILTER(?s = item:5) }"),
                contains(row(item(5), number(50))));

        // an IRI of another class, or no IRI at all, is held by no row
        assertThat(execute("SELECT ?v WHERE { ?s ex:value ?v FILTER(?s = source:5) }"), is(empty()));
        assertThat(execute("SELECT ?v WHERE { ?s ex:value ?v FILTER(sameTerm(?s, 'x')) }"), is(empty()));
    }


    @Test
    @DisplayName("a literal sameTerm requires of a variable restricts the table access")
    void requiredLiteral() throws Exception
    {
        String constant = translate("SELECT ?s WHERE { ?s ex:value '50'^^xsd:int }");

        assertThat(translate("SELECT ?s WHERE { ?s ex:value ?v FILTER(sameTerm(?v, '50'^^xsd:int)) }"), is(constant));

        assertThat(execute("SELECT ?s WHERE { ?s ex:value ?v FILTER(sameTerm(?v, '50'^^xsd:int)) }"),
                contains(row(item(5))));
        assertThat(execute("SELECT ?s WHERE { ?s ex:kind ?k FILTER(sameTerm(?k, 'a')) }"),
                containsInAnyOrder(row(item(1)), row(item(3)), row(item(5))));

        // the same value is another term in another datatype
        assertThat(execute("SELECT ?s WHERE { ?s ex:value ?v FILTER(sameTerm(?v, 50)) }"), is(empty()));
    }


    @Test
    @DisplayName("a condition that other terms satisfy as well stays a filter")
    void otherConditions() throws Exception
    {
        String sql = translate("SELECT ?s WHERE { ?s ex:value ?v FILTER(?v = 50) }");

        assertThat(sql, containsString(filtered));
        assertThat(execute("SELECT ?s WHERE { ?s ex:value ?v FILTER(?v = 50) }"), contains(row(item(5))));

        assertThat(translate("SELECT ?v WHERE { ?s ex:value ?v FILTER(?s != item:5) }"), containsString(filtered));
        assertThat(translate("SELECT ?v WHERE { ?s ex:value ?v FILTER(!(?s = item:5)) }"), containsString(filtered));
        assertThat(translate("SELECT ?v WHERE { ?s ex:value ?v FILTER(?s = item:5 || ?s = item:6) }"),
                containsString(filtered));

        assertThat(execute("SELECT ?v WHERE { ?s ex:value ?v FILTER(?s = item:5 || ?s = item:6) }"),
                containsInAnyOrder(row(number(50)), row(number(60))));
    }


    @Test
    @DisplayName("the conjuncts of a filter count by themselves")
    void conjunction() throws Exception
    {
        String sql = translate("SELECT ?v WHERE { ?s ex:value ?v FILTER(?s = item:5 && ?v > 20) }");

        assertThat(sql, containsString("\"item\" WHERE ((\"id\" = '5'::int4))"));
        assertThat(count(sql, "\"id\" = '5'::int4"), is(1));
        assertThat(sql, containsString(filtered));

        assertThat(execute("SELECT ?v WHERE { ?s ex:value ?v FILTER(?s = item:5 && ?v > 20) }"),
                contains(row(number(50))));
        assertThat(execute("SELECT ?v WHERE { ?s ex:value ?v FILTER(?s = item:1 && ?v > 20) }"), is(empty()));
        assertThat(execute("SELECT ?v WHERE { ?s ex:value ?v FILTER(?s = item:5 && ?s = item:6) }"), is(empty()));
    }


    @Test
    @DisplayName("every table access of a join binding the variable takes the term")
    void join() throws Exception
    {
        String sql = translate("SELECT ?v ?t WHERE { ?s ex:value ?v . ?t ex:link ?s FILTER(?s = item:2) }");

        assertThat(sql, containsString("\"id\" = '2'::int4"));
        assertThat(sql, containsString("\"child\" = '2'::int4"));
        assertThat(sql, not(containsString(filtered)));

        assertThat(execute("SELECT ?v ?t WHERE { ?s ex:value ?v . ?t ex:link ?s FILTER(?s = item:2) }"),
                contains(row(number(20), item(1))));

        // the accesses of one table joined by its key are one access
        assertThat(translate("SELECT ?v ?k WHERE { ?s ex:value ?v . ?s ex:kind ?k FILTER(?s = item:2) }"),
                is(translate("SELECT ?v ?k WHERE { item:2 ex:value ?v . item:2 ex:kind ?k }")));

        // and so are the accesses joined along a foreign key, the referencing one holding the term as well
        String merged = translate("SELECT ?p WHERE { ?p ex:of ?s . ?s ex:type ex:Item FILTER(?s = item:2) }");

        assertThat(merged, containsString("\"whole\" = '2'::int4"));
        assertThat(merged, not(containsString("\"item\"")));

        assertThat(execute("SELECT ?p WHERE { ?p ex:of ?s . ?s ex:type ex:Item FILTER(?s = item:2) }"),
                containsInAnyOrder(row(new Iri("http://example.org/part/1")),
                        row(new Iri("http://example.org/part/2"))));
    }


    @Test
    @DisplayName("a pattern with a variable predicate keeps only the mappings of the required predicate")
    void variablePredicate() throws Exception
    {
        String sql = translate("SELECT ?s ?o WHERE { ?s ?p ?o FILTER(?p = ex:value) }");

        assertThat(sql, not(containsString("UNION")));
        assertThat(sql, not(containsString("\"kind\"")));
        assertThat(sql, not(containsString(filtered)));

        assertThat(execute("SELECT ?s ?o WHERE { ?s ?p ?o FILTER(?p = ex:value && ?s = item:5) }"),
                contains(row(item(5), number(50))));

        // a constant compared with a constant is decided when the query is translated
        assertThat(translate("SELECT ?s ?o WHERE { ?s ?p ?o FILTER(?p != ex:value) }"),
                not(containsString("\"value\"")));
        assertThat(execute("SELECT ?o WHERE { ?s ?p ?o FILTER(?p != ex:value && ?s = item:1) }"), containsInAnyOrder(
                row(string("a")), row(string("x")), row(item(2)), row(new Iri("http://example.org/Item"))));
    }


    @Test
    @DisplayName("the filter of an OPTIONAL restricts the table accesses of the optional pattern")
    void optionalFilter() throws Exception
    {
        String query = "SELECT ?s ?k WHERE { ?s ex:value ?v OPTIONAL { ?t ex:link ?s . ?t ex:kind ?k FILTER(?t = item:1) } }";

        assertThat(count(translate(query), "'1'::int4"), is(2));

        assertThat(execute(query), containsInAnyOrder(row(item(1), null), row(item(2), string("a")), row(item(3), null),
                row(item(4), null), row(item(5), null), row(item(6), null)));
    }


    @Test
    @DisplayName("a walk starts from the required term, or from its other end when the term is required of that one")
    void walk() throws Exception
    {
        String start = translate("SELECT ?y WHERE { ?x ex:link+ ?y FILTER(?x = item:1) }");

        assertThat(start, containsString("\"parent\" = '1'::int4"));
        assertThat(execute("SELECT ?y WHERE { ?x ex:link+ ?y FILTER(?x = item:1) }"),
                containsInAnyOrder(row(item(2)), row(item(3)), row(item(4))));

        String end = translate("SELECT ?x WHERE { ?x ex:link+ ?y FILTER(?y = item:4) }");

        assertThat(end, containsString("\"child\" = '4'::int4"));
        assertThat(execute("SELECT ?x WHERE { ?x ex:link+ ?y FILTER(?y = item:4) }"), containsInAnyOrder(row(item(1)),
                row(item(2)), row(item(3)), row(new Iri("http://example.org/source/1"))));

        // the filter stays above the walk, which keeps a restriction of one end only
        assertThat(execute("SELECT ?x ?y WHERE { ?x ex:link+ ?y FILTER(?x = item:1 && ?y = item:4) }"),
                contains(row(item(1), item(4))));
        assertThat(execute("SELECT ?x ?y WHERE { ?x ex:link+ ?y FILTER(?x = item:5 && ?y = item:4) }"), is(empty()));
        assertThat(execute("SELECT ?x ?y WHERE { ?x ex:link* ?y FILTER(?y = item:2) }"), containsInAnyOrder(
                row(item(1), item(2)), row(item(2), item(2)), row(new Iri("http://example.org/source/1"), item(2))));

        // a walk joined with a pattern binding its start
        assertThat(execute("SELECT ?v ?y WHERE { ?x ex:value ?v . ?x ex:link+ ?y FILTER(?x = item:3) }"),
                contains(row(number(30), item(4))));
    }


    @Test
    @DisplayName("a slice, an aggregate and a variable that may be unbound keep what the filter decides")
    void keptFilter() throws Exception
    {
        assertThat(execute("SELECT ?v WHERE { { SELECT ?s ?v WHERE { ?s ex:value ?v } ORDER BY ?s LIMIT 3 } "
                + "FILTER(?s = item:5) }"), is(empty()));
        assertThat(execute("SELECT ?v WHERE { { SELECT ?s ?v WHERE { ?s ex:value ?v } ORDER BY ?s LIMIT 3 } "
                + "FILTER(?s = item:2) }"), contains(row(number(20))));

        assertThat(execute("SELECT ?s WHERE { ?s ex:value ?v OPTIONAL { ?s ex:note ?n } FILTER(sameTerm(?n, 'x')) }"),
                contains(row(item(1))));

        assertThat(execute("SELECT ?c WHERE { { SELECT ?k (COUNT(*) AS ?c) WHERE { ?s ex:kind ?k } GROUP BY ?k } "
                + "FILTER(sameTerm(?k, 'a')) }"), contains(row(new TypedLiteral("3", xsdIntegerIri))));
        assertThat(execute(
                "SELECT ?c WHERE { { SELECT (COUNT(*) AS ?c) WHERE { ?s ex:kind ?k } } FILTER(sameTerm(?c, 6)) }"),
                contains(row(new TypedLiteral("6", xsdIntegerIri))));
    }


    @Test
    @DisplayName("a variable bound to a constant is compared when the query is translated")
    void constantBinding() throws Exception
    {
        assertThat(translate("SELECT ?v WHERE { VALUES ?s { item:5 } ?s ex:value ?v FILTER(?s = item:5) }"),
                not(containsString(filtered)));
        assertThat(translate("SELECT ?v WHERE { VALUES ?s { item:5 } ?s ex:value ?v FILTER(?s = item:6) }"),
                not(containsString("\"item\"")));

        assertThat(execute("SELECT ?v WHERE { VALUES ?s { item:5 } ?s ex:value ?v FILTER(?s = item:5) }"),
                contains(row(number(50))));
        assertThat(execute("SELECT ?v WHERE { VALUES ?s { item:5 } ?s ex:value ?v FILTER(?s = item:6) }"), is(empty()));
        assertThat(execute("SELECT ?v WHERE { VALUES ?s { item:5 } ?s ex:value ?v FILTER(?s != item:6) }"),
                contains(row(number(50))));
    }
}
