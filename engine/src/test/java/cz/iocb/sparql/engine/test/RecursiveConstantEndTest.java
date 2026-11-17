package cz.iocb.sparql.engine.test;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.iri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
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
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.imcode.SqlSelect;
import cz.iocb.sparql.engine.rdf.Iri;
import cz.iocb.sparql.engine.rdf.RdfTerm;
import cz.iocb.sparql.engine.request.Engine;
import cz.iocb.sparql.engine.request.LimitExceedException;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.request.Request.PreparedQuery;
import cz.iocb.sparql.engine.request.Result;
import cz.iocb.sparql.engine.translator.ServiceException;
import cz.iocb.sparql.engine.translator.TranslateVisitor;
import cz.iocb.sparql.testing.Database;



/**
 * A walk whose end both the initial solutions and the step bind to one constant: every triple of the property has the
 * same object, so the recursion keeps the end as the constant, neither carrying it nor comparing it, and the walk still
 * gives the same solutions. The table {@code item} holds the subjects {@code :a}, {@code :b} and {@code :c}, mapped by
 * {@code :t} to the constant object {@code :c}.
 */
public class RecursiveConstantEndTest
{
    /**
     * Prefix declarations prepended to every query.
     */
    private static final String prefix = "PREFIX : <http://example.org/> ";

    /**
     * Engine over the configuration.
     */
    private static Engine engine = null;


    /**
     * Creates the {@code recursive_constant_end_test} schema with the table of the subjects and maps it.
     *
     * @throws SQLException on database errors
     */
    @BeforeAll
    static void init() throws SQLException
    {
        DataSource connectionPool = Database.getPool();

        try(Connection connection = connectionPool.getConnection(); Statement statement = connection.createStatement())
        {
            statement.execute("create schema recursive_constant_end_test");
            statement.execute("create table recursive_constant_end_test.item (s varchar collate \"C\" not null)");
            statement.execute("insert into recursive_constant_end_test.item values ('http://example.org/a'), "
                    + "('http://example.org/b'), ('http://example.org/c')");
        }

        SparqlDatabaseConfiguration config = new SparqlDatabaseConfiguration(null, connectionPool,
                new DatabaseSchema(connectionPool), true);
        config.addPrefix("", "http://example.org/");

        config.addQuadMapping(new DatabaseTable("recursive_constant_end_test", "item"), null,
                config.createIriMapping(iri, "s"), config.createIriMapping(":t"), config.createIriMapping(":c"));

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
     * Translates the query into SQL.
     *
     * @param query the query, without the prefix declarations
     * @return the generated SQL
     * @throws SQLException on database errors
     * @throws TranslateExceptions if the query has errors
     * @throws ServiceException if a federated SERVICE call fails
     */
    private static String translate(String query) throws SQLException, TranslateExceptions, ServiceException
    {
        try(Request request = engine.getRequest())
        {
            PreparedQuery prepared = request.prepareQuery(prefix + query, null);

            SqlSelect imcode = new TranslateVisitor(request).translate(prepared.getSyntaxTree(), null, null, List.of());

            return imcode.optimize(request, false).optimize(request, true).translate(request);
        }
    }


    /**
     * Row of the IRIs of the example namespace.
     *
     * @param names the local names
     * @return row of the IRIs
     */
    private static List<RdfTerm> row(String... names)
    {
        return Arrays.stream(names).<RdfTerm> map(n -> new Iri("http://example.org/" + n)).toList();
    }


    @Test
    @DisplayName("a walk ending in a constant gives its solutions")
    void solutions() throws Exception
    {
        assertThat(execute("SELECT ?x ?y WHERE { ?x :t+ ?y }"),
                containsInAnyOrder(row("a", "c"), row("b", "c"), row("c", "c")));

        assertThat(execute("SELECT ?y WHERE { :c :t* ?y }"), containsInAnyOrder(row("c")));
        assertThat(execute("SELECT ?y WHERE { :a :t* ?y }"), containsInAnyOrder(row("a"), row("c")));
        assertThat(execute("SELECT ?x WHERE { ?x :t+ :c }"), containsInAnyOrder(row("a"), row("b"), row("c")));
    }


    @Test
    @DisplayName("the recursion does not carry the constant end")
    void constantEnd() throws Exception
    {
        String sql = translate("SELECT ?x ?y WHERE { ?x :t+ ?y }");
        String recursion = sql.substring(sql.indexOf("WITH RECURSIVE"),
                sql.indexOf(" AS (", sql.indexOf("WITH RECURSIVE")));

        assertThat(recursion, not(containsString("y#")));
    }
}
