package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdBooleanIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
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
 * Functions that are an error for some arguments of the kinds they take, run against an empty configuration: the call
 * is unbound for such arguments whether they are constants or come with the solutions, and what is built on the call
 * sees that, so BOUND is false, COALESCE takes its next argument and a CONSTRUCT template gives no triple. Such
 * functions are IRI (and URI) of a string that is no IRI reference, REGEX with an invalid pattern or invalid flags, and
 * REPLACE with these, with an invalid replacement or with a pattern matching the empty string.
 */
public class FunctionErrorTest
{
    private static final Iri subject = new Iri("http://example.org/s");
    private static final Iri predicate = new Iri("http://example.org/p");
    private static final Iri first = new Iri("http://example.org/a");
    private static final Iri second = new Iri("http://example.org/b");
    private static final TypedLiteral bound = new TypedLiteral("true", xsdBooleanIri);
    private static final TypedLiteral unbound = new TypedLiteral("false", xsdBooleanIri);
    private static final TypedLiteral fallback = new TypedLiteral("fallback", xsdStringIri);
    private static final TypedLiteral matched = new TypedLiteral("true", xsdBooleanIri);
    private static final TypedLiteral replaced = new TypedLiteral("axc", xsdStringIri);

    /**
     * Selection of a value, of whether it is bound and of the value or a fallback.
     */
    private static final String select = "SELECT ?v (BOUND(?v) AS ?b) (COALESCE(?v, \"fallback\") AS ?c) ";

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
    @DisplayName("IRI of a constant string that is no IRI reference is unbound")
    void iriOfConstant() throws Exception
    {
        assertThat(execute(select + "WHERE { BIND(IRI(\"http://example.org/a>b\") AS ?v) }"),
                contains(row(null, unbound, fallback)));
        assertThat(execute(select + "WHERE { BIND(URI(\"a b\") AS ?v) }"), contains(row(null, unbound, fallback)));
        assertThat(execute(select + "WHERE { BIND(IRI(\"http://example.org/a\") AS ?v) }"),
                contains(row(first, bound, first)));
    }


    @Test
    @DisplayName("IRI of a string of a solution that is no IRI reference is unbound")
    void iriOfVariable() throws Exception
    {
        String strings = "VALUES ?x { \"http://example.org/a\" \"http://example.org/a>b\" } ";

        assertThat(execute(select + "WHERE { " + strings + "BIND(IRI(?x) AS ?v) }"),
                containsInAnyOrder(row(first, bound, first), row(null, unbound, fallback)));
        assertThat(execute(select + "WHERE { " + strings + "BIND(URI(?x) AS ?v) }"),
                containsInAnyOrder(row(first, bound, first), row(null, unbound, fallback)));

        // strings together with an IRI are taken from the box
        String terms = "VALUES ?x { <http://example.org/a> \"http://example.org/b\" \"http://example.org/a>b\" } ";

        assertThat(execute(select + "WHERE { " + terms + "BIND(IRI(?x) AS ?v) }"),
                containsInAnyOrder(row(first, bound, first), row(second, bound, second), row(null, unbound, fallback)));
    }


    @Test
    @DisplayName("a template with IRI of a string that is no IRI reference gives no triple")
    void iriInTemplate() throws Exception
    {
        String template = "CONSTRUCT { <http://example.org/s> <http://example.org/p> ?v } ";

        assertThat(execute(template + "WHERE { BIND(IRI(\"http://example.org/a>b\") AS ?v) }"), empty());
        assertThat(execute(template + "WHERE { VALUES ?x { \"http://example.org/a\" \"http://example.org/a>b\" } "
                + "BIND(IRI(?x) AS ?v) }"), contains(row(subject, predicate, first)));
    }


    @Test
    @DisplayName("REGEX with an invalid pattern or invalid flags is unbound")
    void regex() throws Exception
    {
        assertThat(execute(select + "WHERE { BIND(REGEX(\"abc\", \"(\") AS ?v) }"),
                contains(row(null, unbound, fallback)));
        assertThat(execute(select + "WHERE { BIND(REGEX(\"abc\", \"b\", \"z\") AS ?v) }"),
                contains(row(null, unbound, fallback)));
        assertThat(execute(select + "WHERE { BIND(REGEX(\"abc\"@en, \"(\") AS ?v) }"),
                contains(row(null, unbound, fallback)));

        assertThat(execute(select + "WHERE { VALUES ?p { \"b\" \"(\" } BIND(REGEX(\"abc\", ?p) AS ?v) }"),
                containsInAnyOrder(row(matched, bound, matched), row(null, unbound, fallback)));
        assertThat(execute(select + "WHERE { VALUES ?f { \"i\" \"z\" } BIND(REGEX(\"abc\", \"B\", ?f) AS ?v) }"),
                containsInAnyOrder(row(matched, bound, matched), row(null, unbound, fallback)));
    }


    @Test
    @DisplayName("REPLACE with an invalid pattern, replacement or flags is unbound")
    void replace() throws Exception
    {
        assertThat(execute(select + "WHERE { BIND(REPLACE(\"abc\", \"(\", \"x\") AS ?v) }"),
                contains(row(null, unbound, fallback)));
        assertThat(execute(select + "WHERE { BIND(REPLACE(\"abc\", \"b\", \"x\", \"z\") AS ?v) }"),
                contains(row(null, unbound, fallback)));
        assertThat(execute(select + "WHERE { BIND(REPLACE(\"abc\", \"b\", \"$\") AS ?v) }"),
                contains(row(null, unbound, fallback)));

        // a pattern matching the empty string is an error of fn:replace
        assertThat(execute(select + "WHERE { BIND(REPLACE(\"abc\", \"\", \"x\") AS ?v) }"),
                contains(row(null, unbound, fallback)));

        assertThat(execute(select + "WHERE { VALUES ?p { \"b\" \"(\" \"\" } BIND(REPLACE(\"abc\", ?p, \"x\") AS ?v) }"),
                containsInAnyOrder(row(replaced, bound, replaced), row(null, unbound, fallback),
                        row(null, unbound, fallback)));
        assertThat(execute(select + "WHERE { VALUES ?r { \"x\" \"$\" } BIND(REPLACE(\"abc\", \"b\", ?r) AS ?v) }"),
                containsInAnyOrder(row(replaced, bound, replaced), row(null, unbound, fallback)));
    }


    @Test
    @DisplayName("a failing REPLACE of a language-tagged string has no language tag")
    void replaceOfLanguageTaggedString() throws Exception
    {
        String selection = "SELECT ?v (LANG(?v) AS ?l) (BOUND(?v) AS ?b) ";

        assertThat(execute(selection + "WHERE { BIND(REPLACE(\"abc\"@en, \"(\", \"x\") AS ?v) }"),
                contains(row(null, null, unbound)));
        assertThat(
                execute(selection + "WHERE { VALUES ?x { \"abc\"@en \"abc\"@en--ltr } "
                        + "BIND(REPLACE(?x, \"(\", \"x\") AS ?v) }"),
                contains(row(null, null, unbound), row(null, null, unbound)));
    }
}
