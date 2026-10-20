package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.database.Function;
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
 * A procedure call is evaluated for the solutions of the patterns preceding it in its group, so that its parameters can
 * take their values from them. A call whose parameters take no value from those solutions is evaluated once and joined
 * with them instead; otherwise the database runs the function again for every solution. Such a call is evaluated once
 * even when the statement contains it several times, e.g. in every branch of a union the join was distributed over.
 */
public class ProcedureContextTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> ";

    /**
     * Configuration mapping the test table and the procedure.
     */
    private static SparqlDatabaseConfiguration config = null;


    /**
     * Creates the {@code procedure_context_test} schema: the things {@code a} and {@code b} have the tag {@code red},
     * {@code c} the tag {@code blue} and {@code d} the tag {@code green}; the procedure {@code ex:search} returns the
     * things with the tag given as {@code ex:query} and counts its calls in the sequence {@code calls}.
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema procedure_context_test");
            statement.execute("create table procedure_context_test.item (x varchar not null, tag varchar not null)");
            statement.execute("insert into procedure_context_test.item values "
                    + "('a', 'red'), ('b', 'red'), ('c', 'blue'), ('d', 'green')");
            statement.execute("create sequence procedure_context_test.calls minvalue 0");
            statement.execute("""
                    create function procedure_context_test.search(query varchar) returns setof varchar \
                    language plpgsql as $$ begin perform nextval('procedure_context_test.calls'); \
                    return query select x from procedure_context_test.item where tag = query; end $$""");
        }

        DatabaseSchema schema = new DatabaseSchema(connectionPool);

        config = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);
        config.addPrefix("ex", "http://example.org/");

        StringUserIriClass thing = new StringUserIriClass("thing", "http://example.org/thing/", "[a-z]");
        config.addIriClass(thing);

        DatabaseTable item = new DatabaseTable("procedure_context_test", "item");

        config.addQuadMapping(item, config.createIriMapping("<http://example.org/graph>"),
                config.createIriMapping("thing", "x"), config.createIriMapping("ex:tag"),
                config.createLiteralMapping(xsdString, "tag"));

        ProcedureDefinition search = new ProcedureDefinition("http://example.org/search",
                new Function("procedure_context_test", "search"));
        search.addParameter(new ParameterDefinition("http://example.org/query", xsdString, null));
        search.addResult(new ResultDefinition(thing));
        config.addProcedure(search);
    }


    /**
     * Runs the query and returns all rows.
     */
    private static List<List<RdfTerm>> execute(String query)
            throws SQLException, TranslateExceptions, LimitExceedException, ServiceException
    {
        Engine engine = new Engine(config);

        try(Request request = engine.getRequest(); Result result = request.execute(prefix + query))
        {
            List<List<RdfTerm>> rows = new ArrayList<>();

            while(result.next())
                rows.add(Arrays.asList(result.getRow()));

            return rows;
        }
    }


    /**
     * Runs the query and returns how many times the procedure was called.
     */
    private static long calls(String query) throws Exception
    {
        try(Connection connection = Database.getPool().getConnection();
                Statement statement = connection.createStatement())
        {
            statement.execute("select setval('procedure_context_test.calls', 0)");

            execute(query);

            try(ResultSet result = statement.executeQuery("select last_value from procedure_context_test.calls"))
            {
                result.next();
                return result.getLong(1);
            }
        }
    }


    /**
     * Expected row.
     */
    private static List<RdfTerm> row(RdfTerm... terms)
    {
        return Arrays.asList(terms);
    }


    /**
     * IRI of the thing with the given name.
     */
    private static Iri thing(String name)
    {
        return new Iri("http://example.org/thing/" + name);
    }


    /**
     * String literal.
     */
    private static TypedLiteral string(String value)
    {
        return new TypedLiteral(value, xsdStringIri);
    }


    @Test
    @DisplayName("a call with a constant parameter after a pattern is evaluated once")
    void independentCall() throws Exception
    {
        String query = "SELECT ?x ?y WHERE { ?x ex:tag ?t . ?y ex:search [ ex:query 'blue' ] }";

        assertThat(execute(query), containsInAnyOrder(row(thing("a"), thing("c")), row(thing("b"), thing("c")),
                row(thing("c"), thing("c")), row(thing("d"), thing("c"))));
        assertThat(calls(query), is(1L));
    }


    @Test
    @DisplayName("a call whose result is bound by a preceding pattern is joined on it")
    void independentCallSharedResult() throws Exception
    {
        String query = "SELECT ?x ?t WHERE { ?x ex:tag ?t . ?x ex:search [ ex:query 'red' ] }";

        assertThat(execute(query), containsInAnyOrder(row(thing("a"), string("red")), row(thing("b"), string("red"))));
        assertThat(calls(query), is(1L));
    }


    @Test
    @DisplayName("a call whose parameter is bound by a preceding pattern is evaluated for every solution")
    void dependentCall() throws Exception
    {
        String query = "SELECT ?x ?y WHERE { ?x ex:tag ?t . ?y ex:search [ ex:query ?t ] }";

        assertThat(execute(query),
                containsInAnyOrder(row(thing("a"), thing("a")), row(thing("a"), thing("b")),
                        row(thing("b"), thing("a")), row(thing("b"), thing("b")), row(thing("c"), thing("c")),
                        row(thing("d"), thing("d"))));
        assertThat(calls(query), is(4L));
    }


    @Test
    @DisplayName("a call whose parameter is not bound by a preceding pattern gets no value")
    void unboundParameter() throws Exception
    {
        String query = "SELECT ?x ?y WHERE { ?x ex:tag 'blue' . ?y ex:search [ ex:query ?q ] . ?z ex:tag ?q }";

        assertThat(execute(query), empty());
    }


    @Test
    @DisplayName("a call at the start of its group is evaluated once")
    void leadingCall() throws Exception
    {
        String query = "SELECT ?x ?t WHERE { ?x ex:search [ ex:query 'red' ] . ?x ex:tag ?t }";

        assertThat(execute(query), containsInAnyOrder(row(thing("a"), string("red")), row(thing("b"), string("red"))));
        assertThat(calls(query), is(1L));
    }


    @Test
    @DisplayName("a call with a constant parameter joined with a union is evaluated once")
    void independentCallWithUnion() throws Exception
    {
        String query = "SELECT ?x ?y WHERE { { ?x ex:tag 'red' } UNION { ?x ex:tag 'blue' } "
                + "?y ex:search [ ex:query 'red' ] }";

        assertThat(execute(query),
                containsInAnyOrder(row(thing("a"), thing("a")), row(thing("a"), thing("b")),
                        row(thing("b"), thing("a")), row(thing("b"), thing("b")), row(thing("c"), thing("a")),
                        row(thing("c"), thing("b"))));
        assertThat(calls(query), is(1L));
    }


    @Test
    @DisplayName("two calls with the same constant parameter are evaluated once")
    void sameIndependentCalls() throws Exception
    {
        String query = "SELECT ?y ?z WHERE { ?y ex:search [ ex:query 'red' ] . ?z ex:search [ ex:query 'red' ] }";

        assertThat(execute(query), containsInAnyOrder(row(thing("a"), thing("a")), row(thing("a"), thing("b")),
                row(thing("b"), thing("a")), row(thing("b"), thing("b"))));
        assertThat(calls(query), is(1L));
    }


    @Test
    @DisplayName("two calls with different constant parameters are evaluated separately")
    void differentIndependentCalls() throws Exception
    {
        String query = "SELECT ?y ?z WHERE { ?y ex:search [ ex:query 'red' ] . ?z ex:search [ ex:query 'blue' ] }";

        assertThat(execute(query), containsInAnyOrder(row(thing("a"), thing("c")), row(thing("b"), thing("c"))));
        assertThat(calls(query), is(2L));
    }


    @Test
    @DisplayName("a call with a constant parameter in an exists pattern is joined with the current solution")
    void independentCallInExists() throws Exception
    {
        String query = "SELECT ?x WHERE { ?x ex:tag ?t FILTER EXISTS { ?x ex:search [ ex:query 'red' ] } }";

        assertThat(execute(query), containsInAnyOrder(row(thing("a")), row(thing("b"))));
    }


    @Test
    @DisplayName("a call with a parameter of the current solution in an exists pattern is evaluated for it")
    void dependentCallInExists() throws Exception
    {
        String query = "SELECT ?x WHERE { ?x ex:tag ?t FILTER EXISTS { ?x ex:search [ ex:query ?t ] } }";

        assertThat(execute(query),
                containsInAnyOrder(row(thing("a")), row(thing("b")), row(thing("c")), row(thing("d"))));
    }
}
