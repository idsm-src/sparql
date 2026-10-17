package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDecimalIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdDoubleIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdFloatIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerIri;
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
 * ABS, ROUND, CEIL and FLOOR of numeric literals written in non-canonical lexical forms, which are kept together with
 * their lexical forms in the columns of the base classes, run against an empty configuration: the functions compute the
 * canonical forms of the values instead of yielding unbound, and ROUND, CEIL and FLOOR do not return an integer in its
 * original lexical form.
 */
public class NonCanonicalNumericTest
{
    private static final String prefix = "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> ";

    private static Engine engine = null;


    @BeforeAll
    static void init() throws SQLException
    {
        engine = new Engine(new SparqlDatabaseConfiguration(null, Database.getPool(), null, false));
    }


    /**
     * Rows of ABS, ROUND, CEIL and FLOOR, in this order, of the given values run against the empty configuration.
     *
     * @param values the values of the VALUES clause
     * @return rows of the functions of the values
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws LimitExceedException if the generated SQL is too long
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static List<List<RdfTerm>> execute(String values)
            throws SQLException, TranslateExceptions, LimitExceedException, ServiceException
    {
        String query = prefix + "SELECT (ABS(?x) AS ?a) (ROUND(?x) AS ?r) (CEIL(?x) AS ?c) (FLOOR(?x) AS ?f) "
                + "WHERE { VALUES ?x { " + values + " } }";

        try(Request request = engine.getRequest(); Result result = request.execute(query))
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
    @DisplayName("functions of a double in a non-canonical lexical form")
    void doubleValue() throws Exception
    {
        assertEquals(List.of(row(xsdDoubleIri, "2.25E0", "-2.0E0", "-2.0E0", "-3.0E0")), execute("-2.25e0"));
    }


    @Test
    @DisplayName("functions of a float in a non-canonical lexical form")
    void floatValue() throws Exception
    {
        assertEquals(List.of(row(xsdFloatIri, "2.25E0", "-2.0E0", "-2.0E0", "-3.0E0")),
                execute("\"-2.25e0\"^^xsd:float"));
    }


    @Test
    @DisplayName("functions of a decimal in a non-canonical lexical form")
    void decimal() throws Exception
    {
        assertEquals(List.of(row(xsdDecimalIri, "2.25", "-2.0", "-2.0", "-3.0")), execute("\"-2.250\"^^xsd:decimal"));
    }


    @Test
    @DisplayName("functions of an integer in a non-canonical lexical form")
    void integer() throws Exception
    {
        assertEquals(List.of(row(xsdIntegerIri, "2", "-2", "-2", "-2")), execute("\"-02\"^^xsd:integer"));
    }
}
