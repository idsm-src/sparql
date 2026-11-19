package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.imcode.SqlSelect;
import cz.iocb.sparql.engine.rdf.DirLangStringLiteral;
import cz.iocb.sparql.engine.rdf.DirLangStringLiteral.Direction;
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
 * REGEX of a text bound in several classes (an xsd:string, language-tagged strings with a base direction and without,
 * an integer): the classes that can hold a string literal are tested by one call, which reads the string literal of
 * every class, and a text of another kind is an error.
 */
public class RegexFunctionTest
{
    /**
     * The texts of the queries.
     */
    private static final String texts = "VALUES ?v { \"abc\" \"abc\"@en \"abc\"@en--ltr \"xyz\"@de--rtl 1 } ";

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
     * Rows of the query.
     *
     * @param query the query
     * @return rows of the query
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws LimitExceedException if the generated SQL is too long
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static List<List<RdfTerm>> execute(String query)
            throws SQLException, TranslateExceptions, LimitExceedException, ServiceException
    {
        try(Request request = engine.getRequest(); Result result = request.execute(query))
        {
            List<List<RdfTerm>> rows = new ArrayList<>();

            while(result.next())
                rows.add(Arrays.asList(result.getRow()));

            return rows;
        }
    }


    /**
     * The number of the calls of the regular expression functions of the extension in the SQL of the query.
     *
     * @param query the query
     * @return the number of the calls
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static int calls(String query) throws SQLException, TranslateExceptions, ServiceException
    {
        try(Request request = engine.getRequest())
        {
            PreparedQuery prepared = request.prepareQuery(query, null);

            SqlSelect imcode = new TranslateVisitor(request).translate(prepared.getSyntaxTree(), null, null, List.of());
            String sql = imcode.optimize(request, false).optimize(request, true).translate(request);

            Matcher matcher = Pattern.compile("sparql\\.regex_").matcher(sql);
            int count = 0;

            while(matcher.find())
                count++;

            return count;
        }
    }


    @Test
    @DisplayName("the texts of several classes are tested by one call")
    void severalClasses() throws Exception
    {
        List<String> queries = List.of("SELECT ?v WHERE { " + texts + "FILTER(REGEX(?v, \"b\")) }",
                "SELECT ?v WHERE { " + texts + "BIND(\"B\" AS ?p) FILTER(REGEX(?v, ?p, \"i\")) }",
                "SELECT ?v WHERE { " + texts + "FILTER(REGEX(LCASE(?v), \"^a\")) }");

        for(String query : queries)
        {
            assertThat(query, execute(query),
                    containsInAnyOrder(List.of(new TypedLiteral("abc", xsdStringIri)),
                            List.of(new LangStringLiteral("abc", "en")),
                            List.of(new DirLangStringLiteral("abc", "en", Direction.LTR))));

            assertThat(query, calls(query), is(1));
        }
    }
}
