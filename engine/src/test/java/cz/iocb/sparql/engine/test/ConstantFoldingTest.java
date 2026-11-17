package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdBooleanIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.imcode.SqlSelect;
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
 * Comparisons and effective boolean values of constant literals decided at translation: exact numbers of any datatypes
 * and booleans by their values, xsd:strings by equality, two literals of unknown datatypes or invalid lexical forms by
 * equality (an error unless they are one term), and the effective boolean values of booleans, numbers and xsd:strings;
 * the other pairs and literals keep their way through SQL. A decided condition makes an IF take a single branch, and so
 * the class of that branch.
 */
public class ConstantFoldingTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "
            + "PREFIX ex: <http://example.org/> ";

    /**
     * Engine over a configuration mapping nothing.
     */
    private static Engine engine = null;


    /**
     * Creates the engine.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        engine = new Engine(new SparqlDatabaseConfiguration(null, Database.getPool(), null, false));
    }


    /**
     * The value of the expression, or null when it is unbound.
     *
     * @param expression the expression
     * @return the value of the expression, or null when it is unbound
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws LimitExceedException if the generated SQL is too long
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static RdfTerm value(String expression)
            throws SQLException, TranslateExceptions, LimitExceedException, ServiceException
    {
        try(Request request = engine.getRequest();
                Result result = request.execute(prefix + "SELECT (" + expression + " AS ?v) WHERE {}"))
        {
            result.next();
            return result.getRow()[0];
        }
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
        try(Request request = engine.getRequest())
        {
            PreparedQuery prepared = request.prepareQuery(prefix + query, null);

            SqlSelect imcode = new TranslateVisitor(request).translate(prepared.getSyntaxTree(), null, null, List.of());

            return imcode.optimize(request, false).optimize(request, true).translate(request);
        }
    }


    /**
     * The xsd:boolean literal of the value.
     *
     * @param value the value
     * @return the xsd:boolean literal of the value
     */
    private static RdfTerm bool(boolean value)
    {
        return new TypedLiteral(Boolean.toString(value), xsdBooleanIri);
    }


    @Test
    @DisplayName("comparisons of constant literals")
    void comparisons() throws Exception
    {
        assertThat(value("1 = 1.0"), is(bool(true)));
        assertThat(value("\"01\"^^xsd:integer = 1"), is(bool(true)));
        assertThat(value("1.0 = 1.00"), is(bool(true)));
        assertThat(value("\"1\"^^xsd:byte != 2"), is(bool(true)));
        assertThat(value("1 < 2"), is(bool(true)));
        assertThat(value("2 <= 1.5"), is(bool(false)));
        assertThat(value("-1 >= \"-1.0\"^^xsd:decimal"), is(bool(true)));
        assertThat(value("true = \"1\"^^xsd:boolean"), is(bool(true)));
        assertThat(value("false < true"), is(bool(true)));
        assertThat(value("\"a\" = \"a\""), is(bool(true)));
        assertThat(value("\"a\" != \"b\""), is(bool(true)));
        assertThat(value("\"a\" < \"b\""), is(bool(true)));
        assertThat(value("\"x\"^^ex:t = \"x\"^^ex:t"), is(bool(true)));
        assertThat(value("\"x\"^^ex:t != \"x\"^^ex:t"), is(bool(false)));
        assertThat(value("\"x\"^^ex:t = \"y\"^^ex:t"), is(nullValue()));
        assertThat(value("\"abc\"^^xsd:integer = \"abc\"^^xsd:integer"), is(bool(true)));
        assertThat(value("\"abc\"^^xsd:integer = 1"), is(nullValue()));
        assertThat(value("1 = \"1\""), is(bool(false)));
        assertThat(value("\"a\"@en = \"a\"@en"), is(bool(true)));
        assertThat(value("1 IN (1.0, 2)"), is(bool(true)));
        assertThat(value("\"NaN\"^^xsd:double = \"NaN\"^^xsd:double"), is(bool(false)));
        assertThat(value("\"PT1H\"^^xsd:dayTimeDuration = \"PT60M\"^^xsd:dayTimeDuration"), is(bool(true)));
    }


    @Test
    @DisplayName("effective boolean values of constant literals")
    void effectiveBooleanValues() throws Exception
    {
        for(String operand : List.of("0.0", "\"0.00\"^^xsd:decimal", "\"-0.0e0\"^^xsd:double", "\"1e-50\"^^xsd:float",
                "\"NaN\"^^xsd:double", "\"0\"^^xsd:unsignedByte", "false", "\"0\"^^xsd:boolean", "\"\""))
            assertThat(operand, value("IF(" + operand + ", \"t\", \"f\")"), is(new TypedLiteral("f", xsdStringIri)));

        for(String operand : List.of("1", "\"01\"^^xsd:integer", "-0.5", "\"INF\"^^xsd:double", "\"-INF\"^^xsd:float",
                "\"1e-50\"^^xsd:double", "true", "\"1\"^^xsd:boolean", "\"abc\""))
            assertThat(operand, value("IF(" + operand + ", \"t\", \"f\")"), is(new TypedLiteral("t", xsdStringIri)));

        for(String operand : List.of("\"abc\"^^xsd:integer", "\"a\"@en", "\"x\"^^ex:t"))
            assertThat(operand, value("IF(" + operand + ", \"t\", \"f\")"), is(nullValue()));
    }


    @Test
    @DisplayName("a decided condition makes an IF take one branch")
    void decidedConditions() throws Exception
    {
        String values = "VALUES (?n ?s) { (1 \"x\") } ";

        for(String condition : List.of("1 = 1.0", "\"a\" = \"a\"", "true", "2", "\"abc\""))
            assertThat(condition,
                    translate("SELECT ?z WHERE { " + values + "BIND(IF(" + condition + ", ?n, ?s) AS ?z) }"),
                    not(containsString("CASE")));
    }
}
