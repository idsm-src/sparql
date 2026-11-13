package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
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
import cz.iocb.sparql.engine.rdf.Variable;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.LimitExceedException;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.translator.ServiceException;
import cz.iocb.sparql.testing.Database;



/**
 * The variables {@code SELECT *} projects, run against an empty configuration: the variables in scope in the pattern of
 * the select and the variables of its trailing VALUES clause. A sub-select brings into scope only what it projects, so
 * the variables of its pattern and of its trailing VALUES clause only if it projects them or uses {@code *} itself. A
 * variable that occurs only in a FILTER, in a MINUS, in an EXISTS or in the expression of a BIND is not in scope.
 */
public class StarProjectionTest
{
    private static Engine engine = null;


    @BeforeAll
    static void init() throws SQLException
    {
        engine = new Engine(new SparqlDatabaseConfiguration(null, Database.getPool(), null, false));
    }


    /**
     * Names of the variables the query projects.
     *
     * @param query the query
     * @return names of the variables the query projects
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws LimitExceedException if the generated SQL is too long
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static List<String> variables(String query)
            throws SQLException, TranslateExceptions, LimitExceedException, ServiceException
    {
        try(Request request = engine.getRequest(); Result result = request.execute(query))
        {
            return result.getHeads().stream().map(Variable::getName).toList();
        }
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
     * Row of the xsd:integer literals.
     *
     * @param values the values
     * @return row of the xsd:integer literals
     */
    private static List<RdfTerm> row(int... values)
    {
        List<RdfTerm> row = new ArrayList<>();

        for(int value : values)
            row.add(new TypedLiteral(Integer.toString(value), xsdIntegerIri));

        return row;
    }


    @Test
    @DisplayName("the variables of the pattern and of the trailing VALUES clause are projected")
    void patternVariables() throws Exception
    {
        assertEquals(List.of("x", "v"), variables("SELECT * WHERE { VALUES ?x { 1 } } VALUES ?v { 5 }"));
        assertEquals(List.of("x", "o", "u", "w", "b"), variables("SELECT * WHERE { VALUES ?x { 1 } "
                + "OPTIONAL { VALUES ?o { 1 } } { VALUES ?u { 1 } } UNION { VALUES ?w { 1 } } BIND(1 AS ?b) }"));
        assertEquals(List.of("g", "x"), variables("SELECT * WHERE { GRAPH ?g { VALUES ?x { 1 } } }"));
    }


    @Test
    @DisplayName("a variable only in a FILTER, a MINUS, an EXISTS or the expression of a BIND is not projected")
    void hiddenVariables() throws Exception
    {
        assertEquals(List.of("x", "b"), variables("SELECT * WHERE { VALUES ?x { 1 } FILTER(?f = 1) "
                + "MINUS { VALUES ?m { 1 } } BIND(?e + 1 AS ?b) FILTER EXISTS { VALUES ?z { 1 } } }"));
    }


    @Test
    @DisplayName("a sub-select with a projection brings only the projected variables into scope")
    void projectingSubSelect() throws Exception
    {
        String subselect = "{ SELECT ?y WHERE { VALUES (?y ?h) { (1 2) } } VALUES ?v { 5 } }";

        assertEquals(List.of("y"), variables("SELECT * WHERE { " + subselect + " }"));
        assertThat(execute("SELECT * WHERE { " + subselect + " }"), contains(row(1)));

        assertEquals(List.of("count"), variables(
                "SELECT * WHERE { { SELECT (COUNT(*) AS ?count) WHERE { VALUES ?y { 1 } } VALUES ?v { 5 } } }"));
        assertEquals(List.of("y", "w"),
                variables("SELECT * WHERE { { SELECT * WHERE { " + subselect + " } VALUES ?w { 6 } } }"));

        // the variable of the same name outside the sub-select is another variable
        assertEquals(List.of("y", "v"), variables("SELECT * WHERE { " + subselect + " } VALUES ?v { 7 }"));
        assertThat(execute("SELECT * WHERE { " + subselect + " } VALUES ?v { 7 }"), contains(row(1, 7)));
    }


    @Test
    @DisplayName("a sub-select with * brings the variables of its pattern and of its VALUES clause into scope")
    void starSubSelect() throws Exception
    {
        String subselect = "{ SELECT * WHERE { VALUES (?y ?h) { (1 2) } } VALUES ?v { 5 } }";

        assertEquals(List.of("y", "h", "v"), variables("SELECT * WHERE { " + subselect + " }"));
        assertThat(execute("SELECT * WHERE { " + subselect + " }"), contains(row(1, 2, 5)));
    }
}
