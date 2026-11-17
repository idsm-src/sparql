package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdBooleanIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDoubleIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdFloatIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import java.sql.SQLException;
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
 * The positive infinity of xsd:float and xsd:double written with a plus sign, which the lexical spaces of XML Schema
 * 1.1 allow: the literal keeps its form, its value is the infinity of the canonical form {@code INF}, and it compares
 * and has an effective boolean value as that one.
 */
public class InfinityLiteralTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> ";

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
    @DisplayName("an infinity written with a plus sign")
    void positiveInfinity() throws Exception
    {
        assertThat(value("\"+INF\"^^xsd:double"), is(new TypedLiteral("+INF", xsdDoubleIri)));
        assertThat(value("\"+INF\"^^xsd:float"), is(new TypedLiteral("+INF", xsdFloatIri)));
        assertThat(value("\"+INF\"^^xsd:float = \"INF\"^^xsd:float"), is(bool(true)));
        assertThat(value("\" +INF \"^^xsd:double > 1"), is(bool(true)));
        assertThat(value("STR(\"+INF\"^^xsd:double)"), is(new TypedLiteral("+INF", xsdStringIri)));
        assertThat(value("IF(\"+INF\"^^xsd:double, \"t\", \"f\")"), is(new TypedLiteral("t", xsdStringIri)));
    }
}
