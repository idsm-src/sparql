package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.genInt;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.rdfLangString;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdBoolean;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDecimal;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdFloat;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
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
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.LimitExceedException;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Request.PreparedQuery;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.translator.ServiceException;
import cz.iocb.sparql.engine.translator.TranslateVisitor;
import cz.iocb.sparql.testing.Database;



/**
 * A filter comparing a variable with a literal requires a term of the variable where only one term can be equal to the
 * literal: a string is equal to itself only, and a number or a boolean to the single literal of its value that a column
 * of canonical literals of one datatype holds. The table accesses binding the variable then take the term into their
 * own conditions, as they take a constant of a pattern, and the filter is left out. Where several terms may be equal
 * (literals in other lexical forms, floating-point numbers, terms in a box, a variable of two numeric datatypes at
 * once), the filter stays and decides.
 *
 * The table {@code thing} holds the things 1 to 6 with the counts 10 to 60, the totals of the same values, the ratios
 * 1.5 to 6.5, the names {@code a} (odd things) and {@code b} and the flags that are true for the things 1 to 3; the
 * table {@code stock} the counts 20 and 70 of the things 7 and 8 as integers; the table {@code written} the xsd:int
 * literals 20, 020, +20 and 30 of the things 1 to 4; the table {@code label} the labels {@code a} in English and in
 * German and {@code b} in English of the things 1 to 3; the table {@code mark} the terms 20 as an xsd:int, as an
 * xsd:integer and as a string, and the string {@code a}, of the things 1 to 4.
 */
