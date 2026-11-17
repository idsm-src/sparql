package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdBooleanIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.imcode.SqlSelect;
import cz.iocb.sparql.engine.rdf.DirLangStringLiteral;
import cz.iocb.sparql.engine.rdf.DirLangStringLiteral.Direction;
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
 * STRDT, STRLANG and STRLANGDIR of constant arguments: the result is the literal written so, a constant classified as
 * any written literal, so that the comparisons of it are decided at translation as well; the literals are the same as
 * those the functions build in SQL.
 */
public class ConstantConstructorTest
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


    @Test
    @DisplayName("the literals of constant arguments")
    void literals() throws Exception
    {
        assertThat(value("STRDT(\"1\", xsd:integer)"), is(new TypedLiteral("1", xsdIntegerIri)));
        assertThat(value("STRDT(\"01\", xsd:integer)"), is(new TypedLiteral("01", xsdIntegerIri)));
        assertThat(value("STRDT(\"abc\", xsd:integer)"), is(new TypedLiteral("abc", xsdIntegerIri)));
        assertThat(value("STRDT(\"x\", ex:t)"), is(new TypedLiteral("x", new Iri("http://example.org/t"))));
        assertThat(value("STRDT(\"x\", xsd:string)"), is(new TypedLiteral("x", xsdStringIri)));
        assertThat(value("STRLANG(\"a\", \"EN\")"), is(new LangStringLiteral("a", "en")));
        assertThat(value("STRLANGDIR(\"a\", \"en\", \"rtl\")"), is(new DirLangStringLiteral("a", "en", Direction.RTL)));
    }


    @Test
    @DisplayName("comparisons of the literals are decided at translation")
    void comparisons() throws Exception
    {
        assertThat(value("STRDT(\"01\", xsd:integer) = 1"), is(new TypedLiteral("true", xsdBooleanIri)));
        assertThat(value("STRDT(\"2\", xsd:integer) < 1"), is(new TypedLiteral("false", xsdBooleanIri)));

        assertThat(translate("SELECT ?z WHERE { VALUES (?n ?s) { (1 \"x\") } "
                + "BIND(IF(STRDT(\"01\", xsd:integer) = 1, ?n, ?s) AS ?z) }"), not(containsString("CASE")));
    }
}
