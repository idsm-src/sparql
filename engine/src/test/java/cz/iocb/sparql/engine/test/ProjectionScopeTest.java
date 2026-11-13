package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdBooleanIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
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
import cz.iocb.sparql.engine.rdf.TypedLiteral;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.LimitExceedException;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.translator.ServiceException;
import cz.iocb.sparql.testing.Database;



/**
 * The variables the expressions of a projection see, run against an empty configuration. The expressions are evaluated
 * after the grouping and after the join with the trailing VALUES clause of their select, so they see the variables the
 * conditions of the GROUP BY clause introduce and the variables of the trailing VALUES clause, although the clauses
 * follow them in the text. The queries do not project these variables, because a projected variable is known before the
 * clauses are parsed. The pattern of the select sees neither, and HAVING, which is evaluated before the join and before
 * the expressions, sees only the variables of the GROUP BY clause.
 */
public class ProjectionScopeTest
{
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


    /**
     * The xsd:integer literal.
     *
     * @param value the value
     * @return the xsd:integer literal
     */
    private static TypedLiteral integer(int value)
    {
        return new TypedLiteral(Integer.toString(value), xsdIntegerIri);
    }


    /**
     * The xsd:boolean literal.
     *
     * @param value the value
     * @return the xsd:boolean literal
     */
    private static TypedLiteral bool(boolean value)
    {
        return new TypedLiteral(Boolean.toString(value), xsdBooleanIri);
    }


    @Test
    @DisplayName("an expression sees the variable of a GROUP BY condition")
    void groupVariable() throws Exception
    {
        String group = "WHERE { VALUES ?x { 1 2 } } GROUP BY (?x + 1 AS ?g)";

        assertThat(execute("SELECT (?g + 1 AS ?y) " + group), containsInAnyOrder(row(integer(3)), row(integer(4))));
        assertThat(execute("SELECT (?g + 1 AS ?y) (?y + 1 AS ?z) " + group),
                containsInAnyOrder(row(integer(3), integer(4)), row(integer(4), integer(5))));
        assertThat(execute("SELECT (EXISTS { VALUES ?g { 2 } } AS ?e) (COUNT(*) AS ?count) " + group),
                containsInAnyOrder(row(bool(true), integer(1)), row(bool(false), integer(1))));
        assertThat(execute("SELECT (?g + 1 AS ?y) " + group + " HAVING(?g > 2) ORDER BY ?g"),
                contains(row(integer(4))));
    }


    @Test
    @DisplayName("the variable of a GROUP BY condition is bound in the solutions of the groups")
    void groupVariableInGroups() throws Exception
    {
        // the condition is a BIND at the end of the pattern, so the aggregates see its variable
        assertThat(execute("SELECT (SUM(?g) AS ?sum) WHERE { VALUES ?x { 1 2 2 } } GROUP BY (?x + 1 AS ?g)"),
                containsInAnyOrder(row(integer(2)), row(integer(6))));

        // and so do the conditions following it, but not those preceding it
        assertThat(execute("SELECT (?h AS ?y) WHERE { VALUES ?x { 1 2 } } GROUP BY (?x + 1 AS ?g) (?g + 1 AS ?h)"),
                containsInAnyOrder(row(integer(3)), row(integer(4))));
        assertThat(
                execute("SELECT (?g AS ?y) (?h AS ?z) WHERE { VALUES ?x { 1 2 } } GROUP BY (?h + 1 AS ?g) (?x AS ?h)"),
                containsInAnyOrder(row(null, integer(1)), row(null, integer(2))));
    }


    @Test
    @DisplayName("the variable of a GROUP BY condition is seen neither in the pattern nor outside the select")
    void groupVariableElsewhere() throws Exception
    {
        assertThat(
                execute("SELECT (?g + 1 AS ?y) WHERE { VALUES ?x { 1 2 } FILTER(!BOUND(?g)) } GROUP BY (?x + 1 AS ?g)"),
                containsInAnyOrder(row(integer(3)), row(integer(4))));
        assertThat(
                execute("SELECT ?g ?y WHERE { VALUES ?g { 3 } "
                        + "{ SELECT (?g + 1 AS ?y) WHERE { VALUES ?x { 1 2 } } GROUP BY (?x + 1 AS ?g) } }"),
                containsInAnyOrder(row(integer(3), integer(3)), row(integer(3), integer(4))));
    }


    @Test
    @DisplayName("an expression sees a variable of the trailing VALUES clause")
    void valuesVariable() throws Exception
    {
        assertThat(execute("SELECT (?v + 1 AS ?y) WHERE { VALUES ?x { 1 } } VALUES ?v { 5 }"),
                contains(row(integer(6))));
        assertThat(
                execute("SELECT ?x (BOUND(?v) AS ?bound) WHERE { VALUES ?x { 1 2 } } "
                        + "VALUES (?x ?v) { (1 5) (2 UNDEF) }"),
                containsInAnyOrder(row(integer(1), bool(true)), row(integer(2), bool(false))));
        assertThat(
                execute("SELECT ?x (EXISTS { VALUES ?v { 5 } } AS ?e) WHERE { VALUES ?x { 1 2 } } "
                        + "VALUES (?x ?v) { (1 5) (2 6) }"),
                containsInAnyOrder(row(integer(1), bool(true)), row(integer(2), bool(false))));
    }


    @Test
    @DisplayName("a variable of the trailing VALUES clause is seen neither before the join nor outside the select")
    void valuesVariableElsewhere() throws Exception
    {
        assertThat(execute("SELECT ?x WHERE { VALUES ?x { 1 2 } FILTER(!BOUND(?v)) } VALUES (?x ?v) { (1 5) }"),
                contains(row(integer(1))));
        assertThat(execute("SELECT (SUM(?v) AS ?sum) WHERE { VALUES ?x { 1 2 } } GROUP BY ?x VALUES ?v { 5 }"),
                contains(row((RdfTerm) null), row((RdfTerm) null)));
        assertThat(execute("SELECT (COUNT(*) AS ?count) WHERE { VALUES ?x { 1 2 } } GROUP BY ?x HAVING(!BOUND(?v)) "
                + "VALUES ?v { 5 }"), contains(row(integer(1)), row(integer(1))));
        assertThat(
                execute("SELECT ?v ?y WHERE { VALUES ?v { 7 } "
                        + "{ SELECT (?v + 1 AS ?y) WHERE { VALUES ?x { 1 } } VALUES ?v { 5 } } }"),
                contains(row(integer(7), integer(6))));
    }


    @Test
    @DisplayName("HAVING does not see the variable of an expression")
    void expressionVariableInHaving() throws Exception
    {
        String group = "WHERE { VALUES ?x { 1 2 2 } } GROUP BY ?x";

        assertThat(execute("SELECT (COUNT(*) AS ?count) " + group + " HAVING(?count > 1)"), empty());
        assertThat(execute("SELECT (COUNT(*) AS ?count) " + group + " HAVING(COUNT(*) > 1)"),
                contains(row(integer(2))));
    }
}
