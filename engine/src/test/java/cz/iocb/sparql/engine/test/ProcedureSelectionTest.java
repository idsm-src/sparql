package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration.getColumns;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.engine.mapping.datatypes.BuiltinDatatypes.xsdStringIri;
import static java.util.stream.Collectors.joining;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.DatabaseTable;
import cz.iocb.sparql.engine.database.Function;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.imcode.SqlSelect;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.mapping.classes.StringUserIriClass;
import cz.iocb.sparql.engine.mapping.extension.ParameterDefinition;
import cz.iocb.sparql.engine.mapping.extension.ProcedureDefinition;
import cz.iocb.sparql.engine.mapping.extension.ProcedureSpecialization;
import cz.iocb.sparql.engine.mapping.extension.ResultDefinition;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.rdf.TypedLiteral;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.LimitExceedException;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Request.PreparedQuery;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.translator.ServiceException;
import cz.iocb.sparql.engine.translator.TranslateVisitor;
import cz.iocb.sparql.testing.Database;



/**
 * A procedure whose result may take several classes, each coming from an index of its own, is asked for the classes the
 * query can use only: its specialiser (see {@link ProcedureDefinition#setSpecializer}) passes the names of their fields
 * to the function as its first parameter, and the call binds the result to these classes. A join with a pattern of one
 * class, or a filter accepting one, selects the class; a query using the result as it comes asks for every class.
 */
public class ProcedureSelectionTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX ex: <http://example.org/> ";

    /**
     * Configuration mapping the test tables and the procedure.
     */
    private static SparqlDatabaseConfiguration config = null;


    /**
     * Creates the {@code procedure_selection_test} schema: the thing {@code a} with a tag, the other {@code b} with a
     * kind, and the procedure {@code ex:search}, whose function returns the thing when asked for the field
     * {@code thing} and the other when asked for the field {@code other}.
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema procedure_selection_test");
            statement
                    .execute("create table procedure_selection_test.things (x varchar not null, tag varchar not null)");
            statement.execute("insert into procedure_selection_test.things values ('a', 'red')");
            statement.execute(
                    "create table procedure_selection_test.others (y varchar not null, kind varchar not null)");
            statement.execute("insert into procedure_selection_test.others values ('b', 'blue')");
            statement.execute("""
                    create function procedure_selection_test.search(parts varchar[], query varchar) \
                    returns table (thing varchar, other varchar) language sql as $$ \
                    select 'a'::varchar, null::varchar where 'thing' = any(parts) \
                    union all select null::varchar, 'b'::varchar where 'other' = any(parts) $$""");
        }

        DatabaseSchema schema = new DatabaseSchema(connectionPool);

        config = new SparqlDatabaseConfiguration(null, connectionPool, schema, true);
        config.addPrefix("ex", "http://example.org/");

        StringUserIriClass thing = new StringUserIriClass("thing", "http://example.org/thing/", "[a-z]");
        StringUserIriClass other = new StringUserIriClass("other", "http://example.org/other/", "[a-z]");
        config.addIriClass(thing);
        config.addIriClass(other);

        DatabaseTable things = new DatabaseTable("procedure_selection_test", "things");
        DatabaseTable others = new DatabaseTable("procedure_selection_test", "others");

        config.addQuadMapping(things, config.createIriMapping("<http://example.org/graph>"),
                config.createIriMapping("thing", "x"), config.createIriMapping("ex:tag"),
                config.createLiteralMapping(xsdString, "tag"));

        config.addQuadMapping(others, config.createIriMapping("<http://example.org/graph>"),
                config.createIriMapping("other", "y"), config.createIriMapping("ex:kind"),
                config.createLiteralMapping(xsdString, "kind"));

        Map<ResourceClass, List<Column>> mapping = new HashMap<>();
        mapping.put(thing, getColumns(thing, "thing"));
        mapping.put(other, getColumns(other, "other"));

        ProcedureDefinition search = new ProcedureDefinition("http://example.org/search",
                new Function("procedure_selection_test", "search"));
        search.addParameter(new ParameterDefinition("http://example.org/query", xsdString, null));

        ResultDefinition result = new ResultDefinition(null, mapping);
        search.addResult(result);

        // the function takes the names of the fields of the classes the query can use as its first parameter
        search.setSpecializer(classes -> {
            Set<ResourceClass> usable = classes.getOrDefault(result, mapping.keySet());
            String parts = usable.stream().map(c -> "\"" + mapping.get(c).get(0).getName() + "\"").sorted()
                    .collect(joining(",", "'{", "}'::varchar[]"));

            return new ProcedureSpecialization()
            {
                @Override
                public List<String> getArguments(List<String> declared)
                {
                    List<String> arguments = new ArrayList<>();
                    arguments.add(parts);
                    arguments.addAll(declared);
                    return arguments;
                }
            };
        });

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
     * Returns the SQL code the query is translated to.
     */
    private static String translate(String query) throws Exception
    {
        Engine engine = new Engine(config);

        try(Request request = engine.getRequest())
        {
            PreparedQuery prepared = request.prepareQuery(prefix + query, null);
            SqlSelect imcode = new TranslateVisitor(request).translate(prepared.getSyntaxTree(), null, null, List.of());

            return imcode.optimize(request, false).optimize(request, true).translate(request);
        }
    }


    /**
     * Expected row.
     */
    private static List<RdfTerm> row(RdfTerm... terms)
    {
        return Arrays.asList(terms);
    }


    @Test
    @DisplayName("a query using the result as it comes asks for every class")
    void everyClassAsked() throws Exception
    {
        String query = "SELECT ?c WHERE { ?c ex:search [ ex:query 'q' ] }";

        assertThat(execute(query), containsInAnyOrder(row(new Iri("http://example.org/thing/a")),
                row(new Iri("http://example.org/other/b"))));
        assertThat(translate(query), containsString("'{\"other\",\"thing\"}'::varchar[]"));
    }


    @Test
    @DisplayName("a join with a pattern of one class asks for that class only")
    void joinSelectsClass() throws Exception
    {
        String query = "SELECT ?c ?t WHERE { ?c ex:search [ ex:query 'q' ] . ?c ex:tag ?t }";

        assertThat(execute(query),
                containsInAnyOrder(row(new Iri("http://example.org/thing/a"), new TypedLiteral("red", xsdStringIri))));
        assertThat(translate(query), containsString("'{\"thing\"}'::varchar[]"));
    }


    @Test
    @DisplayName("a filter accepting one class asks for that class only")
    void filterSelectsClass() throws Exception
    {
        String query = "SELECT ?c WHERE { ?c ex:search [ ex:query 'q' ] FILTER(?c = <http://example.org/other/b>) }";

        assertThat(execute(query), containsInAnyOrder(row(new Iri("http://example.org/other/b"))));
        assertThat(translate(query), containsString("'{\"other\"}'::varchar[]"));
    }


    @Test
    @DisplayName("a result the query can use in no class has no solutions")
    void noClassAccepted() throws Exception
    {
        String query = "SELECT ?c ?k WHERE { ?c ex:search [ ex:query 'q' ] . ?c ex:kind ?k "
                + "FILTER(?c = <http://example.org/thing/a>) }";

        assertThat(execute(query), empty());
    }
}
