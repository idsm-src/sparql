package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
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
 * The number of arguments of a call of an extension function: a function without optional parameters requires all of
 * them, a function with optional ones at least the required ones and at most all of them; another number is an error of
 * the query.
 */
public class FunctionArgumentCountTest
{
    private static final String prefix = "PREFIX f: <http://example.org/> ";

    private static Engine engine = null;


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
     * Rows of the query run against the configuration of the functions.
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
     * Texts of the error messages of the query, which must be rejected.
     *
     * @param query the query, without the prefix declaration
     * @return texts of the error messages of the query
     */
    private static List<String> errors(String query)
    {
        TranslateExceptions exception = assertThrows(TranslateExceptions.class, () -> execute(query));

        return exception.getMessages().stream().filter(m -> m.getCategory() == MessageCategory.ERROR)
                .map(TranslateMessage::getMessage).toList();
    }


    @Test
    @DisplayName("a function without optional parameters requires all of them")
    void requiredParameters() throws Exception
    {
        assertEquals(List.of(List.of(new TypedLiteral("A", xsdStringIri))),
                execute("SELECT (f:upper(\"a\") AS ?r) WHERE {}"));
        assertEquals(List.of("Function 'f:upper' requires 1 parameter(s)"),
                errors("SELECT (f:upper() AS ?r) WHERE {}"));
        assertEquals(List.of("Function 'f:upper' requires 1 parameter(s)"),
                errors("SELECT (f:upper(\"a\", \"b\") AS ?r) WHERE {}"));
    }


    @Test
    @DisplayName("a function with optional parameters requires at least the required ones and at most all of them")
    void optionalParameters() throws Exception
    {
        assertEquals(List.of(List.of(new TypedLiteral("a", xsdStringIri))),
                execute("SELECT (f:join(\"-\", \"a\") AS ?r) WHERE {}"));
        assertEquals(List.of(List.of(new TypedLiteral("a-b", xsdStringIri))),
                execute("SELECT (f:join(\"-\", \"a\", \"b\") AS ?r) WHERE {}"));
        assertEquals(List.of("Function 'f:join' requires 2 to 3 parameter(s)"),
                errors("SELECT (f:join(\"-\") AS ?r) WHERE {}"));
        assertEquals(List.of("Function 'f:join' requires 2 to 3 parameter(s)"),
                errors("SELECT (f:join(\"-\", \"a\", \"b\", \"c\") AS ?r) WHERE {}"));
    }
}
