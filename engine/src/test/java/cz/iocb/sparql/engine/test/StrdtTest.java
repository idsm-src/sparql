package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.database.SqlType.INT4;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.iri;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdBooleanIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
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
import cz.iocb.sparql.engine.mapping.classes.IntegerUserIriClass;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.TypedLiteral;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.LimitExceedException;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.translator.ServiceException;
import cz.iocb.sparql.testing.Database;



/**
 * STRDT makes the literal of the given lexical form and datatype, whatever the form is, and the literal is the same
 * term as that literal written in the query or stored in a typed column: it compares, joins and counts as it does. The
 * lexical form of a handled datatype is parsed for that. A form that is valid but not canonical is kept and has the
 * value it denotes; a form that is not valid is kept as well, and the literal then has no value, as a literal of a
 * datatype that is not handled has none. The lexical form and the datatype may be constants or computed values.
 */
public class StrdtTest
{
    private static final String prefix = "PREFIX ex: <http://example.org/> "
            + "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> ";

    private static final Iri unhandled = new Iri("http://example.org/datatype");

    private static Engine engine = null;


    /**
     * Creates the {@code strdt_test} schema with the items 1 to 4: an item has the xsd:int value {@code ex:n} equal to
     * its number, the lexical form {@code ex:form} ({@code 1}, {@code 02}, {@code x} and {@code 4}) and the datatype
     * {@code ex:type} (xsd:int, xsd:integer, xsd:int and a datatype that is not handled).
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema strdt_test");
            statement.execute("create table strdt_test.item (id int primary key, n int not null, "
                    + "form varchar not null, type varchar not null)");
            statement.execute("""
                    insert into strdt_test.item values (1, 1, '1', 'http://www.w3.org/2001/XMLSchema#int'), \
                    (2, 2, '02', 'http://www.w3.org/2001/XMLSchema#integer'), \
                    (3, 3, 'x', 'http://www.w3.org/2001/XMLSchema#int'), \
                    (4, 4, '4', 'http://example.org/datatype')""");
        }

        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, connectionPool,
                new DatabaseSchema(connectionPool), false);
        config.addPrefix("ex", "http://example.org/");

        IntegerUserIriClass item = new IntegerUserIriClass("item", INT4, "http://example.org/item/");
        config.addIriClass(item);

        DatabaseTable table = new DatabaseTable("strdt_test", "item");

        config.addQuadMapping(table, null, config.createIriMapping(item, "id"), config.createIriMapping("ex:n"),
                config.createLiteralMapping(xsdInt, "n"));
        config.addQuadMapping(table, null, config.createIriMapping(item, "id"), config.createIriMapping("ex:form"),
                config.createLiteralMapping(xsdString, "form"));
        config.addQuadMapping(table, null, config.createIriMapping(item, "id"), config.createIriMapping("ex:type"),
                config.createIriMapping(iri, "type"));

        engine = new Engine(config);
    }


    /**
     * Rows of the query.
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
        try(Request request = engine.getRequest(); Result result = request.execute(prefix + query))
        {
            List<List<RdfTerm>> rows = new ArrayList<>();

            while(result.next())
                rows.add(Arrays.asList(result.getRow()));

            return rows;
        }
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
     * IRI of an item.
     *
     * @param id the number of the item
     * @return IRI of an item
     */
    private static Iri item(int id)
    {
        return new Iri("http://example.org/item/" + id);
    }


    /**
     * The xsd:int literal of the lexical form.
     *
     * @param form the lexical form
     * @return the xsd:int literal of the lexical form
     */
    private static TypedLiteral intLiteral(String form)
    {
        return new TypedLiteral(form, xsdIntIri);
    }


    /**
     * The xsd:integer literal.
     *
     * @param value the value
     * @return the xsd:integer literal
     */
    private static TypedLiteral integer(int value)
    {
        return new TypedLiteral(Integer.toString(value), xsdIntegerIri);
    }


    /**
     * The xsd:boolean literal.
     *
     * @param value the value
     * @return the xsd:boolean literal
     */
    private static TypedLiteral bool(boolean value)
    {
        return new TypedLiteral(Boolean.toString(value), xsdBooleanIri);
    }


