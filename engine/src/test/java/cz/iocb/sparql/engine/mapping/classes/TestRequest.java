package cz.iocb.sparql.engine.mapping.classes;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.sql.ResultSet;
import java.sql.SQLException;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.request.RdfBoxParser;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.test.OptionalSuffixIriClass;
import cz.iocb.sparql.testing.Database;



/**
 * Requests over a configuration of the test database that registers the {@link UserDatatypes} and one user IRI class,
 * for the tests of the classes that classify constants through the request.
 */
public class TestRequest
{
    /**
     * Prefix of the IRIs of the registered user IRI class.
     */
    public static final String prefix = "http://example.org/id/";

    /**
     * The registered user IRI class, matching {@code <prefix><id>} and {@code <prefix><id>/<sub>}.
     */
    public static final UserIriClass iriClass = new OptionalSuffixIriClass("suffix", prefix);


    /**
     * Not instantiable.
     */
    private TestRequest()
    {
    }


    /**
     * Creates a request over the test configuration; the caller closes it.
     *
     * @return the request
     * @throws SQLException on database errors
     */
    public static Request create() throws SQLException
    {
        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, Database.getPool(), null, false);

        config.addDatatype(UserDatatypes.intDatatype);
        config.addDatatype(UserDatatypes.uuidDatatype);
        config.addDatatype(UserDatatypes.tokenDatatype);
        config.addIriClass(iriClass);

        return new Request(config);
    }


    /**
     * Evaluates the SQL expression of a box in the database and decodes the printed box.
     *
     * @param request the request whose statement runs the query
     * @param expression the SQL expression
     * @return the decoded term
     * @throws SQLException on database errors
     */
    public static RdfTerm evaluate(Request request, String expression) throws SQLException
    {
        try(ResultSet result = request.getStatement().executeQuery("SELECT " + expression))
        {
            assertTrue(result.next());
            return RdfBoxParser.parse(result.getString(1));
        }
    }
}