public class FilterLiteralTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> "
            + "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> ";

    /**
     * Alias a filter gives its input, which the generated SQL holds only when a filter is left.
     */
    private static final String filtered = "\"outer0\"";

    /**
     * Configuration mapping the tables.
     */
    private static SparqlDatabaseConfiguration config = null;


    /**
     * Creates the {@code filter_literal_test} schema and maps the columns of the things by ex:count, ex:total,
     * ex:ratio, ex:name and ex:flag, the counts of the things and of the stock together by ex:amount, so that its
     * objects are of two datatypes, and the other tables by ex:written, ex:label and ex:mark.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema filter_literal_test");
            statement.execute("""
                    create table filter_literal_test.thing (id int primary key, count int not null, \
                    total numeric not null, ratio float4 not null, name varchar not null, flag bool not null)""");
            statement.execute("""
                    insert into filter_literal_test.thing values (1, 10, 10, 1.5, 'a', true), \
                    (2, 20, 20, 2.5, 'b', true), (3, 30, 30, 3.5, 'a', true), (4, 40, 40, 4.5, 'b', false), \
                    (5, 50, 50, 5.5, 'a', false), (6, 60, 60, 6.5, 'b', false)""");
            statement.execute("create table filter_literal_test.stock (id int primary key, count numeric not null)");
            statement.execute("insert into filter_literal_test.stock values (7, 20), (8, 70)");
            statement.execute("create table filter_literal_test.written (id int primary key, count int not null, "
                    + "form varchar not null)");
            statement.execute("insert into filter_literal_test.written values (1, 20, ''), (2, 20, '020'), "
                    + "(3, 20, '+20'), (4, 30, '')");
            statement.execute("create table filter_literal_test.label (id int primary key, label varchar not null, "
                    + "lang varchar not null)");
            statement.execute(
                    "insert into filter_literal_test.label values (1, 'a', 'en'), (2, 'a', 'de'), (3, 'b', 'en')");
            statement.execute("create table filter_literal_test.mark (id int not null, mark sparql.rdfbox not null)");
            statement.execute("""
                    insert into filter_literal_test.mark values \
                    (1, '"20"^^<http://www.w3.org/2001/XMLSchema#int>'), \
                    (2, '"20"^^<http://www.w3.org/2001/XMLSchema#integer>'), (3, '"20"'), (4, '"a"')""");
        }

        DatabaseSchema schema = new DatabaseSchema(connectionPool);

        config = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);
        config.addPrefix("ex", "http://example.org/");
        config.addIriClass(new IntegerUserIriClass("thing", INT4, "http://example.org/thing/"));

        DatabaseTable thing = new DatabaseTable("filter_literal_test", "thing");
        DatabaseTable stock = new DatabaseTable("filter_literal_test", "stock");
        DatabaseTable written = new DatabaseTable("filter_literal_test", "written");
        DatabaseTable label = new DatabaseTable("filter_literal_test", "label");
        DatabaseTable mark = new DatabaseTable("filter_literal_test", "mark");
        TermMapping subject = config.createIriMapping("thing", "id");

        config.addQuadMapping(thing, null, subject, config.createIriMapping("ex:count"),
                config.createLiteralMapping(xsdInt, "count"));
        config.addQuadMapping(thing, null, subject, config.createIriMapping("ex:total"),
                config.createLiteralMapping(xsdDecimal, "total"));
        config.addQuadMapping(thing, null, subject, config.createIriMapping("ex:ratio"),
                config.createLiteralMapping(xsdFloat, "ratio"));
        config.addQuadMapping(thing, null, subject, config.createIriMapping("ex:name"),
                config.createLiteralMapping(xsdString, "name"));
        config.addQuadMapping(thing, null, subject, config.createIriMapping("ex:flag"),
                config.createLiteralMapping(xsdBoolean, "flag"));
        config.addQuadMapping(thing, null, subject, config.createIriMapping("ex:amount"),
                config.createLiteralMapping(xsdInt, "count"));
        config.addQuadMapping(stock, null, subject, config.createIriMapping("ex:amount"),
                config.createLiteralMapping(xsdInteger, "count"));
        config.addQuadMapping(written, null, subject, config.createIriMapping("ex:written"),
                config.createLiteralMapping(genInt, "count", "form"));
        config.addQuadMapping(label, null, subject, config.createIriMapping("ex:label"),
                config.createLiteralMapping(rdfLangString, "label", "lang"));
        config.addQuadMapping(mark, null, subject, config.createIriMapping("ex:mark"),
                config.createTermMapping(box, "mark"));
    }


    /**
     * Translates the query selecting the subjects of the pattern into SQL.
     *
     * @param pattern the group graph pattern, without the braces
     * @return the generated SQL
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static String translate(String pattern) throws SQLException, TranslateExceptions, ServiceException
    {
        Engine engine = new Engine(config);

        try(Request request = engine.getRequest())
        {
            PreparedQuery prepared = request.prepareQuery(prefix + "SELECT ?s WHERE { " + pattern + " }", null);

            SqlSelect imcode = new TranslateVisitor(request).translate(prepared.getSyntaxTree(), null, null, List.of());

            return imcode.optimize(request, false).optimize(request, true).translate(request);
        }
    }


    /**
     * Identifiers of the things that are the subjects of the solutions of the pattern, in ascending order.
     *
     * @param pattern the group graph pattern, without the braces
     * @return identifiers of the things that are the subjects of the solutions of the pattern
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws LimitExceedException if the generated SQL is too long
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static List<Integer> subjects(String pattern)
            throws SQLException, TranslateExceptions, LimitExceedException, ServiceException
    {
        Engine engine = new Engine(config);
        List<Integer> subjects = new java.util.ArrayList<>();

        try(Request request = engine.getRequest();
                Result result = request.execute(prefix + "SELECT ?s WHERE { " + pattern + " }"))
        {
            while(result.next())
                subjects.add(Integer
                        .valueOf(((Iri) result.get(0)).getValue().substring("http://example.org/thing/".length())));
        }

        return subjects.stream().sorted().toList();
    }


    @Test
    @DisplayName("an equality with a number requires the literal of its value in the datatype of the column")
    void number() throws Exception
    {
        String constant = translate("?s ex:count '20'^^xsd:int");

        assertThat(translate("?s ex:count ?c FILTER(?c = 20)"), is(constant));
        assertThat(translate("?s ex:count ?c FILTER(20 = ?c)"), is(constant));
        assertThat(translate("?s ex:count ?c FILTER(?c = 20.0)"), is(constant));
        assertThat(translate("?s ex:count ?c FILTER(?c = +020)"), is(constant));
        assertThat(translate("?s ex:count ?c FILTER(?c = '20'^^xsd:long)"), is(constant));
        assertThat(translate("?s ex:count ?c FILTER(?c = '20'^^xsd:int)"), is(constant));

        assertThat(translate("?s ex:total ?t FILTER(?t = 20)"), is(translate("?s ex:total 20.0")));

        assertThat(subjects("?s ex:count ?c FILTER(?c = 20)"), contains(2));
        assertThat(subjects("?s ex:count ?c FILTER(?c = 20.0)"), contains(2));
        assertThat(subjects("?s ex:count ?c FILTER(?c = '20'^^xsd:long)"), contains(2));
        assertThat(subjects("?s ex:total ?t FILTER(?t = 20)"), contains(2));

        // the term is required of every table access binding the variable
        String joined = translate("?s ex:count ?c . ?s ex:name ?n . ?t ex:count ?c FILTER(?c = 20)");

        assertThat(joined, not(containsString(filtered)));
        assertThat(joined.split("\"count\" = '20'::int4", -1).length - 1, is(2));
        assertThat(subjects("?s ex:count ?c . ?s ex:name ?n . ?t ex:count ?c FILTER(?c = 20)"), contains(2));

        // the datatype of the column holds no literal of the value
        assertThat(subjects("?s ex:count ?c FILTER(?c = 20.5)"), is(empty()));
        assertThat(subjects("?s ex:count ?c FILTER(?c = 2147483648)"), is(empty()));
        assertThat(subjects("?s ex:count ?c FILTER(?c = '20')"), is(empty()));
    }


    @Test
    @DisplayName("an equality with a string or a boolean requires the term")
    void stringAndBoolean() throws Exception
    {
        assertThat(translate("?s ex:name ?n FILTER(?n = 'a')"), is(translate("?s ex:name 'a'")));
        assertThat(translate("?s ex:name ?n FILTER('a' = ?n)"), is(translate("?s ex:name 'a'")));
        assertThat(translate("?s ex:label ?l FILTER(?l = 'a'@en)"), is(translate("?s ex:label 'a'@en")));
        assertThat(translate("?s ex:flag ?f FILTER(?f = true)"), is(translate("?s ex:flag true")));
        assertThat(translate("?s ex:flag ?f FILTER(?f = '0'^^xsd:boolean)"), is(translate("?s ex:flag false")));

        assertThat(subjects("?s ex:name ?n FILTER(?n = 'a')"), contains(1, 3, 5));
        assertThat(subjects("?s ex:label ?l FILTER(?l = 'a'@en)"), contains(1));
        assertThat(subjects("?s ex:label ?l FILTER(?l = 'a'@EN)"), contains(1));
        assertThat(subjects("?s ex:flag ?f FILTER(?f = true)"), contains(1, 2, 3));
        assertThat(subjects("?s ex:flag ?f FILTER(?f = '0'^^xsd:boolean)"), contains(4, 5, 6));

        // a string is equal to no term of another kind
        assertThat(subjects("?s ex:name ?n FILTER(?n = 'a'@en)"), is(empty()));
        assertThat(subjects("?s ex:label ?l FILTER(?l = 'a')"), is(empty()));
        assertThat(subjects("?s ex:flag ?f FILTER(?f = 'true')"), is(empty()));

        // a string in a box is required as a term too
        assertThat(translate("?s ex:mark ?m FILTER(?m = 'a')"), not(containsString(filtered)));
        assertThat(subjects("?s ex:mark ?m FILTER(?m = 'a')"), contains(4));
        assertThat(subjects("?s ex:mark ?m FILTER(?m = '20')"), contains(3));
    }


    @Test
    @DisplayName("a value held in several terms keeps the filter")
    void severalTerms() throws Exception
    {
        // literals in every lexical form
        assertThat(translate("?s ex:written ?c FILTER(?c = 20)"), containsString(filtered));
        assertThat(subjects("?s ex:written ?c FILTER(?c = 20)"), contains(1, 2, 3));

        // a floating-point number on either side
        assertThat(translate("?s ex:ratio ?r FILTER(?r = 2.5)"), containsString(filtered));
        assertThat(translate("?s ex:count ?c FILTER(?c = 2.0e1)"), containsString(filtered));
        assertThat(subjects("?s ex:ratio ?r FILTER(?r = 2.5)"), contains(2));
        assertThat(subjects("?s ex:count ?c FILTER(?c = 2.0e1)"), contains(2));

        // terms in a box
        assertThat(translate("?s ex:mark ?m FILTER(?m = 20)"), containsString(filtered));
        assertThat(subjects("?s ex:mark ?m FILTER(?m = 20)"), contains(1, 2));
    }


    @Test
    @DisplayName("a variable of two numeric datatypes is required to be a term where it is of one of them")
    void twoDatatypes() throws Exception
    {
        // a filter is distributed over the mappings of the predicate, each of which holds one datatype
        String sql = translate("?s ex:amount ?a FILTER(?a = 20)");

        assertThat(sql, containsString("\"count\" = '20'::int4"));
        assertThat(sql, containsString("\"count\" = '20'::numeric"));
        assertThat(sql, not(containsString(filtered)));
        assertThat(subjects("?s ex:amount ?a FILTER(?a = 20)"), contains(2, 7));

        // the two literals of the value are two terms where the variable is of both datatypes
        assertThat(translate("?s ex:name ?n OPTIONAL { ?s ex:amount ?a } FILTER(?a = 20)"), containsString(filtered));
        assertThat(subjects("?s ex:name ?n OPTIONAL { ?s ex:amount ?a } FILTER(?a = 20)"), contains(2));
        assertThat(subjects("{ SELECT ?s ?a WHERE { ?s ex:amount ?a } ORDER BY ?s LIMIT 7 } FILTER(?a = 20)"),
                contains(2, 7));
    }


    @Test
    @DisplayName("the term is required wherever a filter requires one, and other conditions stay filters")
    void conditions() throws Exception
    {
        // the filter of an OPTIONAL
        String optional = translate("?s ex:name ?n OPTIONAL { ?s ex:count ?c FILTER(?c = 20) }");

        assertThat(optional, containsString("\"count\" = '20'::int4"));
        assertThat(subjects("?s ex:name ?n OPTIONAL { ?s ex:count ?c FILTER(?c = 20) } FILTER(BOUND(?c))"),
                contains(2));

        // the conjuncts of a filter
        String conjunction = translate("?s ex:count ?c . ?s ex:name ?n FILTER(?c = 20 && ?n = 'b')");

        assertThat(conjunction, containsString("\"count\" = '20'::int4"));
        assertThat(conjunction, containsString("\"name\" = 'b'::varchar"));
        assertThat(conjunction, not(containsString(filtered)));
        assertThat(subjects("?s ex:count ?c . ?s ex:name ?n FILTER(?c = 20 && ?n = 'b')"), contains(2));
        assertThat(subjects("?s ex:count ?c . ?s ex:name ?n FILTER(?c = 20 && ?n = 'a')"), is(empty()));
        assertThat(subjects("?s ex:count ?c FILTER(?c = 20 && ?c = 30)"), is(empty()));

        // a variable copied by a BIND and a grouping variable
        assertThat(translate("?s ex:count ?c BIND(?c AS ?d) FILTER(?d = 20)"), not(containsString(filtered)));
        assertThat(subjects("?s ex:count ?c BIND(?c AS ?d) FILTER(?d = 20)"), contains(2));

        // conditions that more values satisfy
        assertThat(translate("?s ex:count ?c FILTER(?c != 20)"), containsString(filtered));
        assertThat(translate("?s ex:count ?c FILTER(!(?c = 20))"), containsString(filtered));
        assertThat(translate("?s ex:count ?c FILTER(?c = 20 || ?c = 30)"), containsString(filtered));
        assertThat(translate("?s ex:count ?c FILTER(?c >= 20)"), containsString(filtered));
        assertThat(subjects("?s ex:count ?c FILTER(?c != 20)"), contains(1, 3, 4, 5, 6));
        assertThat(subjects("?s ex:count ?c FILTER(?c = 20 || ?c = 30)"), contains(2, 3));

        // a variable that may be unbound
        assertThat(subjects("?s ex:name ?n OPTIONAL { ?s ex:written ?c } FILTER(?n = 'b')"), contains(2, 4, 6));
        assertThat(subjects("?s ex:count ?c OPTIONAL { ?s ex:label ?l } FILTER(?l = 'a'@en)"), contains(1));
    }
}