    @Test
    @DisplayName("the literal of constants is a value of its datatype")
    void constants() throws Exception
    {
        assertThat(execute("SELECT ?n WHERE { VALUES ?n { -1 1 3 } FILTER(?n > STRDT(\"0\", xsd:integer)) }"),
                containsInAnyOrder(row(integer(1)), row(integer(3))));
        assertThat(execute("""
                SELECT (STRDT("1", xsd:int) AS ?literal) (isNumeric(STRDT("1", xsd:int)) AS ?numeric) \
                (STRDT("1", xsd:int) + 1 AS ?sum) (sameTerm(STRDT("1", xsd:int), "1"^^xsd:int) AS ?same) \
                WHERE { }"""), contains(row(intLiteral("1"), bool(true), integer(2), bool(true))));
        assertThat(execute("ASK { FILTER(STRDT(\"true\", xsd:boolean)) }"), contains(row(bool(true))));
        assertThat(execute("ASK { FILTER(STRDT(\"2020-01-01\", xsd:date) < \"2020-12-31\"^^xsd:date) }"),
                contains(row(bool(true))));
        assertThat(execute("ASK { FILTER(STRDT(\"+INF\", xsd:double) = STRDT(\"INF\", xsd:double)) }"),
                contains(row(bool(true))));
    }


    @Test
    @DisplayName("the literal compares, joins and counts as a value of a typed column does")
    void typedColumn() throws Exception
    {
        assertThat(execute("SELECT ?x WHERE { ?x ex:n ?n FILTER(STRDT(STR(?n), xsd:int) = ?n) }"),
                containsInAnyOrder(row(item(1)), row(item(2)), row(item(3)), row(item(4))));
        assertThat(execute("SELECT ?x WHERE { ?x ex:n ?n FILTER(sameTerm(STRDT(\"2\", xsd:int), ?n)) }"),
                contains(row(item(2))));

        assertThat(execute("SELECT ?x WHERE { BIND(STRDT(\"3\", xsd:int) AS ?n) ?x ex:n ?n }"), contains(row(item(3))));
        assertThat(execute("SELECT ?x WHERE { { ?x ex:n ?n } { BIND(STRDT(\"3\", xsd:int) AS ?n) } }"),
                contains(row(item(3))));

        String union = "{ { ?x ex:n ?n } UNION { BIND(STRDT(\"1\", xsd:int) AS ?n) } }";

        assertThat(execute("SELECT (COUNT(DISTINCT ?n) AS ?count) WHERE " + union), contains(row(integer(4))));
        assertThat(execute("SELECT DISTINCT ?n WHERE " + union), containsInAnyOrder(row(intLiteral("1")),
                row(intLiteral("2")), row(intLiteral("3")), row(intLiteral("4"))));
    }


    @Test
    @DisplayName("a computed lexical form is kept: one that is not canonical has its value, an invalid one has none")
    void computedForm() throws Exception
    {
        assertThat(
                execute("SELECT ?x (STRDT(?form, xsd:int) AS ?literal) (isNumeric(STRDT(?form, xsd:int)) AS ?numeric) "
                        + "(STRDT(?form, xsd:int) + 1 AS ?sum) WHERE { ?x ex:form ?form }"),
                containsInAnyOrder(row(item(1), intLiteral("1"), bool(true), integer(2)),
                        row(item(2), intLiteral("02"), bool(true), integer(3)),
                        row(item(3), intLiteral("x"), bool(false), null),
                        row(item(4), intLiteral("4"), bool(true), integer(5))));

        // the value of the form 02 is the value 2, but the literal is another term
        assertThat(execute("SELECT ?x WHERE { ?x ex:form ?form ; ex:n ?n FILTER(STRDT(?form, xsd:int) = ?n) }"),
                containsInAnyOrder(row(item(1)), row(item(2)), row(item(4))));
        assertThat(
                execute("SELECT ?x WHERE { ?x ex:form ?form ; ex:n ?n FILTER(sameTerm(STRDT(?form, xsd:int), ?n)) }"),
                containsInAnyOrder(row(item(1)), row(item(4))));
    }


