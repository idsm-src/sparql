package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import java.sql.Connection;
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
 * A procedure whose function is not strict, called for parameter values that the class of the parameter does not hold:
 * such a value gives no solution, as it does for a strict function, instead of being handed to the function as NULL.
 * The things {@code a} and {@code b} have the tag {@code red}, {@code c} the tag {@code blue}; the function of the
 * procedure {@code ex:search} returns the things of the tag, and every thing for a NULL tag.
 */
public class ProcedureNullParameterTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> ";

    /**
     * Engine over the configuration.
     */
    private static Engine engine = null;


    /**
     * Creates the {@code procedure_null_parameter_test} schema with the table of the things and the function, and maps
     * the procedure.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema procedure_null_parameter_test");
            statement.execute(
                    "create table procedure_null_parameter_test.item (x varchar not null, tag varchar not null)");
            statement.execute("insert into procedure_null_parameter_test.item values ('a', 'red'), ('b', 'red'), "
                    + "('c', 'blue')");
            statement.execute("""
                    create function procedure_null_parameter_test.search(query varchar) returns setof varchar \
                    language sql as $$ select x from procedure_null_parameter_test.item \
                    where query is null or tag = query $$""");
        }

        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, connectionPool,
                new DatabaseSchema(connectionPool), true);
        config.addPrefix("ex", "http://example.org/");

        StringUserIriClass thing = new StringUserIriClass("thing", "http://example.org/thing/");
        config.addIriClass(thing);

        ProcedureDefinition search = new ProcedureDefinition("http://example.org/search",
                new Function("procedure_null_parameter_test", "search"));
        search.addParameter(new ParameterDefinition("http://example.org/query", xsdString, null));
        search.addResult(new ResultDefinition(thing));
        config.addProcedure(search);

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
     * Row of the tag and the thing.
     *
     * @param tag the tag
     * @param thing the local name of the thing
     * @return row of the tag and the thing
     */
    private static List<RdfTerm> row(String tag, String thing)
    {
        return List.of(new TypedLiteral(tag, xsdStringIri), new Iri("http://example.org/thing/" + thing));
    }


    @Test
    @DisplayName("a parameter value of another class or unbound gives no solution")
    void missingValues() throws Exception
    {
        assertThat(execute("SELECT ?t ?x WHERE { VALUES ?t { \"red\" 1 } ?x ex:search [ ex:query ?t ] }"),
                containsInAnyOrder(row("red", "a"), row("red", "b")));

        assertThat(execute("SELECT ?t ?x WHERE { VALUES ?t { \"blue\" UNDEF } ?x ex:search [ ex:query ?t ] }"),
                containsInAnyOrder(row("blue", "c")));
    }
}
