package cz.iocb.sparql.engine.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.LimitExceedException;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.translator.ServiceException;
import cz.iocb.sparql.testing.Database;



/**
 * The functions testing the kind of a term (isIRI, isURI, isBlank, isLiteral, isNumeric, isTRIPLE, hasLANG and
 * hasLANGDIR) of a variable that every solution leaves unbound, being bound to an error: the test is an error, which
 * leaves the variable bound to it unbound and drops the solutions a filter of it tests.
 */
public class TypeTestFunctionTest
{
    /**
     * The tested functions.
     */
    private static final List<String> functions = List.of("isIRI", "isURI", "isBlank", "isLiteral", "isNumeric",
            "isTRIPLE", "hasLANG", "hasLANGDIR");

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


    @Test
    @DisplayName("a test of a variable bound only to errors is an error")
    void unboundArgument() throws Exception
    {
        for(String function : functions)
        {
            assertThat(function,
                    execute("SELECT ?n WHERE { BIND(STRLANG(1, \"en\") AS ?e) BIND(" + function + "(?e) AS ?n) }"),
                    contains(Arrays.asList((RdfTerm) null)));

            assertThat(function,
                    execute("SELECT * WHERE { BIND(STRLANG(1, \"en\") AS ?e) FILTER(!" + function + "(?e)) }"),
                    empty());
        }
    }
}
