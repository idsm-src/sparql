package cz.iocb.sparql.engine.test;

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
 * The effective boolean value (SPARQL 1.2, section 17.2.3) of terms of every kind given by VALUES: booleans, numerics
 * (zero and NaN are false), strings of the datatype xsd:string (the empty one is false), and the errors (an IRI, a
 * literal of an unknown datatype, an ill-typed literal, and a language-tagged string with or without a base direction,
 * which has no effective boolean value, as the W3C test {@code expression/not-not} confirms). The terms are tested in
 * their own classes and, through CONCAT over string literals of several kinds, in the box.
 */
public class EffectiveBooleanValueTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX : <http://example.org/> "
            + "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> ";

    /**
     * Rows of a term and its name, the string literals of every kind among them.
     */
    private static final String strings = """
            ("a" "string") ("" "empty") ("a"@en "lang") \
            (""@en "emptylang") ("a"@en--ltr "ltr") (""@en--ltr "emptyltr") ("a"@en--rtl "rtl") \
            (""@en--rtl "emptyrtl")\s""";

    /**
     * Rows of a term and its name: the string literals, booleans, numerics and the terms without an effective boolean
     * value.
     */
    private static final String terms = strings + "(true \"true\") (false \"false\") (1 \"int\") (0 \"zero\") "
            + "(1.5 \"decimal\") (0.0 \"zerodecimal\") (\"NaN\"^^xsd:double \"nan\") (1e0 \"double\") "
            + "(:x \"iri\") (\"x\"^^:dt \"unknown\") (\"abc\"^^xsd:int \"illtyped\") ";

    /**
     * Engine over the empty configuration.
     */
    private static Engine engine = null;


    /**
     * Creates the engine over the empty configuration.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        engine = new Engine(new SparqlDatabaseConfiguration(null, Database.getPool(), null, false));
    }


    /**
     * Names of the rows of the given terms selected by the pattern over {@code ?v} and {@code ?n}.
     *
     * @param rows the VALUES rows of a term and its name
     * @param pattern the pattern following the VALUES clause
     * @return names of the selected rows
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws LimitExceedException if the generated SQL is too long
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static List<RdfTerm> names(String rows, String pattern)
            throws SQLException, TranslateExceptions, LimitExceedException, ServiceException
    {
        String query = prefix + "SELECT ?n WHERE { VALUES (?v ?n) { " + rows + "} " + pattern + " }";

        try(Request request = engine.getRequest(); Result result = request.execute(query))
        {
            List<RdfTerm> names = new ArrayList<>();

            while(result.next())
                names.add(result.getRow()[0]);

            return names;
        }
    }


    /**
     * The xsd:string literals of the names.
     *
     * @param names the names
     * @return the xsd:string literals of the names
     */
    private static List<RdfTerm> expected(String... names)
    {
        return Arrays.stream(names).map(n -> (RdfTerm) new TypedLiteral(n, xsdStringIri)).toList();
    }


    @Test
    @DisplayName("FILTER keeps the terms whose effective boolean value is true")
    void filter() throws Exception
    {
        assertThat(names(terms, "FILTER(?v)"),
                containsInAnyOrder(expected("string", "true", "int", "decimal", "double").toArray()));
    }


    @Test
    @DisplayName("the negation keeps the terms whose effective boolean value is false")
    void negation() throws Exception
    {
        assertThat(names(terms, "FILTER(!?v)"),
                containsInAnyOrder(expected("empty", "false", "zero", "zerodecimal", "nan").toArray()));
    }


    @Test
    @DisplayName("terms without an effective boolean value are errors")
    void errors() throws Exception
    {
        assertThat(names(terms, "BIND(IF(?v, 1, 0) AS ?r) FILTER(!BOUND(?r))"), containsInAnyOrder(
                expected("iri", "unknown", "illtyped", "lang", "emptylang", "ltr", "emptyltr", "rtl", "emptyrtl")
                        .toArray()));
    }


    @Test
    @DisplayName("string literals of several kinds held in the box")
    void boxed() throws Exception
    {
        assertThat(names(strings, "FILTER(CONCAT(?v))"), containsInAnyOrder(expected("string").toArray()));
        assertThat(names(strings, "FILTER(!CONCAT(?v))"), containsInAnyOrder(expected("empty").toArray()));
    }
}