    @Test
    @DisplayName("a computed datatype is used as a constant one is")
    void computedDatatype() throws Exception
    {
        assertThat(execute("SELECT ?x (STRDT(?form, ?type) AS ?literal) WHERE { ?x ex:form ?form ; ex:type ?type }"),
                containsInAnyOrder(row(item(1), intLiteral("1")), row(item(2), new TypedLiteral("02", xsdIntegerIri)),
                        row(item(3), intLiteral("x")), row(item(4), new TypedLiteral("4", unhandled))));
        assertThat(execute(
                "SELECT ?x WHERE { ?x ex:form ?form ; ex:type ?type ; ex:n ?n FILTER(STRDT(?form, ?type) = ?n) }"),
                containsInAnyOrder(row(item(1)), row(item(2))));
        assertThat(
                execute("SELECT ?x WHERE { ?x ex:form ?form ; ex:type ?type "
                        + "FILTER(DATATYPE(STRDT(?form, ?type)) = ?type) }"),
                containsInAnyOrder(row(item(1)), row(item(2)), row(item(3)), row(item(4))));
    }


    @Test
    @DisplayName("the literal of an invalid lexical form is the same term as that literal written in the query")
    void invalidForm() throws Exception
    {
        assertThat(execute("""
                SELECT (STRDT("x", xsd:int) AS ?literal) (STR(STRDT("x", xsd:int)) AS ?form) \
                (DATATYPE(STRDT("x", xsd:int)) AS ?type) \
                (sameTerm(STRDT("x", xsd:int), "x"^^xsd:int) AS ?same) WHERE { }"""),
                contains(row(intLiteral("x"), new TypedLiteral("x", xsdStringIri), xsdIntIri, bool(true))));

        // the constant form, the computed form of the item 3 and the written literal
        assertThat(execute("""
                SELECT DISTINCT ?literal WHERE { { BIND(STRDT("x", xsd:int) AS ?literal) } UNION \
                { ?x ex:form ?form FILTER(?form = "x") BIND(STRDT(?form, xsd:int) AS ?literal) } UNION \
                { VALUES ?literal { "x"^^xsd:int } } }"""), contains(row(intLiteral("x"))));

        // a form out of the range of the datatype is invalid too
        assertThat(
                execute("SELECT (STRDT(?form, xsd:int) AS ?literal) (isNumeric(STRDT(?form, xsd:int)) AS ?numeric) "
                        + "WHERE { VALUES ?form { \"2147483648\" } }"),
                contains(row(intLiteral("2147483648"), bool(false))));
    }


    @Test
    @DisplayName("a datatype computed by IRI from a relative reference is resolved against the base of the query")
    void resolvedDatatype() throws Exception
    {
        assertThat(execute("BASE <http://example.org/> SELECT (STRDT(\"4\", IRI(\"datatype\")) AS ?literal) WHERE { }"),
                contains(row(new TypedLiteral("4", unhandled))));
        assertThat(
                execute("BASE <http://www.w3.org/2001/XMLSchema> SELECT ?x WHERE { ?x ex:form ?form ; ex:n ?n "
                        + "FILTER(STRDT(?form, IRI(\"#int\")) = ?n) }"),
                containsInAnyOrder(row(item(1)), row(item(2)), row(item(4))));
    }


    @Test
    @DisplayName("a literal of a datatype that is not handled equals the same literal only")
    void unhandledDatatype() throws Exception
    {
        assertThat(
                execute("SELECT (STRDT(\"4\", ex:datatype) AS ?literal) "
                        + "(STRDT(\"4\", ex:datatype) = \"4\"^^ex:datatype AS ?equal) WHERE { }"),
                contains(row(new TypedLiteral("4", unhandled), bool(true))));
        assertThat(
                execute("SELECT ?x WHERE { ?x ex:form ?form FILTER(STRDT(?form, ex:datatype) = \"4\"^^ex:datatype) }"),
                contains(row(item(4))));
    }


    @Test
    @DisplayName("an argument of a wrong kind or an unbound one makes the call an error")
    void errors() throws Exception
    {
        assertThat(execute("SELECT (STRDT(1, xsd:int) AS ?number) (STRDT(\"1\"@en, xsd:int) AS ?tagged) "
                + "(STRDT(\"1\", \"xsd:int\") AS ?string) WHERE { }"), contains(row(null, null, null)));
        assertThat(
                execute("SELECT (BOUND(?literal) AS ?bound) (COALESCE(?literal, \"none\") AS ?value) "
                        + "WHERE { VALUES ?type { xsd:int UNDEF } BIND(STRDT(\"1\", ?type) AS ?literal) }"),
                containsInAnyOrder(row(bool(true), intLiteral("1")),
                        row(bool(false), new TypedLiteral("none", xsdStringIri))));
    }
}
