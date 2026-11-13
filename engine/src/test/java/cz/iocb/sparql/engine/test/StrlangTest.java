package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdBooleanIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerIri;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
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
import cz.iocb.sparql.engine.rdf.DirLangStringLiteral;
import cz.iocb.sparql.engine.rdf.DirLangStringLiteral.Direction;
import cz.iocb.sparql.engine.rdf.LangStringLiteral;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.TypedLiteral;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.LimitExceedException;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.translator.ServiceException;
import cz.iocb.sparql.testing.Database;



/**
 * The language tag of STRLANG and STRLANGDIR, run against an empty configuration. A tag is well-formed regardless of
 * its case and the literal has it in lower case, so the literal is the same term as that literal written in the query;
 * an empty tag and a tag that is not well-formed make the call an error. That holds for a computed tag as for a
 * constant one.
 */
public class StrlangTest
{
    private static final TypedLiteral yes = new TypedLiteral("true", xsdBooleanIri);
    private static final TypedLiteral no = new TypedLiteral("false", xsdBooleanIri);

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
     * The xsd:string literal.
     *
     * @param value the value
     * @return the xsd:string literal
     */
    private static TypedLiteral string(String value)
    {
        return new TypedLiteral(value, xsdStringIri);
    }


    /**
     * The string {@code a} with the language tag.
     *
     * @param tag the language tag
     * @return the string {@code a} with the language tag
     */
    private static LangStringLiteral tagged(String tag)
    {
        return new LangStringLiteral("a", tag);
    }


    @Test
    @DisplayName("a computed tag is lower-cased as a constant one is")
    void computedTag() throws Exception
    {
        assertThat(
                execute("SELECT ?tag (STRLANG(\"a\", ?tag) AS ?literal) (LANG(STRLANG(\"a\", ?tag)) AS ?lang) "
                        + "WHERE { VALUES ?tag { \"en\" \"EN\" \"en-US\" } }"),
                containsInAnyOrder(row(string("en"), tagged("en"), string("en")),
                        row(string("EN"), tagged("en"), string("en")),
                        row(string("en-US"), tagged("en-us"), string("en-us"))));
        assertThat(execute(
                "SELECT (STRLANG(\"a\", \"EN\") AS ?literal) (LANG(STRLANG(\"a\", \"EN\")) AS ?lang) WHERE { }"),
                contains(row(tagged("en"), string("en"))));
    }


    @Test
    @DisplayName("the literal is the same term as that literal written in the query")
    void sameTerm() throws Exception
    {
        assertThat(
                execute("SELECT ?tag (sameTerm(STRLANG(\"a\", ?tag), \"a\"@en) AS ?same) "
                        + "(STRLANG(\"a\", ?tag) = \"a\"@en AS ?equal) WHERE { VALUES ?tag { \"EN\" \"en-US\" } }"),
                containsInAnyOrder(row(string("EN"), yes, yes), row(string("en-US"), no, no)));

        // the constant tag, the computed tag and the written literal
        assertThat(execute("""
                SELECT DISTINCT ?literal WHERE { { BIND(STRLANG("a", "en") AS ?literal) } UNION \
                { VALUES ?tag { "EN" } BIND(STRLANG("a", ?tag) AS ?literal) } UNION \
                { VALUES ?literal { "a"@en } } }"""), contains(row(tagged("en"))));
        assertThat(
                execute("SELECT ?literal WHERE { { BIND(STRLANG(\"a\", \"en\") AS ?literal) } "
                        + "{ VALUES ?tag { \"EN\" } BIND(STRLANG(\"a\", ?tag) AS ?literal) } }"),
                contains(row(tagged("en"))));
        assertThat(
                execute("SELECT (COUNT(DISTINCT ?literal) AS ?count) "
                        + "WHERE { VALUES ?tag { \"en\" \"EN\" \"En\" } BIND(STRLANG(\"a\", ?tag) AS ?literal) }"),
                contains(row(new TypedLiteral("1", xsdIntegerIri))));
    }


