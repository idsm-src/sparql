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
 * A property path naming a procedure: a negated property set names only the properties a triple must not have, so it
 * calls nothing and is evaluated as any other path, whereas a path that could only call the procedure, like an
 * alternative or a repetition of it, is refused. The things {@code a} and {@code b} have the tags {@code red} and
 * {@code blue} and the notes {@code one} and {@code two}; {@code ex:search} is a procedure.
 */
public class ProcedurePathTest
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
     * Creates the {@code procedure_path_test} schema with the table of the things and the function of the procedure,
     * and maps them.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema procedure_path_test");
            statement.execute("create table procedure_path_test.item (x varchar not null, tag varchar not null, "
                    + "note varchar not null)");
            statement.execute("insert into procedure_path_test.item values ('a', 'red', 'one'), ('b', 'blue', 'two')");
            statement.execute("create function procedure_path_test.search(query varchar) returns setof varchar "
                    + "language sql as $$ select x from procedure_path_test.item where tag = query $$");
        }

        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, connectionPool,
                new DatabaseSchema(connectionPool), true);
        config.addPrefix("ex", "http://example.org/");

        StringUserIriClass thing = new StringUserIriClass("thing", "http://example.org/thing/", "[a-z]");
        config.addIriClass(thing);

        DatabaseTable item = new DatabaseTable("procedure_path_test", "item");

        config.addQuadMapping(item, null, config.createIriMapping("thing", "x"), config.createIriMapping("ex:tag"),
                config.createLiteralMapping(xsdString, "tag"));
        config.addQuadMapping(item, null, config.createIriMapping("thing", "x"), config.createIriMapping("ex:note"),
                config.createLiteralMapping(xsdString, "note"));

        ProcedureDefinition search = new ProcedureDefinition("http://example.org/search",
                new Function("procedure_path_test", "search"));
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
     * The row of a thing and a string.
     *
     * @param thing the local name of the thing
     * @param value the string
     * @return the row of the thing and the string
     */
    private static List<RdfTerm> row(String thing, String value)
    {
        return List.of(new Iri("http://example.org/thing/" + thing), new TypedLiteral(value, xsdStringIri));
    }


    @Test
    @DisplayName("a negated property set naming a procedure is a path")
    void negatedPropertySets() throws Exception
    {
        assertThat(execute("SELECT ?x ?v WHERE { ?x !ex:search ?v }"),
                containsInAnyOrder(row("a", "red"), row("b", "blue"), row("a", "one"), row("b", "two")));

        assertThat(execute("SELECT ?x ?v WHERE { ?x !(ex:search|ex:tag) ?v }"),
                containsInAnyOrder(row("a", "one"), row("b", "two")));

        assertThat(execute("SELECT ?x ?v WHERE { ?v !^ex:search ?x }"),
                containsInAnyOrder(row("a", "red"), row("b", "blue"), row("a", "one"), row("b", "two")));

        // the procedure is called as before
        assertThat(execute("SELECT ?x WHERE { ?x ex:search [ ex:query 'red' ] }"),
                containsInAnyOrder(List.<RdfTerm> of(new Iri("http://example.org/thing/a"))));
    }


    @Test
    @DisplayName("other paths naming a procedure are refused")
    void otherPaths()
    {
        for(String path : List.of("ex:search*", "ex:search|ex:tag", "ex:tag/ex:search"))
        {
            TranslateExceptions e = assertThrows(TranslateExceptions.class,
                    () -> execute("SELECT * WHERE { ?x " + path + " ?v }"));

            assertThat(path, e.getMessages(), hasItem(
                    hasProperty("message", is(MessageType.invalidProcedureCallPropertyPathCombination.getText()))));
        }
    }
}
