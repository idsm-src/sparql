package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.LimitExceedException;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.translator.ServiceException;
import cz.iocb.sparql.testing.Database;



/**
 * Calls of a procedure declared not deterministic, whose results differ from call to call: the engine calls it once for
 * every solution it is called for, and neither shares a call among the places of a query calling it alike nor
 * deduplicates the solutions it is called for, whereas a deterministic procedure of the same function is called once
 * for equal arguments.
 *
 * The things {@code a} and {@code b} have the tag {@code red}, {@code c} the tag {@code blue}; the property
 * {@code ex:both} is mapped twice, to the tag of a thing and to a note of it, so that a join with it is distributed
 * over the two mappings. The procedures {@code ex:draw} (declared not deterministic) and {@code ex:fixed}
 * (deterministic) call the same function, which returns the next value of the sequence {@code draws} whatever its
 * argument is.
 */
public class ProcedureDeterminismTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> ";

    /**
     * Configuration mapping the tables and the procedures.
     */
    private static SparqlDatabaseConfiguration config = null;


    /**
     * Creates the {@code procedure_determinism_test} schema with the tables, the sequence and the function, and maps
     * them.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema procedure_determinism_test");
            statement
                    .execute("create table procedure_determinism_test.item (x varchar not null, tag varchar not null)");
            statement.execute("insert into procedure_determinism_test.item values ('a', 'red'), ('b', 'red'), "
                    + "('c', 'blue')");
            statement.execute(
                    "create table procedure_determinism_test.extra (x varchar not null, note varchar not null)");
            statement.execute("insert into procedure_determinism_test.extra values ('a', 'one'), ('b', 'two'), "
                    + "('c', 'three')");
            statement.execute("create sequence procedure_determinism_test.draws minvalue 0");
            statement.execute("""
                    create function procedure_determinism_test.draw(seed varchar) returns setof int4 \
                    language plpgsql as $$ begin return next nextval('procedure_determinism_test.draws'); end $$""");
        }

        DatabaseSchema schema = new DatabaseSchema(connectionPool);

        config = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);
        config.addPrefix("ex", "http://example.org/");

        StringUserIriClass thing = new StringUserIriClass("thing", "http://example.org/thing/", "[a-z]");
        config.addIriClass(thing);

        DatabaseTable item = new DatabaseTable("procedure_determinism_test", "item");
        DatabaseTable extra = new DatabaseTable("procedure_determinism_test", "extra");

        config.addQuadMapping(item, null, config.createIriMapping("thing", "x"), config.createIriMapping("ex:tag"),
                config.createLiteralMapping(xsdString, "tag"));
        config.addQuadMapping(item, null, config.createIriMapping("thing", "x"), config.createIriMapping("ex:both"),
                config.createLiteralMapping(xsdString, "tag"));
        config.addQuadMapping(extra, null, config.createIriMapping("thing", "x"), config.createIriMapping("ex:both"),
                config.createLiteralMapping(xsdString, "note"));

        for(String name : List.of("draw", "fixed"))
        {
            ProcedureDefinition procedure = new ProcedureDefinition("http://example.org/" + name,
                    new Function("procedure_determinism_test", "draw"));
            procedure.addParameter(new ParameterDefinition("http://example.org/seed", xsdString, null));
            procedure.addResult(new ResultDefinition(xsdInt));
            procedure.setDeterministic(name.equals("fixed"));
            config.addProcedure(procedure);
        }
    }


    /**
     * Rows of the query, the procedures counting their calls from one.
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
        try(Connection connection = Database.getPool().getConnection();
                Statement statement = connection.createStatement())
        {
            statement.execute("select setval('procedure_determinism_test.draws', 0)");
        }

        try(Request request = new Engine(config).getRequest(); Result result = request.execute(prefix + query))
        {
            List<List<RdfTerm>> rows = new ArrayList<>();

            while(result.next())
                rows.add(Arrays.asList(result.getRow()));

            return rows;
        }
    }


    /**
     * The number of calls of the procedures since the last query.
     *
     * @return the number of calls of the procedures since the last query
     * @throws SQLException on database errors
     */
    private static long calls() throws SQLException
    {
        try(Connection connection = Database.getPool().getConnection();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("select last_value from procedure_determinism_test.draws"))
        {
            result.next();
            return result.getLong(1);
        }
    }


    @Test
    @DisplayName("a call taking a value of the solutions before it is made once for every solution")
    void dependentCall() throws Exception
    {
        List<List<RdfTerm>> rows = execute(
                "SELECT ?x ?r WHERE { ?x ex:tag ?t . ?r ex:draw [ ex:seed ?t ] . ?x ex:both ?z }");

        Map<RdfTerm, Set<RdfTerm>> draws = new HashMap<>();

        for(List<RdfTerm> row : rows)
            draws.computeIfAbsent(row.get(0), _ -> new HashSet<>()).add(row.get(1));

        // two rows of every thing, the tag and the note, with the value of the one call made for the thing
        assertThat(rows, hasSize(6));
        assertThat(draws.size(), is(3));

        for(Set<RdfTerm> values : draws.values())
            assertThat(values, hasSize(1));

        assertThat(calls(), is(3L));
    }


    @Test
    @DisplayName("calls taking no values of the solutions are made at every place of the query")
    void independentCalls() throws Exception
    {
        List<List<RdfTerm>> rows = execute(
                "SELECT ?a ?b WHERE { ?a ex:draw [ ex:seed 'x' ] . ?b ex:draw [ ex:seed 'x' ] }");

        assertThat(rows, hasSize(1));
        assertThat(rows.get(0).get(0), not(rows.get(0).get(1)));
        assertThat(calls(), is(2L));

        assertThat(execute("SELECT ?a WHERE { { ?a ex:draw [ ex:seed 'x' ] } UNION { ?a ex:draw [ ex:seed 'x' ] } }"),
                hasSize(2));
        assertThat(calls(), is(2L));

        // a deterministic procedure is called once for the same arguments
        rows = execute("SELECT ?a ?b WHERE { ?a ex:fixed [ ex:seed 'x' ] . ?b ex:fixed [ ex:seed 'x' ] }");

        assertThat(rows, hasSize(1));
        assertThat(rows.get(0).get(0), is(rows.get(0).get(1)));
        assertThat(calls(), is(1L));
    }


    @Test
    @DisplayName("the solutions a call is made for are not deduplicated")
    void distinct() throws Exception
    {
        // the things a and b are two solutions of the tag red, each with a call of its own
        assertThat(execute("SELECT DISTINCT ?r WHERE { VALUES ?t { 'red' } ?x ex:tag ?t . ?r ex:draw [ ex:seed ?t ] }"),
                hasSize(2));
        assertThat(calls(), is(2L));

        // the deterministic procedure may be called once for the equal arguments
        assertThat(
                execute("SELECT DISTINCT ?r WHERE { VALUES ?t { 'red' } ?x ex:tag ?t . ?r ex:fixed [ ex:seed ?t ] }"),
                hasSize(1));
    }
}
