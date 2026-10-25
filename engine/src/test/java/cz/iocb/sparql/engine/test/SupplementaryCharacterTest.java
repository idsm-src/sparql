package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdIntegerIri;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.model.IriNode;
import cz.iocb.sparql.engine.rdf.BlankNode;
import cz.iocb.sparql.engine.rdf.Iri;
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
 * Names with the characters above U+FFFF, which the production PN_CHARS_BASE of the SPARQL grammar includes up to
 * U+EFFFF: the prefixed names, variables and blank node labels of queries, and the prefixed names that IRIs are
 * abbreviated to.
 */
public class SupplementaryCharacterTest
{
    private static final String first = Character.toString(0x10000);
    private static final String last = Character.toString(0xEFFFF);
    private static final String fraktur = Character.toString(0x1D51E);
    private static final String outside = Character.toString(0xF0000);
    private static final String prefix = "PREFIX " + fraktur + first + ": <http://example.org/> ";

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


    @Test
    @DisplayName("a prefix and a local name may contain the characters above U+FFFF")
    void prefixedNames() throws Exception
    {
        assertEquals(List.of(List.of(new Iri("http://example.org/" + first + "." + last))),
                execute("SELECT ?x WHERE { VALUES ?x { " + fraktur + first + ":" + first + "." + last + " } }"));
    }


    @Test
    @DisplayName("a variable name may contain the characters above U+FFFF")
    void variables() throws Exception
    {
        String query = "SELECT ?" + fraktur + " (?" + fraktur + " + 1 AS ?" + first + last + ") WHERE { VALUES $"
                + fraktur + " { 1 } }";

        try(Request request = engine.getRequest(); Result result = request.execute(query))
        {
            assertEquals(List.of(new Variable(fraktur), new Variable(first + last)), result.getHeads());
            assertTrue(result.next());
            assertEquals(List.of(new TypedLiteral("1", xsdIntegerIri), new TypedLiteral("2", xsdIntegerIri)),
                    Arrays.asList(result.getRow()));
        }
    }


    @Test
    @DisplayName("a blank node label may contain the characters above U+FFFF")
    void blankNodeLabels() throws Exception
    {
        List<List<RdfTerm>> rows = execute(
                "CONSTRUCT { _:" + fraktur + " " + fraktur + first + ":p _:" + first + last + " } WHERE {}");

        assertEquals(1, rows.size());
        assertInstanceOf(BlankNode.class, rows.get(0).get(0));
        assertEquals(new Iri("http://example.org/p"), rows.get(0).get(1));
        assertInstanceOf(BlankNode.class, rows.get(0).get(2));
        assertNotEquals(rows.get(0).get(0), rows.get(0).get(2));
    }


    @Test
    @DisplayName("the characters from U+F0000 cannot occur in names")
    void charactersOutsideRange()
    {
        assertThrows(TranslateExceptions.class,
                () -> execute("SELECT ?x WHERE { VALUES ?x { " + fraktur + first + ":" + outside + " } }"));
        assertThrows(TranslateExceptions.class, () -> execute("SELECT ?" + outside + " WHERE {}"));
        assertThrows(TranslateExceptions.class, () -> execute("CONSTRUCT { _:" + outside + " ?p ?o } WHERE {}"));
    }


    @Test
    @DisplayName("an IRI is abbreviated to a prefixed name with the characters above U+FFFF")
    void abbreviation()
    {
        Map<String, String> prefixes = Map.of("e", "http://example.org/");

        assertEquals("e:" + first + "." + last,
                IriNode.toPrefixedIri("http://example.org/" + first + "." + last, prefixes));
        assertNull(IriNode.toPrefixedIri("http://example.org/" + outside, prefixes));
    }
}
