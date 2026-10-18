package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDecimalIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDoubleIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerIri;
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
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.TypedLiteral;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.LimitExceedException;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.translator.ServiceException;
import cz.iocb.sparql.testing.Database;



/**
 * Unary plus and minus, run against an empty configuration: plus keeps the value and minus negates it, both in the base
 * numeric type of the operand and in the canonical lexical form, and a non-numeric operand is an error.
 */
public class UnaryArithmeticTest
{
    private static final String prefix = "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> ";
    private static final List<RdfTerm> unbound = Arrays.asList(null, null);

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
     * Row of literals of the datatype.
     *
     * @param datatype the datatype
     * @param values lexical forms of the literals
     * @return row of the literals
     */
    private static List<RdfTerm> row(Iri datatype, String... values)
    {
        return Arrays.stream(values).map(v -> (RdfTerm) new TypedLiteral(v, datatype)).toList();
    }


    @Test
    @DisplayName("unary plus keeps the value, unary minus negates it")
    void plusAndMinus() throws Exception
    {
        assertThat(
                execute("SELECT (+?x AS ?p) (-?x AS ?m) WHERE { VALUES ?x { 2 -2.5 \"-0.0\"^^xsd:double "
                        + "\"3\"^^xsd:byte \"02\"^^xsd:integer \"a\" } }"),
                containsInAnyOrder(row(xsdIntegerIri, "2", "-2"), row(xsdDecimalIri, "-2.5", "2.5"),
                        row(xsdDoubleIri, "-0.0E0", "0.0E0"), row(xsdIntegerIri, "3", "-3"),
                        row(xsdIntegerIri, "2", "-2"), unbound));
    }


    @Test
    @DisplayName("a filter with unary plus compares the value itself")
    void plusInFilter() throws Exception
    {
        assertThat(execute("SELECT ?x WHERE { VALUES ?x { 1 2 } FILTER(+?x = 1) }"),
                containsInAnyOrder(row(xsdIntegerIri, "1")));
    }
}
