package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDecimalIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDoubleIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdFloatIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.TypedLiteral;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.LimitExceedException;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.translator.ServiceException;
import cz.iocb.sparql.testing.Database;



/**
 * ROUND of values kept in columns of their own numeric type, run against an empty configuration: a half is rounded
 * towards positive infinity, as fn:round does, also where round() of PostgreSQL rounds it away from zero (xsd:decimal)
 * or to even (xsd:double, xsd:float), and a negative double or float that rounds to zero keeps its sign.
 */
public class RoundFunctionTest
{
    private static final String prefix = "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> ";

    private static Engine engine = null;


    @BeforeAll
    static void init() throws SQLException
    {
        engine = new Engine(new SparqlDatabaseConfiguration(null, Database.getPool(), null, false));
    }


    /**
     * Values of the only projected variable of the query run against the empty configuration.
     *
     * @param query the query, without the prefix declaration
     * @return values of the only projected variable
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws LimitExceedException if the generated SQL is too long
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static List<RdfTerm> execute(String query)
            throws SQLException, TranslateExceptions, LimitExceedException, ServiceException
    {
        try(Request request = engine.getRequest(); Result result = request.execute(prefix + query))
        {
            List<RdfTerm> values = new ArrayList<>();

            while(result.next())
                values.add(result.getRow()[0]);

            return values;
        }
    }


    @Test
    @DisplayName("ROUND of xsd:decimal rounds a half up")
    void decimal() throws Exception
    {
        assertThat(execute("SELECT (ROUND(?x) AS ?r) WHERE { VALUES ?x { -2.5 -0.5 0.5 2.5 } }"),
                containsInAnyOrder(new TypedLiteral("-2.0", xsdDecimalIri), new TypedLiteral("0.0", xsdDecimalIri),
                        new TypedLiteral("1.0", xsdDecimalIri), new TypedLiteral("3.0", xsdDecimalIri)));
    }


    @Test
    @DisplayName("ROUND of xsd:double rounds a half up")
    void doubleValue() throws Exception
    {
        assertThat(
                execute("SELECT (ROUND(?x) AS ?r) WHERE { VALUES ?x { -2.5E0 -5.0E-1 4.9999999999999994E-1 5.0E-1 "
                        + "2.5E0 } }"),
                containsInAnyOrder(new TypedLiteral("-2.0E0", xsdDoubleIri), new TypedLiteral("-0.0E0", xsdDoubleIri),
                        new TypedLiteral("0.0E0", xsdDoubleIri), new TypedLiteral("1.0E0", xsdDoubleIri),
                        new TypedLiteral("3.0E0", xsdDoubleIri)));
    }


    @Test
    @DisplayName("ROUND of xsd:float rounds a half up")
    void floatValue() throws Exception
    {
        assertThat(
                execute("SELECT (ROUND(?x) AS ?r) WHERE { VALUES ?x { \"-2.5E0\"^^xsd:float \"-5.0E-1\"^^xsd:float "
                        + "\"5.0E-1\"^^xsd:float \"2.5E0\"^^xsd:float } }"),
                containsInAnyOrder(new TypedLiteral("-2.0E0", xsdFloatIri), new TypedLiteral("-0.0E0", xsdFloatIri),
                        new TypedLiteral("1.0E0", xsdFloatIri), new TypedLiteral("3.0E0", xsdFloatIri)));
    }
}
