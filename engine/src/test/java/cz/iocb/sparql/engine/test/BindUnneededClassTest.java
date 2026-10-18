package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
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
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.TypedLiteral;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.LimitExceedException;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.translator.ServiceException;
import cz.iocb.sparql.testing.Database;



/**
 * BIND of an expression some of whose classes the rest of the query does not need, run against an empty configuration:
 * the expression does not materialise those classes, neither does the bound variable, and the query runs with the
 * classes that are needed.
 */
public class BindUnneededClassTest
{
    private static final TypedLiteral two = new TypedLiteral("2", xsdIntegerIri);
    private static final TypedLiteral minusTwo = new TypedLiteral("-2", xsdIntegerIri);
    private static final TypedLiteral b = new TypedLiteral("b", xsdStringIri);
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
    @DisplayName("a numeric operation on a bound variable that may be a string")
    void numericOperation() throws Exception
    {
        assertThat(execute("SELECT (-?b AS ?m) WHERE { VALUES ?x { 2 \"b\" } BIND(?x AS ?b) }"),
                containsInAnyOrder(row(minusTwo), unbound));
        assertThat(execute("SELECT (ABS(?b) AS ?m) WHERE { VALUES ?x { -2 \"b\" } BIND(?x AS ?b) }"),
                containsInAnyOrder(row(two), unbound));
    }


    @Test
    @DisplayName("a numeric operation on a variable bound by COALESCE")
    void coalesce() throws Exception
    {
        assertThat(execute("SELECT (-?b AS ?m) WHERE { VALUES (?x ?y) { (2 \"a\") (\"b\" 1) } "
                + "BIND(COALESCE(?x, ?y) AS ?b) }"), containsInAnyOrder(row(minusTwo), unbound));
    }


    @Test
    @DisplayName("the classes the projection needs stay materialised")
    void projected() throws Exception
    {
        assertThat(execute("SELECT ?b (-?b AS ?m) WHERE { VALUES ?x { 2 \"b\" } BIND(?x AS ?b) }"),
                containsInAnyOrder(row(two, minusTwo), row(b, null)));
    }
}
