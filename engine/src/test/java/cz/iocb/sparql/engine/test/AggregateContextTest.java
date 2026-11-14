package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.Function;
import cz.iocb.sparql.engine.error.MessageCategory;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.error.TranslateMessage;
import cz.iocb.sparql.engine.mapping.extension.FunctionDefinition;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.TypedLiteral;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.LimitExceedException;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.translator.ServiceException;
import cz.iocb.sparql.testing.Database;



/**
 * The places an aggregate may stand at: anywhere in the expressions of the projection, of HAVING and of ORDER BY of a
 * select, also in the arguments of a call written with an IRI, which is a cast or an extension function. Inside another
 * aggregate and in the other expressions of a query (FILTER, BIND, GROUP BY) an aggregate is an error of the query, at
 * any depth of the calls.
 */
public class AggregateContextTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "
            + "PREFIX f: <http://example.org/> ";

    /**
     * The solutions: the groups 1 to 4 have the sums 4.0, 0.5, 11.25 and 3.0 and the averages 2.0, 0.5, 5.625 and 3.0
     * of their numbers.
     */
    private static final String values = """
            VALUES (?g ?x ?s) { (1 1.5 'a') (1 2.5 'b') (2 0.5 'c') (3 10.25 'd') (3 1.0 'e') (4 3.0 'f') }""";

    /**
     * The message of an aggregate at a place that takes none.
     */
    private static final String misplaced = "Aggregates are allowed only in result sets.";

    /**
     * Engine of the configuration with the extension functions.
     */
    private static Engine engine = null;


    /**
     * Creates the configuration with the extension functions {@code f:upper} of one argument and {@code f:join} of a
     * separator and one or two strings.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, Database.getPool(), null, false);

        config.addFunction(new FunctionDefinition("http://example.org/upper", new Function("pg_catalog", "upper"),
                xsdString, List.of(xsdString), false, true));
        config.addFunction(new FunctionDefinition("http://example.org/join", new Function("pg_catalog", "concat_ws"),
                xsdString, List.of(xsdString, xsdString, xsdString), 2, false, true));

        engine = new Engine(config);
    }


    /**
     * Rows of the query in the order the query returns them.
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
     * Texts of the error messages of the query, which must be rejected.
     *
     * @param query the query, without the prefix declarations
     * @return texts of the error messages of the query
     */
    private static List<String> errors(String query)
    {
        TranslateExceptions exception = assertThrows(TranslateExceptions.class, () -> execute(query));

        return exception.getMessages().stream().filter(m -> m.getCategory() == MessageCategory.ERROR)
                .map(TranslateMessage::getMessage).toList();
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
     * An xsd:integer literal.
     *
     * @param value the value
     * @return the literal
     */
    private static RdfTerm integer(int value)
    {
        return new TypedLiteral(Integer.toString(value), xsdIntegerIri);
    }


    /**
     * An xsd:string literal.
     *
     * @param value the value
     * @return the literal
     */
    private static RdfTerm string(String value)
    {
        return new TypedLiteral(value, xsdStringIri);
    }


    @Test
    @DisplayName("an aggregate in the argument of a cast of the projection")
    void inCast() throws Exception
    {
        assertEquals(
                List.of(row(integer(1), integer(4)), row(integer(2), integer(0)), row(integer(3), integer(11)),
                        row(integer(4), integer(3))),
                execute("SELECT ?g (xsd:integer(SUM(?x)) AS ?i) WHERE { " + values + " } GROUP BY ?g ORDER BY ?g"));

        // the aggregate in the cast is what makes the select a grouped one
        assertEquals(List.of(row(string("6"))),
                execute("SELECT (xsd:string(COUNT(*)) AS ?c) WHERE { " + values + " }"));

        // a cast inside the aggregate inside the cast
        assertEquals(List.of(row(string("17"))),
                execute("SELECT (xsd:string(SUM(xsd:integer(?x))) AS ?c) WHERE { " + values + " }"));

        // beside a grouped variable in a cast
        assertEquals(
                List.of(row(integer(1), integer(5)), row(integer(2), integer(2)), row(integer(3), integer(14)),
                        row(integer(4), integer(7))),
                execute("SELECT ?g (xsd:integer(?g) + xsd:integer(SUM(?x)) AS ?i) WHERE { " + values
                        + " } GROUP BY ?g ORDER BY ?g"));
    }


    @Test
    @DisplayName("an aggregate in the arguments of an extension function")
    void inExtensionFunction() throws Exception
    {
        assertEquals(
                List.of(row(integer(1), string("A"), string("a-b")), row(integer(2), string("C"), string("c-c")),
                        row(integer(3), string("D"), string("d-e")), row(integer(4), string("F"), string("f-f"))),
                execute("SELECT ?g (f:upper(MIN(?s)) AS ?u) (f:join('-', MIN(?s), MAX(?s)) AS ?j) WHERE { " + values
                        + " } GROUP BY ?g ORDER BY ?g"));
    }


    @Test
    @DisplayName("an aggregate in the argument of a cast of HAVING")
    void inHaving() throws Exception
    {
        List<List<RdfTerm>> groups = List.of(row(integer(1)), row(integer(3)));

        assertEquals(groups,
                execute("SELECT ?g WHERE { " + values + " } GROUP BY ?g HAVING(xsd:integer(SUM(?x)) > 3) ORDER BY ?g"));

        // a condition that is a call written with an IRI itself
        assertEquals(groups, execute(
                "SELECT ?g WHERE { " + values + " } GROUP BY ?g HAVING xsd:boolean(COUNT(?x) - 1) ORDER BY ?g"));
    }


    @Test
    @DisplayName("an aggregate in the argument of a cast of ORDER BY")
    void inOrderBy() throws Exception
    {
        assertEquals(List.of(row(integer(2)), row(integer(4)), row(integer(1)), row(integer(3))),
                execute("SELECT ?g WHERE { " + values + " } GROUP BY ?g ORDER BY xsd:integer(SUM(?x)) ?g"));
        assertEquals(List.of(row(integer(3)), row(integer(4)), row(integer(1)), row(integer(2))),
                execute("SELECT ?g WHERE { " + values + " } GROUP BY ?g ORDER BY DESC(xsd:integer(AVG(?x))) ?g"));

        // the aggregate in the cast is what makes the select a grouped one, of a single group
        assertEquals(List.of(row(integer(1))),
                execute("SELECT (1 AS ?one) WHERE { " + values + " } ORDER BY xsd:integer(COUNT(*))"));
    }


    @Test
    @DisplayName("an aggregate inside an aggregate or outside the result expressions is an error")
    void rejected()
    {
        assertEquals(List.of(misplaced),
                errors("SELECT (xsd:integer(SUM(xsd:integer(MAX(?x)))) AS ?c) WHERE { " + values + " }"));
        assertEquals(List.of(misplaced), errors("SELECT ?g WHERE { " + values + " FILTER(xsd:integer(SUM(?x)) > 1) }"));
        assertEquals(List.of(misplaced), errors("SELECT ?g WHERE { " + values + " BIND(xsd:integer(SUM(?x)) AS ?y) }"));
        assertEquals(List.of(misplaced),
                errors("SELECT ?k WHERE { " + values + " } GROUP BY (xsd:integer(SUM(?x)) AS ?k)"));
    }


    @Test
    @DisplayName("a variable in a cast of a grouped select has to be grouped or aggregated")
    void ungroupedVariable()
    {
        assertEquals(List.of("Variable 'x' is used outside aggregate, but it is not mentioned in the GROUP BY clause."),
                errors("SELECT (xsd:integer(?x) AS ?i) WHERE { " + values + " } GROUP BY ?g"));
    }
}
