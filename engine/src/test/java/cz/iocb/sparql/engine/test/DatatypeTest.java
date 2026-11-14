package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.box;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
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
 * The function DATATYPE: the datatype IRI of a literal, whether the literal is held in a class of its datatype, in a
 * box or written in the query, and an error for a term that is no literal and for an unbound value. The datatype of a
 * value that is always a literal of one datatype known when the query is translated is a constant IRI, so a comparison
 * with it is decided at once and a pattern joined through it keeps only what can hold that IRI.
 *
 * The table {@code item} holds the items 1 to 6 with an xsd:int number (50, 10, 5, 50, 1, 2), an optional xsd:int
 * number (5, none, 7, none, 3, none), a term in a box (the integer 1, the string abc, the item 5, the xsd:int 50, the
 * language-tagged string a@en, a literal of an unknown datatype) and a string label.
 */
public class DatatypeTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> "
            + "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> ";

    /**
     * The datatype xsd:int.
     */
    private static final RdfTerm xsdInt32 = xsd("int");

    /**
     * The datatype xsd:integer.
     */
    private static final RdfTerm xsdInteger = xsd("integer");

    /**
     * The datatype xsd:string.
     */
    private static final RdfTerm xsdStr = xsd("string");

    /**
     * The datatype rdf:langString.
     */
    private static final RdfTerm langString = new Iri("http://www.w3.org/1999/02/22-rdf-syntax-ns#langString");

    /**
     * The datatype rdf:dirLangString.
     */
    private static final RdfTerm dirLangString = new Iri("http://www.w3.org/1999/02/22-rdf-syntax-ns#dirLangString");

    /**
     * The datatype of the literal of an unknown datatype.
     */
    private static final RdfTerm unknown = new Iri("http://example.org/unknown");

    /**
     * Configuration mapping the table.
     */
    private static SparqlDatabaseConfiguration config = null;


    /**
     * Creates the {@code datatype_test} schema with the table {@code item} and maps the numbers of the items by ex:num,
     * their optional numbers by ex:opt, their boxed terms by ex:value and their labels by ex:label.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema datatype_test");
            statement.execute("create table datatype_test.item (id int primary key, num int not null, opt int, "
                    + "value sparql.rdfbox not null, label varchar not null)");
            statement.execute("""
                    insert into datatype_test.item values \
                    (1, 50, 5, '"1"^^<http://www.w3.org/2001/XMLSchema#integer>', 'abc'), \
                    (2, 10, null, '"abc"', '5'), (3, 5, 7, '<http://example.org/item/5>', 'x'), \
                    (4, 50, null, '"50"^^<http://www.w3.org/2001/XMLSchema#int>', 'abc'), \
                    (5, 1, 3, '"a"@en', 'x'), (6, 2, null, '"x"^^<http://example.org/unknown>', 'y')""");
        }

        DatabaseSchema schema = new DatabaseSchema(connectionPool);

        config = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);
        config.addPrefix("ex", "http://example.org/");
        config.addIriClass(new IntegerUserIriClass("item", INT4, "http://example.org/item/"));

        DatabaseTable table = new DatabaseTable("datatype_test", "item");
        ConstantIriMapping graph = config.createIriMapping("<http://example.org/graph>");

        config.addQuadMapping(table, graph, config.createIriMapping("item", "id"), config.createIriMapping("ex:num"),
                config.createLiteralMapping(xsdInt, "num"));
        config.addQuadMapping(table, graph, config.createIriMapping("item", "id"), config.createIriMapping("ex:opt"),
                config.createLiteralMapping(xsdInt, "opt"));
        config.addQuadMapping(table, graph, config.createIriMapping("item", "id"), config.createIriMapping("ex:value"),
                config.createTermMapping(box, "value"));
        config.addQuadMapping(table, graph, config.createIriMapping("item", "id"), config.createIriMapping("ex:label"),
                config.createLiteralMapping(xsdString, "label"));
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
     * A datatype of XML Schema.
     *
     * @param name the local name of the datatype
     * @return the IRI of the datatype
     */
    private static RdfTerm xsd(String name)
    {
        return new Iri("http://www.w3.org/2001/XMLSchema#" + name);
    }


    /**
     * An IRI of the example namespace.
     *
     * @param name the local name
     * @return the IRI
     */
    private static RdfTerm iri(String name)
    {
        return new Iri("http://example.org/" + name);
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


    @Test
    @DisplayName("the datatype of a literal of a class, of a boxed term and of a term that is no literal")
    void columns() throws Exception
    {
        String query = """
                SELECT (DATATYPE(?n) AS ?a) (DATATYPE(?m) AS ?b) (DATATYPE(?v) AS ?c) (DATATYPE(?l) AS ?d) \
                (DATATYPE(?s) AS ?e) WHERE { ?s ex:num ?n ; ex:value ?v ; ex:label ?l OPTIONAL { ?s ex:opt ?m } } \
                ORDER BY ?s""";

        assertThat(execute(query),
                contains(row(xsdInt32, xsdInt32, xsdInteger, xsdStr, null), row(xsdInt32, null, xsdStr, xsdStr, null),
                        row(xsdInt32, xsdInt32, null, xsdStr, null), row(xsdInt32, null, xsdInt32, xsdStr, null),
                        row(xsdInt32, xsdInt32, langString, xsdStr, null), row(xsdInt32, null, unknown, xsdStr, null)));
    }


    @Test
    @DisplayName("the datatype of a literal written in the query")
    void constants() throws Exception
    {
        String query = """
                SELECT (DATATYPE(1) AS ?a) (DATATYPE('a') AS ?b) (DATATYPE('a'@en) AS ?c) \
                (DATATYPE('a'@en--ltr) AS ?d) (DATATYPE('x'^^ex:unknown) AS ?e) (DATATYPE('abc'^^xsd:integer) AS ?f) \
                (DATATYPE(ex:x) AS ?g) (DATATYPE(?u) AS ?h) WHERE {}""";

        assertThat(execute(query),
                contains(row(xsdInteger, xsdStr, langString, dirLangString, unknown, xsdInteger, null, null)));
    }


    @Test
    @DisplayName("the datatype of a computed value")
    void expressions() throws Exception
    {
        String query = """
                SELECT (DATATYPE(?n + 1) AS ?a) (DATATYPE(STR(?n)) AS ?b) (DATATYPE(IF(?n > 5, ?n, ?l)) AS ?c) \
                (DATATYPE(COALESCE(?m, ?l)) AS ?d) WHERE { ?s ex:num ?n ; ex:label ?l OPTIONAL { ?s ex:opt ?m } } \
                ORDER BY ?s""";

        assertThat(execute(query),
                contains(row(xsdInteger, xsdStr, xsdInt32, xsdInt32), row(xsdInteger, xsdStr, xsdInt32, xsdStr),
                        row(xsdInteger, xsdStr, xsdStr, xsdInt32), row(xsdInteger, xsdStr, xsdInt32, xsdStr),
                        row(xsdInteger, xsdStr, xsdStr, xsdInt32), row(xsdInteger, xsdStr, xsdStr, xsdStr)));
    }


    @Test
    @DisplayName("a comparison of the datatype of a value of one datatype is decided when the query is translated")
    void comparison() throws Exception
    {
        String other = "SELECT ?s WHERE { ?s ex:num ?n FILTER(DATATYPE(?n) = xsd:integer) }";

        assertThat(execute(other), empty());
        assertThat(translate(other), not(containsString("datatype_test")));

        String same = "SELECT ?s WHERE { ?s ex:num ?n FILTER(DATATYPE(?n) = xsd:int) }";

        assertThat(execute(same), containsInAnyOrder(row(iri("item/1")), row(iri("item/2")), row(iri("item/3")),
                row(iri("item/4")), row(iri("item/5")), row(iri("item/6"))));
        assertThat(translate(same), not(containsString("XMLSchema")));
    }


    @Test
    @DisplayName("a pattern joined through the datatype of a value of one datatype keeps only what can hold the IRI")
    void join() throws Exception
    {
        // no subject of the mappings is the IRI of a datatype
        String query = "SELECT ?s ?p WHERE { ?s ex:num ?n BIND(DATATYPE(?n) AS ?d) ?d ?p ?o }";

        assertThat(execute(query), empty());
        assertThat(translate(query), not(containsString("datatype_test")));
    }


    @Test
    @DisplayName("a filter on the datatype of the objects keeps only the mappings whose objects can have the datatype")
    void objects() throws Exception
    {
        String query = "SELECT ?s ?p WHERE { ?s ?p ?o FILTER(DATATYPE(?o) = xsd:int) }";

        assertThat(execute(query),
                containsInAnyOrder(row(iri("item/1"), iri("num")), row(iri("item/2"), iri("num")),
                        row(iri("item/3"), iri("num")), row(iri("item/4"), iri("num")), row(iri("item/5"), iri("num")),
                        row(iri("item/6"), iri("num")), row(iri("item/1"), iri("opt")), row(iri("item/3"), iri("opt")),
                        row(iri("item/5"), iri("opt")), row(iri("item/4"), iri("value"))));

        // the labels are strings, and the numbers need no test
        assertThat(translate(query), not(containsString("\"label\"")));
        assertThat(translate(query), not(containsString("XMLSchema#string")));
    }
}
