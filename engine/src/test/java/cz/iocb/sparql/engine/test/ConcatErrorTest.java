package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
import static cz.iocb.sparql.engine.rdf.DirLangStringLiteral.Direction.LTR;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.rdf.DirLangStringLiteral;
import cz.iocb.sparql.engine.rdf.LangStringLiteral;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.TypedLiteral;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.LimitExceedException;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.translator.ServiceException;
import cz.iocb.sparql.testing.Database;



/**
 * CONCAT with an argument that is unbound or an error at run time, run against an empty configuration: the error
 * propagates to the result in every result class (xsd:string, a fixed language tag and the box) instead of the argument
 * being dropped.
 */
public class ConcatErrorTest
{
    private static final String prefix = "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> ";
    private static final TypedLiteral ab = new TypedLiteral("ab", xsdStringIri);
    private static final TypedLiteral a1 = new TypedLiteral("a1", xsdStringIri);
    private static final LangStringLiteral abEn = new LangStringLiteral("ab", "en");
    private static final DirLangStringLiteral abEnLtr = new DirLangStringLiteral("ab", "en", LTR);
    private static final List<RdfTerm> unbound = row((RdfTerm) null);

    private static Engine engine = null;


    @BeforeAll
    static void init() throws SQLException
    {
        engine = new Engine(new SparqlDatabaseConfiguration(null, Database.getPool(), null, false));
    }


    /**
     * Rows of the query run against the empty configuration.
     *
     * @param query the query, without the prefix declaration
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


    @Test
    @DisplayName("an unbound argument makes CONCAT unbound")
    void unboundArgument() throws Exception
    {
        assertThat(execute("SELECT (CONCAT(\"a\", ?x) AS ?r) WHERE { VALUES ?x { \"b\" UNDEF } }"),
                containsInAnyOrder(row(ab), unbound));
        assertThat(execute("SELECT (CONCAT(?x, \"b\") AS ?r) WHERE { VALUES ?x { \"a\" UNDEF } }"),
                containsInAnyOrder(row(ab), unbound));
        assertThat(execute("SELECT (CONCAT(\"a\", ?x, \"b\") AS ?r) WHERE { VALUES ?x { \"\" UNDEF } }"),
                containsInAnyOrder(row(ab), unbound));
    }


    @Test
    @DisplayName("an argument that is not a string literal makes CONCAT unbound")
    void nonStringArgument() throws Exception
    {
        assertThat(execute("SELECT (CONCAT(\"a\", ?x) AS ?r) WHERE { VALUES ?x { \"b\" 1 <http://example.org/> } }"),
                containsInAnyOrder(row(ab), unbound, unbound));
    }


    @Test
    @DisplayName("an erroneous argument makes CONCAT an error")
    void erroneousArgument() throws Exception
    {
        assertThat(execute("SELECT (CONCAT(\"a\", STR(xsd:integer(?x))) AS ?r) WHERE { VALUES ?x { \"1\" \"b\" } }"),
                containsInAnyOrder(row(a1), unbound));
    }


    @Test
    @DisplayName("an unbound argument makes CONCAT of a fixed language tag unbound")
    void fixedTag() throws Exception
    {
        assertThat(execute("SELECT (CONCAT(\"a\"@en, ?x) AS ?r) WHERE { VALUES ?x { \"b\"@en UNDEF } }"),
                containsInAnyOrder(row(abEn), unbound));
        assertThat(execute("SELECT (CONCAT(\"a\"@en--ltr, ?x) AS ?r) WHERE { VALUES ?x { \"b\"@en--ltr UNDEF } }"),
                containsInAnyOrder(row(abEnLtr), unbound));
    }


    @Test
    @DisplayName("an unbound argument makes CONCAT of the box unbound")
    void boxed() throws Exception
    {
        assertThat(
                execute("SELECT (CONCAT(?x, ?y) AS ?r) WHERE { VALUES (?x ?y) "
                        + "{ (\"a\" \"b\") (\"a\"@en \"b\"@en) (\"a\" UNDEF) (UNDEF \"b\"@en) } }"),
                containsInAnyOrder(row(ab), row(abEn), unbound, unbound));
    }
}