    @Test
    @DisplayName("an empty tag and a tag that is not well-formed make the call an error")
    void invalidTag() throws Exception
    {
        assertThat(execute("""
                SELECT ?tag (STRLANG("a", ?tag) AS ?literal) (BOUND(?bound) AS ?is) \
                (COALESCE(STRLANG("a", ?tag), "none") AS ?value) WHERE \
                { VALUES ?tag { "" "not valid" "abcdefghi" "e" } BIND(STRLANG("a", ?tag) AS ?bound) }"""),
                containsInAnyOrder(row(string(""), null, no, string("none")),
                        row(string("not valid"), null, no, string("none")),
                        row(string("abcdefghi"), null, no, string("none")),
                        row(string("e"), null, no, string("none"))));
        assertThat(
                execute("SELECT (STRLANG(\"a\", \"\") AS ?empty) (STRLANG(\"a\", \"not valid\") AS ?space) "
                        + "(STRLANG(\"a\", \"abcdefghi\") AS ?long) (STRLANG(\"a\", \"e\") AS ?short) WHERE { }"),
                contains(row(null, null, null, null)));
    }


    @Test
    @DisplayName("a tag is well-formed regardless of its case")
    void tagCase() throws Exception
    {
        assertThat(execute("SELECT (STRLANG(\"a\", \"en-X-abc\") AS ?use) (STRLANG(\"a\", \"X-private\") AS ?private) "
                + "(STRLANG(\"a\", \"I-AMI\") AS ?irregular) (STRLANG(\"a\", \"sgn-be-fr\") AS ?sign) WHERE { }"),
                contains(row(tagged("en-x-abc"), tagged("x-private"), tagged("i-ami"), tagged("sgn-be-fr"))));
        assertThat(
                execute("SELECT ?tag (STRLANG(\"a\", ?tag) AS ?literal) "
                        + "WHERE { VALUES ?tag { \"en-X-abc\" \"X-private\" \"I-AMI\" \"sgn-be-fr\" } }"),
                containsInAnyOrder(row(string("en-X-abc"), tagged("en-x-abc")),
                        row(string("X-private"), tagged("x-private")), row(string("I-AMI"), tagged("i-ami")),
                        row(string("sgn-be-fr"), tagged("sgn-be-fr"))));
    }


    @Test
    @DisplayName("an argument that is not a string is an error")
    void arguments() throws Exception
    {
        assertThat(execute("SELECT (STRLANG(1, \"en\") AS ?number) (STRLANG(\"a\"@en, \"en\") AS ?tagged) "
                + "(STRLANG(\"a\", 1) AS ?tag) WHERE { }"), contains(row(null, null, null)));
        assertThat(execute("SELECT (STRLANG(?form, ?tag) AS ?literal) WHERE "
                + "{ VALUES (?form ?tag) { (1 \"en\") (\"a\"@en \"en\") (\"a\" 1) (UNDEF \"en\") (\"a\" UNDEF) } }"),
                contains(row((RdfTerm) null), row((RdfTerm) null), row((RdfTerm) null), row((RdfTerm) null),
                        row((RdfTerm) null)));
    }


    @Test
    @DisplayName("STRLANGDIR takes its tag as STRLANG does")
    void direction() throws Exception
    {
        assertThat(execute("""
                SELECT (STRLANGDIR("a", "EN", "ltr") AS ?upper) \
                (STRLANGDIR("a", "en-X-abc", "ltr") AS ?use) \
                (STRLANGDIR("a", "I-AMI", "rtl") AS ?irregular) \
                (STRLANGDIR("a", "", "ltr") AS ?empty) WHERE { }"""),
                contains(row(new DirLangStringLiteral("a", "en", Direction.LTR),
                        new DirLangStringLiteral("a", "en-x-abc", Direction.LTR),
                        new DirLangStringLiteral("a", "i-ami", Direction.RTL), null)));
        assertThat(
                execute("SELECT ?tag (STRLANGDIR(\"a\", ?tag, \"ltr\") AS ?literal) "
                        + "WHERE { VALUES ?tag { \"EN\" \"en-X-abc\" \"\" \"not valid\" } }"),
                containsInAnyOrder(row(string("EN"), new DirLangStringLiteral("a", "en", Direction.LTR)),
                        row(string("en-X-abc"), new DirLangStringLiteral("a", "en-x-abc", Direction.LTR)),
                        row(string(""), null), row(string("not valid"), null)));
    }
}
