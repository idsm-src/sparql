package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.database.Function;
import cz.iocb.sparql.engine.error.MessageType;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.mapping.classes.StringUserIriClass;
import cz.iocb.sparql.engine.mapping.extension.ParameterDefinition;
import cz.iocb.sparql.engine.mapping.extension.ProcedureDefinition;
import cz.iocb.sparql.engine.mapping.extension.ResultDefinition;
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
 * The nodes standing for the parameters and the results in the text of a procedure call: a variable may be such a node,
 * as clients that replace blank nodes by variables write it so, but the call binds nothing to it, so any other
 * occurrence of the variable in the patterns of the query is refused, as is one of a blank node inside a triple term,
 * like the one that an annotation of the call reifies; the projections and the solution modifiers may name the
 * variable, and the query then has the solutions of the call written with blank nodes. The things {@code a} and
 * {@code b} have the tag {@code red}, {@code c} the tag {@code blue}; the procedure {@code ex:search} returns the
 * things of a tag, {@code ex:describe} the things of a tag with the tag as the results {@code ex:thing} and
 * {@code ex:tag}.
 */
public class ProcedureCallSyntaxTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> ";

    /**
     * Call of {@code ex:search} for the tag {@code red} with the parameter node {@code ?p}.
     */
    private static final String search = "?y ex:search ?p . ?p ex:query 'red' . ";

    /**
     * Call of {@code ex:describe} for the tag {@code red} with the result node {@code ?r} and the parameter node
     * {@code ?q}.
     */
    private static final String describe = "?r ex:describe ?q . ?q ex:query 'red' . ?r ex:thing ?y ; ex:tag ?t . ";

    /**
     * Engine over the configuration.
     */
    private static Engine engine = null;


    /**
     * Creates the {@code procedure_call_syntax_test} schema with the table of the things and the functions of the
     * procedures, and maps them.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema procedure_call_syntax_test");
            statement.execute("create table procedure_call_syntax_test.item (x varchar, tag varchar)");
            statement.execute("insert into procedure_call_syntax_test.item values ('a', 'red'), ('b', 'red'), "
                    + "('c', 'blue')");
            statement.execute("""
                    create function procedure_call_syntax_test.search(query varchar) returns setof varchar \
                    language sql as $$ select x from procedure_call_syntax_test.item where tag = query $$""");
            statement.execute("""
                    create function procedure_call_syntax_test.describe(query varchar) \
                    returns table (x varchar, tag varchar) language sql as \
                    $$ select x, tag from procedure_call_syntax_test.item where tag = query $$""");
        }

        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, connectionPool,
                new DatabaseSchema(connectionPool), true);
        config.addPrefix("ex", "http://example.org/");

        StringUserIriClass thing = new StringUserIriClass("thing", "http://example.org/thing/", "[a-z]");
        config.addIriClass(thing);

        config.addQuadMapping(new DatabaseTable("procedure_call_syntax_test", "item"), null,
                config.createIriMapping("thing", "x"), config.createIriMapping("ex:tag"),
                config.createLiteralMapping(xsdString, "tag"));

        ProcedureDefinition search = new ProcedureDefinition("http://example.org/search",
                new Function("procedure_call_syntax_test", "search"));
        search.addParameter(new ParameterDefinition("http://example.org/query", xsdString, null));
        search.addResult(new ResultDefinition(thing));
        config.addProcedure(search);

        ProcedureDefinition describe = new ProcedureDefinition("http://example.org/describe",
                new Function("procedure_call_syntax_test", "describe"));
        describe.addParameter(new ParameterDefinition("http://example.org/query", xsdString, null));
        describe.addResult(new ResultDefinition("http://example.org/thing", thing, "x"));
        describe.addResult(new ResultDefinition("http://example.org/tag", xsdString, "tag"));
        config.addProcedure(describe);

        engine = new Engine(config);
    }


    /**
     * Rows of the query.
     *
     * @param query the query, without the prefix declarations
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
     * The thing.
     *
     * @param name the local name of the thing
     * @return the thing
     */
    private static Iri thing(String name)
    {
        return new Iri("http://example.org/thing/" + name);
    }


    /**
     * The string literal.
     *
     * @param value the string
     * @return the string literal
     */
    private static TypedLiteral string(String value)
    {
        return new TypedLiteral(value, xsdStringIri);
    }


    /**
     * Checks that the query is refused with the message of the given type about the node.
     *
     * @param query the query, without the prefix declarations
     * @param type the type of the message
     * @param node the name of the variable or the label of the blank node
     */
    private static void assertRefused(String query, MessageType type, String node)
    {
        TranslateExceptions e = assertThrows(TranslateExceptions.class, () -> execute(query));

        assertThat(query, e.getMessages(),
                hasItem(hasProperty("message", is(String.format(Locale.ROOT, type.getText(), node)))));
    }


    @Test
    @DisplayName("another occurrence of a variable standing for parameters or results is refused")
    void otherOccurrences()
    {
        MessageType parameter = MessageType.invalidParameterVariableOccurence;
        MessageType result = MessageType.invalidResultVariableOccurence;

        assertRefused("SELECT * WHERE { " + search + "?z ex:tag ?p }", parameter, "p");
        assertRefused("SELECT ?y WHERE { " + search + "FILTER(BOUND(?p)) }", parameter, "p");
        assertRefused("SELECT ?y WHERE { " + search + "{ ?z ex:tag ?p } }", parameter, "p");
        assertRefused("SELECT ?y WHERE { " + search + "OPTIONAL { ?z ex:tag ?t FILTER(?t = ?p) } }", parameter, "p");
        assertRefused("SELECT ?y WHERE { " + search + "BIND(1 AS ?p) }", parameter, "p");
        assertRefused("SELECT ?y WHERE { " + search + "} VALUES ?p { 'red' }", parameter, "p");
        assertRefused("SELECT ?y WHERE { " + search + "FILTER EXISTS { ?z ex:tag ?p } }", parameter, "p");
        assertRefused("SELECT ?y WHERE { " + search + "<< ?z ex:tag ?p >> ex:tag ?t }", parameter, "p");
        assertRefused("SELECT ?y WHERE { ?y ex:search ?p {| ex:tag ?n |} . ?p ex:query 'red' }", parameter, "p");
        assertRefused("SELECT ?y WHERE { " + describe + "?z ex:tag ?r }", result, "r");
        assertRefused("SELECT ?y WHERE { " + describe + "?z ex:tag ?q }", parameter, "q");
    }


    @Test
    @DisplayName("a blank node standing for parameters inside a triple term is refused")
    void blankNodeInTripleTerm()
    {
        assertRefused("SELECT ?y WHERE { ?y ex:search _:p {| ex:tag ?n |} . _:p ex:query 'red' }",
                MessageType.invalidParameterBlankNodeOccurence, "p");
        assertRefused("SELECT ?y WHERE { ?y ex:search _:p . _:p ex:query 'red' . ?z ex:tag <<( ?z ex:tag _:p )>> }",
                MessageType.invalidParameterBlankNodeOccurence, "p");
    }


    @Test
    @DisplayName("projected and ordered nodes give the solutions of the call written with blank nodes")
    void projectedNodes() throws Exception
    {
        // the variable is in scope, as it occurs in a triple of the query, but the call leaves it unbound
        for(String query : List.of("SELECT ?y ?p WHERE { " + search + "}", "SELECT * WHERE { " + search + "}",
                "SELECT ?y ?p WHERE { ?y ex:search [ ex:query 'red' ] }"))
            assertThat(execute(query), containsInAnyOrder(row(thing("a"), null), row(thing("b"), null)));

        assertThat(execute("SELECT ?y WHERE { " + search + "} ORDER BY ?p ?y"),
                is(List.of(row(thing("a")), row(thing("b")))));

        assertThat(execute("SELECT ?y ?t ?r ?q WHERE { " + describe + "}"), containsInAnyOrder(
                row(thing("a"), string("red"), null, null), row(thing("b"), string("red"), null, null)));
    }


    @Test
    @DisplayName("a variable of another scope and calls of their own groups are no other occurrences")
    void otherVariables() throws Exception
    {
        assertThat(execute("SELECT ?y ?p WHERE { { SELECT ?y WHERE { " + search + "} } ?y ex:tag ?p }"),
                containsInAnyOrder(row(thing("a"), string("red")), row(thing("b"), string("red"))));

        assertThat(execute("SELECT ?y ?z WHERE { { " + search + "} { ?z ex:search ?p . ?p ex:query 'blue' } }"),
                containsInAnyOrder(row(thing("a"), thing("c")), row(thing("b"), thing("c"))));
    }
}
