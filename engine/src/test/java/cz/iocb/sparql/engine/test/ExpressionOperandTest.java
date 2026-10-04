package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDecimalIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import cz.iocb.sparql.engine.rdf.TypedLiteral;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.LimitExceedException;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.translator.ServiceException;
import cz.iocb.sparql.testing.Database;



/**
 * Operations whose operands are computed by SQL expressions (arithmetic over a variable of mixed numeric classes), run
 * against an empty configuration: the combinations of disjoint operand classes are kept apart instead of being merged
 * into a union of the result classes, and a membership test evaluates such an operand once in a subquery.
 */
public class ExpressionOperandTest
{
    private static final TypedLiteral five = new TypedLiteral("5", xsdIntegerIri);
    private static final TypedLiteral fiveAndHalf = new TypedLiteral("5.5", xsdDecimalIri);

    private static Engine engine = null;


    @BeforeAll
    static void init() throws SQLException
    {
        engine = new Engine(new SparqlDatabaseConfiguration(null, Database.getPool(), null, false));
    }


    /**
     * Rows of the query run against the empty configuration.
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
    @DisplayName("arithmetic over an expression of several numeric classes keeps the classes apart")
    void nestedArithmetic() throws Exception
    {
        assertEquals(List.of(row(five)), execute("SELECT ?v WHERE { VALUES ?v { 5 5.5 } FILTER(((?v + 1) + 1) = 7) }"));
        assertEquals(List.of(row(fiveAndHalf)),
                execute("SELECT ?v WHERE { VALUES ?v { 5 5.5 } FILTER(((?v * 2) + 1) = 12) }"));
        assertEquals(List.of(row(fiveAndHalf)),
                execute("SELECT ?v WHERE { VALUES ?v { 5 5.5 } FILTER(((?v + 1) * 2) > 12) }"));
    }


    @Test
    @DisplayName("a membership test of an arithmetic result evaluates it once")
    void membership() throws Exception
    {
        assertEquals(List.of(row(five)), execute("SELECT ?v WHERE { VALUES ?v { 5 7 } FILTER((?v + 1) IN (6, 9)) }"));
        assertThat(execute("SELECT ?v WHERE { VALUES ?v { 5 5.5 } FILTER((?v * 2) IN (10, 11.0)) }"),
                containsInAnyOrder(row(five), row(fiveAndHalf)));
        assertEquals(List.of(row(fiveAndHalf)),
                execute("SELECT ?v WHERE { VALUES ?v { 5 5.5 } FILTER((?v * 2) NOT IN (10, 12)) }"));
    }
}
